package com.example.demo.ui;

import com.example.demo.client.model.User;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.effect.DropShadow;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

/**
 * Modern Content area for chat with enhanced UI
 * Features: Avatars, Animations, Clear Section Division, Private Chat Mode
 */
@Getter
public class ContentArea extends BorderPane {

    private ListView<HBox> messageListView;
    private TextField inputField;
    private Button sendButton;
    private Button fileButton;
    private Label statusLabel;
    private ComboBox<String> roomSelector;
    private Circle statusIndicator;
    private Label roomTitleLabel;

    // Private chat mode
    @Setter
    private boolean privateMode = false;
    private User privateChatUser;
    private HBox headerBox;
    private VBox chatContainer;

    // Callbacks
    @Setter
    private Consumer<Void> onBackToRoomClicked;
    @Setter
    private Runnable onPrivateSendClicked;
    @Setter
    private Runnable onPrivateFileClicked;

    // Avatar colors for consistent user coloring
    private static final Color[] AVATAR_COLORS = {
            Color.web("#667eea"), Color.web("#764ba2"), Color.web("#f093fb"),
            Color.web("#f5576c"), Color.web("#4facfe"), Color.web("#43e97b"),
            Color.web("#fa709a"), Color.web("#fee140"), Color.web("#30cfd0"),
            Color.web("#a8edea"), Color.web("#ff9a9e"), Color.web("#fbc2eb")
    };

    public ContentArea() {
        getStyleClass().add("chat-container");
        setStyle("-fx-background-color: #f0f2f5;");

        // Create main chat area
        createChatArea();

        // Create input area
        createInputArea();
    }

    private void createChatArea() {
        VBox chatContainer = new VBox(0);
        chatContainer.getStyleClass().add("chat-container");
        VBox.setVgrow(chatContainer, Priority.ALWAYS);

        // Header with room selector and status
        HBox header = createHeader();

        // Message list
        messageListView = new ListView<>();
        messageListView.getStyleClass().add("chat-list-view");
        messageListView.setCellFactory(param -> new MessageCell());
        VBox.setVgrow(messageListView, Priority.ALWAYS);

        chatContainer.getChildren().addAll(header, messageListView);

        setCenter(chatContainer);
    }

    private HBox createHeader() {
        HBox header = new HBox(15);
        header.getStyleClass().add("content-area-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(15, 25, 15, 25));

        // Room icon
        Circle roomIcon = new Circle(18);
        roomIcon.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#667eea")),
                new Stop(1, Color.web("#764ba2"))));

        Label roomIconLabel = new Label("💬");
        roomIconLabel.setStyle("-fx-font-size: 14px;");
        StackPane roomIconPane = new StackPane(roomIcon, roomIconLabel);

        // Room info
        VBox roomInfo = new VBox(2);

        roomSelector = new ComboBox<>();
        roomSelector.setStyle(
                "-fx-background-color: transparent; -fx-border-color: transparent; " +
                        "-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 0;");
        roomSelector.setPrefHeight(30);

        HBox statusBox = new HBox(6);
        statusBox.setAlignment(Pos.CENTER_LEFT);

        statusIndicator = new Circle(5);
        statusIndicator.setFill(Color.web("#28a745"));

        statusLabel = new Label("Online - Đã kết nối");
        statusLabel.getStyleClass().add("content-header-status");
        statusLabel.setStyle("-fx-text-fill: #28a745; -fx-font-size: 12px;");

        statusBox.getChildren().addAll(statusIndicator, statusLabel);
        roomInfo.getChildren().addAll(roomSelector, statusBox);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Action buttons in header
        Button searchBtn = createHeaderButton("🔍");
        Button moreBtn = createHeaderButton("⋯");

        header.getChildren().addAll(roomIconPane, roomInfo, spacer, searchBtn, moreBtn);

        return header;
    }

    private Button createHeaderButton(String icon) {
        Button btn = new Button(icon);
        btn.setStyle(
                "-fx-background-color: #f0f2f5; -fx-background-radius: 50; " +
                        "-fx-min-width: 36; -fx-min-height: 36; -fx-max-width: 36; -fx-max-height: 36; " +
                        "-fx-cursor: hand; -fx-font-size: 14px;");
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: #e4e6eb; -fx-background-radius: 50; " +
                        "-fx-min-width: 36; -fx-min-height: 36; -fx-max-width: 36; -fx-max-height: 36; " +
                        "-fx-cursor: hand; -fx-font-size: 14px;"));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: #f0f2f5; -fx-background-radius: 50; " +
                        "-fx-min-width: 36; -fx-min-height: 36; -fx-max-width: 36; -fx-max-height: 36; " +
                        "-fx-cursor: hand; -fx-font-size: 14px;"));
        return btn;
    }

    private void createInputArea() {
        HBox inputContainer = new HBox(12);
        inputContainer.getStyleClass().add("content-area-input-box");
        inputContainer.setAlignment(Pos.CENTER);
        inputContainer.setPadding(new Insets(15, 20, 15, 20));

        // Emoji button with picker
        Button emojiBtn = new Button("😊");
        emojiBtn.setStyle(
                "-fx-background-color: transparent; -fx-font-size: 18px; " +
                        "-fx-cursor: hand; -fx-padding: 5;");
        emojiBtn.setOnAction(e -> showEmojiPicker(emojiBtn));

        inputField = new TextField();
        inputField.setPromptText("Nhập tin nhắn...");
        inputField.getStyleClass().add("chat-input-field");
        HBox.setHgrow(inputField, Priority.ALWAYS);

        // File button with FontAwesome icon
        FontAwesomeIconView paperclipIcon = new FontAwesomeIconView(FontAwesomeIcon.PAPERCLIP);
        paperclipIcon.setSize("16");
        paperclipIcon.setStyleClass("fa-icon");
        fileButton = new Button();
        fileButton.setGraphic(paperclipIcon);
        fileButton.getStyleClass().add("file-button");
        fileButton.setPrefWidth(45);
        fileButton.setPrefHeight(40);

        // Send button with FontAwesome icon
        FontAwesomeIconView sendIcon = new FontAwesomeIconView(FontAwesomeIcon.PAPER_PLANE);
        sendIcon.setSize("16");
        sendIcon.setStyleClass("fa-icon");
        sendIcon.setStyle("-fx-fill: white;");
        sendButton = new Button("Gửi ");
        sendButton.setGraphic(sendIcon);
        sendButton.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
        sendButton.setPrefWidth(100);
        sendButton.setPrefHeight(40);
        sendButton.getStyleClass().add("send-button");

        inputContainer.getChildren().addAll(emojiBtn, inputField, fileButton, sendButton);
        setBottom(inputContainer);
    }

    /**
     * Show emoji picker popup
     */
    private void showEmojiPicker(Button anchor) {
        // Check if dark mode is enabled
        boolean isDarkMode = getStyleClass().stream().anyMatch(s -> s.equals("dark-theme")) ||
                (getParent() != null && getParent().getStyleClass().stream().anyMatch(s -> s.equals("dark-theme")));

        // Theme colors
        String bgColor = isDarkMode ? "#2d2d44" : "white";
        String hoverBgColor = isDarkMode ? "#3a3a5c" : "#f0f2f5";
        String textColor = isDarkMode ? "#e4e6eb" : "#333";

        // Common emojis arranged in categories
        String[] emojis = {
                // Smileys
                "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂", "🙂", "😊",
                "😇", "🥰", "😍", "🤩", "😘", "😗", "😚", "😋", "😛", "😜",
                "🤪", "😝", "🤑", "🤗", "🤭", "🤫", "🤔", "😐", "😑", "😶",
                "😏", "😒", "🙄", "😬", "🤐", "😌", "😔", "😪", "🤤", "😴",
                "😷", "🤒", "🤕", "🤢", "🤮", "🤧", "🥵", "🥶", "🥴", "😵",
                // Hearts & Gestures
                "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💕",
                "💞", "💓", "💗", "💖", "💘", "💝", "💔", "💯", "💢", "💥",
                "👍", "👎", "👌", "✌️", "🤞", "🤟", "🤘", "🤙", "👋", "🙌",
                "👏", "🙏", "💪", "🦾", "🖐️", "✋", "🤚", "👐", "🤲", "🤝",
                // Objects & Symbols
                "🎉", "🎊", "🎁", "🎂", "🍰", "☕", "🍵", "🍺", "🍻", "🥂",
                "🔥", "✨", "⭐", "🌟", "💫", "🎈", "🎀", "🏆", "🥇", "🎯",
                "📱", "💻", "🖥️", "⌨️", "🖱️", "📷", "📹", "🎥", "🎬", "📺"
        };

        // Create popup
        javafx.stage.Popup popup = new javafx.stage.Popup();
        popup.setAutoHide(true);

        // Create emoji grid
        javafx.scene.layout.FlowPane emojiGrid = new javafx.scene.layout.FlowPane();
        emojiGrid.setHgap(2);
        emojiGrid.setVgap(2);
        emojiGrid.setPrefWrapLength(300);
        emojiGrid.setStyle(
                "-fx-background-color: " + bgColor + "; " +
                        "-fx-padding: 10; " +
                        "-fx-background-radius: 12;");

        for (String emoji : emojis) {
            Button emojiButton = new Button(emoji);
            String defaultStyle = "-fx-background-color: transparent; " +
                    "-fx-font-size: 20px; " +
                    "-fx-padding: 4; " +
                    "-fx-cursor: hand; " +
                    "-fx-min-width: 36; -fx-min-height: 36;";
            String hoverStyle = "-fx-background-color: " + hoverBgColor + "; " +
                    "-fx-font-size: 20px; " +
                    "-fx-padding: 4; " +
                    "-fx-cursor: hand; " +
                    "-fx-background-radius: 8; " +
                    "-fx-min-width: 36; -fx-min-height: 36;";

            emojiButton.setStyle(defaultStyle);
            emojiButton.setOnMouseEntered(e -> emojiButton.setStyle(hoverStyle));
            emojiButton.setOnMouseExited(e -> emojiButton.setStyle(defaultStyle));
            emojiButton.setOnAction(e -> {
                // Insert emoji at cursor position
                int caretPos = inputField.getCaretPosition();
                String currentText = inputField.getText();
                String newText = currentText.substring(0, caretPos) + emoji + currentText.substring(caretPos);
                inputField.setText(newText);
                inputField.positionCaret(caretPos + emoji.length());
                inputField.requestFocus();
                popup.hide();
            });
            emojiGrid.getChildren().add(emojiButton);
        }

        // Wrap in scroll pane for many emojis
        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(emojiGrid);
        scrollPane.setStyle(
                "-fx-background-color: " + bgColor + "; -fx-background-radius: 12; -fx-background: " + bgColor + ";");
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(250);
        scrollPane.setPrefViewportWidth(320);
        scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);

        // Container with header
        VBox container = new VBox(8);
        container.setStyle(
                "-fx-background-color: " + bgColor + "; " +
                        "-fx-background-radius: 12; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 15, 0, 0, 5);");
        container.setPadding(new Insets(10));

        Label headerLabel = new Label("😊 Chọn emoji");
        headerLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + textColor + ";");

        container.getChildren().addAll(headerLabel, scrollPane);
        popup.getContent().add(container);

        // Show popup above the emoji button
        javafx.geometry.Bounds bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        popup.show(anchor, bounds.getMinX(), bounds.getMinY() - 280);
    }

    /**
     * Add message with avatar and animation
     */
    public void addMessage(String user, String message) {
        addMessage(user, message, LocalDateTime.now(), false);
    }

    public void addMessage(String user, String message, LocalDateTime timestamp, boolean isMine) {
        HBox alignmentBox = new HBox(10);
        alignmentBox.setPadding(new Insets(2, 10, 2, 10));

        // Create avatar
        StackPane avatar = createAvatar(user, isMine);

        VBox messageContainer = new VBox(2);
        messageContainer.setMaxWidth(400);

        // Sender Name (Only show for others)
        if (!isMine) {
            Label userLabel = new Label(user);
            userLabel.getStyleClass().add("message-sender");
            messageContainer.getChildren().add(userLabel);
        }

        // Message Bubble
        VBox bubble = new VBox(4);
        bubble.getStyleClass().add("message-bubble");
        bubble.getStyleClass().add(isMine ? "mine" : "others");
        bubble.setPadding(new Insets(10, 14, 10, 14));

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(370);
        messageLabel.getStyleClass().add("message-text");
        messageLabel.getStyleClass().add(isMine ? "mine" : "others");

        Label timeLabel = new Label(timestamp.format(DateTimeFormatter.ofPattern("HH:mm")));
        timeLabel.getStyleClass().add("message-time");
        timeLabel.getStyleClass().add(isMine ? "mine" : "others");

        HBox timeBox = new HBox(timeLabel);
        timeBox.setAlignment(isMine ? Pos.BOTTOM_RIGHT : Pos.BOTTOM_LEFT);

        bubble.getChildren().addAll(messageLabel, timeBox);
        messageContainer.getChildren().add(bubble);

        // Arrange based on sender
        if (isMine) {
            alignmentBox.setAlignment(Pos.CENTER_RIGHT);
            alignmentBox.getChildren().addAll(messageContainer, avatar);
        } else {
            alignmentBox.setAlignment(Pos.CENTER_LEFT);
            alignmentBox.getChildren().addAll(avatar, messageContainer);
        }

        // Add animation
        alignmentBox.setOpacity(0);
        alignmentBox.setTranslateY(20);

        messageListView.getItems().add(alignmentBox);
        messageListView.scrollTo(messageListView.getItems().size() - 1);

        // Fade + Slide animation
        FadeTransition fade = new FadeTransition(Duration.millis(250), alignmentBox);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(250), alignmentBox);
        slide.setFromY(20);
        slide.setToY(0);

        fade.play();
        slide.play();
    }

    /**
     * Create avatar with user initial
     */
    private StackPane createAvatar(String user, boolean isMine) {
        Circle avatar = new Circle(18);
        avatar.getStyleClass().add("message-avatar");

        // Get consistent color for user
        Color avatarColor = getAvatarColor(user);
        avatar.setFill(avatarColor);

        String initial = user.length() > 0 ? user.substring(0, 1).toUpperCase() : "?";
        Label initialLabel = new Label(initial);
        initialLabel.getStyleClass().add("avatar-initial");
        initialLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");

        StackPane avatarPane = new StackPane(avatar, initialLabel);
        avatarPane.setMinSize(36, 36);
        avatarPane.setMaxSize(36, 36);

        return avatarPane;
    }

    /**
     * Get consistent color for user based on username hash
     */
    private Color getAvatarColor(String username) {
        if (username == null || username.isEmpty()) {
            return AVATAR_COLORS[0];
        }
        int hash = Math.abs(username.hashCode());
        return AVATAR_COLORS[hash % AVATAR_COLORS.length];
    }

    // Kept for backward compatibility
    public void addMessage(String user, String message, LocalDateTime timestamp) {
        addMessage(user, message, timestamp, false);
    }

    public void clearMessages() {
        messageListView.getItems().clear();
    }

    // Updated to accept current username to check ownership
    public void addMessages(List<com.example.demo.client.model.ChatMessage> messages, String currentUsername) {
        for (com.example.demo.client.model.ChatMessage msg : messages) {
            String displayName = msg.getSenderDisplayName() != null ? msg.getSenderDisplayName()
                    : msg.getSenderUsername();

            boolean isMine = msg.getSenderUsername() != null && msg.getSenderUsername().equals(currentUsername);

            if (msg.getMessageType() == com.example.demo.client.model.ChatMessage.MessageType.FILE) {
                addFileMessage(displayName, msg.getFileName(), msg.getContent(), msg.getTimestamp(), isMine);
            } else {
                addMessage(displayName, msg.getContent(), msg.getTimestamp(), isMine);
            }
        }
    }

    // Legacy method support
    public void addMessages(List<com.example.demo.client.model.ChatMessage> messages) {
        addMessages(messages, "");
    }

    public void setOnlineStatus(boolean online) {
        if (online) {
            statusIndicator.setFill(Color.web("#28a745"));
            statusLabel.setText("Online - Đã kết nối");
            statusLabel.setStyle("-fx-text-fill: #28a745; -fx-font-size: 12px;");
        } else {
            statusIndicator.setFill(Color.web("#dc3545"));
            statusLabel.setText("Offline - Mất kết nối");
            statusLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px;");
        }
    }

    public void addFileMessage(String user, String fileName, String fileUrl, LocalDateTime timestamp, boolean isMine) {
        HBox alignmentBox = new HBox(10);
        alignmentBox.setPadding(new Insets(4, 15, 4, 15));

        // Create avatar
        StackPane avatar = createAvatar(user, isMine);

        VBox messageContainer = new VBox(2);

        if (!isMine) {
            Label userLabel = new Label(user);
            userLabel.getStyleClass().add("message-sender");
            messageContainer.getChildren().add(userLabel);
        }

        // File card with modern design
        HBox fileCard = new HBox(12);
        fileCard.setAlignment(Pos.CENTER_LEFT);
        fileCard.setPadding(new Insets(14, 16, 14, 16));
        fileCard.setMaxWidth(320);

        if (isMine) {
            fileCard.setStyle(
                    "-fx-background-color: linear-gradient(135deg, #667eea 0%, #764ba2 100%); " +
                            "-fx-background-radius: 16 16 4 16; " +
                            "-fx-effect: dropshadow(gaussian, rgba(102,126,234,0.3), 8, 0, 0, 3);");
        } else {
            fileCard.setStyle(
                    "-fx-background-color: white; " +
                            "-fx-background-radius: 16 16 16 4; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 3);");
        }

        // File icon with gradient background
        StackPane fileIconPane = new StackPane();
        Circle iconBg = new Circle(24);
        if (isMine) {
            iconBg.setFill(Color.web("rgba(255,255,255,0.25)"));
        } else {
            iconBg.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#667eea")),
                    new Stop(1, Color.web("#764ba2"))));
        }
        iconBg.setEffect(new DropShadow(4, Color.web("#00000020")));

        // Determine file icon based on extension
        String fileExt = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase() : "";
        String iconEmoji = getFileIcon(fileExt);
        Label fileIcon = new Label(iconEmoji);
        fileIcon.setStyle("-fx-font-size: 18px;");
        fileIconPane.getChildren().addAll(iconBg, fileIcon);

        // File info
        VBox fileInfo = new VBox(3);
        fileInfo.setMaxWidth(160);

        Label fileLabel = new Label(fileName);
        fileLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; " +
                (isMine ? "-fx-text-fill: white;" : "-fx-text-fill: #212529;"));
        fileLabel.setWrapText(true);
        fileLabel.setMaxWidth(160);

        Label sizeLabel = new Label("📦 Tệp đính kèm");
        sizeLabel.setStyle("-fx-font-size: 11px; " +
                (isMine ? "-fx-text-fill: rgba(255,255,255,0.75);" : "-fx-text-fill: #6c757d;"));

        fileInfo.getChildren().addAll(fileLabel, sizeLabel);
        HBox.setHgrow(fileInfo, Priority.ALWAYS);

        // Download button - prominent like Zalo
        Button downloadBtn = new Button("⬇");
        downloadBtn.setMinSize(42, 42);
        downloadBtn.setMaxSize(42, 42);
        if (isMine) {
            downloadBtn.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.3); " +
                            "-fx-text-fill: white; -fx-font-size: 16px; " +
                            "-fx-background-radius: 50; -fx-cursor: hand;");
            downloadBtn.setOnMouseEntered(e -> downloadBtn.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.5); " +
                            "-fx-text-fill: white; -fx-font-size: 16px; " +
                            "-fx-background-radius: 50; -fx-cursor: hand; " +
                            "-fx-scale-x: 1.1; -fx-scale-y: 1.1;"));
            downloadBtn.setOnMouseExited(e -> downloadBtn.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.3); " +
                            "-fx-text-fill: white; -fx-font-size: 16px; " +
                            "-fx-background-radius: 50; -fx-cursor: hand;"));
        } else {
            downloadBtn.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #4ade80, #22c55e); " +
                            "-fx-text-fill: white; -fx-font-size: 16px; " +
                            "-fx-background-radius: 50; -fx-cursor: hand; " +
                            "-fx-effect: dropshadow(gaussian, rgba(34,197,94,0.4), 5, 0, 0, 2);");
            downloadBtn.setOnMouseEntered(e -> downloadBtn.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #22c55e, #16a34a); " +
                            "-fx-text-fill: white; -fx-font-size: 16px; " +
                            "-fx-background-radius: 50; -fx-cursor: hand; " +
                            "-fx-scale-x: 1.1; -fx-scale-y: 1.1; " +
                            "-fx-effect: dropshadow(gaussian, rgba(34,197,94,0.6), 8, 0, 0, 3);"));
            downloadBtn.setOnMouseExited(e -> downloadBtn.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #4ade80, #22c55e); " +
                            "-fx-text-fill: white; -fx-font-size: 16px; " +
                            "-fx-background-radius: 50; -fx-cursor: hand; " +
                            "-fx-effect: dropshadow(gaussian, rgba(34,197,94,0.4), 5, 0, 0, 2);"));
        }

        // Download action - direct download to local file
        downloadBtn.setOnAction(e -> downloadFile(fileName, fileUrl, downloadBtn, sizeLabel));

        fileCard.getChildren().addAll(fileIconPane, fileInfo, downloadBtn);

        // Timestamp
        Label timeLabel = new Label(timestamp.format(DateTimeFormatter.ofPattern("HH:mm")));
        timeLabel.setStyle("-fx-font-size: 10px; -fx-padding: 3 0 0 0; " +
                (isMine ? "-fx-text-fill: rgba(255,255,255,0.6);" : "-fx-text-fill: #9ca3af;"));

        VBox contentBox = new VBox(4);
        contentBox.getChildren().addAll(fileCard, timeLabel);
        if (isMine) {
            contentBox.setAlignment(Pos.CENTER_RIGHT);
        }

        messageContainer.getChildren().add(contentBox);

        // Arrange based on sender
        if (isMine) {
            alignmentBox.setAlignment(Pos.CENTER_RIGHT);
            alignmentBox.getChildren().addAll(messageContainer, avatar);
        } else {
            alignmentBox.setAlignment(Pos.CENTER_LEFT);
            alignmentBox.getChildren().addAll(avatar, messageContainer);
        }

        // Add animation
        alignmentBox.setOpacity(0);
        alignmentBox.setTranslateY(20);

        messageListView.getItems().add(alignmentBox);
        messageListView.scrollTo(messageListView.getItems().size() - 1);

        FadeTransition fade = new FadeTransition(Duration.millis(250), alignmentBox);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(250), alignmentBox);
        slide.setFromY(20);
        slide.setToY(0);

        fade.play();
        slide.play();
    }

    /**
     * Get appropriate icon for file type
     */
    private String getFileIcon(String extension) {
        return switch (extension.toLowerCase()) {
            case "pdf" -> "📕";
            case "doc", "docx" -> "📘";
            case "xls", "xlsx" -> "📗";
            case "ppt", "pptx" -> "📙";
            case "jpg", "jpeg", "png", "gif", "bmp", "webp" -> "🖼️";
            case "mp4", "avi", "mov", "mkv", "wmv" -> "🎬";
            case "mp3", "wav", "flac", "aac", "ogg" -> "🎵";
            case "zip", "rar", "7z", "tar", "gz" -> "📦";
            case "txt" -> "📝";
            case "html", "css", "js", "java", "py", "cpp", "c" -> "💻";
            default -> "📄";
        };
    }

    /**
     * Download file directly to Downloads folder
     */
    private void downloadFile(String fileName, String fileUrl, Button downloadBtn, Label statusLabel) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "URL file không hợp lệ");
            return;
        }

        // Get the Downloads folder path
        String userHome = System.getProperty("user.home");
        File downloadsFolder = new File(userHome, "Downloads");

        // Create Downloads folder if it doesn't exist
        if (!downloadsFolder.exists()) {
            downloadsFolder.mkdirs();
        }

        // Generate unique file name if file already exists
        File saveFile = new File(downloadsFolder, fileName);
        if (saveFile.exists()) {
            String baseName = fileName;
            String extension = "";
            int dotIndex = fileName.lastIndexOf(".");
            if (dotIndex > 0) {
                baseName = fileName.substring(0, dotIndex);
                extension = fileName.substring(dotIndex);
            }

            int counter = 1;
            while (saveFile.exists()) {
                saveFile = new File(downloadsFolder, baseName + "_" + counter + extension);
                counter++;
            }
        }

        // Create final reference for use in lambda
        final File finalSaveFile = saveFile;

        // Disable button and show downloading state
        String originalText = downloadBtn.getText();
        downloadBtn.setDisable(true);
        downloadBtn.setText("⏳");
        statusLabel.setText("⏳ Đang tải...");

        // Download in background thread
        Task<Boolean> downloadTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(fileUrl);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(60000);

                    int responseCode = conn.getResponseCode();
                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        throw new IOException("Server returned HTTP " + responseCode);
                    }

                    long fileSize = conn.getContentLengthLong();

                    try (InputStream in = new BufferedInputStream(conn.getInputStream());
                            FileOutputStream fos = new FileOutputStream(finalSaveFile)) {

                        byte[] buffer = new byte[8192];
                        long downloaded = 0;
                        int bytesRead;

                        while ((bytesRead = in.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                            downloaded += bytesRead;

                            // Update progress
                            if (fileSize > 0) {
                                long finalDownloaded = downloaded;
                                Platform.runLater(() -> {
                                    int percent = (int) ((finalDownloaded * 100) / fileSize);
                                    statusLabel.setText("⏳ " + percent + "%");
                                });
                            }
                        }
                    }
                    return true;
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            }
        };

        downloadTask.setOnSucceeded(event -> {
            downloadBtn.setDisable(false);
            downloadBtn.setText(originalText);
            statusLabel.setText("✅ Đã tải xong!");

            // Show success notification
            showAlert(Alert.AlertType.INFORMATION, "Thành công",
                    "File đã được lưu tại:\n" + finalSaveFile.getAbsolutePath());

            // Reset status after 3 seconds
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    Platform.runLater(() -> statusLabel.setText("📦 Tệp đính kèm"));
                } catch (InterruptedException ignored) {
                }
            }).start();
        });

        downloadTask.setOnFailed(event -> {
            downloadBtn.setDisable(false);
            downloadBtn.setText(originalText);
            statusLabel.setText("❌ Lỗi tải file");

            Throwable ex = downloadTask.getException();
            showAlert(Alert.AlertType.ERROR, "Lỗi tải file",
                    "Không thể tải file:\n" + (ex != null ? ex.getMessage() : "Unknown error"));

            // Reset status after 3 seconds
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    Platform.runLater(() -> statusLabel.setText("📦 Tệp đính kèm"));
                } catch (InterruptedException ignored) {
                }
            }).start();
        });

        // Start download
        new Thread(downloadTask).start();
    }

    /**
     * Show alert dialog
     */
    private void showAlert(Alert.AlertType type, String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    // Legacy support
    public void addFileMessage(String user, String fileName, String fileUrl, LocalDateTime timestamp) {
        addFileMessage(user, fileName, fileUrl, timestamp, false);
    }

    /**
     * Load rooms from ChatService instead of hardcoded values
     */
    public void loadRooms(List<String> roomNames) {
        roomSelector.getItems().clear();
        if (roomNames != null && !roomNames.isEmpty()) {
            roomSelector.getItems().addAll(roomNames);
            roomSelector.setValue(roomNames.get(0));
        }
    }

    /**
     * Load rooms from ChatRoom objects
     */
    public void loadRoomsFromChatRooms(List<com.example.demo.client.model.ChatRoom> rooms) {
        roomSelector.getItems().clear();
        if (rooms != null && !rooms.isEmpty()) {
            for (com.example.demo.client.model.ChatRoom room : rooms) {
                String displayName = room.isPrivate() ? "🔒 " + room.getName() : "🌐 " + room.getName();
                roomSelector.getItems().add(displayName);
            }
            roomSelector.setValue(roomSelector.getItems().get(0));
        }
    }

    /**
     * Switch to private chat mode with a specific user
     */
    public void switchToPrivateChatMode(User user) {
        this.privateMode = true;
        this.privateChatUser = user;

        // Clear messages
        clearMessages();

        // Update header to show private chat header
        if (chatContainer != null && headerBox != null) {
            chatContainer.getChildren().remove(headerBox);
        }

        headerBox = createPrivateChatHeader(user);

        // Rebuild chat container with private header
        chatContainer = new VBox(0);
        chatContainer.getStyleClass().add("chat-container");
        VBox.setVgrow(chatContainer, Priority.ALWAYS);
        VBox.setVgrow(messageListView, Priority.ALWAYS);
        chatContainer.getChildren().addAll(headerBox, messageListView);

        setCenter(chatContainer);
    }

    /**
     * Switch back to room mode
     */
    public void switchToRoomMode() {
        this.privateMode = false;
        this.privateChatUser = null;

        // Clear messages
        clearMessages();

        // Rebuild with normal room header
        headerBox = createHeader();

        chatContainer = new VBox(0);
        chatContainer.getStyleClass().add("chat-container");
        VBox.setVgrow(chatContainer, Priority.ALWAYS);
        VBox.setVgrow(messageListView, Priority.ALWAYS);
        chatContainer.getChildren().addAll(headerBox, messageListView);

        setCenter(chatContainer);

        // Trigger callback to reload room data
        if (onBackToRoomClicked != null) {
            onBackToRoomClicked.accept(null);
        }
    }

    /**
     * Create header for private chat mode
     */
    private HBox createPrivateChatHeader(User user) {
        HBox header = new HBox(15);
        header.getStyleClass().add("content-area-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(15, 25, 15, 25));
        header.setStyle("-fx-background-color: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        // Back button
        Button backBtn = new Button("← Quay lại");
        backBtn.setStyle(
                "-fx-background-color: #f0f2f5; -fx-text-fill: #667eea; " +
                        "-fx-font-size: 13px; -fx-font-weight: bold; " +
                        "-fx-background-radius: 20; -fx-padding: 8 16; -fx-cursor: hand;");
        backBtn.setOnMouseEntered(e -> backBtn.setStyle(
                "-fx-background-color: #667eea; -fx-text-fill: white; " +
                        "-fx-font-size: 13px; -fx-font-weight: bold; " +
                        "-fx-background-radius: 20; -fx-padding: 8 16; -fx-cursor: hand;"));
        backBtn.setOnMouseExited(e -> backBtn.setStyle(
                "-fx-background-color: #f0f2f5; -fx-text-fill: #667eea; " +
                        "-fx-font-size: 13px; -fx-font-weight: bold; " +
                        "-fx-background-radius: 20; -fx-padding: 8 16; -fx-cursor: hand;"));
        backBtn.setOnAction(e -> switchToRoomMode());

        // User avatar
        StackPane avatarPane = new StackPane();
        Circle avatar = new Circle(22);
        int hash = Math.abs((user.getUsername() != null ? user.getUsername() : "").hashCode());
        avatar.setFill(AVATAR_COLORS[hash % AVATAR_COLORS.length]);
        avatar.setEffect(new DropShadow(4, Color.web("#00000020")));

        String initial = user.getDisplayName() != null && user.getDisplayName().length() > 0
                ? user.getDisplayName().substring(0, 1).toUpperCase()
                : "?";
        Label initialLabel = new Label(initial);
        initialLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        avatarPane.getChildren().addAll(avatar, initialLabel);

        // User info
        VBox userInfo = new VBox(2);
        String displayName = user.getDisplayName() != null ? user.getDisplayName() : user.getUsername();
        Label userLabel = new Label("💬 " + displayName);
        userLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #212529;");

        HBox statusBox = new HBox(6);
        statusBox.setAlignment(Pos.CENTER_LEFT);

        Circle onlineIndicator = new Circle(5);
        onlineIndicator.setFill(Color.web("#4ade80"));
        onlineIndicator.setEffect(new DropShadow(4, Color.web("#4ade8060")));

        Label privateStatusLabel = new Label("Đang chat riêng");
        privateStatusLabel.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 12px;");

        statusBox.getChildren().addAll(onlineIndicator, privateStatusLabel);
        userInfo.getChildren().addAll(userLabel, statusBox);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // More options button
        Button moreBtn = createHeaderButton("⋯");

        header.getChildren().addAll(backBtn, avatarPane, userInfo, spacer, moreBtn);
        return header;
    }

    /**
     * Get the current private chat user
     */
    public User getPrivateChatUser() {
        return privateChatUser;
    }

    /**
     * Apply dark mode styling to all components
     */
    public void applyDarkMode(boolean isDark) {
        if (isDark) {
            setStyle("-fx-background-color: #1a1a2e;");
            // Update message list view background
            messageListView.setStyle("-fx-background-color: #1a1a2e;");
        } else {
            setStyle("-fx-background-color: #f0f2f5;");
            messageListView.setStyle("-fx-background-color: transparent;");
        }
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