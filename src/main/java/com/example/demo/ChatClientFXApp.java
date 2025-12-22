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
import com.example.demo.client.model.RecallResponse;

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

            // Setup avatar change callback to update sidebar avatar
            SettingsDialog.setAvatarChangeCallback(newAvatarUrl -> {
                if (sidebar != null && currentUsername != null) {
                    Platform.runLater(() -> sidebar.setCurrentUserAvatar(newAvatarUrl, currentUsername));
                }
            });

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

        // Join public room handler
        sidebar.setOnJoinPublicRoom(roomId -> {
            boolean success = chatService.joinRoom(roomId);
            if (success) {
                // Immediately subscribe to the new room for notifications
                if (webSocketClient != null && webSocketClient.isConnected()) {
                    webSocketClient.subscribeToRoom(roomId, this::handleIncomingMessage);
                    webSocketClient.subscribeToRoomRecall(roomId, this::handleMessageRecall);
                    log.info("✅ Subscribed to newly joined room: {}", roomId);
                }

                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Thành công");
                    alert.setHeaderText(null);
                    alert.setContentText("Đã tham gia phòng!");
                    alert.show();
                    loadRooms(); // Refresh my rooms
                    loadPublicRooms(); // Refresh public rooms (remove joined room)
                });
            } else {
                Platform.runLater(() -> showError("Lỗi", "Không thể tham gia phòng"));
            }
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
                sidebar.applyDarkMode(true);
            } else {
                // Remove dark-theme class from all containers
                root.getStyleClass().remove("dark-theme");
                sidebar.getStyleClass().remove("dark-theme");
                contentArea.getStyleClass().remove("dark-theme");
                root.setStyle("-fx-background-color: #f0f2f5;");
                contentArea.applyDarkMode(false);
                sidebar.applyDarkMode(false);
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
        dialog.setOnBadgeUpdate(() -> {
            loadInitialBadgeCounts(); // Reload badge counts when accept/decline
        });
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
        dialog.setOnBadgeUpdate(() -> {
            loadInitialBadgeCounts(); // Reload badge counts when accept/decline
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
            // 📨 Clear unread badge for this friend (UI) and mark as read on backend
            sidebar.clearUnreadCount(friend.getId());

            // Mark all private messages from this friend as read on backend
            // This ensures the unread count is reset on the server side
            new Thread(() -> {
                chatService.markAllPrivateMessagesAsRead(friend.getId());
            }).start();

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

                    // Check if this is a file message
                    if (msg.getMessageType() == ChatMessage.MessageType.FILE ||
                            msg.getMessageType() == ChatMessage.MessageType.IMAGE) {
                        // Pass message ID for recall functionality
                        contentArea.addFileMessage(msg.getId(), displayName, msg.getFileName(), msg.getContent(),
                                msg.getTimestamp(), isMine, msg.isRecalled());
                    } else {
                        // Pass message ID for recall functionality
                        contentArea.addMessage(msg.getId(), displayName, msg.getContent(), msg.getTimestamp(), isMine,
                                msg.isRecalled());
                    }
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

            // Handle local UI update for recalled message or new incoming message
            javafx.application.Platform.runLater(() -> {
                boolean isMine = message.getSenderId().equals(currentUserId);
                String displayName = isMine ? currentUsername
                        : (expectedUser.getDisplayName() != null ? expectedUser.getDisplayName()
                                : expectedUser.getUsername());

                if (message.isRecalled()) {
                    contentArea.updateMessageAsRecalled(message.getId());
                } else {
                    // Add ALL messages from server (both mine and others) - server provides the ID
                    // Check if this is a file message
                    if (message.getMessageType() == ChatMessage.MessageType.FILE ||
                            message.getMessageType() == ChatMessage.MessageType.IMAGE) {
                        contentArea.addFileMessage(message.getId(), displayName, message.getFileName(),
                                message.getContent(),
                                message.getTimestamp(), isMine, false);
                    } else {
                        contentArea.addMessage(message.getId(), displayName, message.getContent(),
                                message.getTimestamp(), isMine, false);
                    }
                }
            });
        }
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
            contentArea.setChatService(chatService);
            webSocketClient = new WebSocketClient(serverUrl);

            // Configure AvatarUtils with server base URL
            com.example.demo.util.AvatarUtils.setBaseUrl(serverUrl);

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

        // Use a holder for login result
        final boolean[] loginSuccess = { false };

        // Handle login button click with event filter to prevent dialog from closing on
        // error
        Button loginButton = (Button) dialog.getDialogPane().lookupButton(loginButtonType);
        loginButton.addEventFilter(ActionEvent.ACTION, event -> {
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
                    webSocketClient.registerSession(); // Register user session after connecting

                    // Subscribe to default room messages
                    webSocketClient.subscribeToRoom(1L, this::handleIncomingMessage);

                    // Subscribe to notifications
                    webSocketClient.subscribeToFriendRequests(this::handleFriendRequestNotification);
                    webSocketClient.subscribeToRoomInvites(this::handleRoomInviteNotification);
                    webSocketClient.subscribeToPrivateMessages(this::handlePrivateMessageNotification);
                    webSocketClient.subscribeToUserStatus(this::handleUserStatusUpdate);
                    webSocketClient.subscribeToMessageRecall(this::handleMessageRecall);
                    webSocketClient.subscribeToRoomEvents(this::handleRoomEvent);

                    // Update UI status
                    contentArea.setOnlineStatus(true);
                    sidebar.setCurrentUser(currentUsername);
                    // Load user avatar on sidebar
                    if (currentUser != null) {
                        sidebar.setCurrentUserAvatar(currentUser.getAvatarUrl(), currentUsername);
                    }

                    // Load initial badge counts from backend
                    loadInitialBadgeCounts();

                    // Load public rooms for sidebar
                    loadPublicRooms();

                    loginSuccess[0] = true;
                    // Allow dialog to close
                } else {
                    // Show error and prevent dialog from closing
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("❌ Đăng nhập thất bại");
                    alert.setHeaderText(null);
                    alert.setContentText("Tên đăng nhập hoặc mật khẩu không đúng.");
                    alert.initOwner(dialog.getDialogPane().getScene().getWindow());
                    alert.showAndWait();
                    event.consume();
                }
            } catch (Exception e) {
                log.error("Login error: {}", e.getMessage());
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("❌ Đăng nhập thất bại");
                alert.setHeaderText(null);
                alert.setContentText("Lỗi kết nối: " + e.getMessage());
                alert.initOwner(dialog.getDialogPane().getScene().getWindow());
                alert.showAndWait();
                event.consume();
            }
        });

        // Handle register button click
        Button registerButton = (Button) dialog.getDialogPane().lookupButton(registerButtonType);
        registerButton.addEventFilter(ActionEvent.ACTION, event -> {
            // First show the display name field if not visible
            if (!displayName.isVisible()) {
                displayName.setVisible(true);
                displayNameLabel.setVisible(true);
                dialog.setHeaderText("📝 Tạo tài khoản mới");
                event.consume(); // Prevent dialog from closing, just show fields
                return;
            }

            // Otherwise, attempt registration
            try {
                boolean success = chatService.register(username.getText(), password.getText(), displayName.getText());
                if (success) {
                    // Show success message and reset for login
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("✅ Đăng ký thành công");
                    successAlert.setHeaderText(null);
                    successAlert.setContentText("Đăng ký thành công! Vui lòng đăng nhập.");
                    successAlert.initOwner(dialog.getDialogPane().getScene().getWindow());
                    successAlert.showAndWait();

                    // Reset to login mode
                    displayName.setVisible(false);
                    displayNameLabel.setVisible(false);
                    displayName.clear();
                    dialog.setHeaderText("Đăng nhập vào WebChat");
                    event.consume(); // Stay for login
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("❌ Đăng ký thất bại");
                    alert.setHeaderText(null);
                    alert.setContentText("Không thể đăng ký tài khoản.");
                    alert.initOwner(dialog.getDialogPane().getScene().getWindow());
                    alert.showAndWait();
                    event.consume();
                }
            } catch (Exception e) {
                log.error("Register error: {}", e.getMessage());
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("❌ Đăng ký thất bại");
                alert.setHeaderText(null);
                alert.setContentText("Lỗi kết nối: " + e.getMessage());
                alert.initOwner(dialog.getDialogPane().getScene().getWindow());
                alert.showAndWait();
                event.consume();
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (loginSuccess[0]) {
            // Logged in successfully - load rooms
            appendMessage("✅ Đăng nhập thành công!");
            loadRooms();
        } else {
            // Not logged in, close app
            // Use runLater to exit AFTER dialog has fully closed to avoid
            // "Key not associated with a running event loop" error
            Platform.runLater(() -> {
                if (webSocketClient != null) {
                    try {
                        webSocketClient.disconnect();
                    } catch (Exception ignored) {
                    }
                }
                Platform.exit();
                System.exit(0);
            });
        }
    }

    private void loadRooms() {
        try {
            // Load rooms from ChatService - filter out auto-created private chat rooms
            // (PRIVATE_*)
            loadedRooms = chatService.getMyRooms().stream()
                    .filter(room -> room.getName() == null || !room.getName().startsWith("PRIVATE_"))
                    .collect(java.util.stream.Collectors.toList());
            if (loadedRooms != null && !loadedRooms.isEmpty()) {
                // Load rooms into sidebar
                sidebar.loadRoomsFromChatRooms(loadedRooms);
                // Load rooms into content area
                contentArea.loadRoomsFromChatRooms(loadedRooms);

                // Subscribe to ALL rooms for notifications
                if (webSocketClient != null && webSocketClient.isConnected()) {
                    for (ChatRoom room : loadedRooms) {
                        // Subscribe to each room for messages
                        webSocketClient.subscribeToRoom(room.getId(), this::handleIncomingMessage);
                        // Subscribe to room recall notifications
                        webSocketClient.subscribeToRoomRecall(room.getId(), this::handleMessageRecall);
                        log.info("✅ Subscribed to room for notifications: {} (ID: {})", room.getName(), room.getId());
                    }

                    // Set first room as current and load its messages
                    ChatRoom firstRoom = loadedRooms.get(0);
                    currentRoomId = firstRoom.getId();

                    // Load message history for first room
                    List<ChatMessage> messages = chatService.getRoomMessages(currentRoomId);
                    roomMessages.put(currentRoomId, messages);
                    contentArea.clearMessages();
                    contentArea.addMessages(messages, currentUsername);
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
        log.info("🔄 Starting loadOnlineUsers (friends only)...");
        try {
            // Get friends list and filter to only show online friends
            List<java.util.Map<String, Object>> friendsData = chatService.getFriends();
            List<User> onlineFriends = new ArrayList<>();

            if (friendsData != null) {
                for (java.util.Map<String, Object> data : friendsData) {
                    Long friendId = Long.valueOf(data.get("id").toString());
                    // Skip current user
                    if (currentUserId != null && friendId.equals(currentUserId)) {
                        continue;
                    }

                    // Check if friend is online and allows showing status
                    String status = data.get("status") != null ? data.get("status").toString() : "OFFLINE";
                    Object showOnlineStatusObj = data.get("showOnlineStatus");
                    boolean showOnlineStatus = showOnlineStatusObj == null || Boolean.TRUE.equals(showOnlineStatusObj);

                    // Only add to online list if they are online AND allow showing status
                    if ("ONLINE".equals(status) && showOnlineStatus) {
                        User friend = new User();
                        friend.setId(friendId);
                        friend.setUsername((String) data.get("username"));
                        friend.setDisplayName((String) data.get("displayName"));
                        friend.setAvatarUrl((String) data.get("avatarUrl"));
                        friend.setStatus(User.Status.ONLINE);
                        friend.setShowOnlineStatus(showOnlineStatus);
                        onlineFriends.add(friend);
                    }
                }
            }

            sidebar.loadOnlineUsers(onlineFriends);
            log.info("✅ Online friends loaded to sidebar: {} friends online", onlineFriends.size());
        } catch (Exception e) {
            log.error("❌ Error loading online friends", e);
            appendMessage("❌ Lỗi khi tải danh sách bạn bè online: " + e.getMessage());
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
                    Long friendId = Long.valueOf(data.get("id").toString());
                    // Skip current user - don't show yourself in friends list
                    if (currentUserId != null && friendId.equals(currentUserId)) {
                        continue;
                    }
                    User friend = new User();
                    friend.setId(friendId);
                    friend.setUsername((String) data.get("username"));
                    friend.setDisplayName((String) data.get("displayName"));
                    friend.setAvatarUrl((String) data.get("avatarUrl"));
                    friend.setStatus(data.get("status") != null ? User.Status.valueOf(data.get("status").toString())
                            : User.Status.OFFLINE);
                    // Set showOnlineStatus from backend data
                    Object showOnlineStatusObj = data.get("showOnlineStatus");
                    friend.setShowOnlineStatus(showOnlineStatusObj == null || Boolean.TRUE.equals(showOnlineStatusObj));
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
                    // 📨 Clear unread badge for this room (UI) and mark as read on backend
                    sidebar.clearRoomUnreadCount(targetRoom.getId());

                    // Mark all messages in this room as read on backend
                    // This ensures unread count is reset on server side
                    final Long roomIdToMark = targetRoom.getId();
                    new Thread(() -> {
                        chatService.markAllMessagesInRoomAsRead(roomIdToMark);
                    }).start();

                    // Fetch message history for the room
                    List<ChatMessage> messages = chatService.getRoomMessages(targetRoom.getId());
                    roomMessages.put(targetRoom.getId(), messages);

                    // Clear current messages and display room messages
                    contentArea.clearMessages();
                    contentArea.addMessages(messages, currentUsername);

                    // Update current room ID (no need to unsubscribe/subscribe - already subscribed
                    // to all rooms)
                    currentRoomId = targetRoom.getId();
                    appendMessage("✅ Đã chuyển sang phòng: " + targetRoom.getName());

                    // Update content area room selector
                    contentArea.getRoomSelector().setValue(roomName);
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
                        // Send private message - server will echo back with ID via WebSocket
                        User privateChatUser = contentArea.getPrivateChatUser();
                        webSocketClient.sendPrivateMessage(privateChatUser.getId(), message);
                        // Message will appear via handlePrivateChatMessage callback with proper ID
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
                if (webSocketClient != null && webSocketClient.isConnected()) {
                    // Show loading
                    contentArea.getFileButton().setDisable(true);
                    contentArea.getFileButton().setText("⏳ Đang tải...");

                    // Check if we're in private chat mode
                    if (contentArea.isPrivateMode() && contentArea.getPrivateChatUser() != null) {
                        // PRIVATE CHAT FILE UPLOAD
                        User privateChatUser = contentArea.getPrivateChatUser();

                        // Upload file to private endpoint
                        String fileUrl = chatService.uploadPrivateFile(
                                privateChatUser.getId(),
                                selectedFile.getAbsolutePath());

                        if (fileUrl != null && !fileUrl.isEmpty()) {
                            // Success - message will appear via WebSocket callback from server broadcast
                            // Don't add locally to avoid duplicate (server broadcasts file message)
                            appendMessage("✅ Đã gửi file: " + selectedFile.getName());
                            log.info("📎 Sent private file: {} to user {}", selectedFile.getName(),
                                    privateChatUser.getId());
                        } else {
                            appendMessage("❌ Lỗi tải lên file");
                        }
                    } else if (currentRoomId != null) {
                        // ROOM CHAT FILE UPLOAD
                        // REST API saves message to DB and broadcasts via WebSocket automatically
                        // No need to send WebSocket message manually - it will arrive via
                        // handleIncomingMessage
                        String fileUrl = chatService.uploadFile(currentRoomId, selectedFile.getAbsolutePath());

                        if (fileUrl != null) {
                            // Success - message will appear via WebSocket callback from server broadcast
                            appendMessage("✅ Đã gửi file: " + selectedFile.getName());
                        } else {
                            appendMessage("❌ Lỗi tải lên file");
                        }
                    } else {
                        contentArea.addMessage("System", "❌ Chưa chọn phòng hoặc người nhận",
                                java.time.LocalDateTime.now());
                    }
                } else {
                    contentArea.addMessage("System", "❌ Chưa kết nối đến server",
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
        if (message != null && message.getRoomId() != null) {
            // Check if message already exists (prevent duplicates/handle updates)
            List<ChatMessage> messages = roomMessages.computeIfAbsent(message.getRoomId(), k -> new ArrayList<>());

            ChatMessage existing = messages.stream()
                    .filter(m -> m.getId() != null && m.getId().equals(message.getId()))
                    .findFirst()
                    .orElse(null);

            if (existing != null) {
                // Update existing message (e.g., recalled status)
                existing.setRecalled(message.isRecalled());
                if (message.isRecalled() && message.getRoomId().equals(currentRoomId)) {
                    contentArea.updateMessageAsRecalled(message.getId());
                }
                return;
            }

            // Store new message in room messages
            messages.add(message);

            // Only display if it's the current room
            if (message.getRoomId().equals(currentRoomId)) {
                Platform.runLater(() -> {
                    String displayName = message.getSenderDisplayName() != null ? message.getSenderDisplayName()
                            : message.getSenderUsername();

                    boolean isMine = message.getSenderUsername() != null
                            && message.getSenderUsername().equals(currentUsername);

                    if (message.isRecalled()) {
                        contentArea.addMessage(message.getId(), displayName, null, message.getTimestamp(), isMine,
                                true);
                    } else if (message.getMessageType() == ChatMessage.MessageType.FILE) {
                        contentArea.addFileMessage(message.getId(), displayName, message.getFileName(),
                                message.getContent(),
                                message.getTimestamp(), isMine, false);
                    } else {
                        contentArea.addMessage(message.getId(), displayName, message.getContent(),
                                message.getTimestamp(), isMine, false);
                    }
                });
            }

            // Show notification for messages from others
            // - Skip if focused AND viewing the message's room (already seeing it)
            // - Show for other rooms (in-app toast if focused, desktop if not)
            // - Show for current room if not focused (desktop notification)
            if (message.getSenderId() != null && !message.getSenderId().equals(currentUserId)) {
                boolean isCurrentRoom = message.getRoomId().equals(currentRoomId);
                boolean isFocused = notificationService.isWindowFocused();
                boolean isInPrivateMode = contentArea.isPrivateMode();

                // 📨 Increment room unread count if NOT viewing this room
                // (only when NOT in private chat mode)
                if (!isCurrentRoom || isInPrivateMode) {
                    Platform.runLater(() -> {
                        sidebar.incrementRoomUnreadCount(message.getRoomId());
                    });
                }

                // Skip notification only when focused AND it's the current room AND not in
                // private mode
                if (isCurrentRoom && isFocused && !isInPrivateMode) {
                    // User is already looking at this room, no need to notify
                    return;
                }

                String displayName = message.getSenderDisplayName() != null
                        ? message.getSenderDisplayName()
                        : message.getSenderUsername();

                // Include room name in notification if from different room
                String notificationTitle = displayName;
                if (!isCurrentRoom && loadedRooms != null) {
                    ChatRoom sourceRoom = loadedRooms.stream()
                            .filter(r -> r.getId().equals(message.getRoomId()))
                            .findFirst()
                            .orElse(null);
                    if (sourceRoom != null) {
                        notificationTitle = displayName + " (📢 " + sourceRoom.getName() + ")";
                    }
                }
                notificationService.showMessageNotification(notificationTitle, message.getContent());
            }
        }
    }

    /**
     * Handle friend request notification - handles new requests, accepts, and
     * removals
     */
    private void handleFriendRequestNotification(FriendRequestNotification notification) {
        if (notification == null)
            return;

        String eventType = notification.getEventType();
        log.info("👋 Friend notification received: eventType={}, from={}", eventType, notification.getSenderUsername());

        Platform.runLater(() -> {
            if ("REQUEST_SENT".equals(eventType)) {
                // New friend request received
                String displayName = notification.getDisplayName();
                notificationService.showFriendRequestNotification(displayName);
                sidebar.incrementFriendRequestBadge();
                log.info("👋 New friend request notification shown for: {}", displayName);

            } else if ("REQUEST_ACCEPTED".equals(eventType)) {
                // Someone accepted our friend request - refresh friend list
                log.info("✅ Friend request accepted - refreshing friend list");
                loadFriends();
                loadOnlineUsers();
                // Show notification to user
                String displayName = notification.getDisplayName();
                appendMessage("✅ " + displayName + " đã chấp nhận lời mời kết bạn của bạn!");

            } else if ("FRIEND_REMOVED".equals(eventType)) {
                // Someone removed us as a friend - refresh friend list
                log.info("❌ Friend removed - refreshing friend list");
                loadFriends();
                loadOnlineUsers();
                // Optionally show notification
                String displayName = notification.getDisplayName();
                if (displayName != null && !displayName.isEmpty()) {
                    log.info("❌ {} đã hủy kết bạn với bạn", displayName);
                }

            } else {
                // Default handling for other/unknown event types
                String displayName = notification.getDisplayName();
                notificationService.showFriendRequestNotification(displayName);
                sidebar.incrementFriendRequestBadge();
            }
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
     * Load initial badge counts from backend (pending friend requests and room
     * invites)
     */
    private void loadInitialBadgeCounts() {
        try {
            // Load pending friend requests count
            var pendingFriendRequests = chatService.getPendingRequests();
            int friendRequestCount = pendingFriendRequests != null ? pendingFriendRequests.size() : 0;
            log.info("📊 Pending friend requests from backend: {} items", friendRequestCount);
            if (pendingFriendRequests != null && !pendingFriendRequests.isEmpty()) {
                for (var req : pendingFriendRequests) {
                    log.info("   - Request: {}", req);
                }
            }

            // Load pending room invites count
            var pendingRoomInvites = chatService.getPendingRoomInvites();
            int roomInviteCount = pendingRoomInvites != null ? pendingRoomInvites.size() : 0;
            log.info("📊 Pending room invites from backend: {} items (User: {})", roomInviteCount, currentUsername);
            if (pendingRoomInvites != null && !pendingRoomInvites.isEmpty()) {
                for (var inv : pendingRoomInvites) {
                    log.info("   - Invite: roomName={}, inviterId={}, inviteeId={}",
                            inv.get("roomName"),
                            inv.get("inviter") != null ? ((java.util.Map) inv.get("inviter")).get("id") : "null",
                            inv.get("invitee") != null ? ((java.util.Map) inv.get("invitee")).get("id") : "null");
                }
            }

            // Update sidebar badges
            Platform.runLater(() -> {
                sidebar.setFriendRequestBadgeCount(friendRequestCount);
                sidebar.setRoomInviteBadgeCount(roomInviteCount);
                log.info("📊 Set badge counts - Friends: {}, Room invites: {} (calling sidebar update)",
                        friendRequestCount, roomInviteCount);
            });

            // 📨 Load unread message counts for friends
            loadUnreadMessageCounts();

        } catch (Exception e) {
            log.error("Failed to load initial badge counts: " + e.getMessage());
        }
    }

    /**
     * Load unread message counts for all friends and rooms, update sidebar badges
     */
    private void loadUnreadMessageCounts() {
        try {
            // Load friend unread counts
            List<java.util.Map<String, Object>> friendsData = chatService.getFriends();
            if (friendsData != null && !friendsData.isEmpty()) {
                for (java.util.Map<String, Object> data : friendsData) {
                    Long friendId = Long.valueOf(data.get("id").toString());
                    // Skip current user
                    if (currentUserId != null && friendId.equals(currentUserId)) {
                        continue;
                    }

                    // Get unread count from backend
                    int unreadCount = chatService.getUnreadPrivateMessageCount(friendId);
                    if (unreadCount > 0) {
                        final Long fId = friendId;
                        final int count = unreadCount;
                        Platform.runLater(() -> {
                            sidebar.setUnreadCount(fId, count);
                        });
                        log.debug("📨 Friend {} has {} unread messages", friendId, unreadCount);
                    }
                }
                Platform.runLater(() -> {
                    sidebar.refreshUnreadBadges();
                });
                log.info("📨 Loaded unread message counts for {} friends", friendsData.size());
            }

            // Load room unread counts
            if (loadedRooms != null && !loadedRooms.isEmpty()) {
                for (ChatRoom room : loadedRooms) {
                    int roomUnreadCount = chatService.getUnreadMessageCount(room.getId());
                    if (roomUnreadCount > 0) {
                        final Long roomId = room.getId();
                        final int count = roomUnreadCount;
                        Platform.runLater(() -> {
                            sidebar.setRoomUnreadCount(roomId, count);
                        });
                        log.debug("🏠 Room {} has {} unread messages", room.getName(), roomUnreadCount);
                    }
                }
                Platform.runLater(() -> {
                    sidebar.refreshRoomsList();
                });
                log.info("🏠 Loaded unread message counts for {} rooms", loadedRooms.size());
            }
        } catch (Exception e) {
            log.error("Failed to load unread message counts: " + e.getMessage());
        }
    }

    /**
     * Load public rooms into sidebar
     */
    private void loadPublicRooms() {
        try {
            var publicRooms = chatService.getPublicRooms();
            Platform.runLater(() -> {
                sidebar.loadPublicRooms(publicRooms);
                log.info("🌐 Loaded {} public rooms", publicRooms != null ? publicRooms.size() : 0);
            });
        } catch (Exception e) {
            log.error("Failed to load public rooms: " + e.getMessage());
        }
    }

    /**
     * Handle room events (ROOM_CREATED, ROOM_DELETED) for real-time updates
     */
    @SuppressWarnings("unchecked")
    private void handleRoomEvent(Map<String, Object> event) {
        if (event == null)
            return;

        String eventType = (String) event.get("type");
        log.info("🏠 Handling room event: {}", eventType);

        Platform.runLater(() -> {
            try {
                if ("ROOM_CREATED".equals(eventType)) {
                    // New room created - refresh public rooms list
                    Map<String, Object> roomData = (Map<String, Object>) event.get("room");
                    if (roomData != null) {
                        Boolean isPrivate = (Boolean) roomData.get("isPrivate");
                        String roomName = (String) roomData.get("name");

                        // Only refresh for public rooms
                        if (isPrivate == null || !isPrivate) {
                            log.info("🌐 New public room created: {}, refreshing list...", roomName);
                            loadPublicRooms();
                            appendMessage("🆕 Phòng mới được tạo: " + roomName);
                        }
                    }

                } else if ("ROOM_DELETED".equals(eventType)) {
                    // Room deleted - refresh lists
                    Object roomIdObj = event.get("roomId");
                    Long deletedRoomId = null;
                    if (roomIdObj instanceof Number) {
                        deletedRoomId = ((Number) roomIdObj).longValue();
                    }

                    log.info("🗑️ Room {} deleted, refreshing lists...", deletedRoomId);
                    loadPublicRooms();

                    // If deleted room is in our loaded rooms, remove it
                    if (loadedRooms != null && deletedRoomId != null) {
                        final Long finalDeletedRoomId = deletedRoomId;
                        loadedRooms.removeIf(room -> room.getId().equals(finalDeletedRoomId));
                        sidebar.loadRoomsFromChatRooms(loadedRooms);

                        // If we were in the deleted room, switch to first available room
                        if (currentRoomId != null && currentRoomId.equals(finalDeletedRoomId)) {
                            if (!loadedRooms.isEmpty()) {
                                ChatRoom firstRoom = loadedRooms.get(0);
                                currentRoomId = firstRoom.getId();
                                var messages = chatService.getRoomMessages(currentRoomId);
                                contentArea.clearMessages();
                                contentArea.addMessages(messages, currentUsername);
                                appendMessage("⚠️ Phòng bạn đang xem đã bị xóa. Đã chuyển sang phòng: "
                                        + firstRoom.getName());
                            } else {
                                contentArea.clearMessages();
                                appendMessage("⚠️ Phòng bạn đang xem đã bị xóa.");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error handling room event: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Handle private message notification (for notifications AND display)
     */
    private void handlePrivateMessageNotification(ChatMessage message) {
        if (message == null)
            return;

        // Check if we're viewing a private chat
        User privateChatUser = contentArea.getPrivateChatUser();
        boolean isInPrivateMode = contentArea.isPrivateMode() && privateChatUser != null;

        // Check if this message is for the current private chat
        boolean isForCurrentChat = false;
        if (isInPrivateMode) {
            // Message from the user we're chatting with, sent to us
            boolean isFromChatPartner = message.getSenderId().equals(privateChatUser.getId())
                    && message.getRecipientId() != null
                    && message.getRecipientId().equals(currentUserId);
            // Message from us, sent to the user we're chatting with
            boolean isFromMe = message.getSenderId().equals(currentUserId)
                    && message.getRecipientId() != null
                    && message.getRecipientId().equals(privateChatUser.getId());
            isForCurrentChat = isFromChatPartner || isFromMe;
        }

        // Display message in UI if we're in the relevant private chat
        if (isForCurrentChat) {
            Platform.runLater(() -> {
                boolean isMine = message.getSenderId().equals(currentUserId);
                String displayName = isMine ? currentUsername
                        : (privateChatUser.getDisplayName() != null ? privateChatUser.getDisplayName()
                                : privateChatUser.getUsername());

                if (message.isRecalled()) {
                    contentArea.updateMessageAsRecalled(message.getId());
                } else {
                    // Check if this is a file message
                    if (message.getMessageType() == ChatMessage.MessageType.FILE ||
                            message.getMessageType() == ChatMessage.MessageType.IMAGE) {
                        contentArea.addFileMessage(message.getId(), displayName, message.getFileName(),
                                message.getContent(), message.getTimestamp(), isMine, false);
                    } else {
                        contentArea.addMessage(message.getId(), displayName, message.getContent(),
                                message.getTimestamp(), isMine, false);
                    }
                }
            });
        }

        // Skip notification for own messages
        if (message.getSenderId().equals(currentUserId)) {
            return;
        }

        // Check if we're viewing this specific private chat
        boolean isViewingThisChat = isInPrivateMode
                && privateChatUser.getId().equals(message.getSenderId());

        // Only increment unread if NOT viewing this chat
        // (if viewing, user already sees the message - no need to count)
        if (!isViewingThisChat) {
            Platform.runLater(() -> {
                sidebar.incrementUnreadCount(message.getSenderId());
            });
        }

        // Show notification if not viewing this chat OR window not focused
        if (!isViewingThisChat || !notificationService.isWindowFocused()) {
            String displayName = message.getSenderDisplayName() != null
                    ? message.getSenderDisplayName()
                    : message.getSenderUsername();
            notificationService.showMessageNotification(displayName, message.getContent());
        }
    }

    /**
     * Handle user status update (online/offline) in real-time
     */
    private void handleUserStatusUpdate(com.example.demo.client.model.UserStatusMessage statusMessage) {
        if (statusMessage == null)
            return;

        // Skip status updates for current user - don't show yourself
        if (statusMessage.getUserId() != null && statusMessage.getUserId().equals(currentUserId)) {
            log.debug("👥 Ignoring status update for self");
            return;
        }

        boolean isOnline = "ONLINE".equals(statusMessage.getStatus())
                || Boolean.TRUE.equals(statusMessage.getIsOnline());
        log.info("👥 User status update: {} (id={}) is now {}",
                statusMessage.getUsername(), statusMessage.getUserId(), isOnline ? "ONLINE" : "OFFLINE");

        Platform.runLater(() -> {
            // Update friend status in sidebar Direct Messages list
            sidebar.updateFriendStatus(statusMessage.getUserId(), isOnline);

            // Reload online users and friends to get fresh showOnlineStatus from backend
            // This ensures Online Now tab and DM list respect privacy settings correctly
            loadOnlineUsers();
            loadFriends();
        });
    }

    /**
     * 🔙 Handle message recall notification from WebSocket
     */
    private void handleMessageRecall(RecallResponse recallResponse) {
        if (recallResponse == null || recallResponse.getMessageId() == null)
            return;

        log.info("🔙 Received recall for message: {}", recallResponse.getMessageId());

        Platform.runLater(() -> {
            // Update UI to show recalled message
            contentArea.updateMessageAsRecalled(recallResponse.getMessageId());

            // Also update in local message storage if present
            if (recallResponse.getRoomId() != null) {
                List<ChatMessage> messages = roomMessages.get(recallResponse.getRoomId());
                if (messages != null) {
                    for (ChatMessage msg : messages) {
                        if (msg.getId() != null && msg.getId().equals(recallResponse.getMessageId())) {
                            msg.setRecalled(true);
                            break;
                        }
                    }
                }
            }
        });
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