import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const WS_URL = process.env.REACT_APP_WS_URL || 'http://localhost:8080/ws';

let stompClient = null;
let reconnectAttempts = 0;
const MAX_RECONNECT = 5;

export const connect = (userId, onNotification, onUnreadCount) => {
  stompClient = new Client({
    webSocketFactory: () => new SockJS(WS_URL),
    reconnectDelay: 3000,

    onConnect: () => {
      reconnectAttempts = 0;
      console.log('WebSocket connected');

      // Subscribe to broadcast notifications
      stompClient.subscribe('/topic/notifications', (message) => {
        const notification = JSON.parse(message.body);
        onNotification(notification);
      });

      // Subscribe to user-specific notifications
      if (userId) {
        stompClient.subscribe(`/user/${userId}/queue/notifications`, (message) => {
          const notification = JSON.parse(message.body);
          onNotification(notification);
        });

        // Subscribe to live unread count badge
        stompClient.subscribe(`/user/${userId}/queue/unread-count`, (message) => {
          const data = JSON.parse(message.body);
          onUnreadCount(data.count);
        });
      }
    },

    onDisconnect: () => {
      console.log('WebSocket disconnected');
    },

    onStompError: (frame) => {
      console.error('STOMP error', frame);
      reconnectAttempts++;
      if (reconnectAttempts >= MAX_RECONNECT) {
        console.error('Max reconnect attempts reached');
      }
    },
  });

  stompClient.activate();
};

export const disconnect = () => {
  if (stompClient) {
    stompClient.deactivate();
    stompClient = null;
  }
};

export const sendNotification = (notificationRequest) => {
  if (stompClient && stompClient.connected) {
    stompClient.publish({
      destination: '/app/notification.send',
      body: JSON.stringify(notificationRequest),
    });
  }
};

export const isConnected = () => stompClient?.connected || false;
