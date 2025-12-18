package com.example.demo.ui;

import atlantafx.base.theme.Styles;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.scene.shape.Circle;
import javafx.scene.effect.DropShadow;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Consumer;

/**
 * Modern Sidebar with Direct Messages, Rooms, and Online Users
 */
@Getter
public class Sidebar extends VBox {

    private ListView<String> userListView;
    private ListView<String> friendsListView;
    private ComboBox<String> roomSelector;
    private Button settingsButton;
    private Button profileButton;
    private Button addRoomButton;
    private Button friendsButton;
    private Button invitesButton;
    private Button historyButton;
    private Label currentUserLabel;
    private Circle userAvatar;
    private Label onlineCountLabel;
    private Label friendsCountLabel;
    private TabPane sidebarTabs;

    private Consumer<String> onRoomSelected;
    private Consumer<Void> onSettingsClicked;
    private Consumer<Void> onProfileClicked;
    private Consumer<Void> onAddRoomClicked;
    private Consumer<Void> onFriendsClicked;
    private Consumer<Void> onInvitesClicked;
    private Consumer<Void> onHistoryClicked;
    private Consumer<String> onUserClicked;
    private Consumer<com.example.demo.client.model.User> onFriendMessageClicked;

    // Store friends for direct access
    private List<com.example.demo.client.model.User> loadedFriends;

    // Avatar colors
    private static final Color[] AVATAR_COLORS = {
            Color.web("#667eea"), Color.web("#764ba2"), Color.web("#f093fb"),
            Color.web("#f5576c"), Color.web("#4facfe"), Color.web("#43e97b"),
            Color.web("#fa709a"), Color.web("#30cfd0")
    };

    // Unread badge tracking
    private final Map<Long, Integer> unreadCounts = new HashMap<>();
    private int friendRequestBadgeCount = 0;
    private int roomInviteBadgeCount = 0;
    private Label dmBadgeLabel;

    public Sidebar() {
        super(0);
        setPrefWidth(300);
        getStyleClass().add("sidebar");

        createHeader();
        createNavigationSection();
        createTabbedSection();

        // Apply Atlantafx styles
        getStyleClass().add(Styles.DENSE);
    }

    private void createHeader() {
        VBox headerBox = new VBox(12);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setPadding(new Insets(25, 20, 20, 20));
        headerBox.getStyleClass().add("sidebar-header");

        // User avatar with gradient and shadow
        StackPane avatarContainer = new StackPane();

        userAvatar = new Circle(35);
        userAvatar.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#667eea")),
                new Stop(1, Color.web("#764ba2"))));
        userAvatar.setStroke(Color.WHITE);
        userAvatar.setStrokeWidth(3);
        userAvatar.setEffect(new DropShadow(10, Color.web("#00000040")));

        // Avatar hover animation
        avatarContainer.setOnMouseEntered(e -> {
            userAvatar.setScaleX(1.1);
            userAvatar.setScaleY(1.1);
        });
        avatarContainer.setOnMouseExited(e -> {
            userAvatar.setScaleX(1.0);
            userAvatar.setScaleY(1.0);
        });

        avatarContainer.getChildren().add(userAvatar);

        // Current user label with glow effect
        currentUserLabel = new Label("Guest User");
        currentUserLabel.getStyleClass().add("sidebar-header-label");
        currentUserLabel.setAlignment(Pos.CENTER);

        // Online status badge with glow
        HBox statusBox = new HBox(6);
        statusBox.setAlignment(Pos.CENTER);

        Circle onlineIndicator = new Circle(5);
        onlineIndicator.setFill(Color.web("#4ade80"));
        onlineIndicator.setEffect(new DropShadow(6, Color.web("#4ade8080")));

        Label statusLabel = new Label("Online");
        statusLabel.getStyleClass().add("sidebar-status-label");

        statusBox.getChildren().addAll(onlineIndicator, statusLabel);

        headerBox.getChildren().addAll(avatarContainer, currentUserLabel, statusBox);
        getChildren().add(headerBox);
    }

    private void createNavigationSection() {
        // Quick action buttons in a more compact grid
        HBox quickActions = new HBox(10);
        quickActions.setPadding(new Insets(15, 15, 15, 15));
        quickActions.setAlignment(Pos.CENTER);
        quickActions.setStyle("-fx-background-color: rgba(0,0,0,0.15); -fx-background-radius: 15;");

        profileButton = createQuickButton("👤", "Profile", "#FF6B6B");
        friendsButton = createQuickButton("👥", "Friends", "#4ECDC4");
        Button roomManageButton = createQuickButton("🏠", "Phòng", "#45B7D1");
        settingsButton = createQuickButton("⚙️", "Settings", "#96CEB4");

        profileButton.setOnAction(e -> {
            if (onProfileClicked != null)
                onProfileClicked.accept(null);
        });
        friendsButton.setOnAction(e -> {
            if (onFriendsClicked != null)
                onFriendsClicked.accept(null);
        });
        roomManageButton.setOnAction(e -> {
            if (onAddRoomClicked != null)
                onAddRoomClicked.accept(null);
        });
        settingsButton.setOnAction(e -> {
            if (onSettingsClicked != null)
                onSettingsClicked.accept(null);
        });

        quickActions.getChildren().addAll(profileButton, friendsButton, roomManageButton, settingsButton);

        VBox navContainer = new VBox(10);
        navContainer.setPadding(new Insets(5, 15, 5, 15));
        navContainer.getChildren().add(quickActions);

        getChildren().add(navContainer);
    }

    private Button createQuickButton(String icon, String tooltip, String bgColor) {
        Button btn = new Button(icon);
        btn.setTooltip(new Tooltip(tooltip));
        btn.setStyle(
                "-fx-background-color: " + bgColor + "; " +
                        "-fx-text-fill: white; -fx-font-size: 20px; " +
                        "-fx-min-width: 55; -fx-min-height: 55; -fx-max-width: 55; -fx-max-height: 55; " +
                        "-fx-background-radius: 15; -fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 8, 0, 0, 3);");
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: derive(" + bgColor + ", 20%); " +
                        "-fx-text-fill: white; -fx-font-size: 20px; " +
                        "-fx-min-width: 55; -fx-min-height: 55; -fx-max-width: 55; -fx-max-height: 55; " +
                        "-fx-background-radius: 15; -fx-cursor: hand; " +
                        "-fx-scale-x: 1.15; -fx-scale-y: 1.15; " +
                        "-fx-effect: dropshadow(gaussian, " + bgColor + ", 15, 0.5, 0, 0);"));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: " + bgColor + "; " +
                        "-fx-text-fill: white; -fx-font-size: 20px; " +
                        "-fx-min-width: 55; -fx-min-height: 55; -fx-max-width: 55; -fx-max-height: 55; " +
                        "-fx-background-radius: 15; -fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 8, 0, 0, 3);"));
        return btn;
    }

    private void createTabbedSection() {
        VBox tabbedContainer = new VBox();
        tabbedContainer.setPadding(new Insets(5, 10, 10, 10));
        VBox.setVgrow(tabbedContainer, Priority.ALWAYS);

        sidebarTabs = new TabPane();
        sidebarTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        sidebarTabs.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-tab-min-width: 80;");
        VBox.setVgrow(sidebarTabs, Priority.ALWAYS);

        // Direct Messages Tab (Friends)
        Tab dmTab = new Tab("💬 DMs");
        dmTab.setContent(createDirectMessagesTab());

        // Rooms Tab
        Tab roomsTab = new Tab("🏠 Rooms");
        roomsTab.setContent(createRoomsTab());

        // Online Users Tab
        Tab onlineTab = new Tab("🟢 Online");
        onlineTab.setContent(createOnlineUsersTab());

        sidebarTabs.getTabs().addAll(dmTab, roomsTab, onlineTab);

        tabbedContainer.getChildren().add(sidebarTabs);
        getChildren().add(tabbedContainer);
    }

    private VBox createDirectMessagesTab() {
        VBox dmBox = new VBox(10);
        dmBox.setPadding(new Insets(12, 8, 8, 8));
        dmBox.setStyle("-fx-background-color: rgba(0,0,0,0.1); -fx-background-radius: 12;");

        // Header with icon and count
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(8, 12, 8, 12));
        header.setStyle("-fx-background-color: linear-gradient(to right, #667eea, #764ba2); " +
                "-fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);");

        Label iconLabel = new Label("💬");
        iconLabel.setStyle("-fx-font-size: 18px;");

        Label titleLabel = new Label("Direct Messages");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        friendsCountLabel = new Label("✨ 0");
        friendsCountLabel.setStyle(
                "-fx-background-color: #FFD93D; -fx-text-fill: #333; " +
                        "-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 4 10 4 10; -fx-background-radius: 15;");

        header.getChildren().addAll(iconLabel, titleLabel, spacer, friendsCountLabel);

        // Friends list for direct messaging
        friendsListView = new ListView<>();
        friendsListView.setStyle(
                "-fx-background-color: rgba(255,255,255,0.95); " +
                        "-fx-background-radius: 12; -fx-border-radius: 12; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 3);");
        friendsListView.setCellFactory(param -> new FriendDMCell());
        friendsListView.setPlaceholder(new Label("👋 Chưa có bạn bè.\nVào Friends để thêm bạn!"));
        VBox.setVgrow(friendsListView, Priority.ALWAYS);

        // Add new room button with icon
        addRoomButton = new Button("➕ Tạo phòng mới");
        addRoomButton.setMaxWidth(Double.MAX_VALUE);
        addRoomButton.setStyle(
                "-fx-background-color: linear-gradient(to right, #11998e, #38ef7d); " +
                        "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; " +
                        "-fx-padding: 12 20; -fx-background-radius: 12; -fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(17,153,142,0.4), 8, 0, 0, 3);");
        addRoomButton.setOnMouseEntered(e -> addRoomButton.setStyle(
                "-fx-background-color: linear-gradient(to right, #38ef7d, #11998e); " +
                        "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; " +
                        "-fx-padding: 12 20; -fx-background-radius: 12; -fx-cursor: hand; " +
                        "-fx-scale-x: 1.02; -fx-scale-y: 1.02; " +
                        "-fx-effect: dropshadow(gaussian, rgba(56,239,125,0.5), 12, 0, 0, 3);"));
        addRoomButton.setOnMouseExited(e -> addRoomButton.setStyle(
                "-fx-background-color: linear-gradient(to right, #11998e, #38ef7d); " +
                        "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; " +
                        "-fx-padding: 12 20; -fx-background-radius: 12; -fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(17,153,142,0.4), 8, 0, 0, 3);"));
        addRoomButton.setOnAction(e -> {
            if (onAddRoomClicked != null)
                onAddRoomClicked.accept(null);
        });

        dmBox.getChildren().addAll(header, friendsListView, addRoomButton);
        return dmBox;
    }

    private VBox createRoomsTab() {
        VBox roomsBox = new VBox(12);
        roomsBox.setPadding(new Insets(12, 8, 8, 8));
        roomsBox.setStyle("-fx-background-color: rgba(0,0,0,0.1); -fx-background-radius: 12;");

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(8, 12, 8, 12));
        header.setStyle("-fx-background-color: linear-gradient(to right, #f093fb, #f5576c); " +
                "-fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);");

        Label iconLabel = new Label("🏠");
        iconLabel.setStyle("-fx-font-size: 18px;");

        Label roomLabel = new Label("Chat Rooms");
        roomLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        header.getChildren().addAll(iconLabel, roomLabel);

        roomSelector = new ComboBox<>();
        roomSelector.setStyle(
                "-fx-background-color: white; -fx-font-size: 13px; -fx-font-weight: bold; " +
                        "-fx-background-radius: 10; -fx-border-radius: 10; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 5, 0, 0, 2);");
        roomSelector.setMaxWidth(Double.MAX_VALUE);
        roomSelector.setPrefHeight(45);
        roomSelector.setOnAction(e -> {
            if (onRoomSelected != null) {
                onRoomSelected.accept(roomSelector.getValue());
            }
        });

        // More room actions with colored buttons
        HBox roomActions = new HBox(10);
        roomActions.setAlignment(Pos.CENTER);

        invitesButton = createColoredSmallButton("📨", "Lời mời", "#FF6B6B");
        historyButton = createColoredSmallButton("📜", "Lịch sử", "#4ECDC4");

        invitesButton.setOnAction(e -> {
            if (onInvitesClicked != null)
                onInvitesClicked.accept(null);
        });
        historyButton.setOnAction(e -> {
            if (onHistoryClicked != null)
                onHistoryClicked.accept(null);
        });

        roomActions.getChildren().addAll(invitesButton, historyButton);

        roomsBox.getChildren().addAll(header, roomSelector, roomActions);
        return roomsBox;
    }

    private Button createColoredSmallButton(String icon, String text, String bgColor) {
        Button btn = new Button(icon + " " + text);
        btn.setStyle(
                "-fx-background-color: " + bgColor + "; -fx-text-fill: white; " +
                        "-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 10 16; " +
                        "-fx-background-radius: 10; -fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);");
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: derive(" + bgColor + ", 15%); -fx-text-fill: white; " +
                        "-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 10 16; " +
                        "-fx-background-radius: 10; -fx-cursor: hand; " +
                        "-fx-scale-x: 1.05; -fx-scale-y: 1.05; " +
                        "-fx-effect: dropshadow(gaussian, " + bgColor + ", 10, 0.3, 0, 0);"));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: " + bgColor + "; -fx-text-fill: white; " +
                        "-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 10 16; " +
                        "-fx-background-radius: 10; -fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);"));
        HBox.setHgrow(btn, Priority.ALWAYS);
        btn.setMaxWidth(Double.MAX_VALUE);
        return btn;
    }

    private VBox createOnlineUsersTab() {
        VBox usersBox = new VBox(10);
        usersBox.setPadding(new Insets(12, 8, 8, 8));
        usersBox.setStyle("-fx-background-color: rgba(0,0,0,0.1); -fx-background-radius: 12;");

        // Header with glow effect
        HBox usersHeader = new HBox(10);
        usersHeader.setAlignment(Pos.CENTER_LEFT);
        usersHeader.setPadding(new Insets(8, 12, 8, 12));
        usersHeader.setStyle("-fx-background-color: linear-gradient(to right, #11998e, #38ef7d); " +
                "-fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);");

        Label iconLabel = new Label("🟢");
        iconLabel.setStyle("-fx-font-size: 16px; -fx-effect: dropshadow(gaussian, #4ade80, 8, 0.8, 0, 0);");

        Label usersLabel = new Label("Online Now");
        usersLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        onlineCountLabel = new Label("⚡ 0");
        onlineCountLabel.setStyle(
                "-fx-background-color: #FFD93D; -fx-text-fill: #333; " +
                        "-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 4 12 4 12; " +
                        "-fx-background-radius: 15; " +
                        "-fx-effect: dropshadow(gaussian, rgba(255,217,61,0.5), 5, 0, 0, 1);");

        usersHeader.getChildren().addAll(iconLabel, usersLabel, spacer, onlineCountLabel);

        userListView = new ListView<>();
        userListView.setStyle(
                "-fx-background-color: rgba(255,255,255,0.95); " +
                        "-fx-background-radius: 12; -fx-border-radius: 12; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 3);");
        userListView.setCellFactory(param -> new UserListCell());
        VBox.setVgrow(userListView, Priority.ALWAYS);

        userListView.setOnMouseClicked(e -> {
            String selectedUser = userListView.getSelectionModel().getSelectedItem();
            if (selectedUser != null && onUserClicked != null) {
                String username = selectedUser.replaceAll("[^a-zA-Z0-9@]", "").trim();
                if (!username.isEmpty()) {
                    onUserClicked.accept(username);
                }
            }
        });

        usersBox.getChildren().addAll(usersHeader, userListView);
        return usersBox;
    }

    /**
     * Load rooms from ChatService
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
     * Load friends for Direct Messages
     */
    public void loadFriends(List<com.example.demo.client.model.User> friends) {
        this.loadedFriends = friends;
        friendsListView.getItems().clear();
        if (friends != null && !friends.isEmpty()) {
            for (com.example.demo.client.model.User friend : friends) {
                String displayName = friend.getDisplayName() != null ? friend.getDisplayName() : friend.getUsername();
                friendsListView.getItems().add(displayName);
            }
            friendsCountLabel.setText(String.valueOf(friends.size()));
        } else {
            friendsCountLabel.setText("0");
        }
    }

    /**
     * Load online users
     */
    public void loadOnlineUsers(List<com.example.demo.client.model.User> users) {
        userListView.getItems().clear();
        if (users != null && !users.isEmpty()) {
            for (com.example.demo.client.model.User user : users) {
                if (user != null) {
                    String displayName = user.getDisplayName() != null ? user.getDisplayName()
                            : (user.getUsername() != null ? user.getUsername() : "Unknown User");
                    userListView.getItems().add(displayName);
                }
            }
            onlineCountLabel.setText(String.valueOf(users.size()));
        } else {
            onlineCountLabel.setText("0");
        }
    }

    public void setCurrentUser(String username) {
        currentUserLabel.setText(username);
        if (username != null && !username.isEmpty()) {
            int hash = Math.abs(username.hashCode());
            userAvatar.setFill(AVATAR_COLORS[hash % AVATAR_COLORS.length]);
        }
    }

    public void setOnlineCount(int count) {
        onlineCountLabel.setText(String.valueOf(count));
    }

    public void addUser(String username) {
        userListView.getItems().add(username);
        setOnlineCount(userListView.getItems().size());
    }

    public void removeUser(String username) {
        userListView.getItems().remove(username);
        setOnlineCount(userListView.getItems().size());
    }

    // Event handler setters
    public void setOnRoomSelected(Consumer<String> handler) {
        this.onRoomSelected = handler;
    }

    public void setOnSettingsClicked(Consumer<Void> handler) {
        this.onSettingsClicked = handler;
    }

    public void setOnProfileClicked(Consumer<Void> handler) {
        this.onProfileClicked = handler;
    }

    public void setOnAddRoomClicked(Consumer<Void> handler) {
        this.onAddRoomClicked = handler;
    }

    public void setOnFriendsClicked(Consumer<Void> handler) {
        this.onFriendsClicked = handler;
    }

    public void setOnInvitesClicked(Consumer<Void> handler) {
        this.onInvitesClicked = handler;
    }

    public void setOnHistoryClicked(Consumer<Void> handler) {
        this.onHistoryClicked = handler;
    }

    public void setOnUserClicked(Consumer<String> handler) {
        this.onUserClicked = handler;
    }

    public void setOnFriendMessageClicked(Consumer<com.example.demo.client.model.User> handler) {
        this.onFriendMessageClicked = handler;
    }

    // ============== Badge Methods ==============

    /**
     * Increment friend request notification badge
     */
    public void incrementFriendRequestBadge() {
        friendRequestBadgeCount++;
        updateFriendsBadge();
    }

    /**
     * Clear friend request badge
     */
    public void clearFriendRequestBadge() {
        friendRequestBadgeCount = 0;
        updateFriendsBadge();
    }

    /**
     * Increment room invite notification badge
     */
    public void incrementRoomInviteBadge() {
        roomInviteBadgeCount++;
        updateInvitesBadge();
    }

    /**
     * Clear room invite badge
     */
    public void clearRoomInviteBadge() {
        roomInviteBadgeCount = 0;
        updateInvitesBadge();
    }

    /**
     * Increment unread count for a specific friend
     */
    public void incrementUnreadCount(Long friendId) {
        unreadCounts.merge(friendId, 1, Integer::sum);
        updateDMBadge();
        friendsListView.refresh(); // Refresh to show badge on friend item
    }

    /**
     * Clear unread count for a specific friend
     */
    public void clearUnreadCount(Long friendId) {
        unreadCounts.remove(friendId);
        updateDMBadge();
        friendsListView.refresh();
    }

    /**
     * Get unread count for a friend
     */
    public int getUnreadCount(Long friendId) {
        return unreadCounts.getOrDefault(friendId, 0);
    }

    /**
     * Get total unread message count
     */
    public int getTotalUnreadCount() {
        return unreadCounts.values().stream().mapToInt(Integer::intValue).sum();
    }

    private void updateDMBadge() {
        int total = getTotalUnreadCount();
        if (dmBadgeLabel != null) {
            if (total > 0) {
                dmBadgeLabel.setText(String.valueOf(total));
                dmBadgeLabel.setVisible(true);
            } else {
                dmBadgeLabel.setVisible(false);
            }
        }
    }

    private void updateFriendsBadge() {
        // Update friends button to show badge
        if (friendRequestBadgeCount > 0) {
            friendsButton.setText("👥 " + friendRequestBadgeCount);
        } else {
            friendsButton.setText("👥");
        }
    }

    private void updateInvitesBadge() {
        // Update invites button to show badge
        if (roomInviteBadgeCount > 0) {
            invitesButton.setText("📨 " + roomInviteBadgeCount);
        } else {
            invitesButton.setText("📨 Lời mời");
        }
    }

    /**
     * Cell for Direct Messages (Friends list)
     */
    private class FriendDMCell extends ListCell<String> {
        @Override
        protected void updateItem(String friendName, boolean empty) {
            super.updateItem(friendName, empty);
            if (empty || friendName == null) {
                setGraphic(null);
                setText(null);
            } else {
                HBox friendBox = new HBox(14);
                friendBox.setAlignment(Pos.CENTER_LEFT);
                friendBox.setPadding(new Insets(14, 16, 14, 16));
                friendBox.setStyle(
                        "-fx-background-color: white; -fx-background-radius: 14; -fx-cursor: hand; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8, 0, 0, 3);");

                // Avatar with vibrant color
                StackPane avatarPane = new StackPane();
                Circle avatar = new Circle(22);
                int hash = Math.abs(friendName.hashCode());
                avatar.setFill(AVATAR_COLORS[hash % AVATAR_COLORS.length]);
                avatar.setEffect(new DropShadow(6, Color.web("#00000030")));

                String initial = friendName.length() > 0 ? friendName.substring(0, 1).toUpperCase() : "?";
                Label initialLabel = new Label(initial);
                initialLabel.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");

                avatarPane.getChildren().addAll(avatar, initialLabel);

                // Info with dark text for contrast
                VBox infoBox = new VBox(4);
                Label nameLabel = new Label("👤 " + friendName);
                nameLabel.setStyle("-fx-text-fill: #333; -fx-font-size: 14px; -fx-font-weight: bold;");

                Label lastMsgLabel = new Label("💬 Nhấn để nhắn tin...");
                lastMsgLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");

                infoBox.getChildren().addAll(nameLabel, lastMsgLabel);
                HBox.setHgrow(infoBox, Priority.ALWAYS);

                // Online indicator with bright glow
                VBox statusBox = new VBox(2);
                statusBox.setAlignment(Pos.CENTER);
                Circle onlineIndicator = new Circle(7);
                onlineIndicator.setFill(Color.web("#4ade80"));
                onlineIndicator.setEffect(new DropShadow(10, Color.web("#4ade80")));
                Label onlineLabel = new Label("🟢");
                onlineLabel.setStyle("-fx-font-size: 8px;");
                statusBox.getChildren().add(onlineIndicator);

                friendBox.getChildren().addAll(avatarPane, infoBox, statusBox);

                // Hover effect with elevation
                friendBox.setOnMouseEntered(e -> friendBox.setStyle(
                        "-fx-background-color: #f0f8ff; -fx-background-radius: 14; -fx-cursor: hand; " +
                                "-fx-scale-x: 1.02; -fx-scale-y: 1.02; " +
                                "-fx-effect: dropshadow(gaussian, rgba(102,126,234,0.35), 15, 0, 0, 5);"));
                friendBox.setOnMouseExited(e -> friendBox.setStyle(
                        "-fx-background-color: white; -fx-background-radius: 14; -fx-cursor: hand; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8, 0, 0, 3);"));

                // Click to open private chat
                friendBox.setOnMouseClicked(e -> {
                    if (loadedFriends != null && onFriendMessageClicked != null) {
                        for (com.example.demo.client.model.User friend : loadedFriends) {
                            String displayName = friend.getDisplayName() != null ? friend.getDisplayName()
                                    : friend.getUsername();
                            if (displayName.equals(friendName)) {
                                onFriendMessageClicked.accept(friend);
                                break;
                            }
                        }
                    }
                });

                setGraphic(friendBox);
                setText(null);
                setStyle("-fx-background-color: transparent; -fx-padding: 3 0 3 0;");
            }
        }
    }

    /**
     * Cell for Online Users list
     */
    private class UserListCell extends ListCell<String> {
        @Override
        protected void updateItem(String user, boolean empty) {
            super.updateItem(user, empty);
            if (empty || user == null) {
                setGraphic(null);
                setText(null);
            } else {
                HBox userBox = new HBox(12);
                userBox.setAlignment(Pos.CENTER_LEFT);
                userBox.setPadding(new Insets(12, 14, 12, 14));
                userBox.setStyle(
                        "-fx-background-color: white; -fx-background-radius: 12; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 6, 0, 0, 2);");

                // Avatar with initial
                StackPane avatarPane = new StackPane();
                Circle avatar = new Circle(18);
                int hash = Math.abs(user.hashCode());
                avatar.setFill(AVATAR_COLORS[hash % AVATAR_COLORS.length]);
                avatar.setEffect(new DropShadow(5, Color.web("#00000025")));

                String initial = user.length() > 0 ? user.substring(0, 1).toUpperCase() : "?";
                Label initialLabel = new Label(initial);
                initialLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");

                avatarPane.getChildren().addAll(avatar, initialLabel);

                // Username with dark text
                Label nameLabel = new Label("👤 " + user);
                nameLabel.setStyle("-fx-text-fill: #333; -fx-font-size: 13px; -fx-font-weight: bold;");
                HBox.setHgrow(nameLabel, Priority.ALWAYS);

                // Online indicator with bright glow
                Circle onlineIndicator = new Circle(6);
                onlineIndicator.setFill(Color.web("#4ade80"));
                onlineIndicator.setEffect(new DropShadow(8, Color.web("#4ade80")));

                userBox.getChildren().addAll(avatarPane, nameLabel, onlineIndicator);

                // Hover effect
                userBox.setOnMouseEntered(e -> userBox.setStyle(
                        "-fx-background-color: #f0f8ff; -fx-background-radius: 12; " +
                                "-fx-scale-x: 1.02; -fx-scale-y: 1.02; " +
                                "-fx-effect: dropshadow(gaussian, rgba(102,126,234,0.3), 12, 0, 0, 4);"));
                userBox.setOnMouseExited(e -> userBox.setStyle(
                        "-fx-background-color: white; -fx-background-radius: 12; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 6, 0, 0, 2);"));

                setGraphic(userBox);
                setText(null);
                setStyle("-fx-background-color: transparent; -fx-padding: 3 0 3 0;");
            }
        }
    }
}