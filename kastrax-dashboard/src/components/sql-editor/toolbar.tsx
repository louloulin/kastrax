import React from "react";
import { 
  Play,
  Pause,
  Save,
  FileText,
  Clock,
  Database,
  Settings,
  LayoutGrid,
  AlignJustify,
  Table as TableIcon,
  RefreshCw,
  Share,
  PanelLeft,
  MoreHorizontal,
  Plus,
} from "lucide-react";

interface ToolbarProps {
  onExecute: () => void;
  onExecuteSelected: () => void;
  onFormat: () => void;
  onSave: () => void;
  onToggleHistory: () => void;
  onNewQuery: () => void;
  isLoading: boolean;
  activeConnection?: string;
  activeDatabase?: string;
  showHistory: boolean;
}

export default function Toolbar({ 
  onExecute, 
  onExecuteSelected, 
  onFormat, 
  onSave,
  onToggleHistory,
  onNewQuery,
  isLoading,
  activeConnection,
  activeDatabase,
  showHistory
}: ToolbarProps) {
  return (
    <div className="bg-card border border-border rounded-md mb-2">
      <div className="flex items-center p-1 border-b border-border overflow-x-auto">
        <div className="flex items-center">
          <button
            onClick={onNewQuery}
            className="p-1.5 text-muted-foreground hover:text-foreground hover:bg-secondary rounded-sm flex items-center mr-1"
            title="新建查询"
          >
            <Plus size={16} />
          </button>
          
          <button
            onClick={onExecute}
            disabled={isLoading}
            className="bg-primary text-primary-foreground hover:bg-primary/90 rounded-sm px-2.5 py-1.5 text-sm font-medium flex items-center mr-1 disabled:opacity-50"
            title="执行查询 (Ctrl+Enter)"
          >
            {isLoading ? <Pause size={14} className="mr-1.5" /> : <Play size={14} className="mr-1.5" />}
            运行
          </button>
          
          <button
            onClick={onExecuteSelected}
            disabled={isLoading}
            className="border border-input bg-background hover:bg-secondary rounded-sm px-2.5 py-1.5 text-sm flex items-center mr-1 disabled:opacity-50"
            title="执行选中的查询"
          >
            <Play size={14} className="mr-1.5 text-primary" />
            运行选中
          </button>
          
          <button
            onClick={onFormat}
            className="p-1.5 text-muted-foreground hover:text-foreground hover:bg-secondary rounded-sm flex items-center mr-1"
            title="格式化 SQL"
          >
            <AlignJustify size={16} />
          </button>
          
          <button
            onClick={onSave}
            className="p-1.5 text-muted-foreground hover:text-foreground hover:bg-secondary rounded-sm flex items-center mr-1"
            title="保存查询"
          >
            <Save size={16} />
          </button>
          
          <div className="h-5 border-l border-border mx-1"></div>
          
          <button
            onClick={onToggleHistory}
            className={`p-1.5 hover:bg-secondary rounded-sm flex items-center mr-1 ${
              showHistory ? "text-primary bg-primary/10" : "text-muted-foreground hover:text-foreground"
            }`}
            title="显示历史记录"
          >
            <Clock size={16} />
          </button>
        </div>
        
        <div className="flex-grow"></div>
        
        <div className="flex items-center">
          <div className="flex items-center bg-secondary/30 rounded-sm border border-border mr-2">
            <span className="flex items-center px-2 py-1 text-xs border-r border-border">
              <Database size={12} className="mr-1 text-muted-foreground" />
              <span className="font-medium">连接:</span>
            </span>
            <span className="px-2 py-1 text-xs">
              {activeConnection || "未连接"}
            </span>
          </div>
          
          <div className="flex items-center bg-secondary/30 rounded-sm border border-border">
            <span className="flex items-center px-2 py-1 text-xs border-r border-border">
              <TableIcon size={12} className="mr-1 text-muted-foreground" />
              <span className="font-medium">数据库:</span>
            </span>
            <span className="px-2 py-1 text-xs">
              {activeDatabase || "未选择"}
            </span>
          </div>
        </div>
      </div>
      
      <div className="flex items-center px-3 py-1.5 text-xs text-muted-foreground">
        <div className="flex items-center mr-4">
          <FileText size={12} className="mr-1" />
          <span>query.sql</span>
        </div>
        
        <div className="flex items-center">
          <RefreshCw size={12} className="mr-1" />
          <span>自动保存已启用</span>
        </div>
      </div>
    </div>
  );
} 