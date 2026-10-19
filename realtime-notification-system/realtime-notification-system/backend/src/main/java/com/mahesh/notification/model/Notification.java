package com.mahesh.notification.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Notification entity persisted to PostgreSQL.
 * Each notification belongs to a user and carries a type, payload, and read status.
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_user_id", columnList = "userId"),
        @Index(name = "idx_created_at", columnList = "createdAt"),
        @Index(name = "idx_is_read", columnList = "isRead")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    // Optional JSON payload for rich notification data
    @Column(columnDefinition = "TEXT")
    private String payload;

    @Builder.Default
    private boolean isRead = false;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime readAt;

    public enum NotificationType {
        SYSTEM,       // System-wide broadcast
        USER,         // User-specific targeted notification
        ALERT,        // High-priority alert
        INFO,         // Informational message
        SUCCESS,      // Success confirmation
        WARNING       // Warning notification
    }
}
