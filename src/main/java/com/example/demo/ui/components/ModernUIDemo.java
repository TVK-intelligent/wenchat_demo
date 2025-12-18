package com.example.demo.ui.components;

import com.jfoenix.controls.JFXButton;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import com.example.demo.util.IconFactory;
import com.example.demo.util.AnimationUtils;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Demo application để test Modern UI Components
 * Run this để xem JFoenix, FontAwesome icons, và animations hoạt động
 */
public class ModernUIDemo extends Application {

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(30);
        root.setPadding(new Insets(40));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #f0f2f5;");

        // Title
        Label title = new Label("🎨 Modern UI Components Demo");
        title.setStyle(
                "-fx-font-size: 24px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: #667eea; " +
                        "-fx-letter-spacing: 1px;");
        AnimationUtils.slideInUp(title);

        // Subtitle
        Label subtitle = new Label("JFoenix + FontAwesome + AnimateFX");
        subtitle.setStyle(
                "-fx-font-size: 14px; " +
                        "-fx-text-fill: #888; " +
                        "-fx-letter-spacing: 0.5px;");

        // Modern Input Example
        ModernInputExample inputExample = new ModernInputExample();

        // Demo buttons
        VBox buttonDemo = createButtonDemo();

        root.getChildren().addAll(title, subtitle, inputExample, buttonDemo);

        Scene scene = new Scene(root, 600, 500);

        // Load stylesheet
        String cssPath = getClass().getResource("/styles.css").toExternalForm();
        scene.getStylesheets().add(cssPath);

        primaryStage.setTitle("Modern UI Demo - JFoenix & FontAwesome");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createButtonDemo() {
        VBox container = new VBox(15);
        container.setAlignment(Pos.CENTER);
        container.getStyleClass().add("card");
        container.setPadding(new Insets(20));
        container.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 12; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2);");

        Label demoTitle = new Label("Button Styles Demo");
        demoTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Primary button
        JFXButton primaryBtn = new JFXButton("PRIMARY BUTTON");
        primaryBtn.setGraphic(IconFactory.createWhiteIcon(FontAwesomeIcon.CHECK));
        primaryBtn.getStyleClass().add("jfx-button");
        primaryBtn.setOnAction(e -> AnimationUtils.pulse(primaryBtn));

        // Secondary button với different icon
        JFXButton secondaryBtn = new JFXButton("SETTINGS");
        secondaryBtn.setGraphic(IconFactory.createPrimaryIcon(FontAwesomeIcon.COG));
        secondaryBtn.getStyleClass().add("file-button");
        secondaryBtn.setOnAction(e -> AnimationUtils.bounce(secondaryBtn));

        // FAB Demo
        JFXButton fabBtn = new JFXButton();
        fabBtn.setGraphic(IconFactory.createIcon(FontAwesomeIcon.PLUS, "24", "white"));
        fabBtn.getStyleClass().add("fab");
        fabBtn.setOnAction(e -> AnimationUtils.rotateIn(fabBtn));

        container.getChildren().addAll(
                demoTitle,
                primaryBtn,
                secondaryBtn,
                new Label("Floating Action Button:"),
                fabBtn);

        AnimationUtils.slideInUp(container);

        return container;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
