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
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
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

    // Store message data for theme switching
    private static class MessageData {
        Long messageId;
        String user;
        String message;
        LocalDateTime timestamp;
        boolean isMine;
        boolean isFile;
        String fileName;
        String fileUrl;
        boolean recalled;

        MessageData(Long messageId, String user, String message, LocalDateTime timestamp, boolean isMine,
                boolean recalled) {
            this.messageId = messageId;
            this.user = user;
            this.message = message;
            this.timestamp = timestamp;
            this.isMine = isMine;
            this.isFile = false;
            this.recalled = recalled;
        }

        MessageData(Long messageId, String user, String fileName, String fileUrl, LocalDateTime timestamp,
                boolean isMine, boolean recalled) {
            this.messageId = messageId;
            this.user = user;
            this.fileName = fileName;
            this.fileUrl = fileUrl;
            this.timestamp = timestamp;
            this.isMine = isMine;
            this.isFile = true;
            this.recalled = recalled;
        }
    }

    private com.example.demo.client.service.ChatService chatService;

    public void setChatService(com.example.demo.client.service.ChatService chatService) {
        this.chatService = chatService;
    }

    private List<MessageData> messageHistory = new ArrayList<>();
    private boolean isRefreshingMessages = false;

    public ContentArea() {
        getStyleClass().add("chat-container");
        setStyle("-fx-background-color: #fafbfc;");

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

        // Message list - hỗ trợ dark/light mode
        messageListView = new ListView<>();
        // Check if dark mode is enabled
        boolean isDarkMode = getStyleClass().contains("dark-theme") ||
                (getParent() != null && getParent().getStyleClass().contains("dark-theme"));
        if (isDarkMode) {
            messageListView.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #1f2937 0%, #374151 100%); " +
                            "-fx-background-radius: 0; " +
                            "-fx-border-color: transparent;");
        } else {
            messageListView.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #fafbfc 0%, #f1f5f9 100%); " +
                            "-fx-background-radius: 0; " +
                            "-fx-border-color: transparent;");
        }
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
        // Check if dark mode is enabled
        boolean isDarkMode = getStyleClass().contains("dark-theme") ||
                (getParent() != null && getParent().getStyleClass().contains("dark-theme"));

        HBox inputContainer = new HBox(12);
        inputContainer.setAlignment(Pos.CENTER);
        inputContainer.setPadding(new Insets(18, 24, 18, 24));
        inputContainer.setStyle(
                "-fx-background-color: " + (isDarkMode ? "#1f2937" : "#fafbfc") + "; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 6, 0, 0, 2);");

        // Emoji button with picker
        Button emojiBtn = new Button("😊");
        emojiBtn.setStyle(
                "-fx-background-color: " + (isDarkMode ? "#374151" : "#f0f2f5") + "; -fx-background-radius: 50; " +
                        "-fx-min-width: 42; -fx-min-height: 42; -fx-max-width: 42; -fx-max-height: 42; " +
                        "-fx-cursor: hand; -fx-font-size: 18px;");
        emojiBtn.setOnMouseEntered(e -> emojiBtn.setStyle(
                "-fx-background-color: " + (isDarkMode ? "#4b5563" : "#e4e6eb") + "; -fx-background-radius: 50; " +
                        "-fx-min-width: 42; -fx-min-height: 42; -fx-max-width: 42; -fx-max-height: 42; " +
                        "-fx-cursor: hand; -fx-font-size: 18px;"));
        emojiBtn.setOnMouseExited(e -> emojiBtn.setStyle(
                "-fx-background-color: " + (isDarkMode ? "#374151" : "#f0f2f5") + "; -fx-background-radius: 50; " +
                        "-fx-min-width: 42; -fx-min-height: 42; -fx-max-width: 42; -fx-max-height: 42; " +
                        "-fx-cursor: hand; -fx-font-size: 18px;"));
        emojiBtn.setOnAction(e -> showEmojiPicker(emojiBtn));

        inputField = new TextField();
        inputField.setPromptText("Nhập tin nhắn...");
        inputField.setStyle(
                "-fx-background-color: " + (isDarkMode ? "#374151" : "#f8fafc") + "; " +
                        "-fx-border-color: " + (isDarkMode ? "#6b7280" : "#e2e8f0") + "; " +
                        "-fx-border-radius: 25; -fx-background-radius: 25; " +
                        "-fx-padding: 14 22; -fx-font-size: 16px; " +
                        "-fx-text-fill: " + (isDarkMode ? "#f9fafb" : "#1a202c") + "; " +
                        "-fx-prompt-text-fill: " + (isDarkMode ? "#9ca3af" : "#718096") + "; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 3, 0, 0, 1);");
        HBox.setHgrow(inputField, Priority.ALWAYS);

        // File button with FontAwesome icon
        FontAwesomeIconView paperclipIcon = new FontAwesomeIconView(FontAwesomeIcon.PAPERCLIP);
        paperclipIcon.setSize("16");
        paperclipIcon.setStyle("-fx-fill: #667eea;");
        fileButton = new Button();
        fileButton.setGraphic(paperclipIcon);
        fileButton.setStyle(
                "-fx-background-color: " + (isDarkMode ? "#374151" : "#f0f2f5") + "; -fx-background-radius: 50; " +
                        "-fx-min-width: 42; -fx-min-height: 42; -fx-max-width: 42; -fx-max-height: 42; " +
                        "-fx-cursor: hand;");
        fileButton.setOnMouseEntered(e -> fileButton.setStyle(
                "-fx-background-color: " + (isDarkMode ? "#4b5563" : "#e4e6eb") + "; -fx-background-radius: 50; " +
                        "-fx-min-width: 42; -fx-min-height: 42; -fx-max-width: 42; -fx-max-height: 42; " +
                        "-fx-cursor: hand;"));
        fileButton.setOnMouseExited(e -> fileButton.setStyle(
                "-fx-background-color: " + (isDarkMode ? "#374151" : "#f0f2f5") + "; -fx-background-radius: 50; " +
                        "-fx-min-width: 42; -fx-min-height: 42; -fx-max-width: 42; -fx-max-height: 42; " +
                        "-fx-cursor: hand;"));

        // Send button with FontAwesome icon
        FontAwesomeIconView sendIcon = new FontAwesomeIconView(FontAwesomeIcon.PAPER_PLANE);
        sendIcon.setSize("16");
        sendIcon.setStyle("-fx-fill: white;");
        sendButton = Sidebar.createBeautifulButton("Gửi", "", "#667eea");
        sendButton.setGraphic(sendIcon);
        sendButton.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);

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
                    "-fx-font-family: 'Segoe UI Emoji'; " +
                    "-fx-font-size: 20px; " +
                    "-fx-padding: 4; " +
                    "-fx-cursor: hand; " +
                    "-fx-min-width: 36; -fx-min-height: 36;";
            String hoverStyle = "-fx-background-color: " + hoverBgColor + "; " +
                    "-fx-font-family: 'Segoe UI Emoji'; " +
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
        addMessage(null, user, message, LocalDateTime.now(), false, false);
    }

    public void addMessage(Long messageId, String user, String message, LocalDateTime timestamp, boolean isMine,
            boolean recalled) {
        // Store message data for theme switching (only if not refreshing)
        if (!isRefreshingMessages) {
            messageHistory.add(new MessageData(messageId, user, message, timestamp, isMine, recalled));
        }

        // Check if dark mode is enabled
        boolean isDarkMode = getStyleClass().contains("dark-theme") ||
                (getParent() != null && getParent().getStyleClass().contains("dark-theme"));

        HBox alignmentBox = new HBox(12);
        alignmentBox.setPadding(new Insets(12, 20, 12, 20));

        // Create avatar
        StackPane avatar = createAvatar(user, isMine);

        VBox messageContainer = new VBox(2);
        messageContainer.setMaxWidth(400);

        // Sender Name (Only show for others) - hỗ trợ dark/light mode
        if (!isMine) {
            Label userLabel = new Label(user);
            userLabel.setStyle(
                    "-fx-font-weight: 600; " +
                            "-fx-font-size: 14px; " +
                            "-fx-font-family: 'Segoe UI', sans-serif; " +
                            (isDarkMode ? "-fx-text-fill: white;" : "-fx-text-fill: #4a5568;") + " " +
                            "-fx-padding: 0 0 6 0;");
            messageContainer.getChildren().add(userLabel);
        }

        // Message Bubble với design đẹp hơn - hỗ trợ dark/light mode
        VBox bubble = new VBox(4);
        bubble.setPadding(new Insets(16, 18, 16, 18));
        bubble.setMaxWidth(450);

        // Style cho bubble - ĐỒNG NHẤT cả 2 bên A và B
        if (isMine) {
            if (isDarkMode) {
                // Dark mode: bubble tím đậm
                bubble.setStyle(
                        "-fx-background-color: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%); " +
                                "-fx-background-radius: 18 18 4 18; " +
                                "-fx-border-color: #818cf8; -fx-border-width: 1.5; -fx-border-radius: 18 18 4 18; " +
                                "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.5), 12, 0, 0, 4);");
            } else {
                // Light mode: bubble tím nhạt giống bên B (dễ đọc)
                bubble.setStyle(
                        "-fx-background-color: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%); " +
                                "-fx-background-radius: 18 18 4 18; " +
                                "-fx-border-color: #a5b4fc; -fx-border-width: 2; -fx-border-radius: 18 18 4 18; " +
                                "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.3), 10, 0, 0, 4);");
            }
        } else {
            if (isDarkMode) {
                // Dark mode: bubble xám đậm
                bubble.setStyle(
                        "-fx-background-color: linear-gradient(135deg, #374151 0%, #4b5563 100%); " +
                                "-fx-background-radius: 18 18 18 4; " +
                                "-fx-border-color: #6b7280; -fx-border-width: 1.5; -fx-border-radius: 18 18 18 4; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 4);");
            } else {
                // Light mode: bubble tím nhạt
                bubble.setStyle(
                        "-fx-background-color: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%); " +
                                "-fx-background-radius: 18 18 18 4; " +
                                "-fx-border-color: #a5b4fc; -fx-border-width: 2; -fx-border-radius: 18 18 18 4; " +
                                "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.3), 10, 0, 0, 4);");
            }
        }

        String displayContent = recalled ? user + " đã thu hồi tin nhắn" : message;
        Label messageLabel = new Label(displayContent);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(400);
        // Text color - CHỮ ĐEN cho cả 2 bên trong Light mode
        if (isDarkMode) {
            // Dark mode: chữ trắng
            messageLabel.setStyle(
                    "-fx-font-size: 15px; " +
                            "-fx-font-family: 'Segoe UI', 'Helvetica Neue', 'Arial', sans-serif; " +
                            "-fx-font-weight: 500; " +
                            (recalled ? "-fx-font-style: italic; " : "") +
                            "-fx-text-fill: #ffffff; " +
                            "-fx-padding: 2 0 2 0;");
        } else {
            // Light mode: chữ đen đậm cho cả 2 bên
            messageLabel.setStyle(
                    "-fx-font-size: 15px; " +
                            "-fx-font-family: 'Segoe UI', 'Helvetica Neue', 'Arial', sans-serif; " +
                            "-fx-font-weight: 500; " +
                            (recalled ? "-fx-font-style: italic; " : "") +
                            "-fx-text-fill: #1e293b; " +
                            (recalled ? "-fx-text-fill: #64748b; " : "") +
                            "-fx-padding: 2 0 2 0;");
        }

        // Context Menu for recall and reactions
        System.out.println(
                "📩 ContentArea.addMessage: messageId=" + messageId + ", isMine=" + isMine + ", recalled=" + recalled);

        ContextMenu contextMenu = new ContextMenu();

        // Add reaction menu for OTHER PEOPLE's messages only (not mine, not recalled)
        if (!isMine && !recalled && messageId != null) {
            Menu reactionMenu = new Menu("😀 Phản ứng");
            String[] quickEmojis = { "👍", "❤️", "😂", "😮", "😢", "😡" };
            for (String emoji : quickEmojis) {
                MenuItem emojiItem = new MenuItem(emoji);
                emojiItem.setStyle("-fx-font-size: 18px;");
                final Long finalMsgId = messageId;
                emojiItem.setOnAction(e -> {
                    if (chatService != null) {
                        Boolean added = chatService.toggleReaction(finalMsgId, emoji);
                        if (added != null) {
                            System.out.println("😀 Reaction " + emoji + " " + (added ? "added" : "removed")
                                    + " on message " + finalMsgId);
                        }
                    }
                });
                reactionMenu.getItems().add(emojiItem);
            }
            contextMenu.getItems().add(reactionMenu);
        }

        // Add recall option for own messages
        if (isMine && !recalled && messageId != null) {
            long minutesElapsed = ChronoUnit.MINUTES.between(timestamp, LocalDateTime.now());
            System.out.println("⏱️ Message " + messageId + " - minutes elapsed: " + minutesElapsed);
            if (minutesElapsed < 2) {
                MenuItem recallItem = new MenuItem("🔄 Thu hồi");
                recallItem.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                final Long finalMessageId = messageId;
                recallItem.setOnAction(e -> {
                    if (chatService != null) {
                        boolean success = chatService.recallMessage(finalMessageId);
                        if (success) {
                            // Cập nhật UI ngay lập tức - không đợi WebSocket
                            updateMessageAsRecalled(finalMessageId);
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể thu hồi tin nhắn.");
                        }
                    }
                });
                contextMenu.getItems().add(recallItem);
                System.out.println("✅ Added recall context menu for message " + messageId);
            } else {
                System.out.println("⚠️ Message " + messageId + " is older than 2 minutes");
            }
        } else if (isMine && !recalled && messageId == null) {
            System.out.println("⚠️ Message has NULL ID - cannot add recall menu!");
        }

        // Set context menu on bubble if has items
        if (!contextMenu.getItems().isEmpty()) {
            bubble.setOnContextMenuRequested(ev -> contextMenu.show(bubble, ev.getScreenX(), ev.getScreenY()));
        }

        // Thời gian - màu xám đậm cho light mode, trắng mờ cho dark mode
        Label timeLabel = new Label(timestamp.format(DateTimeFormatter.ofPattern("HH:mm")));
        if (isDarkMode) {
            timeLabel.setStyle(
                    "-fx-font-size: 11px; " +
                            "-fx-font-weight: 600; " +
                            "-fx-font-family: 'Segoe UI', sans-serif; " +
                            "-fx-text-fill: rgba(255,255,255,0.85); " +
                            "-fx-padding: 4 0 0 0;");
        } else {
            // Light mode: thời gian màu xám đậm
            timeLabel.setStyle(
                    "-fx-font-size: 11px; " +
                            "-fx-font-weight: 600; " +
                            "-fx-font-family: 'Segoe UI', sans-serif; " +
                            "-fx-text-fill: #64748b; " +
                            "-fx-padding: 4 0 0 0;");
        }

        HBox timeBox = new HBox(timeLabel);
        timeBox.setAlignment(isMine ? Pos.BOTTOM_RIGHT : Pos.BOTTOM_LEFT);
        timeBox.setPadding(new Insets(2, 0, 0, 0));

        bubble.getChildren().addAll(messageLabel, timeBox);

        // 🎨 Zalo-style: Show reactions count + floating popup on hover
        if (!recalled && messageId != null) {
            final Long msgId = messageId;
            final boolean darkMode = isDarkMode;

            // Reaction counts display (always visible if has reactions)
            HBox reactionCountsBox = new HBox(3);
            reactionCountsBox.setId("reactionCountsBox");
            reactionCountsBox.setUserData(msgId); // Store messageId for later lookup
            reactionCountsBox.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            reactionCountsBox.setPadding(new Insets(4, 0, 0, 0));
            reactionCountsBox.setStyle("-fx-background-color: transparent;");

            // Load and display reaction counts
            loadReactionCounts(msgId, reactionCountsBox, darkMode);
            bubble.getChildren().add(reactionCountsBox);

            // 🎯 Zalo-style floating reaction picker (only for OTHER people's messages)
            if (!isMine) {
                // Create floating reaction popup - NO BACKGROUND, just emojis
                HBox floatingReactionBar = new HBox(3);
                floatingReactionBar.setAlignment(Pos.CENTER);
                floatingReactionBar.setPadding(new Insets(2, 4, 2, 4));
                floatingReactionBar.setStyle(
                        "-fx-background-color: transparent;");
                floatingReactionBar.setVisible(false);
                floatingReactionBar.setManaged(false);
                floatingReactionBar.setOpacity(0);

                String[] quickEmojis = { "👍", "❤️", "😂", "😮", "😢", "😡" };

                for (String emoji : quickEmojis) {
                    Label emojiLabel = new Label(emoji);
                    emojiLabel.setStyle(
                            "-fx-font-size: 16px; " +
                                    "-fx-cursor: hand; " +
                                    "-fx-padding: 1;");

                    // Hover animation
                    emojiLabel.setOnMouseEntered(e -> {
                        emojiLabel.setScaleX(1.2);
                        emojiLabel.setScaleY(1.2);
                        emojiLabel.setStyle("-fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 1;");
                    });
                    emojiLabel.setOnMouseExited(e -> {
                        emojiLabel.setScaleX(1.0);
                        emojiLabel.setScaleY(1.0);
                        emojiLabel.setStyle("-fx-font-size: 16px; -fx-cursor: hand; -fx-padding: 1;");
                    });

                    // Click to react
                    emojiLabel.setOnMouseClicked(e -> {
                        if (chatService != null) {
                            Boolean added = chatService.toggleReaction(msgId, emoji);
                            if (added != null) {
                                // Pop animation
                                emojiLabel.setScaleX(1.5);
                                emojiLabel.setScaleY(1.5);
                                new Thread(() -> {
                                    try {
                                        // Wait for backend to process before refreshing
                                        Thread.sleep(300);
                                        Platform.runLater(() -> {
                                            emojiLabel.setScaleX(1.0);
                                            emojiLabel.setScaleY(1.0);
                                            // Refresh counts immediately after reaction
                                            System.out.println("🔄 Refreshing reaction counts for msg " + msgId);
                                            loadReactionCounts(msgId, reactionCountsBox, darkMode);
                                        });
                                    } catch (InterruptedException ignored) {
                                    }
                                }).start();
                                System.out.println(
                                        "😀 " + emoji + " " + (added ? "added ✅" : "removed ❌") + " on msg " + msgId);
                            }
                        }
                    });

                    floatingReactionBar.getChildren().add(emojiLabel);
                }

                // Position popup above the bubble
                StackPane bubbleWrapper = new StackPane();
                bubbleWrapper.getChildren().addAll(bubble, floatingReactionBar);
                StackPane.setAlignment(floatingReactionBar, Pos.TOP_CENTER);
                StackPane.setMargin(floatingReactionBar, new Insets(-45, 0, 0, 0));

                // Show/hide on hover with animation
                bubbleWrapper.setOnMouseEntered(e -> {
                    floatingReactionBar.setVisible(true);
                    floatingReactionBar.setManaged(true);
                    FadeTransition fadeIn = new FadeTransition(Duration.millis(150), floatingReactionBar);
                    fadeIn.setFromValue(0);
                    fadeIn.setToValue(1);
                    fadeIn.play();
                });
                bubbleWrapper.setOnMouseExited(e -> {
                    FadeTransition fadeOut = new FadeTransition(Duration.millis(100), floatingReactionBar);
                    fadeOut.setFromValue(1);
                    fadeOut.setToValue(0);
                    fadeOut.setOnFinished(ev -> {
                        floatingReactionBar.setVisible(false);
                        floatingReactionBar.setManaged(false);
                    });
                    fadeOut.play();
                });

                messageContainer.getChildren().add(bubbleWrapper);
            } else {
                messageContainer.getChildren().add(bubble);
            }
        } else {
            messageContainer.getChildren().add(bubble);
        }

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
        // Đảm bảo scroll sau khi item được render
        Platform.runLater(() -> {
            messageListView.scrollTo(messageListView.getItems().size() - 1);
        });

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
     * Create avatar with user initial - đẹp hơn
     */
    private StackPane createAvatar(String user, boolean isMine) {
        Circle avatar = new Circle(20);
        avatar.setEffect(new DropShadow(6, Color.web("#00000025")));

        // Get consistent color for user
        Color avatarColor = getAvatarColor(user);
        avatar.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, avatarColor),
                new Stop(1, avatarColor.darker())));

        String initial = user.length() > 0 ? user.substring(0, 1).toUpperCase() : "?";
        Label initialLabel = new Label(initial);
        initialLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");

        StackPane avatarPane = new StackPane(avatar, initialLabel);
        avatarPane.setMinSize(40, 40);
        avatarPane.setMaxSize(40, 40);

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
        addMessage(null, user, message, timestamp, false, false);
    }

    public void addMessage(String user, String message, LocalDateTime timestamp, boolean isMine) {
        addMessage(null, user, message, timestamp, isMine, false);
    }

    public void clearMessages() {
        messageListView.getItems().clear();
        messageHistory.clear();
    }

    // Updated to accept current username to check ownership
    public void addMessages(List<com.example.demo.client.model.ChatMessage> messages, String currentUsername) {
        for (com.example.demo.client.model.ChatMessage msg : messages) {
            String displayName = msg.getSenderDisplayName() != null ? msg.getSenderDisplayName()
                    : msg.getSenderUsername();

            boolean isMine = msg.getSenderUsername() != null && msg.getSenderUsername().equals(currentUsername);

            if (msg.isRecalled()) {
                addMessage(msg.getId(), displayName, null, msg.getTimestamp(), isMine, true);
            } else if (msg.getMessageType() == com.example.demo.client.model.ChatMessage.MessageType.FILE
                    && msg.getFileName() != null && !msg.getFileName().isEmpty()) {
                addFileMessage(msg.getId(), displayName, msg.getFileName(), msg.getContent(), msg.getTimestamp(),
                        isMine, false);
            } else {
                addMessage(msg.getId(), displayName, msg.getContent(), msg.getTimestamp(), isMine, false);
            }
        }

        // 📜 Scroll to bottom after loading all messages
        if (!messages.isEmpty()) {
            Platform.runLater(() -> {
                messageListView.scrollTo(messageListView.getItems().size() - 1);
            });
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

    public void addFileMessage(Long messageId, String user, String fileName, String fileUrl, LocalDateTime timestamp,
            boolean isMine, boolean recalled) {
        if (recalled) {
            addMessage(messageId, user, null, timestamp, isMine, true);
            return;
        }

        // Store message data for theme switching (only if not refreshing)
        if (!isRefreshingMessages) {
            messageHistory.add(new MessageData(messageId, user, fileName, fileUrl, timestamp, isMine, false));
        }

        // Check if dark mode is enabled
        boolean isDarkMode = getStyleClass().contains("dark-theme") ||
                (getParent() != null && getParent().getStyleClass().contains("dark-theme"));

        HBox alignmentBox = new HBox(10);
        alignmentBox.setPadding(new Insets(4, 15, 4, 15));

        // Create avatar
        StackPane avatar = createAvatar(user, isMine);

        VBox messageContainer = new VBox(2);

        if (!isMine) {
            Label userLabel = new Label(user);
            userLabel.setStyle(
                    "-fx-font-weight: 600; " +
                            "-fx-font-size: 14px; " +
                            "-fx-font-family: 'Segoe UI', sans-serif; " +
                            (isDarkMode ? "-fx-text-fill: #f1f5f9;" : "-fx-text-fill: #4a5568;") + " " +
                            "-fx-padding: 0 0 6 0;");
            messageContainer.getChildren().add(userLabel);
        }

        // File card with modern design
        HBox fileCard = new HBox(12);
        fileCard.setAlignment(Pos.CENTER_LEFT);
        fileCard.setPadding(new Insets(14, 16, 14, 16));
        fileCard.setMaxWidth(320);

        if (isMine) {
            if (isDarkMode) {
                fileCard.setStyle(
                        "-fx-background-color: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%); " +
                                "-fx-background-radius: 16 16 4 16; " +
                                "-fx-border-color: #818cf8; -fx-border-width: 1.5; -fx-border-radius: 16 16 4 16; " +
                                "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.5), 12, 0, 0, 4);");
            } else {
                // Light mode: bubble tím nhạt giống tin nhắn thường
                fileCard.setStyle(
                        "-fx-background-color: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%); " +
                                "-fx-background-radius: 16 16 4 16; " +
                                "-fx-border-color: #a5b4fc; -fx-border-width: 2; -fx-border-radius: 16 16 4 16; " +
                                "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.3), 10, 0, 0, 4);");
            }
        } else {
            if (isDarkMode) {
                fileCard.setStyle(
                        "-fx-background-color: linear-gradient(135deg, #374151 0%, #4b5563 100%); " +
                                "-fx-background-radius: 16 16 16 4; " +
                                "-fx-border-color: #6b7280; -fx-border-width: 2; -fx-border-radius: 16 16 16 4; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.7), 14, 0, 0, 5);");
            } else {
                // Light mode: bubble tím nhạt
                fileCard.setStyle(
                        "-fx-background-color: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%); " +
                                "-fx-background-radius: 16 16 16 4; " +
                                "-fx-border-color: #a5b4fc; -fx-border-width: 2; -fx-border-radius: 16 16 16 4; " +
                                "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.3), 10, 0, 0, 4);");
            }
        }

        // File icon with gradient background - MÀU ĐẬM CHO CẢ 2 BÊN
        StackPane fileIconPane = new StackPane();
        Circle iconBg = new Circle(24);
        if (isDarkMode) {
            // Dark mode: icon tím đậm
            iconBg.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#6366f1")),
                    new Stop(1, Color.web("#4f46e5"))));
        } else {
            // Light mode: icon tím đậm cho cả 2 bên
            iconBg.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#6366f1")),
                    new Stop(1, Color.web("#4f46e5"))));
        }
        iconBg.setEffect(new DropShadow(4, Color.web("#00000020")));

        // Determine file icon based on extension (with null check)
        String safeFileName = fileName != null ? fileName : "file";
        String fileExt = safeFileName.contains(".")
                ? safeFileName.substring(safeFileName.lastIndexOf(".") + 1).toLowerCase()
                : "";
        String iconEmoji = getFileIcon(fileExt);
        Label fileIcon = new Label(iconEmoji);
        fileIcon.setStyle("-fx-font-size: 18px;");
        fileIconPane.getChildren().addAll(iconBg, fileIcon);

        // File info
        VBox fileInfo = new VBox(3);
        fileInfo.setMaxWidth(160);

        Label fileLabel = new Label(safeFileName);
        // Chữ đen cho light mode, trắng cho dark mode
        fileLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; " +
                (isDarkMode ? "-fx-text-fill: #ffffff;" : "-fx-text-fill: #1e293b;"));
        fileLabel.setWrapText(true);
        fileLabel.setMaxWidth(160);

        Label sizeLabel = new Label("📦 Tệp đính kèm");
        sizeLabel.setStyle("-fx-font-size: 11px; " +
                (isDarkMode ? "-fx-text-fill: rgba(255,255,255,0.8);" : "-fx-text-fill: #64748b;"));

        fileInfo.getChildren().addAll(fileLabel, sizeLabel);
        HBox.setHgrow(fileInfo, Priority.ALWAYS);

        // Download button - XANH LÁ ĐẬM cho cả 2 bên
        Button downloadBtn = new Button("⬇");
        downloadBtn.setMinSize(42, 42);
        downloadBtn.setMaxSize(42, 42);
        downloadBtn.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #22c55e, #16a34a); " +
                        "-fx-text-fill: white; -fx-font-size: 16px; " +
                        "-fx-background-radius: 50; -fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(34,197,94,0.5), 6, 0, 0, 3);");
        downloadBtn.setOnMouseEntered(e -> downloadBtn.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #16a34a, #15803d); " +
                        "-fx-text-fill: white; -fx-font-size: 16px; " +
                        "-fx-background-radius: 50; -fx-cursor: hand; " +
                        "-fx-scale-x: 1.1; -fx-scale-y: 1.1; " +
                        "-fx-effect: dropshadow(gaussian, rgba(34,197,94,0.7), 10, 0, 0, 4);"));
        downloadBtn.setOnMouseExited(e -> downloadBtn.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #22c55e, #16a34a); " +
                        "-fx-text-fill: white; -fx-font-size: 16px; " +
                        "-fx-background-radius: 50; -fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(34,197,94,0.5), 6, 0, 0, 3);"));

        // Download action - direct download to local file
        downloadBtn.setOnAction(e -> downloadFile(fileName, fileUrl, downloadBtn, sizeLabel));

        // Context Menu for recall (Files)
        System.out.println("📩 ContentArea.addFileMessage: messageId=" + messageId + ", isMine=" + isMine);
        if (isMine && messageId != null) {
            long minutesElapsed = ChronoUnit.MINUTES.between(timestamp, LocalDateTime.now());
            System.out.println("⏱️ File message " + messageId + " - minutes elapsed: " + minutesElapsed);
            if (minutesElapsed < 2) {
                ContextMenu contextMenu = new ContextMenu();
                MenuItem recallItem = new MenuItem("Thu hồi");
                recallItem.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                final Long finalMessageId = messageId;
                recallItem.setOnAction(e -> {
                    if (chatService != null) {
                        boolean success = chatService.recallMessage(finalMessageId);
                        if (success) {
                            // Cập nhật UI ngay lập tức - không đợi WebSocket
                            updateMessageAsRecalled(finalMessageId);
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể thu hồi tệp tin.");
                        }
                    }
                });
                contextMenu.getItems().add(recallItem);
                fileCard.setOnContextMenuRequested(e -> contextMenu.show(fileCard, e.getScreenX(), e.getScreenY()));
                System.out.println("✅ Added recall context menu for file message " + messageId);
            } else {
                System.out.println("⚠️ File message " + messageId + " is older than 2 minutes");
            }
        } else if (isMine && messageId == null) {
            System.out.println("⚠️ File message has NULL ID - cannot add recall menu!");
        }

        fileCard.getChildren().addAll(fileIconPane, fileInfo, downloadBtn);

        // Timestamp
        Label timeLabel = new Label(timestamp.format(DateTimeFormatter.ofPattern("HH:mm")));
        timeLabel.setStyle("-fx-font-size: 10px; -fx-padding: 3 0 0 0; " +
                (isMine ? "-fx-text-fill: rgba(255,255,255,0.9);"
                        : (isDarkMode ? "-fx-text-fill: #cbd5e1;" : "-fx-text-fill: #9ca3af;")));

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

        // Prepend default server URL if relative path
        String fullUrl = fileUrl;
        if (!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://")) {
            // Use default server URL or get from config
            String serverUrl = System.getenv("WEBCHAT_G10_SERVER_URL");
            if (serverUrl == null || serverUrl.isEmpty()) {
                serverUrl = "http://26.6.143.150:8081"; // Default server
            }
            fullUrl = serverUrl + fileUrl;
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
        final String finalFileUrl = fullUrl;

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
                    URL url = URI.create(finalFileUrl).toURL();
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
        addFileMessage(null, user, fileName, fileUrl, timestamp, false, false);
    }

    public void addFileMessage(String user, String fileName, String fileUrl, LocalDateTime timestamp, boolean isMine) {
        addFileMessage(null, user, fileName, fileUrl, timestamp, isMine, false);
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

        // Refresh all messages with new theme
        refreshAllMessages();
    }

    /**
     * Update a message in the UI to recalled state
     */
    public void updateMessageAsRecalled(Long messageId) {
        if (messageId == null)
            return;

        System.out.println("🔙 ContentArea.updateMessageAsRecalled called for messageId: " + messageId);
        System.out.println("🔙 Current messageHistory size: " + messageHistory.size());

        Platform.runLater(() -> {
            boolean updated = false;
            for (MessageData data : messageHistory) {
                System.out.println("🔍 Checking message: id=" + data.messageId + ", user=" + data.user + ", content="
                        + data.message);
                if (messageId.equals(data.messageId)) {
                    data.recalled = true;
                    updated = true;
                    System.out.println("✅ Found and updated message " + messageId + " as recalled");
                }
            }

            if (updated) {
                System.out.println("🔄 Refreshing messages after recall update");
                // Refresh the whole list because we need to rebuild the bubbles
                refreshAllMessages();
            } else {
                System.out.println("⚠️ Message " + messageId + " NOT found in messageHistory!");
            }
        });
    }

    /**
     * Update the ID of a local message after receiving the echo from server
     * This is needed for recall functionality to work on messages we just sent
     */
    public void updateLocalMessageWithServerId(Long messageId, String content, boolean isMine) {
        if (messageId == null)
            return;

        Platform.runLater(() -> {
            // Find the most recent message from us that doesn't have an ID yet
            for (int i = messageHistory.size() - 1; i >= 0; i--) {
                MessageData data = messageHistory.get(i);
                if (data.messageId == null && data.isMine == isMine) {
                    // Match by content if provided, otherwise just match first null ID message
                    if (content == null || (data.message != null && data.message.equals(content))) {
                        data.messageId = messageId;
                        System.out.println("✅ ContentArea: Updated local message with server ID: " + messageId);

                        // Refresh to add recall context menu now that we have ID
                        refreshAllMessages();
                        break;
                    }
                }
            }
        });
    }

    /**
     * Refresh all messages with current theme
     */
    private void refreshAllMessages() {
        if (isRefreshingMessages)
            return;
        isRefreshingMessages = true;

        // Clear current messages
        messageListView.getItems().clear();

        // Re-add all messages with current theme
        for (MessageData data : messageHistory) {
            if (data.isFile) {
                addFileMessage(data.messageId, data.user, data.fileName, data.fileUrl, data.timestamp, data.isMine,
                        data.recalled);
            } else {
                addMessage(data.messageId, data.user, data.message, data.timestamp, data.isMine, data.recalled);
            }
        }

        // Scroll to bottom to show latest messages
        messageListView.scrollTo(messageListView.getItems().size() - 1);

        isRefreshingMessages = false;
    }

    /**
     * 🎨 Load and display reaction counts for a message (e.g., ❤️2 👍1)
     * Uses colorful badges - each emoji has its own background color!
     */
    @SuppressWarnings("unchecked")
    private void loadReactionCounts(Long messageId, HBox reactionCountsBox, boolean isDarkMode) {
        if (chatService == null || messageId == null)
            return;

        new Thread(() -> {
            try {
                java.util.Map<String, Object> summary = chatService.getReactionSummary(messageId);
                if (summary == null || summary.isEmpty())
                    return;

                java.util.Map<String, Integer> counts = (java.util.Map<String, Integer>) summary.get("counts");
                if (counts == null || counts.isEmpty())
                    return;

                Platform.runLater(() -> {
                    reactionCountsBox.getChildren().clear();

                    // Emoji to color mapping for colorful badges
                    java.util.Map<String, String> emojiColors = new java.util.HashMap<>();
                    emojiColors.put("👍", "#3b82f6"); // Blue
                    emojiColors.put("❤️", "#ef4444"); // Red
                    emojiColors.put("😂", "#f59e0b"); // Orange
                    emojiColors.put("😮", "#8b5cf6"); // Purple
                    emojiColors.put("😢", "#06b6d4"); // Cyan
                    emojiColors.put("😡", "#f97316"); // Deep orange

                    for (java.util.Map.Entry<String, Integer> entry : counts.entrySet()) {
                        String emoji = entry.getKey();
                        Integer count = entry.getValue();
                        if (count == null || count <= 0)
                            continue;

                        // Get color for this emoji (or default gray)
                        String bgColor = emojiColors.getOrDefault(emoji, isDarkMode ? "#4b5563" : "#9ca3af");

                        // Create colorful emoji badge
                        Label badge = new Label(emoji + (count > 1 ? " " + count : ""));
                        badge.setStyle(
                                "-fx-background-color: " + bgColor + "; " +
                                        "-fx-text-fill: white; " +
                                        "-fx-padding: 3 8; " +
                                        "-fx-background-radius: 12; " +
                                        "-fx-font-size: 13px; " +
                                        "-fx-font-weight: bold; " +
                                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 3, 0, 0, 1); " +
                                        "-fx-cursor: hand;");

                        // Hover effect - brighter
                        badge.setOnMouseEntered(e -> {
                            badge.setScaleX(1.1);
                            badge.setScaleY(1.1);
                        });
                        badge.setOnMouseExited(e -> {
                            badge.setScaleX(1.0);
                            badge.setScaleY(1.0);
                        });

                        reactionCountsBox.getChildren().add(badge);
                    }
                });
            } catch (Exception e) {
                System.err.println("Failed to load reactions: " + e.getMessage());
            }
        }).start();
    }

    /**
     * 😀 Refresh reaction counts for a specific message (called when receiving
     * reaction update via WebSocket)
     */
    public void refreshReactionForMessage(Long messageId) {
        if (messageId == null)
            return;

        System.out.println("😀 Refreshing reaction for message: " + messageId);

        // Find the reactionCountsBox for this message in the ListView
        Platform.runLater(() -> {
            for (HBox row : messageListView.getItems()) {
                // Each row contains a StackPane (bubbleWrapper) which contains the message
                // bubble
                // The reactionCountsBox is stored as user data on the row
                @SuppressWarnings("unchecked")
                java.util.Map<Long, HBox> reactionBoxMap = (java.util.Map<Long, HBox>) row.getUserData();

                if (reactionBoxMap == null) {
                    // Try to find reactionCountsBox through the hierarchy
                    // The row structure is: HBox (row) -> children -> StackPane (bubbleWrapper) ->
                    // VBox (contentVBox) -> HBox (reactionCountsBox)
                    for (javafx.scene.Node child : row.getChildren()) {
                        if (child instanceof StackPane stackPane) {
                            for (javafx.scene.Node stackChild : stackPane.getChildren()) {
                                if (stackChild instanceof VBox vbox) {
                                    for (javafx.scene.Node vboxChild : vbox.getChildren()) {
                                        if (vboxChild instanceof HBox hbox
                                                && "reactionCountsBox".equals(hbox.getId())) {
                                            // Found it! Check if this is the right message
                                            Object msgIdData = hbox.getUserData();
                                            if (msgIdData != null && msgIdData.equals(messageId)) {
                                                System.out.println("😀 Found reactionCountsBox for message " + messageId
                                                        + " - refreshing!");
                                                loadReactionCounts(messageId, hbox, true);
                                                return;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // If not found by traversing, just refresh all messages (fallback)
            System.out.println("😀 Could not find specific reaction box, refreshing all messages");
            refreshAllMessages();
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