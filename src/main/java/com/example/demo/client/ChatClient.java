package com.example.demo.client;

import com.example.demo.client.command.CommandParser;
import com.example.demo.client.model.ChatRoom;
import com.example.demo.client.model.User;
import com.example.demo.client.model.ChatMessage;
import com.example.demo.client.service.ChatService;
import com.example.demo.client.ui.TerminalUI;
import com.example.demo.client.ui.MenuUI;
import com.example.demo.client.websocket.WebSocketClient;
import com.example.demo.client.config.ServerConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.nio.charset.StandardCharsets;

/**
 * 💬 ChatClient - Main entry point for CLI chat application
 * 
 * Usage:
 * java -cp target/demo-0.0.1-SNAPSHOT.jar com.example.demo.client.ChatClient
 * 
 * Or from Maven:
 * mvn exec:java -Dexec.mainClass="com.example.demo.client.ChatClient"
 */
@Slf4j
public class ChatClient {

    private final ChatService chatService;
    private final WebSocketClient wsClient;
    private final Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);

    // Message queue to buffer incoming messages
    private final BlockingQueue<ChatMessage> messageQueue = new LinkedBlockingQueue<>();

    // Track message IDs to prevent duplicates
    private final Set<Long> displayedMessageIds = new CopyOnWriteArraySet<>();

    // Store conversation history by friend ID
    private final Map<Long, java.util.List<ChatMessage>> conversationHistory = new java.util.concurrent.ConcurrentHashMap<>();

    // Lock for synchronized console output
    private final Object consoleLock = new Object();

    private boolean isLoggedIn = false;
    private boolean isInRoom = false;
    private String currentUsername;
    private Long currentUserId;
    private Long currentRoomId;
    private String currentRoomName;
    private Thread messageListenerThread;

    // Server configuration - loaded from ServerConfig
    private static String SERVER_URL;
    private static String WS_URL;

    static {
        SERVER_URL = ServerConfig.getServerUrl();
        WS_URL = ServerConfig.getWsUrl();
    }

    public ChatClient() {
        this.chatService = new ChatService(SERVER_URL);
        this.wsClient = new WebSocketClient(WS_URL);
    }

    /**
     * 🚀 Start the chat client
     */
    public void start() {
        TerminalUI.drawHeader();

        // Hiển thị cấu hình server
        ServerConfig.printConfig();

        // Check if server is running
        if (!checkServerConnection()) {
            TerminalUI.printError("Cannot connect to server at " + SERVER_URL);
            System.exit(1);
        }

        TerminalUI.printSuccess("Server is running!");

        // Show menu loop
        menuLoop();
    }

    /**
     * 🎯 Menu loop
     */
    private void menuLoop() {
        while (true) {
            int choice = MenuUI.showMainMenu();

            switch (choice) {
                case 1:
                    handleMenuLogin();
                    break;
                case 2:
                    TerminalUI.printHelp();
                    MenuUI.waitForContinue();
                    break;
                case 3:
                    TerminalUI.println(TerminalUI.GREEN + "\nGoodbye!" + TerminalUI.RESET);
                    System.exit(0);
            }
        }
    }

    /**
     * 🔐 Menu login
     */
    private void handleMenuLogin() {
        MenuUI.LoginInfo loginInfo = MenuUI.showLoginMenu();

        TerminalUI.printInfo("Logging in as " + loginInfo.username + "...");

        ChatService.LoginResponse response = chatService.login(loginInfo.username, loginInfo.password);

        if (response == null || response.getToken() == null) {
            TerminalUI.printError("Login failed");
            MenuUI.waitForContinue();
            return;
        }

        // Successfully logged in
        isLoggedIn = true;
        currentUsername = loginInfo.username;
        currentUserId = response.getUserId();

        // Connect to WebSocket
        try {
            wsClient.connect(response.getToken());
            wsClient.setCurrentUserId(currentUserId);
            wsClient.setCurrentUsername(loginInfo.username);
            TerminalUI.printSuccess("WebSocket connected!");

            // Wait a bit for the connection to fully stabilize
            Thread.sleep(500);

            // Subscribe to private messages
            wsClient.subscribeToPrivateMessages(msg -> {
                // Handle incoming private message
                if (msg.getSenderId().equals(currentUserId)) {
                    // Echo-back detected - own message, just ignore it
                } else if (msg.getRecipientId() != null && msg.getRecipientId().equals(currentUserId)) {
                    // This is a message we received from someone else
                    String senderName = msg.getSenderDisplayName() != null && !msg.getSenderDisplayName().isEmpty()
                            ? msg.getSenderDisplayName()
                            : msg.getSenderUsername();

                    String timeStr = msg.getTimestamp() != null
                            ? msg.getTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
                            : "";

                    synchronized (consoleLock) {
                        TerminalUI.println("");
                        TerminalUI.println(TerminalUI.BRIGHT_YELLOW + "💬 " + senderName + " [" + timeStr + "]:"
                                + TerminalUI.RESET);
                        TerminalUI.println(TerminalUI.BRIGHT_YELLOW + msg.getContent() + TerminalUI.RESET);
                        TerminalUI.println("");
                    }
                    // Store in conversation history
                    long senderId = msg.getSenderId();
                    conversationHistory.computeIfAbsent(senderId, k -> new java.util.ArrayList<>()).add(msg);
                }
            });
            TerminalUI.printSuccess("Private messages subscription enabled!");

            // Subscribe to user status updates
            wsClient.subscribeToUserStatus(statusUpdate -> {
                // Handle real-time user status changes (online/offline)
                log.debug("👥 User status update received: {} ({})", statusUpdate.getUsername(),
                        statusUpdate.getStatus());
                // Status update will be reflected when user requests friend list next time
            });
            TerminalUI.printSuccess("User status updates enabled!");

            // Send online status to server
            wsClient.sendStatusChange(currentUserId, true);

        } catch (Exception e) {
            TerminalUI.printError("Failed to connect to WebSocket: " + e.getMessage());
            isLoggedIn = false;
            MenuUI.waitForContinue();
            return;
        }

        TerminalUI.printSuccess("Welcome, " + loginInfo.username + "!");
        MenuUI.waitForContinue();

        // Enter chat menu
        chatMenuLoop();
    }

    /**
     * 💬 Chat menu loop
     */
    private void chatMenuLoop() {
        while (isLoggedIn) {
            int choice = MenuUI.showChatMenu();

            switch (choice) {
                case 1:
                    handleMenuListRooms();
                    break;
                case 2:
                    handleMenuJoinRoom();
                    break;
                case 3:
                    handleMenuCreateRoom();
                    break;
                case 4:
                    handleMenuViewFriends();
                    break;
                case 5:
                    handleMenuPrivateMessage();
                    break;
                case 6:
                    handleMenuSearchFriends();
                    break;
                case 7:
                    handleMenuViewFriendRequests();
                    break;
                case 8:
                    handleMenuLogout();
                    return;
            }
        }
    }

    /**
     * 📚 Menu: List rooms
     */
    private void handleMenuListRooms() {
        List<ChatRoom> rooms = chatService.getRooms();

        if (rooms.isEmpty()) {
            TerminalUI.printWarning("No rooms available");
            MenuUI.waitForContinue();
            return;
        }

        java.util.List<String> roomStrings = new java.util.ArrayList<>();
        for (ChatRoom room : rooms) {
            roomStrings.add(room.toString());
        }

        int choice = MenuUI.showListMenu("Available Rooms", roomStrings);
        if (choice > 0) {
            ChatRoom selectedRoom = rooms.get(choice - 1);
            handleMenuJoinSelectedRoom(selectedRoom);
        }
    }

    /**
     * 🚪 Menu: Join room
     */
    private void handleMenuJoinRoom() {
        String roomIdStr = MenuUI.showInputDialog("Enter Room ID");

        try {
            Long roomId = Long.parseLong(roomIdStr);

            if (isInRoom) {
                if (MenuUI.showConfirmDialog("Leave current room?")) {
                    handleMenuLeaveRoom();
                }
            }

            if (chatService.joinRoom(roomId)) {
                isInRoom = true;
                currentRoomId = roomId;

                // Lấy room details để có được room name
                try {
                    ChatRoom room = chatService.getRoom(roomId);
                    if (room != null) {
                        currentRoomName = room.getName();
                    } else {
                        currentRoomName = "Room " + roomId;
                    }
                } catch (Exception e) {
                    currentRoomName = "Room " + roomId;
                }

                // Subscribe with queue callback
                wsClient.subscribeToRoom(roomId, msg -> {
                    // Queue the message for display after menu input
                    if (msg != null && msg.getId() != null) {
                        messageQueue.offer(msg);
                    }
                });
                TerminalUI.printSuccess("Joined room " + roomId);
                MenuUI.waitForContinue();
                roomChatLoop();
            }

        } catch (NumberFormatException e) {
            TerminalUI.printError("Invalid room ID");
            MenuUI.waitForContinue();
        }
    }

    /**
     * ➕ Menu: Create room
     */
    private void handleMenuCreateRoom() {
        String roomName = MenuUI.showInputDialog("Enter room name");
        if (roomName.trim().isEmpty()) {
            TerminalUI.printWarning("Room name cannot be empty");
            MenuUI.waitForContinue();
            return;
        }

        String description = MenuUI.showInputDialog("Enter room description (optional)");

        try {
            ChatRoom newRoom = chatService.createRoom(roomName, description);
            TerminalUI.printSuccess("Room created successfully! Room ID: " + newRoom.getId());
            TerminalUI.println("Room name: " + newRoom.getName());
            MenuUI.waitForContinue();
        } catch (Exception e) {
            TerminalUI.printError("Failed to create room: " + e.getMessage());
            MenuUI.waitForContinue();
        }
    }

    /**
     * 🚪 Menu: Join selected room
     */
    private void handleMenuJoinSelectedRoom(ChatRoom room) {
        if (isInRoom && !currentRoomId.equals(room.getId())) {
            if (MenuUI.showConfirmDialog("Leave current room?")) {
                handleMenuLeaveRoom();
            }
        }

        if (chatService.joinRoom(room.getId())) {
            isInRoom = true;
            currentRoomId = room.getId();
            currentRoomName = room.getName();

            // Subscribe with queue callback
            wsClient.subscribeToRoom(room.getId(), msg -> {
                // Queue the message for display after menu input
                if (msg != null && msg.getId() != null) {
                    messageQueue.offer(msg);
                }
            });

            TerminalUI.printSuccess("Joined: " + room.getName());
            MenuUI.waitForContinue();
            roomChatLoop();
        }
    }

    /**
     * Room chat loop - Simplified like private message
     */
    private void roomChatLoop() {
        // Clear displayed message IDs for new room
        displayedMessageIds.clear();
        messageQueue.clear();

        TerminalUI.println("");
        TerminalUI.println(TerminalUI.BRIGHT_CYAN + "╔════════════════ 📂 " + currentRoomName + " 📂 ════════════════╗"
                + TerminalUI.RESET);
        TerminalUI.println(TerminalUI.GRAY + "║ /list • /invite • /leave (hoặc exit/quit)                     ║"
                + TerminalUI.RESET);
        TerminalUI.println(TerminalUI.BRIGHT_CYAN + "╚════════════════════════════════════════════════════════════╝"
                + TerminalUI.RESET);
        TerminalUI.println("");

        // Start background thread to display messages while waiting for input
        Thread messageDisplayThread = new Thread(() -> {
            while (isInRoom && isLoggedIn) {
                try {
                    Thread.sleep(50);
                    displayPendingMessages();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        messageDisplayThread.setDaemon(true);
        messageDisplayThread.start();

        while (isInRoom && isLoggedIn) {
            String input = TerminalUI.getInputSilent();

            if (input.equalsIgnoreCase("/leave") || input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                handleMenuLeaveRoom();
                break;
            } else if (input.equalsIgnoreCase("/list")) {
                handleMenuListUsers();
            } else if (input.equalsIgnoreCase("/invite")) {
                handleMenuInviteUsers();
            } else if (!input.isEmpty() && !input.startsWith("/")) {
                // Send regular message (don't print here, wait for server response)
                if (currentRoomId != null) {
                    wsClient.sendChatMessage(currentRoomId, input);
                    // Message will be displayed when received from server via WebSocket
                }
            } else if (input.startsWith("/")) {
                TerminalUI.printWarning("Unknown command: " + input);
            }
        }

        TerminalUI.println("");
    }

    /**
     * Display all pending messages from queue
     */
    private void displayPendingMessages() {
        ChatMessage msg;
        while ((msg = messageQueue.poll()) != null) {
            if (msg != null && msg.getId() != null) {
                // Only display if we haven't displayed this message before
                if (displayedMessageIds.add(msg.getId())) {
                    synchronized (consoleLock) {
                        String timeStr = msg.getTimestamp() != null ? msg.getTimestamp().toString().substring(11, 16)
                                : "??:??";
                        String senderName = msg.getSenderDisplayName() != null ? msg.getSenderDisplayName()
                                : msg.getSenderUsername();

                        // Add "(You)" if this is our message
                        boolean isOwnMessage = msg.getSenderId() != null
                                && msg.getSenderId().equals(wsClient.getCurrentUserId());
                        String youMarker = isOwnMessage ? " (You)" : "";

                        String displayMsg = String.format(
                                "%s[%s] %s%s: %s%s",
                                TerminalUI.BRIGHT_GREEN,
                                timeStr,
                                senderName,
                                youMarker,
                                TerminalUI.RESET,
                                msg.getContent());
                        TerminalUI.println(displayMsg);
                    }
                }
            }
        }
    }

    /**
     * 👥 Menu: List users
     */
    private void handleMenuListUsers() {
        List<User> users = chatService.getOnlineUsers();

        if (users.isEmpty()) {
            TerminalUI.printWarning("No users online");
            MenuUI.waitForContinue();
            return;
        }

        java.util.List<String> userStrings = new java.util.ArrayList<>();
        for (User user : users) {
            userStrings.add(user.toString());
        }

        MenuUI.showListMenu("Online Users", userStrings);
        MenuUI.waitForContinue();
    }

    /**
     * 👥 Menu: Invite users to current room
     */
    private void handleMenuInviteUsers() {
        if (currentRoomId == null) {
            TerminalUI.printWarning("Not in a room");
            MenuUI.waitForContinue();
            return;
        }

        // Get list of online users (friends)
        List<User> users = chatService.getOnlineUsers();

        if (users == null || users.isEmpty()) {
            TerminalUI.printWarning("No users online to invite");
            MenuUI.waitForContinue();
            return;
        }

        // Convert to display strings
        java.util.List<String> userStrings = new java.util.ArrayList<>();
        java.util.Map<Integer, User> userMap = new java.util.HashMap<>();

        int index = 1;
        for (User user : users) {
            if (!user.getId().equals(currentUserId)) { // Don't invite self
                userStrings.add(String.format("%s (ID: %d)", user.getUsername(), user.getId()));
                userMap.put(index, user);
                index++;
            }
        }

        if (userMap.isEmpty()) {
            TerminalUI.printWarning("No other users available to invite");
            MenuUI.waitForContinue();
            return;
        }

        // Show user list with selection
        int choice = MenuUI.showListMenu("Invite Users to " + currentRoomName, userStrings);

        if (choice > 0 && choice <= userMap.size()) {
            User selectedUser = userMap.get(choice);
            try {
                chatService.inviteUserToRoom(currentRoomId, selectedUser.getId());
                TerminalUI.printSuccess("Invitation sent to " + selectedUser.getUsername());
            } catch (Exception e) {
                TerminalUI.printError("Failed to invite user: " + e.getMessage());
            }
        }
        MenuUI.waitForContinue();
    }

    /**
     * 📨 Menu: Private message
     */
    private void handleMenuPrivateMessage() {
        // Get list of friends only
        List<Map<String, Object>> friendList = chatService.getFriends();

        if (friendList == null || friendList.isEmpty()) {
            TerminalUI.printWarning("You have no friends yet");
            MenuUI.waitForContinue();
            return;
        }

        // Convert to display strings with status
        java.util.List<String> userStrings = new java.util.ArrayList<>();
        java.util.List<Long> friendIds = new java.util.ArrayList<>();
        java.util.List<String> friendUsernames = new java.util.ArrayList<>();

        for (Map<String, Object> friendship : friendList) {
            // Get friend info from friendship object
            java.util.Map<String, Object> friendData = null;
            Long otherUserId = null;

            // Try "friend" field first
            Object friendObj = friendship.get("friend");
            if (friendObj instanceof java.util.Map) {
                java.util.Map<String, Object> friendMap = (java.util.Map<String, Object>) friendObj;
                Object idObj = friendMap.get("id");
                Long friendId = null;
                if (idObj instanceof Long) {
                    friendId = (Long) idObj;
                } else if (idObj instanceof Integer) {
                    friendId = ((Integer) idObj).longValue();
                }

                if (friendId != null && !friendId.equals(currentUserId)) {
                    friendData = friendMap;
                    otherUserId = friendId;
                }
            }

            // Try "user" field if friend field didn't work
            if (friendData == null) {
                Object userObj = friendship.get("user");
                if (userObj instanceof java.util.Map) {
                    java.util.Map<String, Object> userMap = (java.util.Map<String, Object>) userObj;
                    Object idObj = userMap.get("id");
                    Long userId = null;
                    if (idObj instanceof Long) {
                        userId = (Long) idObj;
                    } else if (idObj instanceof Integer) {
                        userId = ((Integer) idObj).longValue();
                    }

                    if (userId != null && !userId.equals(currentUserId)) {
                        friendData = userMap;
                        otherUserId = userId;
                    }
                }
            }

            if (friendData != null && otherUserId != null) {
                String username = (String) friendData.get("username");
                String displayName = (String) friendData.get("displayName");
                Boolean isOnline = (Boolean) friendData.get("isOnline");

                String statusIcon = (isOnline != null && isOnline) ? "●" : "○";
                String name = displayName != null && !displayName.isEmpty() ? displayName : username;

                userStrings.add(String.format("%s %s", statusIcon, name));
                friendIds.add(otherUserId);
                friendUsernames.add(username);
            }
        }

        if (userStrings.isEmpty()) {
            TerminalUI.printWarning("No friends available");
            MenuUI.waitForContinue();
            return;
        }

        // Show friend list and get selection
        int choice = MenuUI.showListMenu("Select Friend to Message", userStrings);

        if (choice == 0) {
            // Back to main menu
            return;
        } else if (choice > 0 && choice <= friendIds.size()) {
            Long selectedFriendId = friendIds.get(choice - 1);
            String selectedUsername = friendUsernames.get(choice - 1);

            // Enter private chat mode with this friend
            privateMessageChatLoop(selectedFriendId, selectedUsername);
        }
    }

    /**
     * 💬 Private message chat loop - Stay in conversation with one friend
     */
    private void privateMessageChatLoop(Long friendId, String friendUsername) {
        TerminalUI.println("");
        TerminalUI.println(
                TerminalUI.BRIGHT_CYAN + "╔════════════════ 💬 Chat with " + friendUsername + " 💬 ════════════════╗"
                        + TerminalUI.RESET);
        TerminalUI.println(TerminalUI.GRAY + "║ exit or quit to go back                                        ║"
                + TerminalUI.RESET);
        TerminalUI.println(TerminalUI.BRIGHT_CYAN + "╚════════════════════════════════════════════════════════════╝"
                + TerminalUI.RESET);
        TerminalUI.println("");

        // Display existing conversation history for this friend
        if (conversationHistory.containsKey(friendId)) {
            TerminalUI.println(TerminalUI.GRAY + "📜 ━━━━━━ Conversation History ━━━━━━ 📜" + TerminalUI.RESET);
            for (ChatMessage historyMsg : conversationHistory.get(friendId)) {
                displayMessageInChat(historyMsg);
            }
            TerminalUI.println(TerminalUI.GRAY + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + TerminalUI.RESET);
            TerminalUI.println("");
        }

        while (true) {
            String message = TerminalUI.getInputSilent();

            if (message.equalsIgnoreCase("exit") || message.equalsIgnoreCase("quit")) {
                TerminalUI.println(TerminalUI.GRAY + "Exiting conversation..." + TerminalUI.RESET);
                break;
            }

            if (!message.isEmpty()) {
                wsClient.sendPrivateMessage(friendId, message);
                TerminalUI.println(TerminalUI.GREEN + "✓ Sent" + TerminalUI.RESET);
            }
        }

        TerminalUI.println("");
    }

    /**
     * Helper method to display a message in chat
     */
    private void displayMessageInChat(ChatMessage msg) {
        String senderName = msg.getSenderDisplayName() != null && !msg.getSenderDisplayName().isEmpty()
                ? msg.getSenderDisplayName()
                : msg.getSenderUsername();

        String timeStr = msg.getTimestamp() != null
                ? msg.getTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
                : "";

        TerminalUI.println(TerminalUI.BRIGHT_YELLOW + "💬 " + senderName + " [" + timeStr + "]:" + TerminalUI.RESET);
        TerminalUI.println(TerminalUI.BRIGHT_YELLOW + msg.getContent() + TerminalUI.RESET);
        TerminalUI.println("");
    }

    /**
     * 🚪 Menu: Leave room
     */
    private void handleMenuLeaveRoom() {
        if (!isInRoom) {
            TerminalUI.printWarning("Not in a room");
            return;
        }

        if (chatService.leaveRoom(currentRoomId)) {
            isInRoom = false;
            currentRoomId = null;
            TerminalUI.printSuccess("Left room");
        }
    }

    /**
     * 🔓 Menu: Logout
     */
    private void handleMenuLogout() {
        if (isInRoom) {
            handleMenuLeaveRoom();
        }

        // Send offline status to server before disconnecting
        if (currentUserId != null) {
            wsClient.sendStatusChange(currentUserId, false);
        }

        // Call logout API to set user status to OFFLINE
        chatService.logout();

        wsClient.disconnect();
        isLoggedIn = false;
        currentUsername = null;
        currentUserId = null;
        currentRoomId = null;

        TerminalUI.printSuccess("Logged out");
        MenuUI.waitForContinue();
    }

    /**
     * 📬 Menu: View pending friend requests
     */
    private void handleMenuViewFriendRequests() {
        List<java.util.Map<String, Object>> pendingRequests = chatService.getPendingRequests();

        if (pendingRequests == null || pendingRequests.isEmpty()) {
            TerminalUI.printWarning("No pending friend requests");
            MenuUI.waitForContinue();
            return;
        }

        // Display pending requests
        TerminalUI.println("");
        TerminalUI.println(TerminalUI.BRIGHT_CYAN + "+====== PENDING REQUESTS ======+" + TerminalUI.RESET);
        TerminalUI.println("");

        java.util.List<String> requestStrings = new java.util.ArrayList<>();
        java.util.List<Long> friendshipIds = new java.util.ArrayList<>();
        java.util.List<String> senderNames = new java.util.ArrayList<>();

        int index = 1;
        for (java.util.Map<String, Object> request : pendingRequests) {
            // Get user info from request
            java.util.Map<String, Object> userData = null;
            Object userObj = request.get("user");
            if (userObj instanceof java.util.Map) {
                userData = (java.util.Map<String, Object>) userObj;
            }

            if (userData != null) {
                Object idObj = request.get("id");
                Long friendshipId = null;
                if (idObj instanceof Long) {
                    friendshipId = (Long) idObj;
                } else if (idObj instanceof Integer) {
                    friendshipId = ((Integer) idObj).longValue();
                }

                String username = (String) userData.get("username");
                String displayName = (String) userData.get("displayName");
                String name = displayName != null && !displayName.isEmpty() ? displayName : username;

                requestStrings.add(String.format("[%d] %s - wants to be friends", index, name));
                friendshipIds.add(friendshipId);
                senderNames.add(username);
                index++;
            }
        }

        TerminalUI.println("");
        for (int i = 0; i < requestStrings.size(); i++) {
            TerminalUI.println(TerminalUI.GREEN + (i + 1) + ". " + requestStrings.get(i) + TerminalUI.RESET);
        }
        TerminalUI.println(TerminalUI.GREEN + "0. Back" + TerminalUI.RESET);
        TerminalUI.println("");

        String prompt = TerminalUI.getInput("Choose request (1-" + requestStrings.size() + ") or 0 to go back: ");
        int choice = 0;
        try {
            choice = Integer.parseInt(prompt);
        } catch (NumberFormatException e) {
            MenuUI.waitForContinue();
            return;
        }

        if (choice > 0 && choice <= friendshipIds.size()) {
            Long selectedFriendshipId = friendshipIds.get(choice - 1);
            String selectedName = senderNames.get(choice - 1);

            TerminalUI.println("");
            TerminalUI.println(String.format("What do you want to do with %s's request?", selectedName));
            TerminalUI.println(TerminalUI.GREEN + "1. Accept  2. Decline  0. Back" + TerminalUI.RESET);

            String actionStr = TerminalUI.getInput("Choose action: ");
            int action = 0;
            try {
                action = Integer.parseInt(actionStr);
            } catch (NumberFormatException e) {
                MenuUI.waitForContinue();
                return;
            }

            if (action == 1) {
                if (chatService.acceptFriendRequest(selectedFriendshipId)) {
                    TerminalUI.printSuccess("Friend request accepted!");
                }
            } else if (action == 2) {
                if (chatService.declineFriendRequest(selectedFriendshipId)) {
                    TerminalUI.printSuccess("Friend request declined!");
                }
            }
        }

        MenuUI.waitForContinue();
    }

    /**
     * 👥 Menu: View friends list (online/offline)
     */
    private void handleMenuViewFriends() {
        List<java.util.Map<String, Object>> friends = chatService.getFriends();

        if (friends == null || friends.isEmpty()) {
            TerminalUI.printWarning("You have no friends yet");
            MenuUI.waitForContinue();
            return;
        }

        // Display friends with online status
        TerminalUI.println("");
        TerminalUI.println(TerminalUI.BRIGHT_CYAN + "+========== FRIENDS LIST =========+" + TerminalUI.RESET);
        TerminalUI.println("");

        int count = 0;
        int onlineCount = 0;

        for (java.util.Map<String, Object> friendship : friends) {
            // Get friend info from friendship object
            java.util.Map<String, Object> friendData = null;
            Long otherUserId = null;

            // Try "friend" field first
            Object friendObj = friendship.get("friend");
            if (friendObj instanceof java.util.Map) {
                java.util.Map<String, Object> friendMap = (java.util.Map<String, Object>) friendObj;
                Object idObj = friendMap.get("id");
                Long friendId = null;
                if (idObj instanceof Long) {
                    friendId = (Long) idObj;
                } else if (idObj instanceof Integer) {
                    friendId = ((Integer) idObj).longValue();
                }

                if (friendId != null && !friendId.equals(currentUserId)) {
                    friendData = friendMap;
                    otherUserId = friendId;
                }
            }

            // Try "user" field if friend field didn't work
            if (friendData == null) {
                Object userObj = friendship.get("user");
                if (userObj instanceof java.util.Map) {
                    java.util.Map<String, Object> userMap = (java.util.Map<String, Object>) userObj;
                    Object idObj = userMap.get("id");
                    Long userId = null;
                    if (idObj instanceof Long) {
                        userId = (Long) idObj;
                    } else if (idObj instanceof Integer) {
                        userId = ((Integer) idObj).longValue();
                    }

                    if (userId != null && !userId.equals(currentUserId)) {
                        friendData = userMap;
                        otherUserId = userId;
                    }
                }
            }

            if (friendData != null && otherUserId != null) {
                count++;

                String username = (String) friendData.get("username");
                String displayName = (String) friendData.get("displayName");
                Boolean isOnline = (Boolean) friendData.get("isOnline");

                String onlineStatus = (isOnline != null && isOnline)
                        ? TerminalUI.GREEN + "● ONLINE" + TerminalUI.RESET
                        : TerminalUI.GRAY + "○ OFFLINE" + TerminalUI.RESET;

                if (isOnline != null && isOnline) {
                    onlineCount++;
                }

                String name = displayName != null && !displayName.isEmpty() ? displayName : username;
                TerminalUI.println(String.format("  [%d] %s - %s", count, name, onlineStatus));
            }
        }

        TerminalUI.println("");
        TerminalUI.println(TerminalUI.BRIGHT_CYAN +
                String.format("Total Friends: %d | Online: %d | Offline: %d",
                        count, onlineCount, count - onlineCount)
                + TerminalUI.RESET);
        TerminalUI.println(TerminalUI.BRIGHT_CYAN + "+=================================" + TerminalUI.RESET);
        TerminalUI.println("");

        MenuUI.waitForContinue();
    }

    /**
     * 👫 Menu: Search and add friends
     */
    private void handleMenuSearchFriends() {
        String keyword = MenuUI.showInputDialog("Search username");

        if (keyword.isEmpty()) {
            TerminalUI.printWarning("Please enter a username to search");
            MenuUI.waitForContinue();
            return;
        }

        List<User> searchResults = chatService.searchUsers(keyword);

        if (searchResults.isEmpty()) {
            TerminalUI.printWarning("No users found");
            MenuUI.waitForContinue();
            return;
        }

        // Filter out current user
        searchResults.removeIf(u -> u.getId().equals(currentUserId));

        if (searchResults.isEmpty()) {
            TerminalUI.printWarning("No other users found");
            MenuUI.waitForContinue();
            return;
        }

        // Show search results
        java.util.List<String> userStrings = new java.util.ArrayList<>();
        java.util.Map<Integer, User> userMap = new java.util.HashMap<>();

        int index = 1;
        for (User user : searchResults) {
            userStrings.add(String.format("[%d] %s", user.getId(), user.getUsername()));
            userMap.put(index, user);
            index++;
        }

        int choice = MenuUI.showListMenu("Search Results", userStrings);

        if (choice > 0 && choice < index) {
            User selectedUser = userMap.get(choice);
            if (selectedUser != null) {
                chatService.addFriend(selectedUser.getId());
            }
        }

        MenuUI.waitForContinue();
    }

    /**
     * 🔗 Check server connection
     */
    private boolean checkServerConnection() {
        try {
            // Try simple socket connection to port 8081
            java.net.Socket socket = new java.net.Socket("localhost", 8081);
            socket.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 📤 Helper to print
     */
    private void println(String msg) {
        TerminalUI.println(msg);
    }

    /**
     * 🎬 Main entry point
     */
    public static void main(String[] args) {
        try {
            ChatClient client = new ChatClient();
            client.start();
        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
