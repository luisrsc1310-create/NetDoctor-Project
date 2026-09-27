package com.netdiag.service;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class SystemInfoService {

    public static class DiskInfo {
        public final String name;
        public final long totalBytes;
        public final long freeBytes;

        public DiskInfo(String name, long totalBytes, long freeBytes) {
            this.name = name;
            this.totalBytes = totalBytes;
            this.freeBytes = freeBytes;
        }

        public double getPercentUsed() {
            if (totalBytes == 0) return 0;
            return ((double) (totalBytes - freeBytes) / totalBytes) * 100.0;
        }

        @Override
        public String toString() {
            long totalGb = totalBytes / (1024 * 1024 * 1024);
            long freeGb = freeBytes / (1024 * 1024 * 1024);
            return String.format("%s [Total: %d GB | Livre: %d GB (%.1f%% usado)]", name, totalGb, freeGb, getPercentUsed());
        }
    }

    public static class ComputerDiagnostics {
        public String osName;
        public String osArch;
        public String hostname;
        public String userName;
        public int availableProcessors;
        public long totalRamBytes;
        public long freeRamBytes;
        public List<DiskInfo> disks = new ArrayList<>();
    }

    public ComputerDiagnostics collectDiagnostics() {
        ComputerDiagnostics diag = new ComputerDiagnostics();
        diag.osName = System.getProperty("os.name") + " (" + System.getProperty("os.version") + ")";
        diag.osArch = System.getProperty("os.arch");
        diag.userName = System.getProperty("user.name");
        diag.availableProcessors = Runtime.getRuntime().availableProcessors();

        try {
            diag.hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            diag.hostname = "Desconhecido";
        }

        // Memória RAM
        try {
            var osBean = ManagementFactory.getOperatingSystemMXBean();
            if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
                diag.totalRamBytes = sunOsBean.getTotalMemorySize();
                diag.freeRamBytes = sunOsBean.getFreeMemorySize();
            }
        } catch (Exception ignored) {
        }

        // Discos
        File[] roots = File.listRoots();
        if (roots != null) {
            for (File root : roots) {
                diag.disks.add(new DiskInfo(root.getAbsolutePath(), root.getTotalSpace(), root.getFreeSpace()));
            }
        }

        return diag;
    }
}
