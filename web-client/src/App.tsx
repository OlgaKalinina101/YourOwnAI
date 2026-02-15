import React, { useState, useEffect, useRef } from 'react';
import type { Conversation, Message, MemoryEntry } from '../types';
import { apiClient } from '../api/client';
import '../styles/App.css';

const App: React.FC = () => {
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [filteredConversations, setFilteredConversations] = useState<Conversation[]>([]);
  const [selectedConversation, setSelectedConversation] = useState<Conversation | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [memories, setMemories] = useState<MemoryEntry[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [connectionStatus, setConnectionStatus] = useState<'connecting' | 'connected' | 'error'>('connecting');
  const [showMemories, setShowMemories] = useState(false);
  const [serverInfo, setServerInfo] = useState<any>(null);
  
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Load data on mount
  useEffect(() => {
    checkConnection();
    loadConversations();
    loadMemories();
  }, []);

  // Filter conversations when search changes
  useEffect(() => {
    if (searchQuery.trim() === '') {
      setFilteredConversations(conversations);
    } else {
      const query = searchQuery.toLowerCase();
      const filtered = conversations.filter(conv => 
        conv.title.toLowerCase().includes(query)
      );
      setFilteredConversations(filtered);
    }
  }, [searchQuery, conversations]);

  // Auto-scroll to bottom when messages change
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const checkConnection = async () => {
    try {
      const status = await apiClient.getServerStatus();
      setServerInfo(status);
      setConnectionStatus('connected');
    } catch (error) {
      setConnectionStatus('error');
      console.error('Failed to connect to server:', error);
    }
  };

  const loadConversations = async () => {
    try {
      setIsLoading(true);
      const data = await apiClient.getConversations();
      const sorted = data.sort((a, b) => b.updatedAt - a.updatedAt);
      setConversations(sorted);
      setFilteredConversations(sorted);
      
      // Select first conversation if none selected
      if (!selectedConversation && sorted.length > 0) {
        selectConversation(sorted[0]);
      }
    } catch (error) {
      console.error('Failed to load conversations:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const loadMemories = async () => {
    try {
      const data = await apiClient.getMemories();
      setMemories(data);
    } catch (error) {
      console.error('Failed to load memories:', error);
    }
  };

  const selectConversation = async (conversation: Conversation) => {
    try {
      setSelectedConversation(conversation);
      setShowMemories(false);
      const data = await apiClient.getMessages(conversation.id);
      setMessages(data.sort((a, b) => a.timestamp - b.timestamp));
    } catch (error) {
      console.error('Failed to load messages:', error);
    }
  };

  const exportToMarkdown = () => {
    if (!selectedConversation || messages.length === 0) return;
    
    let markdown = `# ${selectedConversation.title}\n\n`;
    markdown += `*Exported: ${new Date().toLocaleString()}*\n\n`;
    markdown += `---\n\n`;
    
    messages.forEach(msg => {
      const role = msg.role === 'USER' ? '**You**' : '**AI**';
      const time = new Date(msg.timestamp).toLocaleString();
      markdown += `${role} *(${time})*:\n\n${msg.content}\n\n---\n\n`;
    });
    
    // Download file
    const blob = new Blob([markdown], { type: 'text/markdown' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${selectedConversation.title.replace(/[^a-z0-9]/gi, '_')}.md`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const exportToTxt = () => {
    if (!selectedConversation || messages.length === 0) return;
    
    let text = `${selectedConversation.title}\n`;
    text += `Exported: ${new Date().toLocaleString()}\n`;
    text += `${'='.repeat(60)}\n\n`;
    
    messages.forEach(msg => {
      const role = msg.role === 'USER' ? 'You' : 'AI';
      const time = new Date(msg.timestamp).toLocaleString();
      text += `[${time}] ${role}:\n${msg.content}\n\n`;
    });
    
    // Download file
    const blob = new Blob([text], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${selectedConversation.title.replace(/[^a-z0-9]/gi, '_')}.txt`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const formatTime = (timestamp: number) => {
    const date = new Date(timestamp);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  const formatDate = (timestamp: number) => {
    const date = new Date(timestamp);
    const today = new Date();
    
    if (date.toDateString() === today.toDateString()) {
      return 'Today';
    } else {
      return date.toLocaleDateString([], { month: 'short', day: 'numeric' });
    }
  };

  if (connectionStatus === 'error') {
    return (
      <div className="loading">
        <div style={{ textAlign: 'center' }}>
          <h2 style={{ color: 'var(--error)', marginBottom: '8px' }}>📱 Connection Failed</h2>
          <p>Cannot connect to YourOwnAI server.</p>
          <p style={{ fontSize: '12px', marginTop: '8px', color: 'var(--text-secondary)' }}>
            Make sure the server is running on your phone:<br/>
            <strong>Settings → Local Network Sync → Start Server</strong>
          </p>
          <button 
            onClick={checkConnection}
            style={{ marginTop: '16px' }}
            className="primary-btn"
          >
            🔄 Retry Connection
          </button>
        </div>
      </div>
    );
  }

  if (isLoading && conversations.length === 0) {
    return <div className="loading">⏳ Loading conversations...</div>;
  }

  return (
    <div className="app">
      {/* Sidebar */}
      <div className="sidebar">
        <div className="sidebar-header">
          <div>
            <h1>📱 YourOwnAI</h1>
            {serverInfo && (
              <div className="server-device">
                {serverInfo.deviceInfo.deviceName}
              </div>
            )}
          </div>
        </div>
        
        {/* Search */}
        <div className="search-box">
          <input
            type="text"
            placeholder="🔍 Search conversations..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="search-input"
          />
          {searchQuery && (
            <button 
              className="clear-search"
              onClick={() => setSearchQuery('')}
            >
              ✕
            </button>
          )}
        </div>
        
        {/* Memories button */}
        <button 
          className={`memories-btn ${showMemories ? 'active' : ''}`}
          onClick={() => {
            setShowMemories(!showMemories);
            setSelectedConversation(null);
          }}
        >
          🧠 Memories ({memories.length})
        </button>
        
        {/* Conversations list */}
        <div className="conversation-list">
          {filteredConversations.length === 0 ? (
            <div className="empty-state-small">
              <p>No conversations found</p>
            </div>
          ) : (
            filteredConversations.map(conv => (
              <div
                key={conv.id}
                className={`conversation-item ${selectedConversation?.id === conv.id ? 'active' : ''}`}
                onClick={() => selectConversation(conv)}
              >
                <div>
                  <div className="conversation-title">{conv.title}</div>
                  <div className="conversation-date">{formatDate(conv.updatedAt)}</div>
                </div>
                {conv.isPinned && <span className="pin-icon">📌</span>}
              </div>
            ))
          )}
        </div>
        
        {/* Footer */}
        <div className="sidebar-footer">
          <div className="status-info">
            <div className="status-dot"></div>
            <span>Connected to {serverInfo?.deviceInfo.deviceName || 'Phone'}</span>
          </div>
        </div>
      </div>

      {/* Main Content Area */}
      <div className="chat-area">
        {showMemories ? (
          // Memories View
          <>
            <div className="chat-header">
              <div className="chat-title">🧠 Your Memories</div>
              <div className="header-actions">
                <span className="memory-count">{memories.length} total</span>
              </div>
            </div>

            <div className="memories-container">
              {memories.length === 0 ? (
                <div className="empty-state">
                  <h2>No memories yet</h2>
                  <p>Memories will appear here as you chat</p>
                </div>
              ) : (
                <div className="memories-grid">
                  {memories.map(memory => (
                    <div key={memory.id} className="memory-card">
                      <div className="memory-content">{memory.fact}</div>
                      <div className="memory-time">
                        {new Date(memory.createdAt).toLocaleDateString()}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </>
        ) : selectedConversation ? (
          // Chat View
          <>
            <div className="chat-header">
              <div className="chat-title">{selectedConversation.title}</div>
              <div className="header-actions">
                <button className="icon-btn" onClick={exportToMarkdown} title="Export as Markdown">
                  📄 MD
                </button>
                <button className="icon-btn" onClick={exportToTxt} title="Export as Text">
                  📝 TXT
                </button>
                <button 
                  className="icon-btn" 
                  onClick={() => window.location.reload()}
                  title="Refresh"
                >
                  🔄
                </button>
              </div>
            </div>

            <div className="messages-container">
              {messages.length === 0 ? (
                <div className="empty-state">
                  <h2>No messages yet</h2>
                  <p>Start chatting on your phone</p>
                </div>
              ) : (
                <>
                  {messages.map(msg => (
                    <div key={msg.id} className={`message ${msg.role.toLowerCase()}`}>
                      <div className="message-avatar">
                        {msg.role === 'USER' ? '👤' : '🤖'}
                      </div>
                      <div className="message-wrapper">
                        <div className="message-content">
                          {msg.content.split('\n').map((line, i) => (
                            <p key={i}>{line || '\u00A0'}</p>
                          ))}
                        </div>
                        <div className="message-footer">
                          <span className="message-time">{formatTime(msg.timestamp)}</span>
                          {msg.model && <span className="message-model">• {msg.model}</span>}
                          <button 
                            className="copy-btn"
                            onClick={() => {
                              navigator.clipboard.writeText(msg.content);
                              alert('Copied to clipboard!');
                            }}
                            title="Copy to clipboard"
                          >
                            📋
                          </button>
                        </div>
                      </div>
                    </div>
                  ))}
                  <div ref={messagesEndRef} />
                </>
              )}
            </div>

            {/* View-only notice */}
            <div className="view-only-notice">
              <div className="notice-content">
                <span className="notice-icon">👀</span>
                <span>View-only mode • To send messages, use your phone</span>
              </div>
            </div>
          </>
        ) : (
          // Empty state - no chat selected
          <div className="empty-state">
            <h2>📱 YourOwnAI Web Viewer</h2>
            <p>Select a conversation from the sidebar to view messages</p>
            {serverInfo && (
              <div className="welcome-info">
                <p>Connected to <strong>{serverInfo.deviceInfo.deviceName}</strong></p>
                <p className="stats">
                  {serverInfo.totalConversations} conversations • {serverInfo.totalMemories} memories
                </p>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default App;
