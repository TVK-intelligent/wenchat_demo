package com.example.demo.client.service;

import com.example.demo.client.model.ChatRoom;
import com.example.demo.client.model.User;
import com.example.demo.client.ui.TerminalUI;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * 🌐 ChatService - REST API client for backend
 */
@Slf4j
public class ChatService {

    private final String baseUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String jwtToken;

    public ChatService(String baseUrl) {
        this.baseUrl = baseUrl;
        // Register Java 8 date/time module
        objectMapper.registerModule(new JavaTimeModule());
        // Write dates as ISO-8601 strings instead of arrays
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * 🔓 Set JWT token for authentication
     */
    public void setJwtToken(String token) {
        this.jwtToken = token;
    }

    /**
     * 🔐 Login user
     */
    public LoginResponse login(String username, String password) {
        try {
            Map<String, String> loginRequest = new HashMap<>();
            loginRequest.put("username", username);
            loginRequest.put("password", password);

            String response = post("/api/auth/login", objectMapper.writeValueAsString(loginRequest), false);
            LoginResponse result = objectMapper.readValue(response, LoginResponse.class);

            if (result.getToken() != null) {
                this.jwtToken = result.getToken();
                TerminalUI.printSuccess("Logged in as " + username);
            }

            return result;

        } catch (Exception e) {
            TerminalUI.printError("Login failed: " + e.getMessage());
            log.error("Login error", e);
            return null;
        }
    }

    /**
     * 🔓 Logout
     */
    public void logout() {
        try {
            post("/api/auth/logout", "{}", true);
            jwtToken = null;
            TerminalUI.printSuccess("Logged out successfully");
        } catch (Exception e) {
            TerminalUI.printError("Logout failed: " + e.getMessage());
            log.error("Logout error", e);
        }
    }

    /**
     * 📚 Get all rooms
     */
    public List<ChatRoom> getRooms() {
        try {
            String response = get("/api/rooms", true);
            List<ChatRoom> rooms = objectMapper.readValue(response,
                    TypeFactory.defaultInstance().constructCollectionType(List.class, ChatRoom.class));
            return rooms;

        } catch (Exception e) {
            TerminalUI.printError("Failed to fetch rooms: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 📚 Get room by ID
     */
    public ChatRoom getRoom(Long roomId) {
        try {
            String response = get("/api/rooms/" + roomId, true);
            return objectMapper.readValue(response, ChatRoom.class);

        } catch (Exception e) {
            TerminalUI.printError("Failed to fetch room: " + e.getMessage());
            return null;
        }
    }

    /**
     * 👥 Get online users
     */
    public List<User> getOnlineUsers() {
        try {
            String response = get("/api/users/online", true);
            List<User> users = objectMapper.readValue(response,
                    TypeFactory.defaultInstance().constructCollectionType(List.class, User.class));
            return users;

        } catch (Exception e) {
            TerminalUI.printError("Failed to fetch users: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 👥 Get friends list with online/offline status
     */
    public List<java.util.Map<String, Object>> getFriends() {
        try {
            String response = get("/api/friends", true);
            List<java.util.Map<String, Object>> friends = objectMapper.readValue(response,
                    TypeFactory.defaultInstance().constructCollectionType(List.class, java.util.Map.class));
            return friends;

        } catch (Exception e) {
            TerminalUI.printError("Failed to fetch friends: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 👤 Get user by ID
     */
    public User getUser(Long userId) {
        try {
            String response = get("/api/users/" + userId, true);
            return objectMapper.readValue(response, User.class);

        } catch (Exception e) {
            TerminalUI.printError("Failed to fetch user: " + e.getMessage());
            return null;
        }
    }

    /**
     * 📨 Join room
     */
    public boolean joinRoom(Long roomId) {
        try {
            post("/api/rooms/" + roomId + "/join", "{}", true);
            TerminalUI.printSuccess("Joined room " + roomId);
            return true;

        } catch (Exception e) {
            String errorMsg = e.getMessage();

            // Check if user is already in the room
            if (errorMsg.contains("HTTP 403")) {
                TerminalUI.printWarning("Already a member of this room or access denied");
                return true; // Treat as success since user can access the room
            }

            TerminalUI.printError("Failed to join room: " + errorMsg);
            return false;
        }
    }

    /**
     * 🚪 Leave room
     */
    public boolean leaveRoom(Long roomId) {
        try {
            post("/api/rooms/" + roomId + "/leave", "{}", true);
            TerminalUI.printSuccess("Left room " + roomId);
            return true;

        } catch (Exception e) {
            TerminalUI.printError("Failed to leave room: " + e.getMessage());
            return false;
        }
    }

    /**
     * 🔍 Search users by username
     */
    public List<User> searchUsers(String keyword) {
        try {
            String response = get("/api/users/search?keyword=" + java.net.URLEncoder.encode(keyword, "UTF-8"), true);
            List<User> users = objectMapper.readValue(response,
                    TypeFactory.defaultInstance().constructCollectionType(List.class, User.class));
            return users;

        } catch (Exception e) {
            TerminalUI.printError("Failed to search users: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 👫 Add friend (send friend request)
     */
    public boolean addFriend(Long userId) {
        try {
            // Use query parameter for the friend request endpoint
            String endpoint = String.format("/api/friends/request?friendId=%d", userId);
            post(endpoint, "{}", true);
            TerminalUI.printSuccess("Friend request sent!");
            return true;

        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg.contains("HTTP 409")) {
                TerminalUI.printWarning("Already friends or request already sent");
                return true;
            }
            TerminalUI.printError("Failed to add friend: " + errorMsg);
            return false;
        }
    }

    /**
     * 📥 Get pending friend requests
     */
    public List<java.util.Map<String, Object>> getPendingRequests() {
        try {
            String response = get("/api/friends/pending", true);
            List<java.util.Map<String, Object>> pending = objectMapper.readValue(response,
                    TypeFactory.defaultInstance().constructCollectionType(List.class, java.util.Map.class));
            return pending;

        } catch (Exception e) {
            TerminalUI.printError("Failed to fetch pending requests: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * ✅ Accept friend request
     */
    public boolean acceptFriendRequest(Long friendshipId) {
        try {
            String endpoint = String.format("/api/friends/%d/accept", friendshipId);
            post(endpoint, "{}", true);
            TerminalUI.printSuccess("Friend request accepted!");
            return true;

        } catch (Exception e) {
            TerminalUI.printError("Failed to accept friend request: " + e.getMessage());
            return false;
        }
    }

    /**
     * ❌ Decline friend request
     */
    public boolean declineFriendRequest(Long friendshipId) {
        try {
            String endpoint = String.format("/api/friends/%d/decline", friendshipId);
            delete(endpoint, true);
            TerminalUI.printSuccess("Friend request declined!");
            return true;

        } catch (Exception e) {
            TerminalUI.printError("Failed to decline friend request: " + e.getMessage());
            return false;
        }
    }

    /**
     * 🏠 Create new room
     */
    public ChatRoom createRoom(String roomName, String description) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("name", roomName);
            request.put("description", description);
            request.put("isPrivate", false); // Create as public room by default
            String response = post("/api/rooms", objectMapper.writeValueAsString(request), true);
            ChatRoom room = objectMapper.readValue(response, ChatRoom.class);
            TerminalUI.printSuccess("Room created: " + roomName);
            return room;

        } catch (Exception e) {
            TerminalUI.printError("Failed to create room: " + e.getMessage());
            return null;
        }
    }

    /**
     * 👥 Invite user to room
     */
    public boolean inviteUserToRoom(Long roomId, Long userId) {
        try {
            // Use query parameters for the invite endpoint
            String endpoint = String.format("/api/room-invites?roomId=%d&inviteeId=%d", roomId, userId);
            post(endpoint, "{}", true);
            TerminalUI.printSuccess("Invitation sent!");
            return true;

        } catch (Exception e) {
            TerminalUI.printError("Failed to invite user: " + e.getMessage());
            return false;
        }
    }

    /**
     * 🌐 Generic GET request
     */
    private String get(String endpoint, boolean authenticated) throws Exception {
        URL url = new URL(baseUrl + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-Type", "application/json");

        if (authenticated && jwtToken != null) {
            conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
        }

        int responseCode = conn.getResponseCode();

        if (responseCode != 200) {
            // Try to read error response
            String errorMsg = "HTTP " + responseCode;
            try {
                InputStream errorStream = conn.getErrorStream();
                if (errorStream != null) {
                    String errorResponse = readResponse(errorStream);
                    if (!errorResponse.isEmpty()) {
                        errorMsg += " - " + errorResponse;
                    }
                }
            } catch (Exception e) {
                // Ignore error reading error stream
            }
            throw new Exception(errorMsg);
        }

        return readResponse(conn.getInputStream());
    }

    /**
     * 🌐 Generic POST request
     */
    private String post(String endpoint, String body, boolean authenticated) throws Exception {
        URL url = new URL(baseUrl + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        if (authenticated && jwtToken != null) {
            conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
        }

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes("UTF-8"));
            os.flush();
        }

        int responseCode = conn.getResponseCode();

        if (responseCode != 200 && responseCode != 201) {
            // Try to read error response
            String errorMsg = "HTTP " + responseCode;
            try {
                InputStream errorStream = conn.getErrorStream();
                if (errorStream != null) {
                    String errorResponse = readResponse(errorStream);
                    if (!errorResponse.isEmpty()) {
                        errorMsg += " - " + errorResponse;
                    }
                }
            } catch (Exception e) {
                // Ignore error reading error stream
            }
            throw new Exception(errorMsg);
        }

        return readResponse(conn.getInputStream());
    }

    /**
     * 🗑️ DELETE request
     */
    private String delete(String endpoint, boolean authenticated) throws Exception {
        URL url = new URL(baseUrl + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("DELETE");
        conn.setRequestProperty("Content-Type", "application/json");

        if (authenticated && jwtToken != null) {
            conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
        }

        int responseCode = conn.getResponseCode();

        if (responseCode != 200 && responseCode != 201 && responseCode != 204) {
            // Try to read error response
            String errorMsg = "HTTP " + responseCode;
            try {
                InputStream errorStream = conn.getErrorStream();
                if (errorStream != null) {
                    String errorResponse = readResponse(errorStream);
                    if (!errorResponse.isEmpty()) {
                        errorMsg += " - " + errorResponse;
                    }
                }
            } catch (Exception e) {
                // Ignore error reading error stream
            }
            throw new Exception(errorMsg);
        }

        if (responseCode == 204) {
            return "";
        }

        return readResponse(conn.getInputStream());
    }

    /**
     * 📖 Read HTTP response with UTF-8 encoding
     */
    private String readResponse(InputStream is) throws Exception {
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    /**
     * 📋 Login response wrapper
     */
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class LoginResponse {
        private Long userId;
        private Long id; // From backend AuthResponse
        private String username;
        private String token;
        private String message;

        // Lombok replacements
        public Long getUserId() {
            return userId != null ? userId : id;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Long getId() {
            return id;
        }

        @com.fasterxml.jackson.annotation.JsonProperty("id")
        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
