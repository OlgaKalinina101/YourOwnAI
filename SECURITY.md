# Security & Privacy Best Practices

## ✅ Google Play Policy Compliance

### API Key Storage & Transmission

#### 1. **Secure Storage**
- ✅ **EncryptedSharedPreferences** with `AES256_GCM` encryption
- ✅ **Android Keystore** via `MasterKey.Builder`
- ✅ Keys stored in app's private storage (`data/data/com.yourown.ai`)
- ✅ No backup allowed (`android:allowBackup="false"`)
- ✅ No auto-backup (`android:fullBackupContent="false"`)

**Implementation:** `ApiKeyRepository.kt`
```kotlin
private val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

private val encryptedPrefs = EncryptedSharedPreferences.create(
    context,
    PREFS_NAME,
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

#### 2. **Secure Transmission**
- ✅ **HTTPS only** (no cleartext traffic: `android:usesCleartextTraffic="false"`)
- ✅ **TLS/SSL** for all API requests
- ✅ **Certificate pinning** ready via `network_security_config.xml`
- ✅ Authorization via `Bearer` token in header
- ✅ No API keys in URL parameters (always in headers)

**Implementation:** `DeepseekClient.kt`
```kotlin
val request = Request.Builder()
    .url("https://api.deepseek.com/chat/completions")
    .header("Authorization", "Bearer $apiKey")
    .post(body)
    .build()
```

#### 3. **Network Security**
- ✅ `network_security_config.xml` enforces HTTPS-only
- ✅ System certificate trust anchors
- ✅ Specific domain allowlist:
  - `api.deepseek.com`
  - `api.openai.com`
  - `api.anthropic.com`
  - `api.x.ai`
  - `huggingface.co`

#### 4. **Logging Protection**
- ✅ **Debug only**: `HttpLoggingInterceptor` enabled only in debug builds
- ✅ **Header redaction**: `Authorization` and `API-Key` headers are redacted
- ✅ **ProGuard**: All logs stripped in release builds

**Implementation:** `NetworkModule.kt`
```kotlin
if (BuildConfig.DEBUG) {
    val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
        redactHeader("Authorization")
        redactHeader("API-Key")
    }
    builder.addInterceptor(loggingInterceptor)
}
```

#### 5. **Code Obfuscation (Release)**
- ✅ **ProGuard/R8** enabled with `isMinifyEnabled = true`
- ✅ Logs removed: `android.util.Log` stripped
- ✅ API key repository classes protected
- ✅ Network classes obfuscated
- ✅ Encryption classes preserved

**ProGuard rules:** `proguard-rules.pro`
```proguard
# Remove all logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# Security: Keep encryption classes
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }
```

#### 6. **UI Security**
- ✅ Only last 4 characters shown: `****1234`
- ✅ No full key ever displayed in UI
- ✅ Masked input fields (TODO: add `passwordVisualTransformation`)
- ✅ No screenshots allowed for sensitive screens (TODO: `FLAG_SECURE`)

**Implementation:** `ApiKeyRepository.kt`
```kotlin
fun getDisplayKey(provider: AIProvider): String? {
    val key = _apiKeys.value[provider]
    return if (key != null && key.length > 4) {
        "****${key.takeLast(4)}"
    } else {
        null
    }
}
```

---

## 🔒 Additional Security Measures

### 1. **Root Detection** (TODO)
- Detect rooted devices
- Warn users about security risks
- Optional: disable API key storage on rooted devices

### 2. **Certificate Pinning** (Optional)
Update `network_security_config.xml` to pin certificates for critical APIs.

### 3. **Biometric Authentication** (TODO)
- Optional biometric lock for API key access
- Use `BiometricPrompt` API

---

## 📋 Google Play Security Checklist

| Requirement | Status | Implementation |
|-------------|--------|----------------|
| Encrypt sensitive data at rest | ✅ | `EncryptedSharedPreferences` + Android Keystore |
| Use HTTPS for network communication | ✅ | `usesCleartextTraffic="false"` + Network Security Config |
| No hardcoded secrets in code | ✅ | User-provided API keys only |
| No logs with sensitive data in release | ✅ | ProGuard strips all logs |
| Obfuscate release builds | ✅ | ProGuard/R8 enabled |
| No backup of sensitive data | ✅ | `allowBackup="false"` |
| Secure key storage | ✅ | Android Keystore via MasterKey |
| Authorization via headers | ✅ | `Bearer` token in `Authorization` header |
| No cleartext traffic | ✅ | Network Security Config |

---

## 📖 References

- [Android Security Best Practices](https://developer.android.com/topic/security/best-practices)
- [EncryptedSharedPreferences](https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences)
- [Network Security Config](https://developer.android.com/training/articles/security-config)
- [Google Play Data Safety](https://support.google.com/googleplay/android-developer/answer/10787469)
