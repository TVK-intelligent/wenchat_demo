package com.example.demo.ui;

import com.example.demo.client.model.ChatRoom;
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
import java.util.function.Consumer;

/**
 * Room Management Dialog - Create, Join, Delete, and Manage Rooms
 */
@Slf4j
public class RoomManagementDialog extends Stage {

    private final ChatService chatService;

    // UI Components
    private ListView<ChatRoom> myRoomsList;
    private ListView<ChatRoom> publicRoomsList;
    private ListView<User> availableFriendsList;
    private ListView<java.util.Map<String, Object>> pendingInvitesList;
    private TextField roomNameField;
    private TextArea roomDescriptionField;
    private CheckBox privateRoomCheckBox;
    private Button createRoomButton;
    private Button joinRoomButton;
    private Button deleteRoomButton;
    private Button leaveRoomButton;
    private Button inviteButton;
    private ComboBox<ChatRoom> inviteRoomSelector;
    private Label myRoomCountLabel;

    // Members management
    private ListView<java.util.Map<String, Object>> membersList;
    private ComboBox<ChatRoom> membersRoomSelector;

    // Avatar colors
    private static final Color[] AVATAR_COLORS = {
            Color.web("#667eea"), Color.web("#764ba2"), Color.web("#f093fb"),
            Color.web("#f5576c"), Color.web("#4facfe"), Color.web("#43e97b"),
            Color.web("#fa709a"), Color.web("#30cfd0")
    };

    // Callback for room selection
    private Consumer<ChatRoom> onRoomSelected;

    // Callback for badge refresh when accept/decline invite
    private Runnable onBadgeUpdate;

    private Button createBeautifulButton(String icon, String text, String bgColor) {
        return Sidebar.createBeautifulButton(icon, text, bgColor);
    }

    public RoomManagementDialog(ChatService chatService) {
        this.chatService = chatService;

        initModality(Modality.APPLICATION_MODAL);
        setTitle("🏠 Quản lý Phòng Chat");
        setResizable(true);
        setWidth(800);
        setHeight(700);

        initComponents();
        setupEventHandlers();

        Scene scene = new Scene(createLayout());
        if (getClass().getResource("/styles.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        }
        setScene(scene);

        // Load data AFTER layout is created (pendingInvitesList is initialized in
        // createLayout)
        loadData();
    }

    public void setOnRoomSelected(Consumer<ChatRoom> callback) {
        this.onRoomSelected = callback;
    }

    public void setOnBadgeUpdate(Runnable callback) {
        this.onBadgeUpdate = callback;
    }

    private void initComponents() {
        // Room lists
        myRoomsList = new ListView<>();
        myRoomsList.setPrefHeight(250);
        myRoomsList.setCellFactory(param -> new MyRoomListCell());

        publicRoomsList = new ListView<>();
        publicRoomsList.setPrefHeight(250);
        publicRoomsList.setCellFactory(param -> new PublicRoomListCell());

        // Create room form
        roomNameField = new TextField();
        roomNameField.setPromptText("🏠 Nhập tên phòng...");
        roomNameField.setStyle(
                "-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; " +
                        "-fx-border-radius: 25; -fx-background-radius: 25; -fx-padding: 10 20;");

        roomDescriptionField = new TextArea();
        roomDescriptionField.setPromptText("📝 Mô tả phòng (tùy chọn)...");
        roomDescriptionField.setPrefRowCount(3);
        roomDescriptionField.setStyle(
                "-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; " +
                        "-fx-border-radius: 10; -fx-background-radius: 10;");

        privateRoomCheckBox = new CheckBox("🔒 Phòng riêng tư");
        privateRoomCheckBox.setStyle("-fx-font-size: 13px;");

        createRoomButton = createBeautifulButton("➕", "Tạo Phòng", "#4ade80");

        // Join room
        joinRoomButton = createBeautifulButton("🚪", "Tham Gia Phòng", "#667eea");
        joinRoomButton.setDisable(true);

        // Delete room
        deleteRoomButton = createBeautifulButton("🗑️", "Xóa Phòng", "#f87171");
        deleteRoomButton.setDisable(true);

        // Leave room
        leaveRoomButton = createBeautifulButton("🚪", "Rời Phòng", "#fb923c");
        leaveRoomButton.setDisable(true);

        // Invite friends
        availableFriendsList = new ListView<>();
        availableFriendsList.setPrefHeight(180);
        availableFriendsList.setCellFactory(param -> new UserListCell());

        inviteRoomSelector = new ComboBox<>();
        inviteRoomSelector.setPromptText("📌 Chọn phòng để mời...");
        inviteRoomSelector.setStyle("-fx-background-radius: 20;");
        inviteRoomSelector.setPrefWidth(300);

        inviteButton = createBeautifulButton("📨", "Gửi Lời Mời", "#8b5cf6");
        inviteButton.setDisable(true);
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

        Label iconEmoji = new Label("🏠");
        iconEmoji.setStyle("-fx-font-size: 20px;");
        StackPane iconPane = new StackPane(headerIcon, iconEmoji);

        VBox titleBox = new VBox(2);
        Label titleLabel = new Label("Quản lý Phòng Chat");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + textColor + ";");
        Label subtitleLabel = new Label("Tạo, tham gia và quản lý các phòng chat");
        subtitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + mutedColor + ";");
        titleBox.getChildren().addAll(titleLabel, subtitleLabel);

        headerBox.getChildren().addAll(iconPane, titleBox);

        // Tab pane with custom styling
        TabPane tabPane = new TabPane();
        tabPane.setStyle("-fx-background-color: transparent;");

        // My Rooms Tab
        Tab myRoomsTab = new Tab("🏠 Phòng của tôi", createMyRoomsTab());
        myRoomsTab.setClosable(false);

        // Public Rooms Tab
        Tab publicRoomsTab = new Tab("🌐 Phòng công khai", createPublicRoomsTab());
        publicRoomsTab.setClosable(false);

        // Create Room Tab
        Tab createRoomTab = new Tab("➕ Tạo phòng", createRoomTab());
        createRoomTab.setClosable(false);

        // Invites Tab (send invites)
        Tab invitesTab = new Tab("📨 Mời bạn bè", createInvitesTab());
        invitesTab.setClosable(false);

        // Pending Invites Tab (received invites)
        Tab pendingInvitesTab = new Tab("📥 Lời mời nhận được", createPendingInvitesTab());
        pendingInvitesTab.setClosable(false);

        // Members Management Tab
        Tab membersTab = new Tab("👥 Thành viên", createMembersTab());
        membersTab.setClosable(false);

        // Banned Members Tab
        Tab bannedTab = new Tab("⛔ Bị cấm", createBannedMembersTab());
        bannedTab.setClosable(false);

        tabPane.getTabs().addAll(myRoomsTab, publicRoomsTab, createRoomTab, invitesTab, pendingInvitesTab, membersTab,
                bannedTab);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        // Bottom buttons
        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button refreshButton = createBeautifulButton("🔄", "Làm mới", "#4ade80");
        refreshButton.setOnAction(e -> refreshData());

        Button closeButton = createBeautifulButton("", "Đóng", "#6c757d");
        closeButton.setOnAction(e -> close());

        buttonBox.getChildren().addAll(refreshButton, closeButton);

        mainLayout.getChildren().addAll(headerBox, tabPane, buttonBox);

        return mainLayout;
    }

    private VBox createMyRoomsTab() {
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
        myRoomCountLabel = new Label("0");
        myRoomCountLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #667eea;");
        Label statLabel = new Label("Phòng");
        statLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6c757d;");
        statBox.getChildren().addAll(myRoomCountLabel, statLabel);

        statsCard.getChildren().add(statBox);

        Label label = new Label("Danh sách phòng của bạn:");
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #495057;");

        VBox.setVgrow(myRoomsList, Priority.ALWAYS);
        myRoomsList.setStyle(
                "-fx-background-color: #f8f9fa; -fx-background-radius: 12; " +
                        "-fx-border-radius: 12; -fx-border-color: #e9ecef;");

        // Action buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        Button selectButton = createBeautifulButton("✔️", "Chọn Phòng", "#667eea");
        selectButton.setDisable(true);
        selectButton.setOnAction(e -> {
            ChatRoom selected = myRoomsList.getSelectionModel().getSelectedItem();
            if (selected != null && onRoomSelected != null) {
                onRoomSelected.accept(selected);
                close();
            }
        });

        myRoomsList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectButton.setDisable(newVal == null);
            deleteRoomButton.setDisable(newVal == null);
            leaveRoomButton.setDisable(newVal == null);
        });

        buttonBox.getChildren().addAll(selectButton, deleteRoomButton, leaveRoomButton);
        tabContent.getChildren().addAll(statsCard, label, myRoomsList, buttonBox);

        return tabContent;
    }

    private VBox createPublicRoomsTab() {
        VBox tabContent = new VBox(15);
        tabContent.setPadding(new Insets(15));

        Label label = new Label("Các phòng công khai có thể tham gia:");
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #495057;");

        VBox.setVgrow(publicRoomsList, Priority.ALWAYS);
        publicRoomsList.setStyle(
                "-fx-background-color: #f8f9fa; -fx-background-radius: 12; " +
                        "-fx-border-radius: 12; -fx-border-color: #e9ecef;");
        publicRoomsList.setPlaceholder(new Label("Không có phòng công khai nào"));

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.getChildren().add(joinRoomButton);

        tabContent.getChildren().addAll(label, publicRoomsList, buttonBox);

        return tabContent;
    }

    private VBox createRoomTab() {
        // Check dark mode state
        boolean isDark = SettingsDialog.isDarkTheme();
        String textColor = isDark ? "#e2e8f0" : "#495057";
        String infoBg = isDark ? "#1e3a5f" : "#e7f5ff";
        String infoBorder = isDark ? "#3b82f6" : "#74c0fc";
        String infoText = isDark ? "#93c5fd" : "#1971c2";

        VBox tabContent = new VBox(15);
        tabContent.setPadding(new Insets(15));

        // Info card
        HBox infoCard = new HBox(15);
        infoCard.setPadding(new Insets(12));
        infoCard.setStyle(
                "-fx-background-color: " + infoBg + "; -fx-background-radius: 10; " +
                        "-fx-border-color: " + infoBorder + "; -fx-border-radius: 10;");
        Label infoIcon = new Label("💡");
        infoIcon.setStyle("-fx-font-size: 16px;");
        Label infoTextLabel = new Label(
                "Tạo phòng mới để chat với bạn bè. Phòng riêng tư chỉ có thể tham gia qua lời mời.");
        infoTextLabel.setStyle("-fx-text-fill: " + infoText + "; -fx-font-size: 12px;");
        infoTextLabel.setWrapText(true);
        infoCard.getChildren().addAll(infoIcon, infoTextLabel);
        HBox.setHgrow(infoTextLabel, Priority.ALWAYS);

        Label nameLabel = new Label("Tên phòng:");
        nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + textColor + ";");

        Label descLabel = new Label("Mô tả:");
        descLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + textColor + ";");

        // Update checkbox style for dark mode
        privateRoomCheckBox.setStyle("-fx-font-size: 13px; -fx-text-fill: " + textColor + ";");

        tabContent.getChildren().addAll(
                infoCard,
                nameLabel, roomNameField,
                descLabel, roomDescriptionField,
                privateRoomCheckBox, createRoomButton);

        return tabContent;
    }

    private VBox createInvitesTab() {
        VBox tabContent = new VBox(15);
        tabContent.setPadding(new Insets(15));

        Label roomLabel = new Label("Chọn phòng để mời bạn bè:");
        roomLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #495057;");

        Label friendsLabel = new Label("Bạn bè có thể mời:");
        friendsLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #495057;");

        VBox.setVgrow(availableFriendsList, Priority.ALWAYS);
        availableFriendsList.setStyle(
                "-fx-background-color: #f8f9fa; -fx-background-radius: 12; " +
                        "-fx-border-radius: 12; -fx-border-color: #e9ecef;");
        availableFriendsList.setPlaceholder(new Label("Chọn một phòng để xem bạn bè có thể mời"));

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.getChildren().add(inviteButton);

        tabContent.getChildren().addAll(
                roomLabel, inviteRoomSelector,
                friendsLabel, availableFriendsList,
                buttonBox);

        return tabContent;
    }

    private VBox createPendingInvitesTab() {
        VBox tabContent = new VBox(15);
        tabContent.setPadding(new Insets(15));

        Label label = new Label("Lời mời vào phòng đang chờ xử lý:");
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #495057;");

        pendingInvitesList = new ListView<>();
        pendingInvitesList.setPrefHeight(300);
        pendingInvitesList.setCellFactory(param -> new PendingInviteCell());
        VBox.setVgrow(pendingInvitesList, Priority.ALWAYS);
        pendingInvitesList.setStyle(
                "-fx-background-color: #f8f9fa; -fx-background-radius: 12; " +
                        "-fx-border-radius: 12; -fx-border-color: #e9ecef;");
        pendingInvitesList.setPlaceholder(new Label("Không có lời mời nào"));

        tabContent.getChildren().addAll(label, pendingInvitesList);
        return tabContent;
    }

    private VBox createMembersTab() {
        boolean isDark = SettingsDialog.isDarkTheme();
        String textColor = isDark ? "#e2e8f0" : "#495057";
        String bgColor = isDark ? "#374151" : "#f8f9fa";

        VBox tabContent = new VBox(15);
        tabContent.setPadding(new Insets(15));

        // Room selector
        Label selectLabel = new Label("Chọn phòng để quản lý thành viên:");
        selectLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + textColor + ";");

        membersRoomSelector = new ComboBox<>();
        membersRoomSelector.setPromptText("Chọn phòng...");
        membersRoomSelector.setPrefWidth(300);
        membersRoomSelector.setCellFactory(param -> new ListCell<ChatRoom>() {
            @Override
            protected void updateItem(ChatRoom room, boolean empty) {
                super.updateItem(room, empty);
                if (empty || room == null) {
                    setText(null);
                } else {
                    setText(room.getName() + (room.isPrivate() ? " 🔒" : " 🌐"));
                }
            }
        });
        membersRoomSelector.setButtonCell(new ListCell<ChatRoom>() {
            @Override
            protected void updateItem(ChatRoom room, boolean empty) {
                super.updateItem(room, empty);
                if (empty || room == null) {
                    setText("Chọn phòng...");
                } else {
                    setText(room.getName() + (room.isPrivate() ? " 🔒" : " 🌐"));
                }
            }
        });
        membersRoomSelector.setOnAction(e -> loadMembersForRoom());

        // Instructions
        Label infoLabel = new Label("💡 Click chuột phải vào thành viên để thực hiện các thao tác admin");
        infoLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (isDark ? "#9ca3af" : "#6c757d") + ";");

        // Members list
        membersList = new ListView<>();
        membersList.setPrefHeight(300);
        membersList.setCellFactory(param -> new MemberListCell());
        VBox.setVgrow(membersList, Priority.ALWAYS);
        membersList.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 12; " +
                "-fx-border-radius: 12; -fx-border-color: " + (isDark ? "#4b5563" : "#e9ecef") + ";");
        membersList.setPlaceholder(new Label("Chọn phòng để xem thành viên"));

        // Refresh button
        Button refreshMembersBtn = createBeautifulButton("🔄", "Làm mới", "#4ade80");
        refreshMembersBtn.setOnAction(e -> loadMembersForRoom());

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.getChildren().add(refreshMembersBtn);

        tabContent.getChildren().addAll(selectLabel, membersRoomSelector, infoLabel, membersList, buttonBox);
        return tabContent;
    }

    private void loadMembersForRoom() {
        ChatRoom selectedRoom = membersRoomSelector.getValue();
        if (selectedRoom == null) {
            membersList.getItems().clear();
            return;
        }

        List<java.util.Map<String, Object>> members = chatService.getRoomMembersWithRoles(selectedRoom.getId());
        membersList.getItems().clear();
        membersList.getItems().addAll(members);
        log.info("👥 Loaded {} members for room {}", members.size(), selectedRoom.getName());
    }

    // ==================== BANNED MEMBERS TAB ====================

    private ComboBox<ChatRoom> bannedRoomSelector;
    private ListView<java.util.Map<String, Object>> bannedMembersList;

    private VBox createBannedMembersTab() {
        boolean isDark = SettingsDialog.isDarkTheme();
        String textColor = isDark ? "#e2e8f0" : "#495057";
        String bgColor = isDark ? "#374151" : "#f8f9fa";

        VBox tabContent = new VBox(15);
        tabContent.setPadding(new Insets(15));

        // Room selector
        Label selectLabel = new Label("Chọn phòng để xem danh sách bị cấm:");
        selectLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + textColor + ";");

        bannedRoomSelector = new ComboBox<>();
        bannedRoomSelector.setPromptText("Chọn phòng...");
        bannedRoomSelector.setPrefWidth(300);
        bannedRoomSelector.setCellFactory(param -> new ListCell<ChatRoom>() {
            @Override
            protected void updateItem(ChatRoom room, boolean empty) {
                super.updateItem(room, empty);
                if (empty || room == null) {
                    setText(null);
                } else {
                    setText(room.getName() + (room.isPrivate() ? " 🔒" : " 🌐"));
                }
            }
        });
        bannedRoomSelector.setButtonCell(new ListCell<ChatRoom>() {
            @Override
            protected void updateItem(ChatRoom room, boolean empty) {
                super.updateItem(room, empty);
                if (empty || room == null) {
                    setText("Chọn phòng...");
                } else {
                    setText(room.getName() + (room.isPrivate() ? " 🔒" : " 🌐"));
                }
            }
        });
        bannedRoomSelector.setOnAction(e -> loadBannedMembers());

        // Instructions
        Label infoLabel = new Label("⛔ Click vào thành viên bị cấm và bấm nút Gỡ cấm để cho phép họ tham gia lại");
        infoLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (isDark ? "#9ca3af" : "#6c757d") + ";");

        // Banned members list
        bannedMembersList = new ListView<>();
        bannedMembersList.setPrefHeight(250);
        bannedMembersList.setCellFactory(param -> new BannedMemberCell());
        VBox.setVgrow(bannedMembersList, Priority.ALWAYS);
        bannedMembersList.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 12; " +
                "-fx-border-radius: 12; -fx-border-color: " + (isDark ? "#4b5563" : "#e9ecef") + ";");
        bannedMembersList.setPlaceholder(new Label("Chọn phòng để xem danh sách bị cấm"));

        // Unban button
        Button unbanButton = createBeautifulButton("✅", "Gỡ cấm", "#4ade80");
        unbanButton.setDisable(true);
        unbanButton.setOnAction(e -> {
            var selected = bannedMembersList.getSelectionModel().getSelectedItem();
            ChatRoom room = bannedRoomSelector.getValue();
            if (selected != null && room != null) {
                Long bannedUserId = ((Number) selected.get("userId")).longValue();
                String username = (String) selected.get("username");
                if (chatService.unbanMember(room.getId(), bannedUserId)) {
                    showInfo("Gỡ cấm thành công", username + " đã được gỡ cấm và có thể tham gia phòng lại");
                    loadBannedMembers();
                } else {
                    showError("Lỗi", "Không thể gỡ cấm thành viên");
                }
            }
        });

        bannedMembersList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            unbanButton.setDisable(newVal == null);
        });

        // Refresh button
        Button refreshBtn = createBeautifulButton("🔄", "Làm mới", "#60a5fa");
        refreshBtn.setOnAction(e -> loadBannedMembers());

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.getChildren().addAll(unbanButton, refreshBtn);

        tabContent.getChildren().addAll(selectLabel, bannedRoomSelector, infoLabel, bannedMembersList, buttonBox);
        return tabContent;
    }

    private void loadBannedMembers() {
        ChatRoom selectedRoom = bannedRoomSelector.getValue();
        if (selectedRoom == null) {
            bannedMembersList.getItems().clear();
            return;
        }

        List<java.util.Map<String, Object>> bannedMembers = chatService.getBannedMembers(selectedRoom.getId());
        bannedMembersList.getItems().clear();
        bannedMembersList.getItems().addAll(bannedMembers);
        log.info("⛔ Loaded {} banned members for room {}", bannedMembers.size(), selectedRoom.getName());
    }

    /**
     * Custom cell for banned members list
     */
    private class BannedMemberCell extends ListCell<java.util.Map<String, Object>> {
        @Override
        protected void updateItem(java.util.Map<String, Object> banData, boolean empty) {
            super.updateItem(banData, empty);
            if (empty || banData == null) {
                setGraphic(null);
                return;
            }

            boolean isDark = SettingsDialog.isDarkTheme();

            String username = (String) banData.get("username");
            String displayName = banData.get("displayName") != null ? (String) banData.get("displayName") : username;
            String reason = banData.get("reason") != null ? (String) banData.get("reason") : "Không có lý do";
            String bannedAt = banData.get("bannedAt") != null ? banData.get("bannedAt").toString() : "";

            HBox container = new HBox(12);
            container.setAlignment(Pos.CENTER_LEFT);
            container.setPadding(new Insets(10, 15, 10, 15));
            container.setStyle("-fx-background-color: " + (isDark ? "#374151" : "#ffffff") + "; " +
                    "-fx-background-radius: 10; -fx-border-color: " + (isDark ? "#4b5563" : "#e9ecef") + "; " +
                    "-fx-border-radius: 10;");

            // Avatar
            Circle avatar = new Circle(20);
            int colorIndex = Math.abs(username.hashCode()) % AVATAR_COLORS.length;
            avatar.setFill(AVATAR_COLORS[colorIndex]);
            String initial = displayName.length() > 0 ? displayName.substring(0, 1).toUpperCase() : "?";
            Label initialLabel = new Label(initial);
            initialLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
            StackPane avatarPane = new StackPane(avatar, initialLabel);
            avatarPane.setMinSize(40, 40);
            avatarPane.setMaxSize(40, 40);

            // User info
            VBox infoBox = new VBox(2);
            Label nameLabel = new Label("⛔ " + displayName);
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: " +
                    (isDark ? "#f1f5f9" : "#212529") + ";");
            Label usernameLabel = new Label("@" + username);
            usernameLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (isDark ? "#9ca3af" : "#6c757d") + ";");
            Label reasonLabel = new Label("Lý do: " + reason);
            reasonLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #ef4444;");
            infoBox.getChildren().addAll(nameLabel, usernameLabel, reasonLabel);
            HBox.setHgrow(infoBox, Priority.ALWAYS);

            // Banned badge
            Label badge = new Label("BỊ CẤM");
            badge.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; " +
                    "-fx-padding: 4 10; -fx-background-radius: 12; -fx-font-size: 10px; -fx-font-weight: bold;");

            container.getChildren().addAll(avatarPane, infoBox, badge);
            setGraphic(container);
        }
    }

    /**
     * Custom cell for members list with role display and admin actions
     */
    private class MemberListCell extends ListCell<java.util.Map<String, Object>> {
        @Override
        protected void updateItem(java.util.Map<String, Object> memberData, boolean empty) {
            super.updateItem(memberData, empty);
            if (empty || memberData == null) {
                setGraphic(null);
                return;
            }

            boolean isDark = SettingsDialog.isDarkTheme();

            // Extract user and role info
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> userMap = (java.util.Map<String, Object>) memberData.get("user");
            String role = String.valueOf(memberData.get("role"));

            String username = userMap != null ? String.valueOf(userMap.get("username")) : "Unknown";
            String displayName = userMap != null && userMap.get("displayName") != null
                    ? String.valueOf(userMap.get("displayName"))
                    : username;
            Long userId = userMap != null && userMap.get("id") != null
                    ? ((Number) userMap.get("id")).longValue()
                    : null;

            HBox container = new HBox(12);
            container.setAlignment(Pos.CENTER_LEFT);
            container.setPadding(new Insets(10, 15, 10, 15));
            container.setStyle("-fx-background-color: " + (isDark ? "#374151" : "#ffffff") + "; " +
                    "-fx-background-radius: 10; -fx-border-color: " + (isDark ? "#4b5563" : "#e9ecef") + "; " +
                    "-fx-border-radius: 10;");

            // Avatar
            Circle avatar = new Circle(20);
            int colorIndex = Math.abs(username.hashCode()) % AVATAR_COLORS.length;
            avatar.setFill(AVATAR_COLORS[colorIndex]);
            String initial = displayName.length() > 0 ? displayName.substring(0, 1).toUpperCase() : "?";
            Label initialLabel = new Label(initial);
            initialLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
            StackPane avatarPane = new StackPane(avatar, initialLabel);
            avatarPane.setMinSize(40, 40);
            avatarPane.setMaxSize(40, 40);

            // User info
            VBox infoBox = new VBox(2);
            Label nameLabel = new Label(displayName);
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: " +
                    (isDark ? "#f1f5f9" : "#212529") + ";");
            Label usernameLabel = new Label("@" + username);
            usernameLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (isDark ? "#9ca3af" : "#6c757d") + ";");
            infoBox.getChildren().addAll(nameLabel, usernameLabel);
            HBox.setHgrow(infoBox, Priority.ALWAYS);

            // Role badge
            Label roleBadge = new Label();
            switch (role) {
                case "OWNER":
                    roleBadge.setText("👑 Chủ phòng");
                    roleBadge.setStyle("-fx-background-color: #fbbf24; -fx-text-fill: #1f2937; " +
                            "-fx-padding: 4 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
                    break;
                case "ADMIN":
                    roleBadge.setText("⭐ Phó phòng");
                    roleBadge.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; " +
                            "-fx-padding: 4 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
                    break;
                default:
                    roleBadge.setText("👤 Thành viên");
                    roleBadge.setStyle("-fx-background-color: " + (isDark ? "#4b5563" : "#e5e7eb") + "; " +
                            "-fx-text-fill: " + (isDark ? "#e5e7eb" : "#4b5563") + "; " +
                            "-fx-padding: 4 10; -fx-background-radius: 12; -fx-font-size: 11px;");
            }

            container.getChildren().addAll(avatarPane, infoBox, roleBadge);

            // Context menu for admin actions
            ChatRoom selectedRoom = membersRoomSelector.getValue();
            if (selectedRoom != null && userId != null) {
                ContextMenu contextMenu = new ContextMenu();
                contextMenu.setStyle("-fx-background-color: white; -fx-padding: 8; -fx-background-radius: 12; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 12, 0, 0, 4);");

                Long ownerId = selectedRoom.getOwnerId();
                Long currentUserId = chatService.getCurrentUserId();

                // Debug logging
                System.out.println("🔍 DEBUG: selectedRoom=" + selectedRoom.getName() +
                        ", ownerId=" + ownerId +
                        ", currentUserId=" + currentUserId +
                        ", targetUserId=" + userId +
                        ", role=" + role);

                // Only owner can promote/demote
                if (ownerId != null && ownerId.equals(currentUserId) && !role.equals("OWNER")) {
                    if (role.equals("MEMBER")) {
                        // Create beautiful promote button
                        HBox promoteBox = RoomManagementDialog.this.createStyledMenuItem("⭐ Phong làm Phó phòng",
                                "#8b5cf6", false);
                        CustomMenuItem promoteItem = new CustomMenuItem(promoteBox);
                        promoteItem.setHideOnClick(true);
                        promoteItem.setOnAction(e -> {
                            if (chatService.promoteToAdmin(selectedRoom.getId(), userId)) {
                                showInfo("Thành công", displayName + " đã được phong làm Phó phòng");
                                loadMembersForRoom();
                            } else {
                                showError("Lỗi", "Không thể phong Phó phòng");
                            }
                        });
                        contextMenu.getItems().add(promoteItem);
                    } else if (role.equals("ADMIN")) {
                        // Create beautiful demote button
                        HBox demoteBox = RoomManagementDialog.this.createStyledMenuItem("⬇️ Hạ cấp về Thành viên",
                                "#6b7280", false);
                        CustomMenuItem demoteItem = new CustomMenuItem(demoteBox);
                        demoteItem.setHideOnClick(true);
                        demoteItem.setOnAction(e -> {
                            if (chatService.demoteFromAdmin(selectedRoom.getId(), userId)) {
                                showInfo("Thành công", displayName + " đã bị hạ cấp về Thành viên");
                                loadMembersForRoom();
                            } else {
                                showError("Lỗi", "Không thể hạ cấp Admin");
                            }
                        });
                        contextMenu.getItems().add(demoteItem);
                    }
                }

                // Owner and Admin can kick (except owner can't be kicked)
                boolean canKick = (ownerId != null && ownerId.equals(currentUserId)) ||
                        (chatService.isRoomAdmin(selectedRoom.getId()));
                if (canKick && !role.equals("OWNER") && !userId.equals(currentUserId)) {
                    // Admin can only kick Member, not other Admin
                    boolean isCurrentUserOwner = ownerId != null && ownerId.equals(currentUserId);
                    if (isCurrentUserOwner || role.equals("MEMBER")) {
                        // Create beautiful kick button
                        HBox kickBox = RoomManagementDialog.this.createStyledMenuItem("🚫 Đuổi khỏi phòng", "#ef4444",
                                true);
                        CustomMenuItem kickItem = new CustomMenuItem(kickBox);
                        kickItem.setHideOnClick(true);
                        kickItem.setOnAction(e -> {
                            if (chatService.kickMember(selectedRoom.getId(), userId)) {
                                showInfo("Thành công", displayName + " đã bị đuổi khỏi phòng");
                                loadMembersForRoom();
                            } else {
                                showError("Lỗi", "Không thể đuổi thành viên");
                            }
                        });
                        contextMenu.getItems().add(kickItem);

                        // Create beautiful ban button (permanent ban)
                        HBox banBox = RoomManagementDialog.this.createStyledMenuItem("⛔ Cấm vĩnh viễn", "#991b1b",
                                true);
                        CustomMenuItem banItem = new CustomMenuItem(banBox);
                        banItem.setHideOnClick(true);
                        banItem.setOnAction(e -> {
                            // Show confirmation dialog with reason input
                            javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
                            dialog.setTitle("Cấm thành viên");
                            dialog.setHeaderText("Cấm " + displayName + " khỏi phòng vĩnh viễn");
                            dialog.setContentText("Lý do (tùy chọn):");
                            dialog.showAndWait().ifPresent(reason -> {
                                if (chatService.banMember(selectedRoom.getId(), userId, reason)) {
                                    showInfo("Đã cấm", displayName + " đã bị cấm khỏi phòng vĩnh viễn");
                                    loadMembersForRoom();
                                } else {
                                    showError("Lỗi", "Không thể cấm thành viên");
                                }
                            });
                        });
                        contextMenu.getItems().add(banItem);
                    }
                }

                if (!contextMenu.getItems().isEmpty()) {
                    container.setOnContextMenuRequested(
                            e -> contextMenu.show(container, e.getScreenX(), e.getScreenY()));
                }
            }

            setGraphic(container);
        }
    }

    private void loadData() {
        // Load my rooms - only rooms where I am the OWNER
        Long currentUserId = chatService.getCurrentUserId();
        List<ChatRoom> myRooms = chatService.getMyRooms().stream()
                .filter(room -> room.getName() == null || !room.getName().startsWith("PRIVATE_"))
                .filter(room -> room.getOwnerId() != null && room.getOwnerId().equals(currentUserId))
                .collect(java.util.stream.Collectors.toList());
        myRoomsList.getItems().clear();
        myRoomsList.getItems().addAll(myRooms);
        if (myRoomCountLabel != null) {
            myRoomCountLabel.setText(String.valueOf(myRooms.size()));
        }

        // Load public rooms
        List<ChatRoom> publicRooms = chatService.getPublicRooms();
        publicRoomsList.getItems().clear();
        publicRoomsList.getItems().addAll(publicRooms);

        // Load rooms for invite selector (only private rooms - public rooms don't need
        // invites)
        List<ChatRoom> privateRooms = myRooms.stream()
                .filter(ChatRoom::isPrivate)
                .collect(java.util.stream.Collectors.toList());
        inviteRoomSelector.getItems().clear();
        inviteRoomSelector.getItems().addAll(privateRooms);

        if (!privateRooms.isEmpty()) {
            inviteRoomSelector.setValue(privateRooms.get(0));
            loadAvailableFriends(privateRooms.get(0).getId());
        }

        // Load pending room invites
        if (pendingInvitesList != null) {
            java.util.List<java.util.Map<String, Object>> pendingInvites = chatService.getPendingRoomInvites();
            pendingInvitesList.getItems().clear();
            pendingInvitesList.getItems().addAll(pendingInvites);
        }

        // Load rooms for members management selector
        if (membersRoomSelector != null) {
            membersRoomSelector.getItems().clear();
            membersRoomSelector.getItems().addAll(myRooms);
        }

        // Load rooms for banned members management selector
        if (bannedRoomSelector != null) {
            bannedRoomSelector.getItems().clear();
            bannedRoomSelector.getItems().addAll(myRooms);
        }
    }

    private void refreshData() {
        loadData();
        showInfo("Đã làm mới", "Danh sách phòng đã được cập nhật!");
    }

    private void loadAvailableFriends(Long roomId) {
        List<User> friends = chatService.getAvailableFriendsForInvite(roomId);
        availableFriendsList.getItems().clear();
        availableFriendsList.getItems().addAll(friends);
    }

    private void setupEventHandlers() {
        // Join room button
        publicRoomsList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            joinRoomButton.setDisable(newVal == null);
        });

        joinRoomButton.setOnAction(e -> {
            ChatRoom selected = publicRoomsList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                boolean success = chatService.joinRoom(selected.getId());
                if (success) {
                    showInfo("Thành công", "Đã tham gia phòng: " + selected.getName());
                    refreshData();
                } else {
                    showError("Lỗi", "Không thể tham gia phòng này");
                }
            }
        });

        // Delete room button
        deleteRoomButton.setOnAction(e -> {
            ChatRoom selected = myRoomsList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                // Confirm deletion
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Xác nhận xóa");
                confirm.setHeaderText("Xóa phòng: " + selected.getName());
                confirm.setContentText("Bạn có chắc muốn xóa phòng này? Tất cả tin nhắn sẽ bị mất.");
                confirm.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        boolean success = chatService.deleteRoom(selected.getId());
                        if (success) {
                            showInfo("Thành công", "Đã xóa phòng: " + selected.getName());
                            refreshData();
                        } else {
                            showError("Lỗi", "Không thể xóa phòng. Có thể bạn không phải là chủ phòng.");
                        }
                    }
                });
            }
        });

        // Leave room button
        leaveRoomButton.setOnAction(e -> {
            ChatRoom selected = myRoomsList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Xác nhận rời phòng");
                confirm.setHeaderText("Rời phòng: " + selected.getName());
                confirm.setContentText("Bạn có chắc muốn rời khỏi phòng này?");
                confirm.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        boolean success = chatService.leaveRoom(selected.getId());
                        if (success) {
                            showInfo("Đã rời phòng", "Bạn đã rời khỏi phòng: " + selected.getName());
                            refreshData();
                        } else {
                            showError("Lỗi", "Không thể rời phòng");
                        }
                    }
                });
            }
        });

        // Create room button
        createRoomButton.setOnAction(e -> {
            String name = roomNameField.getText().trim();
            if (name.isEmpty()) {
                showError("Lỗi", "Vui lòng nhập tên phòng");
                return;
            }

            ChatRoom newRoom = chatService.createRoom(name, roomDescriptionField.getText(),
                    privateRoomCheckBox.isSelected());
            if (newRoom != null) {
                // Refresh data
                refreshData();

                // Clear form
                roomNameField.clear();
                roomDescriptionField.clear();
                privateRoomCheckBox.setSelected(false);

                showInfo("Thành công", "Đã tạo phòng: " + name);
            } else {
                showError("Lỗi", "Không thể tạo phòng");
            }
        });

        // Invite room selector
        inviteRoomSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadAvailableFriends(newVal.getId());
            }
        });

        // Invite friends
        availableFriendsList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            inviteButton.setDisable(newVal == null || inviteRoomSelector.getValue() == null);
        });

        inviteButton.setOnAction(e -> {
            User selectedFriend = availableFriendsList.getSelectionModel().getSelectedItem();
            ChatRoom selectedRoom = inviteRoomSelector.getValue();

            if (selectedFriend != null && selectedRoom != null) {
                boolean success = chatService.inviteUserToRoom(selectedRoom.getId(), selectedFriend.getId());
                if (success) {
                    String friendName = selectedFriend.getDisplayName() != null
                            ? selectedFriend.getDisplayName()
                            : selectedFriend.getUsername();
                    showInfo("Thành công", "Đã gửi lời mời đến " + friendName);
                    // Refresh available friends
                    loadAvailableFriends(selectedRoom.getId());
                } else {
                    showError("Lỗi", "Không thể gửi lời mời");
                }
            }
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

    /**
     * Create a styled menu item with background color for context menus
     */
    private HBox createStyledMenuItem(String text, String color, boolean isDanger) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(10, 16, 10, 16));
        box.setMinWidth(180);

        String bgColor = isDanger ? "#fef2f2" : "#f5f3ff";
        String hoverBgColor = isDanger ? "#fee2e2" : "#ede9fe";
        String textColor = color;

        box.setStyle(
                "-fx-background-color: " + bgColor + "; " +
                        "-fx-background-radius: 8; " +
                        "-fx-cursor: hand;");

        Label label = new Label(text);
        label.setStyle(
                "-fx-font-size: 13px; " +
                        "-fx-font-weight: 600; " +
                        "-fx-text-fill: " + textColor + ";");

        box.getChildren().add(label);

        // Hover effect
        box.setOnMouseEntered(e -> box.setStyle(
                "-fx-background-color: " + hoverBgColor + "; " +
                        "-fx-background-radius: 8; " +
                        "-fx-cursor: hand;"));
        box.setOnMouseExited(e -> box.setStyle(
                "-fx-background-color: " + bgColor + "; " +
                        "-fx-background-radius: 8; " +
                        "-fx-cursor: hand;"));

        return box;
    }

    // Custom cell for my rooms list with action buttons
    private class MyRoomListCell extends ListCell<ChatRoom> {
        @Override
        protected void updateItem(ChatRoom room, boolean empty) {
            super.updateItem(room, empty);
            if (empty || room == null) {
                setText(null);
                setGraphic(null);
            } else {
                HBox roomBox = new HBox(12);
                roomBox.setAlignment(Pos.CENTER_LEFT);
                roomBox.setPadding(new Insets(12, 15, 12, 15));
                roomBox.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

                // Room icon
                StackPane iconPane = new StackPane();
                Circle roomIcon = new Circle(22);
                int hash = Math.abs((room.getName() != null ? room.getName() : "").hashCode());
                roomIcon.setFill(AVATAR_COLORS[hash % AVATAR_COLORS.length]);
                roomIcon.setEffect(new DropShadow(4, Color.web("#00000020")));

                Label iconEmoji = new Label(room.isPrivate() ? "🔒" : "🌐");
                iconEmoji.setStyle("-fx-font-size: 16px;");
                iconPane.getChildren().addAll(roomIcon, iconEmoji);

                // Info
                VBox infoBox = new VBox(3);
                Label nameLabel = new Label(room.getName());
                nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #212529;");

                HBox detailsBox = new HBox(10);
                detailsBox.setAlignment(Pos.CENTER_LEFT);
                Label memberLabel = new Label("👥 " + room.getMemberCount() + " thành viên");
                memberLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d;");
                Label typeLabel = new Label(room.isPrivate() ? "Riêng tư" : "Công khai");
                typeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d;");
                detailsBox.getChildren().addAll(memberLabel, typeLabel);

                infoBox.getChildren().addAll(nameLabel, detailsBox);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                roomBox.getChildren().addAll(iconPane, infoBox, spacer);

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

    // Custom cell for public rooms list
    private class PublicRoomListCell extends ListCell<ChatRoom> {
        @Override
        protected void updateItem(ChatRoom room, boolean empty) {
            super.updateItem(room, empty);
            if (empty || room == null) {
                setText(null);
                setGraphic(null);
            } else {
                HBox roomBox = new HBox(12);
                roomBox.setAlignment(Pos.CENTER_LEFT);
                roomBox.setPadding(new Insets(10, 12, 10, 12));
                roomBox.setStyle("-fx-background-color: white; -fx-background-radius: 8;");

                // Room icon
                StackPane iconPane = new StackPane();
                Circle roomIcon = new Circle(18);
                int hash = Math.abs((room.getName() != null ? room.getName() : "").hashCode());
                roomIcon.setFill(AVATAR_COLORS[hash % AVATAR_COLORS.length]);

                Label iconEmoji = new Label("🌐");
                iconEmoji.setStyle("-fx-font-size: 12px;");
                iconPane.getChildren().addAll(roomIcon, iconEmoji);

                // Info
                VBox infoBox = new VBox(2);
                Label nameLabel = new Label(room.getName());
                nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

                Label descLabel = new Label(room.getDescription() != null ? room.getDescription() : "");
                descLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d;");

                infoBox.getChildren().addAll(nameLabel, descLabel);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Label memberCount = new Label("👥 " + room.getMemberCount());
                memberCount.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");

                roomBox.getChildren().addAll(iconPane, infoBox, spacer, memberCount);

                // Hover
                roomBox.setOnMouseEntered(e -> roomBox.setStyle(
                        "-fx-background-color: #f0f2f5; -fx-background-radius: 8;"));
                roomBox.setOnMouseExited(e -> roomBox.setStyle(
                        "-fx-background-color: white; -fx-background-radius: 8;"));

                setGraphic(roomBox);
                setText(null);
                setStyle("-fx-background-color: transparent; -fx-padding: 2 0;");
            }
        }
    }

    // Custom cell for user list
    private class UserListCell extends ListCell<User> {
        @Override
        protected void updateItem(User user, boolean empty) {
            super.updateItem(user, empty);
            if (empty || user == null) {
                setText(null);
                setGraphic(null);
            } else {
                HBox userBox = new HBox(10);
                userBox.setAlignment(Pos.CENTER_LEFT);
                userBox.setPadding(new Insets(8, 12, 8, 12));
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
                nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

                Label usernameLabel = new Label("@" + user.getUsername());
                usernameLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 10px;");

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

    // Custom cell for pending room invites
    private class PendingInviteCell extends ListCell<java.util.Map<String, Object>> {
        @Override
        protected void updateItem(java.util.Map<String, Object> invite, boolean empty) {
            super.updateItem(invite, empty);
            if (empty || invite == null) {
                setText(null);
                setGraphic(null);
            } else {
                HBox inviteBox = new HBox(12);
                inviteBox.setAlignment(Pos.CENTER_LEFT);
                inviteBox.setPadding(new Insets(12, 15, 12, 15));
                inviteBox.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

                // Room icon
                StackPane iconPane = new StackPane();
                Circle roomIcon = new Circle(22);
                String roomNameRaw = (String) invite.get("roomName");
                final String roomName = (roomNameRaw != null) ? roomNameRaw : "Phòng";
                int hash = Math.abs(roomName.hashCode());
                roomIcon.setFill(AVATAR_COLORS[hash % AVATAR_COLORS.length]);
                roomIcon.setEffect(new DropShadow(4, Color.web("#00000020")));

                Label iconEmoji = new Label("🏠");
                iconEmoji.setStyle("-fx-font-size: 16px;");
                iconPane.getChildren().addAll(roomIcon, iconEmoji);

                // Extract inviter info from nested object
                String inviterName = "Ai đó";
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> inviter = (java.util.Map<String, Object>) invite.get("inviter");
                if (inviter != null) {
                    String displayName = (String) inviter.get("displayName");
                    String username = (String) inviter.get("username");
                    inviterName = displayName != null && !displayName.isEmpty() ? displayName : username;
                }

                // Info
                VBox infoBox = new VBox(3);
                Label nameLabel = new Label(roomName);
                nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #212529;");

                Label inviterLabel = new Label("👤 Mời bởi: " + inviterName);
                inviterLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d;");

                infoBox.getChildren().addAll(nameLabel, inviterLabel);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                // Action buttons
                Button acceptButton = new Button("✓ Chấp nhận");
                acceptButton.setStyle(
                        "-fx-background-color: #4ade80; -fx-text-fill: white; " +
                                "-fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 12; -fx-cursor: hand;");

                Button declineButton = new Button("✕");
                declineButton.setStyle(
                        "-fx-background-color: #f87171; -fx-text-fill: white; " +
                                "-fx-font-size: 11px; -fx-padding: 6 10; -fx-background-radius: 12; -fx-cursor: hand;");

                // Get invite ID
                Long inviteId = Long.valueOf(invite.get("id").toString());

                acceptButton.setOnAction(e -> {
                    boolean success = chatService.acceptRoomInvite(inviteId);
                    if (success) {
                        showInfo("Thành công", "Đã tham gia phòng: " + roomName);
                        loadData(); // Refresh data
                        System.out.println("🔄 Room invite accepted, calling badge update callback...");
                        if (onBadgeUpdate != null) {
                            onBadgeUpdate.run();
                            System.out.println("✅ Badge update callback executed");
                        } else {
                            System.out.println("⚠️ onBadgeUpdate is null!");
                        }
                    } else {
                        showError("Lỗi", "Không thể chấp nhận lời mời");
                    }
                });

                declineButton.setOnAction(e -> {
                    boolean success = chatService.declineRoomInvite(inviteId);
                    if (success) {
                        showInfo("Đã từ chối", "Đã từ chối lời mời vào phòng");
                        loadData(); // Refresh data
                        if (onBadgeUpdate != null)
                            onBadgeUpdate.run();
                    } else {
                        showError("Lỗi", "Không thể từ chối lời mời");
                    }
                });

                inviteBox.getChildren().addAll(iconPane, infoBox, spacer, acceptButton, declineButton);

                // Hover effect
                inviteBox.setOnMouseEntered(e -> inviteBox.setStyle(
                        "-fx-background-color: #f8f9fa; -fx-background-radius: 10;"));
                inviteBox.setOnMouseExited(e -> inviteBox.setStyle(
                        "-fx-background-color: white; -fx-background-radius: 10;"));

                setGraphic(inviteBox);
                setText(null);
                setStyle("-fx-background-color: transparent; -fx-padding: 3 0;");
            }
        }
    }
}