package com.example.demo.ui;

import com.example.demo.client.model.ChatRoom;
import com.example.demo.client.model.User;
import com.example.demo.client.service.ChatService;
import java.util.List;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Room Invite Dialog - Handle room invitations
 */
@Slf4j
public class RoomInviteDialog extends Stage {

    private final ChatService chatService;

    // UI Components
    private ListView<Map<String, Object>> inviteListView;
    private ObservableList<Map<String, Object>> invites;
    private ProgressIndicator loadingIndicator;
    private Label statusLabel;
    private StackPane loadingOverlay;

    public RoomInviteDialog(ChatService chatService) {
        this.chatService = chatService;

        initModality(Modality.APPLICATION_MODAL);
        setTitle("📨 Lời mời tham gia phòng");
        setWidth(500);
        setHeight(400);
        setResizable(true);

        initComponents();

        Scene scene = new Scene(createLayout());
        // Load invites after layout is created (to avoid NullPointerException)
        scene.getStylesheets().add("data:text/css," +
                ".invite-dialog { -fx-background-color: #f8f9fa; }" +
                ".invite-item { -fx-padding: 15; -fx-border-color: #e9ecef; -fx-border-width: 0 0 1 0; -fx-background-color: white; }"
                +
                ".invite-header { -fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #495057; }" +
                ".invite-details { -fx-font-size: 12px; -fx-text-fill: #6c757d; }" +
                ".invite-buttons { -fx-spacing: 10; }" +
                ".accept-button { -fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 12; -fx-border-radius: 4; -fx-cursor: hand; }"
                +
                ".accept-button:hover { -fx-background-color: #1e7e34; }" +
                ".decline-button { -fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 12; -fx-border-radius: 4; -fx-cursor: hand; }"
                +
                ".decline-button:hover { -fx-background-color: #c82333; }" +
                ".loading-indicator { -fx-progress-color: #007bff; }" +
                ".status-label { -fx-text-fill: #6c757d; -fx-font-style: italic; }");

        setScene(scene);

        // Load invites after scene is fully set up
        loadInvites();
    }

    private void initComponents() {
        inviteListView = new ListView<>();
        invites = FXCollections.observableArrayList();
        inviteListView.setItems(invites);
        inviteListView.setCellFactory(listView -> new InviteListCell());

        loadingIndicator = new ProgressIndicator();
        loadingIndicator.getStyleClass().add("loading-indicator");
        loadingIndicator.setPrefSize(50, 50);

        statusLabel = new Label("Đang tải lời mời...");
        statusLabel.getStyleClass().add("status-label");
    }

    private VBox createLayout() {
        VBox mainLayout = new VBox(10);
        mainLayout.setPadding(new Insets(15));
        mainLayout.getStyleClass().add("invite-dialog");

        // Header
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("📨 Lời mời tham gia phòng");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshButton = new Button("🔄 Làm mới");
        refreshButton.setStyle(
                "-fx-background-color: #4ade80; -fx-text-fill: white; " +
                        "-fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 20; -fx-cursor: hand;");
        refreshButton.setOnAction(e -> loadInvites());

        Button closeButton = new Button("Đóng");
        closeButton.setStyle(
                "-fx-background-color: #6c757d; -fx-text-fill: white; " +
                        "-fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 20; -fx-cursor: hand;");
        closeButton.setOnAction(e -> close());

        headerBox.getChildren().addAll(titleLabel, spacer, refreshButton, closeButton);

        // Loading overlay
        StackPane contentPane = new StackPane();
        loadingOverlay = createLoadingOverlay();
        contentPane.getChildren().addAll(inviteListView, loadingOverlay);

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

    private void loadInvites() {
        loadingOverlay.setVisible(true);
        statusLabel.setText("Đang tải lời mời...");

        // Load invites in background
        new Thread(() -> {
            try {
                List<Map<String, Object>> inviteList = chatService.getPendingRoomInvites();
                javafx.application.Platform.runLater(() -> {
                    invites.clear();
                    invites.addAll(inviteList);
                    loadingOverlay.setVisible(false);

                    if (inviteList.isEmpty()) {
                        statusLabel.setText("Không có lời mời nào.");
                        loadingOverlay.setVisible(true);
                    }
                });
            } catch (Exception e) {
                log.error("Failed to load room invites", e);
                javafx.application.Platform.runLater(() -> {
                    statusLabel.setText("Lỗi khi tải lời mời: " + e.getMessage());
                    loadingOverlay.setVisible(true);
                });
            }
        }).start();
    }

    /**
     * Custom ListCell for displaying room invites
     */
    private class InviteListCell extends ListCell<Map<String, Object>> {
        private final VBox content;
        private final Label roomNameLabel;
        private final Label inviterLabel;
        private final Label roomTypeLabel;
        private final HBox buttonBox;
        private final Button acceptButton;
        private final Button declineButton;

        public InviteListCell() {
            content = new VBox(8);
            content.getStyleClass().add("invite-item");

            roomNameLabel = new Label();
            roomNameLabel.getStyleClass().add("invite-header");

            inviterLabel = new Label();
            inviterLabel.getStyleClass().add("invite-details");

            roomTypeLabel = new Label();
            roomTypeLabel.getStyleClass().add("invite-details");

            acceptButton = new Button("✅ Chấp nhận");
            acceptButton.getStyleClass().add("accept-button");
            acceptButton.setOnAction(e -> handleAccept(getItem()));

            declineButton = new Button("❌ Từ chối");
            declineButton.getStyleClass().add("decline-button");
            declineButton.setOnAction(e -> handleDecline(getItem()));

            buttonBox = new HBox(10);
            buttonBox.getStyleClass().add("invite-buttons");
            buttonBox.getChildren().addAll(acceptButton, declineButton);

            content.getChildren().addAll(roomNameLabel, inviterLabel, roomTypeLabel, buttonBox);
        }

        @Override
        protected void updateItem(Map<String, Object> invite, boolean empty) {
            super.updateItem(invite, empty);

            if (empty || invite == null) {
                setGraphic(null);
            } else {
                // Extract room info
                Map<String, Object> room = (Map<String, Object>) invite.get("room");
                String roomName = room != null ? (String) room.get("name") : "Unknown Room";

                // Extract inviter info
                Map<String, Object> inviter = (Map<String, Object>) invite.get("inviter");
                String inviterName = "Unknown";
                if (inviter != null) {
                    String displayName = (String) inviter.get("displayName");
                    String username = (String) inviter.get("username");
                    inviterName = displayName != null && !displayName.trim().isEmpty() ? displayName : "@" + username;
                }

                // Extract room type
                String roomType = room != null ? (String) room.get("type") : "PRIVATE";
                if (roomType == null)
                    roomType = "PRIVATE";

                roomNameLabel.setText("🏠 " + roomName);
                inviterLabel.setText("Mời bởi: " + inviterName);
                roomTypeLabel.setText("Loại phòng: " + roomType);

                setGraphic(content);
            }
        }

        private void handleAccept(Map<String, Object> invite) {
            if (invite == null)
                return;

            Long inviteId = ((Number) invite.get("id")).longValue();
            acceptButton.setDisable(true);
            declineButton.setDisable(true);

            new Thread(() -> {
                try {
                    boolean success = chatService.acceptRoomInvite(inviteId);
                    javafx.application.Platform.runLater(() -> {
                        if (success) {
                            invites.remove(invite);
                            showAlert("Thành công", "Đã chấp nhận lời mời tham gia phòng!");
                        } else {
                            acceptButton.setDisable(false);
                            declineButton.setDisable(false);
                            showAlert("Lỗi", "Không thể chấp nhận lời mời.");
                        }
                    });
                } catch (Exception e) {
                    log.error("Failed to accept room invite", e);
                    javafx.application.Platform.runLater(() -> {
                        acceptButton.setDisable(false);
                        declineButton.setDisable(false);
                        showAlert("Lỗi", "Lỗi khi chấp nhận lời mời: " + e.getMessage());
                    });
                }
            }).start();
        }

        private void handleDecline(Map<String, Object> invite) {
            if (invite == null)
                return;

            Long inviteId = ((Number) invite.get("id")).longValue();
            acceptButton.setDisable(true);
            declineButton.setDisable(true);

            new Thread(() -> {
                try {
                    boolean success = chatService.declineRoomInvite(inviteId);
                    javafx.application.Platform.runLater(() -> {
                        if (success) {
                            invites.remove(invite);
                            showAlert("Thành công", "Đã từ chối lời mời.");
                        } else {
                            acceptButton.setDisable(false);
                            declineButton.setDisable(false);
                            showAlert("Lỗi", "Không thể từ chối lời mời.");
                        }
                    });
                } catch (Exception e) {
                    log.error("Failed to decline room invite", e);
                    javafx.application.Platform.runLater(() -> {
                        acceptButton.setDisable(false);
                        declineButton.setDisable(false);
                        showAlert("Lỗi", "Lỗi khi từ chối lời mời: " + e.getMessage());
                    });
                }
            }).start();
        }

        private void showAlert(String title, String message) {
            javafx.application.Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(title);
                alert.setHeaderText(null);
                alert.setContentText(message);
                alert.showAndWait();
            });
        }
    }
}