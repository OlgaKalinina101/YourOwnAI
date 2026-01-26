# YourOwnAI 🤖

![Android](https://img.shields.io/badge/Android-26%2B-green.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-100%25-purple.svg)
![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)
![Status](https://img.shields.io/badge/Status-Beta-orange.svg)

**Your AI, Your Rules. No Corporations, No Censorship, No Limits.**

YourOwnAI is a privacy-first Android application that gives you complete control over your AI assistant. Use your own API keys, store everything locally, and define your AI's personality exactly how you want it.

**Current Status:** 🚧 Beta - Core features implemented, actively developed

---

## 📸 Screenshots

<div align="center">

### 💬 Chat Interface
<table>
  <tr>
    <td><img src="examples/chat1.jpg" width="200"/></td>
    <td><img src="examples/chat2.jpg" width="200"/></td>
    <td><img src="examples/chat3.jpg" width="200"/></td>
    <td><img src="examples/chat4.jpg" width="200"/></td>
  </tr>
</table>

### 🤖 AI Models & Settings
<table>
  <tr>
    <td><img src="examples/models1.jpg" width="200"/></td>
    <td><img src="examples/models2.jpg" width="200"/></td>
    <td><img src="examples/local_models.jpg" width="200"/></td>
  </tr>
</table>

### ✨ Onboarding & Customization
<table>
  <tr>
    <td><img src="examples/onboarding.gif" width="300"/></td>
    <td><img src="examples/settings.gif" width="300"/></td>
  </tr>
</table>

</div>

---

## 🎯 Why YourOwnAI?

Fed up with:
- 💸 **Subscription fees** that profile and monetize your conversations?
- 🕵️ **Corporate oversight** deciding what's "appropriate" to discuss?
- 🔒 **Vendor lock-in** limiting your AI provider choices?
- ☁️ **Cloud dependency** where your data lives on someone else's servers?

**YourOwnAI gives you back control:**
- ✅ Use **any AI provider** with your API keys - no middleman
- ✅ **100% local storage** - conversations never leave your device
- ✅ **Switch providers** freely - Deepseek, OpenAI, x.ai, or local models
- ✅ **Offline capable** - download models and chat without internet
- ✅ **Open source** - audit the code, contribute, or fork it

## 🎯 Core Philosophy

Every person has the right to interact with AI on their own terms - not what corporations deem "acceptable," "appropriate," or "safe." Whether it's a digital companion, a work assistant, a creative partner, or anything else - **you decide**.

### Key Principles

**Privacy**
- 🔐 All conversations encrypted and stored locally
- 🔑 API keys secured with Android Keystore System
- 🚫 Zero telemetry - no analytics, tracking, or profiling
- 📱 Data never leaves your device (unless you use cloud API)

**Control**
- ⚙️ Full customization of AI behavior via system prompts
- 🎛️ Adjust temperature, top-p, max tokens, context length
- 🧠 Optional "Deep Empathy" mode for emotional intelligence
- 🔄 Switch between providers and models freely

**Freedom**
- 🌐 Direct API access - no corporate intermediaries
- 💰 No subscriptions - pay only for your API usage
- 🏠 Offline mode with local models (Qwen 2.5, Llama 3.2)
- 📖 Open source - inspect, modify, or fork the code

## 🎨 Design Philosophy

> **"The app is a canvas. You and your AI create the masterpiece."**

YourOwnAI follows a **maximally neutral design approach**. The interface doesn't impose mood, personality, or emotional tone - that comes from your customization and your AI's character.

### Visual Design Principles

**Neutrality First**
- No "cute" or "playful" design elements
- No corporate color schemes that suggest trust/innovation/friendliness
- No fonts with built-in personality
- Clean, functional, minimalist interface
- Content is king, UI is invisible

**Material 3 Dynamic Color (Android 12+)**
- Colors adapt from your device wallpaper
- Familiar, personalized, yet neutral
- Respects system dark/light theme
- Falls back to grayscale on older devices

**Typography**
- **Roboto** (Android default) - maximally neutral, universally familiar
- Option to use system font (respects user's device settings)
- Adjustable text size for accessibility
- No decorative or "emotional" typefaces

**User Customization Options**
- System colors (Dynamic Color) or neutral grayscale
- Light/dark/system theme
- Custom accent color (optional, for those who want it)
- Font size adjustments
- UI density options

**The Philosophy:**
The app should feel like a **tool**, not a product with personality. It's your space to build whatever relationship with AI you choose - companion, assistant, note-taker, or anything else. The design stays out of the way.

## ✨ Features

### ✅ Implemented & Working

#### 🔐 Privacy & Control
- **Local-first architecture** - all data stored on device with Room Database
- **Encrypted API keys** - secured with Android Keystore System
- **No backend** - direct communication with AI providers
- **No tracking** - zero analytics, telemetry, or user profiling
- **Onboarding customization** - theme, colors, fonts, text size
- **Dynamic theming** - Material 3 Dynamic Color from your wallpaper
- **Settings persistence** - all preferences saved locally

#### 💬 Chat Experience
- **Streaming responses** - real-time AI generation with smooth animations
- **Multiple conversations** - organize chats by topic
- **Model switching** - change AI provider/model per conversation
- **Markdown rendering** - **bold**, *italic*, [clickable links](url), and > blockquotes
- **Request logs** - inspect full API requests (JSON) for debugging
- **Message history** - configurable context length (5-50 messages)
- **Conversation titles** - auto-generated or manual edit

#### 🤖 AI Providers & Models
- **Deepseek** - deepseek-chat, deepseek-reasoner
- **OpenAI** - GPT-5 series, GPT-4o, o1/o3 reasoning models
  - Smart parameter detection (max_completion_tokens, conditional temperature)
- **x.ai (Grok)** - Grok 4.1, Grok 4, Grok 3, Grok Code
- **Local inference** - Qwen 2.5 1.7B (950MB), Llama 3.2 3B (1.9GB)
  - Download queue system (one at a time)
  - Progress tracking with UI updates
  - Automatic corruption detection (GGUF validation)
  - Thread-safe loading and generation (Mutex)

#### ⚙️ AI Configuration
- **System prompt editor** - customize AI personality
- **User context** - persistent facts about you
- **Temperature** (0.0-2.0) - control creativity vs consistency
- **Top-P** (0.0-1.0) - nucleus sampling for diversity
- **Max tokens** - response length limit
- **Deep Empathy mode** - experimental emotional intelligence flag

#### 🎨 Appearance & Accessibility
- **Three themes** - Light, Dark, System
- **Two color styles** - Dynamic (from wallpaper), Neutral (grayscale)
- **Three fonts** - Roboto, System, Monospace
- **Four text sizes** - Small, Medium, Large, Extra Large
- **Real-time theme switching** - no restart required

### 🚧 Coming Soon

#### 📊 Usage Tracking
- Monitor token usage per provider and model
- Cost tracking based on current pricing
- Daily/weekly/monthly statistics
- Export usage reports

#### 🧠 Advanced AI
- **RAG** - Upload documents (PDF/TXT) for contextual responses
- **Long-term memory** - AI remembers facts across conversations
- **Message alternatives** - regenerate or swipe for different responses
- **Voice chat** - Speech-to-text and text-to-speech

#### 🔒 Security Enhancements
- Biometric authentication option
- Screenshot prevention for sensitive screens
- Root detection warnings
- Additional ProGuard hardening

#### 🌐 More Providers
- Anthropic (Claude 3.5 Sonnet, Opus)
- Google (Gemini Pro, Ultra)
- Groq (ultra-fast inference)
- OpenRouter (100+ models)

## 🛠 Technology Stack

- **Language:** Kotlin 100%
- **UI:** Jetpack Compose + Material 3 Dynamic Color
- **Architecture:** Clean Architecture (MVVM)
- **Local Storage:** Room Database + EncryptedSharedPreferences (Android Keystore)
- **Async:** Coroutines + Flow
- **DI:** Hilt (Dagger)
- **Local AI:** Llamatik (llama.cpp Android wrapper)
- **API Clients:** OkHttp + Retrofit + Gson
- **Security:** Certificate Pinning, Network Security Config
- **Build:** Gradle 8.11+ with R8/ProGuard obfuscation

## 🚀 Getting Started

### Quick Start (5 minutes)

1. **Download** the latest APK from [Releases](https://github.com/yourusername/YourOwnAI/releases)
2. **Install** on your Android 8.0+ device
3. **Complete onboarding** - choose your theme and colors
4. **Add API key**:
   - Get free Deepseek API key: https://platform.deepseek.com/
   - Or OpenAI: https://platform.openai.com/api-keys
   - Or x.ai Grok: https://console.x.ai/
5. **Start chatting** - select a model and go!

### For Developers

#### Prerequisites
- Android Studio Ladybug or newer
- Android SDK 26+ (minSdk 26, targetSdk 35)
- Gradle 8.11+
- JDK 17

#### Installation

1. **Clone the repository**
```bash
git clone https://github.com/yourusername/YourOwnAI.git
cd YourOwnAI
```

2. **Open in Android Studio**
```bash
# Open Android Studio and select "Open an existing project"
# Navigate to the cloned directory
```

3. **Build and run**
```bash
./gradlew assembleDebug
# Or use Android Studio's Run button
```

### Building Release APK

For testing release builds (with ProGuard/R8):

1. **Using debug keystore (for testing)**
```bash
./gradlew assembleRelease
# APK location: app/build/outputs/apk/release/app-release.apk
```

2. **For production (Google Play)**
```bash
# Generate production keystore (one time only)
keytool -genkey -v -keystore yourownnai-release.keystore \
  -alias yourownnai -keyalg RSA -keysize 2048 -validity 10000

# Update build.gradle.kts signingConfigs with your keystore
# Then build:
./gradlew assembleRelease
```

3. **Common build issues**
- If Gradle wrapper is missing, use Android Studio's Build menu
- Clean project before release builds: `./gradlew clean`
- Check ProGuard mapping files: `app/build/outputs/mapping/release/`

### First Launch Setup

1. **Complete onboarding**
   - Choose theme (Light/Dark/System)
   - Select color style (Dynamic/Neutral)
   - Pick font (Roboto/System)
   - Adjust text size

2. **Add your API key**
   - Open Settings → API Keys
   - Select provider (Deepseek, OpenAI, x.ai)
   - Enter your API key (stored encrypted with Android Keystore)
   - Test the connection

3. **Optional: Download local model**
   - Settings → Local AI Models
   - Choose Qwen 2.5 1.7B (950MB) or Llama 3.2 3B (1.9GB)
   - Models download one at a time with progress tracking
   - Models are validated automatically (GGUF header check)

4. **Start chatting!**
   - Select a model from the dropdown
   - Customize settings (temperature, system prompt, etc.)
   - View detailed request logs for debugging

## 📱 Usage

### Basic Chat
- Type your message in any conversation
- AI responds using your selected model (API or local)
- Streaming responses with smooth animations
- All conversations stored locally and encrypted
- Markdown rendering: **bold**, *italic*, [links](url), and > blockquotes

### Switching Models
- Tap model selector at top of chat
- Choose from:
  - **API models** - Deepseek, OpenAI GPT-5/4o, x.ai Grok
  - **Local models** - Qwen 2.5 1.7B, Llama 3.2 3B (if downloaded)
- Model persists per conversation

### Customizing AI Behavior
1. Settings → System Prompt
2. Edit the default prompt or write your own
3. Adjust parameters:
   - **Temperature** (0.0-2.0) - creativity vs consistency
   - **Top-P** (0.0-1.0) - diversity of word choices
   - **Max Tokens** - response length limit
   - **Message History** - how many messages to include as context
   - **Deep Empathy** - enhanced emotional intelligence (experimental)

### Debugging API Calls
1. Long press any AI message
2. Select "View Request Logs"
3. See complete JSON snapshot:
   - System prompt
   - Messages sent
   - Model parameters
   - AI flags
4. Copy logs for troubleshooting

## 🏗 Project Structure

```
YourOwnAI/
├── app/
│   ├── src/main/
│   │   ├── java/com/yourown/ai/
│   │   │   ├── data/
│   │   │   │   ├── local/          # Room DB, DAOs, Entities
│   │   │   │   ├── remote/         # API clients (Deepseek, OpenAI, x.ai)
│   │   │   │   ├── repository/     # Data repositories
│   │   │   │   ├── service/        # AI service implementations
│   │   │   │   └── llama/          # Llamatik wrapper for local models
│   │   │   ├── domain/
│   │   │   │   ├── model/          # Domain models, enums
│   │   │   │   └── service/        # Service interfaces
│   │   │   ├── presentation/
│   │   │   │   ├── onboarding/     # First launch setup
│   │   │   │   ├── chat/           # Chat UI and ViewModels
│   │   │   │   ├── settings/       # Settings screens and dialogs
│   │   │   │   ├── home/           # Conversations list
│   │   │   │   └── theme/          # Material 3 theming
│   │   │   ├── di/                 # Hilt dependency injection
│   │   │   └── YourOwnAIApplication.kt
│   │   ├── res/                    # Resources
│   │   │   ├── xml/network_security_config.xml
│   │   │   └── values/strings.xml
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
├── build.gradle.kts
└── README.md
```

## 🔒 Privacy & Security

### What We Store Locally
- Chat conversations (Room Database)
- Messages with request logs for debugging
- API keys (encrypted with Android Keystore)
- User preferences (theme, colors, fonts)
- System prompt and user context
- AI configuration (temperature, top-p, etc.)
- Downloaded local models (Qwen 2.5, Llama 3.2)

### What We DON'T Collect
- ❌ No analytics or telemetry
- ❌ No crash reporting to third parties
- ❌ No user tracking or profiling
- ❌ No cloud backups
- ❌ No data mining
- ❌ No ads or monetization

### Security Measures
- **Android Keystore System** for API key encryption
- **EncryptedSharedPreferences** for sensitive settings
- **HTTPS only** with Network Security Config
- **Certificate pinning** configuration ready
- **ProGuard/R8** obfuscation for release builds
- **No root required** - works on any device
- **Memory isolation** - local models use separate memory heap

## 🌍 Supported AI Providers

| Provider | Models | Notes |
|----------|--------|-------|
| Deepseek | Deepseek Chat, Deepseek Reasoner | Fast, cost-effective reasoning |
| OpenAI | GPT-5, GPT-4o, GPT-4o Mini, o1/o3 | Best quality, newest models |
| x.ai (Grok) | Grok 4.1, Grok 4, Grok 3, Grok Code | Fast reasoning and code models |
| Local | Qwen 2.5 1.7B, Llama 3.2 3B | Completely offline via llama.cpp |

### Coming Soon
- Anthropic (Claude 3.5 Sonnet, Claude 3 Opus)
- Google (Gemini Pro, Gemini Ultra)
- Groq (Llama 3, Mixtral)
- OpenRouter (100+ models with one key)

## 🗺 Roadmap

### Phase 1: Core Chat ✅ (Completed)
- [x] Project setup with Jetpack Compose + Hilt
- [x] Chat interface with streaming responses
- [x] Multiple conversations management
- [x] API key management (encrypted storage)
- [x] Room Database for local storage
- [x] Deepseek API integration
- [x] OpenAI API integration (GPT-5, GPT-4o, o1/o3)
- [x] x.ai (Grok) API integration
- [x] Local model integration (Llamatik/llama.cpp)
- [x] Model download manager with queue
- [x] Onboarding flow with theme customization
- [x] Settings screen with appearance dialog
- [x] Markdown rendering (bold, italic, links, blockquotes)
- [x] Request logs for debugging

### Phase 2: Advanced Features (In Progress)
- [ ] RAG - Document upload (PDF/TXT)
- [ ] Long-term memory system
- [ ] Message regeneration
- [ ] Message alternatives (swipe)
- [ ] Usage tracking (tokens, cost)
- [ ] Voice chat (STT/TTS)
- [ ] Export/backup conversations
- [ ] Anthropic Claude integration

### Phase 3: Polish & Security
- [ ] Biometric authentication
- [ ] Screenshot prevention for sensitive screens
- [ ] Root detection
- [ ] Additional ProGuard hardening
- [ ] Performance optimization for large conversations
- [ ] Accessibility improvements

### Phase 4: Distribution
- [ ] Production keystore setup
- [ ] Google Play release
- [ ] F-Droid release
- [ ] Documentation and tutorials

### Future Considerations
- [ ] Optional Supabase sync
- [ ] Import from Character.AI, Replika, etc.
- [ ] Image generation
- [ ] Custom voice cloning
- [ ] Plugin system

## 🤝 Contributing

This project is open source and contributions are welcome! Whether you're fixing bugs, adding features, or improving documentation - we appreciate your help.

### How to Contribute
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines
- Follow Kotlin coding conventions
- Write clean, documented code
- Test on both debug and release builds
- Keep UI simple and intuitive
- Respect user privacy - no telemetry without explicit opt-in

## 🐛 Known Issues & Solutions

### Local Model Crashes
**Problem:** App crashes when using local models (Qwen/Llama)
**Solution:** 
- Llamatik is not thread-safe - we use Mutex to prevent concurrent access
- Models are validated on startup (GGUF header check)
- Corrupt files are automatically deleted
- Download queue prevents OOM by loading one model at a time

### OutOfMemoryError During Downloads
**Problem:** App crashes with OOM when downloading large models
**Solution:**
- `largeHeap="true"` in AndroidManifest (512MB heap)
- Separate `@DownloadClient` OkHttpClient without body logging
- 4KB buffer size to reduce memory pressure
- Automatic garbage collection every 10%

### OpenAI API Parameter Errors
**Problem:** "Unsupported parameter: 'max_tokens'" or "Unsupported value: 'temperature'"
**Solution:**
- GPT-5/GPT-4.1 use `max_completion_tokens` instead of `max_tokens`
- Reasoning models (o1/o3) don't support `temperature`/`top_p`
- Detection logic automatically handles these differences

### ProGuard Build Issues
**Problem:** Release build crashes due to obfuscation
**Solution:**
- Comprehensive keep rules for Hilt, Llamatik, Gson, Room
- Native method preservation for JNI
- Error logs preserved, debug logs removed

## ❓ FAQ

**Q: How much does this cost?**
A: The app is free and open source. You only pay for API usage directly to providers (e.g., OpenAI, Deepseek). Local models are completely free after download.

**Q: Is my data safe?**
A: Yes. Everything is stored locally on your device. API keys are encrypted with Android Keystore. No data is sent to our servers (we don't have any).

**Q: Can I use this offline?**
A: Yes! Download Qwen 2.5 (950MB) or Llama 3.2 (1.9GB) and chat completely offline. No internet required.

**Q: Which API provider is best?**
A: 
- **Deepseek** - Best price/performance ratio
- **OpenAI GPT-4o** - Highest quality
- **x.ai Grok** - Fast reasoning
- **Local models** - Privacy (offline), free after download

**Q: Why are local models crashing?**
A: Ensure you're on the latest version. We've added:
- Thread-safe model loading (Mutex)
- Automatic corruption detection
- Download queue system
- Memory optimization (largeHeap)

**Q: Can I contribute?**
A: Absolutely! Fork the repo, make changes, and submit a PR. All contributions welcome.

**Q: Will this be on Google Play?**
A: Yes, once we reach stable 1.0. For now, download APK from GitHub Releases.

## 📄 License

This project is licensed under the **Apache License 2.0** - see the [LICENSE](LICENSE) file for details.

### Why Apache 2.0?
- ✅ Free for personal and commercial use
- ✅ Modification and distribution allowed
- ✅ Patent protection
- ✅ Compatible with Google Play and F-Droid

## 🙏 Acknowledgments

- [llama.cpp](https://github.com/ggerganov/llama.cpp) - Foundational local LLM inference
- [Llamatik](https://github.com/diegoberaldin/Llamatik) - Kotlin wrapper for llama.cpp on Android
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Modern declarative UI
- [Deepseek](https://www.deepseek.com/) - Affordable, high-quality AI models
- [Hugging Face](https://huggingface.co/) - GGUF model hosting
- The open source community for making this possible

## 📞 Support

- **Issues:** [GitHub Issues](https://github.com/yourusername/YourOwnAI/issues)
- **Discussions:** [GitHub Discussions](https://github.com/yourusername/YourOwnAI/discussions)
- **Email:** yourown.ai.dev@gmail.com (if needed)

## ⚠️ Disclaimer

This application allows unrestricted AI interactions. Users are responsible for:
- Their own API usage and costs
- Compliance with AI provider terms of service
- Legal and ethical use of the software
- Content generated by AI models

The developers assume no liability for how this software is used.

---

## 📚 Documentation

- [ARCHITECTURE.md](ARCHITECTURE.md) - Detailed architecture and design patterns
- [CONTRIBUTING.md](CONTRIBUTING.md) - How to contribute to the project
- [SECURITY.md](SECURITY.md) - Security best practices and compliance
- [CHANGELOG.md](CHANGELOG.md) - Version history and changes
- [CHAT_IMPLEMENTATION_PLAN.md](CHAT_IMPLEMENTATION_PLAN.md) - Chat feature specifications
- [LLAMA_CPP_INTEGRATION.md](LLAMA_CPP_INTEGRATION.md) - Local model integration details

---

**Made with ❤️ for privacy-conscious humans who believe in digital freedom**

*"Your data. Your AI. Your rules."*

