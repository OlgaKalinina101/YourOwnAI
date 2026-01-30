# Changelog

All notable changes to YourOwnAI will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **Context Inheritance** - fork conversations with inherited message history
  - Dialog for selecting source chat when creating new conversation
  - Last N message pairs from source chat loaded into context
  - Inherited messages gradually replaced as new messages added
- **OpenRouter integration** - 14 models: Claude (6), Llama 4 (2), Gemini (4), GPT-4o (1), OpenAI (1) - access to 200+ models
- **Voice Chat persistent history** - last 100 messages saved locally (SharedPreferences)
- **Improved Memory filtering** - automatically excludes non-meaningful responses ("Нет ключевой информации", etc.)
- **Android Auto Backup** - automatic backup of chats and settings (API keys excluded for security)
- **Import/Export Chat** - export chats to text, import from clipboard or .txt files
- **Voice Chat** - real-time voice conversation with xAI Grok (5 voices, system prompts, user context)
- Multiple AI provider support (Deepseek, OpenAI, x.ai Grok)
- Local model inference with Llamatik (Qwen 2.5 1.7B, Llama 3.2 3B)
- Model download manager with queue system
- Automatic corruption detection (GGUF header validation)
- Markdown rendering in chat (bold, italic, links, blockquotes)
- Request logs dialog for API debugging
- Onboarding flow with theme customization
- Settings screen with appearance customization
- Deep Empathy mode flag
- Thread-safe model loading and generation (Mutex)
- Smart navigation (remembers previous screen)
- Dynamic theme application without restart

### Fixed
- OutOfMemoryError during model downloads (separate OkHttpClient without body logging)
- Chat UI jittering during streaming (optimized state updates)
- Chat switching to previous conversation bug (distinctUntilChanged)
- Auto-scroll to bottom after streaming completes
- Settings screen crash (removed manual Job management)
- OpenAI API parameter incompatibilities (max_completion_tokens, conditional temperature)
- Multiple concurrent model loads causing crashes (Mutex protection)
- Corrupt model files causing SIGSEGV (automatic detection and deletion)
- Progress bar stuck at 0% during downloads (time-based updates)

### Changed
- Improved model download progress tracking (updates every 500ms)
- Reduced buffer size (8KB → 4KB) to lower memory pressure
- Added largeHeap="true" for 512MB memory limit
- Enhanced ProGuard rules for Llamatik and native methods

## [0.1.0] - 2026-01-26

### Added
- Initial beta release
- Basic chat interface with streaming
- Multiple conversations
- Encrypted API key storage
- Room Database for local storage
- Material 3 Dynamic Color theming
- System prompt editor
- User context editor
- AI configuration (temperature, top-p, max tokens, history limit)

---

## Version History Notes

### Current Focus
Working on stabilizing local model inference and fixing release build crashes.

### Next Priorities
1. RAG implementation (document upload)
2. Long-term memory system
3. Usage tracking (tokens, cost)
4. Message regeneration and alternatives
5. Production keystore and Google Play release
