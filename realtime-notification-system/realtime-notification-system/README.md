# Real-Time Notification System

A production-grade real-time notification platform built with **Spring Boot**, **WebSocket (STOMP)**, **Redis pub/sub**, **PostgreSQL**, and **React.js**.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Client (React)                           │
│    NotificationBell → WebSocket (STOMP/SockJS) connection       │
└────────────────────────────┬────────────────────────────────────┘
                             │ WebSocket /ws
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Spring Boot Backend                           │
│                                                                  │
│  REST API ──► NotificationService ──► Redis pub/sub publish     │
│                       │                       │                  │
│               PostgreSQL persist      Redis subscriber          │
│               (audit/history)         │                          │
│                               SimpMessagingTemplate             │
│                               │                                  │
│                        WebSocket push ──► Client                │
└─────────────────────────────────────────────────────────────────┘
         │                          │
    PostgreSQL                   Redis
  (persistence)              (pub/sub + cache)
```

**Why Redis pub/sub?** When multiple backend instances run behind a load balancer, a notification triggered on instance A needs to reach a WebSocket client connected to instance B. Redis acts as the message bus between all instances.

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.2, Spring WebSocket (STOMP) |
| Messaging | Redis pub/sub (Spring Data Redis) |
| Database | PostgreSQL (JPA/Hibernate) |
| Frontend | React 18, @stomp/stompjs, SockJS |
| DevOps | Docker, Docker Compose, Nginx |

## Features

- **Real-time delivery** — WebSocket push with STOMP protocol
- **Redis pub/sub fan-out** — horizontal scaling support across multiple backend instances
- **Notification types** — SYSTEM, USER, ALERT, INFO, SUCCESS, WARNING
- **Targeted vs broadcast** — per-user notifications or system-wide broadcasts
- **Persistent history** — PostgreSQL storage with pagination
- **Live unread badge** — real-time count updates without polling
- **Mark as read** — individual and bulk mark-read with WebSocket confirmation
- **SockJS fallback** — graceful degradation for environments where WebSocket is blocked
- **Docker Compose** — one-command local setup

## Getting Started

### Prerequisites
- Java 17, Maven 3.9+
- Node.js 18+
- Docker + Docker Compose

### Run with Docker (recommended)

```bash
git clone https://github.com/balamahesh/realtime-notification-system
cd realtime-notification-system
docker-compose up --build
```

- Frontend: http://localhost:3000
- Backend API: http://localhost:8080
- H2 Console (local): http://localhost:8080/h2-console

### Run locally without Docker

**Backend:**
```bash
# Start Redis locally first
docker run -d -p 6379:6379 redis:7-alpine

cd backend
mvn spring-boot:run
```

**Frontend:**
```bash
cd frontend
npm install
npm start
```

## API Reference

### Publish a Notification
```http
POST /api/notifications/publish
Content-Type: application/json

{
  "userId": "user-123",        // null = broadcast to all
  "type": "ALERT",
  "title": "New order received",
  "message": "Order #4521 requires review",
  "payload": "{\"orderId\": \"4521\"}"
}
```

### Get Notifications (paginated)
```http
GET /api/notifications/{userId}?page=0&size=20
```

### Mark as Read
```http
PUT /api/notifications/{notificationId}/read?userId={userId}
PUT /api/notifications/{userId}/read-all
```

### WebSocket Integration (React)
```js
import { connect, disconnect } from './services/websocket';

// Connect and subscribe
connect(userId, (notification) => {
  console.log('New notification:', notification);
}, (unreadCount) => {
  setBadgeCount(unreadCount);
});

// Cleanup
disconnect();
```

## Project Structure

```
realtime-notification-system/
├── backend/
│   ├── src/main/java/com/mahesh/notification/
│   │   ├── config/          # WebSocket, Redis, Security config
│   │   ├── controller/      # REST + WebSocket (STOMP) handlers
│   │   ├── model/           # Notification entity + DTOs
│   │   ├── repository/      # JPA repository
│   │   └── service/         # Business logic + Redis subscriber
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── components/      # NotificationBell, NotificationPanel
│   │   └── services/        # WebSocket client, REST API client
│   ├── Dockerfile
│   ├── nginx.conf
│   └── package.json
└── docker-compose.yml
```

## Key Design Decisions

**Redis pub/sub over in-memory broker** — The Spring in-memory broker works for a single instance. Redis enables the system to scale horizontally; all backend instances subscribe to the same channels and any instance can push to any connected WebSocket client.

**STOMP over raw WebSocket** — STOMP adds routing (destinations/topics), message headers, and subscription management on top of the raw WebSocket protocol. This maps cleanly to Spring's `@MessageMapping` and `SimpMessagingTemplate`.

**SockJS fallback** — Certain enterprise networks and proxies block WebSocket upgrades. SockJS transparently falls back to long-polling, keeping the application functional.

**PostgreSQL persistence** — Notifications are stored before Redis publish, ensuring no notification is lost if a Redis call fails. The database also serves the notification history panel.
