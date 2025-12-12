package com.example.demo.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 👥 UserStatusMessage - Real-time user status update
 * Received from server when a user changes online/offline status
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStatusMessage {
    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("username")
    private String username;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("status")
    private String status; // ONLINE or OFFLINE

    @JsonProperty("isOnline")
    private Boolean isOnline;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
}
