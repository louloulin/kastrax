import React, { memo, useState } from 'react';
import { Handle, Position, NodeProps } from 'reactflow';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Pencil1Icon } from '@radix-ui/react-icons';
import { ClockIcon } from '@radix-ui/react-icons';

const DelayNode = memo(({ data, id, selected }: NodeProps) => {
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [duration, setDuration] = useState((data.config?.duration || 1000).toString());

  const handleSave = () => {
    const durationValue = parseInt(duration);
    if (isNaN(durationValue) || durationValue <= 0) {
      alert('请输入有效的延迟时间（毫秒）');
      return;
    }

    data.config = { 
      ...data.config,
      duration: durationValue
    };
    setIsDialogOpen(false);
  };

  const handleNodeClick = () => {
    if (data.onConfigure) {
      data.onConfigure(id);
    }
  };

  const handleEditButtonClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    setIsDialogOpen(true);
  };

  // Format the duration for display
  const formatDuration = (ms: number) => {
    if (ms < 1000) {
      return `${ms}毫秒`;
    } else if (ms < 60000) {
      return `${(ms / 1000).toFixed(1)}秒`;
    } else {
      return `${(ms / 60000).toFixed(1)}分钟`;
    }
  };

  return (
    <>
      <div 
        className={`relative rounded-lg border ${selected ? 'border-orange-600 ring-2 ring-primary' : 'border-orange-500'} bg-white px-4 py-2 shadow-md cursor-pointer`}
        onClick={handleNodeClick}
      >
        <div className="flex items-center">
          <div className="mr-2 h-3 w-3 rounded-full bg-orange-500"></div>
          <div className="flex-1 text-sm font-medium">{data.label}</div>
          <Button 
            variant="ghost" 
            size="sm" 
            className="h-6 w-6 p-0" 
            onClick={handleEditButtonClick}
          >
            <Pencil1Icon className="h-3 w-3" />
          </Button>
        </div>
        
        {data.config?.duration && (
          <div className="mt-1 text-xs text-gray-500 flex items-center">
            <ClockIcon className="h-3 w-3 mr-1" />
            <span>延迟时间: {formatDuration(data.config.duration)}</span>
          </div>
        )}
        
        {/* 输入连接点 */}
        <Handle
          type="target"
          position={Position.Top}
          id="in"
          className="h-3 w-3 bg-orange-500"
        />
        
        {/* 输出连接点 */}
        <Handle
          type="source"
          position={Position.Bottom}
          id="out"
          className="h-3 w-3 bg-orange-500"
        />
      </div>
      
      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent className="sm:max-w-[425px]">
          <DialogHeader>
            <DialogTitle>编辑延迟</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="grid grid-cols-4 items-center gap-4">
              <Label htmlFor="duration" className="text-right">
                延迟时间 (毫秒)
              </Label>
              <Input
                id="duration"
                type="number"
                value={duration}
                onChange={(e) => setDuration(e.target.value)}
                className="col-span-3"
                min="1"
              />
            </div>
            <div className="col-span-4 text-xs text-muted-foreground pl-[25%]">
              {duration && !isNaN(parseInt(duration)) && parseInt(duration) > 0 && (
                <div>相当于 {formatDuration(parseInt(duration))}</div>
              )}
            </div>
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => setIsDialogOpen(false)}>
              取消
            </Button>
            <Button type="button" onClick={handleSave}>
              保存
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
});

DelayNode.displayName = 'DelayNode';

export default DelayNode; 