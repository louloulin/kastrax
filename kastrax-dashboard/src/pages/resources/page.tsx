import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import SupabaseSidebar from "../../components/supabase-sidebar";
import TopNavigation from "../../components/top-navigation";
import DataTable from "../../components/data-table";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import { 
  Plus, 
  Search, 
  Database, 
  Trash2, 
  Edit, 
  MoreHorizontal, 
  RefreshCw,
  Menu,
  ExternalLink,
} from "lucide-react";
import { getResourceList, deleteResource, Resource } from "../../lib/api/resources";
import { toast } from "../../components/ui/use-toast";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "../../components/ui/alert-dialog";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../../components/ui/tabs";
import ResourceTypeManager from "../../components/resources/resource-type-manager";

// Column definition for resources
const resourceColumns = [
  { id: "resourceName", header: "资源名称", accessorKey: "resourceName" },
  { id: "resourceType", header: "资源类型", accessorKey: "resourceType" },
  { id: "status", header: "状态", accessorKey: "status", 
    cell: ({ row }: any) => (
      <div className="flex items-center">
        <div className={`h-2 w-2 rounded-full mr-2 ${row.original.status === 1 ? 'bg-green-500' : 'bg-red-500'}`}></div>
        <span>{row.original.status === 1 ? '正常' : '异常'}</span>
      </div>
    )
  },
  { id: "createTime", header: "创建时间", accessorKey: "createTime" },
];

export default function ResourcesPage() {
  const navigate = useNavigate();
  const [resources, setResources] = useState<Resource[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [resourceToDelete, setResourceToDelete] = useState<Resource | null>(null);
  
  // Load resources on component mount
  useEffect(() => {
    fetchResources();
  }, []);
  
  // Function to fetch resources from API
  const fetchResources = async () => {
    setLoading(true);
    try {
      const response = await getResourceList();
      setResources(response.data.data || []);
    } catch (error) {
      console.error("Failed to fetch resources:", error);
      toast({
        title: "获取资源列表失败",
        description: "请检查网络连接后重试",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };
  
  // Filter resources based on search query
  const filteredResources = resources.filter(resource => 
    resource.resourceName.toLowerCase().includes(searchQuery.toLowerCase()) ||
    resource.resourceType.toLowerCase().includes(searchQuery.toLowerCase())
  );
  
  // Handle resource deletion
  const handleDeleteResource = async () => {
    if (!resourceToDelete) return;
    
    try {
      await deleteResource(resourceToDelete.resourceId);
      setResources(resources.filter(r => r.resourceId !== resourceToDelete.resourceId));
      toast({
        title: "资源已删除",
        description: `资源 "${resourceToDelete.resourceName}" 已成功删除`,
      });
    } catch (error) {
      console.error("Failed to delete resource:", error);
      toast({
        title: "删除资源失败",
        description: "请稍后重试",
        variant: "destructive",
      });
    } finally {
      setResourceToDelete(null);
      setDeleteDialogOpen(false);
    }
  };
  
  // Open delete confirmation dialog
  const confirmDelete = (resource: Resource) => {
    setResourceToDelete(resource);
    setDeleteDialogOpen(true);
  };
  
  return (
    <div className="flex min-h-screen bg-background">
      <SupabaseSidebar collapsed={sidebarCollapsed} setCollapsed={setSidebarCollapsed} />
      
      <div className="flex-1 flex flex-col">
        <TopNavigation>
          <Button variant="ghost" size="sm" onClick={() => setSidebarCollapsed(!sidebarCollapsed)}>
            <Menu size={16} />
          </Button>
        </TopNavigation>
        
        <div className="flex-1 p-6 overflow-auto">
          <div className="flex justify-between items-center mb-6">
            <div>
              <h1 className="text-2xl font-medium">资源管理</h1>
              <p className="text-muted-foreground">管理各种类型的数据库和服务资源连接</p>
            </div>
            
            <div className="flex gap-2">
              <Button variant="outline" size="sm" onClick={fetchResources}>
                <RefreshCw size={16} className="mr-2" />
                刷新
              </Button>
              <Button size="sm" onClick={() => navigate('/resources/create')}>
                <Plus size={16} className="mr-2" />
                新建资源
              </Button>
            </div>
          </div>
          
          <Tabs defaultValue="resources" className="space-y-4">
            <TabsList className="grid w-full grid-cols-3">
              <TabsTrigger value="resources">资源列表</TabsTrigger>
              <TabsTrigger value="types">资源类型</TabsTrigger>
              <TabsTrigger value="settings">配置设置</TabsTrigger>
            </TabsList>
            
            <TabsContent value="resources">
              <div className="mb-6">
                <div className="relative">
                  <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
                  <Input
                    type="search"
                    placeholder="搜索资源名称或类型..."
                    className="pl-8"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                  />
                </div>
              </div>
              
              <DataTable 
                title="资源列表"
                description="管理已连接的数据库和服务"
                columns={resourceColumns}
                data={filteredResources}
                isLoading={loading}
                actions={(row: any) => (
                  <div className="flex gap-2">
                    <Button 
                      variant="ghost" 
                      size="sm" 
                      className="h-8 w-8 p-0" 
                      onClick={() => navigate(`/resources/${row.original.resourceId}`)}
                      title="查看详情"
                    >
                      <ExternalLink className="h-4 w-4" />
                    </Button>
                    <Button 
                      variant="ghost" 
                      size="sm" 
                      className="h-8 w-8 p-0"
                      onClick={() => navigate(`/resources/${row.original.resourceId}`)}
                      title="编辑资源"
                    >
                      <Edit className="h-4 w-4" />
                    </Button>
                    <Button 
                      variant="ghost" 
                      size="sm" 
                      className="h-8 w-8 p-0"
                      onClick={() => confirmDelete(row.original)}
                      title="删除资源"
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                )}
                onRowClick={(row) => navigate(`/resources/${row.resourceId}`)}
              />
            </TabsContent>
            
            <TabsContent value="types">
              <ResourceTypeManager />
            </TabsContent>
            
            <TabsContent value="settings">
              <div className="rounded-lg border p-6">
                <h2 className="text-xl font-semibold mb-4">资源配置设置</h2>
                <p className="text-muted-foreground">
                  资源配置设置功能即将推出...
                </p>
              </div>
            </TabsContent>
          </Tabs>
        </div>
      </div>
      
      {/* Delete confirmation dialog */}
      <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>确认删除资源</AlertDialogTitle>
            <AlertDialogDescription>
              您确定要删除资源 "{resourceToDelete?.resourceName}" 吗？此操作无法撤销。
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>取消</AlertDialogCancel>
            <AlertDialogAction onClick={handleDeleteResource} className="bg-destructive text-destructive-foreground">
              删除
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
} 