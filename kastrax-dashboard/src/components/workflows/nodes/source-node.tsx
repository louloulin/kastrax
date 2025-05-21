import React, { useState } from 'react';
import { Handle, Position } from 'reactflow';
import { SourceType } from '@/lib/api/workflows';
import { DatabaseIcon, GlobeIcon, FileIcon, CalendarIcon, WebhookIcon, ServerIcon } from 'lucide-react';
import { cn } from '@/lib/utils';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover';
import { Button } from '@/components/ui/button';
import { ChevronDown } from 'lucide-react';

interface SourceNodeProps {
  id: string;
  data: {
    label: string;
    subType: SourceType;
    config: any;
    onConfigure?: (id: string) => void;
  };
  selected: boolean;
}

const SourceNode: React.FC<SourceNodeProps> = ({ id, data, selected }) => {
  const [isOpen, setIsOpen] = useState(false);
  
  // Get icon based on source type
  const getIcon = () => {
    switch (data.subType) {
      case SourceType.DATABASE:
        return <DatabaseIcon className="w-4 h-4" />;
      case SourceType.API:
        return <GlobeIcon className="w-4 h-4" />;
      case SourceType.FILE:
        return <FileIcon className="w-4 h-4" />;
      case SourceType.WEBHOOK:
        return <WebhookIcon className="w-4 h-4" />;
      case SourceType.SCHEDULE:
        return <CalendarIcon className="w-4 h-4" />;
      case SourceType.RESOURCE:
        return <ServerIcon className="w-4 h-4" />;
      default:
        return <DatabaseIcon className="w-4 h-4" />;
    }
  };
  
  // Get background color
  const getBgColor = () => {
    switch (data.subType) {
      case SourceType.DATABASE:
        return 'bg-blue-500';
      case SourceType.API:
        return 'bg-green-500';
      case SourceType.FILE:
        return 'bg-amber-500';
      case SourceType.WEBHOOK:
        return 'bg-purple-500';
      case SourceType.SCHEDULE:
        return 'bg-orange-500';
      case SourceType.RESOURCE:
        return 'bg-slate-500';
      default:
        return 'bg-blue-500';
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
      case SourceType.DATABASE:
        return '数据库';
      case SourceType.API:
        return 'API';
      case SourceType.FILE:
        return '文件';
      case SourceType.WEBHOOK:
        return 'Webhook';
      case SourceType.SCHEDULE:
        return '定时';
      case SourceType.RESOURCE:
        return '资源';
      default:
        return '数据源';
    }
  };
  
  // Render configuration preview
  const renderConfigPreview = () => {
    switch (data.subType) {
      case SourceType.DATABASE:
        return (
          <div className="text-xs text-muted-foreground">
            {data.config.resourceId && <div>资源: {data.config.resourceId}</div>}
            {data.config.query && <div className="truncate max-w-[200px]">查询: {data.config.query.substring(0, 40)}{data.config.query.length > 40 ? '...' : ''}</div>}
          </div>
        );
      case SourceType.API:
        return (
          <div className="text-xs text-muted-foreground">
            {data.config.url && <div className="truncate max-w-[200px]">URL: {data.config.url}</div>}
            {data.config.method && <div>方法: {data.config.method}</div>}
          </div>
        );
      case SourceType.FILE:
        return (
          <div className="text-xs text-muted-foreground">
            {data.config.path && <div className="truncate max-w-[200px]">路径: {data.config.path}</div>}
            {data.config.format && <div>格式: {data.config.format}</div>}
          </div>
        );
      case SourceType.SCHEDULE:
        return (
          <div className="text-xs text-muted-foreground">
            {data.config.schedule && <div>Cron: {data.config.schedule}</div>}
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
              <div className="text-muted-foreground mb-2">数据源 ({data.subType})</div>
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

export default SourceNode; 