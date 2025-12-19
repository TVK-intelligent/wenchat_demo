package com.example.demo.ui;

import com.example.demo.client.model.User;
import com.example.demo.client.service.ChatService;
import com.example.demo.util.AvatarUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

/**
 * Profile Dialog - View and edit user profiles
 */
@Slf4j
public class ProfileDialog extends Stage {

    private final ChatService chatService;
    private final User profileUser;
    private final boolean isOwnProfile;

    // UI Components
    private Circle avatarCircle;
    private Label usernameLabel;
    private Label displayNameLabel;
    private Label statusLabel;
    private Label joinDateLabel;
    private Button addFriendButton;
    private Button messageButton;
    private Button editButton;

    public ProfileDialog(ChatService chatService, User profileUser) {
        this.chatService = chatService;
        this.profileUser = profileUser;
        this.isOwnProfile = chatService.getCurrentUser() != null &&
                chatService.getCurrentUser().getId().equals(profileUser.getId());

        initModality(Modality.APPLICATION_MODAL);
        setTitle("👤 " + (isOwnProfile ? "Profile của bạn" : "Profile"));
        setResizable(false);
        setWidth(400);
        setHeight(500);

        initComponents();
        loadProfileData();

        Scene scene = new Scene(createLayout());
        scene.getStylesheets().add("data:text/css," +
                ".profile-card { -fx-background-color: #f8f9fa; -fx-padding: 20; -fx-border-radius: 12; -fx-border-color: #dee2e6; }"
                +
                ".profile-avatar { -fx-stroke: #007bff; -fx-stroke-width: 3; }" +
                ".profile-info { -fx-background-color: white; -fx-padding: 15; -fx-border-radius: 8; -fx-border-color: #e9ecef; }"
                +
                ".profile-button { -fx-background-color: #007bff; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-border-radius: 6; -fx-cursor: hand; }"
                +
                ".profile-button:hover { -fx-background-color: #0056b3; }" +
                ".add-friend-button { -fx-background-color: #28a745; }" +
                ".add-friend-button:hover { -fx-background-color: #1e7e34; }" +
                ".edit-button { -fx-background-color: #ffc107; -fx-text-fill: #212529; }" +
                ".edit-button:hover { -fx-background-color: #e0a800; }");

        setScene(scene);
    }

    private void initComponents() {
        avatarCircle = new Circle(50);
        avatarCircle.getStyleClass().add("profile-avatar");

        usernameLabel = new Label();
        usernameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #495057;");

        displayNameLabel = new Label();
        displayNameLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #6c757d;");

        statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size: 12px;");

        joinDateLabel = new Label();
        joinDateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6c757d;");

        addFriendButton = Sidebar.createBeautifulButton("➕", "Kết bạn", "#4ade80");
        addFriendButton.setVisible(!isOwnProfile);

        messageButton = Sidebar.createBeautifulButton("💬", "Nhắn tin", "#667eea");
        messageButton.setVisible(!isOwnProfile);

        editButton = Sidebar.createBeautifulButton("✏️", "Chỉnh sửa", "#fb923c");
        editButton.setVisible(isOwnProfile);
    }

    private VBox createLayout() {
        VBox mainLayout = new VBox(20);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setStyle("-fx-background-color: #ffffff;");

        // Profile card
        VBox profileCard = new VBox(20);
        profileCard.getStyleClass().add("profile-card");
        profileCard.setAlignment(Pos.CENTER);

        // Avatar - load from URL or use fallback color
        if (profileUser != null) {
            log.info("📷 ProfileDialog loading avatar - username: {}, avatarUrl: {}",
                    profileUser.getUsername(), profileUser.getAvatarUrl());
            AvatarUtils.setAvatarOnCircleAsync(avatarCircle, profileUser.getAvatarUrl(), profileUser.getUsername(), 50);
        }

        // User info
        VBox infoBox = new VBox(15);
        infoBox.getStyleClass().add("profile-info");
        infoBox.setAlignment(Pos.CENTER_LEFT);

        // Name section
        VBox nameSection = new VBox(5);
        nameSection.setAlignment(Pos.CENTER);
        nameSection.getChildren().addAll(usernameLabel, displayNameLabel);

        // Status and join date
        HBox statusBox = new HBox(10);
        statusBox.setAlignment(Pos.CENTER);
        statusBox.getChildren().addAll(statusLabel, joinDateLabel);

        infoBox.getChildren().addAll(nameSection, statusBox);

        profileCard.getChildren().addAll(avatarCircle, infoBox);

        // Action buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        if (!isOwnProfile) {
            buttonBox.getChildren().addAll(addFriendButton, messageButton);
        } else {
            buttonBox.getChildren().add(editButton);
        }

        // Bottom buttons
        HBox bottomBox = new HBox();
        bottomBox.setAlignment(Pos.CENTER_RIGHT);

        Button closeButton = Sidebar.createBeautifulButton("", "Đóng", "#6c757d");
        closeButton.setOnAction(e -> close());

        bottomBox.getChildren().add(closeButton);

        mainLayout.getChildren().addAll(profileCard, buttonBox, bottomBox);

        return mainLayout;
    }

    private void loadProfileData() {
        if (profileUser != null) {
            usernameLabel.setText("@" + profileUser.getUsername());

            String displayName = profileUser.getDisplayName();
            if (displayName != null && !displayName.trim().isEmpty()) {
                displayNameLabel.setText(displayName);
                displayNameLabel.setVisible(true);
            } else {
                displayNameLabel.setVisible(false);
            }

            // Status
            if (profileUser.getStatus() != null) {
                switch (profileUser.getStatus()) {
                    case ONLINE:
                        statusLabel.setText("🟢 Online");
                        statusLabel.setStyle("-fx-text-fill: #28a745;");
                        break;
                    case OFFLINE:
                        statusLabel.setText("⚫ Offline");
                        statusLabel.setStyle("-fx-text-fill: #6c757d;");
                        break;
                    default:
                        statusLabel.setText("⚪ Unknown");
                        statusLabel.setStyle("-fx-text-fill: #6c757d;");
                }
            }

            // Join date
            if (profileUser.getCreatedAt() != null) {
                joinDateLabel.setText("Tham gia " + profileUser.getCreatedAt().toLocalDate());
            }
        }
    }

    // Set up event handlers - to be called after dialog is shown
    public void setupEventHandlers(Runnable onAddFriend, Runnable onMessage, Runnable onEdit) {
        if (!isOwnProfile) {
            addFriendButton.setOnAction(e -> {
                if (onAddFriend != null)
                    onAddFriend.run();
                close();
            });

            messageButton.setOnAction(e -> {
                if (onMessage != null)
                    onMessage.run();
                close();
            });
        } else {
            editButton.setOnAction(e -> {
                if (onEdit != null)
                    onEdit.run();
                close();
            });
        }
    }
}