# Native llama.cpp Integration Guide

## Current Status
✅ **Architecture implemented** - Service layer, JNI wrapper, and integration with ChatViewModel complete
⏳ **Native library pending** - llama.cpp Android bindings need to be added

## What's Working Now
The app is fully functional with a **fallback implementation**:
- Model loading/unloading simulation
- Message history management with configurable limit
- System prompt and user context integration
- Temperature and Top-P parameter support
- Streaming response simulation
- Request logs with all parameters

## Adding Real llama.cpp Support

### Option 1: Use Pre-built Library (Recommended)
1. Download llama.cpp Android bindings from:
   - https://github.com/ggerganov/llama.cpp/tree/master/examples/android
   - Or use community builds like `llama-android`

2. Add native libraries to `app/src/main/jniLibs/`:
   ```
   app/src/main/jniLibs/
   ├── arm64-v8a/
   │   └── libllama-android.so
   ├── armeabi-v7a/
   │   └── libllama-android.so
   ├── x86/
   │   └── libllama-android.so
   └── x86_64/
       └── libllama-android.so
   ```

3. Update `LlamaCppWrapper.kt` to remove fallback and implement real JNI methods

### Option 2: Build from Source
1. Install Android NDK
2. Clone llama.cpp:
   ```bash
   git clone https://github.com/ggerganov/llama.cpp.git
   ```

3. Build for Android using CMake
4. Copy `.so` files to `jniLibs` folders

### Option 3: Use llama.cpp Alternative
Consider using alternatives like:
- **Transformers.js** (via WebView)
- **ONNX Runtime Mobile**
- **TensorFlow Lite with custom ops**

## Current Implementation Details

### LlamaCppWrapper.kt
- JNI method declarations for model loading/generation
- Fallback text generation for testing
- Error handling and logging

### LlamaServiceImpl.kt
- Model lifecycle management
- Conversation history building (respects `messageHistoryLimit`)
- System prompt and context injection
- Parameter passthrough (temperature, top-p)
- Streaming response with Flow
- Request logging

### ChatViewModel Integration
- Auto-loads model when selected
- Streams responses word-by-word
- Saves all parameters in message metadata
- Shows "Thinking..." during generation
- Error handling with error messages

## Testing Without Native Library
The app works perfectly for UI/UX testing:
1. Download models (files are saved)
2. Select model (loads successfully with fallback)
3. Send messages (gets simulated responses)
4. View request logs (shows all parameters)
5. All features work except real AI generation

## Next Steps
1. ✅ UI/UX完成测试
2. ✅ Settings integration working
3. ✅ Message history limiting working
4. ⏳ Add real llama.cpp bindings
5. ⏳ Test with actual GGUF models

## Performance Notes
When adding real llama.cpp:
- First load takes 5-30 seconds (model loading)
- Generation: ~1-5 tokens/second on mobile
- Context size: 2048 tokens (configurable)
- Memory: ~1-3GB depending on model

## Model Format
Both Qwen and Llama models should be:
- Format: GGUF (not GGML)
- Quantization: Q4_K_M or Q5_K_M recommended
- Size: 1-3GB for mobile devices
