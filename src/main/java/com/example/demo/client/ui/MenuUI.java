package com.example.demo.client.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * 📋 MenuUI - Interactive menu system for CLI
 */
public class MenuUI {

    /**
     * Main menu
     */
    public static int showMainMenu() {
        TerminalUI.clearScreen();
        TerminalUI.println(
                TerminalUI.BRIGHT_CYAN + "+------ WebChat Group 10 - Main Menu ------+" + TerminalUI.RESET);
        TerminalUI.println("");

        TerminalUI.println(TerminalUI.GREEN + "  1. Login" + TerminalUI.RESET);
        TerminalUI.println(TerminalUI.GREEN + "  2. View Help" + TerminalUI.RESET);
        TerminalUI.println(TerminalUI.GREEN + "  3. Exit" + TerminalUI.RESET);
        TerminalUI.println("");

        return getMenuChoice(1, 3);
    }

    /**
     * Login menu
     */
    public static LoginInfo showLoginMenu() {
        TerminalUI.clearScreen();
        TerminalUI.println(
                TerminalUI.BRIGHT_CYAN + "+--------- LOGIN ---------+" + TerminalUI.RESET);
        TerminalUI.println("");

        String username = TerminalUI.getInput("Username: ");
        String password = TerminalUI.getPassword("Password: ");

        return new LoginInfo(username, password);
    }

    /**
     * Chat menu (after login)
     */
    public static int showChatMenu() {
        TerminalUI.println("");
        TerminalUI.println(
                TerminalUI.BRIGHT_CYAN + "+-- MAIN MENU --+" + TerminalUI.RESET);
        TerminalUI.println("");

        TerminalUI.println(TerminalUI.GREEN + "  1. View Rooms" + TerminalUI.RESET);
        TerminalUI.println(TerminalUI.GREEN + "  2. Join Room" + TerminalUI.RESET);
        TerminalUI.println(TerminalUI.GREEN + "  3. Create Room" + TerminalUI.RESET);
        TerminalUI.println(TerminalUI.GREEN + "  4. View Friends" + TerminalUI.RESET);
        TerminalUI.println(TerminalUI.GREEN + "  5. Send Private Message" + TerminalUI.RESET);
        TerminalUI.println(TerminalUI.GREEN + "  6. Search & Add Friends" + TerminalUI.RESET);
        TerminalUI.println(TerminalUI.GREEN + "  7. View Friend Requests" + TerminalUI.RESET);
        TerminalUI.println(TerminalUI.GREEN + "  8. Exit Chat" + TerminalUI.RESET);
        TerminalUI.println("");

        return getMenuChoice(1, 8);
    }

    /**
     * Room menu (in-room chat) - WhatsApp style
     */
    public static int showRoomMenu(String roomName) {
        TerminalUI.println("");
        TerminalUI.println(TerminalUI.GREEN + "[1] Send  [2] Users  [3] Invite  [4] Leave" + TerminalUI.RESET);
        TerminalUI.println("");
        return getMenuChoice(1, 4);
    }

    /**
     * Show room header only (called once when entering room)
     */
    public static void showRoomHeader(String roomName) {
        TerminalUI.println("");
        TerminalUI.println(
                TerminalUI.BRIGHT_CYAN + "+========================================+" + TerminalUI.RESET);
        TerminalUI.println(
                TerminalUI.BRIGHT_CYAN + "| Room: " + padStringLeft(roomName, 32) + " |" + TerminalUI.RESET);
        TerminalUI.println(
                TerminalUI.BRIGHT_CYAN + "+========================================+" + TerminalUI.RESET);
        TerminalUI.println("");
    }

    /**
     * Pad string to left (no centering)
     */
    private static String padStringLeft(String str, int length) {
        if (str == null || str.isEmpty()) {
            return " ".repeat(length);
        }
        if (str.length() >= length) {
            return str.substring(0, Math.min(str.length(), length));
        }
        return str + " ".repeat(length - str.length());
    }

    /**
     * 📋 Show list with selection
     */
    public static int showListMenu(String title, List<String> items) {
        TerminalUI.clearScreen();
        TerminalUI.println(
                TerminalUI.BRIGHT_CYAN + "╔════════════════════════════════════════════════════╗" + TerminalUI.RESET);
        TerminalUI.println(TerminalUI.BRIGHT_CYAN + "║  " + padString(title, 48) + "║" + TerminalUI.RESET);
        TerminalUI.println(
                TerminalUI.BRIGHT_CYAN + "╚════════════════════════════════════════════════════╝" + TerminalUI.RESET);
        TerminalUI.println("");

        for (int i = 0; i < items.size(); i++) {
            TerminalUI.println(TerminalUI.GREEN + (i + 1) + ". " + items.get(i) + TerminalUI.RESET);
        }
        TerminalUI.println(TerminalUI.GREEN + "0. Back" + TerminalUI.RESET);
        TerminalUI.println("");

        return getMenuChoice(0, items.size());
    }

    /**
     * 🎯 Get menu choice from user
     */
    private static int getMenuChoice(int min, int max) {
        while (true) {
            try {
                String input = TerminalUI.getInput(
                        TerminalUI.BRIGHT_CYAN + "Choose option [" + min + "-" + max + "]: " + TerminalUI.RESET);

                // Handle empty input - silently re-prompt without error message
                if (input.isEmpty()) {
                    continue;
                }

                int choice = Integer.parseInt(input);

                if (choice >= min && choice <= max) {
                    return choice;
                } else {
                    TerminalUI.printError("Invalid choice. Please enter " + min + " to " + max);
                }
            } catch (NumberFormatException e) {
                TerminalUI.printError("Please enter a valid number");
            }
        }
    }

    /**
     * 📝 Pad string to specific length
     */
    private static String padString(String str, int length) {
        if (str.length() >= length) {
            return str.substring(0, length);
        }
        int padding = length - str.length();
        int leftPad = padding / 2;
        int rightPad = padding - leftPad;
        return " ".repeat(leftPad) + str + " ".repeat(rightPad);
    }

    /**
     * 📤 Show confirmation dialog
     */
    public static boolean showConfirmDialog(String message) {
        TerminalUI.println("");
        String response = TerminalUI.getInput(message + " (Y/N): ");
        return response.equalsIgnoreCase("Y") || response.equalsIgnoreCase("YES");
    }

    /**
     * 📥 Show input dialog
     */
    public static String showInputDialog(String prompt) {
        TerminalUI.println("");
        return TerminalUI.getInput(prompt + ": ");
    }

    /**
     * 📊 Show status
     */
    public static void showStatus(String message) {
        TerminalUI.println(TerminalUI.BRIGHT_YELLOW + "─".repeat(50) + TerminalUI.RESET);
        TerminalUI.println(TerminalUI.BRIGHT_YELLOW + "Status: " + message + TerminalUI.RESET);
        TerminalUI.println(TerminalUI.BRIGHT_YELLOW + "─".repeat(50) + TerminalUI.RESET);
    }

    /**
     * 🔄 Wait for user to continue
     */
    public static void waitForContinue() {
        try {
            TerminalUI.print(TerminalUI.GRAY + "Press Enter to continue..." + TerminalUI.RESET);
            // Clear entire input buffer to consume any pending input
            while (System.in.available() > 0) {
                System.in.read();
            }
            System.in.read(); // Wait for new Enter
            // Clear again to ensure no leftover input
            while (System.in.available() > 0) {
                System.in.read();
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Show quick message input (inline) - WhatsApp style
     */
    public static String showQuickMessageInput() {
        return TerminalUI.getInputSilent();
    }

    /**
     * 📋 Login Info DTO
     */
    public static class LoginInfo {
        public String username;
        public String password;

        public LoginInfo(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }
}
