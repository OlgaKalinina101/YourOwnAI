// API Client for YourOwnAI Android Server

import type {
  ServerStatus,
  Conversation,
  Message,
  MemoryEntry,
  Persona,
  SendMessageRequest,
  CreateConversationRequest,
  StreamChunk
} from '../types';

const API_BASE_URL = process.env.REACT_APP_API_URL || '';

class ApiClient {
  private baseUrl: string;

  constructor(baseUrl: string = API_BASE_URL) {
    this.baseUrl = baseUrl;
  }

  // Server status
  async getServerStatus(): Promise<ServerStatus> {
    const response = await fetch(`${this.baseUrl}/status`);
    if (!response.ok) throw new Error('Failed to fetch server status');
    return response.json();
  }

  // Conversations
  async getConversations(): Promise<Conversation[]> {
    const response = await fetch(`${this.baseUrl}/api/conversations`);
    if (!response.ok) throw new Error('Failed to fetch conversations');
    return response.json();
  }

  async createConversation(request: CreateConversationRequest): Promise<Conversation> {
    const response = await fetch(`${this.baseUrl}/api/conversations`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request)
    });
    if (!response.ok) throw new Error('Failed to create conversation');
    return response.json();
  }

  async deleteConversation(id: string): Promise<void> {
    const response = await fetch(`${this.baseUrl}/api/conversations/${id}`, {
      method: 'DELETE'
    });
    if (!response.ok) throw new Error('Failed to delete conversation');
  }

  // Messages
  async getMessages(conversationId: string): Promise<Message[]> {
    const response = await fetch(`${this.baseUrl}/api/conversations/${conversationId}/messages`);
    if (!response.ok) throw new Error('Failed to fetch messages');
    return response.json();
  }

  // Send message with streaming response
  async sendMessage(
    conversationId: string,
    request: SendMessageRequest,
    onChunk: (chunk: string) => void,
    onDone: () => void,
    onError: (error: Error) => void
  ): Promise<void> {
    try {
      const response = await fetch(`${this.baseUrl}/api/conversations/${conversationId}/messages`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request)
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const reader = response.body?.getReader();
      if (!reader) {
        throw new Error('No response body');
      }

      const decoder = new TextDecoder();

      while (true) {
        const { value, done } = await reader.read();
        
        if (done) {
          onDone();
          break;
        }

        const chunk = decoder.decode(value, { stream: true });
        
        // Parse SSE format: "data: {...}\n\n"
        const lines = chunk.split('\n');
        for (const line of lines) {
          if (line.startsWith('data: ')) {
            try {
              const json = JSON.parse(line.substring(6));
              if (json.chunk) {
                onChunk(json.chunk);
              }
              if (json.done) {
                onDone();
                return;
              }
            } catch (e) {
              // Ignore parse errors for partial chunks
            }
          }
        }
      }
    } catch (error) {
      onError(error as Error);
    }
  }

  // Memories
  async getMemories(): Promise<MemoryEntry[]> {
    const response = await fetch(`${this.baseUrl}/api/memories`);
    if (!response.ok) throw new Error('Failed to fetch memories');
    return response.json();
  }

  // Personas
  async getPersonas(): Promise<Persona[]> {
    const response = await fetch(`${this.baseUrl}/api/personas`);
    if (!response.ok) throw new Error('Failed to fetch personas');
    return response.json();
  }
}

export const apiClient = new ApiClient();
