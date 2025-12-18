package com.example.demo.util;

import animatefx.animation.*;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * Utility class cho animations sử dụng AnimateFX
 */
public class AnimationUtils {

    private static final Duration DEFAULT_DURATION = Duration.millis(300);
    private static final Duration FAST_DURATION = Duration.millis(150);
    private static final Duration SLOW_DURATION = Duration.millis(500);

    /**
     * Fade in animation
     */
    public static void fadeIn(Node node) {
        new FadeIn(node)
                .setSpeed(1.5)
                .play();
    }

    /**
     * Fade out animation
     */
    public static void fadeOut(Node node) {
        new FadeOut(node)
                .setSpeed(1.5)
                .play();
    }

    /**
     * Slide in from left
     */
    public static void slideInLeft(Node node) {
        new SlideInLeft(node)
                .setSpeed(1.5)
                .play();
    }

    /**
     * Slide in from right
     */
    public static void slideInRight(Node node) {
        new SlideInRight(node)
                .setSpeed(1.5)
                .play();
    }

    /**
     * Slide in from bottom (cho messages)
     */
    public static void slideInUp(Node node) {
        new SlideInUp(node)
                .setSpeed(2.0)
                .play();
    }

    /**
     * Bounce animation
     */
    public static void bounce(Node node) {
        new Bounce(node)
                .setSpeed(1.0)
                .play();
    }

    /**
     * Pulse animation (cho notifications)
     */
    public static void pulse(Node node) {
        new Pulse(node)
                .setSpeed(1.5)
                .setCycleCount(2)
                .play();
    }

    /**
     * Shake animation (cho errors)
     */
    public static void shake(Node node) {
        new Shake(node)
                .setSpeed(2.0)
                .play();
    }

    /**
     * Zoom in animation
     */
    public static void zoomIn(Node node) {
        new ZoomIn(node)
                .setSpeed(2.0)
                .play();
    }

    /**
     * Flash animation (để thu hút sự chú ý)
     */
    public static void flash(Node node) {
        new Flash(node)
                .setSpeed(1.5)
                .setCycleCount(3)
                .play();
    }

    /**
     * Rotate in animation
     */
    public static void rotateIn(Node node) {
        new RotateIn(node)
                .setSpeed(1.5)
                .play();
    }
}
