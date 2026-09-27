package com.netdiag.service;

import com.netdiag.model.DiagnosticLog;
import com.netdiag.model.NetworkProfile;
import com.netdiag.model.PrinterInfo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {

    public File generateHtmlReport(
            SystemInfoService.ComputerDiagnostics comp,
            List<NetworkService.NetworkInterfaceInfo> adapters,
            List<PrinterInfo> printers,
            List<NetworkProfile> profiles,
            List<DiagnosticLog> logs
    ) throws IOException {

        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        File reportFile = new File("relatorio-tecnico-" + System.currentTimeMillis() + ".html");

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html lang='pt-BR'><head><meta charset='UTF-8'>");
        html.append("<title>Relatório Técnico de Diagnóstico - NetDiag Pro</title>");
        html.append("<style>");
        html.append("body { font-family: 'Segoe UI', Arial, sans-serif; margin: 30px; background: #f8fafc; color: #1e293b; }");
        html.append(".container { max-width: 900px; margin: auto; background: #ffffff; padding: 30px; border-radius: 8px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); }");
        html.append("h1 { color: #1e40af; border-bottom: 2px solid #e2e8f0; padding-bottom: 10px; margin-top: 0; }");
        html.append("h2 { color: #0f172a; margin-top: 25px; border-left: 4px solid #2563eb; padding-left: 10px; }");
        html.append("table { width: 100%; border-collapse: collapse; margin-top: 10px; }");
        html.append("th, td { border: 1px solid #e2e8f0; padding: 10px; text-align: left; font-size: 13px; }");
        html.append("th { background-color: #f1f5f9; color: #334155; font-weight: 600; }");
        html.append(".badge { display: inline-block; padding: 4px 8px; border-radius: 4px; font-weight: bold; font-size: 11px; }");
        html.append(".badge-ok { background: #dcfce7; color: #15803d; }");
        html.append(".badge-warn { background: #fef9c3; color: #a16207; }");
        html.append(".footer { margin-top: 30px; text-align: center; font-size: 12px; color: #94a3b8; }");
        html.append("</style></head><body>");

        html.append("<div class='container'>");
        html.append("<h1>📋 Relatório Técnico de TI & Diagnóstico</h1>");
        html.append("<p><strong>Data de Emissão:</strong> ").append(dateStr).append("</p>");

        // Computador
        html.append("<h2>1. Dados do Computador & Hardware</h2>");
        html.append("<table>");
        html.append("<tr><th>Item</th><th>Informação</th></tr>");
        html.append("<tr><td>Hostname / Usuário</td><td>").append(comp.hostname).append(" / ").append(comp.userName).append("</td></tr>");
        html.append("<tr><td>Sistema Operacional</td><td>").append(comp.osName).append(" (").append(comp.osArch).append(")</td></tr>");
        html.append("<tr><td>Processadores (Cores)</td><td>").append(comp.availableProcessors).append(" núcleos</td></tr>");
        if (comp.totalRamBytes > 0) {
            long totalGb = comp.totalRamBytes / (1024 * 1024 * 1024);
            long freeGb = comp.freeRamBytes / (1024 * 1024 * 1024);
            html.append("<tr><td>Memória RAM</td><td>Total: ").append(totalGb).append(" GB | Livre: ").append(freeGb).append(" GB</td></tr>");
        }
        for (SystemInfoService.DiskInfo disk : comp.disks) {
            html.append("<tr><td>Disco ").append(disk.name).append("</td><td>").append(disk.toString()).append("</td></tr>");
        }
        html.append("</table>");

        // Placas de Rede
        html.append("<h2>2. Interfaces de Rede (Placas)</h2>");
        html.append("<table>");
        html.append("<tr><th>Adaptador</th><th>IPv4</th><th>MAC Address</th><th>Status</th></tr>");
        for (NetworkService.NetworkInterfaceInfo ad : adapters) {
            html.append("<tr>")
                .append("<td>").append(ad.displayName).append("</td>")
                .append("<td>").append(ad.ipAddress.isEmpty() ? "Não atribuído" : ad.ipAddress).append("</td>")
                .append("<td>").append(ad.macAddress).append("</td>")
                .append("<td><span class='badge badge-ok'>").append(ad.isUp ? "Ativa" : "Inativa").append("</span></td>")
                .append("</tr>");
        }
        html.append("</table>");

        // Impressoras
        html.append("<h2>3. Impressoras Encontradas</h2>");
        html.append("<table>");
        html.append("<tr><th>Nome da Impressora</th><th>Porta / Conexão</th><th>Padrão</th><th>Status</th></tr>");
        for (PrinterInfo pr : printers) {
            html.append("<tr>")
                .append("<td>").append(pr.getName()).append("</td>")
                .append("<td>").append(pr.getPortOrIp()).append("</td>")
                .append("<td>").append(pr.isDefault() ? "SIM" : "NÃO").append("</td>")
                .append("<td>").append(pr.getStatus()).append("</td>")
                .append("</tr>");
        }
        html.append("</table>");

        // Histórico recente
        html.append("<h2>4. Histórico Recente de Diagnósticos</h2>");
        html.append("<table>");
        html.append("<tr><th>Data/Hora</th><th>Categoria</th><th>Alvo</th><th>Status</th><th>Detalhes</th></tr>");
        for (DiagnosticLog log : logs) {
            html.append("<tr>")
                .append("<td>").append(log.getTimestamp()).append("</td>")
                .append("<td>").append(log.getCategory()).append("</td>")
                .append("<td>").append(log.getTarget()).append("</td>")
                .append("<td>").append(log.getStatus()).append("</td>")
                .append("<td>").append(log.getDetails()).append("</td>")
                .append("</tr>");
        }
        html.append("</table>");

        html.append("<div class='footer'>Gerado automaticamente pelo <strong>NetDiag Pro</strong> - Ferramenta de Suporte e Diagnóstico em Java.</div>");
        html.append("</div></body></html>");

        try (FileWriter fw = new FileWriter(reportFile)) {
            fw.write(html.toString());
        }

        return reportFile;
    }
}
