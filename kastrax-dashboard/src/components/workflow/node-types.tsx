import React from 'react';
import KafkaNode from './nodes/kafka-node';
import { NodeProps } from 'reactflow';

// 默认节点组件，用于尚未实现的节点类型
const DefaultNode: React.FC<NodeProps> = ({ data, selected }) => {
  // 简单的默认实现，显示节点类型和标签
  const style: React.CSSProperties = {
    border: selected ? '2px solid #1890ff' : '1px solid #ddd',
    borderRadius: '4px',
    padding: '10px',
    backgroundColor: 'white',
    minWidth: '150px',
    minHeight: '40px',
    fontSize: '12px',
  };

  return (
    <div style={style}>
      <div style={{ fontWeight: 'bold' }}>{data.label}</div>
      <div style={{ fontSize: '10px', color: '#666' }}>{data.nodeType}</div>
    </div>
  );
};

// 节点类型映射
export const nodeTypes: Record<string, React.FC<NodeProps>> = {
  // 消息队列节点
  KAFKA_SUBSCRIBE: KafkaNode,
  KAFKA_PUBLISH: KafkaNode,
  
  // 默认使用简单的默认节点组件
  default: DefaultNode,
};

// 根据节点类型获取组件，如果没有特定实现则返回默认组件
export const getNodeComponent = (nodeType: string): React.FC<NodeProps> => {
  return (nodeTypes[nodeType] as React.FC<NodeProps>) || nodeTypes.default;
}; 