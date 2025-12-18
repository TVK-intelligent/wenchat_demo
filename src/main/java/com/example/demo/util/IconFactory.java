package com.example.demo.util;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.scene.paint.Color;

/**
 * Factory class để tạo và quản lý FontAwesome icons
 */
public class IconFactory {

    private static final String DEFAULT_ICON_SIZE = "18";
    private static final String LARGE_ICON_SIZE = "24";
    private static final String SMALL_ICON_SIZE = "14";

    /**
     * Tạo icon với kích thước mặc định
     */
    public static FontAwesomeIconView createIcon(FontAwesomeIcon icon) {
        return createIcon(icon, DEFAULT_ICON_SIZE, null);
    }

    /**
     * Tạo icon với màu sắc tùy chỉnh
     */
    public static FontAwesomeIconView createIcon(FontAwesomeIcon icon, String color) {
        return createIcon(icon, DEFAULT_ICON_SIZE, color);
    }

    /**
     * Tạo icon với kích thước và màu tùy chỉnh
     */
    public static FontAwesomeIconView createIcon(FontAwesomeIcon icon, String size, String color) {
        FontAwesomeIconView iconView = new FontAwesomeIconView(icon);
        iconView.setGlyphSize(Integer.parseInt(size));

        if (color != null) {
            iconView.setFill(Color.web(color));
        }

        iconView.getStyleClass().add("icon");
        return iconView;
    }

    /**
     * Tạo icon lớn
     */
    public static FontAwesomeIconView createLargeIcon(FontAwesomeIcon icon) {
        return createIcon(icon, LARGE_ICON_SIZE, null);
    }

    /**
     * Tạo icon nhỏ
     */
    public static FontAwesomeIconView createSmallIcon(FontAwesomeIcon icon) {
        return createIcon(icon, SMALL_ICON_SIZE, null);
    }

    /**
     * Tạo white icon (cho sidebar)
     */
    public static FontAwesomeIconView createWhiteIcon(FontAwesomeIcon icon) {
        return createIcon(icon, DEFAULT_ICON_SIZE, "#FFFFFF");
    }

    /**
     * Tạo primary color icon
     */
    public static FontAwesomeIconView createPrimaryIcon(FontAwesomeIcon icon) {
        return createIcon(icon, DEFAULT_ICON_SIZE, "#667eea");
    }
}
