import { useEffect, useState } from 'react';
import { useRouter } from 'next/router';
import { Plus, RefreshCw, Play, Edit, Trash } from 'lucide-react';
import { Workflow, getWorkflowList, deleteWorkflow, runWorkflow } from '@/lib/api/workflows';
import { useToast } from '@/components/ui/use-toast';
import { Button } from '@/components/ui/button';
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Badge } from '@/components/ui/badge';
import { SupabaseSidebar } from '@/components/supabase-sidebar';
import { TopNavigation } from '@/components/top-navigation';
import { formatDistanceToNow } from 'date-fns';
import { zhCN } from 'date-fns/locale';

export default function WorkflowsPage() {
  const router = useRouter();
  const { toast } = useToast();
  const [workflows, setWorkflows] = useState<Workflow[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [createWorkflowName, setCreateWorkflowName] = useState('');
  const [createWorkflowDescription, setCreateWorkflowDescription] = useState('');
  const [isConfirmDeleteOpen, setIsConfirmDeleteOpen] = useState(false);
  const [workflowToDelete, setWorkflowToDelete] = useState<string | null>(null);
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

  // Load workflows
  useEffect(() => {
    loadWorkflows();
  }, []);

  const loadWorkflows = async () => {
    setLoading(true);
    try {
      const data = await getWorkflowList();
      setWorkflows(data);
    } catch (error) {
      console.error('Error loading workflows:', error);
      toast({
        title: '加载失败',
        description: '无法加载工作流列表，请稍后重试',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  // Filter workflows based on search query
  const filteredWorkflows = workflows.filter(workflow => 
    workflow.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    workflow.description.toLowerCase().includes(searchQuery.toLowerCase())
  );

  // Handle workflow creation
  const handleCreateWorkflow = async () => {
    if (!createWorkflowName.trim()) {
      toast({
        title: '请输入工作流名称',
        variant: 'destructive',
      });
      return;
    }

    try {
      const response = await fetch('/api/workflows', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          name: createWorkflowName,
          description: createWorkflowDescription,
        }),
      });
      
      if (!response.ok) throw new Error('创建工作流失败');
      
      const data = await response.json();
      
      toast({
        title: '创建成功',
        description: '工作流已创建',
      });
      
      // Reset form and close dialog
      setCreateWorkflowName('');
      setCreateWorkflowDescription('');
      setIsCreateDialogOpen(false);
      
      // Redirect to workflow editor
      router.push(`/workflows/editor/${data.id}`);
    } catch (error) {
      console.error('Error creating workflow:', error);
      toast({
        title: '创建失败',
        description: '工作流创建失败，请稍后重试',
        variant: 'destructive',
      });
    }
  };

  // Handle workflow deletion
  const handleDeleteWorkflow = async (id: string) => {
    setWorkflowToDelete(id);
    setIsConfirmDeleteOpen(true);
  };

  const confirmDeleteWorkflow = async () => {
    if (!workflowToDelete) return;
    
    try {
      await deleteWorkflow(workflowToDelete);
      
      toast({
        title: '删除成功',
        description: '工作流已删除',
      });
      
      // Refresh workflow list
      loadWorkflows();
    } catch (error) {
      console.error('Error deleting workflow:', error);
      toast({
        title: '删除失败',
        description: '工作流删除失败，请稍后重试',
        variant: 'destructive',
      });
    } finally {
      setIsConfirmDeleteOpen(false);
      setWorkflowToDelete(null);
    }
  };

  // Handle workflow execution
  const handleRunWorkflow = async (id: string) => {
    try {
      await runWorkflow(id);
      
      toast({
        title: '执行成功',
        description: '工作流已开始执行',
      });
    } catch (error) {
      console.error('Error running workflow:', error);
      toast({
        title: '执行失败',
        description: '工作流执行失败，请稍后重试',
        variant: 'destructive',
      });
    }
  };

  // Get status badge
  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return <Badge className="bg-green-500">活跃</Badge>;
      case 'DISABLED':
        return <Badge variant="outline">禁用</Badge>;
      case 'DRAFT':
        return <Badge variant="secondary">草稿</Badge>;
      default:
        return <Badge variant="outline">{status}</Badge>;
    }
  };

  return (
    <div className="flex h-screen">
      <SupabaseSidebar
        collapsed={sidebarCollapsed}
        setCollapsed={setSidebarCollapsed}
      />
      <div className="flex-1 flex flex-col overflow-hidden">
        <TopNavigation />
        <main className="flex-1 overflow-auto p-6">
          <div className="flex justify-between items-center mb-6">
            <div>
              <h1 className="text-2xl font-bold">工作流管理</h1>
              <p className="text-muted-foreground">
                创建和管理数据处理工作流
              </p>
            </div>
            <div className="flex gap-2">
              <Button 
                onClick={loadWorkflows} 
                variant="outline" 
                size="sm"
                disabled={loading}
              >
                <RefreshCw className={`h-4 w-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
                刷新
              </Button>
              <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
                <DialogTrigger asChild>
                  <Button>
                    <Plus className="h-4 w-4 mr-2" />
                    新建工作流
                  </Button>
                </DialogTrigger>
                <DialogContent>
                  <DialogHeader>
                    <DialogTitle>创建新工作流</DialogTitle>
                    <DialogDescription>
                      输入工作流信息，创建后将跳转至编辑器进行节点配置
                    </DialogDescription>
                  </DialogHeader>
                  <div className="space-y-4 py-2">
                    <div className="space-y-2">
                      <Label htmlFor="name">工作流名称</Label>
                      <Input
                        id="name"
                        placeholder="输入工作流名称"
                        value={createWorkflowName}
                        onChange={(e) => setCreateWorkflowName(e.target.value)}
                      />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="description">描述（可选）</Label>
                      <Textarea
                        id="description"
                        placeholder="输入工作流描述"
                        value={createWorkflowDescription}
                        onChange={(e) => setCreateWorkflowDescription(e.target.value)}
                      />
                    </div>
                  </div>
                  <DialogFooter>
                    <Button variant="outline" onClick={() => setIsCreateDialogOpen(false)}>
                      取消
                    </Button>
                    <Button onClick={handleCreateWorkflow}>
                      创建
                    </Button>
                  </DialogFooter>
                </DialogContent>
              </Dialog>
            </div>
          </div>

          <div className="mb-6">
            <Input
              placeholder="搜索工作流..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="max-w-md"
            />
          </div>

          {loading ? (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {[1, 2, 3].map(i => (
                <Card key={i} className="opacity-70">
                  <CardHeader className="pb-2">
                    <div className="h-5 w-40 bg-muted rounded animate-pulse"></div>
                    <div className="h-3 w-32 bg-muted rounded animate-pulse"></div>
                  </CardHeader>
                  <CardContent>
                    <div className="h-3 w-full bg-muted rounded animate-pulse"></div>
                    <div className="h-3 w-2/3 bg-muted rounded animate-pulse mt-2"></div>
                  </CardContent>
                  <CardFooter className="flex justify-between">
                    <div className="h-3 w-20 bg-muted rounded animate-pulse"></div>
                    <div className="h-8 w-20 bg-muted rounded animate-pulse"></div>
                  </CardFooter>
                </Card>
              ))}
            </div>
          ) : filteredWorkflows.length === 0 ? (
            <div className="text-center py-12">
              <p className="text-muted-foreground">
                {searchQuery ? '没有找到匹配的工作流' : '暂无工作流，点击"新建工作流"按钮创建'}
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {filteredWorkflows.map((workflow) => (
                <Card key={workflow.id} className="overflow-hidden">
                  <CardHeader className="pb-2">
                    <div className="flex justify-between items-start">
                      <CardTitle className="text-xl">{workflow.name}</CardTitle>
                      {getStatusBadge(workflow.status)}
                    </div>
                    <CardDescription>
                      ID: {workflow.id}
                    </CardDescription>
                  </CardHeader>
                  <CardContent>
                    <p className="text-sm text-muted-foreground line-clamp-2">
                      {workflow.description || '无描述'}
                    </p>
                    <div className="flex flex-wrap mt-3 gap-2 text-xs text-muted-foreground">
                      <div>
                        节点: {workflow.nodes.length || 0}
                      </div>
                      <div>
                        连接: {workflow.edges.length || 0}
                      </div>
                      <div>
                        更新时间: {formatDistanceToNow(new Date(workflow.updatedAt), { addSuffix: true, locale: zhCN })}
                      </div>
                    </div>
                  </CardContent>
                  <CardFooter className="flex justify-between border-t pt-4">
                    <Button 
                      variant="ghost" 
                      size="sm" 
                      onClick={() => handleRunWorkflow(workflow.id)}
                      disabled={workflow.status === 'DISABLED' || workflow.status === 'DRAFT'}
                    >
                      <Play className="h-4 w-4 mr-1" />
                      执行
                    </Button>
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="sm">
                          操作
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem onClick={() => router.push(`/workflows/editor/${workflow.id}`)}>
                          <Edit className="h-4 w-4 mr-2" />
                          编辑
                        </DropdownMenuItem>
                        <DropdownMenuItem 
                          className="text-red-600"
                          onClick={() => handleDeleteWorkflow(workflow.id)}
                        >
                          <Trash className="h-4 w-4 mr-2" />
                          删除
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </CardFooter>
                </Card>
              ))}
            </div>
          )}
        </main>
      </div>

      {/* Confirm Delete Dialog */}
      <Dialog open={isConfirmDeleteOpen} onOpenChange={setIsConfirmDeleteOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>确认删除</DialogTitle>
            <DialogDescription>
              确定要删除此工作流吗？此操作无法撤消。
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setIsConfirmDeleteOpen(false)}>
              取消
            </Button>
            <Button variant="destructive" onClick={confirmDeleteWorkflow}>
              删除
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
} 