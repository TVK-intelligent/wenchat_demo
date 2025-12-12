package com.example.demo.client.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 🎯 CommandParser - Parse user input into commands
 */
public class CommandParser {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Command {
        private CommandType type;
        private List<String> args;
        private String rawInput;

        @Override
        public String toString() {
            return type + " " + args;
        }
    }

    public enum CommandType {
        HELP,
        LOGIN,
        LOGOUT,
        ROOMS,
        JOIN,
        LEAVE,
        SEND,
        USERS,
        PRIVATE,
        RECALL,
        TYPING,
        STATUS,
        CLEAR,
        QUIT,
        UNKNOWN
    }

    /**
     * 📝 Parse user input
     */
    public static Command parse(String input) {
        input = input.trim();

        if (input.isEmpty()) {
            return Command.builder()
                    .type(CommandType.UNKNOWN)
                    .args(new ArrayList<>())
                    .rawInput(input)
                    .build();
        }

        // Split by spaces but respect quoted strings
        List<String> tokens = tokenize(input);

        if (tokens.isEmpty()) {
            return Command.builder()
                    .type(CommandType.UNKNOWN)
                    .args(new ArrayList<>())
                    .rawInput(input)
                    .build();
        }

        String cmd = tokens.get(0).toLowerCase();
        List<String> args = new ArrayList<>(tokens.subList(1, tokens.size()));

        CommandType type;
        switch (cmd) {
            case "/help":
                type = CommandType.HELP;
                break;
            case "/login":
                type = CommandType.LOGIN;
                break;
            case "/logout":
                type = CommandType.LOGOUT;
                break;
            case "/rooms":
                type = CommandType.ROOMS;
                break;
            case "/join":
                type = CommandType.JOIN;
                break;
            case "/leave":
                type = CommandType.LEAVE;
                break;
            case "/send":
                type = CommandType.SEND;
                break;
            case "/users":
                type = CommandType.USERS;
                break;
            case "/private":
                type = CommandType.PRIVATE;
                break;
            case "/recall":
                type = CommandType.RECALL;
                break;
            case "/typing":
                type = CommandType.TYPING;
                break;
            case "/status":
                type = CommandType.STATUS;
                break;
            case "/clear":
                type = CommandType.CLEAR;
                break;
            case "/quit":
                type = CommandType.QUIT;
                break;
            default:
                // If doesn't start with /, treat as regular message
                if (!cmd.startsWith("/")) {
                    type = CommandType.SEND;
                    args = new ArrayList<>();
                    args.add(input); // Whole input is the message
                } else {
                    type = CommandType.UNKNOWN;
                }
        }

        return Command.builder()
                .type(type)
                .args(args)
                .rawInput(input)
                .build();
    }

    /**
     * 🔪 Tokenize input, respecting quoted strings
     */
    private static List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            tokens.add(current.toString());
        }

        return tokens;
    }

    /**
     * ✅ Validate command syntax
     */
    public static boolean validate(Command cmd) {
        switch (cmd.getType()) {
            case LOGIN:
                return cmd.getArgs().size() >= 2; // username password
            case JOIN:
                return cmd.getArgs().size() >= 1; // roomId
            case SEND:
                return cmd.getArgs().size() >= 1; // message
            case PRIVATE:
                return cmd.getArgs().size() >= 2; // userId message
            case RECALL:
                return cmd.getArgs().size() >= 1; // messageId
            case TYPING:
                return cmd.getArgs().size() >= 1; // start|stop
            case STATUS:
                return cmd.getArgs().size() >= 1; // online|idle|offline
            case UNKNOWN:
                return false;
            default:
                return true;
        }
    }

    /**
     * 📖 Get help for specific command
     */
    public static String getCommandHelp(CommandType type) {
        switch (type) {
            case LOGIN:
                return "Usage: /login <username> <password>\nExample: /login john mypassword";
            case ROOMS:
                return "Usage: /rooms\nShows all available rooms";
            case JOIN:
                return "Usage: /join <roomId>\nExample: /join 1";
            case SEND:
                return "Usage: /send \"<message>\"\nExample: /send \"Hello everyone\"";
            case USERS:
                return "Usage: /users\nShows all online users";
            case PRIVATE:
                return "Usage: /private <userId> \"<message>\"\nExample: /private 5 \"Hi there\"";
            case RECALL:
                return "Usage: /recall <messageId>\nRecalls your message";
            case TYPING:
                return "Usage: /typing start|stop\nShows typing indicator";
            case STATUS:
                return "Usage: /status online|idle|offline\nChanges your status";
            case HELP:
                return "Usage: /help\nShows help information";
            case CLEAR:
                return "Usage: /clear\nClears the terminal";
            case QUIT:
                return "Usage: /quit\nExits the chat";
            default:
                return "Unknown command";
        }
    }
}
