package com.example.demo.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 👋 FriendRequestNotification - Notification for friend request events
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class FriendRequestNotification {
    private Long id;
    private Long friendRequestId;

    // Backend sends these fields
    private Long fromUserId;
    private String fromUserUsername;
    private String fromUserDisplayName;
    private String fromUserAvatar;
    private Long toUserId;
    private String status;
    private String eventType;

    // Legacy fields for backward compatibility
    private Long senderId;
    private String senderUsername;
    private String senderDisplayName;
    private String senderAvatarUrl;
    private LocalDateTime timestamp;

    public String getDisplayName() {
        // Prefer fromUserDisplayName from backend
        if (fromUserDisplayName != null && !fromUserDisplayName.isEmpty()) {
            return fromUserDisplayName;
        }
        if (senderDisplayName != null && !senderDisplayName.isEmpty()) {
            return senderDisplayName;
        }
        // Fallback to username
        if (fromUserUsername != null && !fromUserUsername.isEmpty()) {
            return fromUserUsername;
        }
        return senderUsername;
    }

    public String getSenderUsername() {
        return fromUserUsername != null ? fromUserUsername : senderUsername;
    }
}
