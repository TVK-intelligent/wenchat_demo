package com.example.demo.ui;

import com.example.demo.client.model.ChatMessage;
import com.example.demo.client.model.ChatRoom;
import com.example.demo.client.service.ChatService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Message History Dialog - View message history for a room
 */
@Slf4j
public class MessageHistoryDialog extends Stage {

    private final ChatService chatService;
    private final ChatRoom room;

    // UI Components
    private ListView<ChatMessage> messageListView;
    private ObservableList<ChatMessage> messages;
    private ProgressIndicator loadingIndicator;
    private Label statusLabel;
    private StackPane loadingOverlay;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public MessageHistoryDialog(ChatService chatService, ChatRoom room) {
        this.chatService = chatService;
        this.room = room;

        initModality(Modality.APPLICATION_MODAL);
        setTitle("📜 Lịch sử tin nhắn - " + room.getName());
        setWidth(600);
        setHeight(500);
        setResizable(true);

        initComponents();

        Scene scene = new Scene(createLayout());
        scene.getStylesheets().add("data:text/css," +
                ".message-history { -fx-background-color: #f8f9fa; }" +
                ".message-item { -fx-padding: 10; -fx-border-color: #e9ecef; -fx-border-width: 0 0 1 0; }" +
                ".message-header { -fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #6c757d; }" +
                ".message-content { -fx-font-size: 14px; -fx-text-fill: #212529; }" +
                ".message-time { -fx-font-size: 11px; -fx-text-fill: #adb5bd; }" +
                ".loading-indicator { -fx-progress-color: #007bff; }" +
                ".status-label { -fx-text-fill: #6c757d; -fx-font-style: italic; }");

        setScene(scene);

        // Load message history after scene is fully set up (to avoid
        // NullPointerException)
        loadMessageHistory();
    }

    private void initComponents() {
        messageListView = new ListView<>();
        messages = FXCollections.observableArrayList();
        messageListView.setItems(messages);
        messageListView.setCellFactory(listView -> new MessageListCell());

        loadingIndicator = new ProgressIndicator();
        loadingIndicator.getStyleClass().add("loading-indicator");
        loadingIndicator.setPrefSize(50, 50);

        statusLabel = new Label("Đang tải lịch sử tin nhắn...");
        statusLabel.getStyleClass().add("status-label");
    }

    private VBox createLayout() {
        VBox mainLayout = new VBox(10);
        mainLayout.setPadding(new Insets(15));
        mainLayout.getStyleClass().add("message-history");

        // Header
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("📜 Lịch sử tin nhắn");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshButton = new Button("🔄 Làm mới");
        refreshButton.setOnAction(e -> loadMessageHistory());

        Button closeButton = new Button("Đóng");
        closeButton.setOnAction(e -> close());

        headerBox.getChildren().addAll(titleLabel, spacer, refreshButton, closeButton);

        // Loading overlay
        StackPane contentPane = new StackPane();
        loadingOverlay = createLoadingOverlay();
        contentPane.getChildren().addAll(messageListView, loadingOverlay);

        mainLayout.getChildren().addAll(headerBox, contentPane);

        return mainLayout;
    }

    private StackPane createLoadingOverlay() {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(255, 255, 255, 0.8);");
        overlay.setVisible(false);

        VBox loadingBox = new VBox(10);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.getChildren().addAll(loadingIndicator, statusLabel);

        overlay.getChildren().add(loadingBox);
        return overlay;
    }

    private void loadMessageHistory() {
        loadingOverlay.setVisible(true);
        statusLabel.setText("Đang tải lịch sử tin nhắn...");

        // Load messages in background
        new Thread(() -> {
            try {
                List<ChatMessage> messages = chatService.getRoomMessages(room.getId());

                javafx.application.Platform.runLater(() -> {
                    this.messages.clear();
                    this.messages.addAll(messages);
                    loadingOverlay.setVisible(false);

                    if (messages.isEmpty()) {
                        statusLabel.setText("Không có tin nhắn nào.");
                        loadingOverlay.setVisible(true);
                    }
                });
            } catch (Exception e) {
                log.error("Failed to load message history", e);
                javafx.application.Platform.runLater(() -> {
                    statusLabel.setText("Lỗi khi tải lịch sử tin nhắn: " + e.getMessage());
                    loadingOverlay.setVisible(true);
                });
            }
        }).start();
    }

    /**
     * Custom ListCell for displaying messages
     */
    private static class MessageListCell extends ListCell<ChatMessage> {
        private final VBox content;
        private final Label headerLabel;
        private final TextFlow messageFlow;
        private final Label timeLabel;

        public MessageListCell() {
            content = new VBox(5);
            content.getStyleClass().add("message-item");

            headerLabel = new Label();
            headerLabel.getStyleClass().add("message-header");

            messageFlow = new TextFlow();
            messageFlow.getStyleClass().add("message-content");

            timeLabel = new Label();
            timeLabel.getStyleClass().add("message-time");

            HBox bottomBox = new HBox();
            bottomBox.setAlignment(Pos.BOTTOM_RIGHT);
            bottomBox.getChildren().add(timeLabel);

            content.getChildren().addAll(headerLabel, messageFlow, bottomBox);
        }

        @Override
        protected void updateItem(ChatMessage message, boolean empty) {
            super.updateItem(message, empty);

            if (empty || message == null) {
                setGraphic(null);
            } else {
                // Header with sender info
                String senderName = message.getSenderDisplayName() != null
                        && !message.getSenderDisplayName().trim().isEmpty() ? message.getSenderDisplayName()
                                : "@" + message.getSenderUsername();
                headerLabel.setText(senderName);

                // Message content
                Text contentText = new Text(message.getContent() != null ? message.getContent() : "");
                messageFlow.getChildren().clear();
                messageFlow.getChildren().add(contentText);

                // Timestamp
                if (message.getTimestamp() != null) {
                    timeLabel.setText(message.getTimestamp().format(TIME_FORMATTER) + " " +
                            message.getTimestamp().format(DATE_FORMATTER));
                } else {
                    timeLabel.setText("");
                }

                setGraphic(content);
            }
        }
    }
}