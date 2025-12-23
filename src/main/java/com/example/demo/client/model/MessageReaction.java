package com.example.demo.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 😀 MessageReaction model for client
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageReaction {
    private Long id;
    private Long messageId;
    private Long userId;
    private String username;
    private String displayName;
    private String emoji;
    private String createdAt;
}
