import React, { useState, useEffect } from "react";
import SupabaseSidebar from "@/components/supabase-sidebar";
import TopNavigation from "@/components/top-navigation";
import DataTable from "@/components/data-table";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Plus,
  Search,
  Eye,
  EyeOff,
  Trash2,
  Edit,
  RefreshCw,
  Menu,
  Copy,
  Loader2
} from "lucide-react";
import { getVariableList, Variable } from "@/lib/api/variables";
import { useToast } from "@/components/ui/use-toast";
import VariableDialog from "@/components/variables/variable-dialog";
import VariableDeleteDialog from "@/components/variables/variable-delete-dialog";

// 提取为单独的组件
const VariableValueCell = React.memo(({ value, row }: { value: string, row: any }) => {
  const [showValue, setShowValue] = useState(false);
  
  // 安全获取变量值，避免undefined错误
  const variableValue = row?.original?.variableValue || value || '';
  
  return (
    <div className="flex items-center">
      <div className="flex-1 mr-2 overflow-hidden text-ellipsis">
        {showValue ? variableValue : '••••••••'}
      </div>
      <Button
        variant="ghost"
        size="sm"
        className="h-6 w-6 p-0 flex-shrink-0"
        onClick={() => setShowValue(!showValue)}
        title={showValue ? "隐藏值" : "显示值"}
      >
        {showValue ? <EyeOff className="h-3 w-3" /> : <Eye className="h-3 w-3" />}
      </Button>
    </div>
  );
});

// Column definition for variables
const variableColumns = [
  { id: "variableName", header: "变量名称", accessorKey: "variableName" },
  { 
    id: "variableValue", 
    header: "变量值", 
    accessorKey: "variableValue",
    cell: ({ row }: any) => {
      // 安全地访问变量值
      const value = row?.original?.variableValue || '';
      return <VariableValueCell value={value} row={row} />;
    }
  },
  { id: "description", header: "描述", accessorKey: "description" },
  { id: "updateTime", header: "更新时间", accessorKey: "updateTime" },
];

export default function VariablesPage() {
  const { toast } = useToast();
  const [variables, setVariables] = useState<Variable[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  
  // 对话框状态
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [selectedVariable, setSelectedVariable] = useState<Variable | null>(null);
  
  // Load variables on component mount
  useEffect(() => {
    fetchVariables();
  }, []);
  
  // Function to fetch variables from API
  const fetchVariables = async () => {
    setLoading(true);
    try {
      const response = await getVariableList();
      // 确保每行数据都有唯一的id
      const dataWithIds = (response.data.data || []).map((variable: Variable) => ({
        ...variable,
        id: variable.variableId // 使用variableId作为行id
      }));
      setVariables(dataWithIds);
    } catch (error) {
      console.error("Failed to fetch variables:", error);
      toast({
        variant: "destructive",
        title: "加载失败",
        description: "无法加载变量列表，请刷新重试",
      });
    } finally {
      setLoading(false);
    }
  };

  // Function to copy variable value to clipboard
  const copyToClipboard = (value: string | undefined, name: string | undefined) => {
    if (!value) {
      toast({
        variant: "destructive",
        title: "复制失败",
        description: "变量值为空，无法复制"
      });
      return;
    }
    
    navigator.clipboard.writeText(value);
    toast({
      title: "已复制到剪贴板",
      description: `变量 "${name || '未命名'}" 的值已复制到剪贴板。`
    });
  };

  // 编辑变量
  const handleEdit = (variable: Variable) => {
    setSelectedVariable(variable);
    setEditDialogOpen(true);
  };

  // 删除变量
  const handleDelete = (variable: Variable) => {
    setSelectedVariable(variable);
    setDeleteDialogOpen(true);
  };

  // Filter variables based on search query
  const filteredVariables = variables.filter(variable => 
    variable.variableName.toLowerCase().includes(searchQuery.toLowerCase()) ||
    (variable.description && variable.description.toLowerCase().includes(searchQuery.toLowerCase()))
  );
  
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
              <h1 className="text-2xl font-medium">变量管理</h1>
              <p className="text-muted-foreground">管理环境变量和配置</p>
            </div>
            
            <div className="flex gap-2">
              <Button variant="outline" size="sm" onClick={fetchVariables} disabled={loading}>
                {loading ? (
                  <Loader2 size={16} className="mr-2 animate-spin" />
                ) : (
                  <RefreshCw size={16} className="mr-2" />
                )}
                刷新
              </Button>
              <Button size="sm" onClick={() => setCreateDialogOpen(true)}>
                <Plus size={16} className="mr-2" />
                新建变量
              </Button>
            </div>
          </div>
          
          <div className="mb-6">
            <div className="relative">
              <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
              <Input
                type="search"
                placeholder="搜索变量名称或描述..."
                className="pl-8"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>
          </div>
          
          <DataTable 
            title="变量列表"
            description="管理系统环境变量和配置"
            columns={variableColumns}
            data={filteredVariables}
            isLoading={loading}
            actions={(row: Variable & { id?: string }) => (
              <div className="flex gap-2">
                <Button
                  variant="ghost"
                  size="sm"
                  className="h-8 w-8 p-0"
                  title="复制变量值"
                  onClick={() => copyToClipboard(row?.variableValue, row?.variableName)}
                >
                  <Copy className="h-4 w-4" />
                </Button>
                <Button 
                  variant="ghost" 
                  size="sm" 
                  className="h-8 w-8 p-0" 
                  title="编辑变量"
                  onClick={() => handleEdit(row)}
                >
                  <Edit className="h-4 w-4" />
                </Button>
                <Button 
                  variant="ghost" 
                  size="sm" 
                  className="h-8 w-8 p-0" 
                  title="删除变量"
                  onClick={() => handleDelete(row)}
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>
            )}
          />
        </div>
      </div>

      {/* 创建变量对话框 */}
      <VariableDialog
        open={createDialogOpen}
        onOpenChange={setCreateDialogOpen}
        onSuccess={fetchVariables}
      />

      {/* 编辑变量对话框 */}
      <VariableDialog
        open={editDialogOpen}
        onOpenChange={setEditDialogOpen}
        variable={selectedVariable || undefined}
        onSuccess={fetchVariables}
      />

      {/* 删除变量确认对话框 */}
      <VariableDeleteDialog
        open={deleteDialogOpen}
        onOpenChange={setDeleteDialogOpen}
        variable={selectedVariable}
        onSuccess={fetchVariables}
      />
    </div>
  );
} 