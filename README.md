# Chat Application

Spring Boot REST service for creating chat rooms, joining them, sending messages, and reading history. Redis is the primary store: Hash for room metadata, Set for participants, List for messages, and Pub/Sub for live updates.

There is no UI. All APIs are REST and can be tested with Postman.

## Prerequisites

- Java 21 or later
- Maven 3.9+
- Docker

## How to run

Start Redis in Docker. AOF and RDB snapshots are enabled in `docker-compose.yml` so rooms and messages survive a restart.

```bash
cd chat_application
docker compose up -d
```

Then start the application:

```bash
cd chat_application
mvn spring-boot:run
```

The service listens on `http://localhost:8080`.

- Health: `GET http://localhost:8080/actuator/health`
- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

## Deploy on Render

Create a **Web Service** with Docker and set the root directory to `chat_application`. Render uses `Dockerfile`.

Also create a Render **Redis** instance and add this environment variable on the web service:

| Key | Value |
| --- | --- |
| `REDIS_URL` | Internal Redis URL from the Render Redis instance |

Health check path: `/actuator/health`

## Configuration

`src/main/resources/application.properties`

| Property | Default | Purpose |
| --- | --- | --- |
| `server.port` | `PORT` or `8080` | HTTP port (Render sets `PORT`) |
| `spring.data.redis.url` | `REDIS_URL` | Redis URL, used on Render |
| `spring.data.redis.host` | `localhost` | Redis host |
| `spring.data.redis.port` | `6379` | Redis port |
| `spring.data.redis.database` | `0` | Redis database index |
| `cache.redis.ttl` | `15m` | Cache TTL from the AR-Calling communication Redis config |
| `cache.redis.key-prefix` | `chatapp:` | Cache key prefix |

Redis connection uses the same Lettuce standalone factory, `RedisTemplate` / `StringRedisTemplate`, JSON serializer, and Pub/Sub listener container pattern as the AR-Calling communication service.

## Redis data structures

| Key | Type | Content |
| --- | --- | --- |
| `chat:rooms` | Set | Index of room ids |
| `chat:room:{roomId}` | Hash | `roomName`, `createdAt` |
| `chat:room:{roomId}:participants` | Set | Participant names |
| `chat:room:{roomId}:messages` | List | JSON messages in chronological order |
| `channel:live-chat:{roomId}` | Pub/Sub | Live message broadcast |

Each list entry is:

```json
{
  "participant": "guest_user",
  "message": "Hello, everyone!",
  "timestamp": "2024-01-01T10:00:00Z"
}
```

## APIs

Base path: `/api/chatapp/chatrooms`

### Create a chat room

`POST /api/chatapp/chatrooms`

```json
{
  "roomName": "general"
}
```

```json
{
  "message": "Chat room 'general' created successfully.",
  "roomId": "general",
  "status": "success"
}
```

Duplicate names return HTTP 409 with `"status": "error"`.

### Join a chat room

`POST /api/chatapp/chatrooms/{roomId}/join`

```json
{
  "participant": "guest_user"
}
```

```json
{
  "message": "User 'guest_user' joined chat room 'general'.",
  "status": "success"
}
```

### Send a message

`POST /api/chatapp/chatrooms/{roomId}/messages`

```json
{
  "participant": "guest_user",
  "message": "Hello, everyone!"
}
```

```json
{
  "message": "Message sent successfully.",
  "status": "success"
}
```

The sender must have joined the room. The message is appended to the Redis List and published on `channel:live-chat:{roomId}`.

Sending to a room that does not exist returns HTTP 404:

```json
{
  "message": "Chat room 'missing' does not exist.",
  "status": "error"
}
```

### Retrieve chat history

`GET /api/chatapp/chatrooms/{roomId}/messages?limit=10`

Returns the last N messages, oldest first.

```json
{
  "messages": [
    {
      "participant": "guest_user",
      "message": "Hello, everyone!",
      "timestamp": "2024-01-01T10:00:00Z"
    }
  ]
}
```

### Real-time stream

`GET /api/chatapp/chatrooms/{roomId}/stream`

SSE subscription. Keep this request open in Postman, then send a message from another request. The subscriber receives the JSON payload as an SSE `message` event.

### Optional helpers

- `GET /api/chatapp/chatrooms` lists room ids
- `GET /api/chatapp/chatrooms/{roomId}` returns metadata and participants
- `DELETE /api/chatapp/chatrooms/{roomId}` removes the room hash, participant set, message list, and index entry

## Postman

Import `postman/Chat-Application.postman_collection.json`. The `baseUrl` variable defaults to `http://localhost:8080`.

Suggested flow:

1. Create Chat Room
2. Join Chat Room
3. Open Subscribe Real-Time Stream and leave it running
4. Send Message
5. Retrieve Chat History
6. Get Chat Room
7. Send Message To Missing Room
8. Delete Chat Room

## Tests

```bash
cd chat_application
mvn test
```

JUnit coverage includes:

1. Create room and join as `guest_user` — Hash metadata and participant Set
2. Send two messages and read the last one — chronological List order
3. Publish to the room Pub/Sub channel — in-process subscribers receive the payload
4. Send to a missing room — error response
5. Delete a room — related Redis keys are removed

Integration tests start a local `redis-server` with AOF enabled on an ephemeral port.

## Project layout

The API layer follows the db-service style: controller, service interface, service implementation, repository, DTOs, and a global exception handler with shared `Messages` / `StatusCode` constants.
