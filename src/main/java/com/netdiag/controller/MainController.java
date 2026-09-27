package com.netdiag.controller;

import com.netdiag.model.DiagnosticLog;
import com.netdiag.model.DiscoveredDevice;
import com.netdiag.model.NetworkProfile;
import com.netdiag.model.PrinterInfo;
import com.netdiag.service.*;
import com.netdiag.service.InstallService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.awt.Desktop;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

public class MainController implements Initializable {

    // Serviços
    private final NetworkService networkService = new NetworkService();
    private final PrinterService printerService = new PrinterService();
    private final SystemInfoService systemInfoService = new SystemInfoService();
    private final DatabaseService dbService = DatabaseService.getInstance();
    private final ReportService reportService = new ReportService();

    // Top Header
    @FXML private Label lblInternetStatus;
    @FXML private Button btnCheckInternet;
    @FXML private Button btnExportReport;

    // Aba 1: Computador & Rede
    @FXML private Label lblHostName;
    @FXML private Label lblOsInfo;
    @FXML private Label lblCpuInfo;
    @FXML private Label lblRamInfo;
    @FXML private ProgressBar progressRam;
    @FXML private Label lblDisksInfo;

    @FXML private TableView<NetworkService.NetworkInterfaceInfo> tableAdapters;
    @FXML private TableColumn<NetworkService.NetworkInterfaceInfo, String> colAdapterName;
    @FXML private TableColumn<NetworkService.NetworkInterfaceInfo, String> colAdapterIp;
    @FXML private TableColumn<NetworkService.NetworkInterfaceInfo, String> colAdapterMac;
    @FXML private TableColumn<NetworkService.NetworkInterfaceInfo, String> colAdapterStatus;
    @FXML private Button btnRefreshAdapters;

    @FXML private TextField txtPingHost;
    @FXML private Button btnExecutePing;
    @FXML private Label lblPingResult;

    // Aba 2: Impressoras
    @FXML private Button btnScanPrinters;
    @FXML private TableView<PrinterInfo> tablePrinters;
    @FXML private TableColumn<PrinterInfo, String> colPrinterName;
    @FXML private TableColumn<PrinterInfo, String> colPrinterPort;
    @FXML private TableColumn<PrinterInfo, String> colPrinterDefault;
    @FXML private TableColumn<PrinterInfo, String> colPrinterStatus;

    @FXML private TextField txtPrinterIp;
    @FXML private Button btnDiagnosePrinter;
    @FXML private TextArea txtPrinterDiagOutput;

    // Aba 3: Varredura de Rede (IP Scanner)
    @FXML private TextField txtSubnetBase;
    @FXML private TextField txtScanStart;
    @FXML private TextField txtScanEnd;
    @FXML private Button btnStartScan;
    @FXML private ProgressBar progressScan;
    @FXML private Label lblScanStatus;

    @FXML private TableView<DiscoveredDevice> tableDiscovered;
    @FXML private TableColumn<DiscoveredDevice, String> colDiscIp;
    @FXML private TableColumn<DiscoveredDevice, String> colDiscHost;
    @FXML private TableColumn<DiscoveredDevice, String> colDiscType;
    @FXML private TableColumn<DiscoveredDevice, String> colDiscPing;
    @FXML private TableColumn<DiscoveredDevice, String> colDiscPorts;
    private final ObservableList<DiscoveredDevice> discoveredList = FXCollections.observableArrayList();

    // Aba 4: Configurar IP Manual
    @FXML private ComboBox<NetworkService.NetworkInterfaceInfo> cbAdapters;
    @FXML private TextField txtStaticIp;
    @FXML private TextField txtSubnetMask;
    @FXML private TextField txtGateway;
    @FXML private TextField txtDns1;
    @FXML private TextField txtDns2;
    @FXML private Button btnApplyStatic;
    @FXML private Button btnApplyDhcp;
    @FXML private Label lblIpStatus;

    @FXML private TextField txtProfileName;
    @FXML private Button btnSaveProfile;
    @FXML private TableView<NetworkProfile> tableProfiles;
    @FXML private TableColumn<NetworkProfile, String> colProfName;
    @FXML private TableColumn<NetworkProfile, String> colProfAdapter;
    @FXML private TableColumn<NetworkProfile, String> colProfIp;
    @FXML private TableColumn<NetworkProfile, String> colProfMask;
    @FXML private TableColumn<NetworkProfile, String> colProfGateway;
    @FXML private Button btnLoadProfile;
    @FXML private Button btnDeleteProfile;

    // Aba 5: Ferramentas de Suporte TI
    @FXML private TextArea txtToolsOutput;

    // Aba 6: Histórico
    @FXML private TableView<DiagnosticLog> tableLogs;
    @FXML private TableColumn<DiagnosticLog, String> colLogTime;
    @FXML private TableColumn<DiagnosticLog, String> colLogCat;
    @FXML private TableColumn<DiagnosticLog, String> colLogTarget;
    @FXML private TableColumn<DiagnosticLog, String> colLogStatus;
    @FXML private TableColumn<DiagnosticLog, String> colLogDetails;
    @FXML private Button btnRefreshLogs;
    @FXML private Button btnClearLogs;

    // Aba 7: Testes de Hardware
    @FXML private Label       lblHwCpuModel;
    @FXML private Label       lblHwCpuCores;
    @FXML private Label       lblHwCpuFreq;
    @FXML private Label       lblHwCpuArch;
    @FXML private Label       lblHwCpuLoad;
    @FXML private ProgressBar progressHwCpuLoad;
    @FXML private Label       lblHwCpuTemp;
    @FXML private TextArea    txtHwCoreTemps;
    @FXML private Button      btnHwInstallLhm;
    @FXML private Button      btnHwReadTemp;
    @FXML private ProgressBar progressHwLhm;
    @FXML private Label       lblHwLhmStatus;
    @FXML private Button      btnHwCpuInfo;

    @FXML private TextField   txtHwStressDuration;
    @FXML private Button      btnHwCpuStress;
    @FXML private Label       lblHwStressStatus;
    @FXML private ProgressBar progressHwStress;
    @FXML private Label       lblHwStressResult;

    @FXML private Button      btnHwDiskInfo;
    @FXML private TableView<HardwareTestService.DiskResult> tableHwDisks;
    @FXML private TableColumn<HardwareTestService.DiskResult, String> colHwDiskDrive;
    @FXML private TableColumn<HardwareTestService.DiskResult, String> colHwDiskType;
    @FXML private TableColumn<HardwareTestService.DiskResult, String> colHwDiskSize;
    @FXML private TableColumn<HardwareTestService.DiskResult, String> colHwDiskSerial;
    @FXML private TableColumn<HardwareTestService.DiskResult, String> colHwDiskSmart;
    @FXML private TableColumn<HardwareTestService.DiskResult, String> colHwDiskRead;
    @FXML private TableColumn<HardwareTestService.DiskResult, String> colHwDiskWrite;

    @FXML private ComboBox<String> cbHwDiskDrive;
    @FXML private ComboBox<String> cbHwTestSize;
    @FXML private Button           btnHwDiskSpeed;
    @FXML private ProgressBar      progressHwDisk;
    @FXML private Label            lblHwDiskWrite;
    @FXML private Label            lblHwDiskRead;
    @FXML private TextArea         txtHwLog;

    private final HardwareTestService hwService = new HardwareTestService();

    // Aba 8: Instalações
    @FXML private Label lblInstallDir;
    @FXML private TextArea txtInstallLog;

    @FXML private ProgressBar progressChrome;
    @FXML private Label lblChromeStatus;
    @FXML private Button btnInstallChrome;
    @FXML private Button btnRunChrome;

    @FXML private ProgressBar progressAnyDesk;
    @FXML private Label lblAnyDeskStatus;
    @FXML private Button btnInstallAnyDesk;
    @FXML private Button btnRunAnyDesk;

    @FXML private ProgressBar progressTeamViewer;
    @FXML private Label lblTeamViewerStatus;
    @FXML private Button btnInstallTeamViewer;
    @FXML private Button btnRunTeamViewer;

    @FXML private ProgressBar progressWinRAR;
    @FXML private Label lblWinRARStatus;
    @FXML private Button btnInstallWinRAR;
    @FXML private Button btnRunWinRAR;

    @FXML private ProgressBar progressVLC;
    @FXML private Label lblVLCStatus;
    @FXML private Button btnInstallVLC;
    @FXML private Button btnRunVLC;

    @FXML private ProgressBar progress7Zip;
    @FXML private Label lbl7ZipStatus;
    @FXML private Button btnInstall7Zip;
    @FXML private Button btnRun7Zip;

    @FXML private ProgressBar progressNotepadPP;
    @FXML private Label lblNotepadPPStatus;
    @FXML private Button btnInstallNotepadPP;
    @FXML private Button btnRunNotepadPP;

    @FXML private ProgressBar progressVoixIP;
    @FXML private Label lblVoixIPStatus;
    @FXML private Button btnInstallVoixIP;
    @FXML private Button btnRunVoixIP;

    private final InstallService installService = new InstallService();

    // Bottom
    @FXML private Label lblStatusBar;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initTables();
        handleRefreshSystemInfo(null);
        handleRefreshAdapters(null);
        handleCheckInternet(null);
        handleRefreshLogs(null);
        loadProfilesFromDb();
        // Mostra o caminho da pasta de instaladores
        lblInstallDir.setText("Pasta de destino: " + installService.getDownloadsFolder().toAbsolutePath());

        // Verifica se LHM já está instalado e habilita botão de leitura
        if (hwService.isLhmInstalled()) {
            btnHwInstallLhm.setText("✅ Sensor Instalado (LHM)");
            btnHwInstallLhm.setDisable(true);
            btnHwReadTemp.setDisable(false);
        }
    }

    private void initTables() {
        // Tabela de Placas de Rede
        colAdapterName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().displayName));
        colAdapterIp.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().ipAddress.isEmpty() ? "Não atribuído" : c.getValue().ipAddress));
        colAdapterMac.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().macAddress));
        colAdapterStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isUp ? "Ativa" : "Inativa"));

        // Tabela de Impressoras
        colPrinterName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colPrinterPort.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPortOrIp()));
        colPrinterDefault.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isDefault() ? "SIM" : "NÃO"));
        colPrinterStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));

        // Tabela de Dispositivos Descobertos no Scanner
        tableDiscovered.setItems(discoveredList);
        colDiscIp.setCellValueFactory(new PropertyValueFactory<>("ipAddress"));
        colDiscHost.setCellValueFactory(new PropertyValueFactory<>("hostname"));
        colDiscType.setCellValueFactory(new PropertyValueFactory<>("deviceType"));
        colDiscPing.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPingMs() + " ms"));
        colDiscPorts.setCellValueFactory(new PropertyValueFactory<>("openPorts"));

        // Tabela de Perfis de Rede
        colProfName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colProfAdapter.setCellValueFactory(new PropertyValueFactory<>("adapterName"));
        colProfIp.setCellValueFactory(new PropertyValueFactory<>("ipAddress"));
        colProfMask.setCellValueFactory(new PropertyValueFactory<>("subnetMask"));
        colProfGateway.setCellValueFactory(new PropertyValueFactory<>("gateway"));

        // Tabela de Histórico
        colLogTime.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        colLogCat.setCellValueFactory(new PropertyValueFactory<>("category"));
        colLogTarget.setCellValueFactory(new PropertyValueFactory<>("target"));
        colLogStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colLogDetails.setCellValueFactory(new PropertyValueFactory<>("details"));

        // Tabela de Discos (Hardware)
        colHwDiskDrive.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().model.isBlank() ? c.getValue().drive : c.getValue().model));
        colHwDiskType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().mediaType));
        colHwDiskSize.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().sizeGb));
        colHwDiskSerial.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().serialNumber));
        colHwDiskSmart.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().smartStatus));
        colHwDiskRead.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().readSpeedMBs > 0 ? String.format("%.1f MB/s", c.getValue().readSpeedMBs) : "-"));
        colHwDiskWrite.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().writeSpeedMBs > 0 ? String.format("%.1f MB/s", c.getValue().writeSpeedMBs) : "-"));

        // Popula ComboBox de drives com as raízes do sistema
        java.io.File[] roots = java.io.File.listRoots();
        if (roots != null) {
            for (java.io.File root : roots) {
                String letter = root.getAbsolutePath().replace("\\", "").replace(":", "");
                if (!letter.isBlank()) cbHwDiskDrive.getItems().add(letter);
            }
        }
        if (!cbHwDiskDrive.getItems().isEmpty()) cbHwDiskDrive.getSelectionModel().select(0);
        cbHwTestSize.getSelectionModel().select(0);
    }

    // ==========================================
    // TOP: TESTE DE INTERNET & RELATÓRIO
    // ==========================================
    @FXML
    public void handleCheckInternet(ActionEvent event) {
        lblInternetStatus.setText("TESTANDO CONEXÃO...");
        lblInternetStatus.getStyleClass().removeAll("badge-online", "badge-offline");

        CompletableFuture.supplyAsync(networkService::checkInternetConnection)
                .thenAccept(hasInternet -> Platform.runLater(() -> {
                    if (hasInternet) {
                        lblInternetStatus.setText("● INTERNET ATIVA (ONLINE)");
                        lblInternetStatus.getStyleClass().add("badge-online");
                        setStatusBar("Conexão com a Internet verificada com sucesso.");
                    } else {
                        lblInternetStatus.setText("● OFFLINE / REDE LOCAL");
                        lblInternetStatus.getStyleClass().add("badge-offline");
                        setStatusBar("Sem saída para a Internet. Modo Local / Intranet ativado.");
                    }
                }));
    }

    @FXML
    public void handleExportReport(ActionEvent event) {
        setStatusBar("Gerando relatório técnico...");
        CompletableFuture.runAsync(() -> {
            try {
                var comp = systemInfoService.collectDiagnostics();
                var adapters = networkService.getNetworkAdapters();
                var printers = printerService.getInstalledPrinters();
                var profiles = dbService.getAllProfiles();
                var logs = dbService.getRecentLogs(50);

                File file = reportService.generateHtmlReport(comp, adapters, printers, profiles, logs);
                Platform.runLater(() -> {
                    setStatusBar("Relatório HTML gerado em: " + file.getAbsolutePath());
                    logToDb("Relatório", "Arquivo", "OK", "Exportado para " + file.getName());
                    try {
                        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                            Desktop.getDesktop().open(file);
                        }
                    } catch (Exception ignored) {
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> setStatusBar("Erro ao exportar relatório: " + e.getMessage()));
            }
        });
    }

    // ABA 1: COMPUTADOR E REDE
    @FXML
    public void handleRefreshSystemInfo(ActionEvent event) {
        CompletableFuture.supplyAsync(systemInfoService::collectDiagnostics)
                .thenAccept(diag -> Platform.runLater(() -> {
                    lblHostName.setText(diag.hostname + " (" + diag.userName + ")");
                    lblOsInfo.setText(diag.osName + " [" + diag.osArch + "]");
                    lblCpuInfo.setText(diag.availableProcessors + " núcleos lógicos");

                    if (diag.totalRamBytes > 0) {
                        long totalGb = diag.totalRamBytes / (1024 * 1024 * 1024);
                        long freeGb = diag.freeRamBytes / (1024 * 1024 * 1024);
                        long usedGb = totalGb - freeGb;
                        double percent = (double) usedGb / totalGb;
                        progressRam.setProgress(percent);
                        lblRamInfo.setText(String.format("%d GB Usados de %d GB (%.0f%%) | %d GB Livres", usedGb, totalGb, percent * 100, freeGb));
                    }

                    StringBuilder sbDisks = new StringBuilder();
                    for (SystemInfoService.DiskInfo disk : diag.disks) {
                        sbDisks.append(disk.toString()).append("\n");
                    }
                    lblDisksInfo.setText(sbDisks.toString().trim());
                    setStatusBar("Informações da máquina atualizadas.");
                }));
    }

    @FXML
    public void handleRefreshAdapters(ActionEvent event) {
        CompletableFuture.supplyAsync(networkService::getNetworkAdapters)
                .thenAccept(adapters -> Platform.runLater(() -> {
                    ObservableList<NetworkService.NetworkInterfaceInfo> obs = FXCollections.observableArrayList(adapters);
                    tableAdapters.setItems(obs);
                    cbAdapters.setItems(obs);
                    if (!obs.isEmpty()) {
                        cbAdapters.getSelectionModel().select(0);
                        // Sugere base de IP a partir da primeira placa que tiver IP
                        for (var ad : obs) {
                            if (!ad.ipAddress.isEmpty()) {
                                String[] parts = ad.ipAddress.split("\\.");
                                if (parts.length == 4) {
                                    txtSubnetBase.setText(parts[0] + "." + parts[1] + "." + parts[2]);
                                    break;
                                }
                            }
                        }
                    }
                }));
    }

    @FXML
    public void handleExecutePing(ActionEvent event) {
        String host = txtPingHost.getText();
        if (host == null || host.trim().isEmpty()) {
            lblPingResult.setText("Informe um IP ou domínio!");
            lblPingResult.setStyle("-fx-text-fill: #dc2626;");
            return;
        }

        lblPingResult.setText("Disparando ping...");
        lblPingResult.setStyle("-fx-text-fill: #2563eb;");
        btnExecutePing.setDisable(true);

        CompletableFuture.supplyAsync(() -> networkService.ping(host.trim(), 2000))
                .thenAccept(result -> Platform.runLater(() -> {
                    btnExecutePing.setDisable(false);
                    if (result.reachable) {
                        lblPingResult.setText("Sucesso: " + result.message);
                        lblPingResult.setStyle("-fx-text-fill: #059669;");
                        logToDb("Rede (Ping)", host.trim(), "ONLINE", result.message);
                    } else {
                        lblPingResult.setText("Falha: " + result.message);
                        lblPingResult.setStyle("-fx-text-fill: #dc2626;");
                        logToDb("Rede (Ping)", host.trim(), "OFFLINE", result.message);
                    }
                }));
    }

    // ==========================================
    // ABA 2: IMPRESSORAS
    // ==========================================
    @FXML
    public void handleScanPrinters(ActionEvent event) {
        btnScanPrinters.setDisable(true);
        setStatusBar("Buscando impressoras instaladas no Windows...");

        CompletableFuture.supplyAsync(printerService::getInstalledPrinters)
                .thenAccept(printers -> Platform.runLater(() -> {
                    btnScanPrinters.setDisable(false);
                    tablePrinters.setItems(FXCollections.observableArrayList(printers));
                    setStatusBar(printers.size() + " impressora(s) encontrada(s).");
                    logToDb("Impressoras", "Local", "OK", "Varredura concluiu: " + printers.size() + " impressora(s)");
                }));
    }

    @FXML
    public void handleDiagnoseNetworkPrinter(ActionEvent event) {
        String ip = txtPrinterIp.getText();
        if (ip == null || ip.trim().isEmpty()) {
            txtPrinterDiagOutput.setText("Por favor, digite o IP da impressora de rede.");
            return;
        }

        btnDiagnosePrinter.setDisable(true);
        txtPrinterDiagOutput.setText("Iniciando testes de rede na impressora " + ip + "...\nTestando ICMP, portas RAW 9100, LPD 515 e HTTP...");

        CompletableFuture.supplyAsync(() -> printerService.diagnoseNetworkPrinter(ip.trim()))
                .thenAccept(diag -> Platform.runLater(() -> {
                    btnDiagnosePrinter.setDisable(false);
                    txtPrinterDiagOutput.setText("=== DIAGNÓSTICO DA IMPRESSORA [" + ip.trim() + "] ===\n\n" + diag.details);

                    String status = diag.pingOk ? (diag.rawPort9100Ok ? "PRONTA" : "ALERTA (Porta 9100 Fechada)") : "OFFLINE";
                    logToDb("Impressora IP", ip.trim(), status,
                            String.format("Ping: %b | Porta 9100: %b | HTTP: %b", diag.pingOk, diag.rawPort9100Ok, diag.httpPort80Ok));
                }));
    }

    // ==========================================
    // ABA 3: VARREDURA DE REDE (IP SCANNER)
    // ==========================================
    @FXML
    public void handleStartNetworkScan(ActionEvent event) {
        String base = txtSubnetBase.getText().trim();
        int startHost = 1;
        int endHost = 254;
        try {
            startHost = Integer.parseInt(txtScanStart.getText().trim());
            endHost = Integer.parseInt(txtScanEnd.getText().trim());
        } catch (Exception ignored) {
        }

        discoveredList.clear();
        btnStartScan.setDisable(true);
        progressScan.setProgress(0.0);
        lblScanStatus.setText("Varrendo sub-rede " + base + " (" + startHost + " a " + endHost + ")...");

        int finalStartHost = startHost;
        int finalEndHost = endHost;
        CompletableFuture.runAsync(() -> {
            networkService.scanNetworkRange(
                    base,
                    finalStartHost,
                    finalEndHost,
                    device -> Platform.runLater(() -> discoveredList.add(device)),
                    progress -> Platform.runLater(() -> progressScan.setProgress(progress))
            );
            Platform.runLater(() -> {
                btnStartScan.setDisable(false);
                progressScan.setProgress(1.0);
                lblScanStatus.setText("Varredura concluída! " + discoveredList.size() + " dispositivo(s) encontrado(s).");
                logToDb("Scanner de Rede", base + ".0/24", "CONCLUÍDO", discoveredList.size() + " hosts descobertos");
            });
        });
    }

    // ==========================================
    // ABA 4: CONFIGURAÇÃO DE IP MANUAL / DHCP
    // ==========================================
    @FXML
    public void handleApplyStaticIp(ActionEvent event) {
        NetworkService.NetworkInterfaceInfo selectedAdapter = cbAdapters.getSelectionModel().getSelectedItem();
        if (selectedAdapter == null) {
            lblIpStatus.setText("Selecione uma placa de rede!");
            lblIpStatus.setStyle("-fx-text-fill: #dc2626;");
            return;
        }

        String ip = txtStaticIp.getText();
        String mask = txtSubnetMask.getText();
        String gw = txtGateway.getText();
        String dns1 = txtDns1.getText();
        String dns2 = txtDns2.getText();

        if (ip == null || ip.trim().isEmpty() || mask == null || mask.trim().isEmpty()) {
            lblIpStatus.setText("Preencha ao menos o Endereço IP e a Máscara!");
            lblIpStatus.setStyle("-fx-text-fill: #dc2626;");
            return;
        }

        btnApplyStatic.setDisable(true);
        lblIpStatus.setText("Aplicando endereço IP na interface " + selectedAdapter.name + "...");
        lblIpStatus.setStyle("-fx-text-fill: #2563eb;");

        CompletableFuture.supplyAsync(() -> networkService.applyStaticIp(selectedAdapter.name, ip.trim(), mask.trim(), gw, dns1, dns2))
                .thenAccept(output -> Platform.runLater(() -> {
                    btnApplyStatic.setDisable(false);
                    lblIpStatus.setText(output);
                    lblIpStatus.setStyle("-fx-text-fill: #059669;");
                    handleRefreshAdapters(null);
                    logToDb("Configuração IP", selectedAdapter.name, "ESTÁTICO", "IP: " + ip + " | GW: " + gw);
                }));
    }

    @FXML
    public void handleApplyDhcp(ActionEvent event) {
        NetworkService.NetworkInterfaceInfo selectedAdapter = cbAdapters.getSelectionModel().getSelectedItem();
        if (selectedAdapter == null) {
            lblIpStatus.setText("Selecione uma placa de rede!");
            lblIpStatus.setStyle("-fx-text-fill: #dc2626;");
            return;
        }

        btnApplyDhcp.setDisable(true);
        lblIpStatus.setText("Restaurando DHCP para " + selectedAdapter.name + "...");
        lblIpStatus.setStyle("-fx-text-fill: #2563eb;");

        CompletableFuture.supplyAsync(() -> networkService.applyDhcp(selectedAdapter.name))
                .thenAccept(output -> Platform.runLater(() -> {
                    btnApplyDhcp.setDisable(false);
                    lblIpStatus.setText(output);
                    lblIpStatus.setStyle("-fx-text-fill: #059669;");
                    handleRefreshAdapters(null);
                    logToDb("Configuração IP", selectedAdapter.name, "DHCP", "Restaurado para IP automático");
                }));
    }

    @FXML
    public void handleSaveProfile(ActionEvent event) {
        String name = txtProfileName.getText();
        if (name == null || name.trim().isEmpty()) {
            setStatusBar("Digite um nome para salvar o perfil de rede.");
            return;
        }

        NetworkService.NetworkInterfaceInfo selectedAdapter = cbAdapters.getSelectionModel().getSelectedItem();
        String adapter = selectedAdapter != null ? selectedAdapter.displayName : "Ethernet";

        NetworkProfile profile = new NetworkProfile(
                0,
                name.trim(),
                adapter,
                txtStaticIp.getText() != null ? txtStaticIp.getText().trim() : "",
                txtSubnetMask.getText() != null ? txtSubnetMask.getText().trim() : "",
                txtGateway.getText() != null ? txtGateway.getText().trim() : "",
                txtDns1.getText() != null ? txtDns1.getText().trim() : "",
                txtDns2.getText() != null ? txtDns2.getText().trim() : ""
        );

        dbService.saveProfile(profile);
        txtProfileName.clear();
        loadProfilesFromDb();
        setStatusBar("Perfil '" + name + "' salvo no banco SQLite!");
    }

    private void loadProfilesFromDb() {
        List<NetworkProfile> profiles = dbService.getAllProfiles();
        tableProfiles.setItems(FXCollections.observableArrayList(profiles));
    }

    @FXML
    public void handleLoadProfile(ActionEvent event) {
        NetworkProfile selected = tableProfiles.getSelectionModel().getSelectedItem();
        if (selected != null) {
            txtStaticIp.setText(selected.getIpAddress());
            txtSubnetMask.setText(selected.getSubnetMask());
            txtGateway.setText(selected.getGateway());
            txtDns1.setText(selected.getDnsPrimary());
            txtDns2.setText(selected.getDnsSecondary());
            setStatusBar("Perfil '" + selected.getName() + "' carregado nos campos.");
        }
    }

    @FXML
    public void handleDeleteProfile(ActionEvent event) {
        NetworkProfile selected = tableProfiles.getSelectionModel().getSelectedItem();
        if (selected != null) {
            dbService.deleteProfile(selected.getId());
            loadProfilesFromDb();
            setStatusBar("Perfil excluído com sucesso.");
        }
    }

    // ==========================================
    // ABA 5: FERRAMENTAS DE SUPORTE TI
    // ==========================================
    @FXML
    public void handleRestartSpooler(ActionEvent event) {
        txtToolsOutput.setText("Reiniciando serviço do Spooler de Impressão do Windows...\n");
        CompletableFuture.supplyAsync(printerService::restartSpooler)
                .thenAccept(out -> Platform.runLater(() -> {
                    txtToolsOutput.appendText(out + "\n\n");
                    logToDb("Ferramenta TI", "Spooler", "EXECUTADO", "Reinicio do serviço");
                }));
    }

    @FXML
    public void handleClearPrintQueue(ActionEvent event) {
        txtToolsOutput.setText("Limpando fila de impressão travada do Windows...\n");
        CompletableFuture.supplyAsync(printerService::clearPrintQueue)
                .thenAccept(out -> Platform.runLater(() -> {
                    txtToolsOutput.appendText(out + "\n\n");
                    logToDb("Ferramenta TI", "Fila Impressão", "EXECUTADO", "Limpeza de arquivos travados");
                }));
    }

    @FXML
    public void handleFlushDns(ActionEvent event) {
        txtToolsOutput.setText("Executando limpeza de cache DNS (ipconfig /flushdns)...\n");
        CompletableFuture.supplyAsync(networkService::flushDns)
                .thenAccept(out -> Platform.runLater(() -> {
                    txtToolsOutput.appendText(out + "\n\n");
                    logToDb("Ferramenta TI", "DNS", "EXECUTADO", "Flush DNS");
                }));
    }

    @FXML
    public void handleRenewIp(ActionEvent event) {
        txtToolsOutput.setText("Renovando concessão de IP DHCP (ipconfig /renew)...\n");
        CompletableFuture.supplyAsync(networkService::renewIp)
                .thenAccept(out -> Platform.runLater(() -> {
                    txtToolsOutput.appendText(out + "\n\n");
                    handleRefreshAdapters(null);
                    logToDb("Ferramenta TI", "Rede", "EXECUTADO", "Renew IP");
                }));
    }

    // ==========================================
    // ABA 6: HISTÓRICO & BANCO
    // ==========================================
    @FXML
    public void handleRefreshLogs(ActionEvent event) {
        List<DiagnosticLog> logs = dbService.getRecentLogs(100);
        tableLogs.setItems(FXCollections.observableArrayList(logs));
        setStatusBar("Histórico do banco SQLite atualizado.");
    }

    @FXML
    public void handleClearLogs(ActionEvent event) {
        dbService.clearLogs();
        tableLogs.getItems().clear();
        setStatusBar("Registros do histórico foram limpos.");
    }

    private void logToDb(String category, String target, String status, String details) {
        DiagnosticLog log = new DiagnosticLog(0, null, category, target, status, details);
        dbService.saveLog(log);
        Platform.runLater(this::loadProfilesFromDb);
    }

    private void setStatusBar(String message) {
        Platform.runLater(() -> lblStatusBar.setText(message + " | SQLite Ativo."));
    }

    // ==========================================
    // ABA 7: TESTES DE HARDWARE
    // ==========================================

    @FXML
    public void handleHwCpuInfo(ActionEvent e) {
        btnHwCpuInfo.setDisable(true);
        lblHwCpuModel.setText("Coletando...");
        setStatusBar("Coletando informações da CPU via WMIC...");

        CompletableFuture.supplyAsync(hwService::collectCpuInfo)
                .thenAccept(r -> Platform.runLater(() -> {
                    btnHwCpuInfo.setDisable(false);
                    lblHwCpuModel.setText(r.modelName);
                    lblHwCpuCores.setText(r.physicalCores + " físicos / " + r.logicalCores + " lógicos");
                    lblHwCpuFreq.setText(r.maxFreqGhz);
                    lblHwCpuArch.setText(r.architecture);
                    lblHwCpuTemp.setText(r.temperatureC);

                    try {
                        double load = Double.parseDouble(r.currentLoadPct.replace("%", "").replace(",", ".")) / 100.0;
                        progressHwCpuLoad.setProgress(load);
                    } catch (Exception ignored) { progressHwCpuLoad.setProgress(0); }
                    lblHwCpuLoad.setText(r.currentLoadPct);

                    appendHwLog("=== CPU ===");
                    appendHwLog("Modelo: " + r.modelName);
                    appendHwLog("Núcleos: " + r.physicalCores + " físicos / " + r.logicalCores + " lógicos");
                    appendHwLog("Frequência máx: " + r.maxFreqGhz);
                    appendHwLog("Uso atual: " + r.currentLoadPct);
                    appendHwLog("Temperatura: " + r.temperatureC);
                    setStatusBar("Informações da CPU coletadas.");
                    logToDb("Hardware", "CPU", "OK", r.modelName + " | " + r.logicalCores + " cores");
                }));
    }

    @FXML
    public void handleHwInstallLhm(ActionEvent e) {
        btnHwInstallLhm.setDisable(true);
        progressHwLhm.setVisible(true);
        progressHwLhm.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        lblHwLhmStatus.setText("Baixando LibreHardwareMonitor...");
        lblHwLhmStatus.setStyle("-fx-text-fill: #2563eb;");
        appendHwLog("=== INSTALAÇÃO DO SENSOR (LibreHardwareMonitor) ===");
        appendHwLog("URL: https://github.com/LibreHardwareMonitor/...");

        CompletableFuture.supplyAsync(() ->
                hwService.downloadLhm((pct, bytes) -> Platform.runLater(() -> {
                    if (pct >= 0) {
                        progressHwLhm.setProgress(pct);
                        lblHwLhmStatus.setText(String.format("%.0f%% — %.1f MB", pct * 100, bytes / 1_048_576.0));
                    } else {
                        progressHwLhm.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
                        lblHwLhmStatus.setText(String.format("%.1f MB baixados...", bytes / 1_048_576.0));
                    }
                }))
        ).thenAccept(result -> Platform.runLater(() -> {
            progressHwLhm.setProgress(1.0);
            if (result.startsWith("OK")) {
                lblHwLhmStatus.setText("✅ LibreHardwareMonitor instalado com sucesso!");
                lblHwLhmStatus.setStyle("-fx-text-fill: #059669;");
                btnHwInstallLhm.setText("✅ Sensor Instalado (LHM)");
                btnHwReadTemp.setDisable(false);
                appendHwLog("✅ " + result);
                logToDb("Hardware", "LHM", "INSTALADO", result);
            } else {
                lblHwLhmStatus.setText("❌ " + result);
                lblHwLhmStatus.setStyle("-fx-text-fill: #dc2626;");
                btnHwInstallLhm.setDisable(false);
                appendHwLog("❌ " + result);
            }
        }));
    }

    @FXML
    public void handleHwReadTemp(ActionEvent e) {
        btnHwReadTemp.setDisable(true);
        progressHwLhm.setVisible(true);
        progressHwLhm.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        lblHwLhmStatus.setText("Iniciando LibreHardwareMonitor e lendo sensores (~4s)...");
        lblHwLhmStatus.setStyle("-fx-text-fill: #2563eb;");
        appendHwLog("=== LEITURA DE TEMPERATURA (LHM) ===");

        CompletableFuture.supplyAsync(hwService::collectCpuInfoWithTemperature)
                .thenAccept(r -> Platform.runLater(() -> {
                    btnHwReadTemp.setDisable(false);
                    progressHwLhm.setProgress(1.0);

                    lblHwCpuTemp.setText(r.temperatureC);

                    if (!r.coreTemps.isEmpty()) {
                        txtHwCoreTemps.setVisible(true);
                        StringBuilder sb = new StringBuilder();
                        for (String ct : r.coreTemps) {
                            String[] parts = ct.split("=", 2);
                            if (parts.length == 2) {
                                try {
                                    double val = Double.parseDouble(parts[1].trim().replace(",", "."));
                                    sb.append(String.format("%-20s %.1f °C\n", parts[0].trim() + ":", val));
                                } catch (Exception ignored) {
                                    sb.append(ct).append("\n");
                                }
                            }
                        }
                        txtHwCoreTemps.setText(sb.toString().trim());
                    }

                    lblHwLhmStatus.setText("✅ Temperatura lida com sucesso.");
                    lblHwLhmStatus.setStyle("-fx-text-fill: #059669;");

                    appendHwLog("Temperatura CPU: " + r.temperatureC);
                    r.coreTemps.forEach(this::appendHwLog);
                    setStatusBar("Temperaturas lidas via LibreHardwareMonitor.");
                    logToDb("Hardware", "Temperatura CPU", "OK", r.temperatureC);
                }));
    }

    @FXML
    public void handleHwCpuStress(ActionEvent e) {
        int seconds;
        try {
            seconds = Integer.parseInt(txtHwStressDuration.getText().trim());
            if (seconds < 1 || seconds > 300) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            lblHwStressStatus.setText("⚠ Informe entre 1 e 300 segundos.");
            lblHwStressStatus.setStyle("-fx-text-fill: #dc2626;");
            return;
        }

        btnHwCpuStress.setDisable(true);
        progressHwStress.setVisible(true);
        progressHwStress.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        lblHwStressStatus.setText("🔥 Stress em andamento (" + seconds + "s)...");
        lblHwStressStatus.setStyle("-fx-text-fill: #d97706;");
        lblHwStressResult.setText("");
        appendHwLog("=== STRESS TEST CPU (" + seconds + "s) — INICIADO ===");

        int finalSeconds = seconds;
        CompletableFuture.supplyAsync(() -> hwService.runCpuStress(finalSeconds))
                .thenAccept(r -> Platform.runLater(() -> {
                    btnHwCpuStress.setDisable(false);
                    progressHwStress.setProgress(1.0);
                    lblHwStressStatus.setText("✅ Concluído!");
                    lblHwStressStatus.setStyle("-fx-text-fill: #059669;");
                    lblHwStressResult.setText(r.stressResult);
                    appendHwLog(r.stressResult);
                    appendHwLog("Temperatura após stress: " + r.temperatureC);
                    setStatusBar("Stress test de CPU concluído.");
                    logToDb("Hardware", "CPU Stress", "OK", r.stressResult);
                }));
    }

    @FXML
    public void handleHwDiskInfo(ActionEvent e) {
        btnHwDiskInfo.setDisable(true);
        setStatusBar("Escaneando discos físicos via WMIC...");
        appendHwLog("=== DISCOS FÍSICOS ===");

        CompletableFuture.supplyAsync(hwService::collectDiskInfo)
                .thenAccept(disks -> Platform.runLater(() -> {
                    btnHwDiskInfo.setDisable(false);
                    tableHwDisks.setItems(FXCollections.observableArrayList(disks));
                    for (var d : disks) {
                        appendHwLog(String.format("Disco: %s | Tipo: %s | Tamanho: %s | SMART: %s",
                                d.model, d.mediaType, d.sizeGb, d.smartStatus));
                    }
                    setStatusBar(disks.size() + " disco(s) encontrado(s).");
                    logToDb("Hardware", "Discos", "OK", disks.size() + " disco(s) detectado(s)");
                }));
    }

    @FXML
    public void handleHwDiskSpeed(ActionEvent e) {
        String drive = cbHwDiskDrive.getSelectionModel().getSelectedItem();
        if (drive == null || drive.isBlank()) {
            lblHwDiskRead.setText("Selecione uma unidade.");
            return;
        }

        String sizeStr = cbHwTestSize.getSelectionModel().getSelectedItem();
        int sizeMb = 128;
        if (sizeStr != null) {
            try { sizeMb = Integer.parseInt(sizeStr.split(" ")[0]); }
            catch (Exception ignored) {}
        }

        btnHwDiskSpeed.setDisable(true);
        progressHwDisk.setVisible(true);
        progressHwDisk.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        lblHwDiskWrite.setText("Testando...");
        lblHwDiskRead.setText("Testando...");
        appendHwLog("=== TESTE DE VELOCIDADE — " + drive + ":\\ (" + sizeMb + " MB) ===");
        setStatusBar("Medindo velocidade do disco " + drive + ":\\ ...");

        int finalSizeMb = sizeMb;
        CompletableFuture.supplyAsync(() -> hwService.runDiskSpeedTest(drive, finalSizeMb))
                .thenAccept(r -> Platform.runLater(() -> {
                    btnHwDiskSpeed.setDisable(false);
                    progressHwDisk.setProgress(1.0);

                    String write = String.format("%.1f MB/s", r.writeSpeedMBs);
                    String read  = String.format("%.1f MB/s", r.readSpeedMBs);
                    lblHwDiskWrite.setText(write);
                    lblHwDiskRead.setText(read);

                    appendHwLog("Escrita sequencial: " + write);
                    appendHwLog("Leitura sequencial: " + read);
                    if (!r.rawLines.isEmpty()) r.rawLines.forEach(this::appendHwLog);

                    // Atualiza também a tabela se o disco estiver listado
                    tableHwDisks.getItems().stream()
                            .filter(d -> d.drive.toUpperCase().startsWith(drive.toUpperCase()))
                            .findFirst()
                            .ifPresent(d -> {
                                d.readSpeedMBs  = r.readSpeedMBs;
                                d.writeSpeedMBs = r.writeSpeedMBs;
                                tableHwDisks.refresh();
                            });

                    setStatusBar("Teste de velocidade do disco " + drive + ":\\ concluído.");
                    logToDb("Hardware", "Disco " + drive + ":\\", "OK",
                            "Escrita: " + write + " | Leitura: " + read);
                }));
    }

    private void appendHwLog(String msg) {
        Platform.runLater(() -> {
            if (txtHwLog != null) txtHwLog.appendText(msg + "\n");
        });
    }

    // ==========================================
    // ABA 8: INSTALAÇÕES
    // ==========================================

    /** Inicia o download de um software em background, atualizando a UI. */
    private void startDownload(InstallService.Software sw,
                               ProgressBar progressBar,
                               Label statusLabel,
                               Button btnDownload,
                               Button btnRun) {
        btnDownload.setDisable(true);
        progressBar.setVisible(true);
        progressBar.setProgress(0.0);
        statusLabel.setText("Baixando " + sw.displayName + "...");
        statusLabel.setStyle("-fx-text-fill: #2563eb;");
        appendInstallLog("[" + sw.displayName + "] Iniciando download de: " + sw.downloadUrl);

        CompletableFuture.supplyAsync(() ->
                installService.download(sw, (pct, bytes) -> Platform.runLater(() -> {
                    if (pct >= 0) {
                        progressBar.setProgress(pct);
                        statusLabel.setText(String.format("%.0f%% — %.1f MB baixados", pct * 100, bytes / 1_048_576.0));
                    } else {
                        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
                        statusLabel.setText(String.format("%.1f MB baixados...", bytes / 1_048_576.0));
                    }
                }))
        ).thenAccept(result -> Platform.runLater(() -> {
            progressBar.setProgress(1.0);
            if (result.success()) {
                statusLabel.setText("✅ Pronto! Clique em Instalar para executar.");
                statusLabel.setStyle("-fx-text-fill: #059669;");
                btnRun.setDisable(false);
                btnRun.setUserData(result.filePath());
                appendInstallLog("[" + sw.displayName + "] ✅ Salvo em: " + result.filePath());
                logToDb("Instalação", sw.displayName, "BAIXADO", result.filePath());
            } else {
                statusLabel.setText("❌ " + result.message());
                statusLabel.setStyle("-fx-text-fill: #dc2626;");
                btnDownload.setDisable(false);
                progressBar.setVisible(false);
                appendInstallLog("[" + sw.displayName + "] ❌ " + result.message());
            }
        }));
    }

    /** Executa o instalador previamente baixado. */
    private void runInstaller(Button btnRun, String softwareName) {
        Object path = btnRun.getUserData();
        if (path == null) return;
        boolean ok = installService.openInstaller(path.toString());
        appendInstallLog("[" + softwareName + "] " + (ok ? "▶ Instalador iniciado." : "❌ Falha ao abrir instalador."));
    }

    private void appendInstallLog(String msg) {
        Platform.runLater(() -> {
            if (txtInstallLog != null) {
                txtInstallLog.appendText(msg + "\n");
            }
        });
    }

    @FXML public void handleInstallChrome(ActionEvent e) {
        startDownload(InstallService.Software.GOOGLE_CHROME, progressChrome, lblChromeStatus, btnInstallChrome, btnRunChrome);
    }
    @FXML public void handleRunChrome(ActionEvent e) { runInstaller(btnRunChrome, "Google Chrome"); }

    @FXML public void handleInstallAnyDesk(ActionEvent e) {
        startDownload(InstallService.Software.ANYDESK, progressAnyDesk, lblAnyDeskStatus, btnInstallAnyDesk, btnRunAnyDesk);
    }
    @FXML public void handleRunAnyDesk(ActionEvent e) { runInstaller(btnRunAnyDesk, "AnyDesk"); }

    @FXML public void handleInstallTeamViewer(ActionEvent e) {
        startDownload(InstallService.Software.TEAMVIEWER, progressTeamViewer, lblTeamViewerStatus, btnInstallTeamViewer, btnRunTeamViewer);
    }
    @FXML public void handleRunTeamViewer(ActionEvent e) { runInstaller(btnRunTeamViewer, "TeamViewer"); }

    @FXML public void handleInstallWinRAR(ActionEvent e) {
        startDownload(InstallService.Software.WINRAR, progressWinRAR, lblWinRARStatus, btnInstallWinRAR, btnRunWinRAR);
    }
    @FXML public void handleRunWinRAR(ActionEvent e) { runInstaller(btnRunWinRAR, "WinRAR"); }

    @FXML public void handleInstallVLC(ActionEvent e) {
        startDownload(InstallService.Software.VLCPLAYER, progressVLC, lblVLCStatus, btnInstallVLC, btnRunVLC);
    }
    @FXML public void handleRunVLC(ActionEvent e) { runInstaller(btnRunVLC, "VLC"); }

    @FXML public void handleInstall7Zip(ActionEvent e) {
        startDownload(InstallService.Software.SEVEN_ZIP, progress7Zip, lbl7ZipStatus, btnInstall7Zip, btnRun7Zip);
    }
    @FXML public void handleRun7Zip(ActionEvent e) { runInstaller(btnRun7Zip, "7-Zip"); }

    @FXML public void handleInstallNotepadPP(ActionEvent e) {
        startDownload(InstallService.Software.NOTEPADPP, progressNotepadPP, lblNotepadPPStatus, btnInstallNotepadPP, btnRunNotepadPP);
    }
    @FXML public void handleRunNotepadPP(ActionEvent e) { runInstaller(btnRunNotepadPP, "Notepad++"); }

    @FXML public void handleInstallVoixIP(ActionEvent e) {
        startDownload(InstallService.Software.VOIXIP, progressVoixIP, lblVoixIPStatus, btnInstallVoixIP, btnRunVoixIP);
    }
    @FXML public void handleRunVoixIP(ActionEvent e) { runInstaller(btnRunVoixIP, "VoixIP"); }

    /** Baixa todos os softwares em paralelo. */
    @FXML public void handleInstallAll(ActionEvent e) {
        appendInstallLog("=== INICIANDO DOWNLOAD DE TODOS OS SOFTWARES ===");
        handleInstallChrome(e);
        handleInstallAnyDesk(e);
        handleInstallTeamViewer(e);
        handleInstallWinRAR(e);
        handleInstallVLC(e);
        handleInstall7Zip(e);
        handleInstallNotepadPP(e);
        handleInstallVoixIP(e);
    }

    @FXML public void handleOpenInstallersFolder(ActionEvent e) {
        try {
            java.nio.file.Path folder = installService.getDownloadsFolder();
            java.nio.file.Files.createDirectories(folder);
            Desktop.getDesktop().open(folder.toFile());
        } catch (Exception ex) {
            appendInstallLog("❌ Não foi possível abrir a pasta: " + ex.getMessage());
        }
    }

    // ==========================================
    // ABA 8: LINKS ÚTEIS
    // ==========================================

    private void openUrl(String url) {
        try {
            Desktop.getDesktop().browse(new java.net.URI(url));
            setStatusBar("Abrindo: " + url);
        } catch (Exception ex) {
            setStatusBar("Erro ao abrir link: " + ex.getMessage());
        }
    }

    // Sistemas Internos — ajuste as URLs para as reais da sua empresa
    @FXML public void handleOpenErp(ActionEvent e)        { openUrl("http://192.168.1.10/erp"); }
    @FXML public void handleOpenVoixPortal(ActionEvent e) { openUrl("http://192.168.1.10/voixip"); }
    @FXML public void handleOpenHelpDesk(ActionEvent e)   { openUrl("http://192.168.1.10/helpdesk"); }
    @FXML public void handleOpenNas(ActionEvent e)        { openUrl("http://192.168.1.10:5000"); }

    // Ferramentas Online
    @FXML public void handleOpenWhatIsMyIp(ActionEvent e)  { openUrl("https://www.whatismyip.com"); }
    @FXML public void handleOpenSpeedTest(ActionEvent e)   { openUrl("https://fast.com"); }
    @FXML public void handleOpenMxToolbox(ActionEvent e)   { openUrl("https://mxtoolbox.com"); }
    @FXML public void handleOpenShodan(ActionEvent e)      { openUrl("https://www.shodan.io"); }
    @FXML public void handleOpenPingman(ActionEvent e)     { openUrl("https://pingman.com"); }
    @FXML public void handleOpenHpSupport(ActionEvent e)   { openUrl("https://support.hp.com/br-pt/drivers/printers"); }

    // Documentação
    @FXML public void handleOpenMsDocs(ActionEvent e)     { openUrl("https://learn.microsoft.com/pt-br/windows-server/"); }
    @FXML public void handleOpenMsSupport(ActionEvent e)  { openUrl("https://support.microsoft.com/pt-br"); }

}
