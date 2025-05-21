import React, { memo, useState } from 'react';
import { Handle, Position, NodeProps } from 'reactflow';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Pencil1Icon } from '@radix-ui/react-icons';

const ConditionNode = memo(({ data, id, selected }: NodeProps) => {
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [condition, setCondition] = useState(data.config?.condition || 'value > 0');

  const handleSave = () => {
    data.config = { ...data.config, condition };
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

  return (
    <>
      <div 
        className={`relative rounded-lg border ${selected ? 'border-yellow-600 ring-2 ring-primary' : 'border-yellow-500'} bg-white px-4 py-2 shadow-md cursor-pointer`}
        onClick={handleNodeClick}
      >
        <div className="flex items-center">
          <div className="mr-2 h-3 w-3 rounded-full bg-yellow-500"></div>
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
        
        {data.config?.condition && (
          <div className="mt-1 text-xs text-gray-500 italic">
            {data.config.condition}
          </div>
        )}
        
        {/* 输入连接点 */}
        <Handle
          type="target"
          position={Position.Top}
          id="in"
          className="h-3 w-3 bg-yellow-500"
        />
        
        {/* 输出连接点 - 条件为真 */}
        <Handle
          type="source"
          position={Position.Bottom}
          id="true"
          className="h-3 w-3 bg-green-500 left-1/4"
        />
        
        {/* 输出连接点 - 条件为假 */}
        <Handle
          type="source"
          position={Position.Bottom}
          id="false"
          className="h-3 w-3 bg-red-500 left-3/4"
        />
      </div>
      
      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent className="sm:max-w-[425px]">
          <DialogHeader>
            <DialogTitle>编辑条件</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="grid grid-cols-4 items-center gap-4">
              <Label htmlFor="condition" className="text-right">
                条件表达式
              </Label>
              <Input
                id="condition"
                value={condition}
                onChange={(e) => setCondition(e.target.value)}
                className="col-span-3"
              />
            </div>
            <div className="col-span-4 text-xs text-muted-foreground">
              使用JavaScript表达式，如：value &gt; 10 || status === 'active'
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

ConditionNode.displayName = 'ConditionNode';

export default ConditionNode; 