package com.netdiag.service;

import com.sun.management.OperatingSystemMXBean;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Serviço de diagnóstico de hardware — CPU e HD.
 * Usa Java Management API + WMIC para dados gerais.
 * Usa LibreHardwareMonitor (download automático) para temperatura real da CPU.
 */
public class HardwareTestService {

    private static final String LHM_ZIP_URL =
            "https://openhardwaremonitor.org/files/openhardwaremonitor-v0.9.6.zip";
    private static final String LHM_EXE_NAME = "OpenHardwareMonitorReport.exe";

    // ─────────────────────────────────────────
    // MODELOS DE RESULTADO
    // ─────────────────────────────────────────

    public static class CpuResult {
        public String modelName        = "N/D";
        public int    logicalCores     = 0;
        public int    physicalCores    = 0;
        public String maxFreqGhz       = "N/D";
        public String currentLoadPct   = "N/D";
        public String temperatureC     = "N/D";
        public List<String> coreTemps  = new ArrayList<>();
        public String architecture     = System.getProperty("os.arch");
        public String stressResult     = "";
        public long   stressDurationMs = 0;
        public List<String> rawLines   = new ArrayList<>();
    }

    public static class DiskResult {
        public String drive         = "";
        public String model         = "N/D";
        public String serialNumber  = "N/D";
        public String mediaType     = "N/D";
        public String sizeGb        = "N/D";
        public String smartStatus   = "N/D";
        public double readSpeedMBs  = 0;
        public double writeSpeedMBs = 0;
        public List<String> rawLines = new ArrayList<>();
    }

    // ─────────────────────────────────────────
    // LibreHardwareMonitor — gerenciamento
    // ─────────────────────────────────────────

    public Path getLhmDir() {
        String appData = System.getenv("APPDATA");
        if (appData == null) appData = System.getProperty("user.home");
        return Paths.get(appData, "NetDiagPro", "lhm");
    }

    public Path getLhmExe() {
        return getLhmDir().resolve(LHM_EXE_NAME);
    }

    public boolean isLhmInstalled() {
        return Files.exists(getLhmExe());
    }

    /**
     * Baixa e extrai o LibreHardwareMonitor.exe do ZIP oficial do GitHub.
     */
    public String downloadLhm(BiConsumer<Double, Long> onProgress) {
        try {
            Path dir = getLhmDir();
            Files.createDirectories(dir);
            Path zipPath = dir.resolve("lhm.zip");

            // Download do ZIP
            URL url = new URL(LHM_ZIP_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(120_000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 NetDiagPro/1.0");

            // Segue redirects manualmente
            int status = conn.getResponseCode();
            while (status == 301 || status == 302 || status == 307 || status == 308) {
                String loc = conn.getHeaderField("Location");
                conn.disconnect();
                conn = (HttpURLConnection) new URL(loc).openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.setConnectTimeout(15_000);
                conn.setReadTimeout(120_000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 NetDiagPro/1.0");
                status = conn.getResponseCode();
            }

            long total = conn.getContentLengthLong();
            try (InputStream in  = new BufferedInputStream(conn.getInputStream(), 65536);
                 OutputStream out = new BufferedOutputStream(new FileOutputStream(zipPath.toFile()), 65536)) {
                byte[] buf = new byte[65536];
                long downloaded = 0;
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                    downloaded += n;
                    if (onProgress != null && total > 0)
                        onProgress.accept((double) downloaded / total, downloaded);
                    else if (onProgress != null)
                        onProgress.accept(-1.0, downloaded);
                }
            } finally {
                conn.disconnect();
            }

            // Extrai apenas LibreHardwareMonitor.exe do ZIP
            try (ZipInputStream zis = new ZipInputStream(
                    new BufferedInputStream(new FileInputStream(zipPath.toFile())))) {
                ZipEntry entry;
                boolean found = false;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.getName().endsWith(LHM_EXE_NAME) && !entry.isDirectory()) {
                        Path dest = dir.resolve(LHM_EXE_NAME);
                        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(dest.toFile()))) {
                            byte[] buf = new byte[65536];
                            int n;
                            while ((n = zis.read(buf)) != -1) out.write(buf, 0, n);
                        }
                        found = true;
                        break;
                    }
                    zis.closeEntry();
                }
                if (!found) return "ERRO: " + LHM_EXE_NAME + " não encontrado no ZIP.";
            }

            Files.deleteIfExists(zipPath);
            return "OK: LibreHardwareMonitor instalado em " + getLhmExe();

        } catch (Exception e) {
            return "ERRO: " + e.getMessage();
        }
    }

    /**
     * Executa o OpenHardwareMonitorReport.exe e captura a saída na stdout.
     * Ele imprime todos os sensores diretamente — sem GUI, sem WMI, sem admin.
     * Retorna as linhas de temperatura encontradas no relatório.
     */
    public List<String> readTemperaturesViaLhm() {
        List<String> temps = new ArrayList<>();
        if (!isLhmInstalled()) {
            temps.add("OpenHardwareMonitorReport não instalado.");
            return temps;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(getLhmExe().toString());
            pb.redirectErrorStream(true);
            Process proc = pb.start();

            // Captura toda a saída
            List<String> allLines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(proc.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    allLines.add(line);
                }
            }
            proc.waitFor();

            // O relatório tem seções como:
            // +- CPU Intel Core i5-... [...]
            //    |-> Temperature
            //        |-> CPU Package: 52,0 °C
            //        |-> CPU Core #0: 48,0 °C
            // Filtramos as linhas que contêm °C ou "Temperature"
            boolean inTempSection = false;
            for (String line : allLines) {
                String trimmed = line.trim();
                if (trimmed.contains("Temperature") && trimmed.contains("|->")) {
                    inTempSection = true;
                    continue;
                }
                if (inTempSection) {
                    if (trimmed.startsWith("|->") && trimmed.contains(":")) {
                        // Linha de sensor: "|-> CPU Package: 52,0 °C"
                        String sensor = trimmed.replaceFirst("^\\|->\\s*", "");
                        temps.add(sensor.trim());
                    } else if (trimmed.startsWith("+") || (!trimmed.startsWith("|") && !trimmed.isEmpty())) {
                        // Nova seção — sai do bloco de temperatura
                        inTempSection = false;
                    }
                }
                // Captura também linhas que contêm °C diretamente
                if (!inTempSection && trimmed.contains("°C") && trimmed.contains(":")) {
                    temps.add(trimmed.replaceFirst("^\\|->\\s*", "").trim());
                }
            }

            if (temps.isEmpty() && !allLines.isEmpty()) {
                // Fallback: retorna todas as linhas com °C de qualquer forma
                for (String line : allLines) {
                    if (line.contains("°C")) temps.add(line.trim());
                }
            }

            if (temps.isEmpty()) {
                temps.add("Nenhum sensor de temperatura encontrado no relatório.");
            }

        } catch (Exception e) {
            temps.add("Erro ao executar sensor: " + e.getMessage());
        }

        return temps;
    }

    /** Formata sensores OHM Report em string legível. */
    public static String formatTemperatures(List<String> rawSensors) {
        if (rawSensors.isEmpty()) return "Nenhum sensor encontrado.";
        StringBuilder sb = new StringBuilder();
        boolean found = false;
        for (String s : rawSensors) {
            // Formato OHM: "CPU Package: 52,0 °C" ou "CPU Core #0: 48 °C"
            // Formato LHM (fallback): "CPU Package=52.0"
            String name, valStr;
            if (s.contains(": ") && s.contains("°C")) {
                int colon = s.lastIndexOf(": ");
                name   = s.substring(0, colon).trim();
                valStr = s.substring(colon + 2).replace("°C", "").replace(",", ".").trim();
            } else if (s.contains("=")) {
                String[] p = s.split("=", 2);
                name   = p[0].trim();
                valStr = p[1].trim().replace(",", ".");
            } else {
                sb.append("  ").append(s).append("\n");
                found = true;
                continue;
            }
            try {
                double val = Double.parseDouble(valStr.trim());
                sb.append(String.format("  %-28s %.1f °C\n", name + ":", val));
                found = true;
            } catch (NumberFormatException ignored) {
                sb.append("  ").append(s).append("\n");
                found = true;
            }
        }
        return found ? sb.toString().trim() : "Sensores lidos, sem dados de temperatura.";
    }

    /**
     * Coleta informações da CPU + temperatura via OHM Report.
     * Bloqueia alguns segundos — chamar sempre em background.
     */
    public CpuResult collectCpuInfoWithTemperature() {
        CpuResult r = collectCpuInfo();
        List<String> sensors = readTemperaturesViaLhm();

        if (sensors.isEmpty() || (sensors.size() == 1 && sensors.get(0).startsWith("Erro"))) {
            r.temperatureC = sensors.isEmpty() ? "Nenhum sensor retornado." : sensors.get(0);
            return r;
        }

        String packageTemp = null;
        List<String> coreList = new ArrayList<>();

        for (String s : sensors) {
            String lower = s.toLowerCase();
            // Extrai temperatura do Package (resumo geral)
            if (lower.contains("package") || lower.contains("cpu package")) {
                String valStr = extractTempValue(s);
                if (valStr != null) packageTemp = valStr + " °C (Package)";
            }
            // Coleta temperaturas por núcleo
            if (lower.contains("core #") || lower.contains("cpu core")) {
                coreList.add(s);
            }
        }

        r.coreTemps = coreList;
        // Se não encontrou Package, usa o primeiro sensor disponível
        if (packageTemp == null && !sensors.isEmpty()) {
            String valStr = extractTempValue(sensors.get(0));
            packageTemp = valStr != null ? valStr + " °C" : sensors.get(0);
        }
        r.temperatureC = packageTemp != null ? packageTemp : formatTemperatures(sensors);
        r.rawLines.addAll(sensors);
        return r;
    }

    /** Extrai o valor numérico de uma linha de sensor ("CPU Package: 52,0 °C" → "52.0") */
    private String extractTempValue(String line) {
        try {
            if (line.contains(": ") && line.contains("°C")) {
                String val = line.substring(line.lastIndexOf(": ") + 2)
                        .replace("°C", "").replace(",", ".").trim();
                return String.format("%.1f", Double.parseDouble(val));
            } else if (line.contains("=")) {
                String val = line.split("=", 2)[1].trim().replace(",", ".");
                return String.format("%.1f", Double.parseDouble(val));
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ─────────────────────────────────────────
    // CPU — informações gerais
    // ─────────────────────────────────────────

    public CpuResult collectCpuInfo() {
        CpuResult r = new CpuResult();
        r.logicalCores = Runtime.getRuntime().availableProcessors();

        try {
            OperatingSystemMXBean osBean =
                    (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            double load = osBean.getCpuLoad() * 100.0;
            r.currentLoadPct = String.format("%.1f%%", load);
        } catch (Exception ignored) {}

        try {
            List<String> lines = runWmic(
                    "cpu get Name,NumberOfCores,MaxClockSpeed,NumberOfLogicalProcessors /format:list");
            r.rawLines.addAll(lines);
            for (String line : lines) {
                if (line.startsWith("Name="))
                    r.modelName = line.substring(5).trim();
                else if (line.startsWith("NumberOfCores="))
                    r.physicalCores = parseInt(line.substring(14).trim());
                else if (line.startsWith("NumberOfLogicalProcessors="))
                    r.logicalCores = parseInt(line.substring(26).trim());
                else if (line.startsWith("MaxClockSpeed=")) {
                    int mhz = parseInt(line.substring(14).trim());
                    r.maxFreqGhz = String.format("%.2f GHz", mhz / 1000.0);
                }
            }
        } catch (Exception e) {
            r.rawLines.add("Erro WMIC CPU: " + e.getMessage());
        }

        // Tenta MSAcpi primeiro (rápido, sem dependência externa)
        try {
            List<String> temps = runWmic(
                    "path MSAcpi_ThermalZoneTemperature get CurrentTemperature /namespace:\\\\root\\wmi /format:list");
            for (String line : temps) {
                if (line.startsWith("CurrentTemperature=")) {
                    int raw = parseInt(line.substring(19).trim());
                    double celsius = (raw / 10.0) - 273.15;
                    if (celsius > 0 && celsius < 150)
                        r.temperatureC = String.format("%.1f °C", celsius);
                }
            }
        } catch (Exception ignored) {}

        if (r.temperatureC.equals("N/D")) {
            r.temperatureC = isLhmInstalled()
                    ? "Clique em '🌡️ Ler Temperatura' para usar o LibreHardwareMonitor"
                    : "Clique em '📥 Instalar Sensor' para habilitar leitura de temperatura";
        }

        return r;
    }

    // ─────────────────────────────────────────
    // CPU — Stress Test
    // ─────────────────────────────────────────

    public CpuResult runCpuStress(int durationSeconds) {
        CpuResult r = collectCpuInfo();
        int cores = Runtime.getRuntime().availableProcessors();
        long durationMs = durationSeconds * 1000L;
        long start = System.currentTimeMillis();

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < cores; i++) {
            Thread t = new Thread(() -> {
                long n = 2;
                while (System.currentTimeMillis() - start < durationMs) isPrime(n++);
            });
            t.setDaemon(true);
            t.start();
            threads.add(t);
        }
        for (Thread t : threads) {
            try { t.join(durationMs + 2000); } catch (InterruptedException ignored) {}
        }

        r.stressDurationMs = System.currentTimeMillis() - start;
        try {
            OperatingSystemMXBean osBean =
                    (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            r.stressResult = String.format(
                    "Stress concluído em %d s usando %d núcleos. Carga atual: %.1f%%",
                    durationSeconds, cores, osBean.getCpuLoad() * 100.0);
        } catch (Exception e) {
            r.stressResult = "Stress de " + durationSeconds + "s concluído em " + cores + " núcleos.";
        }
        return r;
    }

    // ─────────────────────────────────────────
    // HD / DISCO
    // ─────────────────────────────────────────

    public List<DiskResult> collectDiskInfo() {
        List<DiskResult> results = new ArrayList<>();
        try {
            List<String> lines = runWmic(
                    "diskdrive get Model,SerialNumber,Size,MediaType,Caption /format:list");
            DiskResult current = null;
            for (String line : lines) {
                if (line.startsWith("Caption=") && !line.substring(8).isBlank()) {
                    current = new DiskResult();
                    current.drive = line.substring(8).trim();
                    results.add(current);
                }
                if (current == null) continue;
                current.rawLines.add(line);
                if (line.startsWith("Model="))
                    current.model = line.substring(6).trim();
                else if (line.startsWith("SerialNumber="))
                    current.serialNumber = line.substring(13).trim();
                else if (line.startsWith("MediaType="))
                    current.mediaType = detectMediaType(line.substring(10).trim());
                else if (line.startsWith("Size=")) {
                    long bytes = parseLong(line.substring(5).trim());
                    if (bytes > 0) current.sizeGb = String.format("%.0f GB", bytes / 1_073_741_824.0);
                }
            }
        } catch (Exception e) {
            DiskResult err = new DiskResult();
            err.drive = "Erro";
            err.rawLines.add("Erro WMIC: " + e.getMessage());
            results.add(err);
        }

        try {
            List<String> statusLines = runWmic("diskdrive get Status /format:list");
            int idx = 0;
            for (String line : statusLines) {
                if (line.startsWith("Status=") && idx < results.size())
                    results.get(idx++).smartStatus = line.substring(7).trim();
            }
        } catch (Exception ignored) {}

        for (DiskResult d : results) {
            if (d.mediaType.isBlank() || d.mediaType.equals("N/D")) {
                d.mediaType = (d.model.toLowerCase().contains("nvme")
                        || d.drive.toLowerCase().contains("nvme")) ? "NVMe SSD" : "Desconhecido";
            }
        }
        return results;
    }

    public DiskResult runDiskSpeedTest(String driveLetter, int fileSizeMb) {
        DiskResult r = new DiskResult();
        r.drive = driveLetter + ":\\";
        Path tempFile = Paths.get(driveLetter + ":\\", "_netdiag_disktest_tmp.bin");
        byte[] block = new byte[1024 * 1024];
        new Random().nextBytes(block);
        long totalBytes = (long) fileSizeMb * 1024 * 1024;

        try {
            long start = System.nanoTime();
            try (OutputStream out = new BufferedOutputStream(
                    new FileOutputStream(tempFile.toFile()), 8 * 1024 * 1024)) {
                long written = 0;
                while (written < totalBytes) { out.write(block); written += block.length; }
                out.flush();
            }
            r.writeSpeedMBs = fileSizeMb / ((System.nanoTime() - start) / 1_000_000_000.0);
        } catch (Exception e) { r.rawLines.add("Erro escrita: " + e.getMessage()); }

        try {
            long start = System.nanoTime();
            try (InputStream in = new BufferedInputStream(
                    new FileInputStream(tempFile.toFile()), 8 * 1024 * 1024)) {
                byte[] buf = new byte[1024 * 1024];
                //noinspection StatementWithEmptyBody
                while (in.read(buf) != -1) {}
            }
            r.readSpeedMBs = fileSizeMb / ((System.nanoTime() - start) / 1_000_000_000.0);
        } catch (Exception e) { r.rawLines.add("Erro leitura: " + e.getMessage()); }

        try { Files.deleteIfExists(tempFile); } catch (Exception ignored) {}
        return r;
    }

    // ─────────────────────────────────────────
    // UTILITÁRIOS
    // ─────────────────────────────────────────

    private List<String> runWmic(String query) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add("wmic");
        cmd.addAll(Arrays.asList(query.split("\\s+")));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String t = line.trim();
                if (!t.isEmpty()) lines.add(t);
            }
        }
        process.waitFor();
        return lines;
    }

    private String detectMediaType(String raw) {
        if (raw == null || raw.isBlank()) return "N/D";
        String lower = raw.toLowerCase();
        if (lower.contains("solid") || lower.contains("ssd")) return "SSD";
        if (lower.contains("external")) return "HDD Externo";
        if (lower.contains("fixed")) return "HDD";
        if (lower.contains("removable")) return "Removível";
        return raw;
    }

    private boolean isPrime(long n) {
        if (n < 2) return false;
        for (long i = 2; i * i <= n; i++) if (n % i == 0) return false;
        return true;
    }

    private int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }

    private long parseLong(String s) {
        try { return Long.parseLong(s); } catch (Exception e) { return 0L; }
    }
}
