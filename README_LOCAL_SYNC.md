# Local Network Sync - Implementation Guide

## 🚀 What We Built

**Android HTTP Server** for syncing data with Desktop app over local Wi-Fi network.

---

## 📱 Android Side (DONE ✅)

### Files Created:
```
data/sync/local/
├── models/LocalSyncModels.kt       # Data models for sync
├── LocalSyncServer.kt              # Ktor HTTP server
└── LocalSyncRepository.kt          # Repository for server management

presentation/settings/sections/
└── LocalSyncSection.kt             # UI for starting/stopping server
```

### Dependencies Added:
```kotlin
// Ktor Server
implementation("io.ktor:ktor-server-core:2.3.7")
implementation("io.ktor:ktor-server-netty:2.3.7")
implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
implementation("io.ktor:ktor-serialization-gson:2.3.7")
implementation("io.ktor:ktor-server-cors:2.3.7")
```

### How It Works:

1. **Start Server** in Settings → Local Network Sync
2. Server runs on port **8765** (default)
3. Advertises itself via **mDNS** (future: `_yourown-ai._tcp`)
4. Desktop app can connect to `http://[phone-ip]:8765`

---

## 🖥️ Desktop Side (TODO)

### Option 1: Compose Desktop (Recommended)
Share 80-90% code with Android:

```kotlin
// shared module
commonMain/
  ├── domain/      # ViewModels, UseCases
  ├── data/        # Repository, DB
  └── ui/          # Compose UI

androidApp/        # Android-specific
desktopApp/        # Desktop-specific (JVM)
```

### Option 2: Tauri (Fast prototyping)
Lightweight Rust + Web frontend:

```bash
npm create tauri-app
```

Frontend: React/Vue/Svelte
Backend: Rust for HTTP client

---

## 🔌 API Endpoints

### 1. Health Check
```
GET /
Response: "YourOwnAI Local Sync Server"
```

### 2. Server Status
```
GET /status
Response: {
  "isRunning": true,
  "deviceInfo": {...},
  "port": 8765,
  "totalConversations": 23,
  "totalMemories": 13,
  "totalPersonas": 2
}
```

### 3. Full Sync
```
POST /sync/full
Body: {
  "deviceInfo": {...},
  "lastSyncTimestamp": 0
}
Response: {
  "conversations": [...],
  "messages": [...],
  "memories": [...],  // WITHOUT embeddings
  "personas": [...]
}
```

### 4. Incremental Sync
```
POST /sync/incremental
Body: {
  "deviceInfo": {...},
  "lastSyncTimestamp": 1738462800000
}
Response: {
  "newConversations": [...],
  "updatedConversations": [...],
  "newMessages": [...],
  "newMemories": [...],
  "newPersonas": [...]
}
```

### 5. Individual Resources
```
GET /conversations
GET /memories
GET /personas
```

---

## 🔄 Sync Flow

### Desktop → Phone (Download)

1. Desktop discovers phone via mDNS (or manual IP)
2. Desktop connects: `http://192.168.1.100:8765`
3. Desktop requests: `POST /sync/incremental` with `lastSyncTimestamp`
4. Phone sends only NEW data since timestamp
5. Desktop saves to local DB
6. Desktop generates embeddings locally

### Phone → Desktop (Upload)

(Future: Reverse sync)
1. Desktop runs HTTP server too
2. Phone sends changes to Desktop
3. Conflict resolution: newest timestamp wins

---

## 🛠️ Next Steps

### Phase 1: Desktop Client (This Week)
- [ ] Create Compose Desktop app (or Tauri)
- [ ] Implement mDNS discovery (or manual IP input)
- [ ] HTTP client for sync endpoints
- [ ] Local SQLite database
- [ ] Generate embeddings on Desktop

### Phase 2: Bidirectional Sync
- [ ] Desktop HTTP server
- [ ] Phone can push changes to Desktop
- [ ] Conflict resolution (timestamp-based)

### Phase 3: Real-time Sync
- [ ] WebSocket for live updates
- [ ] Push notifications when data changes

### Phase 4: Security
- [ ] TLS/SSL for encrypted connection
- [ ] Pairing code / QR code authentication
- [ ] Token-based auth

---

## 🔒 Security Notes

**Current:** HTTP (cleartext) - OK for local network
**Production:** Add HTTPS + authentication:

```kotlin
// Generate self-signed cert
val keyStore = generateKeyStore()
val sslConnector = applicationEngineEnvironment {
    sslConnector(
        keyStore = keyStore,
        keyAlias = "yourown-ai",
        keyStorePassword = { "password".toCharArray() }
    )
}
```

---

## 📊 Data Size Estimates

**500 Conversations + 10,000 Messages + 500 Memories:**

| Component | With Embeddings | Without Embeddings |
|-----------|----------------|-------------------|
| Conversations | ~250 KB | ~250 KB |
| Messages | ~5 MB | ~5 MB |
| Memories | ~6 MB | ~250 KB |
| **Total** | **~11 MB** | **~5.5 MB** |

**Sync time on local Wi-Fi:**
- Full sync: ~2-3 seconds
- Incremental: ~500ms

---

## 🎯 Testing

### 1. Start Server on Android
```kotlin
// In Settings
viewModel.startLocalSyncServer()
```

### 2. Test from Desktop (curl)
```bash
# Get server status
curl http://192.168.1.100:8765/status

# Full sync
curl -X POST http://192.168.1.100:8765/sync/full \
  -H "Content-Type: application/json" \
  -d '{"deviceInfo": {"deviceId": "desktop-1", "deviceName": "My Desktop", "appVersion": "0.1.0", "platform": "Desktop"}}'

# Get conversations
curl http://192.168.1.100:8765/conversations
```

---

## 🐛 Troubleshooting

### Server won't start
- Check port 8765 is not in use
- Check Wi-Fi is connected
- Check network permissions

### Can't connect from Desktop
- Verify phone IP: Settings → About Phone → Status → IP address
- Ping phone: `ping 192.168.1.100`
- Check firewall on Desktop
- Try different port: `startServer(port = 8766)`

### Slow sync
- Use incremental sync instead of full
- Reduce `messageHistoryLimit`
- Compress JSON (gzip)

---

## 📚 Resources

- [Ktor Server Docs](https://ktor.io/docs/server.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Tauri](https://tauri.app/)
- [mDNS / Bonjour](https://developer.android.com/training/connect-devices-wirelessly/nsd)

---

**Status:** ✅ Android Server Implementation Complete  
**Next:** Desktop Client App
