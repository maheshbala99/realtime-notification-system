package com.mahesh.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mahesh.notification.model.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * Listens to Redis pub/sub channels and forwards notifications to WebSocket clients.
 * This subscriber is the bridge between Redis (inter-service messaging) and
 * WebSocket (client push delivery).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisMessageSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody());
            Notification notification = objectMapper.readValue(body, Notification.class);
            notificationService.deliver(notification);
        } catch (Exception e) {
            log.error("Failed to process Redis message: {}", new String(message.getBody()), e);
        }
    }
}
