import React, { useState, useEffect, useRef } from 'react';
import NotificationPanel from './NotificationPanel';
import { api } from '../services/api';
import { connect, disconnect } from '../services/websocket';
import './NotificationBell.css';

/**
 * NotificationBell — top-level notification component.
 *
 * Usage:
 *   <NotificationBell userId="user-123" />
 *
 * - Connects to WebSocket on mount, disconnects on unmount
 * - Shows unread badge count that updates in real time
 * - Opens/closes notification panel on click
 */
const NotificationBell = ({ userId }) => {
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [isOpen, setIsOpen] = useState(false);
  const [connected, setConnected] = useState(false);
  const panelRef = useRef(null);

  useEffect(() => {
    if (!userId) return;

    // Load initial notifications and unread count
    api.getNotifications(userId).then((page) => {
      setNotifications(page.content || []);
    });
    api.getUnreadCount(userId).then((data) => {
      setUnreadCount(data.count || 0);
    });

    // Connect WebSocket
    connect(
      userId,
      (newNotification) => {
        setNotifications((prev) => [newNotification, ...prev]);
        if (!newNotification.isRead) {
          setUnreadCount((c) => c + 1);
          // Browser notification if tab not focused
          if (document.hidden && Notification.permission === 'granted') {
            new Notification(newNotification.title, { body: newNotification.message });
          }
        }
      },
      (count) => setUnreadCount(count)
    );
    setConnected(true);

    // Close panel on outside click
    const handleOutsideClick = (e) => {
      if (panelRef.current && !panelRef.current.contains(e.target)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleOutsideClick);

    return () => {
      disconnect();
      document.removeEventListener('mousedown', handleOutsideClick);
    };
  }, [userId]);

  const handleMarkAsRead = async (id) => {
    await api.markAsRead(id, userId);
    setNotifications((prev) =>
      prev.map((n) => (n.id === id ? { ...n, isRead: true } : n))
    );
  };

  const handleMarkAllAsRead = async () => {
    await api.markAllAsRead(userId);
    setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
    setUnreadCount(0);
  };

  return (
    <div className="notification-bell-container" ref={panelRef}>
      <button
        className={`bell-button ${connected ? 'connected' : ''}`}
        onClick={() => setIsOpen(!isOpen)}
        aria-label={`Notifications - ${unreadCount} unread`}
      >
        <span className="bell-icon">🔔</span>
        {unreadCount > 0 && (
          <span className="badge">{unreadCount > 99 ? '99+' : unreadCount}</span>
        )}
      </button>

      {isOpen && (
        <NotificationPanel
          notifications={notifications}
          onMarkAsRead={handleMarkAsRead}
          onMarkAllAsRead={handleMarkAllAsRead}
          onClose={() => setIsOpen(false)}
        />
      )}
    </div>
  );
};

export default NotificationBell;
