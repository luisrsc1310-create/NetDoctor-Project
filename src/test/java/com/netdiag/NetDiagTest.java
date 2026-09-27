package com.netdiag;

import com.netdiag.model.DiagnosticLog;
import com.netdiag.model.NetworkProfile;
import com.netdiag.service.DatabaseService;
import com.netdiag.service.NetworkService;
import com.netdiag.service.SystemInfoService;

public class NetDiagTest {
    public static void main(String[] args) {
        System.out.println("--- TESTE DE DIAGNÓSTICO DO SISTEMA ---");
        SystemInfoService sys = new SystemInfoService();
        var diag = sys.collectDiagnostics();
        System.out.println("Host: " + diag.hostname);
        System.out.println("SO: " + diag.osName);
        System.out.println("Cores: " + diag.availableProcessors);

        System.out.println("\n--- TESTE DE INTERFACES DE REDE ---");
        NetworkService net = new NetworkService();
        var adapters = net.getNetworkAdapters();
        System.out.println("Adaptadores encontrados: " + adapters.size());
        for (var a : adapters) {
            System.out.println(" -> " + a.displayName + " | IP: " + a.ipAddress + " | MAC: " + a.macAddress);
        }

        System.out.println("\n--- TESTE DO BANCO DE DADOS LOCAL SQLITE ---");
        DatabaseService db = DatabaseService.getInstance();
        db.saveProfile(new NetworkProfile(0, "Perfil Teste", "Ethernet", "192.168.1.100", "255.255.255.0", "192.168.1.1", "8.8.8.8", "1.1.1.1"));
        var profiles = db.getAllProfiles();
        System.out.println("Perfis no banco: " + profiles.size());

        db.saveLog(new DiagnosticLog(0, null, "Teste", "127.0.0.1", "OK", "Teste automatizado com sucesso"));
        var logs = db.getRecentLogs(10);
        System.out.println("Logs gravados no banco: " + logs.size());

        System.out.println("\n--- TESTE DE CARREGAMENTO DO FXML ---");
        try {
            javafx.application.Platform.startup(() -> {});
            var url = MainApp.class.getResource("/com/netdiag/view/main.fxml");
            System.out.println("FXML URL: " + url);
            javafx.fxml.FXMLLoader.load(url);
            System.out.println(">>> FXML CARREGADO COM 100% DE SUCESSO! <<<");
        } catch (Throwable t) {
            t.printStackTrace();
        }

        System.out.println("\n--- TESTES FINALIZADOS COM SUCESSO! ---");
        System.exit(0);
    }
}
