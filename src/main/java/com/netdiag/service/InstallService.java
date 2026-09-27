package com.netdiag.service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.BiConsumer;

/**
 * Serviço responsável por baixar instaladores de software diretamente
 * da internet para a pasta Downloads do usuário atual.
 * Suporta acompanhamento de progresso via callback.
 */
public class InstallService {

    /**
     * Softwares disponíveis para download com metadados.
     */
    public enum Software {
        GOOGLE_CHROME(
                "Google Chrome",
                "🌐",
                "Navegador web rápido e seguro do Google.",
                // Instalador offline completo (Chrome for Testing / Enterprise x64)
                "https://dl.google.com/dl/chrome/install/googlechromestandaloneenterprise64.msi",
                "ChromeEnterprise64.msi"
        ),
        ANYDESK(
                "AnyDesk",
                "🖥️",
                "Acesso remoto leve e de alta velocidade.",
                "https://download.anydesk.com/AnyDesk.exe",
                "AnyDesk.exe"
        ),
        TEAMVIEWER(
                "TeamViewer",
                "🔗",
                "Suporte remoto, reuniões e transferência de arquivos.",
                "https://download.teamviewer.com/download/TeamViewer_Setup.exe",
                "TeamViewerSetup.exe"
        ),
        WINRAR(
                "WinRAR",
                "📦",
                "Compactador e descompactador de arquivos (RAR, ZIP, 7z).",
                "https://www.win-rar.com/fileadmin/winrar-versions/winrar/winrar-x64-701.exe",
                "WinRAR-x64.exe"
        ),
        VLCPLAYER(
                "VLC Media Player",
                "▶️",
                "Reprodutor multimídia universal, suporta todos os formatos.",
                "https://mirror.its.dal.ca/videolan/vlc/3.0.21/win64/vlc-3.0.21-win64.exe",
                "VLC-Setup.exe"
        ),
        LIBREOFFICE(
                "LibreOffice",
                "📝",
                "Suite de escritório gratuita (Writer, Calc, Impress).",
                "https://download.documentfoundation.org/libreoffice/stable/24.8.5/win/x86_64/LibreOffice_24.8.5_Win_x86-64.msi",
                "LibreOffice-Setup.msi"
        ),
        SEVEN_ZIP(
                "7-Zip",
                "🗜️",
                "Compactador de arquivos gratuito e de código aberto.",
                "https://www.7-zip.org/a/7z2408-x64.exe",
                "7zip-Setup.exe"
        ),
        NOTEPADPP(
                "Notepad++",
                "📋",
                "Editor de texto avançado para programadores e técnicos.",
                "https://github.com/notepad-plus-plus/notepad-plus-plus/releases/download/v8.7.4/npp.8.7.4.Installer.x64.exe",
                "NotepadPP-Setup.exe"
        ),
        VOIXIP(
                "VoixIP",
                "📞",
                "Aplicativo interno de telefonia VoIP corporativo.",
                // ⚠️ Altere para a URL real do servidor interno da empresa
                "http://192.168.1.1/instaladores/VoixIP-Setup.exe",
                "VoixIP-Setup.exe"
        );

        public final String displayName;
        public final String icon;
        public final String description;
        public final String downloadUrl;
        public final String fileName;

        Software(String displayName, String icon, String description,
                 String downloadUrl, String fileName) {
            this.displayName = displayName;
            this.icon = icon;
            this.description = description;
            this.downloadUrl = downloadUrl;
            this.fileName = fileName;
        }
    }

    /**
     * Resultado de uma operação de download.
     */
    public record DownloadResult(boolean success, String filePath, String message) {}

    /**
     * Faz o download de um software para a pasta Downloads do usuário.
     *
     * @param software     Software a baixar
     * @param onProgress   Callback chamado com (percentual 0.0-1.0, bytesLidos). Pode ser null.
     * @return             Resultado com status e caminho do arquivo
     */
    public DownloadResult download(Software software, BiConsumer<Double, Long> onProgress) {
        Path destDir = getDownloadsFolder();
        Path destFile = destDir.resolve(software.fileName);

        try {
            Files.createDirectories(destDir);

            URL url = new URL(software.downloadUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(60_000);
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) NetDiagPro/1.0");

            // Segue redirecionamentos manualmente (HTTP → HTTPS)
            int status = conn.getResponseCode();
            if (status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == 307 || status == 308) {
                String newUrl = conn.getHeaderField("Location");
                conn.disconnect();
                conn = (HttpURLConnection) new URL(newUrl).openConnection();
                conn.setConnectTimeout(15_000);
                conn.setReadTimeout(60_000);
                conn.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) NetDiagPro/1.0");
            }

            long totalBytes = conn.getContentLengthLong();

            try (InputStream in = new BufferedInputStream(conn.getInputStream());
                 OutputStream out = new BufferedOutputStream(new FileOutputStream(destFile.toFile()))) {

                byte[] buffer = new byte[8192];
                long downloaded = 0;
                int bytesRead;

                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    downloaded += bytesRead;
                    if (onProgress != null && totalBytes > 0) {
                        double progress = (double) downloaded / totalBytes;
                        onProgress.accept(progress, downloaded);
                    } else if (onProgress != null) {
                        // Tamanho desconhecido — reporta bytes baixados como progresso indeterminado
                        onProgress.accept(-1.0, downloaded);
                    }
                }
            } finally {
                conn.disconnect();
            }

            return new DownloadResult(true, destFile.toAbsolutePath().toString(),
                    "Download concluído: " + destFile.getFileName());

        } catch (Exception e) {
            // Remove arquivo corrompido se existir
            try { Files.deleteIfExists(destFile); } catch (IOException ignored) {}
            return new DownloadResult(false, null,
                    "Erro ao baixar " + software.displayName + ": " + e.getMessage());
        }
    }

    /**
     * Abre o instalador com o método correto para cada tipo de arquivo:
     * - .msi → msiexec /i (Windows Installer)
     * - .exe → Desktop.open() (equivalente a duplo clique)
     */
    public boolean openInstaller(String filePath) {
        try {
            java.io.File file = new java.io.File(filePath);
            if (!file.exists()) return false;

            if (filePath.toLowerCase().endsWith(".msi")) {
                // MSI requer msiexec explicitamente — Desktop.open não funciona para .msi
                new ProcessBuilder("msiexec.exe", "/i", filePath)
                        .inheritIO()
                        .start();
            } else {
                // EXE: Desktop.open() equivale a duplo clique, sem argumentos extras
                java.awt.Desktop.getDesktop().open(file);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Retorna a pasta Downloads do usuário atual.
     */
    public Path getDownloadsFolder() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, "Downloads", "NetDiag-Installers");
    }
}
