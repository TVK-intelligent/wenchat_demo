package com.example.demo.ui;

import com.example.demo.client.model.ChatMessage;
import com.example.demo.client.model.User;
import com.example.demo.client.service.ChatService;
import com.example.demo.client.websocket.WebSocketClient;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.effect.DropShadow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Modern Private Chat Dialog - Direct messaging between users
 */
@Slf4j
public class PrivateChatDialog extends Stage {

    private final ChatService chatService;
    private final WebSocketClient webSocketClient;
    private final User targetUser;
    private final User currentUser;

    // UI Components
    private ListView<HBox> messageListView;
    private TextField inputField;
    private Button sendButton;
    private Button fileButton;
    private Label statusLabel;
    private Circle onlineIndicator;

    // Message storage
    private List<ChatMessage> privateMessages = new ArrayList<>();
    // Map to track message bubbles for recall updates
    private Map<Long, VBox> messageBubbles = new HashMap<>();

    // Avatar colors
    private static final Color[] AVATAR_COLORS = {
            Color.web("#667eea"), Color.web("#764ba2"), Color.web("#f093fb"),
            Color.web("#f5576c"), Color.web("#4facfe"), Color.web("#43e97b"),
            Color.web("#fa709a"), Color.web("#30cfd0")
    };

    public PrivateChatDialog(ChatService chatService, WebSocketClient webSocketClient,
            User currentUser, User targetUser) {
        this.chatService = chatService;
        this.webSocketClient = webSocketClient;
        this.currentUser = currentUser;
        this.targetUser = targetUser;

        initModality(Modality.NONE); // Allow multiple private chats
        setTitle("💬 " + targetUser.getDisplayName());
        setWidth(500);
        setHeight(600);
        setResizable(true);

        initComponents();
        loadPrivateMessages();
        setupEventHandlers();

        Scene scene = new Scene(createLayout());
        if (getClass().getResource("/styles.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        }
        setScene(scene);
    }

    private void initComponents() {
        messageListView = new ListView<>();
        messageListView.setStyle("-fx-background-color: transparent;");
        messageListView.setCellFactory(param -> new MessageCell());

        inputField = new TextField();
        inputField.setPromptText("Nhập tin nhắn...");
        inputField.setStyle(
                "-fx-background-color: #f0f2f5; -fx-border-color: #dee2e6; " +
                        "-fx-border-radius: 25; -fx-background-radius: 25; -fx-padding: 12 20;");

        sendButton = Sidebar.createBeautifulButton("➤", "", "#667eea");
        sendButton.setMinWidth(45);
        sendButton.setMaxWidth(45);
        sendButton.setMinHeight(45);
        sendButton.setMaxHeight(45);

        fileButton = Sidebar.createBeautifulButton("📎", "", "#f0f2f5");
        fileButton.setMinWidth(40);
        fileButton.setMaxWidth(40);
        fileButton.setMinHeight(40);
        fileButton.setMaxHeight(40);
        fileButton.setStyle(fileButton.getStyle() + "-fx-text-fill: #667eea;");

        statusLabel = new Label("Online");
        statusLabel.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 11px;");

        onlineIndicator = new Circle(5);
        onlineIndicator.setFill(Color.web("#4ade80"));
        onlineIndicator.setEffect(new DropShadow(4, Color.web("#4ade8060")));
    }

    private void setupEventHandlers() {
        inputField.setOnAction(e -> sendPrivateMessage());
        sendButton.setOnAction(e -> sendPrivateMessage());
        fileButton.setOnAction(e -> sendPrivateFile());

        // Subscribe to private messages
        if (webSocketClient != null && webSocketClient.isConnected()) {
            webSocketClient.subscribeToPrivateMessages(this::handleIncomingPrivateMessage);
        }
    }

    private BorderPane createLayout() {
        BorderPane mainLayout = new BorderPane();
        mainLayout.setStyle("-fx-background-color: #f0f2f5;");

        // Header
        mainLayout.setTop(createHeader());

        // Message area
        VBox messageArea = new VBox();
        messageArea.setStyle("-fx-background-color: #f0f2f5;");
        VBox.setVgrow(messageListView, Priority.ALWAYS);
        messageArea.getChildren().add(messageListView);
        mainLayout.setCenter(messageArea);

        // Input area
        mainLayout.setBottom(createInputArea());

        return mainLayout;
    }

    private HBox createHeader() {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(15, 20, 15, 20));
        header.setStyle(
                "-fx-background-color: white; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 2);");

        // Avatar
        StackPane avatarPane = new StackPane();
        Circle avatar = new Circle(22);
        int hash = Math.abs((targetUser.getUsername() != null ? targetUser.getUsername() : "").hashCode());
        avatar.setFill(AVATAR_COLORS[hash % AVATAR_COLORS.length]);
        avatar.setEffect(new DropShadow(4, Color.web("#00000020")));

        String initial = targetUser.getDisplayName() != null && targetUser.getDisplayName().length() > 0
                ? targetUser.getDisplayName().substring(0, 1).toUpperCase()
                : "?";
        Label initialLabel = new Label(initial);
        initialLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        avatarPane.getChildren().addAll(avatar, initialLabel);

        // User info
        VBox userInfo = new VBox(2);
        Label userLabel = new Label(
                targetUser.getDisplayName() != null ? targetUser.getDisplayName() : targetUser.getUsername());
        userLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #212529;");

        HBox statusBox = new HBox(5);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        statusBox.getChildren().addAll(onlineIndicator, statusLabel);

        userInfo.getChildren().addAll(userLabel, statusBox);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Close button
        Button closeButton = Sidebar.createBeautifulButton("✕", "", "#f0f2f5");
        closeButton.setMinWidth(35);
        closeButton.setMaxWidth(35);
        closeButton.setMinHeight(35);
        closeButton.setMaxHeight(35);
        closeButton.setStyle(closeButton.getStyle() + "-fx-text-fill: #6c757d;");
        closeButton.setOnAction(e -> hide());

        header.getChildren().addAll(avatarPane, userInfo, spacer, closeButton);
        return header;
    }

    private HBox createInputArea() {
        HBox inputArea = new HBox(10);
        inputArea.setAlignment(Pos.CENTER);
        inputArea.setPadding(new Insets(15, 20, 15, 20));
        inputArea.setStyle(
                "-fx-background-color: white; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, -2);");

        HBox.setHgrow(inputField, Priority.ALWAYS);
        inputArea.getChildren().addAll(fileButton, inputField, sendButton);

        return inputArea;
    }

    private void loadPrivateMessages() {
        try {
            // Load message history from server
            List<ChatMessage> messages = chatService.getPrivateMessages(targetUser.getId());

            if (messages != null && !messages.isEmpty()) {
                privateMessages.addAll(messages);
                for (ChatMessage message : messages) {
                    addMessageToView(message);
                }
                log.info("Loaded {} private messages with {}", messages.size(), targetUser.getUsername());
            } else {
                log.info("No previous messages with {}", targetUser.getUsername());
            }

            statusLabel.setText("Online");
        } catch (Exception e) {
            log.error("Error loading private messages", e);
            statusLabel.setText("Offline");
            statusLabel.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11px;");
            onlineIndicator.setFill(Color.web("#9ca3af"));
        }
    }

    private void sendPrivateMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            try {
                if (webSocketClient != null && webSocketClient.isConnected()) {
                    // Send private message via WebSocket
                    webSocketClient.sendPrivateMessage(targetUser.getId(), message);

                    // Add to local display
                    ChatMessage sentMessage = ChatMessage.builder()
                            .senderId(currentUser.getId())
                            .senderUsername(currentUser.getUsername())
                            .senderDisplayName(currentUser.getDisplayName())
                            .recipientId(targetUser.getId())
                            .content(message)
                            .timestamp(LocalDateTime.now())
                            .messageType(ChatMessage.MessageType.TEXT)
                            .build();

                    privateMessages.add(sentMessage);
                    addMessageToView(sentMessage);
                    inputField.clear();

                } else {
                    showError("Lỗi", "Chưa kết nối đến server");
                }
            } catch (Exception e) {
                log.error("Error sending private message", e);
                showError("Lỗi", "Không thể gửi tin nhắn: " + e.getMessage());
            }
        }
    }

    private void sendPrivateFile() {
        try {
            // Open file chooser
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Chọn file để gửi");
            java.io.File selectedFile = fileChooser.showOpenDialog(this);

            if (selectedFile != null) {
                if (webSocketClient != null && webSocketClient.isConnected()) {
                    // Disable button during upload
                    fileButton.setDisable(true);
                    fileButton.setText("⏳");

                    try {
                        // Upload file to server
                        String fileUrl = chatService.uploadPrivateFile(targetUser.getId(),
                                selectedFile.getAbsolutePath());

                        if (fileUrl != null && !fileUrl.isEmpty()) {
                            // Send file message via WebSocket
                            webSocketClient.sendPrivateMessage(targetUser.getId(),
                                    "📎 " + selectedFile.getName() + "\n" + fileUrl);

                            // Add to local display
                            ChatMessage fileMessage = ChatMessage.builder()
                                    .senderId(currentUser.getId())
                                    .senderUsername(currentUser.getUsername())
                                    .senderDisplayName(currentUser.getDisplayName())
                                    .recipientId(targetUser.getId())
                                    .content("📎 " + selectedFile.getName())
                                    .timestamp(LocalDateTime.now())
                                    .messageType(ChatMessage.MessageType.FILE)
                                    .fileName(selectedFile.getName())
                                    .build();

                            privateMessages.add(fileMessage);
                            addMessageToView(fileMessage);

                            log.info("Sent private file: {}", selectedFile.getName());
                        } else {
                            showError("Lỗi", "Không thể tải lên file");
                        }
                    } finally {
                        // Re-enable button
                        fileButton.setDisable(false);
                        fileButton.setText("📎");
                    }
                } else {
                    showError("Lỗi", "Chưa kết nối đến server");
                }
            }
        } catch (Exception e) {
            log.error("Error sending private file", e);
            showError("Lỗi", "Không thể gửi file: " + e.getMessage());
            fileButton.setDisable(false);
            fileButton.setText("📎");
        }
    }

    private void handleIncomingPrivateMessage(ChatMessage message) {
        // Check if this message is for our conversation
        if ((message.getSenderId().equals(targetUser.getId()) && message.getRecipientId() != null
                && message.getRecipientId().equals(currentUser.getId())) ||
                (message.getSenderId().equals(currentUser.getId()) && message.getRecipientId() != null
                        && message.getRecipientId().equals(targetUser.getId()))) {

            // Avoid duplicates
            boolean isDuplicate = message.getSenderId().equals(currentUser.getId());
            if (!isDuplicate) {
                privateMessages.add(message);
                javafx.application.Platform.runLater(() -> addMessageToView(message));
            }
        }
    }

    private void addMessageToView(ChatMessage message) {
        boolean isMine = message.getSenderId().equals(currentUser.getId());
        boolean isRecalled = message.isRecalled();

        HBox alignmentBox = new HBox(10);
        alignmentBox.setPadding(new Insets(3, 15, 3, 15));

        // Avatar
        StackPane avatarPane = createAvatar(isMine ? currentUser : targetUser);

        // Message bubble - ĐỒNG NHẤT với ContentArea
        VBox bubble = new VBox(4);
        bubble.setMaxWidth(280);
        bubble.setPadding(new Insets(10, 14, 10, 14));

        // Bubble style - tím nhạt cho cả 2 bên (light mode)
        if (isMine) {
            bubble.setStyle(
                    "-fx-background-color: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%); " +
                            "-fx-background-radius: 18 18 4 18; " +
                            "-fx-border-color: #a5b4fc; -fx-border-width: 2; -fx-border-radius: 18 18 4 18; " +
                            "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.3), 10, 0, 0, 4);");
        } else {
            bubble.setStyle(
                    "-fx-background-color: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%); " +
                            "-fx-background-radius: 18 18 18 4; " +
                            "-fx-border-color: #a5b4fc; -fx-border-width: 2; -fx-border-radius: 18 18 18 4; " +
                            "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.3), 10, 0, 0, 4);");
        }

        // Content
        String displayName = message.getSenderDisplayName() != null
                ? message.getSenderDisplayName()
                : message.getSenderUsername();
        String displayContent = isRecalled ? displayName + " đã thu hồi tin nhắn" : message.getContent();

        Label contentLabel = new Label(displayContent);
        contentLabel.setWrapText(true);
        contentLabel.setMaxWidth(260);
        contentLabel.setStyle("-fx-text-fill: #1e293b; -fx-font-size: 14px; -fx-font-weight: 500;" +
                (isRecalled ? " -fx-font-style: italic;" : ""));

        Label timeLabel = new Label(message.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm")));
        timeLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 10px; -fx-font-weight: 600;");

        bubble.getChildren().addAll(contentLabel, timeLabel);

        // Context menu for recall - only for own messages within 2 minutes
        if (isMine && !isRecalled && message.getId() != null) {
            long minutesElapsed = ChronoUnit.MINUTES.between(message.getTimestamp(), LocalDateTime.now());
            if (minutesElapsed < 2) {
                ContextMenu contextMenu = new ContextMenu();
                MenuItem recallItem = new MenuItem("Thu hồi");
                recallItem.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                final Long msgId = message.getId();
                recallItem.setOnAction(e -> {
                    boolean success = chatService.recallMessage(msgId);
                    if (success) {
                        // Update UI immediately
                        updateMessageAsRecalled(msgId);
                    } else {
                        showError("Lỗi", "Không thể thu hồi tin nhắn.");
                    }
                });
                contextMenu.getItems().add(recallItem);
                bubble.setOnContextMenuRequested(ev -> contextMenu.show(bubble, ev.getScreenX(), ev.getScreenY()));
            }
        }

        // Store bubble for recall updates
        if (message.getId() != null) {
            messageBubbles.put(message.getId(), bubble);
        }

        // Arrange based on sender
        if (isMine) {
            alignmentBox.setAlignment(Pos.CENTER_RIGHT);
            alignmentBox.getChildren().addAll(bubble, avatarPane);
        } else {
            alignmentBox.setAlignment(Pos.CENTER_LEFT);
            alignmentBox.getChildren().addAll(avatarPane, bubble);
        }

        // Animation
        alignmentBox.setOpacity(0);
        alignmentBox.setTranslateY(15);

        messageListView.getItems().add(alignmentBox);
        javafx.application.Platform.runLater(() -> {
            messageListView.scrollTo(messageListView.getItems().size() - 1);
        });

        // Fade + Slide animation
        FadeTransition fade = new FadeTransition(Duration.millis(200), alignmentBox);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(200), alignmentBox);
        slide.setFromY(15);
        slide.setToY(0);

        fade.play();
        slide.play();
    }

    /**
     * Update a message as recalled
     */
    private void updateMessageAsRecalled(Long messageId) {
        if (messageId == null)
            return;

        javafx.application.Platform.runLater(() -> {
            // Update in message list
            for (ChatMessage msg : privateMessages) {
                if (messageId.equals(msg.getId())) {
                    msg.setRecalled(true);
                    break;
                }
            }

            // Refresh the view
            messageListView.getItems().clear();
            messageBubbles.clear();
            for (ChatMessage msg : privateMessages) {
                addMessageToView(msg);
            }
        });
    }

    private StackPane createAvatar(User user) {
        StackPane avatarPane = new StackPane();
        Circle avatar = new Circle(16);
        int hash = Math.abs((user.getUsername() != null ? user.getUsername() : "").hashCode());
        avatar.setFill(AVATAR_COLORS[hash % AVATAR_COLORS.length]);

        String initial = user.getDisplayName() != null && user.getDisplayName().length() > 0
                ? user.getDisplayName().substring(0, 1).toUpperCase()
                : "?";
        Label initialLabel = new Label(initial);
        initialLabel.setStyle("-fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold;");

        avatarPane.getChildren().addAll(avatar, initialLabel);
        avatarPane.setMinSize(32, 32);
        avatarPane.setMaxSize(32, 32);
        return avatarPane;
    }

    private void showError(String title, String message) {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // Custom cell for messages
    private static class MessageCell extends ListCell<HBox> {
        @Override
        protected void updateItem(HBox item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                setGraphic(item);
                setPrefWidth(getListView().getWidth() - 20);
            }
            setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        }
    }
}