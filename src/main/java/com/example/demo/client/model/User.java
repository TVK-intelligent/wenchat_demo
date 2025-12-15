package com.example.demo.client.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 👤 User model for CLI client
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    private Long id;
    private String username;
    private String displayName;
    private String email;
    private Status status;
    private String avatarUrl;
    private Boolean showOnlineStatus;
    private Boolean isOnline;
    private LocalDateTime createdAt;

    public enum Status {
        ONLINE, IDLE, OFFLINE
    }

    @Override
    public String toString() {
        String statusIcon = status == Status.ONLINE ? "🟢" : status == Status.IDLE ? "🟡" : "🔴";
        return String.format("%s %s (%s)", statusIcon, displayName != null ? displayName : username, status);
    }
}
