package com.example.demo.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 💬 ChatRoom model for CLI client
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatRoom {
    private Long id;
    private String name;
    private String description;
    private Long ownerId;
    private boolean isPrivate;
    private int memberCount;

    @Override
    public String toString() {
        String icon = isPrivate ? "🔒" : "🌐";
        return String.format("%s [%d] %s (%d members) - %s",
                icon, id, name, memberCount, description);
    }
}
