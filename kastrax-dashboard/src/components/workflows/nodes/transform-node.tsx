import React, { useState } from 'react';
import { Handle, Position } from 'reactflow';
import { TransformType } from '@/lib/api/workflows';
import { FilterIcon, MoveIcon, BarChart2Icon, GitMergeIcon, Code2Icon, SplitIcon } from 'lucide-react';
import { cn } from '@/lib/utils';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover';
import { Button } from '@/components/ui/button';
import { ChevronDown } from 'lucide-react';

interface TransformNodeProps {
  id: string;
  data: {
    label: string;
    subType: TransformType;
    config: any;
    onConfigure?: (id: string) => void;
  };
  selected: boolean;
}

const TransformNode: React.FC<TransformNodeProps> = ({ id, data, selected }) => {
  const [isOpen, setIsOpen] = useState(false);
  
  // Get icon based on transform type
  const getIcon = () => {
    switch (data.subType) {
      case TransformType.FILTER:
        return <FilterIcon className="w-4 h-4" />;
      case TransformType.MAP:
        return <MoveIcon className="w-4 h-4" />;
      case TransformType.AGGREGATE:
        return <BarChart2Icon className="w-4 h-4" />;
      case TransformType.JOIN:
        return <GitMergeIcon className="w-4 h-4" />;
      case TransformType.SCRIPT:
        return <Code2Icon className="w-4 h-4" />;
      case TransformType.SPLIT:
        return <SplitIcon className="w-4 h-4" />;
      default:
        return <MoveIcon className="w-4 h-4" />;
    }
  };
  
  // Get background color
  const getBgColor = () => {
    switch (data.subType) {
      case TransformType.FILTER:
        return 'bg-yellow-500';
      case TransformType.MAP:
        return 'bg-indigo-500';
      case TransformType.AGGREGATE:
        return 'bg-emerald-500';
      case TransformType.JOIN:
        return 'bg-purple-500';
      case TransformType.SCRIPT:
        return 'bg-rose-500';
      case TransformType.SPLIT:
        return 'bg-cyan-500';
      default:
        return 'bg-indigo-500';
    }
  };
  
  // Get border color for selected state
  const getBorderColor = () => {
    if (selected) {
      return 'border-primary ring-2 ring-primary';
    }
    return 'border-border';
  };
  
  // Get type label
  const getTypeLabel = () => {
    switch (data.subType) {
      case TransformType.FILTER:
        return '过滤';
      case TransformType.MAP:
        return '映射';
      case TransformType.AGGREGATE:
        return '聚合';
      case TransformType.JOIN:
        return '连接';
      case TransformType.SCRIPT:
        return '脚本';
      case TransformType.SPLIT:
        return '拆分';
      default:
        return '转换';
    }
  };
  
  // Render configuration preview
  const renderConfigPreview = () => {
    switch (data.subType) {
      case TransformType.FILTER:
        return (
          <div className="text-xs text-muted-foreground">
            {data.config.condition && (
              <div className="truncate max-w-[200px]">
                条件: {data.config.condition.substring(0, 40)}
                {data.config.condition.length > 40 ? '...' : ''}
              </div>
            )}
          </div>
        );
      case TransformType.MAP:
        return (
          <div className="text-xs text-muted-foreground">
            {data.config.mappings && (
              <div>
                字段映射: {Object.keys(data.config.mappings).length} 项
              </div>
            )}
          </div>
        );
      case TransformType.AGGREGATE:
        return (
          <div className="text-xs text-muted-foreground">
            {data.config.groupBy && <div>分组: {data.config.groupBy.join(', ')}</div>}
            {data.config.functions && <div>函数: {Object.keys(data.config.functions).length} 项</div>}
          </div>
        );
      case TransformType.JOIN:
        return (
          <div className="text-xs text-muted-foreground">
            {data.config.type && <div>类型: {data.config.type}</div>}
            {data.config.on && <div>条件: {data.config.on.join(' = ')}</div>}
          </div>
        );
      case TransformType.SCRIPT:
        return (
          <div className="text-xs text-muted-foreground">
            {data.config.scriptId && <div>脚本ID: {data.config.scriptId}</div>}
            {data.config.language && <div>语言: {data.config.language}</div>}
          </div>
        );
      case TransformType.SPLIT:
        return (
          <div className="text-xs text-muted-foreground">
            {data.config.field && <div>拆分字段: {data.config.field}</div>}
            {data.config.separator && <div>分隔符: {data.config.separator}</div>}
          </div>
        );
      default:
        return null;
    }
  };
  
  const handleNodeClick = () => {
    if (data.onConfigure) {
      data.onConfigure(id);
    }
  };
  
  return (
    <div 
      className={cn(
        "min-w-[180px] max-w-[250px] bg-card border shadow-sm rounded-md cursor-pointer",
        getBorderColor()
      )}
      onClick={handleNodeClick}
    >
      <div className="flex items-center px-3 py-2 border-b">
        <div className={cn("w-3 h-3 rounded-full mr-2", getBgColor())}></div>
        <div className="flex items-center text-sm font-medium">
          {getIcon()}
          <span className="ml-1.5">{getTypeLabel()}</span>
        </div>
        <Popover open={isOpen} onOpenChange={setIsOpen}>
          <PopoverTrigger asChild>
            <Button variant="ghost" className="ml-auto h-6 w-6 p-0" onClick={(e) => {
              e.stopPropagation();
              setIsOpen(!isOpen);
            }}>
              <ChevronDown className="h-4 w-4" />
            </Button>
          </PopoverTrigger>
          <PopoverContent className="w-[220px] p-2" align="end" onClick={(e) => e.stopPropagation()}>
            <div className="text-xs">
              <div className="font-medium mb-1">组件ID</div>
              <div className="text-muted-foreground mb-2">{id}</div>
              <div className="font-medium mb-1">组件类型</div>
              <div className="text-muted-foreground mb-2">转换 ({data.subType})</div>
              {Object.keys(data.config || {}).length > 0 && (
                <>
                  <div className="font-medium mb-1">配置</div>
                  <pre className="bg-muted p-1 rounded text-xs overflow-auto max-h-[100px]">
                    {JSON.stringify(data.config, null, 2)}
                  </pre>
                </>
              )}
            </div>
          </PopoverContent>
        </Popover>
      </div>
      
      <div className="px-3 py-2">
        <div className="font-medium text-sm mb-1 truncate" title={data.label}>
          {data.label}
        </div>
        {renderConfigPreview()}
      </div>
      
      {/* Input handle */}
      <Handle
        type="target"
        position={Position.Left}
        id="input"
        className="w-3 h-3 bg-foreground border-background"
      />
      
      {/* Output handle */}
      <Handle
        type="source"
        position={Position.Right}
        id="output"
        className="w-3 h-3 bg-foreground border-background"
      />
    </div>
  );
};

export default TransformNode; 