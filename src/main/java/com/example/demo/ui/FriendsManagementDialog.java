package com.example.demo.ui;

import com.example.demo.client.model.User;
import com.example.demo.client.service.ChatService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.scene.shape.Circle;
import javafx.scene.effect.DropShadow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Friends Management Dialog - Add friends, manage requests, and start
 * conversations
 */
@Slf4j
public class FriendsManagementDialog extends Stage {

    private final ChatService chatService;

    // UI Components
    private ListView<Map<String, Object>> friendsList;
    private ListView<Map<String, Object>> pendingRequestsList;
    private TextField searchField;
    private ListView<User> searchResultsList;
    private Button addFriendButton;
    private Button refreshButton;
    private Label friendCountLabel;

    // Callback for starting private chat
    private Consumer<User> onMessageClicked;

    // Avatar colors
    private static final Color[] AVATAR_COLORS = {
            Color.web("#667eea"), Color.web("#764ba2"), Color.web("#f093fb"),
            Color.web("#f5576c"), Color.web("#4facfe"), Color.web("#43e97b"),
            Color.web("#fa709a"), Color.web("#30cfd0")
    };

    // Callback for badge refresh when accept/decline
    private Runnable onBadgeUpdate;

    public FriendsManagementDialog(ChatService chatService) {
        this.chatService = chatService;

        initModality(Modality.APPLICATION_MODAL);
        setTitle("👥 Quản lý Bạn bè");
        setResizable(true);
        setWidth(750);
        setHeight(650);

        initComponents();
        loadData();
        setupEventHandlers();

        Scene scene = new Scene(createLayout());
        if (getClass().getResource("/styles.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        }
        setScene(scene);
    }

    public void setOnMessageClicked(Consumer<User> callback) {
        this.onMessageClicked = callback;
    }

    public void setOnBadgeUpdate(Runnable callback) {
        this.onBadgeUpdate = callback;
    }

    private void initComponents() {
        // Friends list
        friendsList = new ListView<>();
        friendsList.setPrefHeight(250);
        friendsList.setCellFactory(param -> new FriendListCell());

        // Pending requests
        pendingRequestsList = new ListView<>();
        pendingRequestsList.setPrefHeight(180);
        pendingRequestsList.setCellFactory(param -> new PendingRequestCell());

        // Search components
        searchField = new TextField();
        searchField.setPromptText("🔍 Tìm kiếm người dùng...");
        searchField.setStyle(
                "-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; " +
                        "-fx-border-radius: 25; -fx-background-radius: 25; -fx-padding: 10 20;");

        searchResultsList = new ListView<>();
        searchResultsList.setPrefHeight(180);
        searchResultsList.setCellFactory(param -> new UserSearchCell());

        // Buttons - Nút gửi lời mời nổi bật với màu đỏ đậm
        addFriendButton = new Button("🔥 Gửi lời mời kết bạn");
        addFriendButton.setStyle(
                "-fx-background-color: #dc2626; " +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 12 25; " +
                        "-fx-background-radius: 25; -fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(220,38,38,0.6), 12, 0, 0, 4);");
        addFriendButton.setDisable(true);

        refreshButton = new Button("🔄 Làm mới");
        refreshButton.setStyle(
                "-fx-background-color: #4ade80; -fx-text-fill: white; " +
                        "-fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 20; -fx-cursor: hand;");
    }

    private VBox createLayout() {
        // Check dark mode state
        boolean isDark = SettingsDialog.isDarkTheme();
        String bgColor = isDark ? "#1e293b" : "#ffffff";
        String textColor = isDark ? "#e2e8f0" : "#212529";
        String mutedColor = isDark ? "#9ca3af" : "#6c757d";

        VBox mainLayout = new VBox(20);
        mainLayout.setPadding(new Insets(25));
        mainLayout.setStyle("-fx-background-color: " + bgColor + ";");

        // Header with gradient icon
        HBox headerBox = new HBox(15);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Circle headerIcon = new Circle(25);
        headerIcon.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#667eea")),
                new Stop(1, Color.web("#764ba2"))));
        headerIcon.setEffect(new DropShadow(8, Color.web("#667eea50")));

        Label iconEmoji = new Label("👥");
        iconEmoji.setStyle("-fx-font-size: 20px;");
        StackPane iconPane = new StackPane(headerIcon, iconEmoji);

        VBox titleBox = new VBox(2);
        Label titleLabel = new Label("Quản lý Bạn bè");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + textColor + ";");
        Label subtitleLabel = new Label("Kết nối và trò chuyện với bạn bè");
        subtitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + mutedColor + ";");
        titleBox.getChildren().addAll(titleLabel, subtitleLabel);

        headerBox.getChildren().addAll(iconPane, titleBox);

        // Tab pane with custom styling
        TabPane tabPane = new TabPane();
        tabPane.setStyle("-fx-background-color: transparent;");

        // Friends Tab
        Tab friendsTab = new Tab("👥 Bạn bè", createFriendsTab());
        friendsTab.setClosable(false);

        // Add Friends Tab
        Tab addFriendsTab = new Tab("🔍 Thêm bạn", createAddFriendsTab());
        addFriendsTab.setClosable(false);

        // Requests Tab
        Tab requestsTab = new Tab("📨 Lời mời", createRequestsTab());
        requestsTab.setClosable(false);

        tabPane.getTabs().addAll(friendsTab, addFriendsTab, requestsTab);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        // Bottom buttons
        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button closeButton = new Button("Đóng");
        closeButton.setStyle(
                "-fx-background-color: #6c757d; -fx-text-fill: white; " +
                        "-fx-font-weight: bold; -fx-padding: 10 25; -fx-background-radius: 20; -fx-cursor: hand;");
        closeButton.setOnAction(e -> close());

        buttonBox.getChildren().addAll(refreshButton, closeButton);

        mainLayout.getChildren().addAll(headerBox, tabPane, buttonBox);

        return mainLayout;
    }

    private VBox createFriendsTab() {
        VBox tabContent = new VBox(15);
        tabContent.setPadding(new Insets(15));

        // Stats card
        HBox statsCard = new HBox(20);
        statsCard.setPadding(new Insets(15));
        statsCard.setStyle(
                "-fx-background-color: linear-gradient(135deg, #667eea20 0%, #764ba220 100%); " +
                        "-fx-background-radius: 12;");
        statsCard.setAlignment(Pos.CENTER_LEFT);

        VBox statBox = new VBox(2);
        friendCountLabel = new Label("0");
        friendCountLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #667eea;");
        Label statLabel = new Label("Bạn bè");
        statLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6c757d;");
        statBox.getChildren().addAll(friendCountLabel, statLabel);

        statsCard.getChildren().add(statBox);

        Label label = new Label("Danh sách bạn bè của bạn:");
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #495057;");

        VBox.setVgrow(friendsList, Priority.ALWAYS);
        friendsList.setStyle(
                "-fx-background-color: #f8f9fa; -fx-background-radius: 12; " +
                        "-fx-border-radius: 12; -fx-border-color: #e9ecef;");

        tabContent.getChildren().addAll(statsCard, label, friendsList);
        return tabContent;
    }

    private VBox createAddFriendsTab() {
        VBox tabContent = new VBox(15);
        tabContent.setPadding(new Insets(15));

        Label searchLabel = new Label("Tìm kiếm người dùng:");
        searchLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #495057;");

        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button searchButton = new Button("Tìm");
        searchButton.setStyle(
                "-fx-background-color: #667eea; -fx-text-fill: white; " +
                        "-fx-padding: 10 20; -fx-background-radius: 20; -fx-cursor: hand;");
        searchButton.setOnAction(e -> performSearch());

        searchBox.getChildren().addAll(searchField, searchButton);

        Label resultsLabel = new Label("Kết quả tìm kiếm:");
        resultsLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #495057;");

        VBox.setVgrow(searchResultsList, Priority.ALWAYS);
        searchResultsList.setStyle(
                "-fx-background-color: #f8f9fa; -fx-background-radius: 12; " +
                        "-fx-border-radius: 12; -fx-border-color: #e9ecef;");

        tabContent.getChildren().addAll(
                searchLabel, searchBox,
                resultsLabel, searchResultsList, addFriendButton);

        return tabContent;
    }

    private VBox createRequestsTab() {
        VBox tabContent = new VBox(15);
        tabContent.setPadding(new Insets(15));

        Label label = new Label("Lời mời kết bạn đang chờ:");
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #495057;");

        VBox.setVgrow(pendingRequestsList, Priority.ALWAYS);
        pendingRequestsList.setStyle(
                "-fx-background-color: #f8f9fa; -fx-background-radius: 12; " +
                        "-fx-border-radius: 12; -fx-border-color: #e9ecef;");
        pendingRequestsList.setPlaceholder(new Label("Không có lời mời nào"));

        tabContent.getChildren().addAll(label, pendingRequestsList);
        return tabContent;
    }

    private void loadData() {
        loadFriends();
        loadPendingRequests();
    }

    private void loadFriends() {
        try {
            List<Map<String, Object>> friends = chatService.getFriends();
            friendsList.getItems().clear();

            // Get current user to filter out self
            User currentUser = chatService.getCurrentUser();
            Long currentUserId = currentUser != null ? currentUser.getId() : null;

            for (Map<String, Object> friend : friends) {
                Long friendId = Long.valueOf(friend.get("id").toString());

                // Skip current user - don't show yourself in friends list
                if (currentUserId != null && friendId.equals(currentUserId)) {
                    continue;
                }

                friendsList.getItems().add(friend);
            }

            // Cập nhật số lượng bạn bè
            if (friendCountLabel != null) {
                friendCountLabel.setText(String.valueOf(friendsList.getItems().size()));
            }
        } catch (Exception e) {
            log.error("Error loading friends", e);
        }
    }

    private void loadPendingRequests() {
        try {
            List<Map<String, Object>> requests = chatService.getPendingRequests();
            pendingRequestsList.getItems().clear();
            pendingRequestsList.getItems().addAll(requests);
        } catch (Exception e) {
            log.error("Error loading pending requests", e);
        }
    }

    private void performSearch() {
        String keyword = searchField.getText().trim();
        if (!keyword.isEmpty()) {
            try {
                List<User> results = chatService.searchUsers(keyword);
                int originalCount = results.size();

                // Filter out current user from search results
                User currentUser = chatService.getCurrentUser();
                if (currentUser != null) {
                    results.removeIf(u -> u.getId().equals(currentUser.getId()));
                }

                // Also filter out users who are already friends
                List<Long> friendIds = friendsList.getItems().stream()
                        .map(f -> Long.valueOf(f.get("id").toString()))
                        .collect(java.util.stream.Collectors.toList());
                int beforeFriendFilter = results.size();
                results.removeIf(u -> friendIds.contains(u.getId()));

                searchResultsList.getItems().clear();
                searchResultsList.getItems().addAll(results);

                // Show info message if all results were filtered because they're already
                // friends
                if (originalCount > 0 && results.isEmpty() && beforeFriendFilter > 0) {
                    searchResultsList.setPlaceholder(new Label("Người dùng đã là bạn của bạn rồi!"));
                } else if (results.isEmpty()) {
                    searchResultsList.setPlaceholder(new Label("Không tìm thấy người dùng nào"));
                }
            } catch (Exception e) {
                log.error("Error searching users", e);
            }
        }
    }

    private void setupEventHandlers() {
        searchField.setOnAction(e -> performSearch());

        searchResultsList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            addFriendButton.setDisable(newVal == null);
        });

        addFriendButton.setOnAction(e -> {
            User selected = searchResultsList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                String errorMessage = chatService.addFriend(selected.getId());
                if (errorMessage == null) {
                    showInfo("Thành công", "Đã gửi lời mời kết bạn đến " + selected.getDisplayName() + "!");
                    searchResultsList.getItems().remove(selected);
                } else {
                    showError("Lỗi", errorMessage);
                }
            }
        });

        refreshButton.setOnAction(e -> {
            loadData();
            searchResultsList.getItems().clear();
            searchField.clear();
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

    private void showInfo(String title, String message) {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // Custom cell for friends list with Message and Remove buttons
    private class FriendListCell extends ListCell<Map<String, Object>> {
        @Override
        protected void updateItem(Map<String, Object> friend, boolean empty) {
            super.updateItem(friend, empty);
            if (empty || friend == null) {
                setText(null);
                setGraphic(null);
            } else {
                String displayName = (String) friend.get("displayName");
                String username = (String) friend.get("username");
                String status = friend.get("status") != null ? friend.get("status").toString() : "OFFLINE";
                boolean isOnline = "ONLINE".equals(status);

                // Check if friend has hidden their online status
                Object showOnlineStatusObj = friend.get("showOnlineStatus");
                boolean hideOnlineStatus = showOnlineStatusObj != null && showOnlineStatusObj.equals(Boolean.FALSE);

                HBox friendBox = new HBox(12);
                friendBox.setAlignment(Pos.CENTER_LEFT);
                friendBox.setPadding(new Insets(12, 15, 12, 15));
                friendBox.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

                // Avatar with initial
                StackPane avatarPane = new StackPane();
                Circle avatar = new Circle(22);
                int hash = Math.abs((username != null ? username : "").hashCode());
                avatar.setFill(AVATAR_COLORS[hash % AVATAR_COLORS.length]);
                avatar.setEffect(new DropShadow(4, Color.web("#00000020")));

                String initial = displayName != null && displayName.length() > 0
                        ? displayName.substring(0, 1).toUpperCase()
                        : "?";
                Label initialLabel = new Label(initial);
                initialLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
                avatarPane.getChildren().addAll(avatar, initialLabel);

                // Info
                VBox infoBox = new VBox(3);
                Label nameLabel = new Label(displayName != null ? displayName : username);
                nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #212529;");

                HBox statusBox = new HBox(5);
                statusBox.setAlignment(Pos.CENTER_LEFT);
                Circle statusDot = new Circle(4);
                Label statusLabel;

                if (hideOnlineStatus) {
                    // Friend has hidden their status - show "Ẩn trạng thái"
                    statusDot.setFill(Color.web("#9ca3af"));
                    statusLabel = new Label("Ẩn trạng thái");
                } else if (isOnline) {
                    statusDot.setFill(Color.web("#4ade80"));
                    statusLabel = new Label("Online");
                } else {
                    statusDot.setFill(Color.web("#9ca3af"));
                    statusLabel = new Label("Offline");
                }
                statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d;");
                statusBox.getChildren().addAll(statusDot, statusLabel);

                infoBox.getChildren().addAll(nameLabel, statusBox);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                // Message button
                Button messageBtn = new Button("💬");
                messageBtn.setStyle(
                        "-fx-background-color: #667eea; -fx-text-fill: white; " +
                                "-fx-font-size: 12px; -fx-padding: 8 12; -fx-background-radius: 15; -fx-cursor: hand;");
                messageBtn.setOnMouseEntered(e -> messageBtn.setStyle(
                        "-fx-background-color: #5a67d8; -fx-text-fill: white; " +
                                "-fx-font-size: 12px; -fx-padding: 8 12; -fx-background-radius: 15; -fx-cursor: hand;"));
                messageBtn.setOnMouseExited(e -> messageBtn.setStyle(
                        "-fx-background-color: #667eea; -fx-text-fill: white; " +
                                "-fx-font-size: 12px; -fx-padding: 8 12; -fx-background-radius: 15; -fx-cursor: hand;"));
                messageBtn.setOnAction(e -> {
                    if (onMessageClicked != null) {
                        User user = new User();
                        user.setId(Long.valueOf(friend.get("id").toString()));
                        user.setUsername(username);
                        user.setDisplayName(displayName);
                        user.setAvatarUrl((String) friend.get("avatarUrl"));
                        onMessageClicked.accept(user);
                        close();
                    }
                });

                // Remove friend button
                Button removeBtn = new Button("🗑");
                removeBtn.setStyle(
                        "-fx-background-color: #ef4444; -fx-text-fill: white; " +
                                "-fx-font-size: 12px; -fx-padding: 8 12; -fx-background-radius: 15; -fx-cursor: hand;");
                removeBtn.setOnMouseEntered(e -> removeBtn.setStyle(
                        "-fx-background-color: #dc2626; -fx-text-fill: white; " +
                                "-fx-font-size: 12px; -fx-padding: 8 12; -fx-background-radius: 15; -fx-cursor: hand;"));
                removeBtn.setOnMouseExited(e -> removeBtn.setStyle(
                        "-fx-background-color: #ef4444; -fx-text-fill: white; " +
                                "-fx-font-size: 12px; -fx-padding: 8 12; -fx-background-radius: 15; -fx-cursor: hand;"));
                removeBtn.setOnAction(e -> handleRemoveFriend(friend));

                friendBox.getChildren().addAll(avatarPane, infoBox, spacer, messageBtn, removeBtn);

                // Hover effect for row
                friendBox.setOnMouseEntered(e -> friendBox.setStyle(
                        "-fx-background-color: #f8f9fa; -fx-background-radius: 10;"));
                friendBox.setOnMouseExited(e -> friendBox.setStyle(
                        "-fx-background-color: white; -fx-background-radius: 10;"));

                setGraphic(friendBox);
                setText(null);
                setStyle("-fx-background-color: transparent; -fx-padding: 3 0;");
            }
        }

        private void handleRemoveFriend(Map<String, Object> friend) {
            String displayName = (String) friend.get("displayName");
            String username = (String) friend.get("username");
            String name = displayName != null ? displayName : username;

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Xác nhận xóa bạn");
            confirm.setHeaderText(null);
            confirm.setContentText("Bạn có chắc chắn muốn xóa " + name + " khỏi danh sách bạn bè?");

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        Object friendshipIdObj = friend.get("friendshipId");
                        if (friendshipIdObj != null) {
                            Long friendshipId = Long.valueOf(friendshipIdObj.toString());
                            boolean success = chatService.removeFriend(friendshipId);
                            if (success) {
                                showInfo("Thành công", "Đã xóa " + name + " khỏi danh sách bạn bè");
                                loadFriends();
                            } else {
                                showError("Lỗi", "Không thể xóa bạn bè");
                            }
                        } else {
                            showError("Lỗi", "Không tìm thấy thông tin để xóa bạn");
                        }
                    } catch (Exception ex) {
                        log.error("Error removing friend", ex);
                        showError("Lỗi", "Lỗi xử lý: " + ex.getMessage());
                    }
                }
            });
        }
    }

    // Custom cell for pending requests
    private class PendingRequestCell extends ListCell<Map<String, Object>> {
        @Override
        protected void updateItem(Map<String, Object> request, boolean empty) {
            super.updateItem(request, empty);
            if (empty || request == null) {
                setText(null);
                setGraphic(null);
            } else {
                HBox requestBox = new HBox(12);
                requestBox.setAlignment(Pos.CENTER_LEFT);
                requestBox.setPadding(new Insets(12, 15, 12, 15));
                requestBox.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

                // Avatar
                StackPane avatarPane = new StackPane();
                Circle avatar = new Circle(18);

                // Extract sender info from nested "user" object in API response
                // API returns: { user: {displayName, username, ...}, friend: {...} }
                String senderName = null;
                @SuppressWarnings("unchecked")
                Map<String, Object> senderUser = (Map<String, Object>) request.get("user");
                if (senderUser != null) {
                    senderName = (String) senderUser.get("displayName");
                    if (senderName == null || senderName.isEmpty()) {
                        senderName = (String) senderUser.get("username");
                    }
                }
                // Fallback to old field names for backwards compatibility
                if (senderName == null) {
                    senderName = (String) request.get("senderDisplayName");
                }
                if (senderName == null) {
                    senderName = (String) request.get("senderUsername");
                }
                if (senderName == null) {
                    senderName = "Unknown";
                }

                int hash = Math.abs(senderName.hashCode());
                avatar.setFill(AVATAR_COLORS[hash % AVATAR_COLORS.length]);

                String initial = senderName.length() > 0 ? senderName.substring(0, 1).toUpperCase() : "?";
                Label initialLabel = new Label(initial);
                initialLabel.setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");
                avatarPane.getChildren().addAll(avatar, initialLabel);

                // Info
                VBox infoBox = new VBox(2);
                Label nameLabel = new Label(senderName);
                nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

                Label typeLabel = new Label("Muốn kết bạn với bạn");
                typeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d;");

                infoBox.getChildren().addAll(nameLabel, typeLabel);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                // Action buttons
                Button acceptButton = new Button("✓ Chấp nhận");
                acceptButton.setStyle(
                        "-fx-background-color: #4ade80; -fx-text-fill: white; " +
                                "-fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 12; -fx-cursor: hand;");
                acceptButton.setOnAction(e -> handleAcceptRequest(request));

                Button declineButton = new Button("✕");
                declineButton.setStyle(
                        "-fx-background-color: #f87171; -fx-text-fill: white; " +
                                "-fx-font-size: 11px; -fx-padding: 6 10; -fx-background-radius: 12; -fx-cursor: hand;");
                declineButton.setOnAction(e -> handleDeclineRequest(request));

                requestBox.getChildren().addAll(avatarPane, infoBox, spacer, acceptButton, declineButton);

                setGraphic(requestBox);
                setText(null);
                setStyle("-fx-background-color: transparent; -fx-padding: 3 0;");
            }
        }

        private void handleAcceptRequest(Map<String, Object> request) {
            try {
                Long requestId = Long.valueOf(request.get("id").toString());
                boolean success = chatService.acceptFriendRequest(requestId);
                if (success) {
                    showInfo("Thành công", "Đã chấp nhận lời mời kết bạn!");
                    loadData();
                    if (onBadgeUpdate != null)
                        onBadgeUpdate.run();
                } else {
                    showError("Lỗi", "Không thể chấp nhận lời mời");
                }
            } catch (Exception e) {
                log.error("Error accepting request", e);
                showError("Lỗi", "Lỗi xử lý: " + e.getMessage());
            }
        }

        private void handleDeclineRequest(Map<String, Object> request) {
            try {
                Long requestId = Long.valueOf(request.get("id").toString());
                boolean success = chatService.declineFriendRequest(requestId);
                if (success) {
                    showInfo("Đã từ chối", "Đã từ chối lời mời kết bạn");
                    loadData();
                    if (onBadgeUpdate != null)
                        onBadgeUpdate.run();
                } else {
                    showError("Lỗi", "Không thể từ chối lời mời");
                }
            } catch (Exception e) {
                log.error("Error declining request", e);
                showError("Lỗi", "Lỗi xử lý: " + e.getMessage());
            }
        }
    }

    // Custom cell for user search results
    private class UserSearchCell extends ListCell<User> {
        @Override
        protected void updateItem(User user, boolean empty) {
            super.updateItem(user, empty);
            if (empty || user == null) {
                setText(null);
                setGraphic(null);
            } else {
                HBox userBox = new HBox(12);
                userBox.setAlignment(Pos.CENTER_LEFT);
                userBox.setPadding(new Insets(10, 12, 10, 12));
                userBox.setStyle("-fx-background-color: white; -fx-background-radius: 8;");

                // Avatar
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

                Label nameLabel = new Label(user.getDisplayName() != null ? user.getDisplayName() : user.getUsername());
                nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

                Label usernameLabel = new Label("@" + user.getUsername());
                usernameLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");

                userBox.getChildren().addAll(avatarPane, nameLabel, usernameLabel);

                // Hover
                userBox.setOnMouseEntered(e -> userBox.setStyle(
                        "-fx-background-color: #f0f2f5; -fx-background-radius: 8;"));
                userBox.setOnMouseExited(e -> userBox.setStyle(
                        "-fx-background-color: white; -fx-background-radius: 8;"));

                setGraphic(userBox);
                setText(null);
                setStyle("-fx-background-color: transparent; -fx-padding: 2 0;");
            }
        }
    }
}