package com.netdiag.service;

import com.netdiag.model.DiscoveredDevice;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class NetworkService {

    public static class PingResult {
        public final boolean reachable;
        public final long responseTimeMs;
        public final String message;

        public PingResult(boolean reachable, long responseTimeMs, String message) {
            this.reachable = reachable;
            this.responseTimeMs = responseTimeMs;
            this.message = message;
        }
    }

    public static class NetworkInterfaceInfo {
        public final String name;          // Alias do Windows usado pelo netsh (ex: "Ethernet", "Wi-Fi")
        public final String displayName;   // Descrição completa (ex: "Ethernet - Realtek PCIe 2.5GbE")
        public final String ipAddress;
        public final String macAddress;
        public final boolean isUp;

        public NetworkInterfaceInfo(String name, String displayName, String ipAddress, String macAddress, boolean isUp) {
            this.name = name;
            this.displayName = displayName;
            this.ipAddress = ipAddress;
            this.macAddress = macAddress;
            this.isUp = isUp;
        }

        @Override
        public String toString() {
            return displayName + (ipAddress.isEmpty() ? " (Sem IP)" : " (" + ipAddress + ")");
        }
    }

    public boolean checkInternetConnection() {
        String[] testHosts = {"1.1.1.1", "8.8.8.8"};
        for (String host : testHosts) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, 53), 1500);
                return true;
            } catch (Exception ignored) {

            }
        }
        return false;
    }

    public PingResult ping(String host, int timeoutMs) {
        long start = System.currentTimeMillis();
        try {
            InetAddress address = InetAddress.getByName(host);
            boolean reachable = address.isReachable(timeoutMs);
            long elapsed = System.currentTimeMillis() - start;

            if (reachable) {
                return new PingResult(true, elapsed, "Respondendo em " + elapsed + " ms");
            } else {
                return pingWindows(host);
            }
        } catch (Exception e) {
            return new PingResult(false, -1, "Falha: " + e.getMessage());
        }
    }

    private PingResult pingWindows(String host) {
        long start = System.currentTimeMillis();
        try {
            Process process = new ProcessBuilder("ping", "-n", "1", "-w", "1000", host).start();
            int exitCode = process.waitFor();
            long elapsed = System.currentTimeMillis() - start;
            if (exitCode == 0) {
                return new PingResult(true, elapsed, "Respondendo via ICMP (" + elapsed + " ms)");
            }
        } catch (Exception ignored) {
        }
        return new PingResult(false, -1, "Host inalcançável (Timeout)");
    }

    public boolean testPort(String host, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Lista adaptadores de rede obtendo o nome exato da interface do Windows (ex: "Ethernet")
     * essencial para que comandos netsh funcionem perfeitamente.
     */
    public List<NetworkInterfaceInfo> getNetworkAdapters() {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            List<NetworkInterfaceInfo> winList = getNetworkAdaptersWindows();
            if (!winList.isEmpty()) {
                return winList;
            }
        }
        return getNetworkAdaptersFallback();
    }

    private List<NetworkInterfaceInfo> getNetworkAdaptersWindows() {
        List<NetworkInterfaceInfo> list = new ArrayList<>();
        try {
            String ps = "Get-NetIPConfiguration | ForEach-Object { " +
                    "$alias = $_.InterfaceAlias; " +
                    "$desc = $_.InterfaceDescription; " +
                    "$ip = if ($_.IPv4Address) { $_.IPv4Address.IPAddress } else { '' }; " +
                    "$adapter = Get-NetAdapter -Name $alias -ErrorAction SilentlyContinue; " +
                    "$mac = if ($adapter) { $adapter.MacAddress } else { '' }; " +
                    "$status = if ($adapter) { $adapter.Status } else { '' }; " +
                    "\"$alias;;$desc;;$ip;;$mac;;$status\" " +
                    "}";

            Process process = new ProcessBuilder("powershell.exe", "-NoProfile", "-Command", ps).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(";;");
                    if (parts.length >= 5) {
                        String alias = parts[0].trim();
                        String desc = parts[1].trim();
                        String ip = parts[2].trim();
                        String mac = parts[3].trim();
                        String status = parts[4].trim();

                        boolean isUp = status.equalsIgnoreCase("Up");
                        String display = alias.equalsIgnoreCase(desc) ? alias : alias + " - " + desc;
                        list.add(new NetworkInterfaceInfo(alias, display, ip, mac, isUp));
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private List<NetworkInterfaceInfo> getNetworkAdaptersFallback() {
        List<NetworkInterfaceInfo> list = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;

                String dName = ni.getDisplayName();
                if (dName.contains("Filter") || dName.contains("QoS") || dName.contains("WAN Miniport")) continue;

                String ip = "";
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address) {
                        ip = addr.getHostAddress();
                        break;
                    }
                }

                String mac = "";
                byte[] hardwareAddress = ni.getHardwareAddress();
                if (hardwareAddress != null) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < hardwareAddress.length; i++) {
                        sb.append(String.format("%02X%s", hardwareAddress[i], (i < hardwareAddress.length - 1) ? "-" : ""));
                    }
                    mac = sb.toString();
                }

                list.add(new NetworkInterfaceInfo(ni.getName(), ni.getDisplayName(), ip, mac, ni.isUp()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public void scanNetworkRange(String baseSubnet, int startHost, int endHost, Consumer<DiscoveredDevice> onDeviceFound, Consumer<Double> onProgress) {
        int total = endHost - startHost + 1;
        ExecutorService executor = Executors.newFixedThreadPool(40);
        java.util.concurrent.atomic.AtomicInteger completed = new java.util.concurrent.atomic.AtomicInteger(0);

        for (int i = startHost; i <= endHost; i++) {
            final String targetIp = baseSubnet + "." + i;
            executor.submit(() -> {
                try {
                    long t0 = System.currentTimeMillis();
                    boolean reachable = false;
                    try {
                        InetAddress addr = InetAddress.getByName(targetIp);
                        reachable = addr.isReachable(400);
                    } catch (Exception ignored) {
                    }

                    boolean port9100 = testPort(targetIp, 9100, 300);
                    boolean port80 = testPort(targetIp, 80, 300);
                    boolean port445 = testPort(targetIp, 445, 300);

                    if (reachable || port9100 || port80 || port445) {
                        long elapsed = System.currentTimeMillis() - t0;
                        String hostName = targetIp;
                        try {
                            hostName = InetAddress.getByName(targetIp).getHostName();
                        } catch (Exception ignored) {
                        }

                        String type;
                        StringBuilder ports = new StringBuilder();
                        if (port9100) {
                            type = "Impressora de Rede";
                            ports.append("9100 (RAW) ");
                        } else if (port80) {
                            type = "Interface Web / Dispositivo";
                            ports.append("80 (HTTP) ");
                        } else if (port445) {
                            type = "Computador Windows (SMB)";
                            ports.append("445 (SMB) ");
                        } else {
                            type = "Dispositivo Ativo";
                            ports.append("ICMP Ping ");
                        }

                        DiscoveredDevice dev = new DiscoveredDevice(targetIp, hostName, type, elapsed, ports.toString().trim());
                        onDeviceFound.accept(dev);
                    }
                } finally {
                    int c = completed.incrementAndGet();
                    if (onProgress != null) {
                        onProgress.accept((double) c / total);
                    }
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(25, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * Aplica IP Estático via netsh sem problemas de aspas do cmd.exe
     */
    public String applyStaticIp(String adapterAlias, String ip, String mask, String gateway, String dns1, String dns2) {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add("netsh");
            cmd.add("interface");
            cmd.add("ipv4");
            cmd.add("set");
            cmd.add("address");
            cmd.add("name=" + adapterAlias);
            cmd.add("static");
            cmd.add(ip);
            cmd.add(mask);
            if (gateway != null && !gateway.trim().isEmpty()) {
                cmd.add(gateway.trim());
            }

            String resIp = executeProcess(cmd);
            if (isElevationRequired(resIp)) {
                return "Atenção: É necessário executar o programa como Administrador para alterar o IP do Windows!";
            }

            StringBuilder output = new StringBuilder(resIp.isEmpty() ? "IP e Máscara aplicados com sucesso!\n" : resIp + "\n");

            // DNS Primário
            if (dns1 != null && !dns1.trim().isEmpty()) {
                List<String> cmdDns1 = List.of("netsh", "interface", "ipv4", "set", "dns", "name=" + adapterAlias, "static", dns1.trim());
                String resDns1 = executeProcess(cmdDns1);
                if (!resDns1.isEmpty()) output.append(resDns1).append("\n");
                else output.append("DNS Primário configurado: ").append(dns1).append("\n");
            }

            // DNS Secundário
            if (dns2 != null && !dns2.trim().isEmpty()) {
                List<String> cmdDns2 = List.of("netsh", "interface", "ipv4", "add", "dns", "name=" + adapterAlias, dns2.trim(), "index=2");
                String resDns2 = executeProcess(cmdDns2);
                if (!resDns2.isEmpty()) output.append(resDns2).append("\n");
                else output.append("DNS Secundário configurado: ").append(dns2).append("\n");
            }

            return output.toString().trim();
        } catch (Exception e) {
            return "Erro ao alterar IP: " + e.getMessage();
        }
    }

    /**
     * Restaura placa de rede para DHCP automático via netsh direto
     */
    public String applyDhcp(String adapterAlias) {
        try {
            List<String> cmdIp = List.of("netsh", "interface", "ipv4", "set", "address", "name=" + adapterAlias, "source=dhcp");
            List<String> cmdDns = List.of("netsh", "interface", "ipv4", "set", "dns", "name=" + adapterAlias, "source=dhcp");

            String out1 = executeProcess(cmdIp);
            if (isElevationRequired(out1)) {
                return "Atenção: É necessário executar o programa como Administrador para alterar configurações de rede!";
            }

            String out2 = executeProcess(cmdDns);

            String combined = (out1 + "\n" + out2).trim();
            return combined.isEmpty() ? "Placa '" + adapterAlias + "' configurada para DHCP (IP e DNS automáticos) com sucesso!" : combined;
        } catch (Exception e) {
            return "Erro ao restaurar DHCP: " + e.getMessage();
        }
    }

    public String flushDns() {
        try {
            return executeProcess(List.of("ipconfig", "/flushdns"));
        } catch (Exception e) {
            return "Erro ao limpar DNS: " + e.getMessage();
        }
    }

    public String renewIp() {
        try {
            return executeProcess(List.of("ipconfig", "/renew"));
        } catch (Exception e) {
            return "Erro ao renovar IP: " + e.getMessage();
        }
    }

    private boolean isElevationRequired(String output) {
        if (output == null) return false;
        String lower = output.toLowerCase();
        return lower.contains("requires elevation") ||
                lower.contains("exige elevação") ||
                lower.contains("run as administrator") ||
                lower.contains("como administrador") ||
                lower.contains("acesso negado") ||
                lower.contains("access is denied");
    }

    private String executeProcess(List<String> commandList) throws Exception {
        Process process = new ProcessBuilder(commandList).start();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.ISO_8859_1))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.ISO_8859_1))) {
            String line;
            while ((line = errorReader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        process.waitFor();
        return sb.toString().trim();
    }
}
