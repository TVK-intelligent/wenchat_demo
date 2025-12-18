package com.example.demo.client.service;

import com.example.demo.client.model.ChatRoom;
import com.example.demo.client.model.User;
import com.example.demo.client.model.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.time.LocalDateTime;

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
     * � Get JWT token for authentication
     */
    public String getJwtToken() {
        return this.jwtToken;
    }

    /**
     * �🔐 Login user
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
                log.info("Logged in as " + username);
            }

            return result;

        } catch (Exception e) {
            log.error("Login failed: " + e.getMessage());
            log.error("Login error", e);
            return null;
        }
    }

    /**
     * � Register new user
     */
    public boolean register(String username, String password, String displayName) {
        try {
            Map<String, String> registerRequest = new HashMap<>();
            registerRequest.put("username", username);
            registerRequest.put("password", password);
            registerRequest.put("displayName", displayName);

            post("/api/auth/register", objectMapper.writeValueAsString(registerRequest), false);
            log.info("User registered successfully!");
            return true;

        } catch (Exception e) {
            log.error("Registration failed: " + e.getMessage());
            log.error("Registration error", e);
            return false;
        }
    }

    /**
     * �🔓 Logout
     */
    public void logout() {
        try {
            post("/api/auth/logout", "{}", true);
            jwtToken = null;
            log.info("Logged out successfully");
        } catch (Exception e) {
            log.error("Logout failed: " + e.getMessage());
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
            log.error("Failed to fetch rooms: " + e.getMessage());
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
            log.error("Failed to fetch room: " + e.getMessage());
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
            log.error("Failed to fetch users: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 👥 Get all users
     */
    public List<User> getAllUsers() {
        try {
            String response = get("/api/users", true);
            List<User> users = objectMapper.readValue(response,
                    TypeFactory.defaultInstance().constructCollectionType(List.class, User.class));
            return users;

        } catch (Exception e) {
            log.error("Failed to fetch users: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 👥 Get friends list with online/offline status
     */
    public List<java.util.Map<String, Object>> getFriends() {
        try {
            String response = get("/api/friends", true);
            // Parse as FriendshipDto list first
            List<java.util.Map<String, Object>> friendshipDtos = objectMapper.readValue(response,
                    TypeFactory.defaultInstance().constructCollectionType(List.class, java.util.Map.class));

            // Get online users to check status
            Set<Long> onlineUserIds = new java.util.HashSet<>();
            try {
                List<User> onlineUsers = getOnlineUsers();
                for (User u : onlineUsers) {
                    if (u != null && u.getId() != null) {
                        onlineUserIds.add(u.getId());
                    }
                }
            } catch (Exception e) {
                log.warn("Could not fetch online users for status check: " + e.getMessage());
            }

            // Convert to the expected format for UI
            List<java.util.Map<String, Object>> friends = new ArrayList<>();
            Set<Long> addedUserIds = new java.util.HashSet<>(); // Track added users to avoid duplicates

            for (java.util.Map<String, Object> friendship : friendshipDtos) {
                // Extract friend info from the friendship object
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> friend = (java.util.Map<String, Object>) friendship.get("friend");
                if (friend != null) {
                    Long friendId = Long.valueOf(friend.get("id").toString());
                    if (!addedUserIds.contains(friendId)) {
                        java.util.Map<String, Object> friendData = new java.util.HashMap<>();
                        friendData.put("id", friendId);
                        friendData.put("username", friend.get("username"));
                        friendData.put("displayName", friend.get("displayName"));
                        friendData.put("avatarUrl", friend.get("avatarUrl"));
                        // Check if user is online
                        String status = onlineUserIds.contains(friendId) ? "ONLINE" : "OFFLINE";
                        // Also check if status provided in API response
                        if (friend.get("status") != null) {
                            status = friend.get("status").toString();
                        }
                        friendData.put("status", status);
                        friends.add(friendData);
                        addedUserIds.add(friendId);
                    }
                }

                // Also add the user if it's not the current user (bidirectional friendship)
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> user = (java.util.Map<String, Object>) friendship.get("user");
                if (user != null) {
                    User currentUser = getCurrentUser();
                    Long userId = Long.valueOf(user.get("id").toString());
                    if (currentUser != null && !userId.equals(currentUser.getId()) && !addedUserIds.contains(userId)) {
                        java.util.Map<String, Object> friendData = new java.util.HashMap<>();
                        friendData.put("id", userId);
                        friendData.put("username", user.get("username"));
                        friendData.put("displayName", user.get("displayName"));
                        friendData.put("avatarUrl", user.get("avatarUrl"));
                        // Check if user is online
                        String status = onlineUserIds.contains(userId) ? "ONLINE" : "OFFLINE";
                        if (user.get("status") != null) {
                            status = user.get("status").toString();
                        }
                        friendData.put("status", status);
                        friends.add(friendData);
                        addedUserIds.add(userId);
                    }
                }
            }

            return friends;

        } catch (Exception e) {
            log.error("Failed to fetch friends: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Join room
     */
    public boolean joinRoom(Long roomId) {
        try {
            post("/api/rooms/" + roomId + "/join", "{}", true);
            log.info("Joined room " + roomId);
            return true;

        } catch (Exception e) {
            String errorMsg = e.getMessage();

            // Check if user is already in the room
            if (errorMsg.contains("HTTP 403")) {
                log.warn("Already a member of this room or access denied");
                return true; // Treat as success since user can access the room
            }

            log.error("Failed to join room: " + errorMsg);
            return false;
        }
    }

    /**
     * 🚪 Leave room
     */
    public boolean leaveRoom(Long roomId) {
        try {
            post("/api/rooms/" + roomId + "/leave", "{}", true);
            log.info("Left room " + roomId);
            return true;

        } catch (Exception e) {
            log.error("Failed to leave room: " + e.getMessage());
            return false;
        }
    }

    /**
     * 🗑️ Delete room (only for owner)
     */
    public boolean deleteRoom(Long roomId) {
        try {
            delete("/api/rooms/" + roomId, true);
            log.info("Room " + roomId + " deleted successfully");
            return true;

        } catch (Exception e) {
            log.error("Failed to delete room: " + e.getMessage());
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
            log.error("Failed to search users: " + e.getMessage());
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
            log.info("Friend request sent!");
            return true;

        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg.contains("HTTP 409")) {
                log.warn("Already friends or request already sent");
                return true;
            }
            log.error("Failed to add friend: " + errorMsg);
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
            log.error("Failed to fetch pending requests: " + e.getMessage());
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
            log.info("Friend request accepted!");
            return true;

        } catch (Exception e) {
            log.error("Failed to accept friend request: " + e.getMessage());
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
            log.info("Friend request declined!");
            return true;

        } catch (Exception e) {
            log.error("Failed to decline friend request: " + e.getMessage());
            return false;
        }
    }

    /**
     * ➕ Send friend request (delegates to addFriend for unified logic)
     */
    public boolean sendFriendRequest(Long friendId) {
        return addFriend(friendId);
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
            log.info("Room created: " + roomName);
            return room;

        } catch (Exception e) {
            log.error("Failed to create room: " + e.getMessage());
            return null;
        }
    }

    /**
     * � Get my rooms (rooms I own or joined)
     */
    public List<ChatRoom> getMyRooms() {

        try {
            String response = get("/api/rooms/my-rooms", true);
            List<ChatRoom> rooms = objectMapper.readValue(response,
                    TypeFactory.defaultInstance().constructCollectionType(List.class, ChatRoom.class));
            return rooms;

        } catch (Exception e) {
            log.error("Failed to fetch my rooms: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 🌐 Get public rooms
     */
    public List<ChatRoom> getPublicRooms() {
        try {
            String response = get("/api/rooms/public", true);
            List<ChatRoom> rooms = objectMapper.readValue(response,
                    TypeFactory.defaultInstance().constructCollectionType(List.class, ChatRoom.class));
            return rooms;

        } catch (Exception e) {
            log.error("Failed to fetch public rooms: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 📨 Get room messages/history
     */
    public List<ChatMessage> getRoomMessages(Long roomId) {
        try {
            String response = get("/api/messages/room/" + roomId, true);
            List<java.util.Map<String, Object>> messageMaps = objectMapper.readValue(response,
                    TypeFactory.defaultInstance().constructCollectionType(List.class, java.util.Map.class));

            List<ChatMessage> messages = new ArrayList<>();
            for (Map<String, Object> map : messageMaps) {
                ChatMessage msg = new ChatMessage();
                msg.setId(((Number) map.get("id")).longValue());
                msg.setRoomId(((Number) map.get("roomId")).longValue());
                msg.setSenderId(((Number) map.get("senderId")).longValue());
                msg.setSenderUsername((String) map.get("senderUsername"));
                msg.setSenderDisplayName((String) map.get("senderDisplayName"));
                msg.setContent((String) map.get("content"));
                msg.setTimestamp(LocalDateTime.parse((String) map.get("createdAt")));
                msg.setRecalled((Boolean) map.getOrDefault("recalled", false));
                messages.add(msg);
            }
            return messages;

        } catch (Exception e) {
            log.error("Failed to fetch room messages: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * � Get private messages between current user and another user
     */
    public List<ChatMessage> getPrivateMessages(Long otherUserId) {
        try {
            String response = get("/api/messages/private/" + otherUserId, true);
            List<java.util.Map<String, Object>> messageMaps = objectMapper.readValue(response,
                    TypeFactory.defaultInstance().constructCollectionType(List.class, java.util.Map.class));

            List<ChatMessage> messages = new ArrayList<>();
            for (Map<String, Object> map : messageMaps) {
                ChatMessage msg = new ChatMessage();
                msg.setId(((Number) map.get("id")).longValue());
                msg.setSenderId(((Number) map.get("senderId")).longValue());
                msg.setSenderUsername((String) map.get("senderUsername"));
                msg.setSenderDisplayName((String) map.get("senderDisplayName"));
                msg.setContent((String) map.get("content"));

                // Handle recipientId if present
                if (map.get("recipientId") != null) {
                    msg.setRecipientId(((Number) map.get("recipientId")).longValue());
                }

                // Parse timestamp - handle both createdAt and timestamp fields
                String timestampStr = (String) map.get("createdAt");
                if (timestampStr == null) {
                    timestampStr = (String) map.get("timestamp");
                }
                if (timestampStr != null) {
                    msg.setTimestamp(LocalDateTime.parse(timestampStr));
                }

                // Parse message type correctly
                String messageTypeStr = (String) map.get("messageType");
                if (messageTypeStr != null) {
                    try {
                        msg.setMessageType(ChatMessage.MessageType.valueOf(messageTypeStr));
                    } catch (IllegalArgumentException e) {
                        msg.setMessageType(ChatMessage.MessageType.TEXT);
                    }
                } else {
                    msg.setMessageType(ChatMessage.MessageType.TEXT);
                }

                // Parse fileName for file messages
                msg.setFileName((String) map.get("fileName"));

                msg.setRecalled((Boolean) map.getOrDefault("recalled", false));
                messages.add(msg);
            }
            return messages;

        } catch (Exception e) {
            log.error("Failed to fetch private messages: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * �👥 Get room members
     */
    public List<User> getRoomMembers(Long roomId) {
        try {
            String response = get("/api/rooms/" + roomId + "/members", true);
            List<User> members = objectMapper.readValue(response,
                    TypeFactory.defaultInstance().constructCollectionType(List.class, User.class));
            return members;

        } catch (Exception e) {
            log.error("Failed to fetch room members: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 👤 Get current user info
     */
    public User getCurrentUser() {
        try {
            String response = get("/api/users/me", true);
            return objectMapper.readValue(response, User.class);

        } catch (Exception e) {
            log.error("Failed to fetch current user: " + e.getMessage());
            return null;
        }
    }

    /**
     * ✏️ Update user profile
     */
    public boolean updateUserProfile(Long userId, String displayName, String avatarUrl) {
        try {
            Map<String, Object> request = new HashMap<>();
            if (displayName != null)
                request.put("displayName", displayName);
            if (avatarUrl != null)
                request.put("avatarUrl", avatarUrl);

            put("/api/users/" + userId, objectMapper.writeValueAsString(request), true);
            log.info("User profile updated successfully");
            return true;

        } catch (Exception e) {
            log.error("Failed to update user profile: " + e.getMessage());
            return false;
        }
    }

    /**
     * 📨 Get pending room invites
     */
    public List<java.util.Map<String, Object>> getPendingRoomInvites() {
        try {
            String response = get("/api/room-invites/pending", true);
            List<java.util.Map<String, Object>> invites = objectMapper.readValue(response,
                    TypeFactory.defaultInstance().constructCollectionType(List.class, java.util.Map.class));
            return invites;

        } catch (Exception e) {
            log.error("Failed to fetch pending room invites: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * ✅ Invite user to room
     */
    public boolean inviteUserToRoom(Long roomId, Long inviteeId) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("roomId", roomId);
            params.put("inviteeId", inviteeId);

            post("/api/room-invites", objectMapper.writeValueAsString(params), true);
            log.info("User invited to room!");
            return true;

        } catch (Exception e) {
            log.error("Failed to invite user to room: " + e.getMessage());
            return false;
        }
    }

    /**
     * ✅ Accept room invite
     */
    public boolean acceptRoomInvite(Long inviteId) {
        try {
            post("/api/room-invites/" + inviteId + "/accept", "{}", true);
            log.info("Room invite accepted!");
            return true;

        } catch (Exception e) {
            log.error("Failed to accept room invite: " + e.getMessage());
            return false;
        }
    }

    /**
     * ❌ Decline room invite
     */
    public boolean declineRoomInvite(Long inviteId) {
        try {
            post("/api/room-invites/" + inviteId + "/decline", "{}", true);
            log.info("Room invite declined!");
            return true;

        } catch (Exception e) {
            log.error("Failed to decline room invite: " + e.getMessage());
            return false;
        }
    }

    /**
     * 👥 Get available friends for room invitation
     */
    public List<User> getAvailableFriendsForInvite(Long roomId) {
        try {
            String response = get("/api/room-invites/room/" + roomId + "/available-friends", true);
            List<User> friends = objectMapper.readValue(response,
                    TypeFactory.defaultInstance().constructCollectionType(List.class, User.class));
            return friends;

        } catch (Exception e) {
            log.error("Failed to fetch available friends: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 🌐 Generic PUT request
     */
    private String put(String endpoint, String body, boolean authenticated) throws Exception {
        URL url = new URL(baseUrl + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("PUT");
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
     * 📎 Upload file for message
     */
    public String uploadFile(Long roomId, String filePath) throws Exception {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new Exception("File not found: " + filePath);
        }

        String boundary = "----FormBoundary" + System.currentTimeMillis();
        URL url = new URL(baseUrl + "/api/messages/upload?roomId=" + roomId);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setDoOutput(true);

        if (jwtToken != null) {
            conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
        }

        try (OutputStream os = conn.getOutputStream()) {
            // Write file data
            String header = "--" + boundary + "\r\n" +
                    "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n" +
                    "Content-Type: application/octet-stream\r\n\r\n";

            os.write(header.getBytes(StandardCharsets.UTF_8));

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }

            os.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200 && responseCode != 201) {
            String errorMsg = "HTTP " + responseCode;
            try {
                InputStream errorStream = conn.getErrorStream();
                if (errorStream != null) {
                    errorMsg += " - " + readResponse(errorStream);
                }
            } catch (Exception e) {
                // Ignore
            }
            throw new Exception(errorMsg);
        }

        return readResponse(conn.getInputStream());
    }

    /**
     * 📎 Upload file for private message
     */
    public String uploadPrivateFile(Long recipientId, String filePath) throws Exception {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new Exception("File not found: " + filePath);
        }

        String boundary = "----FormBoundary" + System.currentTimeMillis();
        URL url = new URL(baseUrl + "/api/messages/private/upload?recipientId=" + recipientId);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setDoOutput(true);

        if (jwtToken != null) {
            conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
        }

        try (OutputStream os = conn.getOutputStream()) {
            // Write file data
            String header = "--" + boundary + "\r\n" +
                    "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n" +
                    "Content-Type: application/octet-stream\r\n\r\n";

            os.write(header.getBytes(StandardCharsets.UTF_8));

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }

            os.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200 && responseCode != 201) {
            String errorMsg = "HTTP " + responseCode;
            try {
                InputStream errorStream = conn.getErrorStream();
                if (errorStream != null) {
                    errorMsg += " - " + readResponse(errorStream);
                }
            } catch (Exception e) {
                // Ignore
            }
            throw new Exception(errorMsg);
        }

        // Parse JSON response to extract file URL from 'content' field
        String jsonResponse = readResponse(conn.getInputStream());
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.readValue(jsonResponse, Map.class);
            String fileUrl = (String) responseMap.get("content");
            if (fileUrl != null && !fileUrl.isEmpty()) {
                // Prepend base URL if the file URL is relative
                if (!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://")) {
                    fileUrl = baseUrl + fileUrl;
                }
                return fileUrl;
            }
        } catch (Exception e) {
            log.error("Failed to parse upload response: {}", e.getMessage());
        }

        return null;
    }

    /**
     * � Upload avatar for user
     */
    public boolean uploadAvatar(Long userId, String filePath) throws Exception {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new Exception("File not found: " + filePath);
        }

        String boundary = "----FormBoundary" + System.currentTimeMillis();
        URL url = new URL(baseUrl + "/api/users/" + userId + "/avatar");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setDoOutput(true);

        if (jwtToken != null) {
            conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
        }

        try (OutputStream os = conn.getOutputStream()) {
            // Write boundary start
            os.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            os.write(("Content-Disposition: form-data; name=\"avatar\"; filename=\"" + file.getName() + "\"\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            os.write(("Content-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));

            // Write file content
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }

            // Write boundary end
            os.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                log.debug("Avatar upload response: {}", response.toString());
            }
            return true;
        } else {
            log.error("Avatar upload failed with response code: {}", responseCode);
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder error = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    error.append(line);
                }
                log.error("Error response: {}", error.toString());
            }
            return false;
        }
    }

    /**
     * �📋 Login response wrapper
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
