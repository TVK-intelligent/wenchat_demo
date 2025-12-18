package com.example.demo;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;
import javafx.event.ActionEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Optional;

import com.example.demo.ui.ContentArea;
import com.example.demo.ui.Sidebar;
import com.example.demo.ui.RoomManagementDialog;
import com.example.demo.ui.FriendsManagementDialog;
import com.example.demo.ui.UserSearchDialog;
import com.example.demo.ui.SettingsDialog;
import com.example.demo.ui.ProfileDialog;
import com.example.demo.ui.MessageHistoryDialog;
import com.example.demo.ui.RoomInviteDialog;
import com.example.demo.ui.PrivateChatDialog;
import com.example.demo.client.service.ChatService;
import com.example.demo.client.service.NotificationService;
import com.example.demo.client.websocket.WebSocketClient;
import com.example.demo.client.config.ServerConfig;
import com.example.demo.client.model.ChatRoom;
import com.example.demo.client.model.User;
import com.example.demo.client.model.ChatMessage;
import com.example.demo.client.model.FriendRequestNotification;
import com.example.demo.client.model.RoomInviteNotification;

/**
 * 🚀 WebChat Group 10 Desktop Client - JavaFX Application
 *
 * Đây là ứng dụng desktop cho WebChat Group 10 sử dụng JavaFX.
 *
 * Cách chạy:
 * 1. Build dự án: mvn clean package
 * 2. Chạy từ JAR: java --module-path /path/to/javafx/lib --add-modules
 * javafx.controls,javafx.fxml -jar target/webchat-g10-client.jar
 * 3. Hoặc chạy từ Maven: mvn exec:java
 *
 * @author WenChat Team
 * @version 1.0
 */
@Slf4j
public class ChatClientFXApp extends Application {

    private ChatService chatService;
    private WebSocketClient webSocketClient;
    private NotificationService notificationService;
    private String jwtToken;
    private Long currentUserId;
    private String currentUsername;
    private User currentUser;

    // UI Components
    private ContentArea contentArea;
    private Sidebar sidebar;
    private BorderPane root;

    // Message storage per room
    private Map<Long, List<ChatMessage>> roomMessages = new HashMap<>();

    public static void main(String[] args) {
        // Disable SSL verification for development
        disableSSLVerification();

        // Set UTF-8 encoding
        System.setProperty("file.encoding", "UTF-8");

        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            log.info("🚀 Starting WebChat Group 10 Desktop Client...");

            // Create UI
            contentArea = new ContentArea();
            contentArea.getInputField().setOnAction(e -> sendMessage());
            contentArea.getSendButton().setOnAction(e -> sendMessage());
            contentArea.getFileButton().setOnAction(e -> sendFile());

            // Create sidebar
            sidebar = new Sidebar();
            setupSidebarHandlers();

            // Layout
            root = new BorderPane();
            root.setLeft(sidebar);
            root.setCenter(contentArea);
            root.setStyle("-fx-background-color: #f0f2f5;");

            // Setup theme change callback
            SettingsDialog.setThemeChangeCallback(this::applyTheme);

            // Apply Atlantafx light theme
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

            Scene scene = new Scene(root, 1100, 700);

            // Load custom styles
            if (getClass().getResource("/styles.css") != null) {
                scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            }

            primaryStage.setTitle("WebChat Group 10");
            primaryStage.setScene(scene);
            primaryStage.show();

            // Initialize notification service with window focus tracking
            notificationService = NotificationService.getInstance();
            notificationService.setPrimaryStage(primaryStage);

            // Cleanup notification service on close
            primaryStage.setOnCloseRequest(e -> {
                if (notificationService != null) {
                    notificationService.shutdown();
                }
            });

            // Initialize chat client
            initializeChatClient();

            // Show login dialog on FX thread
            Platform.runLater(this::showLoginDialog);

        } catch (Exception e) {
            log.error("❌ Error starting application: {}", e.getMessage(), e);
            showError("Lỗi khởi động", "Không thể khởi động ứng dụng: " + e.getMessage());
        }
    }

    private void setupSidebarHandlers() {
        // Room selection handler for sidebar
        sidebar.setOnRoomSelected(room -> {
            joinRoom(room);
        });

        // Room selection handler for content area
        contentArea.getRoomSelector().setOnAction(e -> {
            String selectedRoom = contentArea.getRoomSelector().getValue();
            if (selectedRoom != null) {
                joinRoom(selectedRoom);
            }
        });

        // Profile handler
        sidebar.setOnProfileClicked(v -> {
            showProfileDialog();
        });

        // Friends handler
        sidebar.setOnFriendsClicked(v -> {
            showFriendsManagementDialog();
        });

        // Search handler
        sidebar.setOnSearchClicked(v -> {
            showUserSearchDialog();
        });

        // Invites handler
        sidebar.setOnInvitesClicked(v -> {
            showRoomInviteDialog();
        });

        // History handler
        sidebar.setOnHistoryClicked(v -> {
            showMessageHistoryDialog();
        });

        // Settings handler
        sidebar.setOnSettingsClicked(v -> {
            showSettingsDialog();
        });

        // Add room handler
        sidebar.setOnAddRoomClicked(v -> {
            showRoomManagementDialog();
        });

        // User click handler
        sidebar.setOnUserClicked(username -> {
            showUserProfileDialog(username);
        });

        // Friend message click handler (for Direct Messages)
        sidebar.setOnFriendMessageClicked(friend -> {
            openPrivateChatWithUser(friend);
        });
    }

    private void showSettingsDialog() {
        if (chatService == null || jwtToken == null) {
            showError("Lỗi", "Vui lòng đăng nhập trước.");
            return;
        }

        SettingsDialog dialog = new SettingsDialog(chatService);
        dialog.showAndWait();
    }

    /**
     * Apply theme (dark/light) to the application
     */
    private void applyTheme(Boolean isDark) {
        Platform.runLater(() -> {
            if (isDark) {
                // Add dark-theme class to root and all major containers
                if (!root.getStyleClass().contains("dark-theme")) {
                    root.getStyleClass().add("dark-theme");
                }
                if (!sidebar.getStyleClass().contains("dark-theme")) {
                    sidebar.getStyleClass().add("dark-theme");
                }
                if (!contentArea.getStyleClass().contains("dark-theme")) {
                    contentArea.getStyleClass().add("dark-theme");
                }
                root.setStyle("-fx-background-color: #1a1a2e;");
                contentArea.applyDarkMode(true);
            } else {
                // Remove dark-theme class from all containers
                root.getStyleClass().remove("dark-theme");
                sidebar.getStyleClass().remove("dark-theme");
                contentArea.getStyleClass().remove("dark-theme");
                root.setStyle("-fx-background-color: #f0f2f5;");
                contentArea.applyDarkMode(false);
            }
        });
    }

    private void showProfileDialog() {
        if (chatService == null || jwtToken == null || currentUserId == null) {
            showError("Lỗi", "Vui lòng đăng nhập trước.");
            return;
        }

        try {
            User currentUser = chatService.getCurrentUser();
            if (currentUser != null) {
                ProfileDialog dialog = new ProfileDialog(chatService, currentUser);
                dialog.setupEventHandlers(
                        null, // No add friend for own profile
                        null, // No message for own profile
                        () -> showSettingsDialog() // Edit opens settings
                );
                dialog.showAndWait();
            } else {
                showError("Lỗi", "Không thể tải thông tin người dùng.");
            }
        } catch (Exception e) {
            log.error("Error loading profile", e);
            showError("Lỗi", "Không thể tải profile: " + e.getMessage());
        }
    }

    private void showRoomManagementDialog() {
        if (chatService == null || jwtToken == null) {
            showError("Lỗi", "Vui lòng đăng nhập trước.");
            return;
        }

        RoomManagementDialog dialog = new RoomManagementDialog(chatService);
        dialog.showAndWait();
    }

    private void showFriendsManagementDialog() {
        if (chatService == null || jwtToken == null) {
            showError("Lỗi", "Vui lòng đăng nhập trước.");
            return;
        }

        FriendsManagementDialog dialog = new FriendsManagementDialog(chatService);
        dialog.setOnMessageClicked(friend -> {
            openPrivateChatWithUser(friend);
        });
        dialog.showAndWait();
        // Reload friends after dialog closes
        loadFriends();
    }

    /**
     * Open private chat with a user - now in main window
     */
    private void openPrivateChatWithUser(User friend) {
        if (friend == null || webSocketClient == null || currentUser == null) {
            showError("Lỗi", "Không thể mở chat. Vui lòng đăng nhập lại.");
            return;
        }
        try {
            // Switch ContentArea to private chat mode
            contentArea.switchToPrivateChatMode(friend);

            // Set up callbacks for sending messages
            contentArea.setOnBackToRoomClicked(v -> {
                // Reload the current room when going back
                if (currentRoomId != null && loadedRooms != null) {
                    ChatRoom currentRoom = loadedRooms.stream()
                            .filter(r -> r.getId().equals(currentRoomId))
                            .findFirst()
                            .orElse(null);
                    if (currentRoom != null) {
                        String displayName = currentRoom.isPrivate() ? "🔒 " + currentRoom.getName()
                                : "🌐 " + currentRoom.getName();
                        joinRoom(displayName);
                    }
                }
            });

            // Load private message history
            loadPrivateChatMessages(friend);

            // Subscribe to private messages for this user
            if (webSocketClient != null && webSocketClient.isConnected()) {
                webSocketClient.subscribeToPrivateMessages(msg -> handlePrivateChatMessage(msg, friend));
            }

            appendMessage("💬 Đang chat riêng với "
                    + (friend.getDisplayName() != null ? friend.getDisplayName() : friend.getUsername()));
        } catch (Exception e) {
            log.error("Error opening private chat", e);
            showError("Lỗi", "Không thể mở chat: " + e.getMessage());
        }
    }

    /**
     * Load private chat message history
     */
    private void loadPrivateChatMessages(User friend) {
        try {
            List<ChatMessage> messages = chatService.getPrivateMessages(friend.getId());
            if (messages != null && !messages.isEmpty()) {
                for (ChatMessage msg : messages) {
                    boolean isMine = msg.getSenderId().equals(currentUserId);
                    String displayName = isMine ? currentUsername
                            : (friend.getDisplayName() != null ? friend.getDisplayName() : friend.getUsername());
                    contentArea.addMessage(displayName, msg.getContent(), msg.getTimestamp(), isMine);
                }
            }
        } catch (Exception e) {
            log.error("Error loading private messages", e);
        }
    }

    /**
     * Handle incoming private chat message
     */
    private void handlePrivateChatMessage(ChatMessage message, User expectedUser) {
        // Check if we're still in private chat with this user
        if (!contentArea.isPrivateMode()) {
            return;
        }

        User privateChatUser = contentArea.getPrivateChatUser();
        if (privateChatUser == null || !privateChatUser.getId().equals(expectedUser.getId())) {
            return;
        }

        // Check if this message is for our conversation
        if ((message.getSenderId().equals(expectedUser.getId()) &&
                message.getRecipientId() != null && message.getRecipientId().equals(currentUserId)) ||
                (message.getSenderId().equals(currentUserId) &&
                        message.getRecipientId() != null && message.getRecipientId().equals(expectedUser.getId()))) {

            // Avoid duplicates for messages we sent
            if (!message.getSenderId().equals(currentUserId)) {
                javafx.application.Platform.runLater(() -> {
                    String displayName = expectedUser.getDisplayName() != null ? expectedUser.getDisplayName()
                            : expectedUser.getUsername();
                    contentArea.addMessage(displayName, message.getContent(), message.getTimestamp(), false);
                });
            }
        }
    }

    private void showUserSearchDialog() {
        if (chatService == null || jwtToken == null) {
            showError("Lỗi", "Vui lòng đăng nhập trước.");
            return;
        }

        UserSearchDialog dialog = new UserSearchDialog(chatService);
        dialog.showAndWait();
    }

    private void showRoomInviteDialog() {
        if (chatService == null || jwtToken == null) {
            showError("Lỗi", "Vui lòng đăng nhập trước.");
            return;
        }

        RoomInviteDialog dialog = new RoomInviteDialog(chatService);
        dialog.showAndWait();
    }

    private void showMessageHistoryDialog() {
        if (chatService == null || jwtToken == null) {
            showError("Lỗi", "Vui lòng đăng nhập trước.");
            return;
        }

        try {
            ChatRoom currentRoom = null;
            if (currentRoomId != null && loadedRooms != null) {
                currentRoom = loadedRooms.stream()
                        .filter(room -> room.getId().equals(currentRoomId))
                        .findFirst()
                        .orElse(null);
            }

            if (currentRoom == null) {
                // Fallback to default room
                currentRoom = new ChatRoom();
                currentRoom.setId(1L);
                currentRoom.setName("General");
            }

            MessageHistoryDialog dialog = new MessageHistoryDialog(chatService, currentRoom);
            dialog.showAndWait();
        } catch (Exception e) {
            log.error("Error showing message history", e);
            showError("Lỗi", "Không thể tải lịch sử tin nhắn: " + e.getMessage());
        }
    }

    private void showUserProfileDialog(String username) {
        if (chatService == null || jwtToken == null) {
            showError("Lỗi", "Vui lòng đăng nhập trước.");
            return;
        }

        try {
            // Search for user by username
            List<User> users = chatService.searchUsers(username);
            User targetUser = users.stream()
                    .filter(u -> u.getUsername().equals(username) || ("@" + u.getUsername()).equals(username))
                    .findFirst()
                    .orElse(null);

            if (targetUser != null) {
                ProfileDialog dialog = new ProfileDialog(chatService, targetUser);
                dialog.setupEventHandlers(
                        () -> sendFriendRequest(targetUser), // Add friend
                        () -> startPrivateMessage(targetUser), // Message
                        null // No edit for other users
                );
                dialog.showAndWait();
            } else {
                showError("Lỗi", "Không tìm thấy người dùng: " + username);
            }
        } catch (Exception e) {
            log.error("Error loading user profile", e);
            showError("Lỗi", "Không thể tải profile: " + e.getMessage());
        }
    }

    private void sendFriendRequest(User user) {
        try {
            boolean success = chatService.sendFriendRequest(user.getId());
            if (success) {
                appendMessage("✅ Đã gửi lời mời kết bạn đến " + user.getUsername());
            } else {
                showError("Lỗi", "Không thể gửi lời mời kết bạn.");
            }
        } catch (Exception e) {
            log.error("Error sending friend request", e);
            showError("Lỗi", "Lỗi khi gửi lời mời: " + e.getMessage());
        }
    }

    private void startPrivateMessage(User user) {
        try {
            // Create and show private chat dialog
            PrivateChatDialog privateChat = new PrivateChatDialog(
                    chatService,
                    webSocketClient,
                    currentUser,
                    user);
            privateChat.show();
            appendMessage("💬 Mở chat riêng với " + user.getDisplayName());
        } catch (Exception e) {
            log.error("❌ Error opening private chat: {}", e.getMessage(), e);
            appendMessage("❌ Lỗi mở chat riêng: " + e.getMessage());
        }
    }

    private void initializeChatClient() {
        try {
            // Get server URL
            String serverUrl = getServerUrl();
            appendMessage("🔗 Kết nối đến: " + serverUrl);

            // Initialize chat service and websocket client
            chatService = new ChatService(serverUrl);
            webSocketClient = new WebSocketClient(serverUrl);

        } catch (Exception e) {
            log.error("❌ Error initializing chat client: {}", e.getMessage(), e);
            appendMessage("❌ Lỗi kết nối: " + e.getMessage());
        }
    }

    private void showLoginDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Đăng nhập - WebChat Group 10");
        dialog.setHeaderText("Chào mừng đến với WebChat!");
        dialog.getDialogPane().setStyle("-fx-background-color: white;");

        ButtonType loginButtonType = new ButtonType("Đăng nhập", ButtonBar.ButtonData.OK_DONE);
        ButtonType registerButtonType = new ButtonType("Đăng ký", ButtonBar.ButtonData.OTHER);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, registerButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setPadding(new Insets(30, 25, 20, 25));

        TextField username = new TextField();
        username.setPromptText("Nhập tên đăng nhập...");
        username.setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-padding: 8 15 8 15;");
        PasswordField password = new PasswordField();
        password.setPromptText("Nhập mật khẩu...");
        password.setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-padding: 8 15 8 15;");
        TextField displayName = new TextField();
        displayName.setPromptText("Nhập tên hiển thị...");
        displayName.setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-padding: 8 15 8 15;");
        displayName.setVisible(false);

        Label usernameLabel = new Label("Tên đăng nhập:");
        usernameLabel.setStyle("-fx-font-weight: bold;");
        Label passwordLabel = new Label("Mật khẩu:");
        passwordLabel.setStyle("-fx-font-weight: bold;");
        Label displayNameLabel = new Label("Tên hiển thị:");
        displayNameLabel.setStyle("-fx-font-weight: bold;");
        displayNameLabel.setVisible(false);

        grid.add(usernameLabel, 0, 0);
        grid.add(username, 1, 0);
        grid.add(passwordLabel, 0, 1);
        grid.add(password, 1, 1);
        grid.add(displayNameLabel, 0, 2);
        grid.add(displayName, 1, 2);

        dialog.getDialogPane().setContent(grid);

        // Style buttons
        dialog.getDialogPane().lookupButton(loginButtonType).setStyle(
                "-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15 8 15; -fx-cursor: hand;");
        dialog.getDialogPane().lookupButton(registerButtonType).setStyle(
                "-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15 8 15; -fx-cursor: hand;");
        dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setStyle(
                "-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15 8 15; -fx-cursor: hand;");

        // Show display name only for register
        dialog.getDialogPane().lookupButton(registerButtonType).addEventFilter(ActionEvent.ACTION, event -> {
            displayName.setVisible(true);
            displayNameLabel.setVisible(true);
            dialog.setHeaderText("📝 Tạo tài khoản mới");
        });

        dialog.getDialogPane().lookupButton(loginButtonType).addEventFilter(ActionEvent.ACTION, event -> {
            displayName.setVisible(false);
            displayNameLabel.setVisible(false);
            dialog.setHeaderText("Đăng nhập vào WebChat");
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                try {
                    ChatService.LoginResponse loginResponse = chatService.login(username.getText(), password.getText());
                    if (loginResponse != null && loginResponse.getToken() != null) {
                        jwtToken = loginResponse.getToken();
                        currentUserId = loginResponse.getUserId();
                        currentUsername = loginResponse.getUsername();
                        currentUser = chatService.getCurrentUser();
                        chatService.setJwtToken(jwtToken);
                        webSocketClient.setCurrentUserId(currentUserId);
                        webSocketClient.setCurrentUsername(currentUsername);
                        webSocketClient.connect(jwtToken);

                        // Subscribe to default room messages
                        webSocketClient.subscribeToRoom(1L, this::handleIncomingMessage);

                        // Subscribe to notifications
                        webSocketClient.subscribeToFriendRequests(this::handleFriendRequestNotification);
                        webSocketClient.subscribeToRoomInvites(this::handleRoomInviteNotification);
                        webSocketClient.subscribeToPrivateMessages(this::handlePrivateMessageNotification);

                        // Update UI status
                        contentArea.setOnlineStatus(true);
                        sidebar.setCurrentUser(currentUsername);
                        appendMessage("✅ Đăng nhập thành công!");
                        return ButtonType.OK;
                    } else {
                        showError("❌ Đăng nhập thất bại", "Tên đăng nhập hoặc mật khẩu không đúng.");
                        return ButtonType.CANCEL;
                    }
                } catch (Exception e) {
                    log.error("Login error: {}", e.getMessage());
                    showError("❌ Đăng nhập thất bại", "Lỗi kết nối: " + e.getMessage());
                    return ButtonType.CANCEL;
                }
            } else if (dialogButton == registerButtonType) {
                try {
                    boolean success = chatService.register(username.getText(), password.getText(),
                            displayName.getText());
                    if (success) {
                        appendMessage("✅ Đăng ký thành công! Vui lòng đăng nhập.");
                        return ButtonType.CANCEL; // Stay for login
                    } else {
                        showError("❌ Đăng ký thất bại", "Không thể đăng ký tài khoản.");
                        return ButtonType.CANCEL;
                    }
                } catch (Exception e) {
                    log.error("Register error: {}", e.getMessage());
                    showError("❌ Đăng ký thất bại", "Lỗi kết nối: " + e.getMessage());
                    return ButtonType.CANCEL;
                }
            }
            return ButtonType.CANCEL;
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Logged in successfully - load rooms
            loadRooms();
        } else {
            // Not logged in, close app
            Platform.exit();
        }
    }

    private void loadRooms() {
        try {
            // Load rooms from ChatService
            loadedRooms = chatService.getMyRooms();
            if (loadedRooms != null && !loadedRooms.isEmpty()) {
                // Load rooms into sidebar
                sidebar.loadRoomsFromChatRooms(loadedRooms);
                // Load rooms into content area
                contentArea.loadRoomsFromChatRooms(loadedRooms);

                // Load messages for first room and subscribe
                if (webSocketClient != null && webSocketClient.isConnected()) {
                    ChatRoom firstRoom = loadedRooms.get(0);
                    currentRoomId = firstRoom.getId();

                    // Load message history
                    List<ChatMessage> messages = chatService.getRoomMessages(currentRoomId);
                    roomMessages.put(currentRoomId, messages);
                    contentArea.clearMessages();
                    contentArea.addMessages(messages, currentUsername); // Updated to pass currentUsername

                    // Subscribe to room
                    webSocketClient.subscribeToRoom(currentRoomId, this::handleIncomingMessage);
                    appendMessage("✅ Đã tham gia phòng: " + firstRoom.getName());
                }
            } else {
                appendMessage("⚠️ Không có phòng nào để hiển thị");
            }

            // Load online users
            loadOnlineUsers();

            // Load friends for Direct Messages
            loadFriends();

        } catch (Exception e) {
            log.error("Error loading rooms", e);
            appendMessage("❌ Lỗi khi tải danh sách phòng: " + e.getMessage());
        }
    }

    private void loadOnlineUsers() {
        try {
            List<User> onlineUsers = chatService.getOnlineUsers();
            if (onlineUsers != null) {
                sidebar.loadOnlineUsers(onlineUsers);
            }
        } catch (Exception e) {
            log.error("Error loading online users", e);
            appendMessage("❌ Lỗi khi tải danh sách người dùng online: " + e.getMessage());
        }
    }

    /**
     * Load friends list for sidebar Direct Messages
     */
    private void loadFriends() {
        try {
            List<java.util.Map<String, Object>> friendsData = chatService.getFriends();
            if (friendsData != null && !friendsData.isEmpty()) {
                List<User> friends = new ArrayList<>();
                for (java.util.Map<String, Object> data : friendsData) {
                    User friend = new User();
                    friend.setId(Long.valueOf(data.get("id").toString()));
                    friend.setUsername((String) data.get("username"));
                    friend.setDisplayName((String) data.get("displayName"));
                    friend.setAvatarUrl((String) data.get("avatarUrl"));
                    friend.setStatus(data.get("status") != null ? User.Status.valueOf(data.get("status").toString())
                            : User.Status.OFFLINE);
                    friends.add(friend);
                }
                sidebar.loadFriends(friends);
            }
        } catch (Exception e) {
            log.error("Error loading friends", e);
        }
    }

    private String getServerUrl() {
        // Check parameters first (filter out JVM arguments)
        Parameters params = getParameters();
        if (params != null && !params.getUnnamed().isEmpty()) {
            for (String param : params.getUnnamed()) {
                // Skip JVM arguments that start with --
                // Only accept valid HTTP/HTTPS URLs
                if (!param.startsWith("--") && (param.startsWith("http://") || param.startsWith("https://"))) {
                    return param;
                }
            }
        }

        // Check environment variable - validate it's a proper URL
        String envUrl = System.getenv("WEBCHAT_G10_SERVER_URL");
        if (envUrl != null && (envUrl.startsWith("http://") || envUrl.startsWith("https://"))) {
            return envUrl;
        }

        // Default
        return ServerConfig.getServerUrl();
    }

    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("ℹ️ Giới thiệu - WebChat Group 10");
        alert.setHeaderText("💬 WebChat Group 10 Desktop Client");
        alert.setContentText("Ứng dụng chat desktop hiện đại sử dụng JavaFX\n\n" +
                "✨ Tính năng:\n" +
                "• Chat thời gian thực qua WebSocket\n" +
                "• Quản lý phòng chat\n" +
                "• Hệ thống bạn bè\n" +
                "• Giao diện đẹp với JavaFX\n\n" +
                "👥 Phát triển bởi: Group 10\n" +
                "📅 Phiên bản: 1.0\n" +
                "🛠️ Công nghệ: Java 21, Spring Boot, JavaFX");
        alert.getDialogPane().setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.95); -fx-border-color: rgba(102, 126, 234, 0.3); -fx-border-radius: 10; -fx-background-radius: 10;");
        alert.getDialogPane().lookupButton(ButtonType.OK).setStyle(
                "-fx-background-color: linear-gradient(to right, #667eea, #764ba2); -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15 8 15; -fx-border-radius: 20; -fx-background-radius: 20; -fx-cursor: hand;");
        alert.showAndWait();
    }

    // Store loaded rooms for room switching
    private List<ChatRoom> loadedRooms;
    private Long currentRoomId = 1L; // Default to General room

    private void joinRoom(String roomName) {
        try {
            if (loadedRooms != null) {
                // Find room by display name
                ChatRoom targetRoom = loadedRooms.stream()
                        .filter(room -> {
                            String displayName = room.isPrivate() ? "🔒 " + room.getName() : "🌐 " + room.getName();
                            return displayName.equals(roomName);
                        })
                        .findFirst()
                        .orElse(null);

                if (targetRoom != null) {
                    // Fetch message history for the room
                    List<ChatMessage> messages = chatService.getRoomMessages(targetRoom.getId());
                    roomMessages.put(targetRoom.getId(), messages);

                    // Clear current messages and display room messages
                    contentArea.clearMessages();
                    contentArea.addMessages(messages, currentUsername); // Updated to pass currentUsername

                    // Unsubscribe from current room
                    if (webSocketClient != null && currentRoomId != null && !currentRoomId.equals(targetRoom.getId())) {
                        webSocketClient.unsubscribeFromRoom(currentRoomId);
                    }

                    // Subscribe to new room
                    if (webSocketClient != null && webSocketClient.isConnected()) {
                        webSocketClient.subscribeToRoom(targetRoom.getId(), this::handleIncomingMessage);
                        currentRoomId = targetRoom.getId();
                        appendMessage("✅ Đã chuyển sang phòng: " + targetRoom.getName());

                        // Update content area room selector
                        contentArea.getRoomSelector().setValue(roomName);
                    }
                } else {
                    appendMessage("❌ Không tìm thấy phòng: " + roomName);
                }
            }
        } catch (Exception e) {
            log.error("Error joining room", e);
            appendMessage("❌ Lỗi khi chuyển phòng: " + e.getMessage());
        }
    }

    private void sendMessage() {
        String message = contentArea.getInputField().getText().trim();
        if (!message.isEmpty()) {
            try {
                if (webSocketClient != null && webSocketClient.isConnected()) {
                    // Check if we're in private chat mode
                    if (contentArea.isPrivateMode() && contentArea.getPrivateChatUser() != null) {
                        // Send private message
                        User privateChatUser = contentArea.getPrivateChatUser();
                        webSocketClient.sendPrivateMessage(privateChatUser.getId(), message);

                        // Add to local display immediately
                        contentArea.addMessage(currentUsername, message, java.time.LocalDateTime.now(), true);
                        contentArea.getInputField().clear();
                    } else {
                        // Send to current room
                        webSocketClient.sendChatMessage(currentRoomId, message);

                        // Create ChatMessage for local storage and display
                        ChatMessage sentMessage = ChatMessage.builder()
                                .roomId(currentRoomId)
                                .senderId(currentUserId)
                                .senderUsername(currentUsername)
                                .senderDisplayName(currentUsername)
                                .content(message)
                                .timestamp(java.time.LocalDateTime.now())
                                .messageType(ChatMessage.MessageType.TEXT)
                                .build();

                        // Store in room messages
                        // DO NOT add to roomMessages here, wait for server echo in
                        // handleIncomingMessage to avoid duplicates with null IDs
                        // roomMessages.computeIfAbsent(currentRoomId, k -> new
                        // ArrayList<>()).add(sentMessage);

                        contentArea.getInputField().clear();
                    }
                } else {
                    contentArea.addMessage("System", "❌ Chưa kết nối đến server", java.time.LocalDateTime.now());
                }
            } catch (Exception e) {
                log.error("Error sending message: {}", e.getMessage());
                contentArea.addMessage("System", "❌ Lỗi gửi tin nhắn: " + e.getMessage(),
                        java.time.LocalDateTime.now());
            }
        }
    }

    private void sendFile() {
        try {
            // Open file chooser
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Chọn file để gửi");
            File selectedFile = fileChooser.showOpenDialog(contentArea.getScene().getWindow());

            if (selectedFile != null) {
                if (webSocketClient != null && webSocketClient.isConnected() && currentRoomId != null) {
                    // Show loading
                    contentArea.getFileButton().setDisable(true);
                    contentArea.getFileButton().setText("⏳ Đang tải...");

                    // Upload file
                    String fileUrl = chatService.uploadFile(currentRoomId, selectedFile.getAbsolutePath());

                    if (fileUrl != null) {
                        // Send file message via WebSocket
                        webSocketClient.sendFileMessage(currentRoomId, selectedFile.getName(), fileUrl);

                        // Add to local display (Me)
                        // Note: handleIncomingMessage will handle display if echoed, but we display
                        // locally for immediate feedback
                        // Actually, let's let handleIncomingmessage do it to avoid dupes if server
                        // echoes
                        // But file upload might be slow, so maybe instant feedback is good?
                        // For now we rely on handleIncomingMessage for consistency, logic in
                        // sendMessage relies on server echo too?
                        // Wait, sendMessage DOES NOT add to contentArea.
                        // I'll assume server echoes.

                        // Store in room messages
                        // DO NOT add to roomMessages here, wait for server echo
                        /*
                         * ChatMessage fileMessage = ChatMessage.builder()
                         * .roomId(currentRoomId)
                         * .senderId(currentUserId)
                         * .senderUsername(currentUsername)
                         * .senderDisplayName(currentUsername)
                         * .content(fileUrl)
                         * .fileName(selectedFile.getName())
                         * .timestamp(java.time.LocalDateTime.now())
                         * .messageType(ChatMessage.MessageType.FILE)
                         * .build();
                         * roomMessages.computeIfAbsent(currentRoomId, k -> new
                         * ArrayList<>()).add(fileMessage);
                         */

                        appendMessage("✅ Đã gửi file: " + selectedFile.getName());
                    } else {
                        appendMessage("❌ Lỗi tải lên file");
                    }
                } else {
                    contentArea.addMessage("System", "❌ Chưa kết nối đến server hoặc chưa chọn phòng",
                            java.time.LocalDateTime.now());
                }
            }
        } catch (Exception e) {
            log.error("Error sending file: {}", e.getMessage());
            contentArea.addMessage("System", "❌ Lỗi gửi file: " + e.getMessage(), java.time.LocalDateTime.now());
        } finally {
            // Reset button
            contentArea.getFileButton().setDisable(false);
            contentArea.getFileButton().setText("📎 File");
        }
    }

    private void handleIncomingMessage(com.example.demo.client.model.ChatMessage message) {
        if (message != null && message.getContent() != null && message.getRoomId() != null) {
            // Check if message already exists (prevent duplicates)
            List<ChatMessage> messages = roomMessages.computeIfAbsent(message.getRoomId(), k -> new ArrayList<>());
            boolean alreadyExists = messages.stream()
                    .anyMatch(m -> m.getId() != null && m.getId().equals(message.getId()));

            if (!alreadyExists) {
                // Store message in room messages
                messages.add(message);

                // Only display if it's the current room
                if (message.getRoomId().equals(currentRoomId)) {
                    Platform.runLater(() -> {
                        String displayName = message.getSenderDisplayName() != null ? message.getSenderDisplayName()
                                : message.getSenderUsername();

                        boolean isMine = message.getSenderUsername() != null
                                && message.getSenderUsername().equals(currentUsername);

                        if (message.getMessageType() == ChatMessage.MessageType.FILE) {
                            contentArea.addFileMessage(displayName, message.getFileName(), message.getContent(),
                                    message.getTimestamp(), isMine);
                        } else {
                            contentArea.addMessage(displayName, message.getContent(), message.getTimestamp(), isMine);
                        }
                    });
                }

                // Show notification for messages from others (not from current room or window
                // not focused)
                if (message.getSenderId() != null && !message.getSenderId().equals(currentUserId)) {
                    if (!message.getRoomId().equals(currentRoomId) || !notificationService.isWindowFocused()) {
                        String displayName = message.getSenderDisplayName() != null
                                ? message.getSenderDisplayName()
                                : message.getSenderUsername();
                        notificationService.showMessageNotification(displayName, message.getContent());
                    }
                }
            }
        }
    }

    /**
     * Handle friend request notification
     */
    private void handleFriendRequestNotification(FriendRequestNotification notification) {
        if (notification == null)
            return;

        log.info("Received friend request from: {}", notification.getSenderUsername());

        // Show desktop notification
        String displayName = notification.getDisplayName();
        notificationService.showFriendRequestNotification(displayName);

        // Update sidebar badge (if implemented)
        Platform.runLater(() -> {
            sidebar.incrementFriendRequestBadge();
        });
    }

    /**
     * Handle room invite notification
     */
    private void handleRoomInviteNotification(RoomInviteNotification notification) {
        if (notification == null)
            return;

        log.info("Received room invite to: {}", notification.getRoomName());

        // Show desktop notification
        String inviterName = notification.getInviterDisplayName();
        notificationService.showRoomInviteNotification(inviterName, notification.getRoomName());

        // Update sidebar badge (if implemented)
        Platform.runLater(() -> {
            sidebar.incrementRoomInviteBadge();
        });
    }

    /**
     * Handle private message notification (for notifications only, not display)
     */
    private void handlePrivateMessageNotification(ChatMessage message) {
        if (message == null)
            return;

        // Skip if it's our own message
        if (message.getSenderId().equals(currentUserId)) {
            return;
        }

        // Check if we're viewing this private chat
        User privateChatUser = contentArea.getPrivateChatUser();
        boolean isViewingThisChat = contentArea.isPrivateMode()
                && privateChatUser != null
                && privateChatUser.getId().equals(message.getSenderId());

        // Show notification if not viewing this chat or window not focused
        if (!isViewingThisChat || !notificationService.isWindowFocused()) {
            String displayName = message.getSenderDisplayName() != null
                    ? message.getSenderDisplayName()
                    : message.getSenderUsername();
            notificationService.showMessageNotification(displayName, message.getContent());

            // Update unread badge for this friend in sidebar
            Platform.runLater(() -> {
                sidebar.incrementUnreadCount(message.getSenderId());
            });
        }
    }

    private void appendMessage(String message) {
        Platform.runLater(() -> {
            if (message.contains(":")) {
                String[] parts = message.split(":", 2);
                contentArea.addMessage(parts[0].trim(), parts[1].trim());
            } else {
                contentArea.addMessage("System", message);
            }
        });
    }

    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("❌ " + title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.getDialogPane().setStyle(
                    "-fx-background-color: rgba(255, 255, 255, 0.95); -fx-border-color: rgba(255, 102, 102, 0.3); -fx-border-radius: 10; -fx-background-radius: 10;");
            alert.getDialogPane().lookupButton(ButtonType.OK).setStyle(
                    "-fx-background-color: linear-gradient(to right, #ff6b6b, #ffa726); -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15 8 15; -fx-border-radius: 20; -fx-background-radius: 20; -fx-cursor: hand;");
            alert.showAndWait();
        });
    }

    @Override
    public void stop() {
        if (webSocketClient != null) {
            try {
                webSocketClient.disconnect();
            } catch (Exception e) {
                log.error("Error stopping websocket client: {}", e.getMessage());
            }
        }
    }

    /**
     * Disable SSL certificate verification (for development/testing)
     * ⚠️ WARNING: Only use for development/testing with trusted self-signed certs!
     */
    private static void disableSSLVerification() {
        try {
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

            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

            log.debug("⚠️ SSL verification disabled (for development)");
        } catch (Exception e) {
            log.error("Failed to disable SSL verification: {}", e.getMessage());
        }
    }
}