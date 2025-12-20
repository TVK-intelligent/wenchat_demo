package com.example.demo.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 🏠 RoomInviteNotification - Notification for room invitation events
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoomInviteNotification {
    @JsonProperty("inviteId")
    private Long id;
    private Long roomId;
    private String roomName;
    private Long inviterId;
    private String inviterUsername;
    private String inviterDisplayName;
    private LocalDateTime timestamp;

    public String getInviterDisplayName() {
        return inviterDisplayName != null && !inviterDisplayName.isEmpty()
                ? inviterDisplayName
                : inviterUsername;
    }
}
