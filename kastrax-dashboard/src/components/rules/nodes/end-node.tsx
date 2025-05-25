import React, { memo } from 'react';
import { Handle, Position, NodeProps } from 'reactflow';

const EndNode = memo(({ data, id, selected }: NodeProps) => {
  const handleNodeClick = () => {
    if (data.onConfigure) {
      data.onConfigure(id);
    }
  };

  return (
    <div 
      className={`relative rounded-lg border ${selected ? 'border-red-600 ring-2 ring-primary' : 'border-red-500'} bg-white px-4 py-2 shadow-md cursor-pointer`}
      onClick={handleNodeClick}
    >
      <div className="flex items-center">
        <div className="mr-2 h-3 w-3 rounded-full bg-red-500"></div>
        <div>{data.label || '结束'}</div>
      </div>
      
      {/* 只有输入连接点 */}
      <Handle
        type="target"
        position={Position.Top}
        id="in"
        className="h-3 w-3 bg-red-500"
      />
    </div>
  );
});

EndNode.displayName = 'EndNode';

export default EndNode; 