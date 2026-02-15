// TypeScript types matching Android backend

export interface DeviceInfo {
  deviceId: string;
  deviceName: string;
  appVersion: string;
  platform: string;
}

export interface ServerStatus {
  isRunning: boolean;
  deviceInfo: DeviceInfo;
  port: number;
  totalConversations: number;
  totalMessages: number;
  totalMemories: number;
  totalPersonas: number;
}

export interface Conversation {
  id: string;
  title: string;
  personaId: string | null;
  createdAt: number;
  updatedAt: number;
  isArchived: boolean;
  isPinned: boolean;
  webSearchEnabled: boolean;
}

export enum MessageRole {
  USER = "USER",
  ASSISTANT = "ASSISTANT",
  SYSTEM = "SYSTEM"
}

export interface Message {
  id: string;
  conversationId: string;
  content: string;
  role: MessageRole;
  timestamp: number;
  tokens: number;
  model: string | null;
  imageAttachments: string | null;
  fileAttachments: string | null;
  isStreaming: boolean;
  webSearchEnabled: boolean;
}

export interface MemoryEntry {
  id: string;
  conversationId: string | null;
  messageId: string | null;
  fact: string;
  createdAt: number;
  personaId: string | null;
}

export interface Persona {
  id: string;
  name: string;
  description: string;
  systemPrompt: string;
  createdAt: number;
  updatedAt: number;
  isArchived: boolean;
}

export interface SendMessageRequest {
  content: string;
  personaId?: string | null;
  webSearchEnabled?: boolean;
}

export interface CreateConversationRequest {
  title?: string;
  personaId?: string | null;
}

export interface StreamChunk {
  chunk: string;
  done?: boolean;
}

export interface ModelInfo {
  id: string;
  displayName: string;
  provider: string;
}
