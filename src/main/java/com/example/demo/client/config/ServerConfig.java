package com.example.demo.client.config;

import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 🔧 ServerConfig - Quản lý cấu hình server URL
 * 
 * Ưu tiên cấu hình:
 * 1. Biến môi trường: WENCHAT_SERVER_URL
 * 2. File cấu hình: config.properties (trong thư mục gốc)
 * 3. Classpath: config.properties
 * 4. Mặc định: http://localhost:8081
 */
@Slf4j
public class ServerConfig {

    private static String serverUrl;
    private static String wsUrl;
    private static Properties properties;

    static {
        properties = new Properties();
        loadConfig();
    }

    /**
     * Load cấu hình từ các nguồn (theo ưu tiên)
     */
    private static void loadConfig() {
        log.debug("Loading server configuration...");

        // 1. Kiểm tra System property (được set từ command line argument)
        String sysUrl = System.getProperty("WENCHAT_SERVER_URL");
        if (sysUrl != null && !sysUrl.trim().isEmpty()) {
            serverUrl = sysUrl.trim();
            log.info("✓ Using server URL from System property: {}", serverUrl);
            updateWsUrl();
            return;
        }

        // 2. Kiểm tra biến môi trường
        String envUrl = System.getenv("WENCHAT_SERVER_URL");
        if (envUrl != null && !envUrl.trim().isEmpty()) {
            serverUrl = envUrl.trim();
            log.info("✓ Using server URL from environment: {}", serverUrl);
            updateWsUrl();
            return;
        }

        // 2. Kiểm tra config.properties trong classpath (src/main/resources/)
        try {
            InputStream inputStream = ServerConfig.class.getClassLoader().getResourceAsStream("config.properties");
            if (inputStream != null) {
                properties.load(inputStream);
                String url = properties.getProperty("server.url", "").trim();
                if (!url.isEmpty()) {
                    serverUrl = url;
                    wsUrl = properties.getProperty("ws.url", "").trim();
                    log.info("✓ Using server URL from classpath config.properties: {}", serverUrl);
                    if (wsUrl.isEmpty()) {
                        updateWsUrl();
                    }
                    return;
                }
            }
        } catch (IOException e) {
            log.warn("⚠️ Failed to read config.properties from classpath: {}", e.getMessage());
        }

        // 3. Mặc định
        serverUrl = "http://localhost:8081";
        log.info("✓ Using default server URL: {}", serverUrl);
        updateWsUrl();
    }

    /**
     * Tự động chuyển đổi HTTP → WS, HTTPS → WSS
     */
    private static void updateWsUrl() {
        if (wsUrl == null || wsUrl.isEmpty()) {
            wsUrl = serverUrl.replace("https://", "wss://")
                    .replace("http://", "ws://");
            if (!wsUrl.endsWith("/ws")) {
                wsUrl += "/ws";
            }
        }
    }

    /**
     * Lấy URL server HTTP/HTTPS
     */
    public static String getServerUrl() {
        return serverUrl != null ? serverUrl : "http://localhost:8081";
    }

    /**
     * Lấy URL WebSocket (WS/WSS)
     */
    public static String getWsUrl() {
        return wsUrl != null ? wsUrl : "ws://localhost:8081/ws";
    }

    /**
     * Cập nhật URL server tại runtime (cho ngrok hoặc dynamic URL)
     */
    public static void setServerUrl(String url) {
        serverUrl = url.trim();
        updateWsUrl();
        log.info("✓ Updated server URL to: {}", serverUrl);
        log.info("✓ Updated WebSocket URL to: {}", wsUrl);
    }

    /**
     * Cập nhật URL WebSocket riêng (nếu cần)
     */
    public static void setWsUrl(String url) {
        wsUrl = url.trim();
        log.info("✓ Updated WebSocket URL to: {}", wsUrl);
    }

    /**
     * In thông tin cấu hình
     */
    public static void printConfig() {
        System.out.println("\n📋 Server Configuration:");
        System.out.println("├─ Server URL (HTTP/HTTPS): " + getServerUrl());
        System.out.println("└─ WebSocket URL (WS/WSS):  " + getWsUrl());
        System.out.println();
    }
}
