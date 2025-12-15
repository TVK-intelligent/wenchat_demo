package com.example.demo.client.config;

import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 🔧 ServerConfig - Quản lý cấu hình server URL
 * 
 * Ưu tiên cấu hình:
 * 1. Biến môi trường: WEBCHAT_G10_SERVER_URL
 * 2. File cấu hình: config.properties
 * 3. Mặc định: http://26.6.143.150:8081 (Radmin VPN)
 */
@Slf4j
public class ServerConfig {

    private static final String DEFAULT_SERVER_URL = "http://26.6.143.150:8081";
    private static final String DEFAULT_WS_URL = "ws://26.6.143.150:8081/ws";

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
        // 1. Kiểm tra System property
        String sysUrl = System.getProperty("WEBCHAT_G10_SERVER_URL");
        if (sysUrl != null && !sysUrl.trim().isEmpty()) {
            serverUrl = sysUrl.trim();
            updateWsUrl();
            return;
        }

        // 2. Kiểm tra biến môi trường
        String envUrl = System.getenv("WEBCHAT_G10_SERVER_URL");
        if (envUrl != null && !envUrl.trim().isEmpty()) {
            serverUrl = envUrl.trim();
            updateWsUrl();
            return;
        }

        // 3. Kiểm tra config.properties trong classpath
        try {
            InputStream inputStream = ServerConfig.class.getClassLoader().getResourceAsStream("config.properties");
            if (inputStream != null) {
                properties.load(inputStream);
                String url = properties.getProperty("server.url", "").trim();
                if (!url.isEmpty()) {
                    serverUrl = url;
                    wsUrl = properties.getProperty("ws.url", "").trim();
                    if (wsUrl.isEmpty()) {
                        updateWsUrl();
                    }
                    return;
                }
            }
        } catch (IOException e) {
            log.warn("Failed to read config.properties: {}", e.getMessage());
        }

        // 4. Mặc định
        serverUrl = DEFAULT_SERVER_URL;
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
        return serverUrl != null ? serverUrl : DEFAULT_SERVER_URL;
    }

    /**
     * Lấy URL WebSocket (WS/WSS)
     */
    public static String getWsUrl() {
        return wsUrl != null ? wsUrl : DEFAULT_WS_URL;
    }

    /**
     * Cập nhật URL server tại runtime
     */
    public static void setServerUrl(String url) {
        serverUrl = url.trim();
        updateWsUrl();
    }

    /**
     * Cập nhật URL WebSocket riêng
     */
    public static void setWsUrl(String url) {
        wsUrl = url.trim();
    }

    /**
     * In thông tin cấu hình
     */
    public static void printConfig() {
        System.out.println("\n📋 Server Configuration:");
        System.out.println("├─ Server URL: " + getServerUrl());
        System.out.println("└─ WebSocket URL: " + getWsUrl());
        System.out.println();
    }
}
