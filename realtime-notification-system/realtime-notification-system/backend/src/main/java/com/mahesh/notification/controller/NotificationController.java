package com.mahesh.notification.controller;

import com.mahesh.notification.model.Notification;
import com.mahesh.notification.model.NotificationRequest;
import com.mahesh.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

/**
 * REST + WebSocket controller for notification operations.
 *
 * REST endpoints:
 *   POST /api/notifications/publish     - Publish a notification
 *   GET  /api/notifications/{userId}    - Get paginated notification history
 *   GET  /api/notifications/{userId}/unread-count - Get unread count
 *   PUT  /api/notifications/{id}/read  - Mark single notification read
 *   PUT  /api/notifications/{userId}/read-all - Mark all read
 *
 * WebSocket (STOMP):
 *   /app/notification.send    - Client sends a notification via WS
 *   Subscribe /topic/notifications      - Broadcast notifications
 *   Subscribe /user/queue/notifications - User-specific notifications
 *   Subscribe /user/queue/unread-count  - Live unread badge count
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // ── REST API ──────────────────────────────────────────────────────────────

    @PostMapping("/publish")
    public ResponseEntity<Notification> publish(@RequestBody NotificationRequest request) {
        Notification notification = notificationService.publish(request);
        return ResponseEntity.ok(notification);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Page<Notification>> getNotifications(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationService.getForUser(userId, page, size));
    }

    @GetMapping("/{userId}/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable String userId) {
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable String id,
                                           @RequestParam String userId) {
        notificationService.markAsRead(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{userId}/read-all")
    public ResponseEntity<Void> markAllAsRead(@PathVariable String userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }

    // ── WebSocket (STOMP) ─────────────────────────────────────────────────────

    /**
     * Handles notifications sent from a connected WebSocket client.
     * Useful for client-to-client broadcast scenarios.
     */
    @MessageMapping("/notification.send")
    public void sendNotification(@Payload NotificationRequest request, Principal principal) {
        if (principal != null && request.getUserId() == null) {
            request.setUserId(principal.getName());
        }
        notificationService.publish(request);
    }

    /**
     * Immediately sends the unread count when a client subscribes.
     */
    @SubscribeMapping("/user/queue/unread-count")
    public Map<String, Long> onSubscribeUnreadCount(Principal principal) {
        long count = notificationService.getUnreadCount(principal.getName());
        return Map.of("count", count);
    }
}
