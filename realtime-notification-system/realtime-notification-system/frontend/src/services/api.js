const BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

const headers = { 'Content-Type': 'application/json' };

export const api = {
  // Publish a new notification (admin/service use)
  publish: async (request) => {
    const res = await fetch(`${BASE_URL}/notifications/publish`, {
      method: 'POST',
      headers,
      body: JSON.stringify(request),
    });
    return res.json();
  },

  // Get paginated notifications for a user
  getNotifications: async (userId, page = 0, size = 20) => {
    const res = await fetch(`${BASE_URL}/notifications/${userId}?page=${page}&size=${size}`);
    return res.json();
  },

  // Get unread count for badge
  getUnreadCount: async (userId) => {
    const res = await fetch(`${BASE_URL}/notifications/${userId}/unread-count`);
    return res.json();
  },

  // Mark a single notification as read
  markAsRead: async (notificationId, userId) => {
    await fetch(`${BASE_URL}/notifications/${notificationId}/read?userId=${userId}`, {
      method: 'PUT',
    });
  },

  // Mark all notifications as read
  markAllAsRead: async (userId) => {
    await fetch(`${BASE_URL}/notifications/${userId}/read-all`, {
      method: 'PUT',
    });
  },
};
