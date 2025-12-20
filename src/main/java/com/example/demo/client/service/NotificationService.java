package com.example.demo.client.service;

import javafx.application.Platform;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.*;
import java.awt.*;
import java.awt.TrayIcon.MessageType;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

/**
 * 🔔 NotificationService - Centralized notification management
 * Features:
 * - Desktop notifications via System Tray
 * - Sound alerts for different notification types
 * - Per-type enable/disable settings
 */
@Slf4j
public class NotificationService {

    /**
     * Types of notifications supported
     */
    public enum NotificationType {
        MESSAGE("Tin nhắn mới", "/sounds/message.wav"),
        FRIEND_REQUEST("Lời mời kết bạn", "/sounds/friend_request.wav"),
        ROOM_INVITE("Lời mời vào phòng", "/sounds/room_invite.wav");

        private final String displayName;
        private final String soundPath;

        NotificationType(String displayName, String soundPath) {
            this.displayName = displayName;
            this.soundPath = soundPath;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getSoundPath() {
            return soundPath;
        }
    }

    // Singleton instance
    private static NotificationService instance;

    // System tray components
    private SystemTray systemTray;
    private TrayIcon trayIcon;
    private boolean traySupported = false;

    // Settings
    private final Map<NotificationType, Boolean> enabledMap = new EnumMap<>(NotificationType.class);
    private boolean soundEnabled = true;
    private boolean globalEnabled = true;
    private int volume = 70; // Volume level 0-100

    // Window focus tracking
    private Stage primaryStage;
    private boolean windowFocused = true;

    // Sound cache
    private final Map<NotificationType, Clip> soundClips = new EnumMap<>(NotificationType.class);

    private NotificationService() {
        // Initialize all notification types as enabled
        for (NotificationType type : NotificationType.values()) {
            enabledMap.put(type, true);
        }
        initializeSystemTray();
        preloadSounds();
    }

    /**
     * Get singleton instance
     */
    public static synchronized NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }

    /**
     * Initialize system tray for desktop notifications
     */
    private void initializeSystemTray() {
        if (!SystemTray.isSupported()) {
            log.warn("System tray is not supported on this platform");
            traySupported = false;
            return;
        }

        try {
            systemTray = SystemTray.getSystemTray();

            // Create tray icon with app icon
            Image image = Toolkit.getDefaultToolkit().createImage(
                    getClass().getResource("/icons/app_icon.png"));

            // Fallback to a simple generated icon if resource not found
            if (image == null) {
                image = createDefaultIcon();
            }

            trayIcon = new TrayIcon(image, "WenChat");
            trayIcon.setImageAutoSize(true);

            // Add to system tray (but don't show icon permanently, just for notifications)
            // We'll add/remove as needed, or just use it for notifications
            traySupported = true;
            log.info("System tray initialized successfully");

        } catch (Exception e) {
            log.error("Failed to initialize system tray: {}", e.getMessage());
            traySupported = false;
        }
    }

    /**
     * Create a simple default icon if app icon not found
     */
    private Image createDefaultIcon() {
        int size = 16;
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(new Color(102, 126, 234)); // Primary app color
        g2d.fillOval(0, 0, size, size);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        g2d.drawString("W", 3, 12);
        g2d.dispose();
        return img;
    }

    /**
     * Preload sound files for faster playback
     */
    private void preloadSounds() {
        for (NotificationType type : NotificationType.values()) {
            try {
                InputStream audioSrc = getClass().getResourceAsStream(type.getSoundPath());
                if (audioSrc != null) {
                    InputStream bufferedIn = new BufferedInputStream(audioSrc);
                    AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedIn);
                    Clip clip = AudioSystem.getClip();
                    clip.open(audioStream);
                    soundClips.put(type, clip);
                    log.debug("Loaded sound for {}", type);
                } else {
                    log.warn("Sound file not found: {}", type.getSoundPath());
                }
            } catch (Exception e) {
                log.warn("Failed to load sound for {}: {}", type, e.getMessage());
            }
        }
    }

    /**
     * Set the primary stage for focus tracking
     */
    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
        if (stage != null) {
            stage.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                windowFocused = isNowFocused;
            });
            windowFocused = stage.isFocused();
        }
    }

    /**
     * Check if window is currently focused
     */
    public boolean isWindowFocused() {
        return windowFocused;
    }

    /**
     * Show a desktop notification
     */
    public void showNotification(NotificationType type, String title, String message) {
        if (!globalEnabled || !enabledMap.getOrDefault(type, true)) {
            return;
        }

        // Don't show if window is focused (user is actively using app)
        if (windowFocused) {
            return;
        }

        // Play sound
        if (soundEnabled) {
            playNotificationSound(type);
        }

        // Show desktop notification
        if (traySupported && trayIcon != null) {
            try {
                // Ensure tray icon is in system tray
                if (systemTray != null) {
                    // Remove first to avoid duplicates
                    try {
                        systemTray.remove(trayIcon);
                    } catch (Exception ignored) {
                    }

                    systemTray.add(trayIcon);
                }

                // Display the notification
                Platform.runLater(() -> {
                    trayIcon.displayMessage(title, message, MessageType.INFO);
                });

                // Schedule removal of tray icon after a delay
                new Thread(() -> {
                    try {
                        Thread.sleep(5000);
                        if (systemTray != null && trayIcon != null) {
                            systemTray.remove(trayIcon);
                        }
                    } catch (Exception ignored) {
                    }
                }).start();

            } catch (Exception e) {
                log.error("Failed to show notification: {}", e.getMessage());
            }
        } else {
            // Fallback: log the notification
            log.info("Notification [{}]: {} - {}", type, title, message);
        }
    }

    /**
     * Play notification sound for a specific type
     */
    public void playNotificationSound(NotificationType type) {
        if (!soundEnabled) {
            return;
        }

        Clip clip = soundClips.get(type);
        if (clip != null) {
            try {
                // Stop if already playing and reset to beginning
                if (clip.isRunning()) {
                    clip.stop();
                }
                clip.setFramePosition(0);
                clip.start();
            } catch (Exception e) {
                log.warn("Failed to play sound for {}: {}", type, e.getMessage());
                // Try to play fallback beep
                playFallbackBeep();
            }
        } else {
            // No clip loaded, play fallback
            playFallbackBeep();
        }
    }

    /**
     * Play a simple beep as fallback when sound files are not available
     */
    private void playFallbackBeep() {
        try {
            Toolkit.getDefaultToolkit().beep();
        } catch (Exception e) {
            log.warn("Failed to play fallback beep");
        }
    }

    // ============== Settings Methods ==============

    /**
     * Enable or disable notifications globally
     */
    public void setGlobalEnabled(boolean enabled) {
        this.globalEnabled = enabled;
    }

    /**
     * Check if notifications are globally enabled
     */
    public boolean isGlobalEnabled() {
        return globalEnabled;
    }

    /**
     * Enable or disable a specific notification type
     */
    public void setEnabled(NotificationType type, boolean enabled) {
        enabledMap.put(type, enabled);
    }

    /**
     * Check if a specific notification type is enabled
     */
    public boolean isEnabled(NotificationType type) {
        return enabledMap.getOrDefault(type, true);
    }

    /**
     * Enable or disable sound
     */
    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
    }

    /**
     * Check if sound is enabled
     */
    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    /**
     * Set volume level (0-100)
     */
    public void setVolume(int volume) {
        this.volume = Math.max(0, Math.min(100, volume));
        // Apply volume to all loaded clips
        applyVolumeToClips();
    }

    /**
     * Get current volume level (0-100)
     */
    public int getVolume() {
        return volume;
    }

    /**
     * Apply volume setting to all sound clips
     */
    private void applyVolumeToClips() {
        for (Clip clip : soundClips.values()) {
            if (clip != null) {
                try {
                    FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    // Convert 0-100 to decibels (-80 to 0)
                    float dB;
                    if (volume == 0) {
                        dB = gainControl.getMinimum();
                    } else {
                        dB = (float) (20 * Math.log10(volume / 100.0));
                        dB = Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), dB));
                    }
                    gainControl.setValue(dB);
                } catch (Exception e) {
                    log.debug("Could not set volume for clip: {}", e.getMessage());
                }
            }
        }
    }

    // ============== Convenience Methods ==============

    /**
     * Show message notification
     */
    public void showMessageNotification(String senderName, String messagePreview) {
        String title = "💬 " + senderName;
        String message = messagePreview.length() > 50
                ? messagePreview.substring(0, 47) + "..."
                : messagePreview;
        showNotification(NotificationType.MESSAGE, title, message);
    }

    /**
     * Show friend request notification - ALWAYS plays sound
     */
    public void showFriendRequestNotification(String senderName) {
        String title = "👋 Lời mời kết bạn";
        String message = senderName + " muốn kết bạn với bạn";

        // Always play sound for friend requests (important notifications)
        if (soundEnabled && globalEnabled) {
            playNotificationSound(NotificationType.FRIEND_REQUEST);
        }

        // Show desktop notification only if window is not focused
        if (!windowFocused) {
            showNotification(NotificationType.FRIEND_REQUEST, title, message);
        } else {
            // Show in-app toast when window is focused
            showInAppToast(title, message);
        }
    }

    /**
     * Show room invite notification - ALWAYS plays sound
     */
    public void showRoomInviteNotification(String inviterName, String roomName) {
        String title = "🏠 Lời mời vào phòng";
        String message = inviterName + " mời bạn vào phòng " + roomName;

        // Always play sound for room invites (important notifications)
        if (soundEnabled && globalEnabled) {
            playNotificationSound(NotificationType.ROOM_INVITE);
        }

        // Show desktop notification only if window is not focused
        if (!windowFocused) {
            showNotification(NotificationType.ROOM_INVITE, title, message);
        } else {
            // Show in-app toast when window is focused
            showInAppToast(title, message);
        }
    }

    /**
     * Show an in-app toast notification (when window is focused)
     * This creates a small popup inside the application
     */
    public void showInAppToast(String title, String message) {
        if (primaryStage == null || !globalEnabled) {
            return;
        }

        Platform.runLater(() -> {
            try {
                // Create toast notification
                javafx.scene.layout.VBox toast = new javafx.scene.layout.VBox(4);
                toast.setStyle(
                        "-fx-background-color: linear-gradient(to right, #667eea, #764ba2);" +
                                "-fx-background-radius: 10;" +
                                "-fx-padding: 12 16 12 16;" +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 4);");
                toast.setMaxWidth(320);
                toast.setOpacity(0);

                javafx.scene.control.Label titleLabel = new javafx.scene.control.Label(title);
                titleLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");

                javafx.scene.control.Label msgLabel = new javafx.scene.control.Label(message);
                msgLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.9); -fx-font-size: 12px;");
                msgLabel.setWrapText(true);

                toast.getChildren().addAll(titleLabel, msgLabel);

                // Get the scene's root
                if (primaryStage.getScene() != null
                        && primaryStage.getScene().getRoot() instanceof javafx.scene.layout.Pane) {
                    javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) primaryStage.getScene().getRoot();

                    // Position toast at top-right
                    toast.setLayoutX(root.getWidth() - 340);
                    toast.setLayoutY(20);

                    // Add to scene
                    if (root instanceof javafx.scene.layout.BorderPane) {
                        javafx.scene.layout.StackPane overlay = new javafx.scene.layout.StackPane(toast);
                        overlay.setAlignment(javafx.geometry.Pos.TOP_RIGHT);
                        overlay.setPadding(new javafx.geometry.Insets(20, 20, 0, 0));
                        overlay.setMouseTransparent(true);
                        overlay.setPickOnBounds(false);

                        // Find existing overlay or create new one
                        javafx.scene.layout.BorderPane bp = (javafx.scene.layout.BorderPane) root;

                        // Add toast directly to scene
                        javafx.scene.Group toastGroup = new javafx.scene.Group(toast);
                        toastGroup.setManaged(false);
                        toastGroup.setLayoutX(root.getWidth() - 340);
                        toastGroup.setLayoutY(20);

                        if (!root.getChildren().contains(toastGroup)) {
                            root.getChildren().add(toastGroup);
                        }

                        // Fade in animation
                        javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(
                                javafx.util.Duration.millis(300), toast);
                        fadeIn.setFromValue(0);
                        fadeIn.setToValue(1);

                        // Fade out animation after delay
                        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                                javafx.util.Duration.seconds(4));

                        javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(
                                javafx.util.Duration.millis(500), toast);
                        fadeOut.setFromValue(1);
                        fadeOut.setToValue(0);
                        fadeOut.setOnFinished(e -> root.getChildren().remove(toastGroup));

                        // Play animations
                        fadeIn.setOnFinished(e -> pause.play());
                        pause.setOnFinished(e -> fadeOut.play());
                        fadeIn.play();
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to show in-app toast: {}", e.getMessage());
            }
        });
    }

    /**
     * Cleanup resources on shutdown
     */
    public void shutdown() {
        // Close all sound clips
        for (Clip clip : soundClips.values()) {
            if (clip != null) {
                clip.close();
            }
        }
        soundClips.clear();

        // Remove tray icon
        if (systemTray != null && trayIcon != null) {
            try {
                systemTray.remove(trayIcon);
            } catch (Exception ignored) {
            }
        }

        log.info("NotificationService shutdown complete");
    }
}
