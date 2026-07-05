import { useEffect, useRef, useState } from 'react';
import * as Y from 'yjs';
import { MonacoBinding } from 'y-monaco';
import { WsMessage } from '@/types';

interface UseYjsWebSocketOptions {
  roomId: string;
  editor: any; // monaco editor instance
  role: 'OWNER' | 'EDITOR' | 'VIEWER';
  sendWsMessage: (msg: WsMessage) => void;
}

export const useYjsWebSocket = ({ roomId, editor, role, sendWsMessage }: UseYjsWebSocketOptions) => {
  const [yDoc] = useState(() => new Y.Doc());
  const [yText] = useState(() => yDoc.getText('monaco'));
  const bindingRef = useRef<any>(null);

  // Track remote cursors decorations
  // Maps userId -> DecorationIds (String[])
  const remoteCursors = useRef<Record<number, string[]>>({});

  // Bind Yjs text to Monaco Editor when editor instance is ready
  useEffect(() => {
    if (!editor) return;

    // Initialize Monaco Yjs Binding
    const binding = new MonacoBinding(
      yText,
      editor.getModel(),
      new Set([editor])
    );
    bindingRef.current = binding;

    // Track Yjs document updates and send them to WebSocket
    const handleDocUpdate = (update: Uint8Array, origin: any) => {
      if (origin !== 'remote-sync') {
        const updateBase64 = arrayBufferToBase64(update);
        sendWsMessage({
          type: 'YJS_SYNC',
          roomId,
          payload: { update: updateBase64 }
        });
      }
    };

    yDoc.on('update', handleDocUpdate);

    // Track local cursor position changes and broadcast them
    const cursorListener = editor.onDidChangeCursorPosition((e: any) => {
      sendWsMessage({
        type: 'CURSOR_UPDATE',
        roomId,
        payload: {
          line: e.position.lineNumber,
          column: e.position.column,
        }
      });
    });

    return () => {
      yDoc.off('update', handleDocUpdate);
      cursorListener.dispose();
      if (bindingRef.current) {
        bindingRef.current.destroy();
        bindingRef.current = null;
      }
      
      // Clear remote cursors decorations
      Object.keys(remoteCursors.current).forEach(userIdStr => {
        const userId = parseInt(userIdStr);
        clearRemoteCursor(userId);
      });
    };
  }, [editor, yDoc, yText, roomId]);

  // Set editor read-only option separately to avoid reconstructing the binding on role changes
  useEffect(() => {
    if (editor) {
      editor.updateOptions({ readOnly: role === 'VIEWER' });
    }
  }, [editor, role]);

  // Handle incoming Yjs updates
  const handleYjsUpdate = (updateBase64: string) => {
    try {
      const update = base64ToArrayBuffer(updateBase64);
      Y.applyUpdate(yDoc, update, 'remote-sync');
    } catch (err) {
      // Log sync conversion issues
    }
  };

  // Handle incoming remote cursors
  const handleRemoteCursor = (userId: number, username: string, color: string, line: number, column: number) => {
    if (!editor) return;

    // Clear existing cursor for this user
    clearRemoteCursor(userId);

    // If position is invalid, do not add decoration
    if (line <= 0 || column <= 0) return;

    // Add Monaco Editor decorations for remote cursor and label tag
    const cursorDecoration = {
      range: {
        startLineNumber: line,
        startColumn: column,
        endLineNumber: line,
        endColumn: column
      },
      options: {
        className: `remote-cursor-${userId}`,
        beforeContentClassName: `remote-cursor-widget-${userId}`,
        hoverMessage: { value: username }
      }
    };

    // Inject custom CSS for dynamic coloring of cursor and user tooltips
    injectCursorStyles(userId, username, color);

    const newDecorations = editor.deltaDecorations([], [cursorDecoration]);
    remoteCursors.current[userId] = newDecorations;
  };

  const clearRemoteCursor = (userId: number) => {
    if (editor && remoteCursors.current[userId]) {
      editor.deltaDecorations(remoteCursors.current[userId], []);
      delete remoteCursors.current[userId];
    }
  };

  const injectCursorStyles = (userId: number, username: string, color: string) => {
    const styleId = `remote-cursor-styles-${userId}`;
    let styleEl = document.getElementById(styleId);
    if (!styleEl) {
      styleEl = document.createElement('style');
      styleEl.id = styleId;
      document.head.appendChild(styleEl);
    }
    styleEl.innerHTML = `
      .remote-cursor-${userId} {
        border-left: 2px solid ${color} !important;
        position: relative;
      }
      .remote-cursor-widget-${userId} {
        content: '${username}';
        position: absolute;
        top: -15px;
        left: 2px;
        background: ${color};
        color: #fff;
        font-size: 8px;
        font-family: sans-serif;
        padding: 1px 3px;
        border-radius: 2px;
        opacity: 0.8;
        white-space: nowrap;
        pointer-events: none;
        z-index: 10;
        display: none;
      }
      .remote-cursor-${userId}:hover .remote-cursor-widget-${userId} {
        display: block;
      }
    `;
  };

  // Utility ArrayBuffer <-> Base64 helpers
  const arrayBufferToBase64 = (buffer: Uint8Array): string => {
    let binary = '';
    const len = buffer.byteLength;
    for (let i = 0; i < len; i++) {
      binary += String.fromCharCode(buffer[i]);
    }
    return window.btoa(binary);
  };

  const base64ToArrayBuffer = (base64: string): Uint8Array => {
    const binaryString = window.atob(base64);
    const len = binaryString.length;
    const bytes = new Uint8Array(len);
    for (let i = 0; i < len; i++) {
      bytes[i] = binaryString.charCodeAt(i);
    }
    return bytes;
  };

  return {
    yText,
    handleYjsUpdate,
    handleRemoteCursor,
    clearRemoteCursor,
  };
};
