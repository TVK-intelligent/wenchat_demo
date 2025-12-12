package com.example.demo;

import com.example.demo.client.ChatClient;
import lombok.extern.slf4j.Slf4j;
import java.nio.charset.StandardCharsets;
import java.io.PrintStream;
import javax.net.ssl.*;
import java.security.cert.X509Certificate;

/**
 * 🚀 WenChat CLI Client - Main Application Entry Point
 *
 * Đây là file chính để chạy toàn bộ dự án WenChat CLI Client.
 *
 * Cách chạy:
 * 1. Build dự án: mvn clean package
 * 2. Chạy từ JAR: java -jar target/wenchat-client.jar
 * 3. Hoặc chạy từ Maven: mvn exec:java
 *
 * @author WenChat Team
 * @version 1.0
 */
@Slf4j
public class ChatClientApp {

    public static void main(String[] args) {
        try {
            // 0. Disable SSL verification for ngrok (self-signed cert)
            disableSSLVerification();

            // 1. Set UTF-8 encoding for entire JVM (must be done early)
            System.setProperty("file.encoding", "UTF-8");
            System.setProperty("stdout.encoding", "UTF-8");
            System.setProperty("stderr.encoding", "UTF-8");
            System.setProperty("sun.jnu.encoding", "UTF-8");
            System.setProperty("native.encoding", "UTF-8");

            // 2. Set UTF-8 output streams
            System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

            // 3. Enable UTF-8 on Windows console (change code page to UTF-8)
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                try {
                    ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "chcp 65001");
                    pb.inheritIO();
                    pb.start().waitFor();
                } catch (Exception e) {
                    log.debug("Warning: Could not set Windows console to UTF-8 mode");
                }
            }

            log.info("🚀 Starting WenChat CLI Client Application...");
            log.info("Java version: {}", System.getProperty("java.version"));
            log.info("Operating system: {} {}", System.getProperty("os.name"), System.getProperty("os.version"));

            // Set server URL from command line argument or environment variable
            if (args.length > 0) {
                System.setProperty("WENCHAT_SERVER_URL", args[0]);
                log.info("✅ Using server URL: {}", args[0]);
            } else if (System.getenv("WENCHAT_SERVER_URL") != null) {
                log.info("✅ Using server URL from env: {}", System.getenv("WENCHAT_SERVER_URL"));
            } else {
                log.info("ℹ️ Using default server URL: http://localhost:8081");
                log.info(
                        "💡 Tip: Pass server URL as argument: java -jar wenchat-client.jar https://your-url.ngrok-free.dev");
            }

            // Khởi tạo và khởi động ChatClient
            ChatClient chatClient = new ChatClient();
            chatClient.start();

        } catch (Exception e) {
            log.error("❌ Fatal error occurred: {}", e.getMessage(), e);
            System.err.println("\n❌ Application failed to start: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Disable SSL certificate verification (for ngrok self-signed certificates)
     * ⚠️ WARNING: Only use for development/testing with trusted self-signed certs!
     */
    private static void disableSSLVerification() {
        try {
            // Create a trust manager that trusts all certificates
            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };

            // Install the trust manager
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

            // Disable hostname verification
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

            log.debug("⚠️ SSL verification disabled (for ngrok)");
        } catch (Exception e) {
            log.error("Failed to disable SSL verification: {}", e.getMessage());
        }
    }
}
