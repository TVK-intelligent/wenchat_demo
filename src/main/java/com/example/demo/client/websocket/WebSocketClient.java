
package com.example.demo.client.websocket;

import com.example.demo.client.config.ServerConfig;
import com.example.demo.client.model.ChatMessage;
import com.example.demo.client.model.TypingIndicator;
import com.example.demo.client.model.UserStatusMessage;
import com.example.demo.client.ui.TerminalUI;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * 🔌 WebSocketClient - Spring STOMP client for WebSocket communication
 */
@Slf4j
public class WebSocketClient {
    private final String serverUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> subscriptionIds = new ConcurrentHashMap<>();
    private final BlockingQueue<ChatMessage> messageQueue = new LinkedBlockingQueue<>();

    private WebSocketStompClient stompClient;
    private StompSession stompSession;
    private String jwtToken;
    private Long currentUserId;
    private String currentUsername;
    private CountDownLatch connectionLatch;
    private boolean connected = false;

    public WebSocketClient(String serverUrl) {
        this.serverUrl = serverUrl;
        // Register Java 8 date/time module
        objectMapper.registerModule(new JavaTimeModule());
        // Write dates as ISO-8601 strings instead of arrays
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // Ignore unknown fields during deserialization
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * ✅ Connect to WebSocket server with JWT authentication
     */
    public void connect(String token) throws Exception {
        this.jwtToken = token;
        this.connectionLatch = new CountDownLatch(1);

        // Use SockJS with WebSocket transport
        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);

        this.stompClient = new WebSocketStompClient(sockJsClient);

        // Không set converter - để Spring tự chọn converter mặc định
        // Handlers sẽ nhận byte[] thô và convert bằng ObjectMapper

        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("Authorization", "Bearer " + token);

        // Dùng ServerConfig để get WS URL (đảm bảo luôn lấy URL mới nhất)
        String wsUrl = ServerConfig.getWsUrl();

        // Add token as query parameter (for ngrok compatibility)
        wsUrl += "?token=" + token;

        TerminalUI.printInfo("Connecting to " + wsUrl.split("\\?")[0] + "...");

        try {
            StompSessionHandler handler = new MySessionHandler(this.connectionLatch);
            this.stompSession = stompClient.connectAsync(wsUrl, headers, handler).get(10, TimeUnit.SECONDS);

            if (connectionLatch.await(5, TimeUnit.SECONDS)) {
                this.connected = true;
                TerminalUI.printSuccess("Connected to WebSocket!");

                // Send user ID to backend to register this session
                if (currentUserId != null) {
                    try {
                        stompSession.send("/app/register-session", currentUserId.toString());
                    } catch (Exception e) {
                        System.err.println("Failed to send user registration: " + e.getMessage());
                    }
                }
            } else {
                TerminalUI.printError("Connection timeout");
            }
        } catch (Exception e) {
            TerminalUI.printError("WebSocket connection failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 📨 Subscribe to chat room messages
     */
    public void subscribeToRoom(Long roomId, Consumer<ChatMessage> callback) {
        if (stompSession == null || !connected) {
            TerminalUI.printError("Not connected to WebSocket");
            return;
        }

        String destination = "/topic/room/" + roomId;
        String id = "room-" + roomId;

        try {
            stompSession.subscribe(destination, new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    // Không có converter - Spring truyền byte[] thô
                    return byte[].class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    try {
                        ChatMessage msg = null;

                        if (payload == null) {
                            log.warn("Null payload received for room {}", roomId);
                            return;
                        }

                        if (payload instanceof String) {
                            String json = (String) payload;
                            msg = objectMapper.readValue(json, ChatMessage.class);
                        } else if (payload instanceof ChatMessage) {
                            msg = (ChatMessage) payload;
                        } else if (payload instanceof byte[]) {
                            String json = new String((byte[]) payload, java.nio.charset.StandardCharsets.UTF_8);
                            msg = objectMapper.readValue(json, ChatMessage.class);
                        } else {
                            log.debug("Unknown payload type: {}, attempting conversion",
                                    payload.getClass().getSimpleName());
                            msg = objectMapper.convertValue(payload, ChatMessage.class);
                        }

                        if (msg != null) {
                            callback.accept(msg);
                            messageQueue.offer(msg);
                        }
                    } catch (Exception e) {
                        log.error("Error processing message: {} (payload type: {})",
                                e.getMessage(),
                                payload != null ? payload.getClass().getSimpleName() : "null");
                    }
                }
            });

            subscriptionIds.put(id, destination);
            TerminalUI.printSuccess("Subscribed to room " + roomId);
        } catch (Exception e) {
            TerminalUI.printError("Subscribe failed: " + e.getMessage());
        }
    }

    /**
     * 💬 Subscribe to private messages (both user queue and topic fallback)
     */
    public void subscribeToPrivateMessages(Consumer<ChatMessage> callback) {
        if (stompSession == null || !connected) {
            TerminalUI.printError("Not connected to WebSocket");
            log.error("❌ Cannot subscribe - stompSession={}, connected={}", stompSession, connected);
            return;
        }

        // Subscribe to user queue (primary)
        String userQueueDestination = "/user/queue/messages";
        subscribeToDestination(userQueueDestination, callback, "user-queue");

        // Subscribe to topic fallback for this user's ID
        String topicDestination = "/topic/private/" + currentUserId;
        subscribeToDestination(topicDestination, callback, "topic-fallback");

        TerminalUI.printSuccess("Subscribed to private messages");
    }

    private void subscribeToDestination(String destination, Consumer<ChatMessage> callback, String subscriptionName) {
        try {
            log.debug("🔔 [SUBSCRIPTION] Subscribing to {} ({})", destination, subscriptionName);

            stompSession.subscribe(destination, new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    // Không có converter - Spring truyền byte[] thô
                    return byte[].class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    try {
                        ChatMessage msg = null;

                        if (payload instanceof String) {
                            String json = (String) payload;
                            msg = objectMapper.readValue(json, ChatMessage.class);
                        } else if (payload instanceof ChatMessage) {
                            msg = (ChatMessage) payload;
                        } else if (payload instanceof byte[]) {
                            String json = new String((byte[]) payload, java.nio.charset.StandardCharsets.UTF_8);
                            msg = objectMapper.readValue(json, ChatMessage.class);
                        } else {
                            // Try to convert via ObjectMapper as generic object
                            msg = objectMapper.convertValue(payload, ChatMessage.class);
                        }

                        if (msg != null) {
                            callback.accept(msg);
                        }
                    } catch (Exception e) {
                        log.error("❌ [PAYLOAD_ERROR-{}] Error parsing message (payload type: {}): {}",
                                subscriptionName,
                                payload != null ? payload.getClass().getSimpleName() : "null",
                                e.getMessage());
                    }
                }
            });

            log.debug("✓ [SUBSCRIPTION] Successfully subscribed to {} ({})", destination, subscriptionName);
            subscriptionIds.put(subscriptionName, destination);
        } catch (Exception e) {
            log.error("❌ [SUBSCRIPTION_ERROR-{}] Failed: {}", subscriptionName, e.getMessage(), e);
        }
    }

    /**
     * 📤 Send message to room
     */
    public void sendChatMessage(Long roomId, String content) {
        if (stompSession == null || !connected) {
            TerminalUI.printError("Not connected to WebSocket");
            return;
        }

        try {
            ChatMessage message = new ChatMessage();
            message.setContent(content);
            message.setRoomId(roomId);
            message.setSenderId(currentUserId);
            message.setSenderUsername(currentUsername);
            message.setMessageType(ChatMessage.MessageType.TEXT);
            message.setTimestamp(java.time.LocalDateTime.now());

            String destination = "/app/chat/room/" + roomId;

            // Convert to JSON byte[] vì getPayloadType return byte[].class
            String json = objectMapper.writeValueAsString(message);
            byte[] payload = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            stompSession.send(destination, payload);
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            TerminalUI.printError("Send failed: " + errorMsg);
            e.printStackTrace();
        }
    }

    /**
     * 🔒 Send private message
     */
    public void sendPrivateMessage(Long recipientId, String content) {
        if (stompSession == null || !connected) {
            TerminalUI.printError("Not connected to WebSocket");
            return;
        }

        try {
            ChatMessage message = new ChatMessage();
            message.setContent(content);
            message.setSenderId(currentUserId);
            message.setSenderUsername(currentUsername);
            message.setMessageType(ChatMessage.MessageType.TEXT);
            message.setTimestamp(java.time.LocalDateTime.now());
            message.setRecipientId(recipientId); // Set recipient ID

            String destination = "/app/private/" + recipientId;

            // Convert to JSON byte[] vì getPayloadType return byte[].class
            String json = objectMapper.writeValueAsString(message);
            byte[] payload = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            stompSession.send(destination, payload);
            TerminalUI.printSuccess("Private message sent");
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            TerminalUI.printError("Send private failed: " + errorMsg);
        }
    }

    /**
     * ⌨️ Send typing indicator
     */
    public void sendTypingIndicator(Long roomId, boolean typing) {
        if (stompSession == null || !connected)
            return;

        try {
            TypingIndicator indicator = new TypingIndicator();
            indicator.setRoomId(roomId);
            indicator.setTyping(typing);

            String destination = "/app/typing/" + roomId;
            String json = objectMapper.writeValueAsString(indicator);
            byte[] payload = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            stompSession.send(destination, payload);
        } catch (Exception e) {
            // Silent fail for typing indicator
        }
    }

    /**
     * 📨 Get message from queue (blocking)
     */
    public ChatMessage getNextMessage(long timeoutSeconds) throws InterruptedException {
        return messageQueue.poll(timeoutSeconds, TimeUnit.SECONDS);
    }

    /**
     * 👥 Subscribe to user status updates (real-time online/offline status)
     */
    public void subscribeToUserStatus(Consumer<UserStatusMessage> callback) {
        if (stompSession == null || !connected) {
            TerminalUI.printError("Not connected to WebSocket");
            return;
        }

        try {
            String destination = "/topic/user-status";
            log.debug("👥 [SUBSCRIPTION] Subscribing to user status updates: {}", destination);

            stompSession.subscribe(destination, new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    // Không có converter - Spring truyền byte[] thô
                    return byte[].class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    try {
                        UserStatusMessage statusUpdate = null;

                        if (payload instanceof String) {
                            String json = (String) payload;
                            statusUpdate = objectMapper.readValue(json, UserStatusMessage.class);
                        } else if (payload instanceof UserStatusMessage) {
                            statusUpdate = (UserStatusMessage) payload;
                        } else if (payload instanceof byte[]) {
                            String json = new String((byte[]) payload, java.nio.charset.StandardCharsets.UTF_8);
                            statusUpdate = objectMapper.readValue(json, UserStatusMessage.class);
                        } else {
                            statusUpdate = objectMapper.convertValue(payload, UserStatusMessage.class);
                        }

                        if (statusUpdate != null) {
                            callback.accept(statusUpdate);
                        }
                    } catch (Exception e) {
                        log.error("❌ Error parsing user status message (payload type: {}): {}",
                                payload != null ? payload.getClass().getSimpleName() : "null",
                                e.getMessage());
                    }
                }
            });

            subscriptionIds.put("user-status", destination);
            log.debug("✓ [SUBSCRIPTION] Successfully subscribed to user status");
        } catch (Exception e) {
            log.error("❌ [SUBSCRIPTION_ERROR] Failed to subscribe to user status: {}", e.getMessage(), e);
        }
    }

    /**
     * 📤 Send user status change (online/offline visibility)
     */
    public void sendStatusChange(Long userId, boolean isOnline) {
        if (stompSession == null || !connected) {
            log.warn("⚠️ Not connected - cannot send status change");
            return;
        }

        try {
            Map<String, Object> statusMessage = new HashMap<>();
            statusMessage.put("userId", userId);
            statusMessage.put("isOnline", isOnline);
            // Convert to JSON byte[] vì getPayloadType return byte[].class
            String json = objectMapper.writeValueAsString(statusMessage);
            byte[] payload = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            stompSession.send("/app/status/change", payload);
            log.debug("📤 Sent status change: userId={}, isOnline={}", userId, isOnline);
        } catch (Exception e) {
            log.error("❌ Failed to send status change: {}", e.getMessage(), e);
        }
    }

    /**
     * ✅ Check connection status
     */
    public boolean isConnected() {
        return connected && stompSession != null && stompSession.isConnected();
    }

    /**
     * 🔌 Disconnect from server
     */
    public void disconnect() {
        if (stompSession != null) {
            try {
                stompSession.disconnect();
                this.connected = false;
                TerminalUI.printInfo("Disconnected from WebSocket");
            } catch (Exception e) {
                TerminalUI.printError("Disconnect error: " + e.getMessage());
            }
        }
    }

    public void setCurrentUserId(Long userId) {
        this.currentUserId = userId;
    }

    public void setCurrentUsername(String username) {
        this.currentUsername = username;
    }

    public Long getCurrentUserId() {
        return currentUserId;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    /**
     * 🎯 Inner class - STOMP Session Handler
     */
    private static class MySessionHandler implements StompSessionHandler {
        private final CountDownLatch latch;

        public MySessionHandler(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            TerminalUI.printSuccess("STOMP connected");
            latch.countDown();
        }

        @Override
        public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload,
                Throwable exception) {
            TerminalUI.printError("STOMP error: " + exception.getMessage());
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            TerminalUI.printError("Transport error: " + exception.getMessage());
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            // Not used - using inline handlers per subscription
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return null;
        }
    }
}
