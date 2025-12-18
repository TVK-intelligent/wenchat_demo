package com.example.demo.ui;

import com.example.demo.client.model.User;
import com.example.demo.client.service.ChatService;
import com.example.demo.client.service.NotificationService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.function.Consumer;

/**
 * Settings Dialog - Application settings, user preferences, and theme toggle
 */
@Slf4j
public class SettingsDialog extends Stage {

    private final ChatService chatService;
    private User currentUser;
    private Long currentUserId;

    // UI Components
    private TextField displayNameField;
    private TextField avatarUrlField;
    private CheckBox showOnlineStatusCheckBox;
    private Button saveButton;
    private Button uploadAvatarButton;
    private Label avatarLabel;
    private Label statusLabel;

    // Theme toggle
    private ToggleButton themeToggle;
    private static boolean isDarkTheme = false;
    private static Consumer<Boolean> themeChangeCallback;

    // Notification settings
    private CheckBox enableNotificationsCheckBox;
    private CheckBox enableSoundCheckBox;
    private Slider volumeSlider;
    private Label volumeValueLabel;

    public SettingsDialog(ChatService chatService) {
        this.chatService = chatService;

        initModality(Modality.APPLICATION_MODAL);
        setTitle("⚙️ Cài đặt");
        setResizable(false);
        setWidth(500);
        setHeight(700);

        initComponents();
        loadCurrentSettings();
        setupEventHandlers();

        Scene scene = new Scene(createLayout());
        if (getClass().getResource("/styles.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        }

        setScene(scene);
    }

    /**
     * Set callback for theme changes
     */
    public static void setThemeChangeCallback(Consumer<Boolean> callback) {
        themeChangeCallback = callback;
    }

    /**
     * Get current theme state
     */
    public static boolean isDarkTheme() {
        return isDarkTheme;
    }

    private void initComponents() {
        displayNameField = new TextField();
        displayNameField.setStyle(
                "-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; " +
                        "-fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10;");

        avatarUrlField = new TextField();
        avatarUrlField.setStyle(
                "-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; " +
                        "-fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10;");
        avatarUrlField.setEditable(false);

        showOnlineStatusCheckBox = new CheckBox("Hiển thị trạng thái online");
        showOnlineStatusCheckBox.setStyle("-fx-font-size: 13px;");

        saveButton = new Button("💾 Lưu thay đổi");
        saveButton.getStyleClass().add("settings-save-button");

        uploadAvatarButton = new Button("📷 Upload");
        uploadAvatarButton.getStyleClass().add("settings-upload-button");

        avatarLabel = new Label("Không có ảnh đại diện");
        avatarLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12px;");

        statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-size: 12px;");

        // Theme toggle button
        themeToggle = new ToggleButton(isDarkTheme ? "🌙 Dark Mode" : "☀️ Light Mode");
        themeToggle.setSelected(isDarkTheme);
        themeToggle.setStyle(
                "-fx-background-color: " + (isDarkTheme ? "#1a1a2e" : "#f0f2f5") + "; " +
                        "-fx-text-fill: " + (isDarkTheme ? "#e4e6eb" : "#212529") + "; " +
                        "-fx-font-weight: bold; -fx-padding: 12 20; " +
                        "-fx-border-radius: 25; -fx-background-radius: 25; -fx-cursor: hand; " +
                        "-fx-border-color: " + (isDarkTheme ? "#3a3a5c" : "#dee2e6") + ";");
    }

    private VBox createLayout() {
        VBox mainLayout = new VBox(20);
        mainLayout.setPadding(new Insets(25));
        mainLayout.setStyle("-fx-background-color: #ffffff;");

        // Title with icon
        HBox titleBox = new HBox(12);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Circle titleIcon = new Circle(20);
        titleIcon.setFill(javafx.scene.paint.LinearGradient.valueOf(
                "linear-gradient(to bottom right, #667eea, #764ba2)"));
        Label iconLabel = new Label("⚙️");
        iconLabel.setStyle("-fx-font-size: 16px;");
        StackPane iconPane = new StackPane(titleIcon, iconLabel);

        Label titleLabel = new Label("Cài đặt");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #212529;");

        titleBox.getChildren().addAll(iconPane, titleLabel);

        // Appearance section
        VBox appearanceSection = createSection("🎨 Giao diện", createAppearanceContent());

        // Profile section
        VBox profileSection = createSection("👤 Thông tin cá nhân", createProfileContent());

        // Notification section
        VBox notificationSection = createSection("🔔 Thông báo", createNotificationContent());

        // Privacy section
        VBox privacySection = createSection("🔒 Quyền riêng tư", createPrivacyContent());

        // Status label
        HBox statusBox = new HBox();
        statusBox.setAlignment(Pos.CENTER);
        statusBox.getChildren().add(statusLabel);

        // Bottom buttons
        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button closeButton = new Button("Đóng");
        closeButton.getStyleClass().add("settings-close-button");
        closeButton.setOnAction(e -> close());

        buttonBox.getChildren().addAll(saveButton, closeButton);

        mainLayout.getChildren().addAll(
                titleBox,
                appearanceSection,
                profileSection,
                notificationSection,
                privacySection,
                statusBox,
                buttonBox);

        // Wrap in ScrollPane for scrolling when content exceeds height
        ScrollPane scrollPane = new ScrollPane(mainLayout);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #ffffff; -fx-background: #ffffff;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox wrapper = new VBox(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        return wrapper;
    }

    private VBox createSection(String title, VBox content) {
        VBox section = new VBox(12);
        section.setPadding(new Insets(15));
        section.setStyle(
                "-fx-background-color: #f8f9fa; -fx-background-radius: 12; " +
                        "-fx-border-color: #e9ecef; -fx-border-radius: 12;");

        Label sectionTitle = new Label(title);
        sectionTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #495057;");

        section.getChildren().addAll(sectionTitle, content);
        return section;
    }

    private VBox createAppearanceContent() {
        VBox content = new VBox(12);

        HBox themeBox = new HBox(15);
        themeBox.setAlignment(Pos.CENTER_LEFT);

        Label themeLabel = new Label("Chế độ hiển thị:");
        themeLabel.setStyle("-fx-font-size: 13px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        themeBox.getChildren().addAll(themeLabel, spacer, themeToggle);
        content.getChildren().add(themeBox);

        return content;
    }

    private VBox createProfileContent() {
        VBox content = new VBox(12);

        Label displayNameLabel = new Label("Tên hiển thị:");
        displayNameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        HBox avatarBox = new HBox(12);
        avatarBox.setAlignment(Pos.CENTER_LEFT);

        VBox avatarInfo = new VBox(4);
        Label avatarTitle = new Label("Ảnh đại diện:");
        avatarTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        avatarInfo.getChildren().addAll(avatarTitle, avatarLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        avatarBox.getChildren().addAll(avatarInfo, spacer, uploadAvatarButton);

        content.getChildren().addAll(
                displayNameLabel, displayNameField,
                avatarBox);

        return content;
    }

    private VBox createNotificationContent() {
        VBox content = new VBox(12);

        // Get current settings from NotificationService
        NotificationService notificationService = NotificationService.getInstance();

        enableNotificationsCheckBox = new CheckBox("Bật thông báo desktop");
        enableNotificationsCheckBox.setSelected(notificationService.isGlobalEnabled());
        enableNotificationsCheckBox.setStyle("-fx-font-size: 13px;");

        enableSoundCheckBox = new CheckBox("Bật âm thanh thông báo");
        enableSoundCheckBox.setSelected(notificationService.isSoundEnabled());
        enableSoundCheckBox.setStyle("-fx-font-size: 13px;");

        // Volume slider
        HBox volumeBox = new HBox(12);
        volumeBox.setAlignment(Pos.CENTER_LEFT);

        Label volumeLabel = new Label("🔊 Âm lượng:");
        volumeLabel.setStyle("-fx-font-size: 13px;");

        volumeSlider = new Slider(0, 100, notificationService.getVolume());
        volumeSlider.setPrefWidth(150);
        volumeSlider.setShowTickMarks(false);
        volumeSlider.setShowTickLabels(false);
        HBox.setHgrow(volumeSlider, Priority.ALWAYS);

        volumeValueLabel = new Label(notificationService.getVolume() + "%");
        volumeValueLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-min-width: 45px;");

        // Test button for volume
        Button testSoundButton = new Button("🔔 Test");
        testSoundButton.setStyle(
                "-fx-background-color: #fb923c; -fx-text-fill: white; " +
                        "-fx-font-weight: bold; -fx-padding: 6 12; " +
                        "-fx-border-radius: 15; -fx-background-radius: 15; -fx-cursor: hand; -fx-font-size: 11px;");
        testSoundButton.setOnAction(e -> notificationService.playNotificationSound(
                NotificationService.NotificationType.MESSAGE));

        volumeBox.getChildren().addAll(volumeLabel, volumeSlider, volumeValueLabel, testSoundButton);

        // Add event handlers to update NotificationService
        enableNotificationsCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            notificationService.setGlobalEnabled(newVal);
        });

        enableSoundCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            notificationService.setSoundEnabled(newVal);
            volumeSlider.setDisable(!newVal);
            testSoundButton.setDisable(!newVal);
        });

        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int vol = newVal.intValue();
            volumeValueLabel.setText(vol + "%");
            notificationService.setVolume(vol);
        });

        // Initial state
        volumeSlider.setDisable(!notificationService.isSoundEnabled());
        testSoundButton.setDisable(!notificationService.isSoundEnabled());

        Label hintLabel = new Label("💡 Thông báo sẽ hiển thị khi ứng dụng không được focus");
        hintLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");

        content.getChildren().addAll(enableNotificationsCheckBox, enableSoundCheckBox, volumeBox, hintLabel);
        return content;
    }

    private VBox createPrivacyContent() {
        VBox content = new VBox(8);
        content.getChildren().add(showOnlineStatusCheckBox);
        return content;
    }

    private void loadCurrentSettings() {
        try {
            currentUser = chatService.getCurrentUser();
            if (currentUser != null) {
                currentUserId = currentUser.getId();
                displayNameField.setText(currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "");
                avatarUrlField.setText(currentUser.getAvatarUrl() != null ? currentUser.getAvatarUrl() : "");

                showOnlineStatusCheckBox.setSelected(
                        currentUser.getShowOnlineStatus() != null ? currentUser.getShowOnlineStatus() : true);

                if (currentUser.getAvatarUrl() != null && !currentUser.getAvatarUrl().isEmpty()) {
                    avatarLabel.setText("✅ Đã có ảnh đại diện");
                    avatarLabel.setStyle("-fx-text-fill: #28a745; -fx-font-size: 12px;");
                }
            }
        } catch (Exception e) {
            log.error("Failed to load current user settings", e);
            statusLabel.setText("Lỗi khi tải cài đặt: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: #dc3545;");
        }
    }

    private void setupEventHandlers() {
        // Save button
        saveButton.setOnAction(e -> saveSettings());

        // Upload avatar button
        uploadAvatarButton.setOnAction(e -> uploadAvatar());

        // Theme toggle
        themeToggle.setOnAction(e -> toggleTheme());
    }

    private void toggleTheme() {
        isDarkTheme = themeToggle.isSelected();

        // Update toggle button appearance
        themeToggle.setText(isDarkTheme ? "🌙 Dark Mode" : "☀️ Light Mode");
        themeToggle.setStyle(
                "-fx-background-color: " + (isDarkTheme ? "#1a1a2e" : "#f0f2f5") + "; " +
                        "-fx-text-fill: " + (isDarkTheme ? "#e4e6eb" : "#212529") + "; " +
                        "-fx-font-weight: bold; -fx-padding: 12 20; " +
                        "-fx-border-radius: 25; -fx-background-radius: 25; -fx-cursor: hand; " +
                        "-fx-border-color: " + (isDarkTheme ? "#3a3a5c" : "#dee2e6") + ";");

        // Notify callback
        if (themeChangeCallback != null) {
            themeChangeCallback.accept(isDarkTheme);
        }

        statusLabel.setText("✅ Theme đã được thay đổi!");
        statusLabel.setStyle("-fx-text-fill: #28a745;");
    }

    private void saveSettings() {
        if (currentUser == null) {
            showError("Lỗi", "Không thể tải thông tin người dùng");
            return;
        }

        String newDisplayName = displayNameField.getText().trim();
        Boolean newShowOnlineStatus = showOnlineStatusCheckBox.isSelected();

        log.info("💾 Saving settings - Display Name: {}", newDisplayName);

        // Update user profile
        saveButton.setDisable(true);
        saveButton.setText("⏳ Đang lưu...");

        boolean success = chatService.updateUserProfile(
                currentUser.getId(),
                newDisplayName.isEmpty() ? null : newDisplayName,
                currentUser.getAvatarUrl());

        if (success) {
            statusLabel.setText("✅ Cài đặt đã được lưu!");
            statusLabel.setStyle("-fx-text-fill: #28a745;");

            // Refresh user info from server
            User updatedUser = chatService.getCurrentUser();
            if (updatedUser != null) {
                currentUser = updatedUser;
                log.info("✅ User info refreshed - Display Name: {}", updatedUser.getDisplayName());
            }

            currentUser.setDisplayName(newDisplayName);
            currentUser.setShowOnlineStatus(newShowOnlineStatus);

            showSuccess("Thành công", "Đã lưu thay đổi thành công!");
        } else {
            statusLabel.setText("❌ Không thể lưu cài đặt");
            statusLabel.setStyle("-fx-text-fill: #dc3545;");
            showError("Lỗi", "Không thể lưu cài đặt. Vui lòng kiểm tra log để xem chi tiết lỗi.");
        }

        saveButton.setDisable(false);
        saveButton.setText("💾 Lưu thay đổi");
    }

    private void uploadAvatar() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh đại diện");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));

        File selectedFile = fileChooser.showOpenDialog(this);
        if (selectedFile != null) {
            try {
                uploadAvatarButton.setDisable(true);
                uploadAvatarButton.setText("⏳...");

                boolean success = chatService.uploadAvatar(currentUserId, selectedFile.getAbsolutePath());

                if (success) {
                    User updatedUser = chatService.getCurrentUser();
                    if (updatedUser != null) {
                        avatarUrlField.setText(updatedUser.getAvatarUrl() != null ? updatedUser.getAvatarUrl() : "");
                        avatarLabel.setText("✅ Đã có ảnh đại diện");
                        avatarLabel.setStyle("-fx-text-fill: #28a745; -fx-font-size: 12px;");
                        showSuccess("Thành công", "Ảnh đại diện đã được cập nhật!");
                    }
                } else {
                    showError("Lỗi", "Không thể tải lên ảnh đại diện.");
                }
            } catch (Exception e) {
                log.error("Error uploading avatar", e);
                String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.contains("403")) {
                    showError("Lỗi",
                            "Server không hỗ trợ upload ảnh đại diện (HTTP 403). Vui lòng liên hệ admin để kiểm tra API.");
                } else {
                    showError("Lỗi", "Lỗi khi tải lên ảnh: " + errorMessage);
                }
            } finally {
                uploadAvatarButton.setDisable(false);
                uploadAvatarButton.setText("📷 Upload");
            }
        }
    }

    private void showSuccess(String title, String message) {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
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
}