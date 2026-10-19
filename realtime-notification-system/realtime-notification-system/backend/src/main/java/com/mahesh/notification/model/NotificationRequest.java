package com.mahesh.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for incoming notification publish requests via REST API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private String userId;          // null = broadcast to all users
    private Notification.NotificationType type;
    private String title;
    private String message;
    private String payload;         // Optional JSON string
}
