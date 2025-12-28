package com.example.demo.ui;

import com.example.demo.client.model.ChatMessage;
import com.example.demo.client.model.RecallResponse;
import com.example.demo.client.model.User;
import com.example.demo.client.service.ChatService;
import com.example.demo.client.util.VoiceRecorder;
import com.example.demo.client.websocket.WebSocketClient;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
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
    private Button voiceButton;
    private Button emojiButton;
    private Label statusLabel;
    private Circle onlineIndicator;

    // Voice recording
    private VoiceRecorder voiceRecorder;
    private boolean isVoiceRecording = false;
    private HBox recordingIndicator;
    private Label recordingTimeLabel;

    // Message storage
    private List<ChatMessage> privateMessages = new ArrayList<>();
    // Map to track message bubbles for recall updates
    private Map<Long, VBox> messageBubbles = new HashMap<>();

    // 🔙 Recall callback reference for cleanup
    private java.util.function.Consumer<RecallResponse> recallCallback;

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

        // 🔙 Cleanup recall callback when dialog closes
        setOnCloseRequest(e -> cleanup());
        setOnHidden(e -> cleanup());
    }

    /**
     * 🔙 Cleanup resources when dialog is closed
     */
    private void cleanup() {
        if (webSocketClient != null && recallCallback != null) {
            webSocketClient.removeRecallCallback(recallCallback);
            log.info("🔙 Cleaned up recall callback for chat with {}", targetUser.getUsername());
        }
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

        // Emoji button - YELLOW/ORANGE
        emojiButton = new Button("😊");
        emojiButton.setStyle(
                "-fx-background-color: #f59e0b; -fx-background-radius: 50; " +
                        "-fx-min-width: 42; -fx-min-height: 42; -fx-max-width: 42; -fx-max-height: 42; " +
                        "-fx-cursor: hand; -fx-font-size: 18px;");
        emojiButton.setOnMouseEntered(e -> emojiButton.setStyle(
                "-fx-background-color: #d97706; -fx-background-radius: 50; " +
                        "-fx-min-width: 42; -fx-min-height: 42; -fx-max-width: 42; -fx-max-height: 42; " +
                        "-fx-cursor: hand; -fx-font-size: 18px;"));
        emojiButton.setOnMouseExited(e -> emojiButton.setStyle(
                "-fx-background-color: #f59e0b; -fx-background-radius: 50; " +
                        "-fx-min-width: 42; -fx-min-height: 42; -fx-max-width: 42; -fx-max-height: 42; " +
                        "-fx-cursor: hand; -fx-font-size: 18px;"));

        // File button - BLUE
        fileButton = new Button("📎");
        fileButton.setStyle(
                "-fx-background-color: #3b82f6; -fx-background-radius: 50; " +
                        "-fx-min-width: 42; -fx-min-height: 42; -fx-max-width: 42; -fx-max-height: 42; " +
                        "-fx-cursor: hand; -fx-font-size: 18px;");
        fileButton.setOnMouseEntered(e -> fileButton.setStyle(
                "-fx-background-color: #2563eb; -fx-background-radius: 50; " +
                        "-fx-min-width: 42; -fx-min-height: 42; -fx-max-width: 42; -fx-max-height: 42; " +
                        "-fx-cursor: hand; -fx-font-size: 18px;"));
        fileButton.setOnMouseExited(e -> fileButton.setStyle(
                "-fx-background-color: #3b82f6; -fx-background-radius: 50; " +
                        "-fx-min-width: 42; -fx-min-height: 42; -fx-max-width: 42; -fx-max-height: 42; " +
                        "-fx-cursor: hand; -fx-font-size: 18px;"));

        // Voice button - GREEN
        voiceButton = new Button("🎤");
        voiceButton.setStyle(
                "-fx-background-color: #22c55e; -fx-background-radius: 50; " +
                        "-fx-min-width: 42; -fx-min-height: 42; -fx-max-width: 42; -fx-max-height: 42; " +
                        "-fx-cursor: hand; -fx-font-size: 18px;");

        // Voice recorder
        voiceRecorder = new VoiceRecorder();
        voiceRecorder.setOnRecordingProgress(durationMs -> {
            javafx.application.Platform.runLater(() -> {
                if (recordingTimeLabel != null) {
                    recordingTimeLabel.setText(VoiceRecorder.formatTime(durationMs));
                }
            });
        });

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

            // 🔙 Subscribe to recall notifications for private messages
            // Store callback reference for cleanup when dialog closes
            this.recallCallback = this::handleRecallNotification;
            webSocketClient.subscribeToMessageRecall(this.recallCallback);
            log.info("🔙 PrivateChatDialog subscribed to recall notifications for chat with user {}",
                    targetUser.getId());
        }
    }

    /**
     * 🔙 Handle recall notification from WebSocket
     */
    private void handleRecallNotification(RecallResponse recallResponse) {
        if (recallResponse == null || recallResponse.getMessageId() == null) {
            return;
        }

        // Check if this recall is for a message in our conversation
        // The recall could be from us or from the target user
        Long messageId = recallResponse.getMessageId();

        // Check if this message exists in our conversation
        boolean isOurMessage = privateMessages.stream()
                .anyMatch(msg -> messageId.equals(msg.getId()));

        if (isOurMessage) {
            log.info("🔙 Received recall notification for message {} in private chat with {}",
                    messageId, targetUser.getUsername());
            updateMessageAsRecalled(messageId);
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

        // Recording indicator (hidden by default)
        Circle recordingDot = new Circle(6, Color.RED);
        javafx.animation.Timeline blink = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(Duration.seconds(0), e -> recordingDot.setOpacity(1)),
                new javafx.animation.KeyFrame(Duration.seconds(0.5), e -> recordingDot.setOpacity(0.3)),
                new javafx.animation.KeyFrame(Duration.seconds(1), e -> recordingDot.setOpacity(1)));
        blink.setCycleCount(javafx.animation.Timeline.INDEFINITE);

        recordingIndicator = new HBox(10);
        recordingIndicator.setAlignment(Pos.CENTER_LEFT);
        Label recordingLabel = new Label("Đang ghi âm...");
        recordingLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
        recordingTimeLabel = new Label("0:00");
        recordingTimeLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px; -fx-font-weight: 600;");
        Button cancelBtn = new Button("❌");
        cancelBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 16px;");
        cancelBtn.setOnAction(e -> cancelVoiceRecording(blink));
        recordingIndicator.getChildren().addAll(recordingDot, recordingLabel, recordingTimeLabel, cancelBtn);
        recordingIndicator.setVisible(false);
        recordingIndicator.setManaged(false);
        HBox.setHgrow(recordingIndicator, Priority.ALWAYS);

        // Voice button event handlers
        String voiceNormalStyle = "-fx-background-color: #22c55e; -fx-background-radius: 50; " +
                "-fx-min-width: 42; -fx-min-height: 42; -fx-max-width: 42; -fx-max-height: 42; -fx-cursor: hand; -fx-font-size: 18px;";
        String voiceHoverStyle = "-fx-background-color: #16a34a; -fx-background-radius: 50; " +
                "-fx-min-width: 42; -fx-min-height: 42; -fx-max-width: 42; -fx-max-height: 42; -fx-cursor: hand; -fx-font-size: 18px;";
        String voiceRecordingStyle = "-fx-background-color: #ef4444; -fx-background-radius: 50; " +
                "-fx-min-width: 42; -fx-min-height: 42; -fx-max-width: 42; -fx-max-height: 42; -fx-cursor: hand; -fx-font-size: 18px;";

        voiceButton.setOnMouseEntered(e -> {
            if (!isVoiceRecording) {
                voiceButton.setStyle(voiceHoverStyle);
            }
        });
        voiceButton.setOnMouseExited(e -> {
            if (!isVoiceRecording) {
                voiceButton.setStyle(voiceNormalStyle);
            }
        });

        // TAP TO TOGGLE: First tap starts, second tap stops and sends
        voiceButton.setOnAction(e -> {
            if (!isVoiceRecording) {
                // Start recording
                if (voiceRecorder.startRecording()) {
                    isVoiceRecording = true;
                    voiceButton.setStyle(voiceRecordingStyle);
                    voiceButton.setText("⏹️"); // Stop icon

                    // Show recording indicator
                    recordingIndicator.setVisible(true);
                    recordingIndicator.setManaged(true);
                    inputField.setVisible(false);
                    inputField.setManaged(false);
                    blink.play();
                }
            } else {
                // Stop and send
                isVoiceRecording = false;
                blink.stop();
                recordingDot.setOpacity(1);
                voiceButton.setStyle(voiceNormalStyle);
                voiceButton.setText("🎤");

                // Hide recording indicator
                recordingIndicator.setVisible(false);
                recordingIndicator.setManaged(false);
                inputField.setVisible(true);
                inputField.setManaged(true);

                java.io.File voiceFile = voiceRecorder.stopRecording();
                if (voiceFile != null && voiceFile.exists()) {
                    sendVoiceMessage(voiceFile);
                }
            }
        });

        HBox.setHgrow(inputField, Priority.ALWAYS);
        inputArea.getChildren().addAll(emojiButton, fileButton, voiceButton, recordingIndicator, inputField,
                sendButton);

        return inputArea;
    }

    private void cancelVoiceRecording(javafx.animation.Timeline blink) {
        if (isVoiceRecording) {
            isVoiceRecording = false;
            voiceRecorder.cancelRecording();
            blink.stop();

            // Reset UI
            recordingIndicator.setVisible(false);
            recordingIndicator.setManaged(false);
            inputField.setVisible(true);
            inputField.setManaged(true);

            // Reset button style
            voiceButton.setStyle(
                    "-fx-background-color: #22c55e; -fx-background-radius: 50; " +
                            "-fx-min-width: 42; -fx-min-height: 42; -fx-max-width: 42; -fx-max-height: 42; -fx-cursor: hand; -fx-font-size: 18px;");
            voiceButton.setText("🎤");
        }
    }

    private void sendVoiceMessage(java.io.File voiceFile) {
        try {
            if (webSocketClient != null && webSocketClient.isConnected()) {
                // Upload voice file - backend automatically creates the message (like chatroom)
                String fileUrl = chatService.uploadPrivateFile(targetUser.getId(), voiceFile.getAbsolutePath());

                if (fileUrl != null && !fileUrl.isEmpty()) {
                    // DON'T send via WebSocket - backend already created the message!
                    // The message will arrive via WebSocket subscription automatically

                    // Show local feedback immediately
                    ChatMessage voiceMessage = ChatMessage.builder()
                            .senderId(currentUser.getId())
                            .senderUsername(currentUser.getUsername())
                            .senderDisplayName(currentUser.getDisplayName())
                            .recipientId(targetUser.getId())
                            .content(fileUrl)
                            .timestamp(LocalDateTime.now())
                            .messageType(ChatMessage.MessageType.VOICE)
                            .build();

                    privateMessages.add(voiceMessage);
                    addMessageToView(voiceMessage);

                    log.info("🎤 Sent private voice message to {}", targetUser.getUsername());
                } else {
                    showError("Lỗi", "Không thể tải lên tin nhắn thoại");
                }
            } else {
                showError("Lỗi", "Chưa kết nối đến server");
            }
        } catch (Exception e) {
            log.error("Error sending private voice message", e);
            showError("Lỗi", "Không thể gửi tin nhắn thoại: " + e.getMessage());
        }
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

            boolean isMyMessage = message.getSenderId().equals(currentUser.getId());

            if (isMyMessage) {
                // This is an echo of my own message from server - update ID for recall support
                // Find the message without ID (recently sent) and update it with server's ID
                if (message.getId() != null) {
                    javafx.application.Platform.runLater(() -> {
                        for (int i = privateMessages.size() - 1; i >= 0; i--) {
                            ChatMessage localMsg = privateMessages.get(i);
                            // Match by content and sender (recent message without ID)
                            if (localMsg.getId() == null
                                    && localMsg.getSenderId().equals(message.getSenderId())
                                    && localMsg.getContent().equals(message.getContent())) {
                                localMsg.setId(message.getId());
                                // Update the bubble map with the new ID
                                VBox bubble = messageBubbles.remove(null);
                                if (bubble != null) {
                                    messageBubbles.put(message.getId(), bubble);
                                    // Add recall context menu now that we have the ID
                                    addRecallContextMenu(bubble, message.getId(), localMsg.getTimestamp());
                                }
                                log.info("✅ Updated local message with server ID: {}", message.getId());
                                break;
                            }
                        }
                    });
                }
            } else {
                // Message from other user - add to list and display
                privateMessages.add(message);
                javafx.application.Platform.runLater(() -> addMessageToView(message));
            }
        }
    }

    /**
     * Add recall context menu to a message bubble
     */
    private void addRecallContextMenu(VBox bubble, Long messageId, java.time.LocalDateTime messageTimestamp) {
        if (bubble == null || messageId == null)
            return;

        long minutesElapsed = ChronoUnit.MINUTES.between(messageTimestamp, LocalDateTime.now());
        if (minutesElapsed >= 2) {
            log.debug("⏱️ Message {} is older than 2 minutes, no recall menu", messageId);
            return;
        }

        ContextMenu contextMenu = new ContextMenu();
        MenuItem recallItem = new MenuItem("Thu hồi");
        recallItem.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
        recallItem.setOnAction(e -> {
            boolean success = chatService.recallMessage(messageId);
            if (success) {
                updateMessageAsRecalled(messageId);
            } else {
                showError("Lỗi", "Không thể thu hồi tin nhắn.");
            }
        });
        contextMenu.getItems().add(recallItem);
        bubble.setOnContextMenuRequested(ev -> contextMenu.show(bubble, ev.getScreenX(), ev.getScreenY()));
        log.info("✅ Added recall context menu for message {} (via echo update)", messageId);
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

        // Check if this is a voice message - improved detection
        String content = message.getContent() != null ? message.getContent().trim() : "";
        boolean isVoiceMessage = message.getMessageType() == ChatMessage.MessageType.VOICE ||
                content.contains("/uploads/voice/") ||
                content.contains("/voice/") ||
                content.endsWith(".wav") ||
                content.endsWith(".mp3") ||
                content.endsWith(".ogg");

        log.debug("🎤 Message detection: type={}, content='{}', isVoice={}",
                message.getMessageType(), content.length() > 50 ? content.substring(0, 50) + "..." : content,
                isVoiceMessage);

        if (isVoiceMessage && !isRecalled) {
            // Voice message bubble style - PROMINENT
            bubble.setStyle(
                    "-fx-background-color: linear-gradient(135deg, #4f46e5 0%, #6366f1 100%); " +
                            "-fx-background-radius: " + (isMine ? "18 18 4 18" : "18 18 18 4") + "; " +
                            "-fx-border-color: #818cf8; -fx-border-width: 2; " +
                            "-fx-border-radius: " + (isMine ? "18 18 4 18" : "18 18 18 4") + "; " +
                            "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.6), 15, 0, 0, 5);");

            // Voice header
            HBox voiceHeader = new HBox(10);
            voiceHeader.setAlignment(Pos.CENTER_LEFT);
            Label micEmoji = new Label("🎤");
            micEmoji.setStyle("-fx-font-size: 20px;");
            Label voiceLabel = new Label("Tin nhắn thoại");
            voiceLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
            voiceHeader.getChildren().addAll(micEmoji, voiceLabel);

            // Audio player controls
            HBox playerControls = new HBox(12);
            playerControls.setAlignment(Pos.CENTER_LEFT);
            playerControls.setPadding(new Insets(4, 0, 0, 0));

            // Play button
            Button playBtn = new Button("▶");
            playBtn.setStyle(
                    "-fx-background-color: #22c55e; -fx-text-fill: white; -fx-background-radius: 50; " +
                            "-fx-min-width: 44; -fx-min-height: 44; -fx-max-width: 44; -fx-max-height: 44; " +
                            "-fx-font-size: 18px; -fx-font-weight: bold; -fx-cursor: hand; " +
                            "-fx-border-color: white; -fx-border-width: 2; -fx-border-radius: 50;");

            // Progress bar
            ProgressBar progressBar = new ProgressBar(0);
            progressBar.setPrefWidth(100);
            progressBar.setPrefHeight(10);
            progressBar.setStyle("-fx-accent: #22c55e; -fx-control-inner-background: rgba(255,255,255,0.4);");

            // Duration label
            Label durationLabel = new Label("0:00");
            durationLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white; " +
                    "-fx-background-color: rgba(0,0,0,0.3); -fx-padding: 4 8; -fx-background-radius: 10;");

            // Audio player logic
            final boolean[] isPlaying = { false };
            final javax.sound.sampled.Clip[] clipRef = { null };

            // Extract voice URL - handle both formats (just URL or with text prefix)
            String voiceUrl = message.getContent();
            if (voiceUrl != null && voiceUrl.contains("\n")) {
                voiceUrl = voiceUrl.split("\n")[voiceUrl.split("\n").length - 1].trim();
            }
            final String finalVoiceUrl = voiceUrl != null ? voiceUrl.trim() : "";

            playBtn.setOnAction(e -> {
                try {
                    if (finalVoiceUrl.isEmpty()) {
                        showError("Lỗi", "URL tin nhắn thoại không hợp lệ");
                        return;
                    }

                    if (clipRef[0] == null) {
                        String fullUrl = finalVoiceUrl;
                        if (!finalVoiceUrl.startsWith("http")) {
                            fullUrl = chatService.getBaseUrl() + finalVoiceUrl;
                        }

                        java.net.URL audioUrl = new java.net.URL(fullUrl);
                        java.io.InputStream audioStream = audioUrl.openStream();
                        java.io.File tempFile = java.io.File.createTempFile("voice_", ".wav");
                        tempFile.deleteOnExit();
                        java.nio.file.Files.copy(audioStream, tempFile.toPath(),
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        audioStream.close();

                        javax.sound.sampled.AudioInputStream ais = javax.sound.sampled.AudioSystem
                                .getAudioInputStream(tempFile);
                        clipRef[0] = javax.sound.sampled.AudioSystem.getClip();
                        clipRef[0].open(ais);

                        long totalMs = clipRef[0].getMicrosecondLength() / 1000;
                        javafx.application.Platform
                                .runLater(() -> durationLabel.setText(VoiceRecorder.formatTime(totalMs)));

                        clipRef[0].addLineListener(event -> {
                            if (event.getType() == javax.sound.sampled.LineEvent.Type.STOP) {
                                if (clipRef[0].getMicrosecondPosition() >= clipRef[0].getMicrosecondLength()) {
                                    javafx.application.Platform.runLater(() -> {
                                        playBtn.setText("▶");
                                        progressBar.setProgress(0);
                                        isPlaying[0] = false;
                                        clipRef[0].setMicrosecondPosition(0);
                                    });
                                }
                            }
                        });

                        java.util.Timer progressTimer = new java.util.Timer(true);
                        progressTimer.scheduleAtFixedRate(new java.util.TimerTask() {
                            @Override
                            public void run() {
                                if (clipRef[0] != null && clipRef[0].isRunning()) {
                                    double progress = (double) clipRef[0].getMicrosecondPosition()
                                            / clipRef[0].getMicrosecondLength();
                                    javafx.application.Platform.runLater(() -> progressBar.setProgress(progress));
                                }
                            }
                        }, 0, 100);
                    }

                    if (isPlaying[0]) {
                        clipRef[0].stop();
                        playBtn.setText("▶");
                        isPlaying[0] = false;
                    } else {
                        clipRef[0].start();
                        playBtn.setText("⏸");
                        isPlaying[0] = true;
                    }
                } catch (Exception ex) {
                    log.error("Error playing voice message: {}", ex.getMessage());
                    showError("Lỗi", "Không thể phát tin nhắn thoại");
                }
            });

            playerControls.getChildren().addAll(playBtn, progressBar, durationLabel);

            Label timeLabel = new Label(message.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm")));
            timeLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 10px; -fx-font-weight: 600;");

            bubble.getChildren().addAll(voiceHeader, playerControls, timeLabel);
        } else {
            // Regular message bubble style
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
        }

        // Context menu for recall - only for own messages within 2 minutes
        log.debug("🔍 Message recall check: isMine={}, isRecalled={}, messageId={}",
                isMine, isRecalled, message.getId());
        if (isMine && !isRecalled) {
            if (message.getId() != null) {
                long minutesElapsed = ChronoUnit.MINUTES.between(message.getTimestamp(), LocalDateTime.now());
                log.debug("⏱️ Message {} - minutes elapsed: {}", message.getId(), minutesElapsed);
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
                    log.info("✅ Added recall context menu for message {}", msgId);
                } else {
                    log.debug("⏱️ Message {} is older than 2 minutes, no recall menu", message.getId());
                }
            } else {
                log.warn("⚠️ Message has null ID, cannot add recall menu");
            }
        }

        // Store bubble for recall updates
        // For new messages without ID, store with null key (will be updated when echo
        // received)
        messageBubbles.put(message.getId(), bubble);

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
        // Smooth scroll to bottom
        smoothScrollToBottom();

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

    /**
     * Smooth scroll to bottom with animation - Zalo-like smooth scrolling
     */
    private void smoothScrollToBottom() {
        if (messageListView == null || messageListView.getItems().isEmpty()) {
            return;
        }

        // Delay slightly to ensure new item is fully rendered
        javafx.application.Platform.runLater(() -> {
            // Another runLater to ensure the layout pass is complete
            javafx.application.Platform.runLater(() -> {
                // Get the virtual flow (internal scroll container)
                Object virtualFlow = messageListView.lookup(".virtual-flow");
                if (virtualFlow instanceof javafx.scene.layout.Region) {
                    ScrollBar scrollBar = null;
                    for (javafx.scene.Node node : messageListView.lookupAll(".scroll-bar")) {
                        if (node instanceof ScrollBar) {
                            ScrollBar sb = (ScrollBar) node;
                            if (sb.getOrientation() == javafx.geometry.Orientation.VERTICAL) {
                                scrollBar = sb;
                                break;
                            }
                        }
                    }

                    if (scrollBar != null) {
                        final ScrollBar verticalScrollBar = scrollBar;
                        double currentValue = verticalScrollBar.getValue();
                        double endValue = verticalScrollBar.getMax();

                        // Only animate if not already at bottom
                        if (Math.abs(currentValue - endValue) > 0.01) {
                            // Create smooth animation with Zalo-like feel
                            Timeline timeline = new Timeline();
                            KeyValue keyValue = new KeyValue(
                                    verticalScrollBar.valueProperty(),
                                    endValue,
                                    Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0)); // Cubic-bezier like CSS ease
                            KeyFrame keyFrame = new KeyFrame(Duration.millis(350), keyValue);
                            timeline.getKeyFrames().add(keyFrame);
                            timeline.play();
                        }
                    } else {
                        // Fallback: use scrollTo if scrollbar not found
                        messageListView.scrollTo(messageListView.getItems().size() - 1);
                    }
                } else {
                    // Fallback: use scrollTo if virtual flow not found
                    messageListView.scrollTo(messageListView.getItems().size() - 1);
                }
            });
        });
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