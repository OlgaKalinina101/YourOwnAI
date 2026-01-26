# Architecture Overview

YourOwnAI follows **Clean Architecture** principles with MVVM pattern using Jetpack Compose.

## 🏗 Layers

### 1. Presentation Layer (`presentation/`)
**Responsibility:** UI and user interaction

- **Jetpack Compose** - Declarative UI
- **ViewModels** - UI state management
- **StateFlow** - Reactive state updates
- **Navigation** - Screen transitions

**Key Components:**
- `ChatScreen.kt` / `ChatViewModel.kt` - Main chat interface
- `SettingsScreen.kt` / `SettingsViewModel.kt` - App settings
- `HomeScreen.kt` / `HomeViewModel.kt` - Conversations list
- `OnboardingScreen.kt` / `OnboardingViewModel.kt` - First launch setup

### 2. Domain Layer (`domain/`)
**Responsibility:** Business logic and models

- **Models** - Data classes and enums
- **Service interfaces** - Abstract AI operations
- **Business rules** - Validation, transformation

**Key Components:**
- `ModelProvider.kt` - AI model definitions (Deepseek, OpenAI, x.ai, Local)
- `LocalModel.kt` - Local model specifications
- `Settings.kt` - User preferences
- `AIConfig.kt` - AI generation parameters
- `LlamaService.kt` - Local inference interface
- `AIService.kt` - Unified AI service interface

### 3. Data Layer (`data/`)
**Responsibility:** Data sources and repositories

**Submodules:**

#### `data/local/`
- **Room Database** - Conversations, messages, API keys
- **DAO interfaces** - Database access
- **Entities** - Database tables

#### `data/remote/`
- **API clients** - HTTP communication
  - `DeepseekClient.kt` - Deepseek API
  - `OpenAIClient.kt` - OpenAI API (GPT-5, GPT-4o, o1/o3)
  - `XAIClient.kt` - x.ai Grok API
- **Models** - Request/response DTOs
- **Streaming** - SSE (Server-Sent Events) handling

#### `data/repository/`
- **Repositories** - Abstract data sources
  - `ConversationRepository.kt` - Chat history
  - `MessageRepository.kt` - Individual messages
  - `ApiKeyRepository.kt` - Encrypted key storage
  - `LocalModelRepository.kt` - Model downloads and management

#### `data/service/`
- **Service implementations**
  - `AIServiceImpl.kt` - Unified AI service
  - `LlamaServiceImpl.kt` - Local inference with Llamatik

#### `data/llama/`
- `LlamaCppWrapper.kt` - Llamatik JNI wrapper

### 4. Dependency Injection (`di/`)
**Responsibility:** Provide dependencies via Hilt

- `NetworkModule.kt` - OkHttpClient, API clients
  - `@ApiClient` - For API calls (with logging)
  - `@DownloadClient` - For model downloads (no logging)
- `DatabaseModule.kt` - Room DB, DAOs
- `RepositoryModule.kt` - Repositories

## 🔄 Data Flow

### Chat Message Flow
```
User types message
    ↓
ChatScreen (Compose UI)
    ↓
ChatViewModel.sendMessage()
    ↓
AIService.generateResponse()
    ↓
[API Client OR LlamaService]
    ↓
Flow<String> (streaming chunks)
    ↓
ChatViewModel.collect { chunk }
    ↓
Update _uiState.messages (local state)
    ↓
ChatScreen recomposes (shows streaming text)
    ↓
After streaming completes
    ↓
MessageRepository.updateMessage() (save to DB)
```

### Model Download Flow
```
User taps Download
    ↓
SettingsDialogs.LocalModelsDialog
    ↓
SettingsViewModel.downloadModel()
    ↓
LocalModelRepository.downloadModel()
    ↓
Mutex.withLock (queue if another download active)
    ↓
Set status to Queued (show "В очереди..." in UI)
    ↓
Wait for queue
    ↓
Set status to Downloading(0%)
    ↓
OkHttp streams file to disk (4KB buffer)
    ↓
Update progress every 500ms or 1%
    ↓
Verify GGUF header after download
    ↓
Set status to Downloaded or Failed
```

## 🧵 Threading Model

### Main Thread (Dispatchers.Main)
- UI rendering (Jetpack Compose)
- ViewModel state updates
- Navigation

### IO Thread (Dispatchers.IO)
- Database operations (Room)
- File operations (model downloads)
- Network calls (API clients)
- Local model inference (Llamatik)

### Synchronization
- **Mutex** for model loading (LlamaService) - prevents concurrent JNI calls
- **Mutex** for model generation (LlamaService) - thread-safe inference
- **Mutex** for model downloads (LocalModelRepository) - one download at a time
- **StateFlow** for reactive state updates
- **distinctUntilChanged()** to prevent redundant UI updates

## 🔐 Security Architecture

### API Key Storage
```
User enters API key
    ↓
Encrypted with Android Keystore System
    ↓
Stored in EncryptedSharedPreferences
    ↓
Retrieved and decrypted on demand
    ↓
Used in API calls (Authorization header)
    ↓
Redacted in logs
```

### Network Security
- `network_security_config.xml` - Enforce HTTPS
- Certificate pinning configuration ready
- No cleartext traffic allowed

### Build Security (Release)
- **ProGuard/R8** - Code obfuscation
- **Resource shrinking** - Remove unused resources
- **Debug log removal** - Keep only error logs
- **String encryption** - Obfuscate sensitive strings

## 🚨 Critical Implementation Details

### Llamatik Thread Safety
**Problem:** llama.cpp is NOT thread-safe
**Solution:**
- Singleton `LlamaServiceImpl` with Mutex
- Only one `loadModel()` at a time
- Only one `generateResponse()` at a time
- Proper unload before loading new model

### OkHttp Memory Management
**Problem:** LoggingInterceptor with `Level.BODY` loads entire file (1GB+) into memory
**Solution:**
- Two separate OkHttpClient instances:
  - `@ApiClient` - For API calls (with body logging)
  - `@DownloadClient` - For downloads (NO body logging)
- 4KB buffer for streaming downloads
- `largeHeap="true"` for 512MB memory limit

### OpenAI API Compatibility
**Problem:** Different models have different parameter requirements
**Solution:**
- Detection sets: `NEW_API_MODELS`, `REASONING_MODELS`
- Conditional parameter passing:
  - GPT-5/4.1: use `max_completion_tokens`
  - o1/o3: omit `temperature` and `top_p`
  - Legacy: use `max_tokens`, include `temperature`

### UI Performance
**Problem:** Chat UI jitters during streaming
**Solution:**
- Update local `_uiState` during streaming (not DB)
- Single DB write after streaming completes
- `contentType` in LazyColumn for optimization
- `distinctUntilChanged()` on conversation flows
- `scrollToItem()` without animation during streaming

## 📐 Design Patterns

### Repository Pattern
```kotlin
interface ConversationRepository {
    fun getConversations(): Flow<List<Conversation>>
    suspend fun createConversation(): Conversation
}

class ConversationRepositoryImpl @Inject constructor(
    private val conversationDao: ConversationDao
) : ConversationRepository {
    // Implementation
}
```

### Service Pattern
```kotlin
interface AIService {
    suspend fun generateResponse(...): Flow<String>
}

class AIServiceImpl @Inject constructor(
    private val deepseekClient: DeepseekClient,
    private val openAIClient: OpenAIClient,
    // ...
) : AIService {
    // Unified interface for all providers
}
```

### ViewModel Pattern
```kotlin
class ChatViewModel @Inject constructor(
    private val aiService: AIService,
    private val conversationRepository: ConversationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    fun sendMessage() {
        viewModelScope.launch {
            aiService.generateResponse(...).collect { chunk ->
                // Update UI
            }
        }
    }
}
```

## 📖 Further Reading

- [README.md](README.md) - Project overview and features
- [CONTRIBUTING.md](CONTRIBUTING.md) - How to contribute
- [SECURITY.md](SECURITY.md) - Security best practices
- [CHANGELOG.md](CHANGELOG.md) - Version history
- [CHAT_IMPLEMENTATION_PLAN.md](CHAT_IMPLEMENTATION_PLAN.md) - Chat feature specs
- [LLAMA_CPP_INTEGRATION.md](LLAMA_CPP_INTEGRATION.md) - Local inference details
