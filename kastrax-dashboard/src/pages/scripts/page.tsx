import React, { useState, useEffect } from "react";
import SupabaseSidebar from "@/components/supabase-sidebar";
import TopNavigation from "@/components/top-navigation";
import DataTable from "@/components/data-table";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Plus,
  Search,
  Trash2,
  Edit,
  RefreshCw,
  Menu,
  Play,
  Code,
  Download,
  Tag,
  Loader2
} from "lucide-react";
import { getScriptList, Script, ScriptLanguage } from "@/lib/api/scripts";
import { useToast } from "@/components/ui/use-toast";
import { Badge } from "@/components/ui/badge";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import ScriptDialog from "@/components/scripts/script-dialog";
import ScriptDeleteDialog from "@/components/scripts/script-delete-dialog";
import ScriptRunDialog from "@/components/scripts/script-run-dialog";

// 脚本语言标签样式映射
const languageBadgeStyles = {
  [ScriptLanguage.TYPESCRIPT]: "bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300",
  [ScriptLanguage.JAVASCRIPT]: "bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-300",
  [ScriptLanguage.PYTHON]: "bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300",
  [ScriptLanguage.GROOVY]: "bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-300"
};

// 脚本语言显示名称
const languageDisplayNames = {
  [ScriptLanguage.TYPESCRIPT]: "TypeScript",
  [ScriptLanguage.JAVASCRIPT]: "JavaScript",
  [ScriptLanguage.PYTHON]: "Python",
  [ScriptLanguage.GROOVY]: "Groovy"
};

// 脚本表格列定义
const scriptColumns = [
  { id: "scriptName", header: "脚本名称", accessorKey: "scriptName" },
  { 
    id: "language", 
    header: "语言", 
    accessorKey: "language",
    cell: ({ row }: any) => {
      const language = row?.original?.language;
      if (!language) return null;
      
      const style = languageBadgeStyles[language as ScriptLanguage] || "";
      const displayName = languageDisplayNames[language as ScriptLanguage] || language;
      
      return (
        <Badge className={style} variant="outline">
          {displayName}
        </Badge>
      );
    }
  },
  { 
    id: "dependencies", 
    header: "依赖", 
    accessorKey: "dependencies",
    cell: ({ row }: any) => {
      const dependencies = row?.original?.dependencies || [];
      return dependencies.length > 0 ? (
        <Badge variant="outline" className="bg-slate-100 dark:bg-slate-800">
          {dependencies.length} 个依赖
        </Badge>
      ) : (
        <span className="text-muted-foreground text-xs">无依赖</span>
      );
    }
  },
  { id: "description", header: "描述", accessorKey: "description" },
  { id: "updateTime", header: "更新时间", accessorKey: "updateTime" },
];

export default function ScriptsPage() {
  const { toast } = useToast();
  const [scripts, setScripts] = useState<Script[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [languageFilter, setLanguageFilter] = useState<string>("all");
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  
  // 对话框状态
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [runDialogOpen, setRunDialogOpen] = useState(false);
  const [selectedScript, setSelectedScript] = useState<Script | null>(null);
  
  // 加载脚本列表
  useEffect(() => {
    fetchScripts();
  }, []);
  
  // 获取脚本列表
  const fetchScripts = async () => {
    setLoading(true);
    try {
      const response = await getScriptList();
      // 确保每行数据都有唯一的id
      const dataWithIds = (response.data.data.list || []).map((script: Script) => ({
        ...script,
        id: script.scriptId // 使用scriptId作为行id
      }));
      setScripts(dataWithIds);
    } catch (error) {
      console.error("Failed to fetch scripts:", error);
      toast({
        variant: "destructive",
        title: "加载失败",
        description: "无法加载脚本列表，请刷新重试",
      });
    } finally {
      setLoading(false);
    }
  };

  // 编辑脚本
  const handleEdit = (script: Script) => {
    setSelectedScript(script);
    setEditDialogOpen(true);
  };

  // 删除脚本
  const handleDelete = (script: Script) => {
    setSelectedScript(script);
    setDeleteDialogOpen(true);
  };
  
  // 执行脚本
  const handleRun = (script: Script) => {
    setSelectedScript(script);
    setRunDialogOpen(true);
  };

  // 过滤脚本列表
  const filteredScripts = scripts.filter(script => {
    // 名称或描述包含搜索词
    const matchesSearch = (
      script.scriptName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (script.description && script.description.toLowerCase().includes(searchQuery.toLowerCase()))
    );
    
    // 语言过滤
    const matchesLanguage = languageFilter === "all" || script.language === languageFilter;
    
    return matchesSearch && matchesLanguage;
  });
  
  return (
    <div className="flex min-h-screen bg-background">
      <SupabaseSidebar collapsed={sidebarCollapsed} setCollapsed={setSidebarCollapsed} />
      
      <div className="flex-1 flex flex-col">
        <TopNavigation>
          <Button 
            variant="ghost" 
            size="sm" 
            onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
            className="md:hidden"
          >
            <Menu size={16} />
          </Button>
        </TopNavigation>
        
        <div className="flex-1 p-6 overflow-auto">
          <div className="flex justify-between items-center mb-6">
            <div>
              <h1 className="text-2xl font-medium">脚本管理</h1>
              <p className="text-muted-foreground">创建和管理自动化脚本</p>
            </div>
            
            <div className="flex gap-2">
              <Button variant="outline" size="sm" onClick={fetchScripts} disabled={loading}>
                {loading ? (
                  <Loader2 size={16} className="mr-2 animate-spin" />
                ) : (
                  <RefreshCw size={16} className="mr-2" />
                )}
                刷新
              </Button>
              <Button size="sm" onClick={() => setCreateDialogOpen(true)}>
                <Plus size={16} className="mr-2" />
                新建脚本
              </Button>
            </div>
          </div>
          
          <div className="mb-6 flex flex-col sm:flex-row gap-4">
            <div className="relative flex-1">
              <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
              <Input
                type="search"
                placeholder="搜索脚本名称或描述..."
                className="pl-8"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>
            
            <div className="w-full sm:w-64">
              <Select
                value={languageFilter}
                onValueChange={setLanguageFilter}
              >
                <SelectTrigger>
                  <SelectValue placeholder="筛选语言" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">所有语言</SelectItem>
                  <SelectItem value={ScriptLanguage.TYPESCRIPT}>TypeScript</SelectItem>
                  <SelectItem value={ScriptLanguage.JAVASCRIPT}>JavaScript</SelectItem>
                  <SelectItem value={ScriptLanguage.PYTHON}>Python</SelectItem>
                  <SelectItem value={ScriptLanguage.GROOVY}>Groovy</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
          
          <DataTable 
            title="脚本列表"
            description="管理自动化脚本和依赖"
            columns={scriptColumns}
            data={filteredScripts}
            isLoading={loading}
            actions={(row: Script & { id?: string }) => (
              <div className="flex gap-2">
                <Button
                  variant="ghost"
                  size="sm"
                  className="h-8 w-8 p-0"
                  title="执行脚本"
                  onClick={() => handleRun(row)}
                >
                  <Play className="h-4 w-4" />
                </Button>
                <Button 
                  variant="ghost" 
                  size="sm" 
                  className="h-8 w-8 p-0" 
                  title="编辑脚本"
                  onClick={() => handleEdit(row)}
                >
                  <Edit className="h-4 w-4" />
                </Button>
                <Button 
                  variant="ghost" 
                  size="sm" 
                  className="h-8 w-8 p-0" 
                  title="删除脚本"
                  onClick={() => handleDelete(row)}
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>
            )}
          />
        </div>
      </div>

      {/* 创建脚本对话框 */}
      <ScriptDialog
        open={createDialogOpen}
        onOpenChange={setCreateDialogOpen}
        onSuccess={fetchScripts}
      />

      {/* 编辑脚本对话框 */}
      <ScriptDialog
        open={editDialogOpen}
        onOpenChange={setEditDialogOpen}
        script={selectedScript || undefined}
        onSuccess={fetchScripts}
      />

      {/* 删除脚本确认对话框 */}
      <ScriptDeleteDialog
        open={deleteDialogOpen}
        onOpenChange={setDeleteDialogOpen}
        script={selectedScript}
        onSuccess={fetchScripts}
      />
      
      {/* 执行脚本对话框 */}
      <ScriptRunDialog
        open={runDialogOpen}
        onOpenChange={setRunDialogOpen}
        script={selectedScript}
      />
    </div>
  );
}