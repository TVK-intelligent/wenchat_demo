package com.example.demo.ui.components;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import com.example.demo.util.IconFactory;
import com.example.demo.util.AnimationUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Modern Material Design Input Component
 * Demonstrates usage of JFoenix, FontAwesome icons, and animations
 */
public class ModernInputExample extends VBox {

    private JFXTextField messageField;
    private JFXButton sendButton;
    private JFXButton fileButton;
    private JFXButton emojiButton;

    public ModernInputExample() {
        super(15);
        setPadding(new Insets(20));
        setAlignment(Pos.CENTER);
        getStyleClass().add("card");

        createComponents();
        setupLayout();
        setupEventHandlers();
    }

    private void createComponents() {
        // Modern text field with floating label
        messageField = new JFXTextField();
        messageField.setPromptText("Type a message...");
        messageField.getStyleClass().add("jfx-text-field");
        messageField.setPrefWidth(400);

        // Send button with icon
        sendButton = new JFXButton("SEND");
        sendButton.setGraphic(IconFactory.createWhiteIcon(FontAwesomeIcon.SEND));
        sendButton.getStyleClass().add("jfx-button");

        // File button with icon
        fileButton = new JFXButton();
        fileButton.setGraphic(IconFactory.createPrimaryIcon(FontAwesomeIcon.PAPERCLIP));
        fileButton.getStyleClass().add("file-button");
        fileButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");

        // Emoji button with icon
        emojiButton = new JFXButton();
        emojiButton.setGraphic(IconFactory.createIcon(FontAwesomeIcon.SMILE_ALT, "#FFD700"));
        emojiButton.getStyleClass().add("emoji-button");
        emojiButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
    }

    private void setupLayout() {
        // Title
        Label titleLabel = new Label("Modern Input Component Demo");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #667eea;");

        // Input container
        HBox inputContainer = new HBox(10);
        inputContainer.setAlignment(Pos.CENTER_LEFT);

        // Button group
        HBox buttonGroup = new HBox(5);
        buttonGroup.setAlignment(Pos.CENTER);
        buttonGroup.getChildren().addAll(fileButton, emojiButton);

        inputContainer.getChildren().addAll(buttonGroup, messageField, sendButton);

        // Info label
        Label infoLabel = new Label("✨ Features: JFoenix buttons, FontAwesome icons, smooth animations");
        infoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");

        getChildren().addAll(titleLabel, inputContainer, infoLabel);

        // Animate component on creation
        AnimationUtils.fadeIn(this);
    }

    private void setupEventHandlers() {
        // Send button with animation
        sendButton.setOnAction(e -> {
            String message = messageField.getText();
            if (!message.isEmpty()) {
                AnimationUtils.pulse(sendButton);
                System.out.println("Sending message: " + message);
                messageField.clear();
                AnimationUtils.fadeOut(this);
                // Simulate sending delay
                javafx.application.Platform.runLater(() -> {
                    try {
                        Thread.sleep(300);
                        AnimationUtils.fadeIn(this);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                });
            } else {
                AnimationUtils.shake(messageField);
            }
        });

        // File button animation
        fileButton.setOnAction(e -> {
            AnimationUtils.bounce(fileButton);
            System.out.println("File button clicked");
        });

        // Emoji button animation
        emojiButton.setOnAction(e -> {
            AnimationUtils.rotateIn(emojiButton);
            System.out.println("Emoji button clicked");
        });

        // Animate on hover
        sendButton.setOnMouseEntered(e -> AnimationUtils.pulse(sendButton.getGraphic()));
        fileButton.setOnMouseEntered(e -> AnimationUtils.pulse(fileButton.getGraphic()));
        emojiButton.setOnMouseEntered(e -> AnimationUtils.pulse(emojiButton.getGraphic()));
    }

    // Getters
    public JFXTextField getMessageField() {
        return messageField;
    }

    public JFXButton getSendButton() {
        return sendButton;
    }
}
