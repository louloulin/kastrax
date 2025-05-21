import React, { memo, useState } from 'react';
import { Handle, Position, NodeProps } from 'reactflow';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Pencil1Icon } from '@radix-ui/react-icons';

const TransformNode = memo(({ data, id, selected }: NodeProps) => {
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [transformType, setTransformType] = useState(data.config?.transform || 'mapFields');
  const [mappingText, setMappingText] = useState(
    JSON.stringify(data.config?.mapping || {}, null, 2)
  );

  const handleSave = () => {
    try {
      const mapping = mappingText.trim() ? JSON.parse(mappingText) : {};
      data.config = { 
        ...data.config,
        transform: transformType,
        mapping
      };
      setIsDialogOpen(false);
    } catch (e) {
      console.error('Invalid JSON for mapping:', e);
      alert('映射JSON格式无效，请检查');
    }
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
        className={`relative rounded-lg border ${selected ? 'border-purple-600 ring-2 ring-primary' : 'border-purple-500'} bg-white px-4 py-2 shadow-md cursor-pointer`}
        onClick={handleNodeClick}
      >
        <div className="flex items-center">
          <div className="mr-2 h-3 w-3 rounded-full bg-purple-500"></div>
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
        
        {data.config?.transform && (
          <div className="mt-1 text-xs text-gray-500">
            <div className="font-medium">{data.config.transform}</div>
            {Object.keys(data.config.mapping || {}).length > 0 && (
              <div className="mt-1 text-xs text-gray-400">
                映射字段: {Object.keys(data.config.mapping).length} 项
              </div>
            )}
          </div>
        )}
        
        {/* 输入连接点 */}
        <Handle
          type="target"
          position={Position.Top}
          id="in"
          className="h-3 w-3 bg-purple-500"
        />
        
        {/* 输出连接点 */}
        <Handle
          type="source"
          position={Position.Bottom}
          id="out"
          className="h-3 w-3 bg-purple-500"
        />
      </div>
      
      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent className="sm:max-w-[500px]">
          <DialogHeader>
            <DialogTitle>编辑数据转换</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="grid grid-cols-4 items-center gap-4">
              <Label htmlFor="transformType" className="text-right">
                转换类型
              </Label>
              <Input
                id="transformType"
                value={transformType}
                onChange={(e) => setTransformType(e.target.value)}
                className="col-span-3"
              />
            </div>
            <div className="grid grid-cols-4 items-start gap-4">
              <Label htmlFor="mapping" className="text-right pt-2">
                映射配置 (JSON)
              </Label>
              <Textarea
                id="mapping"
                value={mappingText}
                onChange={(e) => setMappingText(e.target.value)}
                className="col-span-3 font-mono text-xs h-32"
              />
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

TransformNode.displayName = 'TransformNode';

export default TransformNode; 