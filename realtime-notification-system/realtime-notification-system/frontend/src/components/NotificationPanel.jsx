import React from 'react';
import './NotificationPanel.css';

const TYPE_COLORS = {
  SYSTEM: '#6c757d',
  USER: '#0d6efd',
  ALERT: '#dc3545',
  INFO: '#0dcaf0',
  SUCCESS: '#198754',
  WARNING: '#ffc107',
};

const TYPE_ICONS = {
  SYSTEM: '⚙️',
  USER: '👤',
  ALERT: '🚨',
  INFO: 'ℹ️',
  SUCCESS: '✅',
  WARNING: '⚠️',
};

const formatTime = (dateString) => {
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now - date;
  const diffMin = Math.floor(diffMs / 60000);

  if (diffMin < 1) return 'Just now';
  if (diffMin < 60) return `${diffMin}m ago`;
  const diffHr = Math.floor(diffMin / 60);
  if (diffHr < 24) return `${diffHr}h ago`;
  return date.toLocaleDateString();
};

const NotificationPanel = ({ notifications, onMarkAsRead, onMarkAllAsRead, onClose }) => {
  const unreadCount = notifications.filter((n) => !n.isRead).length;

  return (
    <div className="notification-panel">
      <div className="panel-header">
        <h3>Notifications</h3>
        <div className="panel-actions">
          {unreadCount > 0 && (
            <button className="mark-all-btn" onClick={onMarkAllAsRead}>
              Mark all read
            </button>
          )}
          <button className="close-btn" onClick={onClose}>✕</button>
        </div>
      </div>

      <div className="notification-list">
        {notifications.length === 0 ? (
          <div className="empty-state">
            <span>🔕</span>
            <p>No notifications yet</p>
          </div>
        ) : (
          notifications.map((notification) => (
            <div
              key={notification.id}
              className={`notification-item ${!notification.isRead ? 'unread' : ''}`}
              onClick={() => !notification.isRead && onMarkAsRead(notification.id)}
            >
              <div
                className="type-indicator"
                style={{ backgroundColor: TYPE_COLORS[notification.type] || '#6c757d' }}
              >
                {TYPE_ICONS[notification.type] || '📢'}
              </div>
              <div className="notification-content">
                <div className="notification-title">{notification.title}</div>
                <div className="notification-message">{notification.message}</div>
                <div className="notification-time">{formatTime(notification.createdAt)}</div>
              </div>
              {!notification.isRead && <div className="unread-dot" />}
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default NotificationPanel;
