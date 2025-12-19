package com.example.demo.client.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 🔙 RecallResponse DTO for WebSocket recall notifications
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecallResponse {
    private Long messageId;
    private Long roomId;
    private Long senderId;
    private String senderUsername;
    private String content;
    private boolean recalled;
    private boolean success;
}
