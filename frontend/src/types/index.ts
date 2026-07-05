export interface User {
  id: number;
  username: string;
  email: string;
}

export interface Room {
  id: number;
  code: string;
  name: string;
  ownerId: number;
  ownerUsername: string;
  role: 'OWNER' | 'EDITOR' | 'VIEWER';
  createdAt: string;
}

export interface UserPresence {
  userId: number;
  username: string;
  role: 'OWNER' | 'EDITOR' | 'VIEWER';
  isMuted: boolean;
  color: string;
}

export interface Snapshot {
  id: number;
  content: string;
  language: string;
  savedAt: string;
  savedByUsername: string;
}

export interface WsMessage {
  type: string;
  roomId: string;
  senderId?: number;
  senderName?: string;
  payload?: any;
}

export interface ExecutionResult {
  stdout: string;
  stderr: string;
  exitCode: number;
  timeMs: number;
}
