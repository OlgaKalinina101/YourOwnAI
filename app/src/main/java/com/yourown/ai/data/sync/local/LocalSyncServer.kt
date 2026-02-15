package com.yourown.ai.data.sync.local

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.type.Date
import com.yourown.ai.data.local.preferences.SettingsManager
import com.yourown.ai.data.repository.ConversationRepository
import com.yourown.ai.data.repository.MemoryRepository
import com.yourown.ai.data.repository.MessageRepository
import com.yourown.ai.data.repository.PersonaRepository
import com.yourown.ai.data.repository.SystemPromptRepository
import com.yourown.ai.data.repository.AIConfigRepository
import com.yourown.ai.data.repository.KnowledgeDocumentRepository
import com.yourown.ai.data.sync.local.models.*
import com.yourown.ai.domain.model.AIConfig
import com.yourown.ai.domain.model.AIProvider
import com.yourown.ai.domain.model.FileAttachment
import com.yourown.ai.domain.model.Message
import com.yourown.ai.domain.model.MessageRole
import com.yourown.ai.domain.model.ModelCapabilities
import com.yourown.ai.domain.model.ModelProvider
import com.yourown.ai.domain.service.AIService
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.netty.handler.timeout.WriteTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.concurrent.ConcurrentHashMap

/**
 * Local Network Sync Server using Ktor
 * Runs HTTP server on local network for Desktop/Web sync
 */
class LocalSyncServer(
    private val context: Context,
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val memoryRepository: MemoryRepository,
    private val personaRepository: PersonaRepository,
    private val systemPromptRepository: SystemPromptRepository,
    private val aiConfigRepository: AIConfigRepository,
    private val knowledgeDocumentRepository: KnowledgeDocumentRepository,
    private val settingsManager: SettingsManager,
    private val aiService: AIService,
    private val deviceId: String,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "LocalSyncServer"
        private const val DEFAULT_PORT = 8765
        private const val SERVICE_TYPE = "_yourown-ai._tcp"
    }
    
    private var server: NettyApplicationEngine? = null
    private var currentPort: Int = DEFAULT_PORT
    private val processedClientMessageIds = ConcurrentHashMap<String, Long>()
    
    /**
     * Start HTTP server
     */
    fun start(port: Int = DEFAULT_PORT): Boolean {
        return try {
            if (server != null) {
                Log.w(TAG, "Server already running")
                return false
            }
            
            currentPort = port
            
            server = embeddedServer(Netty, port = port) {
                install(ContentNegotiation) {
                    gson {
                        setPrettyPrinting()
                    }
                }
                
                install(CORS) {
                    anyHost()
                    allowHeader(HttpHeaders.ContentType)
                    allowHeader(HttpHeaders.Authorization)
                    allowMethod(HttpMethod.Get)
                    allowMethod(HttpMethod.Post)
                    allowMethod(HttpMethod.Put)
                    allowMethod(HttpMethod.Delete)
                    allowMethod(HttpMethod.Options)
                }
                
                routing {
                    // ========== WEB UI ENDPOINTS ==========
                    
                    // Serve web interface
                    get("/") {
                        call.respondText(
                            """
                            <!DOCTYPE html>
                            <html lang="en">
                            <head>
                                <meta charset="UTF-8">
                                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                <title>YourOwnAI - View Only</title>
                                <style>
                                    /* Material Design 3 Colors (Light Theme) */
                                    :root {
                                        --app-font-scale: 1;
                                        --md-sys-color-primary: #1E88E5;
                                        --md-sys-color-on-primary: #FFFFFF;
                                        --md-sys-color-primary-container: #D1E4FF;
                                        --md-sys-color-on-primary-container: #001D36;
                                        --md-sys-color-secondary: #526070;
                                        --md-sys-color-on-secondary: #FFFFFF;
                                        --md-sys-color-secondary-container: #D5E4F7;
                                        --md-sys-color-on-secondary-container: #102130;
                                        --md-sys-color-surface: #FAFCFF;
                                        --md-sys-color-surface-variant: #DFE2EB;
                                        --md-sys-color-on-surface: #1A1C1E;
                                        --md-sys-color-on-surface-variant: #43474E;
                                        --md-sys-color-outline: #73777F;
                                        --md-sys-color-outline-variant: #C3C7CF;
                                        --md-sys-color-background: #FAFCFF;
                                        --md-sys-color-error: #BA1A1A;
                                        --md-sys-color-error-container: #FFDAD6;
                                        --md-elevation-1: 0 1px 2px rgba(0,0,0,0.1), 0 1px 3px rgba(0,0,0,0.08);
                                        --md-elevation-2: 0 2px 4px rgba(0,0,0,0.1), 0 3px 4px rgba(0,0,0,0.08);
                                        --md-elevation-3: 0 4px 8px rgba(0,0,0,0.12), 0 6px 20px rgba(0,0,0,0.08);
                                    }

                                    body[data-theme="dark"] {
                                        --md-sys-color-primary: #90CAF9;
                                        --md-sys-color-on-primary: #0D2236;
                                        --md-sys-color-primary-container: #1C2A38;
                                        --md-sys-color-on-primary-container: #D1E4FF;
                                        --md-sys-color-secondary: #A9B8C9;
                                        --md-sys-color-on-secondary: #1C2A38;
                                        --md-sys-color-secondary-container: #2B3A4A;
                                        --md-sys-color-on-secondary-container: #DCE8F5;
                                        --md-sys-color-surface: #121417;
                                        --md-sys-color-surface-variant: #20252B;
                                        --md-sys-color-on-surface: #E6E9ED;
                                        --md-sys-color-on-surface-variant: #B2B8C0;
                                        --md-sys-color-outline: #7D8590;
                                        --md-sys-color-outline-variant: #3B424B;
                                        --md-sys-color-background: #0F1215;
                                    }

                                    body[data-color-style="neutral"] {
                                        --md-sys-color-primary: #6B7280;
                                        --md-sys-color-primary-container: #E5E7EB;
                                        --md-sys-color-on-primary-container: #111827;
                                        --md-sys-color-secondary: #6B7280;
                                        --md-sys-color-secondary-container: #E5E7EB;
                                        --md-sys-color-on-secondary-container: #111827;
                                    }

                                    body[data-theme="dark"][data-color-style="neutral"] {
                                        --md-sys-color-primary: #9CA3AF;
                                        --md-sys-color-on-primary: #111827;
                                        --md-sys-color-primary-container: #374151;
                                        --md-sys-color-on-primary-container: #E5E7EB;
                                        --md-sys-color-secondary: #9CA3AF;
                                        --md-sys-color-on-secondary: #111827;
                                        --md-sys-color-secondary-container: #374151;
                                        --md-sys-color-on-secondary-container: #E5E7EB;
                                    }
                                    
                                    * { margin: 0; padding: 0; box-sizing: border-box; }
                                    
                                    body { 
                                        font-family: 'Roboto', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                                        background: var(--md-sys-color-background);
                                        color: var(--md-sys-color-on-surface);
                                        -webkit-font-smoothing: antialiased;
                                        font-size: calc(16px * var(--app-font-scale));
                                    }
                                    
                                    .app { 
                                        display: flex; 
                                        height: 100vh;
                                        overflow: hidden;
                                    }
                                    
                                    /* Sidebar */
                                    .sidebar { 
                                        width: 320px; 
                                        background: var(--md-sys-color-surface);
                                        border-right: 1px solid var(--md-sys-color-outline-variant);
                                        display: flex;
                                        flex-direction: column;
                                    }
                                    
                                    .sidebar-header {
                                        padding: 24px 20px;
                                        border-bottom: 1px solid var(--md-sys-color-outline-variant);
                                        background: var(--md-sys-color-surface);
                                    }
                                    
                                    .sidebar-header h1 { 
                                        font-size: 22px;
                                        font-weight: 500;
                                        margin-bottom: 4px;
                                        color: var(--md-sys-color-on-surface);
                                        letter-spacing: 0.15px;
                                    }
                                    
                                    .device-name { 
                                        font-size: 13px;
                                        color: var(--md-sys-color-on-surface-variant);
                                        font-weight: 400;
                                    }
                                    
                                    .search-box { 
                                        padding: 16px;
                                        border-bottom: 1px solid var(--md-sys-color-outline-variant);
                                    }
                                    
                                    .search-input {
                                        width: 100%;
                                        padding: 12px 16px;
                                        border: 1px solid var(--md-sys-color-outline);
                                        border-radius: 28px;
                                        font-size: 14px;
                                        font-family: inherit;
                                        background: var(--md-sys-color-surface-variant);
                                        color: var(--md-sys-color-on-surface);
                                        transition: all 0.2s;
                                    }
                                    
                                    .search-input:focus { 
                                        outline: none;
                                        border-color: var(--md-sys-color-primary);
                                        background: var(--md-sys-color-surface);
                                        box-shadow: var(--md-elevation-1);
                                    }
                                    
                                    .memories-btn {
                                        margin: 16px;
                                        padding: 14px 20px;
                                        background: var(--md-sys-color-secondary-container);
                                        border: none;
                                        border-radius: 12px;
                                        cursor: pointer;
                                        font-size: 14px;
                                        font-weight: 500;
                                        text-align: left;
                                        color: var(--md-sys-color-on-secondary-container);
                                        transition: all 0.2s;
                                        box-shadow: var(--md-elevation-1);
                                    }
                                    
                                    .memories-btn:hover { 
                                        background: #C0D4ED;
                                        box-shadow: var(--md-elevation-2);
                                    }
                                    
                                    .memories-btn.active { 
                                        background: var(--md-sys-color-primary);
                                        color: var(--md-sys-color-on-primary);
                                        box-shadow: var(--md-elevation-2);
                                    }

                                    body[data-theme="dark"] .memories-btn:hover {
                                        background: #314253;
                                        color: #E6E9ED;
                                    }

                                    body[data-theme="dark"] .memories-btn.active {
                                        background: #37506A;
                                        color: #F3F7FC;
                                    }
                                    
                                    .conversation-list {
                                        flex: 1;
                                        overflow-y: auto;
                                        padding: 8px;
                                    }
                                    
                                    .conversation-list::-webkit-scrollbar { width: 8px; }
                                    .conversation-list::-webkit-scrollbar-track { background: transparent; }
                                    .conversation-list::-webkit-scrollbar-thumb { 
                                        background: var(--md-sys-color-outline-variant);
                                        border-radius: 4px;
                                    }
                                    
                                    .conversation-item {
                                        padding: 14px 16px;
                                        margin-bottom: 4px;
                                        border-radius: 12px;
                                        cursor: pointer;
                                        transition: all 0.2s;
                                        background: transparent;
                                    }
                                    
                                    .conversation-item:hover { 
                                        background: var(--md-sys-color-surface-variant);
                                    }
                                    
                                    .conversation-item.active { 
                                        background: var(--md-sys-color-primary-container);
                                        color: var(--md-sys-color-on-primary-container);
                                    }

                                    body[data-theme="dark"] .conversation-item.active {
                                        background: #2D445B;
                                        color: #F3F7FC;
                                    }
                                    
                                    .conversation-title { 
                                        font-size: 15px;
                                        font-weight: 500;
                                        margin-bottom: 4px;
                                        overflow: hidden;
                                        text-overflow: ellipsis;
                                        white-space: nowrap;
                                        letter-spacing: 0.1px;
                                    }
                                    
                                    .conversation-date { 
                                        font-size: 12px;
                                        opacity: 0.7;
                                        font-weight: 400;
                                    }
                                    
                                    .sidebar-footer {
                                        padding: 16px;
                                        border-top: 1px solid var(--md-sys-color-outline-variant);
                                        font-size: 12px;
                                        color: var(--md-sys-color-on-surface-variant);
                                        background: var(--md-sys-color-surface);
                                    }
                                    
                                    .status-dot-container {
                                        display: flex;
                                        align-items: center;
                                        gap: 8px;
                                    }
                                    
                                    .status-dot {
                                        width: 8px;
                                        height: 8px;
                                        border-radius: 50%;
                                        background: #4CAF50;
                                        animation: pulse 2s infinite;
                                    }
                                    
                                    @keyframes pulse {
                                        0%, 100% { opacity: 1; transform: scale(1); }
                                        50% { opacity: 0.7; transform: scale(0.95); }
                                    }
                                    
                                    /* Chat Area */
                                    .chat-area { 
                                        flex: 1;
                                        display: flex;
                                        flex-direction: column;
                                        background: var(--md-sys-color-background);
                                    }
                                    
                                    .chat-header {
                                        padding: 20px 24px;
                                        background: var(--md-sys-color-surface);
                                        border-bottom: 1px solid var(--md-sys-color-outline-variant);
                                        display: flex;
                                        justify-content: space-between;
                                        align-items: center;
                                        box-shadow: var(--md-elevation-1);
                                        z-index: 10;
                                    }
                                    
                                    .chat-title { 
                                        font-size: 20px;
                                        font-weight: 500;
                                        color: var(--md-sys-color-on-surface);
                                        letter-spacing: 0.15px;
                                    }
                                    
                                    .header-actions { 
                                        display: flex;
                                        gap: 8px;
                                        align-items: center;
                                    }
                                    
                                    .icon-btn {
                                        padding: 8px 16px;
                                        background: var(--md-sys-color-surface-variant);
                                        border: none;
                                        border-radius: 20px;
                                        cursor: pointer;
                                        font-size: 13px;
                                        font-weight: 500;
                                        color: var(--md-sys-color-on-surface-variant);
                                        transition: all 0.2s;
                                    }
                                    
                                    .icon-btn:hover { 
                                        background: var(--md-sys-color-secondary-container);
                                        box-shadow: var(--md-elevation-1);
                                    }
                                    
                                    .messages-container {
                                        flex: 1;
                                        overflow-y: auto;
                                        padding: 24px;
                                        background: var(--md-sys-color-background);
                                        scroll-behavior: smooth;
                                    }
                                    
                                    .messages-container::-webkit-scrollbar { width: 8px; }
                                    .messages-container::-webkit-scrollbar-track { background: transparent; }
                                    .messages-container::-webkit-scrollbar-thumb { 
                                        background: var(--md-sys-color-outline-variant);
                                        border-radius: 4px;
                                    }
                                    
                                    .message {
                                        display: flex;
                                        gap: 12px;
                                        margin-bottom: 20px;
                                        animation: slideUp 0.3s ease-out;
                                    }
                                    
                                    @keyframes slideUp {
                                        from { opacity: 0; transform: translateY(10px); }
                                        to { opacity: 1; transform: translateY(0); }
                                    }
                                    
                                    .message.user { flex-direction: row-reverse; }
                                    
                                    .message-avatar {
                                        width: 40px;
                                        height: 40px;
                                        border-radius: 50%;
                                        background: var(--md-sys-color-primary);
                                        color: var(--md-sys-color-on-primary);
                                        display: flex;
                                        align-items: center;
                                        justify-content: center;
                                        font-weight: 500;
                                        font-size: 18px;
                                        flex-shrink: 0;
                                        box-shadow: var(--md-elevation-1);
                                    }
                                    
                                    .message.assistant .message-avatar { 
                                        background: var(--md-sys-color-secondary);
                                        color: var(--md-sys-color-on-secondary);
                                    }
                                    
                                    .message-wrapper { 
                                        flex: 1;
                                        max-width: 75%;
                                        display: flex;
                                        flex-direction: column;
                                    }
                                    
                                    .message-content {
                                        padding: 14px 18px;
                                        border-radius: 20px;
                                        background: var(--md-sys-color-surface-variant);
                                        line-height: 1.6;
                                        white-space: pre-wrap;
                                        word-wrap: break-word;
                                        color: var(--md-sys-color-on-surface);
                                        font-size: 15px;
                                        box-shadow: var(--md-elevation-1);
                                    }
                                    
                                    .message.user .message-content { 
                                        background: var(--md-sys-color-primary);
                                        color: var(--md-sys-color-on-primary);
                                        border-top-right-radius: 4px;
                                    }
                                    
                                    .message.assistant .message-content {
                                        border-top-left-radius: 4px;
                                    }
                                    
                                    .message-footer {
                                        display: flex;
                                        align-items: center;
                                        gap: 8px;
                                        margin-top: 6px;
                                        padding: 0 4px;
                                        font-size: 12px;
                                        color: var(--md-sys-color-on-surface-variant);
                                    }
                                    
                                    .message.user .message-footer { justify-content: flex-end; }
                                    
                                    .copy-btn {
                                        background: none;
                                        border: none;
                                        cursor: pointer;
                                        padding: 6px;
                                        border-radius: 50%;
                                        font-size: 16px;
                                        opacity: 0.6;
                                        transition: all 0.2s;
                                    }
                                    
                                    .copy-btn:hover {
                                        opacity: 1;
                                        background: var(--md-sys-color-surface-variant);
                                    }
                                    
                                    /* Input Area */
                                    .input-area {
                                        padding: 16px;
                                        background: var(--md-sys-color-surface);
                                        border-top: 1px solid var(--md-sys-color-outline-variant);
                                    }
                                    
                                    .input-wrapper {
                                        display: flex;
                                        gap: 12px;
                                        align-items: flex-end;
                                    }
                                    
                                    .message-input {
                                        flex: 1;
                                        padding: 12px 16px;
                                        border: 1px solid var(--md-sys-color-outline);
                                        border-radius: 24px;
                                        font-size: 15px;
                                        font-family: inherit;
                                        resize: none;
                                        max-height: 200px;
                                        min-height: 48px;
                                        background: var(--md-sys-color-surface-variant);
                                        color: var(--md-sys-color-on-surface);
                                        line-height: 1.5;
                                    }
                                    
                                    .message-input:focus {
                                        outline: none;
                                        border-color: var(--md-sys-color-primary);
                                        background: var(--md-sys-color-surface);
                                        box-shadow: var(--md-elevation-1);
                                    }
                                    
                                    .send-button {
                                        padding: 12px 28px;
                                        background: var(--md-sys-color-primary);
                                        color: var(--md-sys-color-on-primary);
                                        border: none;
                                        border-radius: 24px;
                                        font-size: 15px;
                                        font-weight: 500;
                                        cursor: pointer;
                                        transition: all 0.2s;
                                        height: 48px;
                                        box-shadow: var(--md-elevation-2);
                                    }
                                    
                                    .send-button:hover:not(:disabled) {
                                        background: #1976D2;
                                        box-shadow: var(--md-elevation-3);
                                    }
                                    
                                    .send-button:disabled {
                                        opacity: 0.5;
                                        cursor: not-allowed;
                                        box-shadow: none;
                                    }
                                    
                                    /* Attachment Controls */
                                    .attachment-controls {
                                        display: flex;
                                        gap: 8px;
                                        margin-bottom: 8px;
                                        flex-wrap: wrap;
                                    }

                                    .chat-config-row {
                                        display: flex;
                                        gap: 8px;
                                        align-items: center;
                                        flex-wrap: wrap;
                                    }

                                    .readonly-chip {
                                        font-size: 12px;
                                        color: var(--md-sys-color-on-surface-variant);
                                        padding: 6px 10px;
                                        background: var(--md-sys-color-surface-variant);
                                        border-radius: 12px;
                                    }

                                    .config-select {
                                        padding: 8px 12px;
                                        border: 1px solid var(--md-sys-color-outline);
                                        border-radius: 12px;
                                        background: var(--md-sys-color-surface);
                                        color: var(--md-sys-color-on-surface);
                                        font-size: 13px;
                                    }

                                    .capability-hint {
                                        font-size: 12px;
                                        color: var(--md-sys-color-on-surface-variant);
                                        padding: 6px 10px;
                                        background: var(--md-sys-color-surface-variant);
                                        border-radius: 12px;
                                    }

                                    .capability-badge {
                                        display: inline-flex;
                                        flex-direction: column;
                                        gap: 2px;
                                        padding: 8px 10px;
                                        background: #E8F2FF;
                                        border: 1px solid #B7D6FF;
                                        border-radius: 12px;
                                        max-width: 260px;
                                    }

                                    .capability-title {
                                        font-size: 12px;
                                        font-weight: 600;
                                        color: #0B5394;
                                        line-height: 1.2;
                                    }

                                    .capability-limits {
                                        font-size: 11px;
                                        color: #2E6BA8;
                                        line-height: 1.25;
                                        white-space: nowrap;
                                        overflow: hidden;
                                        text-overflow: ellipsis;
                                    }
                                    
                                    .attach-btn {
                                        padding: 8px 12px;
                                        background: var(--md-sys-color-surface-variant);
                                        border: 1px solid var(--md-sys-color-outline);
                                        border-radius: 20px;
                                        cursor: pointer;
                                        font-size: 13px;
                                        color: var(--md-sys-color-on-surface-variant);
                                        transition: all 0.2s;
                                        display: flex;
                                        align-items: center;
                                        gap: 6px;
                                    }
                                    
                                    .attach-btn:hover {
                                        background: var(--md-sys-color-secondary-container);
                                        box-shadow: var(--md-elevation-1);
                                    }
                                    
                                    .attach-btn input {
                                        display: none;
                                    }
                                    
                                    .attachments-preview {
                                        display: flex;
                                        gap: 8px;
                                        flex-wrap: wrap;
                                        margin-bottom: 8px;
                                    }
                                    
                                    .attachment-chip {
                                        display: flex;
                                        align-items: center;
                                        gap: 6px;
                                        padding: 6px 12px;
                                        background: var(--md-sys-color-primary-container);
                                        border-radius: 16px;
                                        font-size: 13px;
                                        color: var(--md-sys-color-on-primary-container);
                                    }

                                    .image-thumb {
                                        width: 24px;
                                        height: 24px;
                                        border-radius: 6px;
                                        object-fit: cover;
                                        border: 1px solid rgba(0,0,0,0.08);
                                    }

                                    .file-thumb {
                                        width: 24px;
                                        height: 24px;
                                        border-radius: 6px;
                                        display: flex;
                                        align-items: center;
                                        justify-content: center;
                                        background: rgba(0,0,0,0.08);
                                        font-size: 14px;
                                    }
                                    
                                    .attachment-chip button {
                                        background: none;
                                        border: none;
                                        cursor: pointer;
                                        padding: 2px;
                                        color: inherit;
                                        opacity: 0.7;
                                        font-size: 16px;
                                        line-height: 1;
                                    }
                                    
                                    .attachment-chip button:hover {
                                        opacity: 1;
                                    }
                                    
                                    .toggle-switch {
                                        display: flex;
                                        align-items: center;
                                        gap: 8px;
                                        padding: 8px 12px;
                                        background: var(--md-sys-color-surface-variant);
                                        border-radius: 20px;
                                        cursor: pointer;
                                        font-size: 13px;
                                        user-select: none;
                                    }
                                    
                                    .toggle-switch.active {
                                        background: var(--md-sys-color-primary-container);
                                        color: var(--md-sys-color-on-primary-container);
                                    }
                                    
                                    .view-only-notice {
                                        padding: 16px;
                                        background: var(--md-sys-color-surface);
                                        border-top: 1px solid var(--md-sys-color-outline-variant);
                                    }
                                    
                                    .notice-content {
                                        display: flex;
                                        align-items: center;
                                        justify-content: center;
                                        gap: 10px;
                                        padding: 14px 20px;
                                        background: #FFF4E5;
                                        border: 1px solid #FFB74D;
                                        border-radius: 12px;
                                        font-size: 14px;
                                        color: #E65100;
                                        font-weight: 500;
                                    }
                                    
                                    .notice-icon { font-size: 20px; }

                                    .snackbar {
                                        position: fixed;
                                        left: 50%;
                                        bottom: 18px;
                                        transform: translateX(-50%);
                                        z-index: 10000;
                                        background: #323232;
                                        color: #fff;
                                        border-radius: 10px;
                                        padding: 10px 14px;
                                        font-size: 13px;
                                        box-shadow: var(--md-elevation-3);
                                        opacity: 0;
                                        pointer-events: none;
                                        transition: opacity 0.2s ease;
                                        max-width: 88vw;
                                        white-space: nowrap;
                                        overflow: hidden;
                                        text-overflow: ellipsis;
                                    }

                                    .snackbar.show {
                                        opacity: 1;
                                    }
                                    
                                    /* Code block with copy button */
                                    .code-block-wrapper {
                                        position: relative;
                                        margin: 8px 0;
                                    }
                                    
                                    .code-block-wrapper pre {
                                        margin: 0;
                                    }
                                    
                                    .code-copy-btn {
                                        position: absolute;
                                        top: 8px;
                                        right: 8px;
                                        background: rgba(255,255,255,0.1);
                                        border: 1px solid rgba(255,255,255,0.2);
                                        border-radius: 6px;
                                        padding: 4px 8px;
                                        font-size: 12px;
                                        cursor: pointer;
                                        opacity: 0;
                                        transition: opacity 0.2s, background 0.2s;
                                        color: inherit;
                                    }
                                    
                                    .code-block-wrapper:hover .code-copy-btn {
                                        opacity: 1;
                                    }
                                    
                                    .code-copy-btn:hover {
                                        background: rgba(255,255,255,0.2);
                                    }
                                    
                                    .code-copy-btn:active {
                                        transform: scale(0.95);
                                    }
                                    
                                    .empty-state {
                                        display: flex;
                                        flex-direction: column;
                                        align-items: center;
                                        justify-content: center;
                                        height: 100%;
                                        color: var(--md-sys-color-on-surface-variant);
                                        text-align: center;
                                        padding: 48px;
                                    }
                                    
                                    .empty-state h2 { 
                                        font-size: 28px;
                                        margin-bottom: 12px;
                                        color: var(--md-sys-color-on-surface);
                                        font-weight: 500;
                                        letter-spacing: 0.15px;
                                    }
                                    
                                    .empty-state p {
                                        font-size: 16px;
                                        color: var(--md-sys-color-on-surface-variant);
                                    }
                                    
                                    .loading { 
                                        display: flex;
                                        align-items: center;
                                        justify-content: center;
                                        height: 100vh;
                                        background: var(--md-sys-color-background);
                                        color: var(--md-sys-color-on-surface);
                                        font-size: 18px;
                                        font-weight: 500;
                                    }
                                    
                                    .memories-grid {
                                        display: grid;
                                        grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
                                        gap: 16px;
                                        padding: 4px;
                                    }
                                    
                                    .memory-card {
                                        padding: 18px;
                                        background: var(--md-sys-color-surface);
                                        border: 1px solid var(--md-sys-color-outline-variant);
                                        border-radius: 16px;
                                        transition: all 0.2s;
                                        box-shadow: var(--md-elevation-1);
                                    }
                                    
                                    .memory-card:hover {
                                        box-shadow: var(--md-elevation-3);
                                        transform: translateY(-2px);
                                    }
                                    
                                    .memory-content { 
                                        font-size: 15px;
                                        line-height: 1.6;
                                        margin-bottom: 12px;
                                        color: var(--md-sys-color-on-surface);
                                    }
                                    
                                    .memory-time { 
                                        font-size: 12px;
                                        color: var(--md-sys-color-on-surface-variant);
                                        font-weight: 500;
                                    }
                                    
                                    /* Markdown Elements */
                                    .message-content a {
                                        color: inherit;
                                        text-decoration: underline;
                                        transition: opacity 0.2s;
                                    }
                                    
                                    .message-content a:hover {
                                        opacity: 0.8;
                                    }
                                    
                                    .message-content code {
                                        font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
                                        font-size: 13px;
                                    }
                                    
                                    .message-content strong {
                                        font-weight: 600;
                                    }
                                    
                                    .message-content em {
                                        font-style: italic;
                                    }
                                    
                                    .message-content hr {
                                        margin: 12px 0;
                                    }
                                    
                                    @media (max-width: 768px) {
                                        .sidebar { width: 100%; position: absolute; z-index: 100; }
                                        .message-wrapper { max-width: 90%; }
                                        .memories-grid { grid-template-columns: 1fr; }
                                    }
                                </style>
                            </head>
                            <body>
                                <div id="app" class="loading">⏳ Loading...</div>
                                
                                <script>
                                const app = {
                                    conversations: [],
                                    filteredConversations: [],
                                    selectedConversation: null,
                                    messages: [],
                                    memories: [],
                                    personas: [],
                                    modelCapabilities: null,
                                    appearance: null,
                                    searchQuery: '',
                                    showMemories: false,
                                    serverInfo: null,
                                    inputValue: '',
                                    isSending: false,
                                    webSearchEnabled: false,
                                    imageAttachments: [],
                                    fileAttachments: [],
                                    toastMessage: '',
                                    toastTimer: null,
                                    preserveScrollTop: 0,
                                    scrollThrottleTimer: null,
                                    lastScrollTime: 0,
                                    skipScrollRestore: false,
                                    
                                    async init() {
                                        try {
                                            const status = await fetch('/status').then(r => r.json());
                                            this.serverInfo = status;
                                            this.appearance = await fetch('/api/appearance').then(r => r.json());
                                            this.applyAppearance(this.appearance);
                                            
                                            const conversations = await fetch('/api/conversations').then(r => r.json());
                                            this.conversations = conversations.sort((a, b) => b.updatedAt - a.updatedAt);
                                            this.filteredConversations = this.conversations;
                                            
                                            const memories = await fetch('/memories').then(r => r.json());
                                            this.memories = memories;

                                            this.personas = await fetch('/personas').then(r => r.json());
                                            
                                            this.render();
                                        } catch (e) {
                                            document.getElementById('app').innerHTML = `
                                                <div class="loading">
                                                    <div style="text-align:center;max-width:500px;">
                                                        <h2 style="color:#ef4444;margin-bottom:16px;">❌ Connection Failed</h2>
                                                        <p style="margin-bottom:16px;">Cannot connect to YourOwnAI server.</p>
                                                        <div style="background:#fef3c7;border:1px solid #fbbf24;border-radius:8px;padding:16px;text-align:left;font-size:14px;line-height:1.6;">
                                                            <strong>💡 How to fix:</strong><br><br>
                                                            <strong>Option 1: Mobile Hotspot (Recommended)</strong><br>
                                                            1. On your phone: Settings → Network → Hotspot<br>
                                                            2. Turn on Mobile Hotspot<br>
                                                            3. Connect your desktop to the hotspot<br>
                                                            4. Refresh this page<br><br>
                                                            <strong>Option 2: Same WiFi</strong><br>
                                                            1. Make sure phone and desktop are on the same WiFi network<br>
                                                            2. Check the link copied from your phone<br>
                                                            3. Refresh this page<br><br>
                                                            <em>Note: Local sync works without internet using hotspot!</em>
                                                        </div>
                                                        <button onclick="location.reload()" style="margin-top:16px;padding:10px 20px;background:#2563eb;color:white;border:none;border-radius:6px;cursor:pointer;font-size:14px;">🔄 Retry Connection</button>
                                                    </div>
                                                </div>
                                            `;
                                        }
                                    },
                                    
                                    async selectConversation(conv) {
                                        this.selectedConversation = conv;
                                        this.showMemories = false;
                                        this.webSearchEnabled = !!conv.webSearchEnabled;
                                        const messages = await fetch(`/api/conversations/${'$'}{conv.id}/messages`).then(r => r.json());
                                        this.messages = messages.sort((a, b) => a.createdAt - b.createdAt);
                                        await this.loadCapabilities(conv.model || '');
                                        this.render();
                                        setTimeout(() => {
                                            const container = document.querySelector('.messages-container');
                                            if (container) container.scrollTop = container.scrollHeight;
                                        }, 100);
                                    },
                                    
                                    search(query) {
                                        this.searchQuery = query;
                                        if (!query.trim()) {
                                            this.filteredConversations = this.conversations;
                                        } else {
                                            const q = query.toLowerCase();
                                            this.filteredConversations = this.conversations.filter(c => c.title.toLowerCase().includes(q));
                                        }
                                        this.render();
                                    },
                                    
                                    toggleMemories() {
                                        this.showMemories = !this.showMemories;
                                        this.selectedConversation = null;
                                        this.render();
                                    },

                                    async loadCapabilities(modelId) {
                                        if (!modelId) {
                                            this.modelCapabilities = null;
                                            return;
                                        }
                                        try {
                                            const caps = await fetch(`/api/model-capabilities?modelId=${'$'}{encodeURIComponent(modelId)}`).then(r => r.json());
                                            this.modelCapabilities = caps;
                                            if (!caps.supportsWebSearch) {
                                                this.webSearchEnabled = false;
                                            }
                                            if (!caps.supportsVision) {
                                                this.imageAttachments = [];
                                            }
                                            if (!caps.supportsDocuments) {
                                                this.fileAttachments = [];
                                            }
                                        } catch (e) {
                                            console.error('Capabilities load error:', e);
                                            this.modelCapabilities = null;
                                        }
                                    },

                                    getPersonaName(personaId) {
                                        if (!personaId) return 'No Persona';
                                        const p = this.personas.find(x => x.id === personaId);
                                        return p?.name || 'Persona';
                                    },

                                    applyAppearance(appearance) {
                                        if (!appearance) return;
                                        const mode = (appearance.themeMode || 'SYSTEM').toUpperCase();
                                        const colorStyle = (appearance.colorStyle || 'DYNAMIC').toLowerCase();
                                        const fontStyle = (appearance.fontStyle || 'ROBOTO').toUpperCase();
                                        const fontScale = appearance.fontScale || 1;

                                        let dark = false;
                                        if (mode === 'DARK') dark = true;
                                        else if (mode === 'SYSTEM') dark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;

                                        document.body.setAttribute('data-theme', dark ? 'dark' : 'light');
                                        document.body.setAttribute('data-color-style', colorStyle === 'neutral' ? 'neutral' : 'dynamic');
                                        document.documentElement.style.setProperty('--app-font-scale', String(fontScale));
                                        document.body.style.fontFamily = fontStyle === 'SYSTEM'
                                            ? "system-ui, -apple-system, 'Segoe UI', sans-serif"
                                            : "'Roboto', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif";
                                    },

                                    showToast(message) {
                                        this.toastMessage = message;
                                        this.render();
                                        if (this.toastTimer) {
                                            clearTimeout(this.toastTimer);
                                        }
                                        this.toastTimer = setTimeout(() => {
                                            this.toastMessage = '';
                                            this.render();
                                        }, 1800);
                                    },

                                    captureMessagesScroll() {
                                        const container = document.querySelector('.messages-container');
                                        if (container) {
                                            this.preserveScrollTop = container.scrollTop;
                                        }
                                    },

                                    restoreMessagesScroll() {
                                        if (this.skipScrollRestore) {
                                            this.skipScrollRestore = false;
                                            return;
                                        }
                                        const container = document.querySelector('.messages-container');
                                        if (container && Number.isFinite(this.preserveScrollTop)) {
                                            container.scrollTop = this.preserveScrollTop;
                                        }
                                    },

                                    autoResizeTextarea(el) {
                                        if (!el) return;
                                        el.style.height = 'auto';
                                        const maxHeight = 200;
                                        el.style.height = Math.min(el.scrollHeight, maxHeight) + 'px';
                                        el.style.overflowY = el.scrollHeight > maxHeight ? 'auto' : 'hidden';
                                    },

                                    getFileTypeIcon(fileName) {
                                        const ext = (fileName.split('.').pop() || '').toLowerCase();
                                        if (ext === 'pdf') return '📕';
                                        if (ext === 'txt') return '📄';
                                        if (ext === 'doc' || ext === 'docx') return '📝';
                                        if (ext === 'csv' || ext === 'xls' || ext === 'xlsx') return '📊';
                                        return '📎';
                                    },

                                    getCapabilityBadgeHtml() {
                                        const caps = this.modelCapabilities;
                                        if (!caps) return '';
                                        const vision = caps.supportsVision ? '🖼️ on' : '🖼️ off';
                                        const docs = caps.supportsDocuments ? '📄 on' : '📄 off';
                                        const web = caps.supportsWebSearch ? '🌐 on' : '🌐 off';
                                        const imgLimit = caps.imageSupport?.maxImages ?? '-';
                                        const docLimit = caps.documentSupport?.maxDocuments ?? '-';
                                        const totalLimit = caps.totalAttachmentsLimit ?? 0;
                                        return `
                                            <span class="capability-badge">
                                                <span class="capability-title">Capabilities: ${'$'}{vision} · ${'$'}{docs} · ${'$'}{web}</span>
                                                <span class="capability-limits">Limits: images ${'$'}{imgLimit}, docs ${'$'}{docLimit}, total ${'$'}{totalLimit}</span>
                                            </span>
                                        `;
                                    },

                                    generateClientMessageId() {
                                        if (window.crypto && window.crypto.randomUUID) {
                                            return window.crypto.randomUUID();
                                        }
                                        return `web_${'$'}{Date.now()}_${'$'}{Math.floor(Math.random() * 1000000)}`;
                                    },
                                    
                                    async sendMessage() {
                                        const hasText = !!this.inputValue.trim();
                                        const hasAttachments = this.imageAttachments.length > 0 || this.fileAttachments.length > 0;
                                        if ((!hasText && !hasAttachments) || !this.selectedConversation || this.isSending) return;
                                        
                                        const userContent = this.inputValue.trim() || 'Sent attachments';
                                        this.inputValue = '';
                                        this.isSending = true;
                                        
                                        // Capture and clear attachments BEFORE render so chips disappear immediately
                                        const sentImages = [...this.imageAttachments];
                                        const sentFiles = [...this.fileAttachments];
                                        this.imageAttachments = [];
                                        this.fileAttachments = [];
                                        
                                        // Add user message to UI immediately
                                        const userMessage = {
                                            id: `temp-${'$'}{Date.now()}`,
                                            conversationId: this.selectedConversation.id,
                                            content: userContent,
                                            role: 'USER',
                                            createdAt: Date.now(),
                                            imageAttachments: sentImages,
                                            fileAttachments: sentFiles
                                        };
                                        this.messages.push(userMessage);
                                        this.skipScrollRestore = true;
                                        this.render();
                                        this.scrollToBottom(false); // Instant scroll to new message
                                        
                                        // Add placeholder for assistant
                                        const assistantId = `temp-ai-${'$'}{Date.now()}`;
                                        const assistantMessage = {
                                            id: assistantId,
                                            conversationId: this.selectedConversation.id,
                                            content: '',
                                            role: 'ASSISTANT',
                                            createdAt: Date.now(),
                                            isStreaming: true
                                        };
                                        this.messages.push(assistantMessage);
                                        this.skipScrollRestore = true;
                                        this.render();
                                        
                                        try {
                                            const clientMessageId = this.generateClientMessageId();
                                            const response = await fetch(`/api/conversations/${'$'}{this.selectedConversation.id}/messages`, {
                                                method: 'POST',
                                                headers: { 'Content-Type': 'application/json' },
                                                body: JSON.stringify({
                                                    content: userContent,
                                                    clientMessageId: clientMessageId,
                                                    webSearchEnabled: this.webSearchEnabled,
                                                    imageAttachments: sentImages,
                                                    fileAttachments: sentFiles
                                                })
                                            });
                                            
                                            if (!response.ok || !response.body) {
                                                throw new Error(`HTTP ${'$'}{response.status}`);
                                            }
                                            const reader = response.body.getReader();
                                            const decoder = new TextDecoder();
                                            
                                            while (true) {
                                                const { done, value } = await reader.read();
                                                if (done) break;
                                                
                                                const chunk = decoder.decode(value, {stream: true});
                                                const lines = chunk.split('\n');
                                                
                                                lines.forEach(line => {
                                                    if (line.startsWith('data: ')) {
                                                        try {
                                                            const data = JSON.parse(line.substring(6));
                                                            if (data.chunk) {
                                                                const msgIndex = this.messages.findIndex(m => m.id === assistantId);
                                                                if (msgIndex !== -1) {
                                                                    this.messages[msgIndex].content += data.chunk;
                                                                    // Incremental update: only re-render this specific message content
                                                                    this.updateStreamingMessage(assistantId, this.messages[msgIndex].content);
                                                                    // Smooth scroll with throttling to follow the stream
                                                                    this.scrollToBottomThrottled();
                                                                }
                                                            }
                                                        } catch (e) {}
                                                    }
                                                });
                                            }
                                            
                                            // Remove streaming flag and do final full render
                                            const msgIndex = this.messages.findIndex(m => m.id === assistantId);
                                            if (msgIndex !== -1) {
                                                delete this.messages[msgIndex].isStreaming;
                                            }
                                            this.skipScrollRestore = true;
                                            this.render(); // Final render to clean up any streaming artifacts
                                            this.scrollToBottom(true); // Final scroll to ensure we're at the bottom
                                            
                                        } catch (e) {
                                            console.error('Send error:', e);
                                            this.messages = this.messages.filter(m => m.id !== assistantId);
                                            this.showToast('Send failed: ' + (e.message || 'Unknown error'));
                                        } finally {
                                            this.isSending = false;
                                            this.skipScrollRestore = true;
                                            this.render();
                                        }
                                    },
                                    
                                    async handleImageFiles(input) {
                                        if (this.modelCapabilities && !this.modelCapabilities.supportsVision) return;
                                        const files = Array.from(input.files || []);
                                        for (const file of files) {
                                            const base64 = await this.fileToBase64(file);
                                            this.imageAttachments.push(base64);
                                        }
                                        input.value = '';
                                        this.showToast(`${'$'}{files.length} image(s) attached`);
                                        this.render();
                                    },

                                    async handlePaste(event) {
                                        if (this.modelCapabilities && !this.modelCapabilities.supportsVision) return;
                                        const items = event.clipboardData?.items || [];
                                        let foundImage = false;
                                        for (const item of items) {
                                            if (item.type && item.type.startsWith('image/')) {
                                                const file = item.getAsFile();
                                                if (file) {
                                                    const base64 = await this.fileToBase64(file);
                                                    this.imageAttachments.push(base64);
                                                    foundImage = true;
                                                }
                                            }
                                        }
                                        if (foundImage) {
                                            event.preventDefault();
                                            this.showToast('Screenshot pasted');
                                            this.render();
                                        }
                                    },

                                    async pasteFromClipboard() {
                                        try {
                                            if (!navigator.clipboard || !navigator.clipboard.read) {
                                                this.showToast('Clipboard API unavailable. Use Ctrl+V in input.');
                                                return;
                                            }
                                            const items = await navigator.clipboard.read();
                                            let count = 0;
                                            for (const item of items) {
                                                const imgType = item.types.find(t => t.startsWith('image/'));
                                                if (imgType) {
                                                    const blob = await item.getType(imgType);
                                                    const base64 = await this.fileToBase64(blob);
                                                    this.imageAttachments.push(base64);
                                                    count++;
                                                }
                                            }
                                            if (count > 0) {
                                                this.showToast(`${'$'}{count} image(s) pasted`);
                                                this.render();
                                            } else {
                                                this.showToast('No image found in clipboard');
                                            }
                                        } catch (e) {
                                            this.showToast('Paste failed. Try Ctrl+V in input.');
                                        }
                                    },
                                    
                                    async handleDocFiles(input) {
                                        if (this.modelCapabilities && !this.modelCapabilities.supportsDocuments) return;
                                        const files = Array.from(input.files || []);
                                        for (const file of files) {
                                            const base64 = await this.fileToBase64(file);
                                            const type = (file.name.split('.').pop() || 'bin').toLowerCase();
                                            this.fileAttachments.push({
                                                name: file.name,
                                                content: base64,
                                                type: type
                                            });
                                        }
                                        input.value = '';
                                        this.showToast(`${'$'}{files.length} file(s) attached`);
                                        this.render();
                                    },
                                    
                                    fileToBase64(file) {
                                        return new Promise((resolve, reject) => {
                                            const reader = new FileReader();
                                            reader.onload = () => resolve(reader.result);
                                            reader.onerror = reject;
                                            reader.readAsDataURL(file);
                                        });
                                    },
                                    
                                    removeImage(index) {
                                        this.imageAttachments.splice(index, 1);
                                        this.render();
                                    },
                                    
                                    removeFile(index) {
                                        this.fileAttachments.splice(index, 1);
                                        this.render();
                                    },
                                    
                                    toggleWebSearch() {
                                        if (this.modelCapabilities && !this.modelCapabilities.supportsWebSearch) return;
                                        this.webSearchEnabled = !this.webSearchEnabled;
                                        this.render();
                                    },
                                    
                                    scrollToBottom(smooth = true) {
                                        const container = document.querySelector('.messages-container');
                                        if (!container) return;
                                        
                                        if (smooth) {
                                            // Smooth scroll for streaming
                                            container.scrollTop = container.scrollHeight;
                                        } else {
                                            // Instant scroll for initial load
                                            container.style.scrollBehavior = 'auto';
                                            container.scrollTop = container.scrollHeight;
                                            requestAnimationFrame(() => {
                                                container.style.scrollBehavior = 'smooth';
                                            });
                                        }
                                    },
                                    
                                    scrollToBottomThrottled() {
                                        // Throttle scroll updates during streaming to avoid jank (max 10/sec)
                                        const now = Date.now();
                                        if (now - this.lastScrollTime < 100) {
                                            // Schedule a final scroll if we're throttling
                                            if (this.scrollThrottleTimer) clearTimeout(this.scrollThrottleTimer);
                                            this.scrollThrottleTimer = setTimeout(() => this.scrollToBottom(true), 100);
                                            return;
                                        }
                                        this.lastScrollTime = now;
                                        
                                        // Auto-scroll only if user is already near bottom (within 150px)
                                        const container = document.querySelector('.messages-container');
                                        if (!container) return;
                                        
                                        const isNearBottom = container.scrollHeight - container.scrollTop - container.clientHeight < 150;
                                        if (isNearBottom) {
                                            this.scrollToBottom(true);
                                        }
                                    },
                                    
                                    exportMarkdown() {
                                        if (!this.selectedConversation || !this.messages.length) return;
                                        let md = `# ${'$'}{this.selectedConversation.title}\\n\\n*Exported: ${'$'}{new Date().toLocaleString()}*\\n\\n---\\n\\n`;
                                        this.messages.forEach(msg => {
                                            const role = msg.role === 'USER' ? '**You**' : '**AI**';
                                            const time = new Date(msg.createdAt).toLocaleString();
                                            md += `${'$'}{role} *(${'$'}{time})*:\\n\\n${'$'}{msg.content}\\n\\n---\\n\\n`;
                                        });
                                        const blob = new Blob([md], { type: 'text/markdown' });
                                        const url = URL.createObjectURL(blob);
                                        const a = document.createElement('a');
                                        a.href = url;
                                        a.download = `${'$'}{this.selectedConversation.title.replace(/[^a-z0-9]/gi, '_')}.md`;
                                        a.click();
                                        URL.revokeObjectURL(url);
                                    },
                                    
                                    copyMessage(content) {
                                        navigator.clipboard.writeText(content);
                                        this.showToast('Copied to clipboard');
                                    },
                                    
                                    copyCode(codeId) {
                                        const codeEl = document.getElementById(codeId);
                                        if (codeEl) {
                                            navigator.clipboard.writeText(codeEl.textContent);
                                            this.showToast('Code copied');
                                        }
                                    },
                                    
                                    parseMarkdown(text, isUser = false) {
                                        try {
                                            if (!text) return '';
                                            
                                            const lines = text.split('\n');
                                            const result = [];
                                            let inCodeBlock = false;
                                            let codeLines = [];
                                            
                                            for (let i = 0; i < lines.length; i++) {
                                                const line = lines[i];
                                                const trimmed = line.trim();
                                                
                                                // Проверка на границы кодблока
                                                if (trimmed.startsWith('```')) {
                                                    if (!inCodeBlock) {
                                                        inCodeBlock = true;
                                                        codeLines = [];
                                                    } else {
                                                        inCodeBlock = false;
                                                        const codeContent = codeLines.join('\n');
                                                        const escaped = this.escapeHtml(codeContent);
                                                        const id = 'code_' + Date.now() + '_' + Math.random().toString(36).substr(2, 5);
                                                        
                                                        const bgColor = isUser ? 'rgba(0,0,0,0.2)' : 'var(--md-sys-color-surface-variant)';
                                                        const textColor = isUser ? 'inherit' : 'var(--md-sys-color-on-surface)';
                                                        
                                                        // ВАЖНО: все переменные заранее объявлены, никаких вложенных шаблонов!
                                                        result.push(
                                                            '<div class="code-block-wrapper" style="margin:8px 0;position:relative;">' +
                                                                '<button type="button" class="code-copy-btn" onclick="app.copyCode(\'' + id + '\')" style="position:absolute;top:8px;right:8px;padding:4px 8px;cursor:pointer;background:rgba(0,0,0,0.5);color:white;border:none;border-radius:4px;font-size:12px;">📋 Copy</button>' +
                                                                '<pre style="background:' + bgColor + ';padding:12px;padding-top:40px;border-radius:8px;overflow-x:auto;margin:0;">' +
                                                                    '<code id="' + id + '" style="font-family:monospace;font-size:13px;color:' + textColor + ';white-space:pre;">' + escaped + '</code>' +
                                                                '</pre>' +
                                                            '</div>'
                                                        );
                                                        codeLines = [];
                                                    }
                                                    continue;
                                                }
                                                
                                                if (inCodeBlock) {
                                                    codeLines.push(line);
                                                    continue;
                                                }
                                                
                                                // Горизонтальная линия
                                                if (/^(---|\*\*\*|___)\s*${'$'}/.test(trimmed)) {
                                                    result.push('<hr style="border:none;border-top:2px solid rgba(0,0,0,0.1);margin:12px 0;">');
                                                    continue;
                                                }
                                                
                                                // Заголовки
                                                const headingMatch = trimmed.match(/^(#{1,6})\s+(.+)${'$'}/);
                                                if (headingMatch) {
                                                    const level = headingMatch[1].length;
                                                    const headingText = this.escapeHtml(
                                                        headingMatch[2]
                                                            .replace(/\*\*(.+?)\*\*/g, '${'$'}1')
                                                            .replace(/\*(.+?)\*/g, '${'$'}1')
                                                            .replace(/`(.+?)`/g, '${'$'}1')
                                                            .replace(/\[(.+?)\]\(.+?\)/g, '${'$'}1')
                                                    );
                                                    const sizes = ['28px', '24px', '20px', '18px', '16px', '15px'];
                                                    const color = isUser ? 'inherit' : 'var(--md-sys-color-primary)';
                                                    result.push('<div style="font-size:' + sizes[level-1] + ';font-weight:500;color:' + color + ';margin:8px 0;">' + headingText + '</div>');
                                                    continue;
                                                }
                                                
                                                // Blockquote
                                                const isQuote = trimmed.startsWith('>');
                                                let content = isQuote ? trimmed.substring(1).trim() : line;
                                                content = this.parseInlineMarkdown(content, isUser);
                                                
                                                if (isQuote) {
                                                    const borderColor = isUser ? 'rgba(255,255,255,0.3)' : 'var(--md-sys-color-primary)';
                                                    result.push('<div style="border-left:3px solid ' + borderColor + ';padding-left:12px;margin:8px 0;opacity:0.8;">' + content + '</div>');
                                                } else {
                                                    result.push(content);
                                                    if (i < lines.length - 1) result.push('<br>');
                                                }
                                            }
                                            
                                            // Незакрытый кодблок (стриминг)
                                            if (inCodeBlock && codeLines.length > 0) {
                                                const escaped = this.escapeHtml(codeLines.join('\n'));
                                                const bgColor = isUser ? 'rgba(0,0,0,0.2)' : 'var(--md-sys-color-surface-variant)';
                                                const textColor = isUser ? 'inherit' : 'var(--md-sys-color-on-surface)';
                                                result.push('<pre style="background:' + bgColor + ';padding:12px;border-radius:8px;border-left:3px solid var(--md-sys-color-primary);opacity:0.9;overflow-x:auto;"><code style="font-family:monospace;font-size:13px;color:' + textColor + ';white-space:pre;">' + escaped + '</code></pre>');
                                            }
                                            
                                            return result.join('');
                                            
                                        } catch (error) {
                                            console.error('Markdown parse error:', error);
                                            return this.escapeHtml(text).replace(/\n/g, '<br>');
                                        }
                                    },

                                    escapeHtml(text) {
                                        // Lightweight HTML escaping without touching the DOM,
                                        // so it cannot break layout or depend on browser quirks.
                                        const str = text == null ? '' : String(text);
                                        return str
                                            .replace(/&/g, '&amp;')
                                            .replace(/</g, '&lt;')
                                            .replace(/>/g, '&gt;')
                                            .replace(/"/g, '&quot;')
                                            .replace(/'/g, '&#39;');
                                    },

                                    parseInlineMarkdown(text, isUser) {
                                        const linkColor = isUser ? '#90CAF9' : 'var(--md-sys-color-primary)';
                                        const codeBlockBg = isUser ? 'rgba(255,255,255,0.2)' : 'var(--md-sys-color-surface-variant)';
                                        const codeColor = isUser ? 'inherit' : 'var(--md-sys-color-secondary)';
                                        
                                        // Citation links [[1]](url)
                                        text = text.replace(/\[\[(\d+)\]\]\(([^\)]+)\)/g,
                                            '<a href="$2" target="_blank" style="color:' + linkColor + ';text-decoration:underline;font-size:11px;vertical-align:super;font-weight:600;">[$1]</a>');
                                        
                                        // Bold links **[text](url)**
                                        text = text.replace(/\*\*\[([^\]]+)\]\(([^\)]+)\)\*\*/g,
                                            '<a href="$2" target="_blank" style="color:' + linkColor + ';text-decoration:underline;font-weight:600;">$1</a>');
                                        
                                        // Regular links [text](url)
                                        text = text.replace(/\[([^\]]+)\]\(([^\)]+)\)/g,
                                            '<a href="$2" target="_blank" style="color:' + linkColor + ';text-decoration:underline;">$1</a>');
                                        
                                        // Inline code `code`
                                        text = text.replace(/`([^`]+?)`/g,
                                            '<code style="background:' + codeBlockBg + ';padding:2px 6px;border-radius:4px;font-family:monospace;font-size:13px;color:' + codeColor + ';">$1</code>');
                                        
                                        // Bold **text**
                                        text = text.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
                                        
                                        // Italic *text*
                                        text = text.replace(/\*([^*]+?)\*/g, '<em style="opacity:0.9;">$1</em>');
                                        
                                        return text;
                                    },

                                    updateStreamingMessage(messageId, content) {
                                        try {
                                            const msgElements = document.querySelectorAll('.message');
                                            for (const el of msgElements) {
                                                const contentEl = el.querySelector('.message-content');
                                                if (contentEl && contentEl.dataset.messageId === messageId) {
                                                    contentEl.innerHTML = this.parseMarkdown(content, false);
                                                    return;
                                                }
                                            }
                                        } catch (error) {
                                            console.error('Update streaming message error:', error);
                                        }
                                    },

                                    getMessageAttachments(msg) {
                                        let images = [];
                                        let files = [];

                                        // Images: temp message keeps array of base64 strings,
                                        // persisted message keeps JSON string of local file paths.
                                        if (Array.isArray(msg.imageAttachments)) {
                                            images = msg.imageAttachments.map(x => ({ src: x, name: 'Image' }));
                                        } else if (typeof msg.imageAttachments === 'string' && msg.imageAttachments.trim()) {
                                            try {
                                                const paths = JSON.parse(msg.imageAttachments);
                                                if (Array.isArray(paths)) {
                                                    images = paths.map((p, i) => ({
                                                        src: `/api/local-file?path=${'$'}{encodeURIComponent(p)}`,
                                                        name: `Image ${'$'}{i + 1}`
                                                    }));
                                                }
                                            } catch (_) {}
                                        }

                                        // Files: temp message keeps array of objects {name,content,type},
                                        // persisted message keeps JSON string of FileAttachment objects {path,name,type,...}.
                                        if (Array.isArray(msg.fileAttachments)) {
                                            files = msg.fileAttachments.map(f => ({
                                                name: f.name || 'File',
                                                icon: this.getFileTypeIcon(f.name || ''),
                                                downloadUrl: f.content || ''
                                            }));
                                        } else if (typeof msg.fileAttachments === 'string' && msg.fileAttachments.trim()) {
                                            try {
                                                const persisted = JSON.parse(msg.fileAttachments);
                                                if (Array.isArray(persisted)) {
                                                    files = persisted.map(f => ({
                                                        name: f.name || 'File',
                                                        icon: this.getFileTypeIcon(f.name || ''),
                                                        downloadUrl: f.path ? `/api/local-file?path=${'$'}{encodeURIComponent(f.path)}` : ''
                                                    }));
                                                }
                                            } catch (_) {}
                                        }

                                        return { images, files };
                                    },
                                    
                                    formatTime(timestamp) {
                                        const date = new Date(timestamp);
                                        return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
                                    },
                                    
                                    formatDate(timestamp) {
                                        const date = new Date(timestamp);
                                        const today = new Date();
                                        return date.toDateString() === today.toDateString() ? 'Today' : date.toLocaleDateString([], { month: 'short', day: 'numeric' });
                                    },
                                    
                                    render() {
                                        this.captureMessagesScroll();
                                        const html = `
                                            <div class="app">
                                                <div class="sidebar">
                                                    <div class="sidebar-header">
                                                        <h1>📱 YourOwnAI</h1>
                                                        ${'$'}{this.serverInfo ? `<div class="device-name">${'$'}{this.serverInfo.deviceInfo.deviceName}</div>` : ''}
                                                    </div>
                                                    <div class="search-box">
                                                        <input type="text" class="search-input" placeholder="🔍 Search..." value="${'$'}{this.searchQuery}" oninput="app.search(this.value)">
                                                    </div>
                                                    <button class="memories-btn ${'$'}{this.showMemories ? 'active' : ''}" onclick="app.toggleMemories()">🧠 Memories (${'$'}{this.memories.length})</button>
                                                    <div class="conversation-list">
                                                        ${'$'}{this.filteredConversations.map(c => `
                                                            <div class="conversation-item ${'$'}{this.selectedConversation?.id === c.id ? 'active' : ''}" onclick="app.selectConversation(${'$'}{JSON.stringify(c).replace(/"/g, '&quot;')})">
                                                                <div class="conversation-title">${'$'}{c.title}</div>
                                                                <div class="conversation-date">${'$'}{this.formatDate(c.updatedAt)}</div>
                                                            </div>
                                                        `).join('')}
                                                    </div>
                                                    <div class="sidebar-footer">
                                                        <div class="status-dot-container">
                                                            <div class="status-dot"></div>
                                                            <span>Connected</span>
                                                        </div>
                                                    </div>
                                                </div>
                                                <div class="chat-area">
                                                    ${'$'}{this.showMemories ? `
                                                        <div class="chat-header">
                                                            <div class="chat-title">🧠 Your Memories</div>
                                                            <div class="header-actions"><span>${'$'}{this.memories.length} total</span></div>
                                                        </div>
                                                        <div class="messages-container">
                                                            <div class="memories-grid">
                                                                ${'$'}{this.memories.map(m => `
                                                                    <div class="memory-card">
                                                                        <div class="memory-content">${'$'}{m.fact}</div>
                                                                        <div class="memory-time">${'$'}{new Date(m.createdAt).toLocaleDateString()}</div>
                                                                    </div>
                                                                `).join('')}
                                                            </div>
                                                        </div>
                                                    ` : this.selectedConversation ? `
                                                        <div class="chat-header">
                                                            <div class="chat-title">${'$'}{this.selectedConversation.title}</div>
                                                            <div class="header-actions">
                                                                <div class="chat-config-row">
                                                                    <span class="readonly-chip">Model: ${'$'}{this.selectedConversation.model || '-'}</span>
                                                                    <span class="readonly-chip">Provider: ${'$'}{this.selectedConversation.provider || '-'}</span>
                                                                    <span class="readonly-chip">Persona: ${'$'}{this.getPersonaName(this.selectedConversation.personaId)}</span>
                                                                    ${'$'}{this.getCapabilityBadgeHtml()}
                                                                </div>
                                                                <button class="icon-btn" onclick="app.exportMarkdown()">📄 MD</button>
                                                                <button class="icon-btn" onclick="location.reload()">🔄</button>
                                                            </div>
                                                        </div>
                                                        <div class="messages-container">
                                                            ${'$'}{this.messages.map(msg => {
                                                                try {
                                                                    const a = this.getMessageAttachments(msg);
                                                                    return `
                                                                <div class="message ${'$'}{msg.role.toLowerCase()}">
                                                                    <div class="message-avatar">${'$'}{msg.role === 'USER' ? '👤' : '🤖'}</div>
                                                                    <div class="message-wrapper">
                                                                        <div class="message-content" data-message-id="${'$'}{msg.id}">${'$'}{this.parseMarkdown(msg.content, msg.role === 'USER')}</div>
                                                                        ${'$'}{(a.images.length || a.files.length) ? `
                                                                            <div class="attachments-preview" style="margin-top:8px;">
                                                                                ${'$'}{a.images.map(img => `
                                                                                    <a href="${'$'}{img.src}" target="_blank" class="attachment-chip" style="text-decoration:none;">
                                                                                        <img class="image-thumb" src="${'$'}{img.src}" alt="${'$'}{img.name}">
                                                                                        <span>${'$'}{img.name}</span>
                                                                                    </a>
                                                                                `).join('')}
                                                                                ${'$'}{a.files.map(f => `
                                                                                    <a href="${'$'}{f.downloadUrl}" target="_blank" class="attachment-chip" style="text-decoration:none;">
                                                                                        <span class="file-thumb">${'$'}{f.icon}</span>
                                                                                        <span>${'$'}{f.name}</span>
                                                                                    </a>
                                                                                `).join('')}
                                                                            </div>
                                                                        ` : ''}
                                                                        <div class="message-footer">
                                                                            <span>${'$'}{this.formatTime(msg.createdAt)}</span>
                                                                            <button class="copy-btn" onclick="app.copyMessage(${'$'}{JSON.stringify(msg.content).replace(/"/g, '&quot;')})">📋</button>
                                                                        </div>
                                                                    </div>
                                                                </div>
                                                                `;
                                                                } catch (error) {
                                                                    console.error('Message render error:', error, msg);
                                                                    return `<div class="message assistant"><div class="message-wrapper"><div class="message-content" style="color:red;">⚠️ Failed to render message</div></div></div>`;
                                                                }
                                                            }).join('')}
                                                        </div>
                                                        <div class="input-area">
                                                            <div class="attachment-controls">
                                                                <label class="attach-btn" style="${'$'}{this.modelCapabilities && !this.modelCapabilities.supportsVision ? 'opacity:0.5;pointer-events:none;' : ''}" title="${'$'}{this.modelCapabilities && !this.modelCapabilities.supportsVision ? 'Current model does not support images' : 'Attach images'}">
                                                                    <span>🖼️ Image</span>
                                                                    <input type="file" accept="image/*" multiple onchange="app.handleImageFiles(this)">
                                                                </label>
                                                                <button class="attach-btn" style="${'$'}{this.modelCapabilities && !this.modelCapabilities.supportsVision ? 'opacity:0.5;pointer-events:none;' : ''}" onclick="app.pasteFromClipboard()" title="Paste screenshot from clipboard">
                                                                    <span>📋 Paste</span>
                                                                </button>
                                                                <label class="attach-btn" style="${'$'}{this.modelCapabilities && !this.modelCapabilities.supportsDocuments ? 'opacity:0.5;pointer-events:none;' : ''}" title="${'$'}{this.modelCapabilities && !this.modelCapabilities.supportsDocuments ? 'Current model does not support documents' : 'Attach documents'}">
                                                                    <span>📄 File</span>
                                                                    <input type="file" accept=".pdf,.txt,.doc,.docx" multiple onchange="app.handleDocFiles(this)">
                                                                </label>
                                                                <div class="toggle-switch ${'$'}{this.webSearchEnabled ? 'active' : ''}" style="${'$'}{this.modelCapabilities && !this.modelCapabilities.supportsWebSearch ? 'opacity:0.5;pointer-events:none;' : ''}" onclick="app.toggleWebSearch()">
                                                                    <span>🌐 Web Search</span>
                                                                </div>
                                                            </div>
                                                            ${'$'}{(this.imageAttachments.length || this.fileAttachments.length) ? `
                                                                <div class="attachments-preview">
                                                                    ${'$'}{this.imageAttachments.map((_, i) => `
                                                                        <div class="attachment-chip">
                                                                            <img class="image-thumb" src="${'$'}{this.imageAttachments[i]}" alt="img">
                                                                            <span>🖼️ Image ${'$'}{i + 1}</span>
                                                                            <button onclick="app.removeImage(${'$'}{i})">×</button>
                                                                        </div>
                                                                    `).join('')}
                                                                    ${'$'}{this.fileAttachments.map((f, i) => `
                                                                        <div class="attachment-chip">
                                                                            <span class="file-thumb">${'$'}{this.getFileTypeIcon(f.name)}</span>
                                                                            <span>${'$'}{f.name}</span>
                                                                            <button onclick="app.removeFile(${'$'}{i})">×</button>
                                                                        </div>
                                                                    `).join('')}
                                                                </div>
                                                            ` : ''}
                                                            <div class="input-wrapper">
                                                                <textarea 
                                                                    class="message-input" 
                                                                    placeholder="Type a message... (Shift+Enter for new line)"
                                                                    value="${'$'}{this.inputValue}"
                                                                    oninput="app.inputValue = this.value; app.autoResizeTextarea(this)"
                                                                    onpaste="app.handlePaste(event)"
                                                                    onkeydown="if(event.key==='Enter' && !event.shiftKey){event.preventDefault();app.sendMessage();}"
                                                                    ${'$'}{this.isSending ? 'disabled' : ''}
                                                                    rows="1"
                                                                ></textarea>
                                                                <button 
                                                                    class="send-button"
                                                                    onclick="app.sendMessage()"
                                                                    ${'$'}{(!this.inputValue.trim() && this.imageAttachments.length === 0 && this.fileAttachments.length === 0) || this.isSending ? 'disabled' : ''}
                                                                >
                                                                    ${'$'}{this.isSending ? '⏳ Sending...' : '📤 Send'}
                                                                </button>
                                                            </div>
                                                        </div>
                                                    ` : `
                                                        <div class="empty-state">
                                                            <h2>📱 YourOwnAI Web Viewer</h2>
                                                            <p>Select a conversation to view messages</p>
                                                        </div>
                                                    `}
                                                </div>
                                            </div>
                                        `;
                                        document.getElementById('app').innerHTML = html;
                                        this.restoreMessagesScroll();
                                        this.autoResizeTextarea(document.querySelector('.message-input'));
                                        const snackbarEl = document.getElementById('snackbar');
                                        if (snackbarEl) {
                                            snackbarEl.className = this.toastMessage ? 'snackbar show' : 'snackbar';
                                            snackbarEl.textContent = this.toastMessage || '';
                                        }
                                    }
                                };
                                
                                app.init();
                                </script>
                                <div id="snackbar" class="snackbar"></div>
                            </body>
                            </html>
                            """.trimIndent(),
                            ContentType.Text.Html
                        )
                    }
                    
                    // ========== API ENDPOINTS ==========
                    
                    // Server status
                    get("/status") {
                        val status = getServerStatus()
                        call.respond(status)
                    }
                    
                    // Get all conversations
                    get("/api/conversations") {
                        try {
                            val conversations = conversationRepository.getAllConversations().first()
                            call.respond(HttpStatusCode.OK, conversations)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error fetching conversations", e)
                            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                        }
                    }
                    
                    // Get messages for conversation
                    get("/api/conversations/{id}/messages") {
                        try {
                            val conversationId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                            val messages = messageRepository.getMessagesByConversation(conversationId).first()
                            call.respond(HttpStatusCode.OK, messages)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error fetching messages", e)
                            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                        }
                    }

                    // Serve local uploaded file/image by path (scoped to local_sync_uploads cache dir)
                    get("/api/local-file") {
                        try {
                            val path = call.request.queryParameters["path"]
                                ?: return@get call.respond(HttpStatusCode.BadRequest)
                            val file = java.io.File(path)
                            val allowedRoot = java.io.File(this@LocalSyncServer.context.cacheDir, "local_sync_uploads").canonicalPath
                            val targetPath = file.canonicalPath
                            if (!targetPath.startsWith(allowedRoot) || !file.exists() || !file.isFile) {
                                return@get call.respond(HttpStatusCode.NotFound)
                            }
                            val contentType = when (file.extension.lowercase()) {
                                "jpg", "jpeg" -> ContentType.Image.JPEG
                                "png" -> ContentType.Image.PNG
                                "gif" -> ContentType.Image.GIF
                                "webp" -> ContentType.parse("image/webp")
                                "pdf" -> ContentType.Application.Pdf
                                "txt" -> ContentType.Text.Plain
                                "doc" -> ContentType.parse("application/msword")
                                "docx" -> ContentType.parse("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                                else -> ContentType.Application.OctetStream
                            }
                            call.response.header(HttpHeaders.ContentType, contentType.toString())
                            call.respondFile(file)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error serving local file", e)
                            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                        }
                    }
                    
                    // Send message and stream response
                    post("/api/conversations/{id}/messages") {
                        try {
                            val conversationId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                            val request = call.receive<SendMessageRequest>()
                            val clientMessageId = request.clientMessageId
                            
                            Log.d(TAG, "Received message for conversation $conversationId: ${request.content}")

                            if (!clientMessageId.isNullOrBlank() && isDuplicateClientMessage(clientMessageId)) {
                                Log.w(TAG, "Duplicate client message ignored: $clientMessageId")
                                call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                                    write("data: ${gson.toJson(mapOf("duplicate" to true, "done" to true))}\n\n")
                                    flush()
                                }
                                return@post
                            }
                            
                            // Get conversation to access settings
                            var conversation = conversationRepository.getConversationByIdSync(conversationId)
                            if (conversation == null) {
                                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Conversation not found"))
                                return@post
                            }
                            if (!request.personaId.isNullOrBlank() && request.personaId != conversation?.personaId) {
                                conversationRepository.updateConversationPersona(conversationId, request.personaId)
                                conversation = conversationRepository.getConversationByIdSync(conversationId)
                            }
                            if (conversation == null) {
                                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Conversation not found"))
                                return@post
                            }
                            val currentConversation = conversation ?: run {
                                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Conversation not found"))
                                return@post
                            }
                            
                            // Convert incoming base64 attachments to local files.
                            // AIServiceImpl already expects local paths in message attachments.
                            val imagePaths = saveIncomingImages(request.imageAttachments)
                            val fileAttachments = saveIncomingFiles(request.fileAttachments)
                            val imageAttachmentsJson = if (imagePaths.isNotEmpty()) gson.toJson(imagePaths) else null
                            val fileAttachmentsJson = if (fileAttachments.isNotEmpty()) gson.toJson(fileAttachments) else null
                            
                            // Save user message
                            val userMessage = Message(
                                id = "msg_${System.currentTimeMillis()}_${(0..999).random()}",
                                conversationId = conversationId,
                                content = request.content,
                                role = MessageRole.USER,
                                createdAt = System.currentTimeMillis(),
                                tokenCount = 0,
                                model = null,
                                imageAttachments = imageAttachmentsJson,
                                fileAttachments = fileAttachmentsJson
                            )
                            messageRepository.addMessage(userMessage)
                            
                            // Get recent messages for context
                            val recentMessages = messageRepository.getLastMessagePairs(conversationId, 10)
                            
                            // Parse provider
                            val aiProvider = when (currentConversation.provider.lowercase()) {
                                "openai" -> AIProvider.OPENAI
                                "deepseek" -> AIProvider.DEEPSEEK
                                "xai" -> AIProvider.XAI
                                "openrouter" -> AIProvider.OPENROUTER
                                else -> AIProvider.OPENAI
                            }
                            val provider = ModelProvider.API(
                                provider = aiProvider,
                                modelId = currentConversation.model,
                                displayName = currentConversation.model
                            )
                            
                            // Create config
                            val config = AIConfig(
                                temperature = 0.7f,
                                maxTokens = 4096,
                                topP = 1.0f
                            )
                            
                            val capabilityWebSearch = ModelCapabilities.forModel(currentConversation.model).supportsWebSearch
                            val effectiveWebSearchEnabled =
                                request.webSearchEnabled && capabilityWebSearch
                            conversationRepository.updateWebSearchEnabled(conversationId, effectiveWebSearchEnabled)
                            
                            // Generate AI response with streaming (SSE format)
                            call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                                val assistantMessageId = "msg_${System.currentTimeMillis()}_${(0..999).random()}"
                                val responseBuilder = StringBuilder()
                                
                                try {
                                    coroutineScope {
                                        val writeMutex = Mutex()

                                        // Send initial event immediately to keep connection warm.
                                        writeMutex.withLock {
                                            write("data: ${gson.toJson(mapOf("started" to true))}\n\n")
                                            flush()
                                        }

                                        // Keep SSE alive while model is thinking (important for o-series).
                                        val heartbeatJob = launch {
                                            while (isActive) {
                                                delay(3000)
                                                writeMutex.withLock {
                                                    write(": ping\n\n")
                                                    flush()
                                                }
                                            }
                                        }

                                        try {
                                            aiService.generateResponse(
                                                provider = provider,
                                                messages = recentMessages + userMessage,
                                                systemPrompt = currentConversation.systemPrompt,
                                                userContext = null,
                                                config = config,
                                                webSearchEnabled = effectiveWebSearchEnabled,
                                                xSearchEnabled = currentConversation.xSearchEnabled
                                            ).collect { chunk ->
                                                responseBuilder.append(chunk)
                                                writeMutex.withLock {
                                                    write("data: ${gson.toJson(mapOf("chunk" to chunk))}\n\n")
                                                    flush()
                                                }
                                            }
                                        } finally {
                                            heartbeatJob.cancel()
                                        }
                                    }
                                    
                                    // Save assistant message
                                    val assistantMessage = Message(
                                        id = assistantMessageId,
                                        conversationId = conversationId,
                                        content = responseBuilder.toString(),
                                        role = MessageRole.ASSISTANT,
                                        createdAt = System.currentTimeMillis(),
                                        tokenCount = 0,
                                        model = currentConversation.model,
                                        imageAttachments = null,
                                        fileAttachments = null
                                    )
                                    messageRepository.addMessage(assistantMessage)
                                    
                                    // Send done signal
                                    write("data: ${gson.toJson(mapOf("done" to true))}\n\n")
                                    flush()
                                } catch (e: Exception) {
                                    if (isClientDisconnectOrTimeout(e)) {
                                        Log.w(TAG, "SSE stream closed by timeout/disconnect: ${e.message}")
                                    } else {
                                        Log.e(TAG, "Error generating response", e)
                                        try {
                                            write("data: ${gson.toJson(mapOf("error" to e.message))}\n\n")
                                            flush()
                                        } catch (_: Exception) {
                                            // Ignore secondary write failures when stream is already closed.
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            if (isClientDisconnectOrTimeout(e)) {
                                Log.w(TAG, "Message endpoint stream closed: ${e.message}")
                            } else {
                                Log.e(TAG, "Error in message endpoint", e)
                                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                            }
                        }
                    }

                    // ========== LEGACY SYNC ENDPOINTS ==========
                    
                    // Full sync endpoint
                    post("/sync/full") {
                        try {
                            val request = call.receive<SyncRequest>()
                            Log.d(TAG, "Full sync request from ${request.deviceInfo.deviceName}")
                            
                            val response = performFullSync()
                            call.respond(HttpStatusCode.OK, response)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in full sync", e)
                            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                        }
                    }
                    
                    // Get memories
                    get("/memories") {
                        try {
                            val memories = memoryRepository.getAllMemoryEntities()
                                .map { it.copy(embedding = null) } // Don't send embeddings
                            call.respond(HttpStatusCode.OK, memories)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error fetching memories", e)
                            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                        }
                    }
                    
                    // Get personas
                    get("/personas") {
                        try {
                            val personas = personaRepository.getAllPersonasEntities()
                            call.respond(HttpStatusCode.OK, personas)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error fetching personas", e)
                            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                        }
                    }
                    
                    // Get system prompts
                    get("/api/system-prompts") {
                        try {
                            val prompts = systemPromptRepository.getAllPrompts().first()
                            call.respond(HttpStatusCode.OK, prompts)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error fetching system prompts", e)
                            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                        }
                    }
                    
                    // Get AI config
                    get("/api/config") {
                        try {
                            val config = aiConfigRepository.getAIConfig()
                            call.respond(HttpStatusCode.OK, config)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error fetching AI config", e)
                            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                        }
                    }

                    // Get appearance settings (theme/fonts) from phone
                    get("/api/appearance") {
                        try {
                            val themeMode = settingsManager.themeMode.first().name
                            val colorStyle = settingsManager.colorStyle.first().name
                            val fontStyle = settingsManager.fontStyle.first().name
                            val fontScale = settingsManager.fontScale.first().scale
                            call.respond(
                                HttpStatusCode.OK,
                                AppearanceSettingsResponse(
                                    themeMode = themeMode,
                                    colorStyle = colorStyle,
                                    fontStyle = fontStyle,
                                    fontScale = fontScale
                                )
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error fetching appearance settings", e)
                            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                        }
                    }
                    
                    // Get documents (RAG)
                    get("/api/documents") {
                        try {
                            val documents = knowledgeDocumentRepository.getAllDocuments().first()
                            call.respond(HttpStatusCode.OK, documents)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error fetching documents", e)
                            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                        }
                    }
                    
                    // Create new conversation
                    post("/api/conversations") {
                        try {
                            val request = call.receive<CreateConversationRequest>()
                            
                            val conversationId = conversationRepository.createConversation(
                                title = request.title ?: "New Chat",
                                systemPrompt = request.systemPrompt ?: "",
                                model = request.model ?: "gpt-4o-mini",
                                provider = request.provider ?: "openai"
                            )
                            
                            val conversation = conversationRepository.getConversationByIdSync(conversationId)
                            call.respond(HttpStatusCode.Created, conversation ?: mapOf("id" to conversationId))
                        } catch (e: Exception) {
                            Log.e(TAG, "Error creating conversation", e)
                            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                        }
                    }
                    
                    // Get available models
                    get("/api/models") {
                        try {
                            val models = mapOf(
                                "openai" to listOf(
                                    mapOf("id" to "gpt-4o", "name" to "GPT-4o"),
                                    mapOf("id" to "gpt-4o-mini", "name" to "GPT-4o Mini"),
                                    mapOf("id" to "o1", "name" to "o1"),
                                    mapOf("id" to "o1-mini", "name" to "o1 Mini"),
                                    mapOf("id" to "o3-mini", "name" to "o3 Mini")
                                ),
                                "deepseek" to listOf(
                                    mapOf("id" to "deepseek-chat", "name" to "DeepSeek Chat"),
                                    mapOf("id" to "deepseek-reasoner", "name" to "DeepSeek R1")
                                ),
                                "xai" to listOf(
                                    mapOf("id" to "grok-2-latest", "name" to "Grok 2"),
                                    mapOf("id" to "grok-beta", "name" to "Grok Beta")
                                ),
                                "openrouter" to listOf(
                                    mapOf("id" to "openai/gpt-4o", "name" to "GPT-4o (OpenRouter)"),
                                    mapOf("id" to "anthropic/claude-3.5-sonnet", "name" to "Claude 3.5 Sonnet")
                                )
                            )
                            call.respond(HttpStatusCode.OK, models)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error fetching models", e)
                            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                        }
                    }
                    
                    // Get model capabilities
                    get("/api/model-capabilities") {
                        try {
                            val modelId = call.request.queryParameters["modelId"]
                                ?: return@get call.respond(HttpStatusCode.BadRequest)
                            val caps = ModelCapabilities.forModel(modelId)
                            call.respond(HttpStatusCode.OK, caps)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error fetching model capabilities", e)
                            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                        }
                    }
                }
            }
            
            server?.start(wait = false)
            
            val ipAddress = getLocalIpAddress()
            Log.i(TAG, "✅ Local Sync Server started on $ipAddress:$port")
            Log.i(TAG, "   Web UI: http://$ipAddress:$port")
            Log.i(TAG, "   API: http://$ipAddress:$port/api")
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start server", e)
            false
        }
    }
    
    /**
     * Stop HTTP server
     */
    fun stop() {
        try {
            server?.stop(1000, 5000)
            server = null
            Log.i(TAG, "✅ Local Sync Server stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping server", e)
        }
    }
    
    /**
     * Check if server is running
     */
    fun isRunning(): Boolean = server != null
    
    /**
     * Get server status
     */
    suspend fun getServerStatus(): ServerStatus {
        val deviceInfo = DeviceInfo(
            deviceId = deviceId,
            deviceName = android.os.Build.MODEL,
            appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown",
            ipAddress = getLocalIpAddress()
        )
        
        return ServerStatus(
            isRunning = isRunning(),
            deviceInfo = deviceInfo,
            port = currentPort,
            totalConversations = conversationRepository.getAllConversations().first().size,
            totalMessages = 0, // TODO: Count messages
            totalMemories = memoryRepository.getTotalMemoryCount(),
            totalPersonas = personaRepository.getAllPersonasEntities().size
        )
    }
    
    /**
     * Perform full sync - send all data
     */
    private suspend fun performFullSync(): SyncResponse {
        val deviceInfo = DeviceInfo(
            deviceId = deviceId,
            deviceName = android.os.Build.MODEL,
            appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown",
            ipAddress = getLocalIpAddress()
        )
        
        val conversations = conversationRepository.getAllConversations().first()
        
        // Get all messages for all conversations
        val allMessages = mutableListOf<com.yourown.ai.domain.model.Message>()
        conversations.forEach { conversation ->
            val messages = messageRepository.getMessagesByConversation(conversation.id).first()
            allMessages.addAll(messages)
        }
        
        val memories = memoryRepository.getAllMemoryEntities()
            .map { it.copy(embedding = null) } // Don't send embeddings
        
        val personas = personaRepository.getAllPersonasEntities()
        
        Log.d(TAG, "Full sync: ${conversations.size} conversations, ${allMessages.size} messages, ${memories.size} memories, ${personas.size} personas")
        
        return SyncResponse(
            deviceInfo = deviceInfo,
            conversations = conversations.map { it.toEntity() },
            messages = allMessages.map { it.toEntity() },
            memories = memories,
            personas = personas
        )
    }
    
    /**
     * Perform incremental sync - send only changes since timestamp
     */
    private suspend fun performIncrementalSync(sinceTimestamp: Long): IncrementalSyncResponse {
        val deviceInfo = DeviceInfo(
            deviceId = deviceId,
            deviceName = android.os.Build.MODEL,
            appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown",
            ipAddress = getLocalIpAddress()
        )
        
        // Get new/updated conversations
        val allConversations = conversationRepository.getAllConversations().first()
        val newConversations = allConversations.filter { it.createdAt > sinceTimestamp }
        val updatedConversations = allConversations.filter { it.updatedAt > sinceTimestamp && it.createdAt <= sinceTimestamp }
        
        // Get new messages
        val allMessages = mutableListOf<com.yourown.ai.domain.model.Message>()
        allConversations.forEach { conversation ->
            val messages = messageRepository.getMessagesByConversation(conversation.id).first()
            allMessages.addAll(messages.filter { it.createdAt > sinceTimestamp })
        }
        
        // Get new memories
        val allMemories = memoryRepository.getAllMemoryEntities()
        val newMemories = allMemories.filter { it.createdAt > sinceTimestamp }
            .map { it.copy(embedding = null) }
        
        // Get new personas
        val allPersonas = personaRepository.getAllPersonasEntities()
        val newPersonas = allPersonas.filter { it.createdAt > sinceTimestamp }
        
        Log.d(TAG, "Incremental sync: ${newConversations.size} new conversations, ${updatedConversations.size} updated, ${allMessages.size} new messages")
        
        return IncrementalSyncResponse(
            deviceInfo = deviceInfo,
            newConversations = newConversations.map { it.toEntity() },
            updatedConversations = updatedConversations.map { it.toEntity() },
            newMessages = allMessages.map { it.toEntity() },
            newMemories = newMemories,
            newPersonas = newPersonas
        )
    }
    
    /**
     * Get local IP address for Wi-Fi connection
     */
    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        return address.hostAddress ?: "unknown"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting IP address", e)
        }
        return "unknown"
    }

    private fun saveIncomingImages(base64Images: List<String>?): List<String> {
        if (base64Images.isNullOrEmpty()) return emptyList()
        val uploadsDir = java.io.File(context.cacheDir, "local_sync_uploads/images")
        if (!uploadsDir.exists()) uploadsDir.mkdirs()

        return base64Images.mapNotNull { encoded ->
            try {
                val cleanBase64 = encoded.substringAfter(",", encoded)
                val bytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
                val file = java.io.File(
                    uploadsDir,
                    "img_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg"
                )
                file.writeBytes(bytes)
                file.absolutePath
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save incoming image", e)
                null
            }
        }
    }

    private fun saveIncomingFiles(files: List<com.yourown.ai.data.sync.local.models.FileAttachment>?): List<FileAttachment> {
        if (files.isNullOrEmpty()) return emptyList()
        val uploadsDir = java.io.File(context.cacheDir, "local_sync_uploads/files")
        if (!uploadsDir.exists()) uploadsDir.mkdirs()

        return files.mapNotNull { incoming ->
            try {
                val extension = incoming.name.substringAfterLast('.', incoming.type.ifBlank { "bin" })
                val safeName = incoming.name.ifBlank { "file_${System.currentTimeMillis()}.$extension" }
                val cleanBase64 = incoming.content.substringAfter(",", incoming.content)
                val bytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
                val file = java.io.File(
                    uploadsDir,
                    "${System.currentTimeMillis()}_${(1000..9999).random()}_$safeName"
                )
                file.writeBytes(bytes)
                FileAttachment(
                    path = file.absolutePath,
                    name = safeName,
                    type = extension.lowercase(),
                    sizeBytes = bytes.size.toLong()
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save incoming file: ${incoming.name}", e)
                null
            }
        }
    }

    private fun isClientDisconnectOrTimeout(error: Throwable): Boolean {
        return error is WriteTimeoutException ||
            error is CancellationException ||
            error.cause is WriteTimeoutException ||
            error.cause is CancellationException
    }

    private fun isDuplicateClientMessage(clientMessageId: String): Boolean {
        val now = System.currentTimeMillis()
        // Keep dedupe IDs for 10 minutes
        val ttlMs = 10 * 60 * 1000L
        if (processedClientMessageIds.size > 2000) {
            processedClientMessageIds.entries.removeIf { now - it.value > ttlMs }
        }
        val existing = processedClientMessageIds.putIfAbsent(clientMessageId, now)
        return existing != null
    }
}

// Extension functions for conversion
private fun com.yourown.ai.domain.model.Conversation.toEntity() = 
    com.yourown.ai.data.local.entity.ConversationEntity(
        id = id,
        title = title,
        systemPrompt = systemPrompt,
        systemPromptId = systemPromptId,
        personaId = personaId,
        model = model,
        provider = provider,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isArchived = isArchived,
        isPinned = isPinned,
        webSearchEnabled = webSearchEnabled,
        xSearchEnabled = xSearchEnabled
    )

private fun com.yourown.ai.domain.model.Message.toEntity() =
    com.yourown.ai.data.local.entity.MessageEntity(
        id = id,
        conversationId = conversationId,
        content = content,
        role = role.name, // Convert enum to string
        createdAt = createdAt,
        tokenCount = tokenCount,
        model = model,
        imageAttachments = imageAttachments,
        fileAttachments = fileAttachments,
        isError = isError,
        errorMessage = errorMessage,
        isLiked = isLiked,
        swipeMessageId = swipeMessageId,
        swipeMessageText = swipeMessageText,
        temperature = temperature,
        topP = topP,
        deepEmpathy = deepEmpathy ?: false,
        memoryEnabled = memoryEnabled ?: true,
        messageHistoryLimit = messageHistoryLimit,
        systemPrompt = systemPrompt,
        requestLogs = requestLogs
    )
