package com.netdiag.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DiagnosticLog {
    private int id;
    private String timestamp;
    private String category; // ex: "Computador", "Impressora", "Rede / IP"
    private String target;   // ex: "192.168.1.50", "HP LaserJet"
    private String status;   // ex: "ONLINE", "OFFLINE", "OK", "ERRO"
    private String details;  // ex: "Ping 12ms", "Porta 9100 aberta", etc.

    public DiagnosticLog() {
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }

    public DiagnosticLog(int id, String timestamp, String category, String target, String status, String details) {
        this.id = id;
        this.timestamp = (timestamp != null && !timestamp.trim().isEmpty())
                ? timestamp
                : LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        this.category = category;
        this.target = target;
        this.status = status;
        this.details = details;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
