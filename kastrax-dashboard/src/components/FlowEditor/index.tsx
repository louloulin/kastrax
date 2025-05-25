import React, { useState, useRef, useCallback } from 'react';
import ReactFlow, {
  ReactFlowProvider,
  Controls,
  Background,
  useNodesState,
  useEdgesState,
  addEdge,
  Connection,
  Edge,
  Node,
  NodeTypes,
  OnConnect,
  XYPosition,
  ReactFlowInstance
} from 'reactflow';
import 'reactflow/dist/style.css';
import { Layout } from 'antd';
import { NodeConfig } from '../../lib/config/node.config';
import NodePanel from '../NodePanel';
import NodePropertiesPanel from '../NodePropertiesPanel';
import styles from './index.module.css';

const { Sider, Content } = Layout;

// 自定义节点类型
const nodeTypes: NodeTypes = {
  // 可以在这里添加自定义节点类型
};

export interface FlowEditorProps {
  initialNodes?: Node[];
  initialEdges?: Edge[];
  onSave?: (nodes: Node[], edges: Edge[]) => void;
  readOnly?: boolean;
}

const FlowEditor: React.FC<FlowEditorProps> = ({
  initialNodes = [],
  initialEdges = [],
  onSave,
  readOnly = false
}) => {
  // 流程图状态
  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);
  const [reactFlowInstance, setReactFlowInstance] = useState<ReactFlowInstance | null>(null);
  
  // 属性面板状态
  const [selectedNode, setSelectedNode] = useState<Node | null>(null);
  const [propertiesPanelVisible, setPropertiesPanelVisible] = useState(false);
  const [loading, setLoading] = useState(false);

  // 引用DOM元素
  const reactFlowWrapper = useRef<HTMLDivElement>(null);

  // 处理节点拖放
  const onDragOver = useCallback((event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    event.dataTransfer.dropEffect = 'move';
  }, []);

  const onDrop = useCallback(
    (event: React.DragEvent<HTMLDivElement>) => {
      event.preventDefault();

      if (!reactFlowWrapper.current || !reactFlowInstance) {
        return;
      }

      const reactFlowBounds = reactFlowWrapper.current.getBoundingClientRect();
      const nodeConfigStr = event.dataTransfer.getData('application/reactflow');
      
      try {
        const nodeConfig = JSON.parse(nodeConfigStr) as NodeConfig;
        
        // 计算放置位置
        const position: XYPosition = reactFlowInstance.project({
          x: event.clientX - reactFlowBounds.left,
          y: event.clientY - reactFlowBounds.top,
        });

        // 创建新节点
        const newNode: Node = {
          id: `${nodeConfig.code}_${Date.now()}`,
          type: 'default', // 可以根据nodeConfig.style设置不同的节点类型
          position,
          data: { 
            label: nodeConfig.name,
            code: nodeConfig.code,
            group: nodeConfig.group,
            ports: nodeConfig.ports,
            properties: {} // 初始化空属性
          }
        };

        // 添加新节点
        setNodes((nds) => nds.concat(newNode));
      } catch (error) {
        console.error('无法解析节点配置:', error);
      }
    },
    [reactFlowInstance, setNodes]
  );

  // 处理节点连接
  const onConnect: OnConnect = useCallback(
    (params: Connection) => setEdges((eds) => addEdge(params, eds)),
    [setEdges]
  );

  // 处理节点选择
  const onNodeClick = (_: React.MouseEvent, node: Node) => {
    setSelectedNode(node);
    setPropertiesPanelVisible(true);
  };

  // 关闭属性面板
  const closePropertiesPanel = () => {
    setPropertiesPanelVisible(false);
  };

  // 保存节点属性
  const saveNodeProperties = (nodeId: string, properties: any) => {
    setLoading(true);
    
    // 更新节点属性
    setNodes((nds) =>
      nds.map((node) => {
        if (node.id === nodeId) {
          return {
            ...node,
            data: {
              ...node.data,
              properties
            }
          };
        }
        return node;
      })
    );

    // 模拟异步操作
    setTimeout(() => {
      setLoading(false);
      setPropertiesPanelVisible(false);
    }, 500);
  };

  // 保存整个流程图
  const handleSave = () => {
    if (onSave) {
      onSave(nodes, edges);
    }
  };

  return (
    <Layout className={styles.flowEditor}>
      <Sider width={250} theme="light" className={styles.sider}>
        <NodePanel />
      </Sider>
      <Content className={styles.content}>
        <ReactFlowProvider>
          <div className={styles.reactFlowWrapper} ref={reactFlowWrapper}>
            <ReactFlow
              nodes={nodes}
              edges={edges}
              onNodesChange={onNodesChange}
              onEdgesChange={onEdgesChange}
              onConnect={onConnect}
              onInit={setReactFlowInstance}
              onDrop={onDrop}
              onDragOver={onDragOver}
              onNodeClick={onNodeClick}
              nodeTypes={nodeTypes}
              fitView
              snapToGrid
              snapGrid={[15, 15]}
            >
              <Controls />
              <Background />
            </ReactFlow>
          </div>
        </ReactFlowProvider>
      </Content>

      <NodePropertiesPanel
        visible={propertiesPanelVisible}
        nodeData={selectedNode?.data}
        onClose={closePropertiesPanel}
        onSave={saveNodeProperties}
        loading={loading}
      />
    </Layout>
  );
};

export default FlowEditor; 