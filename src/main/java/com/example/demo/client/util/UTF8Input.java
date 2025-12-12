package com.example.demo.client.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 🔤 UTF8Input - Handle UTF-8 input reading from console on Windows
 * 
 * This utility ensures proper UTF-8 character handling on Windows consoles
 * where System.in may not be configured for UTF-8 by default.
 */
public class UTF8Input {

    private static BufferedReader reader;

    static {
        // Create a buffered reader with explicit UTF-8 charset
        // This wraps System.in to ensure UTF-8 interpretation on all platforms
        reader = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8),
                8192);
    }

    /**
     * Read a line from console with UTF-8 encoding
     */
    public static String readLine() throws Exception {
        String line = reader.readLine();
        return line != null ? line : "";
    }

    /**
     * Read a line with prompt
     */
    public static String readLine(String prompt) throws Exception {
        System.out.print(prompt);
        System.out.flush();
        return readLine();
    }
}
