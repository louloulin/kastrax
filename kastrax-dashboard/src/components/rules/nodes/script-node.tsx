import React, { memo, useState, useEffect } from 'react';
import { Handle, Position, NodeProps } from 'reactflow';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Pencil1Icon, ReloadIcon } from '@radix-ui/react-icons';
import { getScriptList } from '@/lib/api/scripts';

const ScriptNode = memo(({ data, id, selected }: NodeProps) => {
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [scriptId, setScriptId] = useState(data.config?.scriptId || '');
  const [paramsText, setParamsText] = useState(
    JSON.stringify(data.config?.params || {}, null, 2)
  );
  const [scripts, setScripts] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [language, setLanguage] = useState(data.config?.language || '');
  const [timeout, setTimeout] = useState(data.config?.timeout?.toString() || '0');

  useEffect(() => {
    if (isDialogOpen) {
      fetchScripts();
    }
  }, [isDialogOpen]);

  const fetchScripts = async () => {
    setLoading(true);
    try {
      const response = await getScriptList();
      setScripts(response.data.data.list || []);
    } catch (error) {
      console.error('Failed to fetch scripts:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSave = () => {
    try {
      const params = paramsText.trim() ? JSON.parse(paramsText) : {};
      data.config = { 
        ...data.config,
        scriptId,
        language,
        params,
        timeout: Number(timeout)
      };
      setIsDialogOpen(false);
    } catch (e) {
      console.error('Invalid JSON for params:', e);
      alert('参数JSON格式无效，请检查');
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

  const selectedScript = scripts.find(s => s.scriptId === scriptId);

  return (
    <>
      <div 
        className={`relative rounded-lg border ${selected ? 'border-emerald-600 ring-2 ring-primary' : 'border-emerald-500'} bg-white px-4 py-2 shadow-md cursor-pointer`}
        onClick={handleNodeClick}
      >
        <div className="flex items-center">
          <div className="mr-2 h-3 w-3 rounded-full bg-emerald-500"></div>
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
        
        {data.config?.scriptId && (
          <div className="mt-1 text-xs text-gray-500">
            <div className="font-medium">脚本ID: {data.config.scriptId}</div>
            {Object.keys(data.config.params || {}).length > 0 && (
              <div className="mt-1 text-xs text-gray-400">
                <div className="mb-1">参数:</div>
                {Object.entries(data.config.params).slice(0, 3).map(([key, value]) => (
                  <div key={key} className="truncate">
                    {key}: {JSON.stringify(value).substring(0, 15)}
                    {JSON.stringify(value).length > 15 ? '...' : ''}
                  </div>
                ))}
                {Object.keys(data.config.params).length > 3 && (
                  <div className="italic">+{Object.keys(data.config.params).length - 3} 个参数...</div>
                )}
              </div>
            )}
          </div>
        )}
        
        {/* 输入连接点 */}
        <Handle
          type="target"
          position={Position.Top}
          id="in"
          className="h-3 w-3 bg-emerald-500"
        />
        
        {/* 输出连接点 */}
        <Handle
          type="source"
          position={Position.Bottom}
          id="out"
          className="h-3 w-3 bg-emerald-500"
        />
      </div>
      
      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent className="sm:max-w-[500px]">
          <DialogHeader>
            <DialogTitle>配置脚本</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="grid grid-cols-4 items-center gap-4">
              <Label htmlFor="scriptId" className="text-right">
                选择脚本
              </Label>
              <div className="col-span-3 flex gap-2">
                <Select
                  value={scriptId}
                  onValueChange={setScriptId}
                >
                  <SelectTrigger className="flex-1">
                    <SelectValue placeholder="选择脚本" />
                  </SelectTrigger>
                  <SelectContent>
                    {loading ? (
                      <div className="flex items-center justify-center p-2">
                        <ReloadIcon className="h-4 w-4 animate-spin mr-2" />
                        加载中...
                      </div>
                    ) : (
                      scripts.map(script => (
                        <SelectItem key={script.scriptId} value={script.scriptId}>
                          {script.scriptName}
                        </SelectItem>
                      ))
                    )}
                  </SelectContent>
                </Select>
                <Button type="button" size="sm" variant="outline" onClick={fetchScripts}>
                  <ReloadIcon className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
                </Button>
              </div>
            </div>
            <div className="grid grid-cols-4 items-start gap-4">
              <Label htmlFor="params" className="text-right pt-2">
                参数 (JSON)
              </Label>
              <Textarea
                id="params"
                value={paramsText}
                onChange={(e) => setParamsText(e.target.value)}
                className="col-span-3 font-mono text-xs h-32"
              />
            </div>
            {selectedScript && (
              <div className="col-span-4 text-xs bg-gray-50 p-2 rounded">
                <div className="font-medium mb-1">脚本信息</div>
                <div>名称: {selectedScript.scriptName}</div>
                <div>描述: {selectedScript.description || '无'}</div>
                <div>语言: {selectedScript.language}</div>
              </div>
            )}
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

ScriptNode.displayName = 'ScriptNode';

export default ScriptNode; 