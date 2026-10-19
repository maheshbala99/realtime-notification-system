package com.mahesh.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mahesh.notification.config.RedisConfig;
import com.mahesh.notification.model.Notification;
import com.mahesh.notification.model.NotificationRequest;
import com.mahesh.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Core notification service.
 *
 * Flow for publishing a notification:
 *  1. Persist notification to PostgreSQL (audit trail + history)
 *  2. Publish to Redis pub/sub channel (fan-out to all server instances)
 *  3. RedisMessageSubscriber receives and pushes via WebSocket (STOMP)
 *
 * This architecture supports horizontal scaling: multiple app instances
 * all subscribe to Redis, so any instance can receive and deliver to the
 * correct WebSocket connection.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Publish a notification: persist, then fan-out via Redis pub/sub.
     */
    @Transactional
    public Notification publish(NotificationRequest request) {
        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .type(request.getType())
                .title(request.getTitle())
                .message(request.getMessage())
                .payload(request.getPayload())
                .build();

        notification = notificationRepository.save(notification);

        try {
            String channel = (request.getUserId() != null)
                    ? RedisConfig.USER_NOTIFICATION_CHANNEL_PREFIX + request.getUserId()
                    : RedisConfig.NOTIFICATION_CHANNEL;

            redisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(notification));
            log.info("Published notification id={} to channel={}", notification.getId(), channel);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize notification for Redis", e);
        }

        return notification;
    }

    /**
     * Deliver a notification directly over WebSocket (called by RedisMessageSubscriber).
     * User-specific: /user/{userId}/queue/notifications
     * Broadcast: /topic/notifications
     */
    public void deliver(Notification notification) {
        if (notification.getUserId() != null) {
            messagingTemplate.convertAndSendToUser(
                    notification.getUserId(),
                    "/queue/notifications",
                    notification
            );
        } else {
            messagingTemplate.convertAndSend("/topic/notifications", notification);
        }
        log.debug("Delivered notification id={} to userId={}", notification.getId(), notification.getUserId());
    }

    public Page<Notification> getForUser(String userId, int page, int size) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    public Page<Notification> getUnreadForUser(String userId, int page, int size) {
        return notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false, PageRequest.of(page, size));
    }

    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }

    @Transactional
    public void markAsRead(String notificationId, String userId) {
        notificationRepository.markAsRead(notificationId, userId);
        // Push updated unread count over WebSocket
        long unreadCount = getUnreadCount(userId);
        messagingTemplate.convertAndSendToUser(userId, "/queue/unread-count", Map.of("count", unreadCount));
    }

    @Transactional
    public void markAllAsRead(String userId) {
        notificationRepository.markAllAsReadForUser(userId);
        messagingTemplate.convertAndSendToUser(userId, "/queue/unread-count", Map.of("count", 0));
    }
}
