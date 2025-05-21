import React, { useState } from 'react';
import { Handle, Position } from 'reactflow';
import { SinkType } from '@/lib/api/workflows';
import { 
  DatabaseIcon, 
  FileTextIcon, 
  MessagesSquareIcon, 
  MailIcon, 
  GlobeIcon, 
  ServerIcon 
} from 'lucide-react';
import { cn } from '@/lib/utils';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover';
import { Button } from '@/components/ui/button';
import { ChevronDown } from 'lucide-react';

interface SinkNodeProps {
  id: string;
  data: {
    label: string;
    subType: SinkType;
    config: any;
    onConfigure?: (id: string) => void;
  };
  selected: boolean;
}

const SinkNode: React.FC<SinkNodeProps> = ({ id, data, selected }) => {
  const [isOpen, setIsOpen] = useState(false);
  
  // Get icon based on sink type
  const getIcon = () => {
    switch (data.subType) {
      case SinkType.DATABASE:
        return <DatabaseIcon className="w-4 h-4" />;
      case SinkType.FILE:
        return <FileTextIcon className="w-4 h-4" />;
      case SinkType.MESSAGE:
        return <MessagesSquareIcon className="w-4 h-4" />;
      case SinkType.EMAIL:
        return <MailIcon className="w-4 h-4" />;
      case SinkType.API:
        return <GlobeIcon className="w-4 h-4" />;
      case SinkType.SERVICE:
        return <ServerIcon className="w-4 h-4" />;
      default:
        return <DatabaseIcon className="w-4 h-4" />;
    }
  };
  
  // Get background color
  const getBgColor = () => {
    switch (data.subType) {
      case SinkType.DATABASE:
        return 'bg-blue-500';
      case SinkType.FILE:
        return 'bg-orange-500';
      case SinkType.MESSAGE:
        return 'bg-teal-500';
      case SinkType.EMAIL:
        return 'bg-sky-500';
      case SinkType.API:
        return 'bg-violet-500';
      case SinkType.SERVICE:
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
      case SinkType.DATABASE:
        return '数据库';
      case SinkType.FILE:
        return '文件';
      case SinkType.MESSAGE:
        return '消息';
      case SinkType.EMAIL:
        return '邮件';
      case SinkType.API:
        return 'API';
      case SinkType.SERVICE:
        return '服务';
      default:
        return '输出';
    }
  };
  
  // Render configuration preview
  const renderConfigPreview = () => {
    switch (data.subType) {
      case SinkType.DATABASE:
        return (
          <div className="text-xs text-muted-foreground">
            {data.config.resourceId && <div>资源ID: {data.config.resourceId}</div>}
            {data.config.table && <div>表名: {data.config.table}</div>}
            {data.config.operation && <div>操作: {data.config.operation}</div>}
          </div>
        );
      case SinkType.FILE:
        return (
          <div className="text-xs text-muted-foreground">
            {data.config.path && <div>路径: {data.config.path}</div>}
            {data.config.format && <div>格式: {data.config.format}</div>}
          </div>
        );
      case SinkType.MESSAGE:
        return (
          <div className="text-xs text-muted-foreground">
            {data.config.channel && <div>频道: {data.config.channel}</div>}
            {data.config.template && (
              <div className="truncate max-w-[200px]">
                模板: {data.config.template.substring(0, 30)}
                {data.config.template.length > 30 ? '...' : ''}
              </div>
            )}
          </div>
        );
      case SinkType.EMAIL:
        return (
          <div className="text-xs text-muted-foreground">
            {data.config.to && <div>收件人: {data.config.to.slice(0, 2).join(', ')}{data.config.to.length > 2 ? '...' : ''}</div>}
            {data.config.subject && <div>主题: {data.config.subject}</div>}
          </div>
        );
      case SinkType.API:
        return (
          <div className="text-xs text-muted-foreground">
            {data.config.url && <div>URL: {data.config.url}</div>}
            {data.config.method && <div>方法: {data.config.method}</div>}
          </div>
        );
      case SinkType.SERVICE:
        return (
          <div className="text-xs text-muted-foreground">
            {data.config.service && <div>服务: {data.config.service}</div>}
            {data.config.action && <div>操作: {data.config.action}</div>}
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
              <div className="text-muted-foreground mb-2">输出 ({data.subType})</div>
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
    </div>
  );
};

export default SinkNode; 