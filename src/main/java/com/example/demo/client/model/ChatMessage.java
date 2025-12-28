package com.example.demo.client.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 📨 ChatMessage DTO for WebSocket communication
 * Used for sending/receiving messages in chat rooms
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {
    private Long id;
    private Long roomId;
    private Long senderId;
    private String senderUsername;
    private String senderDisplayName;
    private String content;
    private String fileName;
    private MessageType messageType;
    private LocalDateTime timestamp;
    private boolean recalled;
    private Long recipientId;
    private boolean read; // Track if message is read by current user

    public enum MessageType {
        TEXT, IMAGE, FILE, TYPING, NOTIFICATION, SYSTEM, VOICE
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s",
                timestamp != null ? timestamp.toString().substring(11, 19) : "??:??:??",
                senderDisplayName != null ? senderDisplayName : senderUsername,
                content);
    }
}
