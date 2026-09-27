package com.netdiag.model;

public class NetworkProfile {
    private int id;
    private String name;
    private String adapterName;
    private String ipAddress;
    private String subnetMask;
    private String gateway;
    private String dnsPrimary;
    private String dnsSecondary;

    public NetworkProfile() {}

    public NetworkProfile(int id, String name, String adapterName, String ipAddress, String subnetMask, String gateway, String dnsPrimary, String dnsSecondary) {
        this.id = id;
        this.name = name;
        this.adapterName = adapterName;
        this.ipAddress = ipAddress;
        this.subnetMask = subnetMask;
        this.gateway = gateway;
        this.dnsPrimary = dnsPrimary;
        this.dnsSecondary = dnsSecondary;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAdapterName() { return adapterName; }
    public void setAdapterName(String adapterName) { this.adapterName = adapterName; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getSubnetMask() { return subnetMask; }
    public void setSubnetMask(String subnetMask) { this.subnetMask = subnetMask; }

    public String getGateway() { return gateway; }
    public void setGateway(String gateway) { this.gateway = gateway; }

    public String getDnsPrimary() { return dnsPrimary; }
    public void setDnsPrimary(String dnsPrimary) { this.dnsPrimary = dnsPrimary; }

    public String getDnsSecondary() { return dnsSecondary; }
    public void setDnsSecondary(String dnsSecondary) { this.dnsSecondary = dnsSecondary; }

    @Override
    public String toString() {
        return name + " (" + ipAddress + ")";
    }
}
