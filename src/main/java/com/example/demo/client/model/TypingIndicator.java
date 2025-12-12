package com.example.demo.client.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ⌨️ TypingIndicator - Show when user is typing
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TypingIndicator {
    private Long userId;
    private String username;
    private Long roomId;
    private boolean typing;
    private LocalDateTime timestamp;
}
