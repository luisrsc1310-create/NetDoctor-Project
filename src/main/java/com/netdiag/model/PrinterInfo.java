package com.netdiag.model;

public class PrinterInfo {
    private String name;
    private String portOrIp;
    private boolean isDefault;
    private String status;
    private String details;

    public PrinterInfo(String name, String portOrIp, boolean isDefault, String status, String details) {
        this.name = name;
        this.portOrIp = portOrIp;
        this.isDefault = isDefault;
        this.status = status;
        this.details = details;
    }

    public String getName() { return name; }
    public String getPortOrIp() { return portOrIp; }
    public boolean isDefault() { return isDefault; }
    public String getStatus() { return status; }
    public String getDetails() { return details; }
}
