import React, { useCallback, useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import ReactFlow, {
  Background,
  Controls,
  MiniMap,
  Panel,
  useNodesState,
  useEdgesState,
  addEdge,
  Connection,
  Edge,
  Node,
  NodeChange,
  EdgeChange,
  applyNodeChanges,
  applyEdgeChanges,
  MarkerType,
} from 'reactflow';
import 'reactflow/dist/style.css';

import SupabaseSidebar from "@/components/supabase-sidebar";
import TopNavigation from "@/components/top-navigation";
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Card, CardContent } from '@/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { useToast } from '@/components/ui/use-toast';
import { Separator } from '@/components/ui/separator';
import {
  ArrowLeftIcon,
  PlayIcon,
  PlusIcon,
  ReloadIcon,
  Cross2Icon,
} from '@radix-ui/react-icons';
import { Menu, Maximize, Minimize, Database, FileText, Globe, Calendar, Webhook, Server,
  Filter, MoveIcon, BarChart2, GitMerge, Code2, Split, Mail, MessageSquare } from 'lucide-react';

import {
  getRuleById,
  createRule,
  updateRule,
  Rule,
  RuleType,
  RuleStatus,
  NodeType,
  EdgeType,
  RuleNode,
  RuleEdge,
} from '@/lib/api/rules';

import { 
  ComponentType,
  SourceType,
  TransformType,
  SinkType 
} from '@/lib/api/workflows';

// 自定义节点类型
import StartNode from '@/components/rules/nodes/start-node';
import EndNode from '@/components/rules/nodes/end-node';
import ConditionNode from '@/components/rules/nodes/condition-node';
import ActionNode from '@/components/rules/nodes/action-node';
import TransformNode from '@/components/rules/nodes/transform-node';
import ScriptNode from '@/components/rules/nodes/script-node';
import ApiNode from '@/components/rules/nodes/api-node';
import DelayNode from '@/components/rules/nodes/delay-node';
import ResourceNode from '@/components/rules/nodes/resource-node';

// 工作流节点类型
import SourceNode from '@/components/workflows/nodes/source-node';
import WorkflowTransformNode from '@/components/workflows/nodes/transform-node';
import SinkNode from '@/components/workflows/nodes/sink-node';
import KafkaNode from '@/components/workflow/nodes/kafka-node';

// 边类型样式
const edgeTypes = {};

// 节点类型映射
const nodeTypes = {
  [NodeType.START]: StartNode,
  [NodeType.END]: EndNode,
  [NodeType.CONDITION]: ConditionNode,
  [NodeType.ACTION]: ActionNode,
  [NodeType.TRANSFORM]: TransformNode,
  [NodeType.SCRIPT]: ScriptNode,
  [NodeType.API]: ApiNode,
  [NodeType.DELAY]: DelayNode,
  [NodeType.RESOURCE]: ResourceNode,
  // 工作流节点类型
  'source': SourceNode,
  'workflow-transform': WorkflowTransformNode,
  'sink': SinkNode,
  // Kafka 节点类型
  'KAFKA_SUBSCRIBE': KafkaNode,
  'KAFKA_PUBLISH': KafkaNode,
};

const RuleEditor: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { toast } = useToast();
  const [isNew] = useState(id === 'new');
  const [loading, setLoading] = useState(!isNew);
  const [saving, setSaving] = useState(false);
  const [ruleName, setRuleName] = useState('');
  const [ruleType, setRuleType] = useState<RuleType>(RuleType.CONDITIONAL);
  const [description, setDescription] = useState('');
  const [status, setStatus] = useState<RuleStatus>(RuleStatus.DRAFT);
  const [originalRule, setOriginalRule] = useState<Rule | null>(null);
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [selectedNode, setSelectedNode] = useState<string | null>(null);
  const [configTab, setConfigTab] = useState('info');

  // ReactFlow 状态
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);

  // 加载规则数据
  useEffect(() => {
    if (!isNew) {
      fetchRuleData();
    } else {
      // 新建规则，初始化默认节点
      initializeDefaultNodes();
    }
  }, [id]);

  // 初始化默认节点
  const initializeDefaultNodes = () => {
    const startNode: Node = {
      id: 'node-start',
      type: NodeType.START,
      position: { x: 250, y: 50 },
      data: { label: '开始' },
    };
    
    const endNode: Node = {
      id: 'node-end',
      type: NodeType.END,
      position: { x: 250, y: 350 },
      data: { label: '结束' },
    };
    
    setNodes([startNode, endNode]);
    setLoading(false);
  };

  // 获取规则数据
  const fetchRuleData = async () => {
    setLoading(true);
    try {
      const response = await getRuleById(id as string);
      const rule = response.data.data;
      setOriginalRule(rule);
      
      // 设置基本信息
      setRuleName(rule.ruleName);
      setRuleType(rule.type);
      setDescription(rule.description || '');
      setStatus(rule.status);
      
      // 转换节点和边到ReactFlow格式
      if (rule.graph) {
        // 添加onConfigure回调到节点数据
        const nodesWithConfig = rule.graph.nodes.map(node => ({
          ...node,
          data: {
            ...node.data,
            onConfigure: () => handleNodeClick(node.id)
          }
        }));
        setNodes(nodesWithConfig);
        setEdges(rule.graph.edges);
      }
    } catch (error) {
      console.error('Failed to fetch rule:', error);
      toast({
        title: '获取规则失败',
        description: '无法加载规则数据，请稍后重试',
        variant: 'destructive',
      });
      navigate('/rules');
    } finally {
      setLoading(false);
    }
  };

  // 处理连接创建
  const onConnect = useCallback((connection: Connection) => {
    if (!connection.source || !connection.target) return;
    
    const newEdge: Edge = {
      ...connection,
      id: `edge-${connection.source}-${connection.target}`,
      type: EdgeType.DEFAULT as any,
      animated: false,
      markerEnd: {
        type: MarkerType.ArrowClosed,
      },
    };
    setEdges((eds) => addEdge(newEdge, eds));
  }, [setEdges]);

  // 处理节点点击
  const handleNodeClick = (nodeId: string | undefined) => {
    if (!nodeId) {
      console.warn('节点ID未定义');
      return;
    }
    setSelectedNode(nodeId);
    setConfigTab('config');
  };

  // 添加新节点
  const addNode = (type: string, subType?: string, label?: string) => {
    const newNodeId = `node-${Date.now()}`;
    let nodeData: any = { 
      label: label || getNodeTypeLabel(type as NodeType),
      config: getDefaultConfigForType(type as NodeType),
      onConfigure: () => handleNodeClick(newNodeId)
    };
    
    // 如果是工作流节点类型，添加子类型
    if (subType) {
      nodeData.subType = subType;
    }
    
    // 为Kafka节点添加特定属性
    if (type === 'KAFKA_SUBSCRIBE' || type === 'KAFKA_PUBLISH') {
      nodeData.nodeType = type;
      nodeData.properties = {
        topic: '',
        brokers: '',
        ...(type === 'KAFKA_SUBSCRIBE' ? { groupId: '' } : {})
      };
    }
    
    const newNode: Node = {
      id: newNodeId,
      type,
      position: { 
        x: Math.random() * 300 + 100, 
        y: Math.random() * 300 + 100 
      },
      data: nodeData,
    };
    
    setNodes((nds) => [...nds, newNode]);
    
    // 选中新创建的节点进行配置
    handleNodeClick(newNodeId);
  };

  // 获取节点类型默认标签
  const getNodeTypeLabel = (type: NodeType): string => {
    const typeLabels = {
      [NodeType.START]: '开始节点',
      [NodeType.END]: '结束节点',
      [NodeType.CONDITION]: '条件判断',
      [NodeType.ACTION]: '执行动作',
      [NodeType.TRANSFORM]: '数据转换',
      [NodeType.SCRIPT]: '执行脚本',
      [NodeType.API]: '调用API',
      [NodeType.DELAY]: '延迟执行',
      [NodeType.RESOURCE]: '连接资源',
    };
    
    return typeLabels[type] || '新节点';
  };

  // 获取节点类型默认配置
  const getDefaultConfigForType = (type: NodeType): any => {
    const typeConfigs = {
      [NodeType.CONDITION]: { condition: 'value > 0' },
      [NodeType.ACTION]: { action: 'logMessage', params: { message: '执行动作' } },
      [NodeType.TRANSFORM]: { transform: 'mapFields', mapping: {} },
      [NodeType.SCRIPT]: { scriptId: '', params: {} },
      [NodeType.API]: { url: 'https://api.example.com', method: 'GET', headers: {}, body: {} },
      [NodeType.DELAY]: { duration: 1000 },
      [NodeType.RESOURCE]: { resourceId: '', queryType: 'sql', query: '' },
    };
    
    return typeConfigs[type] || {};
  };

  // 删除选中的节点
  const deleteSelection = useCallback(() => {
    setNodes((nds) => nds.filter((node) => !node.selected));
    setEdges((eds) => eds.filter((edge) => !edge.selected));
    
    // 如果当前选中的节点被删除，清除选中状态
    if (selectedNode && !nodes.find(node => node.id === selectedNode)) {
      setSelectedNode(null);
    }
  }, [setNodes, setEdges, selectedNode, nodes]);

  // 保存规则
  const saveRule = async () => {
    if (!ruleName) {
      toast({
        title: '规则名称不能为空',
        description: '请输入规则名称',
        variant: 'destructive',
      });
      return;
    }
    
    // 验证流程图
    if (nodes.length < 2) {
      toast({
        title: '流程图不完整',
        description: '规则需要至少包含开始和结束节点',
        variant: 'destructive',
      });
      return;
    }
    
    setSaving(true);
    
    try {
      // 准备规则数据
      const ruleGraph = {
        nodes: nodes.map(node => ({
          id: node.id,
          type: node.type as NodeType,
          position: node.position,
          data: {
            ...node.data,
            // 移除不需要保存的回调函数
            onConfigure: undefined
          },
        })),
        edges: edges.map(edge => ({
          id: edge.id,
          source: edge.source,
          target: edge.target,
          type: edge.type as EdgeType || EdgeType.DEFAULT,
          animated: edge.animated,
          label: edge.label,
        })),
      };
      
      if (isNew) {
        // 创建新规则
        const response = await createRule({
          ruleName,
          type: ruleType,
          description,
          status,
          graph: ruleGraph,
        });
        
        toast({
          title: '创建成功',
          description: `规则 "${ruleName}" 已创建`,
        });
        
        // 导航到编辑页面
        navigate(`/rules/editor/${response.data.data.ruleId}`);
      } else {
        // 更新规则
        await updateRule({
          ruleId: id as string,
          ruleName,
          type: ruleType,
          description,
          status,
          graph: ruleGraph,
        });
        
        toast({
          title: '保存成功',
          description: `规则 "${ruleName}" 已更新`,
        });
        
        // 重新加载规则
        fetchRuleData();
      }
    } catch (error) {
      console.error('Failed to save rule:', error);
      toast({
        title: '保存失败',
        description: '请稍后重试',
        variant: 'destructive',
      });
    } finally {
      setSaving(false);
    }
  };

  // 返回列表页
  const goBack = () => {
    navigate('/rules');
  };

  // Toggle fullscreen mode
  const toggleFullscreen = () => {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen().catch(err => {
        console.error(`Error attempting to enable fullscreen: ${err.message}`);
      });
      setIsFullscreen(true);
    } else {
      if (document.exitFullscreen) {
        document.exitFullscreen();
        setIsFullscreen(false);
      }
    }
  };

  // Listen for fullscreen change events
  useEffect(() => {
    const handleFullscreenChange = () => {
      setIsFullscreen(!!document.fullscreenElement);
    };

    document.addEventListener('fullscreenchange', handleFullscreenChange);
    return () => {
      document.removeEventListener('fullscreenchange', handleFullscreenChange);
    };
  }, []);

  // 获取当前选中的节点
  const getSelectedNodeData = () => {
    if (!selectedNode) return null;
    return nodes.find(node => node.id === selectedNode);
  };

  // 更新节点配置
  const updateNodeConfig = (config: any) => {
    if (!selectedNode) return;
    
    setNodes(nds => nds.map(node => {
      if (node.id === selectedNode) {
        return {
          ...node,
          data: {
            ...node.data,
            config: {
              ...node.data.config,
              ...config
            }
          }
        };
      }
      return node;
    }));
  };

  if (loading) {
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
          <div className="flex-1 flex items-center justify-center">
            <div className="flex flex-col items-center">
              <ReloadIcon className="h-8 w-8 animate-spin mb-4" />
              <p>加载规则数据...</p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen bg-background">
      {!isFullscreen && <SupabaseSidebar collapsed={sidebarCollapsed} setCollapsed={setSidebarCollapsed} />}
      
      <div className="flex-1 flex flex-col">
        {!isFullscreen && (
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
        )}
        
        <div className="flex flex-col flex-1">
          <div className="flex items-center justify-between p-4 border-b">
            <div className="flex items-center">
              <Button variant="ghost" size="sm" onClick={goBack}>
                <ArrowLeftIcon className="h-4 w-4 mr-2" />
                返回
              </Button>
              <Separator orientation="vertical" className="h-6 mx-2" />
              <h1 className="text-xl font-medium">
                {isNew ? '创建规则' : `编辑规则: ${originalRule?.ruleName}`}
              </h1>
            </div>
            <div className="flex gap-2">
              <Button 
                variant="outline" 
                size="sm" 
                onClick={toggleFullscreen}
                title={isFullscreen ? "退出全屏" : "全屏模式"}
              >
                {isFullscreen ? <Minimize size={16} /> : <Maximize size={16} />}
              </Button>
              <Button variant="outline" size="sm" disabled={saving} onClick={goBack}>
                取消
              </Button>
              <Button size="sm" disabled={saving} onClick={saveRule}>
                {saving && <ReloadIcon className="h-4 w-4 mr-2 animate-spin" />}
                {saving ? '保存中...' : '保存规则'}
              </Button>
            </div>
          </div>
          
          <div className="flex flex-1 h-full overflow-hidden">
            {/* 左侧面板 */}
            <div className="w-64 border-r p-4 flex flex-col">
              <Tabs value={configTab} onValueChange={setConfigTab}>
                <TabsList className="w-full mb-4">
                  <TabsTrigger value="info" className="flex-1">基本信息</TabsTrigger>
                  <TabsTrigger value="nodes" className="flex-1">节点库</TabsTrigger>
                  {selectedNode && <TabsTrigger value="config" className="flex-1">配置</TabsTrigger>}
                </TabsList>
                
                <TabsContent value="info" className="space-y-4">
                  <div>
                    <Label htmlFor="ruleName">规则名称</Label>
                    <Input
                      id="ruleName"
                      value={ruleName}
                      onChange={(e) => setRuleName(e.target.value)}
                      placeholder="输入规则名称"
                    />
                  </div>
                  
                  <div>
                    <Label htmlFor="ruleType">规则类型</Label>
                    <Select
                      value={ruleType}
                      onValueChange={(value: RuleType) => setRuleType(value)}
                    >
                      <SelectTrigger id="ruleType">
                        <SelectValue placeholder="选择规则类型" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value={RuleType.CONDITIONAL}>条件规则</SelectItem>
                        <SelectItem value={RuleType.SEQUENCE}>顺序规则</SelectItem>
                        <SelectItem value={RuleType.PARALLEL}>并行规则</SelectItem>
                        <SelectItem value={RuleType.TRANSFORM}>转换规则</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  
                  <div>
                    <Label htmlFor="status">规则状态</Label>
                    <Select
                      value={status}
                      onValueChange={(value: RuleStatus) => setStatus(value)}
                    >
                      <SelectTrigger id="status">
                        <SelectValue placeholder="选择规则状态" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value={RuleStatus.DRAFT}>草稿</SelectItem>
                        <SelectItem value={RuleStatus.ACTIVE}>启用</SelectItem>
                        <SelectItem value={RuleStatus.INACTIVE}>禁用</SelectItem>
                        <SelectItem value={RuleStatus.DEPRECATED}>已弃用</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  
                  <div>
                    <Label htmlFor="description">描述</Label>
                    <Textarea
                      id="description"
                      value={description}
                      onChange={(e) => setDescription(e.target.value)}
                      placeholder="输入规则描述"
                      rows={4}
                    />
                  </div>
                </TabsContent>
                
                <TabsContent value="nodes" className="space-y-3">
                  <p className="text-sm text-muted-foreground mb-2">点击节点添加到画布</p>
                  
                  <div className="space-y-2">
                    <Label className="text-xs font-medium">流程控制</Label>
                    <div className="grid grid-cols-2 gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        className="justify-start text-left"
                        onClick={() => addNode(NodeType.CONDITION)}
                      >
                        <div className="w-3 h-3 rounded bg-yellow-500 mr-2"></div>
                        条件判断
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        className="justify-start text-left"
                        onClick={() => addNode(NodeType.ACTION)}
                      >
                        <div className="w-3 h-3 rounded bg-blue-500 mr-2"></div>
                        执行动作
                      </Button>
                    </div>
                  </div>
                  
                  <div className="space-y-2">
                    <Label className="text-xs font-medium">数据处理</Label>
                    <div className="grid grid-cols-2 gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        className="justify-start text-left"
                        onClick={() => addNode(NodeType.TRANSFORM)}
                      >
                        <div className="w-3 h-3 rounded bg-purple-500 mr-2"></div>
                        数据转换
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        className="justify-start text-left"
                        onClick={() => addNode(NodeType.SCRIPT)}
                      >
                        <div className="w-3 h-3 rounded bg-emerald-500 mr-2"></div>
                        执行脚本
                      </Button>
                    </div>
                  </div>
                  
                  <div className="space-y-2">
                    <Label className="text-xs font-medium">系统交互</Label>
                    <div className="grid grid-cols-2 gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        className="justify-start text-left"
                        onClick={() => addNode(NodeType.API)}
                      >
                        <div className="w-3 h-3 rounded bg-indigo-500 mr-2"></div>
                        调用API
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        className="justify-start text-left"
                        onClick={() => addNode(NodeType.DELAY)}
                      >
                        <div className="w-3 h-3 rounded bg-orange-500 mr-2"></div>
                        延迟执行
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        className="justify-start text-left"
                        onClick={() => addNode(NodeType.RESOURCE)}
                      >
                        <div className="w-3 h-3 rounded bg-cyan-500 mr-2"></div>
                        连接资源
                      </Button>
                    </div>
                  </div>
                  
                  <Separator className="my-3" />
                  
                  <div className="space-y-2">
                    <Label className="text-xs font-medium">数据源</Label>
                    <div className="grid grid-cols-2 gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        className="justify-start text-left"
                        onClick={() => addNode('source', SourceType.DATABASE, '数据库源')}
                      >
                        <Database className="h-4 w-4 text-blue-500 mr-2" />
                        数据库
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        className="justify-start text-left"
                        onClick={() => addNode('source', SourceType.FILE, '文件源')}
                      >
                        <FileText className="h-4 w-4 text-amber-500 mr-2" />
                        文件
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        className="justify-start text-left"
                        onClick={() => addNode('source', SourceType.API, 'API源')}
                      >
                        <Globe className="h-4 w-4 text-green-500 mr-2" />
                        API
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        className="justify-start text-left"
                        onClick={() => addNode('source', SourceType.SCHEDULE, '定时任务')}
                      >
                        <Calendar className="h-4 w-4 text-orange-500 mr-2" />
                        定时任务
                      </Button>
                    </div>
                  </div>
                  
                  <div className="space-y-2">
                    <Label className="text-xs font-medium">消息队列</Label>
                    <div className="grid grid-cols-2 gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        className="justify-start text-left"
                        onClick={() => addNode('KAFKA_SUBSCRIBE', undefined, 'Kafka订阅')}
                      >
                        <div className="h-4 w-4 text-green-500 mr-2 flex items-center justify-center text-xs font-bold">KF</div>
                        Kafka订阅
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        className="justify-start text-left"
                        onClick={() => addNode('KAFKA_PUBLISH', undefined, 'Kafka发布')}
                      >
                        <div className="h-4 w-4 text-blue-500 mr-2 flex items-center justify-center text-xs font-bold">KF</div>
                        Kafka发布
                      </Button>
                    </div>
                  </div>
                  
                  <div className="space-y-2">
                    <Label className="text-xs font-medium">数据转换</Label>
                    <div className="grid grid-cols-2 gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        className="justify-start text-left"
                        onClick={() => addNode('transform', TransformType.FILTER, '过滤')}
                      >
                        <Filter className="h-4 w-4 text-yellow-500 mr-2" />
                        过滤
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        className="justify-start text-left"
                        onClick={() => addNode('transform', TransformType.MAP, '映射')}
                      >
                        <MoveIcon className="h-4 w-4 text-indigo-500 mr-2" />
                        映射
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        className="justify-start text-left"
                        onClick={() => addNode('transform', TransformType.SCRIPT, '脚本')}
                      >
                        <Code2 className="h-4 w-4 text-rose-500 mr-2" />
                        脚本
                      </Button>
                    </div>
                  </div>
                  
                  <div className="space-y-2">
                    <Label className="text-xs font-medium">数据输出</Label>
                    <div className="grid grid-cols-2 gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        className="justify-start text-left"
                        onClick={() => addNode('sink', SinkType.DATABASE, '数据库输出')}
                      >
                        <Database className="h-4 w-4 text-blue-500 mr-2" />
                        数据库
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        className="justify-start text-left"
                        onClick={() => addNode('sink', SinkType.FILE, '文件输出')}
                      >
                        <FileText className="h-4 w-4 text-orange-500 mr-2" />
                        文件
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        className="justify-start text-left"
                        onClick={() => addNode('sink', SinkType.API, 'API输出')}
                      >
                        <Globe className="h-4 w-4 text-violet-500 mr-2" />
                        API
                      </Button>
                    </div>
                  </div>
                  
                  <div className="pt-4">
                    <Button
                      variant="destructive"
                      size="sm"
                      className="w-full"
                      onClick={deleteSelection}
                    >
                      <Cross2Icon className="h-4 w-4 mr-2" />
                      删除选中
                    </Button>
                  </div>
                </TabsContent>
                
                <TabsContent value="config" className="space-y-4">
                  {selectedNode && (
                    <NodeConfigPanel 
                      node={getSelectedNodeData()}
                      updateConfig={updateNodeConfig}
                    />
                  )}
                </TabsContent>
              </Tabs>
            </div>
            
            {/* ReactFlow编辑器区域 */}
            <ReactFlow
              nodes={nodes}
              edges={edges}
              onNodesChange={onNodesChange}
              onEdgesChange={onEdgesChange}
              onConnect={onConnect}
              onNodeClick={(_, node) => handleNodeClick(node.id)}
              onPaneClick={() => setSelectedNode(null)}
              fitView
              nodeTypes={nodeTypes}
              edgeTypes={edgeTypes}
              deleteKeyCode={['Backspace', 'Delete']}
            >
              <Background />
              <Controls />
              <MiniMap />
            </ReactFlow>
          </div>
        </div>
      </div>
    </div>
  );
};

// 节点配置面板
const NodeConfigPanel = ({ node, updateConfig }: { node: Node | null, updateConfig: (config: any) => void }) => {
  if (!node) return <div className="text-center text-muted-foreground">请选择一个节点进行配置</div>;
  
  const handleConfigChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    updateConfig({ [e.target.name]: e.target.value });
  };
  
  // 处理属性变更 - 用于Kafka节点
  const handlePropertyChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    if (node.data?.properties) {
      const updatedProperties = {
        ...node.data.properties,
        [e.target.name]: e.target.value
      };
      
      // 更新节点的properties属性
      if (node.data) {
        node.data.properties = updatedProperties;
        // 强制更新
        updateConfig({});
      }
    }
  };
  
  // 根据节点类型渲染不同的配置面板
  return (
    <div className="space-y-4">
      <div>
        <Label>节点ID</Label>
        <Input value={node.id} disabled className="bg-muted" />
      </div>
      
      <div>
        <Label>节点类型</Label>
        <Input value={node.type} disabled className="bg-muted" />
      </div>
      
      <div>
        <Label htmlFor="label">节点名称</Label>
        <Input
          id="label"
          name="label"
          value={node.data?.label || ''}
          onChange={(e) => {
            if (node.data) {
              node.data.label = e.target.value;
              // 强制更新
              updateConfig({});
            }
          }}
        />
      </div>
      
      {/* 根据节点类型显示不同的配置选项 */}
      {node.type === NodeType.CONDITION && (
        <div>
          <Label htmlFor="condition">条件表达式</Label>
          <Textarea
            id="condition"
            name="condition"
            value={node.data?.config?.condition || ''}
            onChange={handleConfigChange}
            placeholder="例如: value > 10 && status === 'active'"
            rows={3}
          />
        </div>
      )}
      
      {node.type === NodeType.DELAY && (
        <div>
          <Label htmlFor="duration">延迟时间 (毫秒)</Label>
          <Input
            id="duration"
            name="duration"
            type="number"
            value={node.data?.config?.duration || 1000}
            onChange={handleConfigChange}
          />
        </div>
      )}
      
      {/* Kafka节点配置 */}
      {(node.type === 'KAFKA_SUBSCRIBE' || node.type === 'KAFKA_PUBLISH') && (
        <div className="space-y-4">
          <div>
            <Label htmlFor="topic">Kafka主题</Label>
            <Input
              id="topic"
              name="topic"
              value={node.data?.properties?.topic || ''}
              onChange={handlePropertyChange}
              placeholder="输入Kafka主题名称"
            />
          </div>
          
          <div>
            <Label htmlFor="brokers">Kafka Brokers</Label>
            <Input
              id="brokers"
              name="brokers"
              value={node.data?.properties?.brokers || ''}
              onChange={handlePropertyChange}
              placeholder="例如: localhost:9092"
            />
          </div>
          
          {node.type === 'KAFKA_SUBSCRIBE' && (
            <div>
              <Label htmlFor="groupId">消费者组ID</Label>
              <Input
                id="groupId"
                name="groupId"
                value={node.data?.properties?.groupId || ''}
                onChange={handlePropertyChange}
                placeholder="输入消费者组ID"
              />
            </div>
          )}
        </div>
      )}
      
      {/* 工作流节点配置 */}
      {node.type === 'source' && node.data?.subType === SourceType.DATABASE && (
        <>
          <div>
            <Label htmlFor="resourceId">资源ID</Label>
            <Input
              id="resourceId"
              name="resourceId"
              value={node.data?.config?.resourceId || ''}
              onChange={handleConfigChange}
              placeholder="数据库资源ID"
            />
          </div>
          <div>
            <Label htmlFor="query">SQL查询</Label>
            <Textarea
              id="query"
              name="query"
              value={node.data?.config?.query || ''}
              onChange={handleConfigChange}
              placeholder="SELECT * FROM table WHERE condition"
              rows={3}
            />
          </div>
        </>
      )}
      
      {/* 添加其他节点类型的配置选项... */}
      
      <div className="pt-4">
        <Button 
          variant="default" 
          size="sm" 
          className="w-full"
          onClick={() => updateConfig({})}
        >
          应用配置
        </Button>
      </div>
    </div>
  );
};

export default RuleEditor; 