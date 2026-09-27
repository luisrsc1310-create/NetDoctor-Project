package com.netdiag.model;

public class DiscoveredDevice {
    private String ipAddress;
    private String hostname;
    private String deviceType; // "Impressora de Rede", "Computador / Servidor", "Dispositivo de Rede"
    private long pingMs;
    private String openPorts; // ex: "Porta 9100 (Impressão), 80 (Web)"

    public DiscoveredDevice(String ipAddress, String hostname, String deviceType, long pingMs, String openPorts) {
        this.ipAddress = ipAddress;
        this.hostname = hostname;
        this.deviceType = deviceType;
        this.pingMs = pingMs;
        this.openPorts = openPorts;
    }

    public String getIpAddress() { return ipAddress; }
    public String getHostname() { return hostname; }
    public String getDeviceType() { return deviceType; }
    public long getPingMs() { return pingMs; }
    public String getOpenPorts() { return openPorts; }
}
