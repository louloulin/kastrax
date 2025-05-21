import React, { memo } from 'react';
import { Handle, Position, NodeProps } from 'reactflow';

const StartNode = memo(({ data, id, selected }: NodeProps) => {
  const handleNodeClick = () => {
    if (data.onConfigure) {
      data.onConfigure(id);
    }
  };

  return (
    <div 
      className={`relative rounded-lg border ${selected ? 'border-green-600 ring-2 ring-primary' : 'border-green-500'} bg-white px-4 py-2 shadow-md cursor-pointer`}
      onClick={handleNodeClick}
    >
      <div className="flex items-center">
        <div className="mr-2 h-3 w-3 rounded-full bg-green-500"></div>
        <div>{data.label || '开始'}</div>
      </div>
      
      {/* 只有输出连接点 */}
      <Handle
        type="source"
        position={Position.Bottom}
        id="out"
        className="h-3 w-3 bg-green-500"
      />
    </div>
  );
});

StartNode.displayName = 'StartNode';

export default StartNode; 