package com.example.demo.client.ui;

import java.nio.charset.StandardCharsets;
import com.example.demo.client.util.UTF8Input;

/**
 * 🎨 TerminalUI - Handle terminal display and colors
 */
public class TerminalUI {

    // Static Scanner - reuse for multiple inputs with UTF-8 charset
    private static final java.util.Scanner INPUT_SCANNER = new java.util.Scanner(System.in, StandardCharsets.UTF_8);

    // ANSI Color Codes
    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";

    public static final String CYAN = "\u001B[36m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String RED = "\u001B[31m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String WHITE = "\u001B[37m";
    public static final String GRAY = "\u001B[90m";
    public static final String BRIGHT_CYAN = "\u001B[96m";
    public static final String BRIGHT_GREEN = "\u001B[92m";
    public static final String BRIGHT_YELLOW = "\u001B[93m";
    public static final String BRIGHT_RED = "\u001B[91m";

    // Background Colors
    public static final String BG_DARK = "\u001B[40m";
    public static final String BG_DARK_GRAY = "\u001B[100m";

    private static final int TERMINAL_WIDTH = 80;

    /**
     * Header banner
     */
    public static void drawHeader() {
        clearScreen();
        printLine("═", TERMINAL_WIDTH, BRIGHT_CYAN);
        println(BRIGHT_CYAN + "  ╔═══════ 💬 WENCHAT TERMINAL CLI CLIENT v1.0 ═══════╗" + RESET);
        println(BRIGHT_CYAN + "  ║       🌐 WebSocket Real-time Chat App 🌐         ║" + RESET);
        println(BRIGHT_CYAN + "  ╚═══════════════════════════════════════════════════╝" + RESET);
        printLine("═", TERMINAL_WIDTH, BRIGHT_CYAN);
        println("");
    }

    /**
     * 📊 Draw status bar
     */
    public static void drawStatusBar(String status, String room, int userCount) {
        printLine("─", TERMINAL_WIDTH, GRAY);
        String statusText = String.format("Status: %s | Room: %s | Users: %d", status, room, userCount);
        println(GRAY + statusText + RESET);
        printLine("─", TERMINAL_WIDTH, GRAY);
    }

    /**
     * 💬 Print message to terminal
     */
    public static void printMessage(String message) {
        println(message);
    }

    /**
     * ✅ Print success message
     */
    public static void printSuccess(String message) {
        println(GREEN + "✅ " + message + RESET);
    }

    /**
     * ❌ Print error message
     */
    public static void printError(String message) {
        println(RED + "❌ " + message + RESET);
    }

    /**
     * ⚠️ Print warning message
     */
    public static void printWarning(String message) {
        println(YELLOW + "⚠️  " + message + RESET);
    }

    /**
     * ℹ️ Print info message
     */
    public static void printInfo(String message) {
        println(BRIGHT_CYAN + "ℹ️  " + message + RESET);
    }

    /**
     * 📝 Print normal message with timestamp
     */
    public static void printChatMessage(String timestamp, String sender, String message) {
        String formatted = String.format("%s[%s]%s %s%s:%s %s",
                GRAY, timestamp, RESET,
                BRIGHT_CYAN, sender, RESET,
                message);
        println(formatted);
    }

    /**
     * ⌨️ Print typing indicator
     */
    public static void printTyping(String username) {
        println(YELLOW + "⌨️  " + username + " is typing..." + RESET);
    }

    /**
     * 🔙 Print recall notification
     */
    public static void printRecall(String username, String messageId) {
        println(MAGENTA + "🔙 " + username + " recalled message #" + messageId + RESET);
    }

    /**
     * 👤 Print user list
     */
    public static void printUserList(String[] users) {
        println(BRIGHT_CYAN + "\n📊 Online Users:" + RESET);
        for (String user : users) {
            println("  • " + user);
        }
    }

    /**
     * 💬 Print room list
     */
    public static void printRoomList(String[] rooms) {
        println(BRIGHT_CYAN + "\n📚 Available Rooms:" + RESET);
        for (String room : rooms) {
            println("  • " + room);
        }
    }

    /**
     * 📋 Print help menu
     */
    public static void printHelp() {
        println(BRIGHT_CYAN + "\n╔════════════════════════════════════════════════════╗" + RESET);
        println(BRIGHT_CYAN + "║           AVAILABLE COMMANDS                       ║" + RESET);
        println(BRIGHT_CYAN + "╚════════════════════════════════════════════════════╝" + RESET);

        String[] commands = {
                "/help                    - Show this help menu",
                "/login <username> <pwd>  - Login to chat",
                "/logout                  - Logout",
                "/rooms                   - List all rooms",
                "/join <roomId>           - Join a room",
                "/leave                   - Leave current room",
                "/send \"<message>\"        - Send message to room",
                "/users                   - List online users",
                "/private <userId> \"msg\" - Send private message",
                "/recall <messageId>      - Recall your message",
                "/typing start|stop       - Show typing status",
                "/status online|idle      - Change online status",
                "/clear                   - Clear screen",
                "/quit                    - Exit chat"
        };

        for (String cmd : commands) {
            println(GREEN + cmd + RESET);
        }
        println("");
    }

    /**
     * 🔄 Clear terminal screen
     */
    public static void clearScreen() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            // Fallback: print newlines
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        }
    }

    /**
     * 📏 Print a line of characters
     */
    public static void printLine(String character, int width, String color) {
        StringBuilder line = new StringBuilder(color);
        for (int i = 0; i < width; i++) {
            line.append(character);
        }
        line.append(RESET);
        println(line.toString());
    }

    /**
     * 📤 Print with newline
     */
    public static void println(String message) {
        System.out.println(message);
    }

    /**
     * 📤 Print without newline
     */
    public static void print(String message) {
        System.out.print(message);
    }

    /**
     * 📥 Get input from user
     */
    public static String getInput(String prompt) {
        try {
            return UTF8Input.readLine(prompt).trim();
        } catch (Exception e) {
            // Fallback to scanner if UTF8Input fails
            print(prompt);
            return INPUT_SCANNER.nextLine().trim();
        }
    }

    /**
     * 📥 Get input without showing prompt (for chat messages)
     */
    public static String getInputSilent() {
        try {
            return UTF8Input.readLine("").trim();
        } catch (Exception e) {
            // Fallback to scanner if UTF8Input fails
            return INPUT_SCANNER.nextLine().trim();
        }
    }

    /**
     * 📥 Get password from user (masked)
     */
    public static String getPassword(String prompt) {
        print(prompt);
        return INPUT_SCANNER.nextLine().trim();
    }

    /**
     * 🎁 Draw decorative box with icon
     */
    public static void drawBox(String title, String color) {
        println("");
        println(color + "╔════════════════════════════════════════════════════╗" + RESET);
        String paddedTitle = padEnd(title.substring(0, Math.min(title.length(), 48)), 48);
        println(color + "║  " + paddedTitle + "  ║" + RESET);
        println(color + "╚════════════════════════════════════════════════════╝" + RESET);
        println("");
    }

    /**
     * 🌟 Draw section header with decorations
     */
    public static void drawSection(String title, String color) {
        println("");
        println(color + "▶ " + title + RESET);
        println(color + "─".repeat(TERMINAL_WIDTH - 2) + RESET);
    }

    /**
     * Utility to pad strings
     */
    private static String padEnd(String str, int length) {
        while (str.length() < length) {
            str += " ";
        }
        return str;
    }
}
