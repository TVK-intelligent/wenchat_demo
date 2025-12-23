package com.example.demo.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

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

    /**
     * Parse ownerId from nested owner object in API response
     * API returns: { "owner": { "id": 1, ... } }
     */
    @JsonSetter("owner")
    public void setOwnerFromMap(Map<String, Object> owner) {
        if (owner != null && owner.get("id") != null) {
            this.ownerId = ((Number) owner.get("id")).longValue();
        }
    }

    @Override
    public String toString() {
        String icon = isPrivate ? "🔒" : "🌐";
        return String.format("%s [%d] %s (%d members) - %s",
                icon, id, name, memberCount, description);
    }
}
