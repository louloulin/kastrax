import React, { useState } from "react";
import { Link, useLocation } from "react-router-dom";
import {
  Database,
  Home,
  Table,
  Code,
  Users,
  Settings,
  HelpCircle,
  ChevronDown,
  ExternalLink,
  ChevronLeft,
  ChevronRight,
  FileCode,
  Save,
  KeyRound,
  BarChart2,
} from "lucide-react";

interface SupabaseSidebarProps {
  collapsed?: boolean;
  setCollapsed?: (collapsed: boolean) => void;
}

export default function SupabaseSidebar({ collapsed, setCollapsed }: SupabaseSidebarProps) {
  const location = useLocation();
  const pathname = location.pathname;
  const [internalCollapsed, setInternalCollapsed] = useState(false);
  
  // Use either the controlled or internal state
  const isCollapsed = collapsed !== undefined ? collapsed : internalCollapsed;
  const toggleCollapsed = () => {
    if (setCollapsed) {
      setCollapsed(!isCollapsed);
    } else {
      setInternalCollapsed(!isCollapsed);
    }
  };

  const isPathActive = (path: string) => {
    return pathname === path || pathname === path + "/";
  };

  return (
    <div 
      className={`hidden md:flex ${isCollapsed ? 'w-16' : 'w-64'} h-screen flex-col bg-card border-r border-border transition-all duration-300`}
    >
      <div className="h-16 border-b border-border flex items-center px-4 justify-between">
        <Link to="/" className="flex items-center">
          <div className="w-6 h-6 bg-primary rounded-md flex items-center justify-center mr-2">
            <Database size={14} className="text-white" />
          </div>
          {!isCollapsed && <span className="font-semibold">DataFlare</span>}
        </Link>
        <button 
          onClick={toggleCollapsed}
          className="p-1 rounded-md hover:bg-secondary text-muted-foreground hover:text-foreground transition-colors"
        >
          {isCollapsed ? <ChevronRight size={16} /> : <ChevronLeft size={16} />}
        </button>
      </div>
      
      <div className="flex-1 overflow-auto py-4 px-3">
        <nav className="space-y-1">
          <Link 
            to="/dashboard" 
            className={`flex items-center px-3 py-2 rounded-md ${isPathActive("/dashboard") || isPathActive("/") ? "bg-primary/10 text-primary" : "text-muted-foreground hover:bg-secondary/80 hover:text-foreground"}`}
            title="仪表盘"
          >
            <Home size={16} />
            {!isCollapsed && <span className="ml-3">仪表盘</span>}
          </Link>
          
          {!isCollapsed && (
            <div className="pt-4 pb-2">
              <div className="flex items-center justify-between px-3">
                <h4 className="text-xs font-medium text-muted-foreground">资源</h4>
                <button className="text-xs text-muted-foreground hover:text-foreground">
                  <ChevronDown size={14} />
                </button>
              </div>
            </div>
          )}
          
          {isCollapsed && <div className="my-4 border-t border-border"></div>}
          
          <Link 
            to="/resources" 
            className={`flex items-center px-3 py-2 rounded-md ${isPathActive("/resources") ? "bg-primary/10 text-primary" : "text-muted-foreground hover:bg-secondary/80 hover:text-foreground"}`}
            title="资源管理"
          >
            <Database size={16} />
            {!isCollapsed && <span className="ml-3">资源管理</span>}
          </Link>
          
          <Link 
            to="/rules" 
            className={`flex items-center px-3 py-2 rounded-md ${isPathActive("/rules") ? "bg-primary/10 text-primary" : "text-muted-foreground hover:bg-secondary/80 hover:text-foreground"}`}
            title="规则管理"
          >
            <Code size={16} />
            {!isCollapsed && <span className="ml-3">规则管理</span>}
          </Link>
          
          <Link 
            to="/scripts" 
            className={`flex items-center px-3 py-2 rounded-md ${isPathActive("/scripts") ? "bg-primary/10 text-primary" : "text-muted-foreground hover:bg-secondary/80 hover:text-foreground"}`}
            title="脚本管理"
          >
            <FileCode size={16} />
            {!isCollapsed && <span className="ml-3">脚本管理</span>}
          </Link>
          
          <Link 
            to="/variables" 
            className={`flex items-center px-3 py-2 rounded-md ${isPathActive("/variables") ? "bg-primary/10 text-primary" : "text-muted-foreground hover:bg-secondary/80 hover:text-foreground"}`}
            title="变量管理"
          >
            <KeyRound size={16} />
            {!isCollapsed && <span className="ml-3">变量管理</span>}
          </Link>
          
          <Link 
            to="/backups" 
            className={`flex items-center px-3 py-2 rounded-md ${isPathActive("/backups") ? "bg-primary/10 text-primary" : "text-muted-foreground hover:bg-secondary/80 hover:text-foreground"}`}
            title="备份管理"
          >
            <Save size={16} />
            {!isCollapsed && <span className="ml-3">备份管理</span>}
          </Link>
          
          {!isCollapsed && (
            <div className="pt-4 pb-2">
              <div className="flex items-center justify-between px-3">
                <h4 className="text-xs font-medium text-muted-foreground">数据</h4>
                <button className="text-xs text-muted-foreground hover:text-foreground">
                  <ChevronDown size={14} />
                </button>
              </div>
            </div>
          )}
          
          {isCollapsed && <div className="my-4 border-t border-border"></div>}
          
          <Link 
            to="/data-import-export" 
            className={`flex items-center px-3 py-2 rounded-md ${isPathActive("/data-import-export") ? "bg-primary/10 text-primary" : "text-muted-foreground hover:bg-secondary/80 hover:text-foreground"}`}
            title="数据导入导出"
          >
            <ExternalLink size={16} />
            {!isCollapsed && <span className="ml-3">数据导入导出</span>}
          </Link>
          
          <Link 
            to="/data-visualization" 
            className={`flex items-center px-3 py-2 rounded-md ${isPathActive("/data-visualization") ? "bg-primary/10 text-primary" : "text-muted-foreground hover:bg-secondary/80 hover:text-foreground"}`}
            title="数据可视化"
          >
            <BarChart2 size={16} />
            {!isCollapsed && <span className="ml-3">数据可视化</span>}
          </Link>
          
          <Link 
            to="/database" 
            className={`flex items-center px-3 py-2 rounded-md ${isPathActive("/database") ? "bg-primary/10 text-primary" : "text-muted-foreground hover:bg-secondary/80 hover:text-foreground"}`}
            title="数据库"
          >
            <Database size={16} />
            {!isCollapsed && <span className="ml-3">数据库</span>}
          </Link>
          
          <Link 
            to="/tables" 
            className={`flex items-center px-3 py-2 rounded-md ${isPathActive("/tables") ? "bg-primary/10 text-primary" : "text-muted-foreground hover:bg-secondary/80 hover:text-foreground"}`}
            title="表格"
          >
            <Table size={16} />
            {!isCollapsed && <span className="ml-3">表格</span>}
          </Link>
          
          <Link 
            to="/sql" 
            className={`flex items-center px-3 py-2 rounded-md ${isPathActive("/sql") ? "bg-primary/10 text-primary" : "text-muted-foreground hover:bg-secondary/80 hover:text-foreground"}`}
            title="SQL 编辑器"
          >
            <Code size={16} />
            {!isCollapsed && <span className="ml-3">SQL 编辑器</span>}
          </Link>
          
          {!isCollapsed && (
            <div className="pt-4 pb-2">
              <div className="flex items-center justify-between px-3">
                <h4 className="text-xs font-medium text-muted-foreground">管理</h4>
                <button className="text-xs text-muted-foreground hover:text-foreground">
                  <ChevronDown size={14} />
                </button>
              </div>
            </div>
          )}
          
          {isCollapsed && <div className="my-4 border-t border-border"></div>}
          
          <Link 
            to="/users" 
            className={`flex items-center px-3 py-2 rounded-md ${isPathActive("/users") ? "bg-primary/10 text-primary" : "text-muted-foreground hover:bg-secondary/80 hover:text-foreground"}`}
            title="用户"
          >
            <Users size={16} />
            {!isCollapsed && <span className="ml-3">用户</span>}
          </Link>
          
          <Link 
            to="/settings" 
            className={`flex items-center px-3 py-2 rounded-md ${isPathActive("/settings") ? "bg-primary/10 text-primary" : "text-muted-foreground hover:bg-secondary/80 hover:text-foreground"}`}
            title="设置"
          >
            <Settings size={16} />
            {!isCollapsed && <span className="ml-3">设置</span>}
          </Link>
        </nav>
      </div>
      
      {!isCollapsed && (
        <div className="p-4 border-t border-border">
          <div className="p-3 bg-secondary/50 rounded-md">
            <h4 className="text-sm font-medium mb-2">需要帮助？</h4>
            <p className="text-xs text-muted-foreground mb-3">
              查看我们的文档或联系支持团队
            </p>
            <a 
              href="#" 
              className="text-xs text-primary flex items-center"
            >
              <HelpCircle size={14} className="mr-1" />
              <span>文档</span>
              <ExternalLink size={10} className="ml-1" />
            </a>
          </div>
        </div>
      )}
    </div>
  );
} 