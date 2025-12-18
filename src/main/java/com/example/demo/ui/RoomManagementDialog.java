package com.example.demo.ui;

import com.example.demo.client.model.ChatRoom;
import com.example.demo.client.model.User;
import com.example.demo.client.service.ChatService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.shape.Circle;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.Consumer;

/**
 * Room Management Dialog - Create, Join, and Manage Rooms
 */
@Slf4j
public class RoomManagementDialog extends Stage {

    private final ChatService chatService;

    // UI Components
    private ListView<ChatRoom> myRoomsList;
    private ListView<ChatRoom> publicRoomsList;
    private ListView<User> availableFriendsList;
    private TextField roomNameField;
    private TextArea roomDescriptionField;
    private CheckBox privateRoomCheckBox;
    private Button createRoomButton;
    private Button joinRoomButton;
    private Button inviteButton;
    private ComboBox<ChatRoom> inviteRoomSelector;

    public RoomManagementDialog(ChatService chatService) {
        this.chatService = chatService;

        initModality(Modality.APPLICATION_MODAL);
        setTitle("🏠 Quản lý Phòng Chat");
        setResizable(true);
        setWidth(900);
        setHeight(700);

        initComponents();
        loadData();
        setupEventHandlers();

        Scene scene = new Scene(createLayout());
        scene.getStylesheets().add("data:text/css," +
                ".room-list { -fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-radius: 8; }" +
                ".room-item { -fx-padding: 10; -fx-border-color: #e9ecef; -fx-border-width: 0 0 1 0; }" +
                ".room-item:hover { -fx-background-color: #e3f2fd; }" +
                ".room-item:selected { -fx-background-color: #007bff; -fx-text-fill: white; }");

        setScene(scene);
    }

    private void initComponents() {
        // Room lists
        myRoomsList = new ListView<>();
        myRoomsList.setPrefHeight(200);
        myRoomsList.setCellFactory(param -> new RoomListCell());

        publicRoomsList = new ListView<>();
        publicRoomsList.setPrefHeight(200);
        publicRoomsList.setCellFactory(param -> new RoomListCell());

        // Create room form
        roomNameField = new TextField();
        roomNameField.setPromptText("Tên phòng...");

        roomDescriptionField = new TextArea();
        roomDescriptionField.setPromptText("Mô tả phòng (tùy chọn)...");
        roomDescriptionField.setPrefRowCount(3);

        privateRoomCheckBox = new CheckBox("Phòng riêng tư");

        createRoomButton = new Button("➕ Tạo Phòng");
        createRoomButton.getStyleClass().add("create-button");

        // Join room
        joinRoomButton = new Button("🚪 Tham Gia");
        joinRoomButton.getStyleClass().add("join-button");
        joinRoomButton.setDisable(true);

        // Invite friends
        availableFriendsList = new ListView<>();
        availableFriendsList.setPrefHeight(150);
        availableFriendsList.setCellFactory(param -> new UserListCell());

        inviteRoomSelector = new ComboBox<>();
        inviteRoomSelector.setPromptText("Chọn phòng...");

        inviteButton = new Button("📨 Mời");
        inviteButton.getStyleClass().add("invite-button");
        inviteButton.setDisable(true);
    }

    private VBox createLayout() {
        VBox mainLayout = new VBox(20);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setStyle("-fx-background-color: #ffffff;");

        // Title
        Label titleLabel = new Label("🏠 Quản lý Phòng Chat");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #495057;");
        mainLayout.getChildren().add(titleLabel);

        // Tab pane for different sections
        TabPane tabPane = new TabPane();

        // My Rooms Tab
        Tab myRoomsTab = new Tab("Phòng của tôi", createMyRoomsTab());
        myRoomsTab.setClosable(false);

        // Public Rooms Tab
        Tab publicRoomsTab = new Tab("Phòng công khai", createPublicRoomsTab());
        publicRoomsTab.setClosable(false);

        // Create Room Tab
        Tab createRoomTab = new Tab("Tạo phòng mới", createRoomTab());
        createRoomTab.setClosable(false);

        // Invites Tab
        Tab invitesTab = new Tab("Mời bạn bè", createInvitesTab());
        invitesTab.setClosable(false);

        tabPane.getTabs().addAll(myRoomsTab, publicRoomsTab, createRoomTab, invitesTab);
        mainLayout.getChildren().add(tabPane);

        // Bottom buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button closeButton = new Button("Đóng");
        closeButton.setOnAction(e -> close());

        buttonBox.getChildren().add(closeButton);
        mainLayout.getChildren().add(buttonBox);

        return mainLayout;
    }

    private VBox createMyRoomsTab() {
        VBox tabContent = new VBox(10);
        tabContent.setPadding(new Insets(10));

        Label label = new Label("Phòng của bạn:");
        label.setStyle("-fx-font-weight: bold;");

        tabContent.getChildren().addAll(label, myRoomsList);

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        Button selectButton = new Button("Chọn Phòng");
        selectButton.setOnAction(e -> {
            ChatRoom selected = myRoomsList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                // TODO: Handle room selection
                close();
            }
        });

        buttonBox.getChildren().add(selectButton);
        tabContent.getChildren().add(buttonBox);

        return tabContent;
    }

    private VBox createPublicRoomsTab() {
        VBox tabContent = new VBox(10);
        tabContent.setPadding(new Insets(10));

        Label label = new Label("Phòng công khai:");
        label.setStyle("-fx-font-weight: bold;");

        tabContent.getChildren().addAll(label, publicRoomsList, joinRoomButton);

        return tabContent;
    }

    private VBox createRoomTab() {
        VBox tabContent = new VBox(15);
        tabContent.setPadding(new Insets(10));

        Label nameLabel = new Label("Tên phòng:");
        Label descLabel = new Label("Mô tả:");

        tabContent.getChildren().addAll(
                nameLabel, roomNameField,
                descLabel, roomDescriptionField,
                privateRoomCheckBox, createRoomButton);

        return tabContent;
    }

    private VBox createInvitesTab() {
        VBox tabContent = new VBox(10);
        tabContent.setPadding(new Insets(10));

        Label roomLabel = new Label("Chọn phòng:");
        Label friendsLabel = new Label("Bạn bè có thể mời:");

        tabContent.getChildren().addAll(
                roomLabel, inviteRoomSelector,
                friendsLabel, availableFriendsList,
                inviteButton);

        return tabContent;
    }

    private void loadData() {
        // Load my rooms
        List<ChatRoom> myRooms = chatService.getMyRooms();
        myRoomsList.getItems().addAll(myRooms);

        // Load public rooms
        List<ChatRoom> publicRooms = chatService.getPublicRooms();
        publicRoomsList.getItems().addAll(publicRooms);

        // Load available friends for invites
        List<ChatRoom> myRoomsForInvite = chatService.getMyRooms();
        inviteRoomSelector.getItems().addAll(myRoomsForInvite);

        if (!myRoomsForInvite.isEmpty()) {
            inviteRoomSelector.setValue(myRoomsForInvite.get(0));
            loadAvailableFriends(myRoomsForInvite.get(0).getId());
        }
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
                    // TODO: Handle room joined
                    close();
                } else {
                    showError("Lỗi", "Không thể tham gia phòng này");
                }
            }
        });

        // Create room button
        createRoomButton.setOnAction(e -> {
            String name = roomNameField.getText().trim();
            if (name.isEmpty()) {
                showError("Lỗi", "Vui lòng nhập tên phòng");
                return;
            }

            ChatRoom newRoom = chatService.createRoom(name, roomDescriptionField.getText());
            if (newRoom != null) {
                // Refresh my rooms list
                myRoomsList.getItems().clear();
                myRoomsList.getItems().addAll(chatService.getMyRooms());

                // Clear form
                roomNameField.clear();
                roomDescriptionField.clear();
                privateRoomCheckBox.setSelected(false);

                showInfo("Thành công", "Phòng đã được tạo!");
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
                    showInfo("Thành công", "Đã gửi lời mời!");
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

    // Custom cell for room list
    private static class RoomListCell extends ListCell<ChatRoom> {
        @Override
        protected void updateItem(ChatRoom room, boolean empty) {
            super.updateItem(room, empty);
            if (empty || room == null) {
                setText(null);
                setGraphic(null);
            } else {
                VBox roomBox = new VBox(5);
                roomBox.setPadding(new Insets(10));

                HBox header = new HBox(10);
                header.setAlignment(Pos.CENTER_LEFT);

                Label nameLabel = new Label(room.getName());
                nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

                Label memberCount = new Label("(" + room.getMemberCount() + ")");
                memberCount.setStyle("-fx-text-fill: #6c757d;");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Label privacyIcon = new Label(room.isPrivate() ? "🔒" : "🌐");
                privacyIcon.setStyle("-fx-font-size: 12px;");

                header.getChildren().addAll(nameLabel, memberCount, spacer, privacyIcon);

                Label descLabel = new Label(room.getDescription() != null ? room.getDescription() : "");
                descLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12px;");

                roomBox.getChildren().addAll(header, descLabel);

                setGraphic(roomBox);
                setText(null);
                getStyleClass().add("room-item");
            }
        }
    }

    // Custom cell for user list
    private static class UserListCell extends ListCell<User> {
        @Override
        protected void updateItem(User user, boolean empty) {
            super.updateItem(user, empty);
            if (empty || user == null) {
                setText(null);
                setGraphic(null);
            } else {
                HBox userBox = new HBox(10);
                userBox.setAlignment(Pos.CENTER_LEFT);
                userBox.setPadding(new Insets(8));

                Circle avatar = new Circle(16);
                avatar.setFill(javafx.scene.paint.Color.color(Math.random(), Math.random(), Math.random()));

                Label nameLabel = new Label(user.getDisplayName() != null ? user.getDisplayName() : user.getUsername());
                nameLabel.setStyle("-fx-font-weight: bold;");

                userBox.getChildren().addAll(avatar, nameLabel);
                HBox.setHgrow(nameLabel, Priority.ALWAYS);

                setGraphic(userBox);
                setText(null);
            }
        }
    }
}