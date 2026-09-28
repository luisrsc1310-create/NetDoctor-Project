package com.netdiag.model;

/**
 * Representa o resultado do scanner de impressoras na rede local.
 * Cada instância corresponde a um IP que respondeu na sub-rede com porta de impressão ativa.
 */
public class NetworkPrinterStatus {

    private final String ipAddress;
    private final String hostname;
    private final boolean pingOk;
    private final boolean port9100Ok;   // RAW / JetDirect
    private final boolean port80Ok;     // Painel web
    private final boolean port515Ok;    // LPD
    private final long pingMs;
    private final String statusLabel;   // "✅ Pronta", "⚠ Alerta", "❌ Offline"
    private final String statusStyle;   // Classe CSS da célula

    public NetworkPrinterStatus(String ipAddress, String hostname,
                                boolean pingOk, boolean port9100Ok,
                                boolean port80Ok, boolean port515Ok, long pingMs) {
        this.ipAddress  = ipAddress;
        this.hostname   = hostname;
        this.pingOk     = pingOk;
        this.port9100Ok = port9100Ok;
        this.port80Ok   = port80Ok;
        this.port515Ok  = port515Ok;
        this.pingMs     = pingMs;

        // Determina rótulo e classe CSS de acordo com o resultado
        if (!pingOk) {
            this.statusLabel = "❌ Offline";
            this.statusStyle = "status-offline";
        } else if (!port9100Ok) {
            this.statusLabel = "⚠ Alerta (Porta 9100 Fechada)";
            this.statusStyle = "status-warning";
        } else {
            this.statusLabel = "✅ Pronta para Imprimir";
            this.statusStyle = "status-ok";
        }
    }

    public String getIpAddress()   { return ipAddress; }
    public String getHostname()    { return hostname; }
    public boolean isPingOk()      { return pingOk; }
    public boolean isPort9100Ok()  { return port9100Ok; }
    public boolean isPort80Ok()    { return port80Ok; }
    public boolean isPort515Ok()   { return port515Ok; }
    public long    getPingMs()     { return pingMs; }
    public String  getStatusLabel(){ return statusLabel; }
    public String  getStatusStyle(){ return statusStyle; }

    /** Texto formatado das portas abertas para exibir na tabela */
    public String getPortsOpen() {
        StringBuilder sb = new StringBuilder();
        if (port9100Ok) sb.append("9100 (RAW) ");
        if (port80Ok)   sb.append("80 (Web) ");
        if (port515Ok)  sb.append("515 (LPD) ");
        return sb.toString().trim().isEmpty() ? "Nenhuma" : sb.toString().trim();
    }

    /** Latência como texto */
    public String getPingText() {
        return pingOk ? pingMs + " ms" : "—";
    }
}
