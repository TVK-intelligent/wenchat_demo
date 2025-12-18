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
    private Long senderId;
    private String senderUsername;
    private String senderDisplayName;
    private String senderAvatarUrl;
    private LocalDateTime timestamp;

    public String getDisplayName() {
        return senderDisplayName != null && !senderDisplayName.isEmpty()
                ? senderDisplayName
                : senderUsername;
    }
}
