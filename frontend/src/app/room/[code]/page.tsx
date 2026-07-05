"use client";

import React, { useState, useEffect, useRef } from 'react';
import { useParams, useRouter } from 'next/navigation';
import dynamic from 'next/dynamic';
import { useAuth } from '@/context/AuthContext';
import { roomApi, executionApi } from '@/services/api';
import { useWebRTC } from '@/hooks/useWebRTC';
import { useYjsWebSocket } from '@/hooks/useYjsWebSocket';
import { Room, UserPresence, WsMessage, ExecutionResult, Snapshot } from '@/types';
import { 
  Code2, Play, Users, MessageSquare, Mic, MicOff, Shield, ShieldAlert,
  ArrowLeft, Terminal, FileCode, CheckCircle, RefreshCw, Send
} from 'lucide-react';
import styles from '@/styles/room.module.css';

// Dynamically import Monaco Editor to avoid Next.js Server Side Rendering (SSR) issues
const MonacoEditor = dynamic(() => import('@/components/MonacoEditor'), { ssr: false });

export default function RoomPage() {
  const { code } = useParams();
  const roomCode = (Array.isArray(code) ? code[0] : code) || "";
  const router = useRouter();
  const { user } = useAuth();

  const [room, setRoom] = useState<Room | null>(null);
  const [role, setRole] = useState<'OWNER' | 'EDITOR' | 'VIEWER'>('VIEWER');
  const [activeUsers, setActiveUsers] = useState<UserPresence[]>([]);
  const [chatMessages, setChatMessages] = useState<any[]>([]);
  const [chatInput, setChatInput] = useState('');
  
  // Editor & Language states
  const [editor, setEditor] = useState<any>(null);
  const [language, setLanguage] = useState('python');
  
  // Console Panels states
  const [activeTab, setActiveTab] = useState<'input' | 'output'>('input');
  const [stdin, setStdin] = useState('');
  const [executionResult, setExecutionResult] = useState<ExecutionResult | null>(null);
  const [isRunning, setIsRunning] = useState(false);

  // Status indicators
  const [saveStatus, setSaveStatus] = useState<'Saved' | 'Saving...' | 'Error saving'>('Saved');
  const [voiceCallActive, setVoiceCallActive] = useState(false);

  // Synchronization state
  const latestSnapshotRef = useRef<string | null>(null);
  const activeCountRef = useRef<number | null>(null);
  const hasPopulatedRef = useRef(false);

  // WebSocket reference
  const ws = useRef<WebSocket | null>(null);
  const chatBottomRef = useRef<HTMLDivElement>(null);

  // 1. WebSocket connection setup
  useEffect(() => {
    if (!user || !roomCode) return;

    const token = localStorage.getItem('token');
    const wsBaseUrl = process.env.NEXT_PUBLIC_WS_BASE_URL || 'ws://localhost:8080';
    const wsUrl = `${wsBaseUrl}/ws/room?token=${token}&code=${roomCode}`;
    const socket = new WebSocket(wsUrl);
    ws.current = socket;

    socket.onopen = () => {
      // Socket connected
    };

    socket.onmessage = (event) => {
      const message: WsMessage = JSON.parse(event.data);
      handleIncomingWsMessage(message);
    };

    socket.onclose = () => {
      // Reconnection logic in a production app
    };

    // Load initial Room properties
    roomApi.joinRoom(roomCode)
      .then(res => {
        setRoom(res.data);
        setRole(res.data.role);
        // Default select language based on latest saved code if possible
        roomApi.getSnapshots(roomCode).then(snapRes => {
          if (snapRes.data && snapRes.data.length > 0) {
            setLanguage(snapRes.data[0].language);
            latestSnapshotRef.current = snapRes.data[0].content;
            populateInitialContent(snapRes.data[0].content, activeCountRef.current);
          }
        });
      })
      .catch(() => {
        router.push('/dashboard');
      });

    return () => {
      if (socket.readyState === WebSocket.OPEN) {
        socket.close();
      }
    };
  }, [roomCode, user, router]);

  // WebSocket message sender
  const sendWsMessage = (message: WsMessage) => {
    if (ws.current && ws.current.readyState === WebSocket.OPEN) {
      ws.current.send(JSON.stringify(message));
    }
  };

  // 2. Custom synchronization hooks
  const yjs = useYjsWebSocket({
    roomId: roomCode,
    editor,
    role,
    sendWsMessage
  });

  const populateInitialContent = (content: string | null, activeCount: number | null) => {
    if (hasPopulatedRef.current) return;
    if (content !== null && activeCount === 1 && yjs.yText && yjs.yText.toString() === "") {
      yjs.yText.insert(0, content);
      hasPopulatedRef.current = true;
    }
  };

  const webrtc = useWebRTC({
    roomId: roomCode,
    currentUser: user,
    activeUsers,
    sendWsMessage
  });

  // 3. Handle incoming WebSocket events
  const handleIncomingWsMessage = (message: WsMessage) => {
    const payload = message.payload;
    const type = message.type;
    const senderId = message.senderId;

    switch (type) {
      case 'JOIN_ACK':
        setRole(payload.role);
        setActiveUsers(payload.activeUsers);
        activeCountRef.current = payload.activeUsers.length;
        populateInitialContent(latestSnapshotRef.current, payload.activeUsers.length);
        break;

      case 'USER_JOINED':
        const newUser: UserPresence = payload.user;
        setActiveUsers(prev => {
          if (prev.some(u => u.userId === newUser.userId)) return prev;
          return [...prev, newUser];
        });
        // If voice chat is currently active for this user, automatically offer WebRTC
        if (voiceCallActive) {
          webrtc.startAudio();
        }
        break;

      case 'USER_LEFT':
        if (senderId !== undefined) {
          setActiveUsers(prev => prev.filter(u => u.userId !== senderId));
          webrtc.closePeerConnection(senderId);
        }
        break;

      case 'SIGNAL':
        if (senderId !== undefined && payload && payload.signal) {
          webrtc.handleSignalingMessage(senderId, payload.signal);
        }
        break;

      case 'YJS_SYNC':
        if (payload && payload.update) {
          yjs.handleYjsUpdate(payload.update);
        }
        break;

      case 'CURSOR_UPDATE':
        if (senderId && payload) {
          const senderPresence = activeUsers.find(u => u.userId === senderId);
          if (senderPresence) {
            yjs.handleRemoteCursor(
              senderId,
              senderPresence.username,
              senderPresence.color,
              payload.line,
              payload.column
            );
          }
        }
        break;

      case 'MIC_STATE':
        const isMutedVal = payload.isMuted;
        if (senderId !== undefined) {
          webrtc.setPeersMuteState(prev => ({
            ...prev,
            [senderId]: isMutedVal
          }));
          setActiveUsers(prev => prev.map(u => {
            if (u.userId === senderId) {
              return { ...u, isMuted: isMutedVal };
            }
            return u;
          }));
        }
        break;

      case 'PERMISSIONS_UPDATE':
        const targetUsername = payload.username;
        const newRole = payload.role;
        
        setActiveUsers(prev => prev.map(u => {
          if (u.username === targetUsername) {
            return { ...u, role: newRole };
          }
          return u;
        }));

        if (user && user.username === targetUsername) {
          setRole(newRole);
        }
        break;

      case 'CHAT_MESSAGE':
        setChatMessages(prev => [...prev, {
          senderName: message.senderName,
          senderId: senderId,
          text: payload.text,
          timestamp: payload.timestamp
        }]);
        break;
    }
  };

  // Scroll to bottom of chat panel when messages update
  useEffect(() => {
    chatBottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [chatMessages]);

  // 4. Code compilation/execution trigger
  const runCode = async () => {
    if (!yjs.yText || isRunning) return;
    setIsRunning(true);
    setActiveTab('output');
    
    try {
      const codeString = yjs.yText.toString();
      const response = await executionApi.executeCode(codeString, language, stdin);
      setExecutionResult(response.data);
    } catch (err: any) {
      setExecutionResult({
        stdout: '',
        stderr: err.response?.data?.error || 'Failed to connect to execution server.',
        exitCode: 1,
        timeMs: 0
      });
    } finally {
      setIsRunning(false);
    }
  };

  // 5. Periodic Database Auto-saving
  useEffect(() => {
    if (role === 'VIEWER' || !yjs.yText) return;

    const interval = setInterval(async () => {
      setSaveStatus('Saving...');
      try {
        const content = yjs.yText.toString();
        await roomApi.saveSnapshot(roomCode, content, language);
        setSaveStatus('Saved');
      } catch (err) {
        setSaveStatus('Error saving');
      }
    }, 30000); // Trigger auto-save every 30 seconds

    return () => clearInterval(interval);
  }, [roomCode, language, role, yjs.yText]);

  // 6. Voice communication triggers
  const handleToggleVoiceCall = () => {
    if (voiceCallActive) {
      webrtc.stopAudio();
      setVoiceCallActive(false);
    } else {
      webrtc.startAudio();
      setVoiceCallActive(true);
    }
  };

  // 7. Chat Message dispatcher
  const sendChatMessage = (e: React.FormEvent) => {
    e.preventDefault();
    if (!chatInput.trim()) return;

    sendWsMessage({
      type: 'CHAT_MESSAGE',
      roomId: roomCode,
      senderName: user?.username,
      payload: {
        text: chatInput.trim(),
        timestamp: Date.now()
      }
    });

    // Append locally
    setChatMessages(prev => [...prev, {
      senderName: user?.username,
      senderId: user?.id,
      text: chatInput.trim(),
      timestamp: Date.now()
    }]);

    setChatInput('');
  };

  // 8. Owner User Permissions Toggle
  const handlePermissionChange = (targetUsername: string, currentRole: string) => {
    const updatedRole = currentRole === 'EDITOR' ? 'VIEWER' : 'EDITOR';
    
    // Dispatch permission API request
    roomApi.updatePermission(roomCode, targetUsername, updatedRole)
      .then(() => {
        // Broadcast change via WS
        sendWsMessage({
          type: 'PERMISSIONS_UPDATE',
          roomId: roomCode,
          payload: {
            username: targetUsername,
            role: updatedRole
          }
        });

        // Update locally
        setActiveUsers(prev => prev.map(u => {
          if (u.username === targetUsername) {
            return { ...u, role: updatedRole };
          }
          return u;
        }));
      })
      .catch(() => {
        // Log permission adjustment error
      });
  };

  return (
    <div className={styles.container}>
      <header className={styles.navbar}>
        <div className={styles.navLeft}>
          <button onClick={() => router.push('/dashboard')} className="btn-secondary" style={{ padding: '6px 12px' }}>
            <ArrowLeft size={16} />
            <span>Dashboard</span>
          </button>
          <span className={styles.roomTitle}>{room?.name}</span>
          <span className={styles.roomCode}>{roomCode}</span>
        </div>

        <div className={styles.navCenter}>
          <select 
            value={language} 
            onChange={(e) => setLanguage(e.target.value)}
            className={styles.roleSelect}
            style={{ 
              background: 'rgba(255,255,255,0.05)', 
              border: '1px solid var(--border-color)',
              borderRadius: '4px',
              padding: '6px 12px',
              color: 'var(--text-primary)',
              fontSize: '13px'
            }}
            disabled={role === 'VIEWER'}
          >
            <option value="python">Python</option>
            <option value="java">Java</option>
            <option value="cpp">C++</option>
          </select>
        </div>

        <div className={styles.navRight}>
          <div className={styles.autoSaveIndicator}>
            <RefreshCw size={12} className={saveStatus === 'Saving...' ? 'glow-effect' : ''} />
            <span>{saveStatus}</span>
          </div>

          <button 
            onClick={handleToggleVoiceCall} 
            className={voiceCallActive ? 'btn-primary glow-effect' : 'btn-secondary'}
            style={{ padding: '6px 12px', background: voiceCallActive ? '#ef4444' : '' }}
          >
            <Mic size={16} />
            <span>{voiceCallActive ? 'Leave Voice' : 'Join Voice'}</span>
          </button>

          {voiceCallActive && (
            <button 
              onClick={webrtc.toggleMute} 
              className="btn-secondary"
              style={{ padding: '6px', minWidth: '36px', justifyContent: 'center' }}
            >
              {webrtc.isMuted ? <MicOff size={16} className={`${styles.micIcon} ${styles.muted}`} /> : <Mic size={16} />}
            </button>
          )}

          <button 
            onClick={runCode} 
            className={styles.runBtn} 
            disabled={isRunning}
          >
            {isRunning ? (
              <span>Running...</span>
            ) : (
              <>
                <Play size={14} fill="white" />
                <span>Run Code</span>
              </>
            )}
          </button>
        </div>
      </header>

      <div className={styles.workspace}>
        {/* Sidebar Panel */}
        <aside className={styles.sidebar}>
          {/* Active Participants List */}
          <div className={styles.sidebarSection}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Users size={16} />
              <h3>Participants ({activeUsers.length})</h3>
            </div>
            <div className={styles.participantList}>
              {activeUsers.map(participant => (
                <div key={participant.userId} className={styles.participantRow}>
                  <div className={styles.pUser}>
                    <div className={styles.userDot} style={{ backgroundColor: participant.color }}></div>
                    <span className={styles.pName} style={{ color: participant.userId === user?.id ? '#ffffff' : '' }}>
                      {participant.username} {participant.userId === user?.id && '(You)'}
                    </span>
                  </div>
                  
                  <div className={styles.pActions}>
                    {participant.isMuted ? <MicOff size={14} className={styles.micIcon + ' ' + styles.muted} /> : <Mic size={14} className={styles.micIcon} />}
                    
                    {role === 'OWNER' && participant.userId !== user?.id ? (
                      <button 
                        onClick={() => handlePermissionChange(participant.username, participant.role)}
                        className={styles.roleSelect}
                      >
                        {participant.role === 'EDITOR' ? 'Set Viewer' : 'Set Editor'}
                      </button>
                    ) : (
                      <span className={styles.badge} style={{ fontSize: '9px', opacity: 0.7 }}>
                        {participant.role}
                      </span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Text Chat Panel */}
          <div className={styles.chatSection}>
            <div className={styles.sidebarSection} style={{ borderBottom: 'none', paddingBottom: '8px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <MessageSquare size={16} />
                <h3>Room Chat</h3>
              </div>
            </div>
            
            <div className={styles.chatMessages}>
              {chatMessages.map((msg, i) => (
                <div key={i} className={`${styles.chatBubble} ${msg.senderId === user?.id ? styles.self : ''}`}>
                  <div className={styles.chatMeta}>
                    <span>{msg.senderName}</span>
                    <span>{new Date(msg.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                  </div>
                  <div className={styles.chatBody}>
                    {msg.text}
                  </div>
                </div>
              ))}
              <div ref={chatBottomRef} />
            </div>

            <form onSubmit={sendChatMessage} className={styles.chatInputArea}>
              <input 
                type="text" 
                placeholder="Type a message..." 
                className={styles.chatInput}
                value={chatInput}
                onChange={(e) => setChatInput(e.target.value)}
              />
              <button type="submit" className="btn-primary" style={{ padding: '8px' }}>
                <Send size={14} />
              </button>
            </form>
          </div>
        </aside>

        {/* Editor Center & Console Bottom Panel */}
        <section className={styles.editorContainer}>
          <div className={styles.monacoWrap}>
            {role === 'VIEWER' && (
              <div className={styles.viewerRestriction}>
                <ShieldAlert size={14} style={{ display: 'inline', marginRight: '4px', verticalAlign: 'middle' }} />
                <span>You are in View-Only Mode</span>
              </div>
            )}
            <MonacoEditor 
              language={language} 
              onMount={(editor) => setEditor(editor)} 
            />
          </div>

          {/* Input & Output Panels */}
          <div className={styles.consolePanel}>
            <div className={styles.consoleHeader}>
              <div className={styles.consoleTabs}>
                <button 
                  onClick={() => setActiveTab('input')} 
                  className={`${styles.consoleTab} ${activeTab === 'input' ? styles.active : ''}`}
                >
                  <Terminal size={14} style={{ display: 'inline', marginRight: '4px', verticalAlign: 'middle' }} />
                  <span>Standard Input</span>
                </button>
                <button 
                  onClick={() => setActiveTab('output')} 
                  className={`${styles.consoleTab} ${activeTab === 'output' ? styles.active : ''}`}
                >
                  <FileCode size={14} style={{ display: 'inline', marginRight: '4px', verticalAlign: 'middle' }} />
                  <span>Standard Output</span>
                </button>
              </div>
            </div>

            <div className={styles.consoleContent}>
              {activeTab === 'input' ? (
                <textarea 
                  className={styles.consoleTextarea}
                  placeholder="Provide test inputs for standard execution here..."
                  value={stdin}
                  onChange={(e) => setStdin(e.target.value)}
                  disabled={role === 'VIEWER'}
                />
              ) : (
                <div className={styles.consoleOutput}>
                  {executionResult ? (
                    <>
                      <div className={styles.outputHeader}>
                        <span>Exit Code: {executionResult.exitCode}</span>
                        <span>Execution Time: {executionResult.timeMs}ms</span>
                      </div>
                      {executionResult.stdout && (
                        <div className={styles.stdoutText}>
                          {executionResult.stdout}
                        </div>
                      )}
                      {executionResult.stderr && (
                        <div className={styles.stderrText}>
                          {executionResult.stderr}
                        </div>
                      )}
                      {!executionResult.stdout && !executionResult.stderr && (
                        <span style={{ color: 'var(--text-muted)' }}>Code executed successfully with no console outputs.</span>
                      )}
                    </>
                  ) : (
                    <span style={{ color: 'var(--text-muted)' }}>Click &apos;Run Code&apos; to view stdout/stderr results.</span>
                  )}
                </div>
              )}
            </div>
          </div>
        </section>
      </div>
    </div>
  );
}
