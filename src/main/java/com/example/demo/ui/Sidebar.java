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
    private ListView<String> roomsListView; // Chat rooms list with unread badges
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
    private Label roomsCountLabel; // Rooms count label
    private TabPane sidebarTabs;
    private ListView<String> publicRoomsListView; // Public rooms list

    private Consumer<String> onRoomSelected;
    private Consumer<Void> onSettingsClicked;
    private Consumer<Void> onProfileClicked;
    private Consumer<Void> onAddRoomClicked;
    private Consumer<Void> onFriendsClicked;
    private Consumer<Void> onInvitesClicked;
    private Consumer<Void> onHistoryClicked;
    private Consumer<String> onUserClicked;
    private Consumer<com.example.demo.client.model.User> onFriendMessageClicked;
    private Consumer<Long> onJoinPublicRoom; // Callback for joining public room

    // Store friends for direct access
    private List<com.example.demo.client.model.User> loadedFriends;
    // Store rooms for direct access
    private List<com.example.demo.client.model.ChatRoom> loadedRooms;

    // Avatar colors
    private static final Color[] AVATAR_COLORS = {
            Color.web("#667eea"), Color.web("#764ba2"), Color.web("#f093fb"),
            Color.web("#f5576c"), Color.web("#4facfe"), Color.web("#43e97b"),
            Color.web("#fa709a"), Color.web("#30cfd0")
    };

    // Unread badge tracking
    private final Map<Long, Integer> unreadCounts = new HashMap<>(); // For DMs (friendId -> count)
    private final Map<Long, Integer> roomUnreadCounts = new HashMap<>(); // For Rooms (roomId -> count)
    private int friendRequestBadgeCount = 0;
    private int roomInviteBadgeCount = 0;
    private Label dmBadgeLabel;
    private Label friendsBadgeLabel; // Badge for friend requests on friends icon
    private Label roomInviteBadgeLabel; // Badge for room invites

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

        profileButton = createQuickButton("\uD83D\uDC64", "Profile", "#FF6B6B"); // 👤
        friendsButton = createQuickButton("\uD83D\uDC65", "Friends", "#4ECDC4"); // 👥
        Button roomManageButton = createQuickButton("\uD83C\uDFE0", "Phòng", "#45B7D1"); // 🏠
        settingsButton = createQuickButton("\u2699", "Settings", "#96CEB4"); // ⚙

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

        // Create StackPanes with badge overlays for Friends and Room buttons
        StackPane friendsPane = createButtonWithBadge(friendsButton, true);
        StackPane roomPane = createButtonWithBadge(roomManageButton, false);

        quickActions.getChildren().addAll(profileButton, friendsPane, roomPane, settingsButton);

        VBox navContainer = new VBox(10);
        navContainer.setPadding(new Insets(5, 15, 5, 15));
        navContainer.getChildren().add(quickActions);

        getChildren().add(navContainer);
    }

    /**
     * Create a button wrapped in StackPane with a badge overlay
     * 
     * @param button         The button to wrap
     * @param isFriendsBadge true for friends badge, false for room invites badge
     */
    private StackPane createButtonWithBadge(Button button, boolean isFriendsBadge) {
        StackPane pane = new StackPane();
        pane.setAlignment(Pos.TOP_RIGHT);

        // Create badge label
        Label badge = new Label("0");
        badge.setStyle(
                "-fx-background-color: #FF3B30; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 10px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 2 6 2 6; " +
                        "-fx-background-radius: 10; " +
                        "-fx-min-width: 18; " +
                        "-fx-min-height: 18; " +
                        "-fx-alignment: center;");
        badge.setVisible(false);
        badge.setTranslateX(5);
        badge.setTranslateY(-5);

        if (isFriendsBadge) {
            friendsBadgeLabel = badge;
        } else {
            roomInviteBadgeLabel = badge;
        }

        pane.getChildren().addAll(button, badge);
        return pane;
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

        // Rooms Tab (includes all public rooms automatically)
        Tab roomsTab = new Tab("\uD83C\uDFE0 Rooms");
        roomsTab.setContent(createRoomsTab());

        // Online Now Tab
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

        addRoomButton = Sidebar.createBeautifulButton("➕", "Tạo phòng mới", "#667eea");
        addRoomButton.setMaxWidth(Double.MAX_VALUE);
        addRoomButton.setOnAction(e -> {
            if (onAddRoomClicked != null)
                onAddRoomClicked.accept(null);
        });

        dmBox.getChildren().addAll(header, friendsListView, addRoomButton);
        return dmBox;
    }

    private VBox createRoomsTab() {
        VBox roomsBox = new VBox(10);
        roomsBox.setPadding(new Insets(12, 8, 8, 8));
        roomsBox.setStyle("-fx-background-color: rgba(0,0,0,0.1); -fx-background-radius: 12;");

        // Header with icon and count
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(8, 12, 8, 12));
        header.setStyle("-fx-background-color: linear-gradient(to right, #f093fb, #f5576c); " +
                "-fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);");

        Label iconLabel = new Label("🏠");
        iconLabel.setStyle("-fx-font-size: 18px;");

        Label roomLabel = new Label("Chat Rooms");
        roomLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        roomsCountLabel = new Label("✨ 0");
        roomsCountLabel.setStyle(
                "-fx-background-color: #FFD93D; -fx-text-fill: #333; " +
                        "-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 4 10 4 10; -fx-background-radius: 15;");

        header.getChildren().addAll(iconLabel, roomLabel, spacer, roomsCountLabel);

        // Rooms list with unread badges
        roomsListView = new ListView<>();
        roomsListView.setStyle(
                "-fx-background-color: rgba(255,255,255,0.95); " +
                        "-fx-background-radius: 12; -fx-border-radius: 12; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 3);");
        roomsListView.setCellFactory(param -> new RoomListCell());
        roomsListView.setPlaceholder(new Label("🏠 Chưa có phòng chat nào."));
        VBox.setVgrow(roomsListView, Priority.ALWAYS);

        // Keep the old ComboBox for compatibility (hidden)
        roomSelector = new ComboBox<>();
        roomSelector.setVisible(false);
        roomSelector.setManaged(false);

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

        roomsBox.getChildren().addAll(header, roomsListView, roomActions);
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

    private VBox createPublicRoomsTab() {
        VBox publicBox = new VBox(10);
        publicBox.setPadding(new Insets(12, 8, 8, 8));
        publicBox.setStyle("-fx-background-color: rgba(0,0,0,0.1); -fx-background-radius: 12;");

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(8, 12, 8, 12));
        header.setStyle("-fx-background-color: linear-gradient(to right, #11998e, #38ef7d); " +
                "-fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);");

        Label iconLabel = new Label("🌐");
        iconLabel.setStyle("-fx-font-size: 18px;");

        Label titleLabel = new Label("Phòng Công Khai");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        header.getChildren().addAll(iconLabel, titleLabel);

        // Public rooms list
        publicRoomsListView = new ListView<>();
        publicRoomsListView.setStyle(
                "-fx-background-color: rgba(255,255,255,0.95); " +
                        "-fx-background-radius: 12; -fx-border-radius: 12; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 3);");
        publicRoomsListView.setCellFactory(param -> new PublicRoomCell());
        publicRoomsListView.setPlaceholder(new Label("Không có phòng công khai nào"));
        VBox.setVgrow(publicRoomsListView, Priority.ALWAYS);

        publicBox.getChildren().addAll(header, publicRoomsListView);
        return publicBox;
    }

    /**
     * Load public rooms into the sidebar
     */
    public void loadPublicRooms(List<com.example.demo.client.model.ChatRoom> rooms) {
        if (publicRoomsListView == null)
            return;
        publicRoomsListView.getItems().clear();
        for (com.example.demo.client.model.ChatRoom room : rooms) {
            // Store as "roomId|roomName|memberCount" format
            String item = room.getId() + "|" + room.getName() + "|" + room.getMemberCount();
            publicRoomsListView.getItems().add(item);
        }
    }

    /**
     * Set callback for joining public room
     */
    public void setOnJoinPublicRoom(Consumer<Long> handler) {
        this.onJoinPublicRoom = handler;
    }

    /**
     * Add a public room dynamically (real-time update when room is created)
     * Now adds to main roomsListView instead of separate publicRooms
     */
    public void addPublicRoom(Long roomId, String roomName, int memberCount) {
        if (roomsListView == null)
            return;
        // Format: "roomId|roomName|isPrivate"
        String item = roomId + "|" + roomName + "|false";
        // Check if already exists
        boolean exists = roomsListView.getItems().stream()
                .anyMatch(i -> i.startsWith(roomId + "|"));
        if (!exists) {
            roomsListView.getItems().add(0, item); // Add to top
            // Update count
            int count = roomsListView.getItems().size();
            roomsCountLabel.setText(String.valueOf(count));
            System.out.println("🏠 Added public room to Rooms tab: " + roomName);
        }
    }

    /**
     * Remove a room dynamically (real-time update when room is deleted)
     */
    public void removePublicRoom(Long roomId) {
        if (roomsListView == null)
            return;
        roomsListView.getItems().removeIf(item -> item.startsWith(roomId + "|"));
        // Update count
        int count = roomsListView.getItems().size();
        roomsCountLabel.setText(String.valueOf(count));
        System.out.println("🗑️ Removed room from Rooms tab: " + roomId);
    }

    /**
     * Cell for public room list with Join button
     */
    private class PublicRoomCell extends ListCell<String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                // Parse item: "roomId|roomName|memberCount"
                String[] parts = item.split("\\|");
                Long roomId = Long.valueOf(parts[0]);
                String roomName = parts.length > 1 ? parts[1] : "Unknown";
                int memberCount = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

                HBox roomBox = new HBox(10);
                roomBox.setAlignment(Pos.CENTER_LEFT);
                roomBox.setPadding(new Insets(10, 12, 10, 12));
                roomBox.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

                // Room icon
                StackPane iconPane = new StackPane();
                Circle roomIcon = new Circle(18);
                int hash = Math.abs(roomName.hashCode());
                roomIcon.setFill(AVATAR_COLORS[hash % AVATAR_COLORS.length]);
                roomIcon.setEffect(new DropShadow(4, Color.web("#00000020")));
                Label iconEmoji = new Label("🌐");
                iconEmoji.setStyle("-fx-font-size: 12px;");
                iconPane.getChildren().addAll(roomIcon, iconEmoji);

                // Info
                VBox infoBox = new VBox(2);
                Label nameLabel = new Label(roomName);
                nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #212529;");
                Label memberLabel = new Label("👥 " + memberCount + " thành viên");
                memberLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #6c757d;");
                infoBox.getChildren().addAll(nameLabel, memberLabel);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                // Enter button (no need to join for public rooms)
                Button enterBtn = new Button("Vào");
                enterBtn.setTooltip(new Tooltip("Vào chat trực tiếp"));
                enterBtn.setStyle(
                        "-fx-background-color: #4ade80; -fx-text-fill: white; " +
                                "-fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 12; -fx-cursor: hand;");
                enterBtn.setOnMouseEntered(e -> enterBtn.setStyle(
                        "-fx-background-color: #22c55e; -fx-text-fill: white; " +
                                "-fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 12; -fx-cursor: hand;"));
                enterBtn.setOnMouseExited(e -> enterBtn.setStyle(
                        "-fx-background-color: #4ade80; -fx-text-fill: white; " +
                                "-fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 12; -fx-cursor: hand;"));
                enterBtn.setOnAction(e -> {
                    if (onJoinPublicRoom != null) {
                        onJoinPublicRoom.accept(roomId);
                    }
                });

                roomBox.getChildren().addAll(iconPane, infoBox, spacer, enterBtn);

                // Hover effect
                roomBox.setOnMouseEntered(e -> roomBox.setStyle(
                        "-fx-background-color: #f8f9fa; -fx-background-radius: 10;"));
                roomBox.setOnMouseExited(e -> roomBox.setStyle(
                        "-fx-background-color: white; -fx-background-radius: 10;"));

                setGraphic(roomBox);
                setText(null);
                setStyle("-fx-background-color: transparent; -fx-padding: 3 0;");
            }
        }
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
        this.loadedRooms = rooms;
        roomSelector.getItems().clear();
        roomsListView.getItems().clear();

        if (rooms != null && !rooms.isEmpty()) {
            for (com.example.demo.client.model.ChatRoom room : rooms) {
                String displayName = room.isPrivate() ? "🔒 " + room.getName() : "🌐 " + room.getName();
                roomSelector.getItems().add(displayName);
                // Store as "roomId|roomName|isPrivate" format for ListView
                String item = room.getId() + "|" + room.getName() + "|" + room.isPrivate();
                roomsListView.getItems().add(item);
            }
            roomSelector.setValue(roomSelector.getItems().get(0));
            roomsCountLabel.setText(String.valueOf(rooms.size()));
        } else {
            roomsCountLabel.setText("0");
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
     * Only shows users who have showOnlineStatus = true (privacy setting)
     */
    public void loadOnlineUsers(List<com.example.demo.client.model.User> users) {
        userListView.getItems().clear();
        System.out.println("🔍 loadOnlineUsers called with " + (users != null ? users.size() : 0) + " users");
        if (users != null && !users.isEmpty()) {
            int addedCount = 0;
            for (com.example.demo.client.model.User user : users) {
                System.out.println("🔍 Processing user: " + user);
                if (user != null) {
                    // Respect privacy: skip users who have disabled showOnlineStatus
                    Boolean showOnlineStatus = user.getShowOnlineStatus();
                    if (showOnlineStatus != null && !showOnlineStatus) {
                        System.out.println("🔒 User has showOnlineStatus=false, skipping: " + user.getDisplayName());
                        continue;
                    }

                    String displayName = user.getDisplayName() != null ? user.getDisplayName()
                            : (user.getUsername() != null ? user.getUsername() : "Unknown User");
                    System.out.println("🔍 Adding to list: " + displayName);
                    userListView.getItems().add(displayName);
                    addedCount++;
                } else {
                    System.out.println("⚠️ User object is null!");
                }
            }
            System.out.println("✅ Added " + addedCount + " users to list");
            onlineCountLabel.setText(String.valueOf(addedCount));
        } else {
            onlineCountLabel.setText("0");
        }
    }

    public void setCurrentUser(String displayName) {
        currentUserLabel.setText(displayName);
        // Note: Don't change avatar here - avatar is set separately via
        // setCurrentUserAvatar()
    }

    /**
     * Set current user's avatar from URL
     */
    public void setCurrentUserAvatar(String avatarUrl, String username) {
        if (userAvatar != null) {
            com.example.demo.util.AvatarUtils.setAvatarOnCircleAsync(userAvatar, avatarUrl, username, 35);
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

    /**
     * Update friend online/offline status in real-time
     * Called when receiving WebSocket user status updates
     */
    public void updateFriendStatus(Long userId, boolean isOnline) {
        System.out.println("🔄 updateFriendStatus called: userId=" + userId + ", isOnline=" + isOnline);

        // Update friend status in Direct Messages list
        if (loadedFriends != null) {
            for (com.example.demo.client.model.User friend : loadedFriends) {
                if (friend.getId().equals(userId)) {
                    friend.setStatus(isOnline ? com.example.demo.client.model.User.Status.ONLINE
                            : com.example.demo.client.model.User.Status.OFFLINE);
                    // Refresh the list to update the status indicator
                    friendsListView.refresh();
                    System.out.println("✅ Updated friend status in DM list: " + friend.getDisplayName());
                    break;
                }
            }
        }

        // Also update Online Users list
        updateOnlineUsersList(userId, isOnline);
    }

    /**
     * Update the Online Users list when a user comes online or goes offline
     */
    private void updateOnlineUsersList(Long userId, boolean isOnline) {
        // Find user display name from loadedFriends or other sources
        String userDisplayName = null;

        if (loadedFriends != null) {
            for (com.example.demo.client.model.User friend : loadedFriends) {
                if (friend.getId().equals(userId)) {
                    userDisplayName = friend.getDisplayName() != null ? friend.getDisplayName() : friend.getUsername();
                    break;
                }
            }
        }

        if (isOnline) {
            // User came online - add to Online Users list if not already present
            if (userDisplayName != null) {
                final String displayName = userDisplayName;
                boolean alreadyInList = userListView.getItems().stream()
                        .anyMatch(item -> item.equals(displayName));
                if (!alreadyInList) {
                    userListView.getItems().add(displayName);
                    System.out.println("➕ Added to Online Users: " + displayName);
                }
            }
        } else {
            // User went offline - remove from Online Users list
            if (userDisplayName != null) {
                final String displayName = userDisplayName;
                userListView.getItems().removeIf(item -> item.equals(displayName));
                System.out.println("➖ Removed from Online Users: " + displayName);
            }
        }

        // Update count
        onlineCountLabel.setText(String.valueOf(userListView.getItems().size()));
    }

    /**
     * Add a user to the Online Users list by username/displayName
     * Called when receiving a user online notification
     */
    public void addOnlineUser(String displayName, String username) {
        if (userListView == null) {
            System.out.println("⚠️ userListView is null, cannot add online user");
            return;
        }
        String nameToUse = displayName != null && !displayName.isEmpty() ? displayName : username;
        if (nameToUse != null && !nameToUse.isEmpty() && !userListView.getItems().contains(nameToUse)) {
            userListView.getItems().add(nameToUse);
            if (onlineCountLabel != null) {
                onlineCountLabel.setText(String.valueOf(userListView.getItems().size()));
            }
            System.out.println("➕ Added online user to Online Now tab: " + nameToUse);
        }
    }

    /**
     * Remove a user from the Online Users list by username/displayName
     * Called when receiving a user offline notification
     */
    public void removeOnlineUser(String displayName, String username) {
        if (userListView == null) {
            System.out.println("⚠️ userListView is null, cannot remove online user");
            return;
        }
        String nameToRemove = displayName != null && !displayName.isEmpty() ? displayName : username;
        if (nameToRemove != null && !nameToRemove.isEmpty()) {
            boolean removed = userListView.getItems().remove(nameToRemove);
            if (onlineCountLabel != null) {
                onlineCountLabel.setText(String.valueOf(userListView.getItems().size()));
            }
            System.out.println(
                    "➖ Removed online user from Online Now tab: " + nameToRemove + " (removed=" + removed + ")");
        }
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
     * Set specific friend request badge count (for initial load from backend)
     */
    public void setFriendRequestBadgeCount(int count) {
        friendRequestBadgeCount = count;
        updateFriendsBadge();
    }

    /**
     * Set specific room invite badge count (for initial load from backend)
     */
    public void setRoomInviteBadgeCount(int count) {
        roomInviteBadgeCount = count;
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

    /**
     * 📊 Set unread count for a specific friend
     * Called by ChatClientFXApp when loading from backend
     */
    public void setUnreadCount(Long friendId, int count) {
        if (count > 0) {
            unreadCounts.put(friendId, count);
        } else {
            unreadCounts.remove(friendId);
        }
        // Refresh the DM list to show/hide badges
        if (friendsListView != null) {
            friendsListView.refresh();
        }
        updateDMBadge();
    }

    /**
     * 📊 Refresh all unread badges (call after loading from backend)
     */
    public void refreshUnreadBadges() {
        if (friendsListView != null) {
            friendsListView.refresh();
        }
        updateDMBadge();
    }

    // ==================== ROOM UNREAD COUNT METHODS ====================

    /**
     * 📊 Set unread count for a specific room
     */
    public void setRoomUnreadCount(Long roomId, int count) {
        if (count > 0) {
            roomUnreadCounts.put(roomId, count);
        } else {
            roomUnreadCounts.remove(roomId);
        }
        refreshRoomsList();
    }

    /**
     * 📊 Increment unread count for a room
     */
    public void incrementRoomUnreadCount(Long roomId) {
        roomUnreadCounts.merge(roomId, 1, Integer::sum);
        refreshRoomsList();
    }

    /**
     * 📊 Clear unread count for a room (when entering the room)
     */
    public void clearRoomUnreadCount(Long roomId) {
        roomUnreadCounts.remove(roomId);
        refreshRoomsList();
    }

    /**
     * 📊 Get unread count for a room
     */
    public int getRoomUnreadCount(Long roomId) {
        return roomUnreadCounts.getOrDefault(roomId, 0);
    }

    /**
     * 📊 Get total unread count across all rooms
     */
    public int getTotalRoomUnreadCount() {
        return roomUnreadCounts.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * 📊 Refresh the rooms list view to show updated badges
     */
    public void refreshRoomsList() {
        if (roomsListView != null) {
            roomsListView.refresh();
        }
    }

    private void updateFriendsBadge() {
        // Update visual badge overlay on friends icon
        if (friendsBadgeLabel != null) {
            if (friendRequestBadgeCount > 0) {
                friendsBadgeLabel.setText(String.valueOf(friendRequestBadgeCount));
                friendsBadgeLabel.setVisible(true);
            } else {
                friendsBadgeLabel.setVisible(false);
            }
        }
    }

    private void updateInvitesBadge() {
        // Update visual badge overlay on room icon
        if (roomInviteBadgeLabel != null) {
            if (roomInviteBadgeCount > 0) {
                roomInviteBadgeLabel.setText(String.valueOf(roomInviteBadgeCount));
                roomInviteBadgeLabel.setVisible(true);
            } else {
                roomInviteBadgeLabel.setVisible(false);
            }
        }
        // Also update invites button in Rooms tab if exists
        if (invitesButton != null) {
            if (roomInviteBadgeCount > 0) {
                invitesButton.setText("📨 " + roomInviteBadgeCount);
            } else {
                invitesButton.setText("📨 Lời mời");
            }
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

                // Check online status from loadedFriends
                boolean isOnline = false;
                boolean hideOnlineStatus = false; // Privacy: friend has disabled showOnlineStatus
                if (loadedFriends != null) {
                    for (com.example.demo.client.model.User friend : loadedFriends) {
                        String displayName = friend.getDisplayName() != null ? friend.getDisplayName()
                                : friend.getUsername();
                        if (displayName.equals(friendName)) {
                            isOnline = friend.getStatus() == com.example.demo.client.model.User.Status.ONLINE;
                            // Check if friend wants to hide their online status
                            Boolean showOnlineStatus = friend.getShowOnlineStatus();
                            if (showOnlineStatus != null && !showOnlineStatus) {
                                hideOnlineStatus = true;
                            }
                            break;
                        }
                    }
                }

                // Online/Offline indicator based on actual status (only if not hidden)
                VBox statusBox = new VBox(2);
                statusBox.setAlignment(Pos.CENTER);
                Circle statusIndicator = new Circle(7);
                if (hideOnlineStatus) {
                    // Hidden status - don't show any indicator (invisible)
                    statusIndicator.setFill(Color.TRANSPARENT);
                    statusIndicator.setEffect(null);
                } else if (isOnline) {
                    // Online - green with glow
                    statusIndicator.setFill(Color.web("#4ade80"));
                    statusIndicator.setEffect(new DropShadow(10, Color.web("#4ade80")));
                } else {
                    // Offline - gray without glow
                    statusIndicator.setFill(Color.web("#9ca3af"));
                    statusIndicator.setEffect(new DropShadow(4, Color.web("#00000020")));
                }
                statusBox.getChildren().add(statusIndicator);

                // 🔴 Unread message badge
                Label unreadBadge = null;
                Long friendId = null;
                if (loadedFriends != null) {
                    for (com.example.demo.client.model.User friend : loadedFriends) {
                        String displayName = friend.getDisplayName() != null ? friend.getDisplayName()
                                : friend.getUsername();
                        if (displayName.equals(friendName)) {
                            friendId = friend.getId();
                            break;
                        }
                    }
                }

                if (friendId != null) {
                    int unreadCount = unreadCounts.getOrDefault(friendId, 0);
                    if (unreadCount > 0) {
                        String badgeText = unreadCount > 99 ? "99+" : String.valueOf(unreadCount);
                        unreadBadge = new Label(badgeText);
                        unreadBadge.setStyle(
                                "-fx-background-color: #ef4444; " +
                                        "-fx-text-fill: white; " +
                                        "-fx-font-size: 11px; " +
                                        "-fx-font-weight: bold; " +
                                        "-fx-padding: 2 6 2 6; " +
                                        "-fx-background-radius: 10; " +
                                        "-fx-min-width: 20; " +
                                        "-fx-alignment: center;");
                    }
                }

                friendBox.getChildren().addAll(avatarPane, infoBox, statusBox);
                if (unreadBadge != null) {
                    friendBox.getChildren().add(unreadBadge);
                }

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
     * Cell for Chat Rooms list with unread badges
     */
    private class RoomListCell extends ListCell<String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
            } else {
                // Parse item: "roomId|roomName|isPrivate"
                String[] parts = item.split("\\|");
                Long roomId = Long.valueOf(parts[0]);
                String roomName = parts.length > 1 ? parts[1] : "Unknown";
                boolean isPrivate = parts.length > 2 && Boolean.parseBoolean(parts[2]);

                HBox roomBox = new HBox(14);
                roomBox.setAlignment(Pos.CENTER_LEFT);
                roomBox.setPadding(new Insets(14, 16, 14, 16));
                roomBox.setStyle(
                        "-fx-background-color: white; -fx-background-radius: 14; -fx-cursor: hand; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8, 0, 0, 3);");

                // Room icon/avatar
                StackPane avatarPane = new StackPane();
                Circle avatar = new Circle(22);
                int hash = Math.abs(roomName.hashCode());
                avatar.setFill(AVATAR_COLORS[hash % AVATAR_COLORS.length]);
                avatar.setEffect(new DropShadow(6, Color.web("#00000030")));

                String icon = isPrivate ? "🔒" : "🌐";
                Label iconLabel = new Label(icon);
                iconLabel.setStyle("-fx-font-size: 14px;");

                avatarPane.getChildren().addAll(avatar, iconLabel);

                // Room info
                VBox infoBox = new VBox(4);
                Label nameLabel = new Label(icon + " " + roomName);
                nameLabel.setStyle("-fx-text-fill: #333; -fx-font-size: 14px; -fx-font-weight: bold;");

                String typeText = isPrivate ? "🔐 Phòng riêng tư" : "🌍 Phòng công khai";
                Label typeLabel = new Label(typeText);
                typeLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");

                infoBox.getChildren().addAll(nameLabel, typeLabel);
                HBox.setHgrow(infoBox, Priority.ALWAYS);

                // Unread message badge
                Label unreadBadge = null;
                int unreadCount = roomUnreadCounts.getOrDefault(roomId, 0);
                if (unreadCount > 0) {
                    String badgeText = unreadCount > 99 ? "99+" : String.valueOf(unreadCount);
                    unreadBadge = new Label(badgeText);
                    unreadBadge.setStyle(
                            "-fx-background-color: #ef4444; " +
                                    "-fx-text-fill: white; " +
                                    "-fx-font-size: 11px; " +
                                    "-fx-font-weight: bold; " +
                                    "-fx-padding: 2 6 2 6; " +
                                    "-fx-background-radius: 10; " +
                                    "-fx-min-width: 20; " +
                                    "-fx-alignment: center;");
                }

                roomBox.getChildren().addAll(avatarPane, infoBox);
                if (unreadBadge != null) {
                    roomBox.getChildren().add(unreadBadge);
                }

                // Hover effect
                roomBox.setOnMouseEntered(e -> roomBox.setStyle(
                        "-fx-background-color: #fff0f5; -fx-background-radius: 14; -fx-cursor: hand; " +
                                "-fx-scale-x: 1.02; -fx-scale-y: 1.02; " +
                                "-fx-effect: dropshadow(gaussian, rgba(240,147,251,0.35), 15, 0, 0, 5);"));
                roomBox.setOnMouseExited(e -> roomBox.setStyle(
                        "-fx-background-color: white; -fx-background-radius: 14; -fx-cursor: hand; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8, 0, 0, 3);"));

                // Click to select room
                roomBox.setOnMouseClicked(e -> {
                    if (onRoomSelected != null) {
                        String displayName = isPrivate ? "🔒 " + roomName : "🌐 " + roomName;
                        onRoomSelected.accept(displayName);
                    }
                });

                setGraphic(roomBox);
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

    /**
     * Tạo nút đẹp với hiệu ứng hover cho toàn bộ ứng dụng
     */
    public static Button createBeautifulButton(String icon, String text, String bgColor) {
        Button btn = new Button(icon + (text.isEmpty() ? "" : " " + text));
        btn.setStyle(
                "-fx-background-color: " + bgColor + "; " +
                        "-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; " +
                        "-fx-min-width: 120; -fx-min-height: 45; -fx-max-width: 200; -fx-max-height: 45; " +
                        "-fx-background-radius: 22; -fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 8, 0, 0, 3);");
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: derive(" + bgColor + ", 20%); " +
                        "-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; " +
                        "-fx-min-width: 120; -fx-min-height: 45; -fx-max-width: 200; -fx-max-height: 45; " +
                        "-fx-background-radius: 22; -fx-cursor: hand; " +
                        "-fx-scale-x: 1.05; -fx-scale-y: 1.05; " +
                        "-fx-effect: dropshadow(gaussian, " + bgColor + ", 15, 0.5, 0, 0);"));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: " + bgColor + "; " +
                        "-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; " +
                        "-fx-min-width: 120; -fx-min-height: 45; -fx-max-width: 200; -fx-max-height: 45; " +
                        "-fx-background-radius: 22; -fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 8, 0, 0, 3);"));
        return btn;
    }

    /**
     * Apply dark mode styling to sidebar components
     */
    public void applyDarkMode(boolean isDark) {
        if (isDark) {
            // Dark mode - tối màu nền sidebar
            setStyle("-fx-background-color: linear-gradient(180deg, #1e293b 0%, #0f172a 100%); " +
                    "-fx-padding: 20 15 20 15; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 2, 0);");

            // Update list views
            if (friendsListView != null) {
                friendsListView.setStyle(
                        "-fx-background-color: rgba(30,41,59,0.95); " +
                                "-fx-background-radius: 12; -fx-border-radius: 12; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 8, 0, 0, 3);");
            }
            if (userListView != null) {
                userListView.setStyle(
                        "-fx-background-color: rgba(30,41,59,0.95); " +
                                "-fx-background-radius: 12; -fx-border-radius: 12; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 8, 0, 0, 3);");
            }
            if (roomSelector != null) {
                roomSelector.setStyle(
                        "-fx-background-color: #1e293b; -fx-font-size: 13px; -fx-font-weight: bold; " +
                                "-fx-background-radius: 10; -fx-border-radius: 10; " +
                                "-fx-text-fill: #e2e8f0; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);");
            }
        } else {
            // Light mode - màu sáng gradient tím
            setStyle("-fx-background-color: linear-gradient(180deg, #667eea 0%, #764ba2 100%); " +
                    "-fx-padding: 20 15 20 15; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 2, 0);");

            // Update list views
            if (friendsListView != null) {
                friendsListView.setStyle(
                        "-fx-background-color: rgba(255,255,255,0.95); " +
                                "-fx-background-radius: 12; -fx-border-radius: 12; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 3);");
            }
            if (userListView != null) {
                userListView.setStyle(
                        "-fx-background-color: rgba(255,255,255,0.95); " +
                                "-fx-background-radius: 12; -fx-border-radius: 12; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 3);");
            }
            if (roomSelector != null) {
                roomSelector.setStyle(
                        "-fx-background-color: white; -fx-font-size: 13px; -fx-font-weight: bold; " +
                                "-fx-background-radius: 10; -fx-border-radius: 10; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 5, 0, 0, 2);");
            }
        }

        // Refresh list views to apply new cell styles
        if (friendsListView != null) {
            friendsListView.refresh();
        }
        if (userListView != null) {
            userListView.refresh();
        }
    }
}