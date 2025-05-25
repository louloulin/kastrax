import React, { useRef } from "react";
import Editor, { OnMount } from "@monaco-editor/react";
import { format } from "sql-formatter";

interface EditorPanelProps {
  value: string;
  onChange: (value: string) => void;
  onExecute: () => void;
}

export default function EditorPanel({ value, onChange, onExecute }: EditorPanelProps) {
  const editorRef = useRef<any>(null);
  
  const handleEditorDidMount: OnMount = (editor) => {
    editorRef.current = editor;
    
    // 添加快捷键
    editor.addCommand(
      // Monaco.KeyMod.CtrlCmd | Monaco.KeyCode.Enter
      2048 | 3, // Ctrl+Enter
      () => {
        onExecute();
      }
    );
  };
  
  const handleFormat = () => {
    try {
      const formattedQuery = format(value, {
        language: "mysql",
        tabWidth: 2,
        keywordCase: "upper",
        linesBetweenQueries: 2,
      });
      
      onChange(formattedQuery);
    } catch (error) {
      console.error("Failed to format SQL:", error);
    }
  };
  
  return (
    <div className="flex flex-col h-full">
      <div className="border border-border rounded-md overflow-hidden flex-grow">
        <Editor
          height="100%"
          language="sql"
          value={value}
          onChange={(value) => onChange(value || "")}
          onMount={handleEditorDidMount}
          options={{
            minimap: { enabled: false },
            scrollBeyondLastLine: false,
            fontSize: 14,
            wordWrap: "on",
            lineNumbers: "on",
            tabSize: 2,
            automaticLayout: true,
            fontFamily: "Menlo, Monaco, 'Courier New', monospace",
            contextmenu: true,
            scrollbar: {
              alwaysConsumeMouseWheel: false,
            },
            suggestOnTriggerCharacters: true,
            acceptSuggestionOnEnter: "on",
            quickSuggestions: true,
            codeLens: true,
            folding: true,
            theme: "vs-dark",
          }}
        />
      </div>
    </div>
  );
} 