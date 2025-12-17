package com.example.demo.ui;

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
 * User Search Dialog - Search and interact with users
 */
@Slf4j
public class UserSearchDialog extends Stage {

    private final ChatService chatService;

    // UI Components
    private TextField searchField;
    private ListView<User> resultsList;
    private Button searchButton;
    private Button addFriendButton;
    private Button viewProfileButton;
    private Label statusLabel;

    public UserSearchDialog(ChatService chatService) {
        this.chatService = chatService;

        initModality(Modality.APPLICATION_MODAL);
        setTitle("🔍 Tìm kiếm Người dùng");
        setResizable(false);
        setWidth(500);
        setHeight(500);

        initComponents();
        setupEventHandlers();

        Scene scene = new Scene(createLayout());
        scene.getStylesheets().add("data:text/css," +
                ".search-results { -fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-radius: 8; }" +
                ".user-item { -fx-padding: 10; -fx-border-color: #e9ecef; -fx-border-width: 0 0 1 0; }" +
                ".user-item:hover { -fx-background-color: #e3f2fd; }" +
                ".user-item:selected { -fx-background-color: #007bff; -fx-text-fill: white; }");

        setScene(scene);
    }

    private void initComponents() {
        searchField = new TextField();
        searchField.setPromptText("Nhập tên người dùng...");

        resultsList = new ListView<>();
        resultsList.setPrefHeight(300);
        resultsList.setCellFactory(param -> new UserListCell());

        searchButton = new Button("🔍 Tìm kiếm");
        searchButton.getStyleClass().add("search-button");

        addFriendButton = new Button("➕ Kết bạn");
        addFriendButton.getStyleClass().add("add-friend-button");
        addFriendButton.setDisable(true);

        viewProfileButton = new Button("👤 Xem Profile");
        viewProfileButton.getStyleClass().add("view-profile-button");
        viewProfileButton.setDisable(true);

        statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12px;");
    }

    private VBox createLayout() {
        VBox mainLayout = new VBox(15);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setStyle("-fx-background-color: #ffffff;");

        // Title
        Label titleLabel = new Label("🔍 Tìm kiếm Người dùng");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #495057;");

        // Search box
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchBox.getChildren().addAll(searchField, searchButton);

        // Results
        Label resultsLabel = new Label("Kết quả tìm kiếm:");
        resultsLabel.setStyle("-fx-font-weight: bold;");

        // Action buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.getChildren().addAll(addFriendButton, viewProfileButton);

        // Bottom buttons
        HBox bottomBox = new HBox(10);
        bottomBox.setAlignment(Pos.CENTER_RIGHT);

        Button closeButton = new Button("Đóng");
        closeButton.setOnAction(e -> close());

        bottomBox.getChildren().add(closeButton);

        mainLayout.getChildren().addAll(
                titleLabel,
                searchBox,
                resultsLabel,
                resultsList,
                statusLabel,
                buttonBox,
                bottomBox);

        return mainLayout;
    }

    private void setupEventHandlers() {
        // Search on Enter key
        searchField.setOnAction(e -> performSearch());

        // Search button
        searchButton.setOnAction(e -> performSearch());

        // Selection listener
        resultsList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            addFriendButton.setDisable(!hasSelection);
            viewProfileButton.setDisable(!hasSelection);
        });

        // Add friend button
        addFriendButton.setOnAction(e -> {
            User selected = resultsList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                boolean success = chatService.addFriend(selected.getId());
                if (success) {
                    statusLabel.setText("✅ Đã gửi lời mời kết bạn!");
                    statusLabel.setStyle("-fx-text-fill: #28a745;");
                    addFriendButton.setDisable(true);
                } else {
                    statusLabel.setText("❌ Không thể gửi lời mời kết bạn");
                    statusLabel.setStyle("-fx-text-fill: #dc3545;");
                }
            }
        });

        // View profile button
        viewProfileButton.setOnAction(e -> {
            User selected = resultsList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                // TODO: Handle user selection
                close();
            }
        });
    }

    private void performSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            statusLabel.setText("Vui lòng nhập từ khóa tìm kiếm");
            statusLabel.setStyle("-fx-text-fill: #ffc107;");
            return;
        }

        statusLabel.setText("Đang tìm kiếm...");
        statusLabel.setStyle("-fx-text-fill: #007bff;");

        List<User> results = chatService.searchUsers(keyword);

        // Filter out current user from search results
        User currentUser = chatService.getCurrentUser();
        if (currentUser != null) {
            results.removeIf(u -> u.getId().equals(currentUser.getId()));
        }

        resultsList.getItems().clear();
        resultsList.getItems().addAll(results);

        if (results.isEmpty()) {
            statusLabel.setText("Không tìm thấy người dùng nào");
            statusLabel.setStyle("-fx-text-fill: #6c757d;");
        } else {
            statusLabel.setText("Tìm thấy " + results.size() + " người dùng");
            statusLabel.setStyle("-fx-text-fill: #28a745;");
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
                userBox.setPadding(new Insets(10));

                Circle avatar = new Circle(20);
                avatar.setFill(javafx.scene.paint.Color.color(Math.random(), Math.random(), Math.random()));

                VBox infoBox = new VBox(2);
                Label nameLabel = new Label(user.getDisplayName() != null ? user.getDisplayName() : user.getUsername());
                nameLabel.setStyle("-fx-font-weight: bold;");

                Label usernameLabel = new Label("@" + user.getUsername());
                usernameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6c757d;");

                infoBox.getChildren().addAll(nameLabel, usernameLabel);

                Circle statusIndicator = new Circle(8);
                if (user.getStatus() == User.Status.ONLINE) {
                    statusIndicator.setFill(javafx.scene.paint.Color.LIME);
                } else {
                    statusIndicator.setFill(javafx.scene.paint.Color.GRAY);
                }

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                userBox.getChildren().addAll(avatar, infoBox, spacer, statusIndicator);

                setGraphic(userBox);
                setText(null);
                getStyleClass().add("user-item");
            }
        }
    }
}