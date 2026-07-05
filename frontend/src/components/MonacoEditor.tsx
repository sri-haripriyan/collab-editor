"use client";

import React from 'react';
import Editor, { Monaco } from '@monaco-editor/react';

interface MonacoEditorProps {
  language: string;
  onMount: (editor: any, monaco: Monaco) => void;
  defaultValue?: string;
}

const MonacoEditor: React.FC<MonacoEditorProps> = ({ language, onMount, defaultValue }) => {
  const handleEditorDidMount = (editor: any, monaco: Monaco) => {
    // Configure Monaco themes
    monaco.editor.defineTheme('collab-dark', {
      base: 'vs-dark',
      inherit: true,
      rules: [],
      colors: {
        'editor.background': '#1e293b', // Slate 800 background matching the theme
        'editor.lineHighlightBackground': '#334155', // Slate 700 highlight
      }
    });
    
    monaco.editor.setTheme('collab-dark');
    onMount(editor, monaco);
  };

  // Convert language code for Monaco compatibility
  const getMonacoLanguage = (lang: string) => {
    const l = lang.toLowerCase();
    if (l === 'cpp' || l === 'c++') return 'cpp';
    return l;
  };

  return (
    <Editor
      height="100%"
      language={getMonacoLanguage(language)}
      defaultValue={defaultValue}
      onMount={handleEditorDidMount}
      options={{
        fontSize: 14,
        fontFamily: "'Fira Code', monospace, Consolas, 'Courier New'",
        minimap: { enabled: true },
        automaticLayout: true,
        wordWrap: 'on',
        cursorBlinking: 'smooth',
        cursorSmoothCaretAnimation: 'on',
        smoothScrolling: true,
      }}
    />
  );
};

export default MonacoEditor;
