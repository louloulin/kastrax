import React, { useCallback, useEffect, useState, useRef } from 'react';
import { useRouter } from 'next/router';
import ReactFlow, {
  Node,
  Edge,
  addEdge,
  Background,
  Controls,
  MarkerType,
  Connection,
  EdgeTypes,
  Panel,
  NodeTypes,
  useNodesState,
  useEdgesState,
  ReactFlowProvider,
} from 'reactflow';
import 'reactflow/dist/style.css';

import { SupabaseSidebar } from '@/components/supabase-sidebar';
import { TopNavigation } from '@/components/top-navigation';
import { useToast } from '@/components/ui/use-toast';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Textarea } from '@/components/ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import { Save, Loader2, Play, ArrowLeft, Database, FileText, Webhook, 
  Calendar, Globe, Server, Filter, MoveIcon, BarChart2, GitMerge, 
  Code2, Split, Mail, MessageSquare } from 'lucide-react';
import {
  getWorkflow,
  updateWorkflow,
  NodeType,
  SourceType,
  TransformType,
  SinkType,
  Workflow,
} from '@/lib/api/workflows';

// Import our node components
import SourceNode from '@/components/workflows/nodes/source-node';
import TransformNode from '@/components/workflows/nodes/transform-node';
import SinkNode from '@/components/workflows/nodes/sink-node';

// Define custom node types
const nodeTypes: NodeTypes = {
  source: SourceNode,
  transform: TransformNode,
  sink: SinkNode,
};

export default function WorkflowEditorPage() {
  const router = useRouter();
  const { id } = router.query;
  const { toast } = useToast();
  const reactFlowWrapper = useRef<HTMLDivElement>(null);
  const [reactFlowInstance, setReactFlowInstance] = useState<any>(null);
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [activeTab, setActiveTab] = useState('info');

  // Workflow data
  const [workflow, setWorkflow] = useState<Workflow | null>(null);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [status, setStatus] = useState<'DRAFT' | 'ACTIVE' | 'DISABLED'>('DRAFT');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  // ReactFlow states
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);

  // Load workflow data
  useEffect(() => {
    if (id) {
      loadWorkflow(id as string);
    }
  }, [id]);

  const loadWorkflow = async (workflowId: string) => {
    setLoading(true);
    try {
      const workflowData = await getWorkflow(workflowId);
      setWorkflow(workflowData);
      setName(workflowData.name);
      setDescription(workflowData.description || '');
      setStatus(workflowData.status);
      
      // Initialize ReactFlow nodes and edges
      setNodes(workflowData.nodes || []);
      setEdges(workflowData.edges || []);
    } catch (error) {
      console.error('Error loading workflow:', error);
      toast({
        title: '加载失败',
        description: '无法加载工作流数据，请稍后重试',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  // Save workflow
  const handleSave = async () => {
    if (!workflow) return;
    
    setSaving(true);
    try {
      await updateWorkflow(workflow.id, {
        name,
        description,
        status,
        nodes,
        edges,
      });
      
      toast({
        title: '保存成功',
        description: '工作流已更新',
      });
    } catch (error) {
      console.error('Error saving workflow:', error);
      toast({
        title: '保存失败',
        description: '工作流保存失败，请稍后重试',
        variant: 'destructive',
      });
    } finally {
      setSaving(false);
    }
  };

  // Handle edge connections
  const onConnect = useCallback((params: Connection) => {
    setEdges((eds) => addEdge({
      ...params,
      animated: true,
      style: { stroke: '#999' },
      markerEnd: {
        type: MarkerType.ArrowClosed,
        width: 15,
        height: 15,
        color: '#999',
      },
    }, eds));
  }, [setEdges]);

  // Handle dropping new nodes
  const onDragOver = useCallback((event: React.DragEvent) => {
    event.preventDefault();
    event.dataTransfer.dropEffect = 'move';
  }, []);

  const onDrop = useCallback(
    (event: React.DragEvent) => {
      event.preventDefault();

      const type = event.dataTransfer.getData('application/reactflow/type');
      const subType = event.dataTransfer.getData('application/reactflow/subtype');
      const nodeLabel = event.dataTransfer.getData('application/reactflow/label');

      // check if the dropped element is valid
      if (typeof type === 'undefined' || !type) {
        return;
      }

      if (!reactFlowWrapper.current || !reactFlowInstance) {
        return;
      }

      const reactFlowBounds = reactFlowWrapper.current.getBoundingClientRect();
      const position = reactFlowInstance.project({
        x: event.clientX - reactFlowBounds.left,
        y: event.clientY - reactFlowBounds.top,
      });

      // Create node data based on type
      let data: any = { label: nodeLabel || 'New Node' };
      
      switch (type) {
        case 'source':
          data = {
            ...data, 
            type: NodeType.SOURCE, 
            subType: subType as SourceType,
            config: {}
          };
          break;
        case 'transform':
          data = {
            ...data, 
            type: NodeType.TRANSFORM, 
            subType: subType as TransformType,
            config: {}
          };
          break;
        case 'sink':
          data = {
            ...data, 
            type: NodeType.SINK, 
            subType: subType as SinkType,
            config: {}
          };
          break;
      }

      const newNode: Node = {
        id: `node-${Date.now()}`,
        type,
        position,
        data,
      };

      setNodes((nds) => nds.concat(newNode));
    },
    [reactFlowInstance, setNodes]
  );

  // Create a new node from the sidebar
  const onNodeDragStart = (event: React.DragEvent, nodeType: string, nodeSubType: string, nodeLabel: string) => {
    event.dataTransfer.setData('application/reactflow/type', nodeType);
    event.dataTransfer.setData('application/reactflow/subtype', nodeSubType);
    event.dataTransfer.setData('application/reactflow/label', nodeLabel);
    event.dataTransfer.effectAllowed = 'move';
  };

  const toggleFullscreen = () => {
    const element = document.documentElement;
    
    if (!isFullscreen) {
      if (element.requestFullscreen) {
        element.requestFullscreen();
      }
    } else {
      if (document.exitFullscreen) {
        document.exitFullscreen();
      }
    }
  };
  
  useEffect(() => {
    const handleFullscreenChange = () => {
      setIsFullscreen(!!document.fullscreenElement);
    };
    
    document.addEventListener('fullscreenchange', handleFullscreenChange);
    
    return () => {
      document.removeEventListener('fullscreenchange', handleFullscreenChange);
    };
  }, []);

  if (loading) {
    return (
      <div className="flex h-screen">
        <SupabaseSidebar
          collapsed={sidebarCollapsed}
          setCollapsed={setSidebarCollapsed}
        />
        <div className="flex-1 flex flex-col">
          <TopNavigation />
          <div className="flex-1 flex items-center justify-center">
            <Loader2 className="w-8 h-8 animate-spin text-primary" />
            <span className="ml-2">正在加载工作流...</span>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="flex h-screen">
      {!isFullscreen && (
        <SupabaseSidebar
          collapsed={sidebarCollapsed}
          setCollapsed={setSidebarCollapsed}
        />
      )}
      <div className="flex-1 flex flex-col overflow-hidden">
        {!isFullscreen && <TopNavigation />}
        <div className="flex items-center border-b px-4 py-2">
          <Button 
            variant="ghost" 
            size="sm" 
            onClick={() => router.push('/workflows')}
            className="mr-4"
          >
            <ArrowLeft className="h-4 w-4 mr-2" />
            返回列表
          </Button>
          <div className="flex-1">
            <h1 className="text-xl font-semibold">{name || '工作流编辑器'}</h1>
            <p className="text-sm text-muted-foreground">ID: {id}</p>
          </div>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={toggleFullscreen}
            >
              {isFullscreen ? '退出全屏' : '全屏编辑'}
            </Button>
            <Button
              onClick={handleSave}
              disabled={saving}
              size="sm"
            >
              {saving ? <Loader2 className="h-4 w-4 mr-2 animate-spin" /> : <Save className="h-4 w-4 mr-2" />}
              保存
            </Button>
          </div>
        </div>
        
        <div className="flex-1 flex overflow-hidden">
          {/* Left Panel */}
          <div className="w-[300px] border-r flex flex-col overflow-hidden">
            <Tabs defaultValue="info" value={activeTab} onValueChange={setActiveTab}>
              <TabsList className="w-full">
                <TabsTrigger value="info" className="flex-1">基本信息</TabsTrigger>
                <TabsTrigger value="nodes" className="flex-1">节点库</TabsTrigger>
              </TabsList>
              
              <TabsContent value="info" className="p-4 flex-1 overflow-auto">
                <div className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="name">工作流名称</Label>
                    <Input
                      id="name"
                      value={name}
                      onChange={(e) => setName(e.target.value)}
                      placeholder="输入工作流名称"
                    />
                  </div>
                  
                  <div className="space-y-2">
                    <Label htmlFor="description">描述</Label>
                    <Textarea
                      id="description"
                      value={description}
                      onChange={(e) => setDescription(e.target.value)}
                      placeholder="输入工作流描述"
                      rows={3}
                    />
                  </div>
                  
                  <div className="space-y-2">
                    <Label htmlFor="status">状态</Label>
                    <Select 
                      value={status} 
                      onValueChange={(value) => setStatus(value as 'DRAFT' | 'ACTIVE' | 'DISABLED')}
                    >
                      <SelectTrigger id="status">
                        <SelectValue placeholder="选择状态" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="DRAFT">草稿</SelectItem>
                        <SelectItem value="ACTIVE">活跃</SelectItem>
                        <SelectItem value="DISABLED">禁用</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  
                  <div className="space-y-2">
                    <Label>工作流统计</Label>
                    <div className="text-sm grid grid-cols-2 gap-2">
                      <div className="border rounded p-2">
                        <div className="text-muted-foreground">节点数量</div>
                        <div className="text-xl font-medium">{nodes?.length || 0}</div>
                      </div>
                      <div className="border rounded p-2">
                        <div className="text-muted-foreground">连接数量</div>
                        <div className="text-xl font-medium">{edges?.length || 0}</div>
                      </div>
                    </div>
                  </div>
                </div>
              </TabsContent>
              
              <TabsContent value="nodes" className="flex-1 flex flex-col overflow-hidden">
                <div className="p-4 border-b">
                  <p className="text-sm text-muted-foreground">拖拽节点到编辑区来创建工作流</p>
                </div>
                
                <div className="flex-1 overflow-auto p-4">
                  <div className="space-y-4">
                    <div>
                      <h3 className="text-sm font-medium mb-2">数据源</h3>
                      <div className="grid grid-cols-2 gap-2">
                        <div
                          className="flex flex-col items-center border rounded p-2 cursor-move hover:bg-accent"
                          draggable
                          onDragStart={(e) => onNodeDragStart(e, 'source', SourceType.DATABASE, '数据库源')}
                        >
                          <Database className="h-10 w-10 mb-1 text-blue-500" />
                          <span className="text-xs">数据库</span>
                        </div>
                        
                        <div
                          className="flex flex-col items-center border rounded p-2 cursor-move hover:bg-accent"
                          draggable
                          onDragStart={(e) => onNodeDragStart(e, 'source', SourceType.FILE, '文件源')}
                        >
                          <FileText className="h-10 w-10 mb-1 text-orange-500" />
                          <span className="text-xs">文件</span>
                        </div>
                        
                        <div
                          className="flex flex-col items-center border rounded p-2 cursor-move hover:bg-accent"
                          draggable
                          onDragStart={(e) => onNodeDragStart(e, 'source', SourceType.API, 'API源')}
                        >
                          <Globe className="h-10 w-10 mb-1 text-violet-500" />
                          <span className="text-xs">API</span>
                        </div>
                        
                        <div
                          className="flex flex-col items-center border rounded p-2 cursor-move hover:bg-accent"
                          draggable
                          onDragStart={(e) => onNodeDragStart(e, 'source', SourceType.SCHEDULE, '定时任务')}
                        >
                          <Calendar className="h-10 w-10 mb-1 text-green-500" />
                          <span className="text-xs">定时任务</span>
                        </div>
                        
                        <div
                          className="flex flex-col items-center border rounded p-2 cursor-move hover:bg-accent"
                          draggable
                          onDragStart={(e) => onNodeDragStart(e, 'source', SourceType.WEBHOOK, 'Webhook源')}
                        >
                          <Webhook className="h-10 w-10 mb-1 text-rose-500" />
                          <span className="text-xs">Webhook</span>
                        </div>
                        
                        <div
                          className="flex flex-col items-center border rounded p-2 cursor-move hover:bg-accent"
                          draggable
                          onDragStart={(e) => onNodeDragStart(e, 'source', SourceType.RESOURCE, '资源')}
                        >
                          <Server className="h-10 w-10 mb-1 text-slate-500" />
                          <span className="text-xs">资源</span>
                        </div>
                      </div>
                    </div>
                    
                    <div>
                      <h3 className="text-sm font-medium mb-2">转换</h3>
                      <div className="grid grid-cols-2 gap-2">
                        <div
                          className="flex flex-col items-center border rounded p-2 cursor-move hover:bg-accent"
                          draggable
                          onDragStart={(e) => onNodeDragStart(e, 'transform', TransformType.FILTER, '过滤')}
                        >
                          <Filter className="h-10 w-10 mb-1 text-yellow-500" />
                          <span className="text-xs">过滤</span>
                        </div>
                        
                        <div
                          className="flex flex-col items-center border rounded p-2 cursor-move hover:bg-accent"
                          draggable
                          onDragStart={(e) => onNodeDragStart(e, 'transform', TransformType.MAP, '映射')}
                        >
                          <MoveIcon className="h-10 w-10 mb-1 text-indigo-500" />
                          <span className="text-xs">映射</span>
                        </div>
                        
                        <div
                          className="flex flex-col items-center border rounded p-2 cursor-move hover:bg-accent"
                          draggable
                          onDragStart={(e) => onNodeDragStart(e, 'transform', TransformType.AGGREGATE, '聚合')}
                        >
                          <BarChart2 className="h-10 w-10 mb-1 text-emerald-500" />
                          <span className="text-xs">聚合</span>
                        </div>
                        
                        <div
                          className="flex flex-col items-center border rounded p-2 cursor-move hover:bg-accent"
                          draggable
                          onDragStart={(e) => onNodeDragStart(e, 'transform', TransformType.JOIN, '连接')}
                        >
                          <GitMerge className="h-10 w-10 mb-1 text-purple-500" />
                          <span className="text-xs">连接</span>
                        </div>
                        
                        <div
                          className="flex flex-col items-center border rounded p-2 cursor-move hover:bg-accent"
                          draggable
                          onDragStart={(e) => onNodeDragStart(e, 'transform', TransformType.SCRIPT, '脚本')}
                        >
                          <Code2 className="h-10 w-10 mb-1 text-rose-500" />
                          <span className="text-xs">脚本</span>
                        </div>
                        
                        <div
                          className="flex flex-col items-center border rounded p-2 cursor-move hover:bg-accent"
                          draggable
                          onDragStart={(e) => onNodeDragStart(e, 'transform', TransformType.SPLIT, '拆分')}
                        >
                          <Split className="h-10 w-10 mb-1 text-cyan-500" />
                          <span className="text-xs">拆分</span>
                        </div>
                      </div>
                    </div>
                    
                    <div>
                      <h3 className="text-sm font-medium mb-2">输出</h3>
                      <div className="grid grid-cols-2 gap-2">
                        <div
                          className="flex flex-col items-center border rounded p-2 cursor-move hover:bg-accent"
                          draggable
                          onDragStart={(e) => onNodeDragStart(e, 'sink', SinkType.DATABASE, '数据库输出')}
                        >
                          <Database className="h-10 w-10 mb-1 text-blue-500" />
                          <span className="text-xs">数据库</span>
                        </div>
                        
                        <div
                          className="flex flex-col items-center border rounded p-2 cursor-move hover:bg-accent"
                          draggable
                          onDragStart={(e) => onNodeDragStart(e, 'sink', SinkType.FILE, '文件输出')}
                        >
                          <FileText className="h-10 w-10 mb-1 text-orange-500" />
                          <span className="text-xs">文件</span>
                        </div>
                        
                        <div
                          className="flex flex-col items-center border rounded p-2 cursor-move hover:bg-accent"
                          draggable
                          onDragStart={(e) => onNodeDragStart(e, 'sink', SinkType.API, 'API输出')}
                        >
                          <Globe className="h-10 w-10 mb-1 text-violet-500" />
                          <span className="text-xs">API</span>
                        </div>
                        
                        <div
                          className="flex flex-col items-center border rounded p-2 cursor-move hover:bg-accent"
                          draggable
                          onDragStart={(e) => onNodeDragStart(e, 'sink', SinkType.MESSAGE, '消息输出')}
                        >
                          <MessageSquare className="h-10 w-10 mb-1 text-teal-500" />
                          <span className="text-xs">消息</span>
                        </div>
                        
                        <div
                          className="flex flex-col items-center border rounded p-2 cursor-move hover:bg-accent"
                          draggable
                          onDragStart={(e) => onNodeDragStart(e, 'sink', SinkType.EMAIL, '邮件输出')}
                        >
                          <Mail className="h-10 w-10 mb-1 text-sky-500" />
                          <span className="text-xs">邮件</span>
                        </div>
                        
                        <div
                          className="flex flex-col items-center border rounded p-2 cursor-move hover:bg-accent"
                          draggable
                          onDragStart={(e) => onNodeDragStart(e, 'sink', SinkType.SERVICE, '服务输出')}
                        >
                          <Server className="h-10 w-10 mb-1 text-slate-500" />
                          <span className="text-xs">服务</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </TabsContent>
            </Tabs>
          </div>
          
          {/* Main Flow Editor */}
          <ReactFlowProvider>
            <div className="flex-1 h-full" ref={reactFlowWrapper}>
              <ReactFlow
                nodes={nodes}
                edges={edges}
                onNodesChange={onNodesChange}
                onEdgesChange={onEdgesChange}
                onConnect={onConnect}
                onInit={setReactFlowInstance}
                onDrop={onDrop}
                onDragOver={onDragOver}
                nodeTypes={nodeTypes}
                fitView
              >
                <Background />
                <Controls />
                <Panel position="top-right">
                  <Button 
                    onClick={handleSave} 
                    disabled={saving}
                    size="sm"
                    className="mr-2"
                  >
                    {saving ? <Loader2 className="h-4 w-4 mr-2 animate-spin" /> : <Save className="h-4 w-4 mr-2" />}
                    保存
                  </Button>
                </Panel>
              </ReactFlow>
            </div>
          </ReactFlowProvider>
        </div>
      </div>
    </div>
  );
} 