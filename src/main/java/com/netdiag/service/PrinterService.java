package com.netdiag.service;

import com.netdiag.model.PrinterInfo;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class PrinterService {

    private final NetworkService networkService = new NetworkService();

    public List<PrinterInfo> getInstalledPrinters() {
        List<PrinterInfo> list = new ArrayList<>();
        try {
            PrintService defaultPrintService = PrintServiceLookup.lookupDefaultPrintService();
            String defaultName = defaultPrintService != null ? defaultPrintService.getName() : "";


            PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
            for (PrintService ps : services) {
                boolean isDefault = ps.getName().equalsIgnoreCase(defaultName);
                list.add(new PrinterInfo(
                        ps.getName(),
                        "Instalada no Windows",
                        isDefault,
                        "Pronta",
                        "Tipo de serviço: " + ps.getClass().getSimpleName()
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (list.isEmpty() && System.getProperty("os.name").toLowerCase().contains("win")) {
            return getPrintersViaPowerShell();
        }

        return list;
    }

    public List<PrinterInfo> getPrintersViaPowerShell() {
        List<PrinterInfo> list = new ArrayList<>();
        try {
            String psCommand = "Get-Printer | Select-Object Name, PortName, PrinterStatus, Default | ConvertTo-Csv -NoTypeInformation";
            Process process = new ProcessBuilder("powershell.exe", "-NoProfile", "-Command", psCommand).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                boolean isFirst = true;
                while ((line = reader.readLine()) != null) {
                    if (isFirst) {
                        isFirst = false;
                        continue;
                    }
                    String[] parts = line.replace("\"", "").split(",");
                    if (parts.length >= 4) {
                        String name = parts[0].trim();
                        String port = parts[1].trim();
                        String status = parts[2].trim();
                        boolean isDefault = Boolean.parseBoolean(parts[3].trim());
                        list.add(new PrinterInfo(name, port, isDefault, status, "Porta: " + port));
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static class PrinterDiagnosticResult {
        public boolean pingOk;
        public boolean rawPort9100Ok;
        public boolean httpPort80Ok;
        public boolean lpdPort515Ok;
        public String details;

        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("Ping: ").append(pingOk ? "OK" : "FALHA").append("\n");
            sb.append("Porta RAW (9100): ").append(rawPort9100Ok ? "ABERTA (Pronta para imprimir)" : "FECHADA").append("\n");
            sb.append("Painel Web HTTP (80/443): ").append(httpPort80Ok ? "DISPONÍVEL" : "NÃO RESPONDE").append("\n");
            sb.append("Porta LPD (515): ").append(lpdPort515Ok ? "ABERTA" : "FECHADA").append("\n");
            return sb.toString();
        }
    }

    public PrinterDiagnosticResult diagnoseNetworkPrinter(String ipAddress) {
        PrinterDiagnosticResult res = new PrinterDiagnosticResult();
        NetworkService.PingResult pingResult = networkService.ping(ipAddress, 1500);
        res.pingOk = pingResult.reachable;
        res.rawPort9100Ok = networkService.testPort(ipAddress, 9100, 1500);
        res.httpPort80Ok = networkService.testPort(ipAddress, 80, 1500) || networkService.testPort(ipAddress, 443, 1500);
        res.lpdPort515Ok = networkService.testPort(ipAddress, 515, 1500);
        res.details = res.getSummary();
        return res;
    }

    /**
     * Reinicia o serviço Spooler de Impressão do Windows
     */
    public String restartSpooler() {
        try {
            executeCmd("net stop spooler");
            Thread.sleep(1500);
            String startOut = executeCmd("net start spooler");
            return "Spooler de impressão reiniciado com sucesso!\n" + startOut;
        } catch (Exception e) {
            return "Erro ao reiniciar Spooler: " + e.getMessage() + "\n(Execute como Administrador)";
        }
    }

    /**
     * Limpa a fila de impressão travada do Windows (deleta arquivos em spool\\PRINTERS)
     */
    public String clearPrintQueue() {
        try {
            executeCmd("net stop spooler");
            Thread.sleep(1000);

            // Deletar arquivos da fila travada
            File spoolDir = new File("C:\\Windows\\System32\\spool\\PRINTERS");
            int deleted = 0;
            if (spoolDir.exists() && spoolDir.isDirectory()) {
                File[] files = spoolDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.delete()) {
                            deleted++;
                        }
                    }
                }
            }

            executeCmd("net start spooler");
            return String.format("Fila de impressão limpa com sucesso! (%d arquivo(s) travados removidos). Spooler reiniciado.", deleted);
        } catch (Exception e) {
            return "Erro ao limpar fila de impressão: " + e.getMessage() + "\n(Execute como Administrador)";
        }
    }

    private String executeCmd(String cmd) throws Exception {
        Process p = new ProcessBuilder("cmd.exe", "/c", cmd).start();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.ISO_8859_1))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        p.waitFor();
        return sb.toString().trim();
    }
}
