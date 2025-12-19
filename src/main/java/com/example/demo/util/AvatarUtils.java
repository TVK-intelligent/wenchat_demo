package com.example.demo.util;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import lombok.extern.slf4j.Slf4j;

/**
 * 🖼️ Avatar Utilities - Helper methods for loading and displaying user avatars
 */
@Slf4j
public class AvatarUtils {

    private static String baseUrl = "http://26.6.143.150:8081";

    /**
     * Set the base URL for avatar loading
     */
    public static void setBaseUrl(String url) {
        baseUrl = url;
    }

    /**
     * 🎨 Generate a consistent color based on username
     */
    public static Color getColorForUsername(String username) {
        if (username == null || username.isEmpty()) {
            return Color.GRAY;
        }

        // Generate consistent color from username hash
        int hash = username.hashCode();
        double hue = Math.abs(hash % 360);
        return Color.hsb(hue, 0.6, 0.8);
    }

    /**
     * 📷 Load avatar image from URL
     * Returns null if loading fails
     */
    public static Image loadAvatarImage(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
            return null;
        }

        try {
            String fullUrl = avatarUrl;

            // Prepend base URL if the avatar URL is relative
            if (!avatarUrl.startsWith("http://") && !avatarUrl.startsWith("https://")) {
                fullUrl = baseUrl + (avatarUrl.startsWith("/") ? "" : "/") + avatarUrl;
            }

            log.debug("📷 Loading avatar from: {}", fullUrl);

            // Load image with background loading
            Image image = new Image(fullUrl, true);

            // Check for loading errors
            if (image.isError()) {
                log.warn("⚠️ Failed to load avatar: {}", image.getException());
                return null;
            }

            return image;

        } catch (Exception e) {
            log.error("❌ Error loading avatar: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 🔄 Set avatar on a Circle - loads image if available, otherwise uses color
     * 
     * @param circle    The circle to set the avatar on
     * @param avatarUrl The URL of the avatar image (can be null)
     * @param username  The username for fallback color generation
     */
    public static void setAvatarOnCircle(Circle circle, String avatarUrl, String username) {
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            Image image = loadAvatarImage(avatarUrl);
            if (image != null) {
                // Apply image pattern to circle
                ImagePattern pattern = new ImagePattern(image);
                circle.setFill(pattern);

                // Add error listener for async loading
                image.errorProperty().addListener((obs, wasError, isError) -> {
                    if (isError) {
                        log.warn("⚠️ Avatar image load error, using fallback color");
                        javafx.application.Platform.runLater(() -> {
                            circle.setFill(getColorForUsername(username));
                        });
                    }
                });

                return;
            }
        }

        // Fallback to username-based color
        circle.setFill(getColorForUsername(username));
    }

    /**
     * 🔄 Set avatar on a Circle with async loading
     * 
     * @param circle    The circle to set the avatar on
     * @param avatarUrl The URL of the avatar image (can be null)
     * @param username  The username for fallback color generation
     * @param size      The desired size of the avatar image
     */
    public static void setAvatarOnCircleAsync(Circle circle, String avatarUrl, String username, double size) {
        // Set initial fallback color
        circle.setFill(getColorForUsername(username));

        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            try {
                // Build full URL - needs to be effectively final for lambda
                final String fullUrl;
                if (!avatarUrl.startsWith("http://") && !avatarUrl.startsWith("https://")) {
                    fullUrl = baseUrl + (avatarUrl.startsWith("/") ? "" : "/") + avatarUrl;
                } else {
                    fullUrl = avatarUrl;
                }

                log.info("📷 Loading avatar async from: {}", fullUrl);

                // Load image asynchronously with specified size
                Image image = new Image(fullUrl, size * 2, size * 2, true, true, true);

                // Check if image is already loaded (cached case)
                if (image.getProgress() >= 1.0 && !image.isError()) {
                    log.info("📷 Avatar loaded immediately (cached): {}", fullUrl);
                    ImagePattern pattern = new ImagePattern(image);
                    circle.setFill(pattern);
                } else {
                    // When loading completes, update the circle
                    image.progressProperty().addListener((obs, oldProgress, newProgress) -> {
                        if (newProgress.doubleValue() >= 1.0 && !image.isError()) {
                            log.info("📷 Avatar loaded async: {}", fullUrl);
                            javafx.application.Platform.runLater(() -> {
                                try {
                                    ImagePattern pattern = new ImagePattern(image);
                                    circle.setFill(pattern);
                                    // Force layout refresh to ensure visual update
                                    circle.setVisible(false);
                                    circle.setVisible(true);
                                    if (circle.getParent() != null) {
                                        circle.getParent().requestLayout();
                                    }
                                    log.info("✅ Avatar setFill completed for circle: {}", circle);
                                } catch (Exception ex) {
                                    log.error("❌ Error setting avatar fill: {}", ex.getMessage());
                                }
                            });
                        }
                    });
                }

                // Handle errors
                image.errorProperty().addListener((obs, wasError, isError) -> {
                    if (isError) {
                        log.warn("⚠️ Avatar async load failed for: {}", avatarUrl);
                    }
                });

            } catch (Exception e) {
                log.error("❌ Error starting async avatar load: {}", e.getMessage());
            }
        }
    }

    /**
     * 🔤 Get initials from a display name or username
     */
    public static String getInitials(String displayName, String username) {
        String name = (displayName != null && !displayName.trim().isEmpty()) ? displayName : username;

        if (name == null || name.trim().isEmpty()) {
            return "?";
        }

        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
        } else if (parts.length == 1 && parts[0].length() >= 2) {
            return parts[0].substring(0, 2).toUpperCase();
        } else {
            return parts[0].substring(0, 1).toUpperCase();
        }
    }

    /**
     * 🎨 Create a paint object for avatar - either image or color
     */
    public static Paint createAvatarPaint(String avatarUrl, String username) {
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            Image image = loadAvatarImage(avatarUrl);
            if (image != null && !image.isError()) {
                return new ImagePattern(image);
            }
        }
        return getColorForUsername(username);
    }
}
