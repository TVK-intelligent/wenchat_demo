# 💬 WenChat - Terminal CLI Chat Client

## 🎯 Overview

WenChat is a **pure terminal-based chat client** written in Java that connects to a Spring Boot WebSocket server.

- ✅ **No HTML/CSS** - Terminal only
- ✅ **No Web UI** - Command-line interface
- ✅ **Real-time chat** via WebSocket (STOMP protocol)
- ✅ **MySQL database** backend
- ✅ **Multi-room support**
- ✅ **Private messaging**
- ✅ **Typing indicators**
- ✅ **Message recall**

## 📋 Requirements

- Java 21+
- Maven 3.6+
- MySQL 5.7+ (or MariaDB)
- Spring Boot 3.5.7

## 🚀 Quick Start

### 1️⃣ Setup MySQL Database

```bash
mysql -u root -p

CREATE DATABASE chat_realtime_db;
USE chat_realtime_db;
```

### 2️⃣ Start Spring Boot Server

```bash
cd e:\Wenchat\demo
mvn spring-boot:run
```

**Expected output:**

```
Started DemoApplication in X.XXX seconds (JVM running for X.XXX)
Tomcat started on port(s): 8081 (http)
```

### 3️⃣ Run CLI Chat Client

In a **NEW TERMINAL** (keep server running in the first):

```bash
cd e:\Wenchat\demo
mvn exec:java -Dexec.mainClass="com.example.demo.client.ChatClient"
```

**Or run directly from JAR:**

```bash
java -cp target/demo-0.0.1-SNAPSHOT.jar com.example.demo.client.ChatClient
```

## 📖 Available Commands

### Authentication

```bash
/login <username> <password>    # Login to chat
/logout                         # Logout
```

### Room Management

```bash
/rooms                          # List all rooms
/join <roomId>                  # Join a room
/leave                          # Leave current room
```

### Messaging

```bash
/send "<message>"               # Send message to room
/users                          # List online users
/private <userId> "<message>"   # Send private message
```

### Advanced

```bash
/recall <messageId>             # Recall your message
/typing start|stop              # Show typing indicator
/status online|idle|offline     # Change your status
```

### Utility

```bash
/help                           # Show this help menu
/clear                          # Clear terminal
/quit                           # Exit chat
```

## 🎨 Terminal Colors

The CLI uses ANSI color codes:

- 🔵 **Cyan** - Headers, system info
- 🟢 **Green** - Success messages
- 🟡 **Yellow** - Warnings
- 🔴 **Red** - Errors
- ⚪ **Gray** - Timestamps, metadata

## 📂 Project Structure

```
demo/
├── src/
│   ├── main/
│   │   └── java/com/example/demo/
│   │       ├── client/              (NEW - CLI Client)
│   │       │   ├── ChatClient.java  (Entry point)
│   │       │   ├── ui/
│   │       │   │   └── TerminalUI.java
│   │       │   ├── command/
│   │       │   │   └── CommandParser.java
│   │       │   ├── websocket/
│   │       │   │   └── WebSocketClient.java
│   │       │   ├── model/
│   │       │   │   ├── ChatMessage.java
│   │       │   │   ├── User.java
│   │       │   │   └── ChatRoom.java
│   │       │   └── service/
│   │       │       └── ChatService.java
│   │       ├── controller/          (REST API)
│   │       ├── service/             (Business logic)
│   │       ├── entity/              (JPA entities)
│   │       ├── repository/          (Database)
│   │       ├── config/              (Configuration)
│   │       ├── websocket/           (WebSocket handlers)
│   │       └── DemoApplication.java (Server entry point)
│   └── resources/
│       └── application.properties
└── pom.xml
```

## 🔧 Configuration

Edit `src/main/resources/application.properties`:

```properties
# Server
server.port=8081

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/chat_realtime_db
spring.datasource.username=root
spring.datasource.password=12345678

# JWT
jwt.secret=X2x5fQyMZ8nT0bH1uV3pR6sY9jK4eL2mN5oQ7rT8uW0xZ3dF6vC9pR1aT7sG2hJ9
jwt.expiration=86400000
```

## 🧪 Testing

### Create Test User (via API)

```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john",
    "password": "password123",
    "displayName": "John Doe",
    "email": "john@example.com"
  }'
```

### Login

```bash
/login john password123
```

### Create Room (via API)

```bash
curl -X POST http://localhost:8081/api/rooms \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "General",
    "description": "General chat room",
    "isPrivate": false
  }'
```

### Chat

```bash
/join 1
/send "Hello everyone!"
/users
```

## 📊 WebSocket Endpoints

The CLI client connects to these STOMP destinations:

| Endpoint                        | Purpose              | Example           |
| ------------------------------- | -------------------- | ----------------- |
| `/app/chat/room/{roomId}`       | Send room message    | `/send "Hello"`   |
| `/app/private/{userId}`         | Send private message | `/private 5 "Hi"` |
| `/app/typing/room/{roomId}`     | Typing indicator     | `/typing start`   |
| `/app/status/change`            | Change online status | `/status online`  |
| `/topic/room/{roomId}`          | Subscribe to room    | Automatic         |
| `/user/{userId}/queue/messages` | Private messages     | Automatic         |
| `/topic/user-status`            | Status updates       | Automatic         |

## 🐛 Troubleshooting

### Server won't start

```bash
# Check if port 8081 is free
netstat -ano | findstr :8081

# Kill process using port 8081
taskkill /PID <PID> /F
```

### Database connection failed

```bash
# Check MySQL is running
mysql -u root -p -e "SELECT 1"

# Create database if missing
mysql -u root -p < create_db.sql
```

### WebSocket connection failed

```bash
# Check server is running on port 8081
curl http://localhost:8081/actuator/health

# Check firewall allows localhost:8081
```

### CLI client crashes

```bash
# Run with debug output
java -cp target/demo-0.0.1-SNAPSHOT.jar \
  -Dlogging.level.root=DEBUG \
  com.example.demo.client.ChatClient
```

## 📈 Architecture

```
┌──────────────────────────┐
│   Terminal (CLI)         │
│  ChatClient.java         │
├──────────────────────────┤
│                          │
│  TerminalUI              │
│  CommandParser           │
│  ChatService (REST API)  │
│  WebSocketClient (STOMP) │
└────────┬─────────────────┘
         │ WebSocket
         │ STOMP/SockJS
         ↓
┌──────────────────────────┐
│  Spring Boot (8081)      │
│  WebSocket Server        │
├──────────────────────────┤
│  ChatMessageController   │
│  REST API Controllers    │
│  Service Layer           │
└────────┬─────────────────┘
         │ JDBC
         ↓
┌──────────────────────────┐
│  MySQL Database          │
│  - chat_realtime_db      │
│  - users, rooms, msgs    │
└──────────────────────────┘
```

## 📝 Example Workflow

```bash
# Terminal 1: Start server
cd e:\Wenchat\demo
mvn spring-boot:run
# Waiting... [Started DemoApplication]

# Terminal 2: Run client
cd e:\Wenchat\demo
mvn exec:java -Dexec.mainClass="com.example.demo.client.ChatClient"

# Terminal 2 output:
╔════════════════════════════════════════════════════════════════╗
║             WENCHAT TERMINAL CLI CLIENT v1.0                   ║
║            WebSocket Real-time Chat Application                ║
╚════════════════════════════════════════════════════════════════╝

> /login alice password
✓ Logged in as alice
✓ WebSocket connected!

> /rooms
📚 Available Rooms:
  • [1] General (5 users)
  • [2] Random (2 users)

> /join 1
✓ Joined room 1
Status: Connected | Room: Room 1 | Users: 5

> /send "Hello everyone!"
✓ Message sent

> /users
📊 Online Users:
  • 🟢 Alice (active now)
  • 🟢 Bob (active now)
  • 🟡 Charlie (idle 5m)

> /quit
Logged out
Thanks for using WenChat! Goodbye!
```

## 🎓 Learning Resources

- [STOMP Protocol](https://stomp.github.io/)
- [Spring WebSocket Docs](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#websocket)
- [ANSI Color Codes](https://en.wikipedia.org/wiki/ANSI_escape_code)
- [Java WebSocket Library](https://github.com/TooTallNate/Java-WebSocket)

## 📄 License

MIT License - Feel free to use and modify

## 👨‍💼 Author

WenChat Team - 2025

---

**Happy Chatting! 💬**
