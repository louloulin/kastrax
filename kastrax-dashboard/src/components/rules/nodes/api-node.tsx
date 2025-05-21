import React, { memo, useState } from 'react';
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
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Pencil1Icon } from '@radix-ui/react-icons';

const ApiNode = memo(({ data, id, selected }: NodeProps) => {
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [url, setUrl] = useState(data.config?.url || 'https://api.example.com');
  const [method, setMethod] = useState(data.config?.method || 'GET');
  const [headersText, setHeadersText] = useState(
    JSON.stringify(data.config?.headers || {}, null, 2)
  );
  const [bodyText, setBodyText] = useState(
    JSON.stringify(data.config?.body || {}, null, 2)
  );
  const [timeout, setTimeout] = useState(data.config?.timeout || 5000);

  // Helpers
  const getHeadersText = () => {
    try {
      return JSON.stringify(headersText ? JSON.parse(headersText) : {}, null, 2);
    } catch (e) {
      return headersText;
    }
  };
  
  const getBodyText = () => {
    try {
      return JSON.stringify(bodyText ? JSON.parse(bodyText) : {}, null, 2);
    } catch (e) {
      return bodyText;
    }
  };
  
  const getMethodColor = (method: string) => {
    switch (method) {
      case 'GET':
        return 'bg-green-100 text-green-800';
      case 'POST':
        return 'bg-blue-100 text-blue-800';
      case 'PUT':
        return 'bg-amber-100 text-amber-800';
      case 'DELETE':
        return 'bg-red-100 text-red-800';
      case 'PATCH':
        return 'bg-purple-100 text-purple-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const handleSave = () => {
    try {
      const headers = headersText.trim() ? JSON.parse(headersText) : {};
      const body = bodyText.trim() ? JSON.parse(bodyText) : {};
      
      data.config = { 
        ...data.config,
        url,
        method,
        headers,
        body,
        timeout
      };
      setIsDialogOpen(false);
    } catch (e) {
      console.error('Invalid JSON:', e);
      alert('JSON格式无效，请检查头部或正文的JSON格式');
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
        className={`relative rounded-lg border ${selected ? 'border-indigo-600 ring-2 ring-primary' : 'border-indigo-500'} bg-white px-4 py-2 shadow-md cursor-pointer`}
        onClick={handleNodeClick}
      >
        <div className="flex items-center">
          <div className="mr-2 h-3 w-3 rounded-full bg-indigo-500"></div>
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
        
        {data.config?.url && (
          <div className="mt-1 text-xs text-gray-500">
            <div className="flex items-center">
              <span className={`mr-1 px-1 rounded text-xs ${getMethodColor(method)}`}>
                {data.config.method}
              </span>
              <span className="truncate">{data.config.url}</span>
            </div>
            {data.config.headers && Object.keys(data.config.headers).length > 0 && (
              <div className="mt-1 text-xs text-gray-400">
                Headers: {Object.keys(data.config.headers).length} 项
              </div>
            )}
          </div>
        )}
        
        {/* 输入连接点 */}
        <Handle
          type="target"
          position={Position.Top}
          id="in"
          className="h-3 w-3 bg-indigo-500"
        />
        
        {/* 输出连接点 */}
        <Handle
          type="source"
          position={Position.Bottom}
          id="out"
          className="h-3 w-3 bg-indigo-500"
        />
      </div>
      
      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent className="sm:max-w-[600px]">
          <DialogHeader>
            <DialogTitle>配置API请求</DialogTitle>
          </DialogHeader>
          
          <div className="py-4">
            <div className="mb-4 flex gap-4 items-center">
              <div className="w-24">
                <Label htmlFor="method">请求方法</Label>
                <Select
                  value={method}
                  onValueChange={setMethod}
                >
                  <SelectTrigger id="method">
                    <SelectValue placeholder="方法" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="GET">GET</SelectItem>
                    <SelectItem value="POST">POST</SelectItem>
                    <SelectItem value="PUT">PUT</SelectItem>
                    <SelectItem value="DELETE">DELETE</SelectItem>
                    <SelectItem value="PATCH">PATCH</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              
              <div className="flex-1">
                <Label htmlFor="url">请求地址</Label>
                <Input
                  id="url"
                  value={url}
                  onChange={(e) => setUrl(e.target.value)}
                  placeholder="https://api.example.com"
                />
              </div>
              
              <div className="w-24">
                <Label htmlFor="timeout">超时(ms)</Label>
                <Input
                  id="timeout"
                  type="number"
                  value={timeout}
                  onChange={(e) => setTimeout(Number(e.target.value))}
                />
              </div>
            </div>
            
            <Tabs defaultValue="headers">
              <TabsList className="w-full">
                <TabsTrigger value="headers" className="flex-1">请求头</TabsTrigger>
                <TabsTrigger value="body" className="flex-1">请求体</TabsTrigger>
              </TabsList>
              
              <TabsContent value="headers" className="mt-4">
                <Label htmlFor="headers">Headers (JSON格式)</Label>
                <Textarea
                  id="headers"
                  value={headersText}
                  onChange={(e) => setHeadersText(e.target.value)}
                  className="mt-1 h-40 font-mono text-xs"
                  placeholder='{"Content-Type": "application/json", "Authorization": "Bearer ${token}"}'
                />
                <p className="text-xs text-muted-foreground mt-1">
                  提示: 可以使用 {"${变量名}"} 语法引用上下文变量
                </p>
              </TabsContent>
              
              <TabsContent value="body" className="mt-4">
                <Label htmlFor="body">Body (JSON格式)</Label>
                <Textarea
                  id="body"
                  value={bodyText}
                  onChange={(e) => setBodyText(e.target.value)}
                  className="mt-1 h-40 font-mono text-xs"
                  placeholder='{"key": "value", "data": "${data}"}'
                />
                <p className="text-xs text-muted-foreground mt-1">
                  提示: 可以使用 {"${变量名}"} 语法引用上下文变量
                </p>
              </TabsContent>
            </Tabs>
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

ApiNode.displayName = 'ApiNode';

export default ApiNode; 