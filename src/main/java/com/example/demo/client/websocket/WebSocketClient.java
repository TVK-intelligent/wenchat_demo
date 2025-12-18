package com.example.demo.client.websocket;

import com.example.demo.client.config.ServerConfig;
import com.example.demo.client.model.ChatMessage;
import com.example.demo.client.model.FriendRequestNotification;
import com.example.demo.client.model.RoomInviteNotification;
import com.example.demo.client.model.TypingIndicator;
import com.example.demo.client.model.UserStatusMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
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
 * (Fixed & Optimized)
 */
@Slf4j
public class WebSocketClient {
    private final String serverUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ✅ FIX 1: Dùng StompSession.Subscription thay vì StompSubscription (không tồn
    // tại)
    private final Map<String, StompSession.Subscription> subscriptionIds = new ConcurrentHashMap<>();

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
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * ✅ Connect to WebSocket server with JWT authentication
     */
    public void connect(String token) throws Exception {
        this.jwtToken = token;
        this.connectionLatch = new CountDownLatch(1);

        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);

        this.stompClient = new WebSocketStompClient(sockJsClient);

        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("Authorization", "Bearer " + token);

        String wsUrl = ServerConfig.getWsUrl();
        wsUrl += "?token=" + token;

        log.info("Connecting to " + wsUrl.split("\\?")[0] + "...");

        try {
            StompSessionHandler handler = new MySessionHandler(this.connectionLatch);
            this.stompSession = stompClient.connectAsync(wsUrl, headers, handler).get(10, TimeUnit.SECONDS);

            if (connectionLatch.await(5, TimeUnit.SECONDS)) {
                this.connected = true;
                log.info("Connected to WebSocket!");

                if (currentUserId != null) {
                    try {
                        stompSession.send("/app/register-session", currentUserId.toString());
                    } catch (Exception e) {
                        System.err.println("Failed to send user registration: " + e.getMessage());
                    }
                }
            } else {
                log.error("Connection timeout");
            }
        } catch (Exception e) {
            log.error("WebSocket connection failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * ♻️ HELPER: Hàm chung để parse dữ liệu (giúp code gọn hơn, tránh lặp lại)
     */
    private <T> T parsePayload(Object payload, Class<T> targetClass) {
        try {
            if (payload == null)
                return null;
            if (targetClass.isInstance(payload)) {
                return targetClass.cast(payload);
            }
            if (payload instanceof String) {
                return objectMapper.readValue((String) payload, targetClass);
            }
            if (payload instanceof byte[]) {
                String json = new String((byte[]) payload, java.nio.charset.StandardCharsets.UTF_8);
                return objectMapper.readValue(json, targetClass);
            }
            return objectMapper.convertValue(payload, targetClass);
        } catch (Exception e) {
            log.error("❌ Parse error for {}: {}", targetClass.getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * 📨 Subscribe to chat room messages
     */
    public void subscribeToRoom(Long roomId, Consumer<ChatMessage> callback) {
        if (stompSession == null || !connected)
            return;

        String destination = "/topic/room/" + roomId;
        String id = "room-" + roomId;

        if (subscriptionIds.containsKey(id))
            return;

        try {
            // ✅ FIX: StompSession.Subscription
            StompSession.Subscription subscription = stompSession.subscribe(destination, new StompFrameHandler() {
                @Override
                @NonNull
                public Type getPayloadType(@NonNull StompHeaders headers) {
                    return byte[].class;
                }

                @Override
                public void handleFrame(@NonNull StompHeaders headers, @Nullable Object payload) {
                    ChatMessage msg = parsePayload(payload, ChatMessage.class); // Dùng hàm chung
                    if (msg != null) {
                        callback.accept(msg);
                        messageQueue.offer(msg);
                    }
                }
            });

            subscriptionIds.put(id, subscription);
            log.info("Subscribed to room " + roomId);
        } catch (Exception e) {
            log.error("Subscribe failed: " + e.getMessage());
        }
    }

    /**
     * Unsubscribe from chat room messages
     */
    public void unsubscribeFromRoom(Long roomId) {
        String id = "room-" + roomId;
        // ✅ FIX: StompSession.Subscription
        StompSession.Subscription subscription = subscriptionIds.get(id);
        if (subscription != null) {
            try {
                subscription.unsubscribe();
                subscriptionIds.remove(id);
                log.info("Unsubscribed from room " + roomId);
            } catch (Exception e) {
                log.error("Unsubscribe failed: " + e.getMessage());
            }
        }
    }

    /**
     * 📎 Send file message to room
     */
    public void sendFileMessage(Long roomId, String fileName, String fileUrl) {
        if (stompSession == null || !connected)
            return;

        try {
            ChatMessage message = new ChatMessage();
            message.setContent(fileUrl);
            message.setFileName(fileName);
            message.setRoomId(roomId);
            message.setSenderId(currentUserId);
            message.setSenderUsername(currentUsername);
            message.setMessageType(ChatMessage.MessageType.FILE);
            message.setTimestamp(java.time.LocalDateTime.now());

            String destination = "/app/chat/room/" + roomId;
            String json = objectMapper.writeValueAsString(message);
            stompSession.send(destination, json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Send file failed: " + e.getMessage());
        }
    }

    /**
     * 💬 Subscribe to private messages
     */
    public void subscribeToPrivateMessages(Consumer<ChatMessage> callback) {
        if (stompSession == null || !connected)
            return;

        // Only subscribe to user queue (primary channel) - avoid duplicate
        // subscriptions
        String userQueueDestination = "/user/queue/messages";
        if (!subscriptionIds.containsKey("private-messages")) {
            subscribeToDestination(userQueueDestination, callback, "private-messages");
        }

        // Fallback topic subscription only if user queue is not available
        // This is kept as backup for servers that don't support user destinations
        String topicDestination = "/topic/private/" + currentUserId;
        if (!subscriptionIds.containsKey("private-messages-fallback")) {
            subscribeToDestination(topicDestination, callback, "private-messages-fallback");
        }
    }

    // Track processed message IDs to avoid duplicates
    private final Set<Long> processedMessageIds = java.util.Collections.synchronizedSet(new java.util.HashSet<>());
    private static final int MAX_PROCESSED_IDS = 1000; // Limit memory usage

    private void subscribeToDestination(String destination, Consumer<ChatMessage> callback, String subscriptionName) {
        try {
            // Skip if already subscribed
            if (subscriptionIds.containsKey(subscriptionName)) {
                log.debug("Already subscribed to {}", subscriptionName);
                return;
            }

            StompSession.Subscription subscription = stompSession.subscribe(destination, new StompFrameHandler() {
                @Override
                @NonNull
                public Type getPayloadType(@NonNull StompHeaders headers) {
                    return byte[].class;
                }

                @Override
                public void handleFrame(@NonNull StompHeaders headers, @Nullable Object payload) {
                    ChatMessage msg = parsePayload(payload, ChatMessage.class);
                    if (msg != null) {
                        // Deduplication: Skip if we've already processed this message
                        if (msg.getId() != null && processedMessageIds.contains(msg.getId())) {
                            log.debug("Skipping duplicate message: {}", msg.getId());
                            return;
                        }

                        // Track processed message ID
                        if (msg.getId() != null) {
                            processedMessageIds.add(msg.getId());
                            // Cleanup old IDs to prevent memory leak
                            if (processedMessageIds.size() > MAX_PROCESSED_IDS) {
                                processedMessageIds.clear();
                            }
                        }

                        callback.accept(msg);
                    }
                }
            });
            subscriptionIds.put(subscriptionName, subscription);
            log.info("Subscribed to {} as {}", destination, subscriptionName);
        } catch (Exception e) {
            log.error("Subscribe failed: " + e.getMessage());
        }
    }

    /**
     * 📤 Send message to room
     */
    public void sendChatMessage(Long roomId, String content) {
        if (stompSession == null || !connected)
            return;

        try {
            ChatMessage message = new ChatMessage();
            message.setContent(content);
            message.setRoomId(roomId);
            message.setSenderId(currentUserId);
            message.setSenderUsername(currentUsername);
            message.setMessageType(ChatMessage.MessageType.TEXT);
            message.setTimestamp(java.time.LocalDateTime.now());

            String destination = "/app/chat/room/" + roomId;
            String json = objectMapper.writeValueAsString(message);
            stompSession.send(destination, json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Send failed: " + e.getMessage());
        }
    }

    /**
     * 🔒 Send private message
     */
    public void sendPrivateMessage(Long recipientId, String content) {
        if (stompSession == null || !connected)
            return;

        try {
            ChatMessage message = new ChatMessage();
            message.setContent(content);
            message.setSenderId(currentUserId);
            message.setSenderUsername(currentUsername);
            message.setMessageType(ChatMessage.MessageType.TEXT);
            message.setTimestamp(java.time.LocalDateTime.now());
            message.setRecipientId(recipientId);

            String destination = "/app/private/" + recipientId;
            String json = objectMapper.writeValueAsString(message);
            stompSession.send(destination, json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Send private failed: " + e.getMessage());
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
            stompSession.send(destination, json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            // silent
        }
    }

    public ChatMessage getNextMessage(long timeoutSeconds) throws InterruptedException {
        return messageQueue.poll(timeoutSeconds, TimeUnit.SECONDS);
    }

    /**
     * 👥 Subscribe to user status updates
     */
    public void subscribeToUserStatus(Consumer<UserStatusMessage> callback) {
        if (stompSession == null || !connected)
            return;

        try {
            String destination = "/topic/user-status";
            // ✅ FIX: Lấy subscription object để lưu vào Map
            StompSession.Subscription subscription = stompSession.subscribe(destination, new StompFrameHandler() {
                @Override
                @NonNull
                public Type getPayloadType(@NonNull StompHeaders headers) {
                    return byte[].class;
                }

                @Override
                public void handleFrame(@NonNull StompHeaders headers, @Nullable Object payload) {
                    UserStatusMessage status = parsePayload(payload, UserStatusMessage.class);
                    if (status != null) {
                        callback.accept(status);
                    }
                }
            });

            subscriptionIds.put("user-status", subscription); // ✅ Đã sửa (lưu Subscription thay vì String)
        } catch (Exception e) {
            log.error("Failed to subscribe to user status: " + e.getMessage());
        }
    }

    /**
     * 📤 Send user status change
     */
    public void sendStatusChange(Long userId, boolean isOnline) {
        if (stompSession == null || !connected)
            return;

        try {
            Map<String, Object> statusMessage = new HashMap<>();
            statusMessage.put("userId", userId);
            statusMessage.put("isOnline", isOnline);
            String json = objectMapper.writeValueAsString(statusMessage);
            stompSession.send("/app/status/change", json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Failed to send status change: " + e.getMessage());
        }
    }

    /**
     * 👋 Subscribe to friend request notifications
     */
    public void subscribeToFriendRequests(Consumer<FriendRequestNotification> callback) {
        if (stompSession == null || !connected)
            return;

        String destination = "/user/queue/friend-requests";
        String subscriptionName = "friend-requests";

        if (subscriptionIds.containsKey(subscriptionName)) {
            log.debug("Already subscribed to {}", subscriptionName);
            return;
        }

        try {
            StompSession.Subscription subscription = stompSession.subscribe(destination, new StompFrameHandler() {
                @Override
                @NonNull
                public Type getPayloadType(@NonNull StompHeaders headers) {
                    return byte[].class;
                }

                @Override
                public void handleFrame(@NonNull StompHeaders headers, @Nullable Object payload) {
                    FriendRequestNotification notification = parsePayload(payload, FriendRequestNotification.class);
                    if (notification != null) {
                        log.info("Received friend request from: {}", notification.getSenderUsername());
                        callback.accept(notification);
                    }
                }
            });

            subscriptionIds.put(subscriptionName, subscription);
            log.info("Subscribed to friend requests");
        } catch (Exception e) {
            log.error("Failed to subscribe to friend requests: " + e.getMessage());
        }
    }

    /**
     * 🏠 Subscribe to room invite notifications
     */
    public void subscribeToRoomInvites(Consumer<RoomInviteNotification> callback) {
        if (stompSession == null || !connected)
            return;

        String destination = "/user/queue/room-invites";
        String subscriptionName = "room-invites";

        if (subscriptionIds.containsKey(subscriptionName)) {
            log.debug("Already subscribed to {}", subscriptionName);
            return;
        }

        try {
            StompSession.Subscription subscription = stompSession.subscribe(destination, new StompFrameHandler() {
                @Override
                @NonNull
                public Type getPayloadType(@NonNull StompHeaders headers) {
                    return byte[].class;
                }

                @Override
                public void handleFrame(@NonNull StompHeaders headers, @Nullable Object payload) {
                    RoomInviteNotification notification = parsePayload(payload, RoomInviteNotification.class);
                    if (notification != null) {
                        log.info("Received room invite to: {}", notification.getRoomName());
                        callback.accept(notification);
                    }
                }
            });

            subscriptionIds.put(subscriptionName, subscription);
            log.info("Subscribed to room invites");
        } catch (Exception e) {
            log.error("Failed to subscribe to room invites: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return connected && stompSession != null && stompSession.isConnected();
    }

    public void disconnect() {
        if (stompSession != null) {
            try {
                stompSession.disconnect();
                this.connected = false;
                log.info("Disconnected from WebSocket");
            } catch (Exception e) {
                log.error("Disconnect error: " + e.getMessage());
            }
        }
    }

    public void setCurrentUserId(Long userId) {
        this.currentUserId = userId;
    }

    /**
     * 📋 Register user session with the server
     * Should be called after setting currentUserId and connecting
     */
    public void registerSession() {
        if (stompSession == null || !connected || currentUserId == null) {
            log.warn("Cannot register session: not connected or no user ID");
            return;
        }

        try {
            stompSession.send("/app/register-session", currentUserId.toString());
            log.info("User session registered for userId: {}", currentUserId);
        } catch (Exception e) {
            log.error("Failed to send user registration: {}", e.getMessage());
        }
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
        public void afterConnected(@NonNull StompSession session, @NonNull StompHeaders connectedHeaders) {
            log.info("STOMP connected");
            latch.countDown();
        }

        @Override
        // ✅ FIX 2: Thêm StompCommand vào tham số và import ở trên
        public void handleException(@NonNull StompSession session, @Nullable StompCommand command,
                @NonNull StompHeaders headers, @Nullable byte[] payload, @NonNull Throwable exception) {
            log.error("STOMP error: " + exception.getMessage());
        }

        @Override
        public void handleTransportError(@NonNull StompSession session, @NonNull Throwable exception) {
            log.error("Transport error: {}", exception.getMessage());
        }

        @Override
        public void handleFrame(@NonNull StompHeaders headers, @Nullable Object payload) {
        }

        @Override
        @NonNull
        public Type getPayloadType(@NonNull StompHeaders headers) {
            return String.class; // Default
        }
    }
}