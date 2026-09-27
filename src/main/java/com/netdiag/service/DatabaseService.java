package com.netdiag.service;

import com.netdiag.model.DiagnosticLog;
import com.netdiag.model.NetworkProfile;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseService {
    private static final String DB_URL = "jdbc:sqlite:netdiag.db";
    private static DatabaseService instance;

    private DatabaseService() {
        initDatabase();
    }

    public static synchronized DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private void initDatabase() {
        String sqlProfiles = """
            CREATE TABLE IF NOT EXISTS network_profiles (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                adapter_name TEXT,
                ip_address TEXT NOT NULL,
                subnet_mask TEXT NOT NULL,
                gateway TEXT,
                dns_primary TEXT,
                dns_secondary TEXT
            );
        """;

        String sqlLogs = """
            CREATE TABLE IF NOT EXISTS diagnostic_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp TEXT NOT NULL,
                category TEXT NOT NULL,
                target TEXT NOT NULL,
                status TEXT NOT NULL,
                details TEXT
            );
        """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sqlProfiles);
            stmt.execute(sqlLogs);
        } catch (SQLException e) {
            System.err.println("Erro ao inicializar banco SQLite: " + e.getMessage());
        }
    }

    public List<NetworkProfile> getAllProfiles() {
        List<NetworkProfile> list = new ArrayList<>();
        String sql = "SELECT * FROM network_profiles ORDER BY name ASC";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new NetworkProfile(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("adapter_name"),
                    rs.getString("ip_address"),
                    rs.getString("subnet_mask"),
                    rs.getString("gateway"),
                    rs.getString("dns_primary"),
                    rs.getString("dns_secondary")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void saveProfile(NetworkProfile profile) {
        String sql = """
            INSERT INTO network_profiles (name, adapter_name, ip_address, subnet_mask, gateway, dns_primary, dns_secondary)
            VALUES (?, ?, ?, ?, ?, ?, ?);
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, profile.getName());
            ps.setString(2, profile.getAdapterName());
            ps.setString(3, profile.getIpAddress());
            ps.setString(4, profile.getSubnetMask());
            ps.setString(5, profile.getGateway());
            ps.setString(6, profile.getDnsPrimary());
            ps.setString(7, profile.getDnsSecondary());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteProfile(int id) {
        String sql = "DELETE FROM network_profiles WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<DiagnosticLog> getRecentLogs(int limit) {
        List<DiagnosticLog> list = new ArrayList<>();
        String sql = "SELECT * FROM diagnostic_logs ORDER BY id DESC LIMIT ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new DiagnosticLog(
                        rs.getInt("id"),
                        rs.getString("timestamp"),
                        rs.getString("category"),
                        rs.getString("target"),
                        rs.getString("status"),
                        rs.getString("details")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void saveLog(DiagnosticLog log) {
        String sql = """
            INSERT INTO diagnostic_logs (timestamp, category, target, status, details)
            VALUES (?, ?, ?, ?, ?);
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, log.getTimestamp());
            ps.setString(2, log.getCategory());
            ps.setString(3, log.getTarget());
            ps.setString(4, log.getStatus());
            ps.setString(5, log.getDetails());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void clearLogs() {
        String sql = "DELETE FROM diagnostic_logs";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
