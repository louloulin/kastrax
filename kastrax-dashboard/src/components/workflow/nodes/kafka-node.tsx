import React from 'react';
import { Handle, Position } from 'reactflow';
import { ApiOutlined } from '@ant-design/icons';

interface KafkaNodeProps {
  data: {
    label: string;
    nodeType?: string;
    type?: string;
    onConfigure?: (nodeId: string) => void;
    id?: string;
    properties?: Record<string, any>;
    config?: Record<string, any>;
  };
  id: string;
  selected: boolean;
}

const KafkaNode: React.FC<KafkaNodeProps> = ({ data, id, selected }) => {
  // 确定节点类型 - 从data.nodeType或节点类型本身
  const nodeType = data.nodeType || data.type || (data.label?.includes('订阅') ? 'KAFKA_SUBSCRIBE' : 'KAFKA_PUBLISH');
  const isSubscribe = nodeType === 'KAFKA_SUBSCRIBE';
  
  // 使用节点属性数据 - 无论是从properties还是config获取
  const properties = data.properties || data.config || {};
  
  const handleConfigure = () => {
    if (data.onConfigure) {
      // 使用节点ID，无论从data.id还是直接从节点id获取
      data.onConfigure(data.id || id);
    }
  };
  
  return (
    <div 
      className={`flex flex-col p-2 rounded-md border ${
        selected ? 'border-blue-500 shadow-md' : 'border-gray-300'
      } bg-white min-w-[140px]`}
      onClick={handleConfigure}
    >
      <div className="flex items-center gap-2 font-medium text-sm">
        <ApiOutlined className={isSubscribe ? 'text-green-600' : 'text-blue-600'} />
        <div className="truncate">{data.label || (isSubscribe ? 'Kafka 订阅' : 'Kafka 发布')}</div>
      </div>
      
      {properties && (
        <div className="mt-2 text-xs text-gray-600 border-t pt-1">
          {properties.topic && (
            <div className="flex justify-between">
              <span>主题:</span>
              <span className="font-mono">{properties.topic}</span>
            </div>
          )}
          {properties.brokers && (
            <div className="flex justify-between">
              <span>Broker:</span>
              <span className="font-mono truncate max-w-[80px]" title={properties.brokers}>
                {properties.brokers}
              </span>
            </div>
          )}
          {isSubscribe && properties.groupId && (
            <div className="flex justify-between">
              <span>分组:</span>
              <span className="font-mono">{properties.groupId}</span>
            </div>
          )}
        </div>
      )}
      
      {isSubscribe ? (
        <Handle
          type="source"
          position={Position.Right}
          id="source"
          style={{ background: '#4caf50', width: 8, height: 8 }}
        />
      ) : (
        <>
          <Handle
            type="target"
            position={Position.Left}
            id="target"
            style={{ background: '#2196f3', width: 8, height: 8 }}
          />
        </>
      )}
    </div>
  );
};

export default KafkaNode; 