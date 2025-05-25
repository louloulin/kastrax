import React, { useState, useEffect } from 'react';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle
} from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Plus, Trash, FileJson, FileCode, Save, RefreshCw } from 'lucide-react';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow
} from '@/components/ui/table';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { ResourceGroupType } from '@/lib/config/resource.config';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { AlertCircle } from 'lucide-react';

// 资源类型接口
interface ResourceTypeDefinition {
  code: string;
  name: string;
  group: ResourceGroupType;
  componentType: string;
  schema: {
    type: 'json' | 'yaml';
    content: string;
  };
  properties?: Record<string, any>; // 解析后的属性
}

// 验证JSON Schema字符串是否有效
const isValidJsonSchema = (schema: string): boolean => {
  try {
    const parsed = JSON.parse(schema);
    // 简单验证是否具有基本的JSON Schema结构
    return parsed && typeof parsed === 'object' && 
           (parsed.type === 'object' || parsed.properties);
  } catch (e) {
    return false;
  }
};

// 验证YAML Schema字符串是否有效
const isValidYamlSchema = (schema: string): boolean => {
  // 这里需要实际的YAML解析逻辑
  // 由于需要yaml库，先做简单验证
  return schema.includes('properties:') || schema.includes('type:');
};

export default function ResourceTypeManager() {
  const [resourceTypes, setResourceTypes] = useState<ResourceTypeDefinition[]>([]);
  const [activeTab, setActiveTab] = useState('list');
  const [selectedType, setSelectedType] = useState<ResourceTypeDefinition | null>(null);
  const [newType, setNewType] = useState<ResourceTypeDefinition>({
    code: '',
    name: '',
    group: ResourceGroupType.DATABASE,
    componentType: 'DynamicProperties',
    schema: {
      type: 'json',
      content: '{\n  "type": "object",\n  "properties": {\n    "url": {\n      "type": "string",\n      "title": "URL地址"\n    }\n  },\n  "required": ["url"]\n}'
    }
  });
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  // 加载资源类型列表
  useEffect(() => {
    // 这里应该是从API获取资源类型列表
    // 为了演示，使用模拟数据
    setResourceTypes([
      {
        code: 'MONGODB',
        name: 'MongoDB数据库',
        group: ResourceGroupType.DATABASE,
        componentType: 'DynamicProperties',
        schema: {
          type: 'json',
          content: '{\n  "type": "object",\n  "properties": {\n    "url": {\n      "type": "string",\n      "title": "连接串"\n    },\n    "database": {\n      "type": "string",\n      "title": "数据库名"\n    }\n  },\n  "required": ["url", "database"]\n}'
        }
      },
      {
        code: 'ELASTICSEARCH',
        name: 'Elasticsearch',
        group: ResourceGroupType.DATABASE,
        componentType: 'DynamicProperties',
        schema: {
          type: 'json',
          content: '{\n  "type": "object",\n  "properties": {\n    "nodes": {\n      "type": "array",\n      "title": "节点列表",\n      "items": {\n        "type": "string"\n      }\n    },\n    "index": {\n      "type": "string",\n      "title": "索引名称"\n    }\n  },\n  "required": ["nodes"]\n}'
        }
      }
    ]);
  }, []);

  // 处理资源类型选择
  const handleTypeSelect = (type: ResourceTypeDefinition) => {
    setSelectedType({...type});
    setActiveTab('edit');
  };

  // 新建资源类型
  const handleAddType = () => {
    setSelectedType(null);
    setNewType({
      code: '',
      name: '',
      group: ResourceGroupType.DATABASE,
      componentType: 'DynamicProperties',
      schema: {
        type: 'json',
        content: '{\n  "type": "object",\n  "properties": {\n    "property1": {\n      "type": "string",\n      "title": "属性1"\n    }\n  },\n  "required": []\n}'
      }
    });
    setActiveTab('create');
  };

  // 保存资源类型
  const handleSaveType = async () => {
    setError(null);
    
    // 验证必填字段
    if (!newType.code || !newType.name) {
      setError('请填写资源类型编码和名称');
      return;
    }
    
    // 验证Schema
    const isValid = newType.schema.type === 'json' 
      ? isValidJsonSchema(newType.schema.content)
      : isValidYamlSchema(newType.schema.content);
    
    if (!isValid) {
      setError(`无效的${newType.schema.type.toUpperCase()}格式`);
      return;
    }
    
    setLoading(true);
    
    try {
      // 这里应该调用API保存资源类型
      // 模拟保存成功
      setTimeout(() => {
        setResourceTypes(prev => [...prev, {...newType}]);
        setActiveTab('list');
        setLoading(false);
      }, 1000);
    } catch (error) {
      setError('保存失败，请稍后重试');
      setLoading(false);
    }
  };

  // 保存编辑的资源类型
  const handleUpdateType = async () => {
    if (!selectedType) return;
    
    setError(null);
    
    // 验证必填字段
    if (!selectedType.code || !selectedType.name) {
      setError('请填写资源类型编码和名称');
      return;
    }
    
    // 验证Schema
    const isValid = selectedType.schema.type === 'json' 
      ? isValidJsonSchema(selectedType.schema.content)
      : isValidYamlSchema(selectedType.schema.content);
    
    if (!isValid) {
      setError(`无效的${selectedType.schema.type.toUpperCase()}格式`);
      return;
    }
    
    setLoading(true);
    
    try {
      // 这里应该调用API更新资源类型
      // 模拟更新成功
      setTimeout(() => {
        setResourceTypes(prev => 
          prev.map(type => type.code === selectedType.code ? selectedType : type)
        );
        setActiveTab('list');
        setLoading(false);
      }, 1000);
    } catch (error) {
      setError('更新失败，请稍后重试');
      setLoading(false);
    }
  };

  // 删除资源类型
  const handleDeleteType = async (code: string) => {
    if (!window.confirm('确定要删除此资源类型吗？')) return;
    
    setLoading(true);
    
    try {
      // 这里应该调用API删除资源类型
      // 模拟删除成功
      setTimeout(() => {
        setResourceTypes(prev => prev.filter(type => type.code !== code));
        setLoading(false);
      }, 1000);
    } catch (error) {
      setError('删除失败，请稍后重试');
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList>
          <TabsTrigger value="list">资源类型列表</TabsTrigger>
          <TabsTrigger value="create" disabled={activeTab === 'edit'}>新建资源类型</TabsTrigger>
          <TabsTrigger value="edit" disabled={!selectedType}>编辑资源类型</TabsTrigger>
        </TabsList>
        
        <TabsContent value="list" className="space-y-4">
          <div className="flex justify-between items-center">
            <h2 className="text-lg font-semibold">自定义资源类型管理</h2>
            <Button onClick={handleAddType}>
              <Plus className="w-4 h-4 mr-2" />
              添加资源类型
            </Button>
          </div>
          
          <Card>
            <CardContent className="pt-6">
              <ScrollArea className="h-[400px]">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>类型编码</TableHead>
                      <TableHead>类型名称</TableHead>
                      <TableHead>分组</TableHead>
                      <TableHead>组件类型</TableHead>
                      <TableHead>Schema类型</TableHead>
                      <TableHead>操作</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {resourceTypes.map(type => (
                      <TableRow key={type.code}>
                        <TableCell className="font-mono">{type.code}</TableCell>
                        <TableCell>{type.name}</TableCell>
                        <TableCell>{type.group}</TableCell>
                        <TableCell>{type.componentType}</TableCell>
                        <TableCell>
                          {type.schema.type === 'json' ? 
                            <FileJson className="w-4 h-4 inline mr-1" /> : 
                            <FileCode className="w-4 h-4 inline mr-1" />
                          }
                          {type.schema.type.toUpperCase()}
                        </TableCell>
                        <TableCell>
                          <div className="flex space-x-2">
                            <Button 
                              variant="outline" 
                              size="sm" 
                              onClick={() => handleTypeSelect(type)}
                            >
                              编辑
                            </Button>
                            <Button 
                              variant="destructive" 
                              size="sm"
                              onClick={() => handleDeleteType(type.code)}
                            >
                              <Trash className="w-4 h-4" />
                            </Button>
                          </div>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </ScrollArea>
            </CardContent>
          </Card>
        </TabsContent>
        
        <TabsContent value="create" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>新建资源类型</CardTitle>
              <CardDescription>
                定义新的资源类型和属性结构
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              {error && (
                <Alert variant="destructive">
                  <AlertCircle className="h-4 w-4" />
                  <AlertTitle>错误</AlertTitle>
                  <AlertDescription>{error}</AlertDescription>
                </Alert>
              )}
              
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="type-code">类型编码</Label>
                  <Input 
                    id="type-code" 
                    placeholder="例如: MONGODB" 
                    value={newType.code}
                    onChange={e => setNewType({...newType, code: e.target.value.toUpperCase()})}
                  />
                </div>
                
                <div className="space-y-2">
                  <Label htmlFor="type-name">类型名称</Label>
                  <Input 
                    id="type-name" 
                    placeholder="例如: MongoDB数据库" 
                    value={newType.name}
                    onChange={e => setNewType({...newType, name: e.target.value})}
                  />
                </div>
                
                <div className="space-y-2">
                  <Label htmlFor="type-group">分组</Label>
                  <Select 
                    value={newType.group} 
                    onValueChange={value => setNewType({...newType, group: value as ResourceGroupType})}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="选择分组" />
                    </SelectTrigger>
                    <SelectContent>
                      {Object.values(ResourceGroupType).map(group => (
                        <SelectItem key={group} value={group}>
                          {group}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                
                <div className="space-y-2">
                  <Label htmlFor="schema-type">Schema类型</Label>
                  <Select 
                    value={newType.schema.type} 
                    onValueChange={value => setNewType({
                      ...newType, 
                      schema: {...newType.schema, type: value as 'json' | 'yaml'}
                    })}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="选择Schema类型" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="json">JSON Schema</SelectItem>
                      <SelectItem value="yaml">YAML Schema</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>
              
              <div className="space-y-2">
                <Label htmlFor="schema-content">Schema定义</Label>
                <Textarea 
                  id="schema-content" 
                  className="font-mono h-[300px]" 
                  placeholder={newType.schema.type === 'json' ? 
                    '{"type": "object", "properties": {...}}' : 
                    'type: object\nproperties:\n  property1:\n    type: string'
                  }
                  value={newType.schema.content}
                  onChange={e => setNewType({
                    ...newType, 
                    schema: {...newType.schema, content: e.target.value}
                  })}
                />
              </div>
              
              <div className="flex justify-end space-x-2">
                <Button 
                  variant="outline" 
                  onClick={() => setActiveTab('list')}
                >
                  取消
                </Button>
                <Button 
                  onClick={handleSaveType}
                  disabled={loading}
                >
                  {loading ? '保存中...' : '保存'}
                  {!loading && <Save className="ml-2 h-4 w-4" />}
                </Button>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
        
        <TabsContent value="edit" className="space-y-4">
          {selectedType && (
            <Card>
              <CardHeader>
                <CardTitle>编辑资源类型</CardTitle>
                <CardDescription>
                  修改现有资源类型的定义
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                {error && (
                  <Alert variant="destructive">
                    <AlertCircle className="h-4 w-4" />
                    <AlertTitle>错误</AlertTitle>
                    <AlertDescription>{error}</AlertDescription>
                  </Alert>
                )}
                
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="edit-type-code">类型编码</Label>
                    <Input 
                      id="edit-type-code" 
                      placeholder="例如: MONGODB" 
                      value={selectedType.code}
                      onChange={e => setSelectedType({...selectedType, code: e.target.value.toUpperCase()})}
                      disabled // 不允许修改编码
                    />
                  </div>
                  
                  <div className="space-y-2">
                    <Label htmlFor="edit-type-name">类型名称</Label>
                    <Input 
                      id="edit-type-name" 
                      placeholder="例如: MongoDB数据库" 
                      value={selectedType.name}
                      onChange={e => setSelectedType({...selectedType, name: e.target.value})}
                    />
                  </div>
                  
                  <div className="space-y-2">
                    <Label htmlFor="edit-type-group">分组</Label>
                    <Select 
                      value={selectedType.group} 
                      onValueChange={value => setSelectedType({...selectedType, group: value as ResourceGroupType})}
                    >
                      <SelectTrigger>
                        <SelectValue placeholder="选择分组" />
                      </SelectTrigger>
                      <SelectContent>
                        {Object.values(ResourceGroupType).map(group => (
                          <SelectItem key={group} value={group}>
                            {group}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                  
                  <div className="space-y-2">
                    <Label htmlFor="edit-schema-type">Schema类型</Label>
                    <Select 
                      value={selectedType.schema.type} 
                      onValueChange={value => setSelectedType({
                        ...selectedType, 
                        schema: {...selectedType.schema, type: value as 'json' | 'yaml'}
                      })}
                    >
                      <SelectTrigger>
                        <SelectValue placeholder="选择Schema类型" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="json">JSON Schema</SelectItem>
                        <SelectItem value="yaml">YAML Schema</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                </div>
                
                <div className="space-y-2">
                  <Label htmlFor="edit-schema-content">Schema定义</Label>
                  <Textarea 
                    id="edit-schema-content" 
                    className="font-mono h-[300px]" 
                    placeholder={selectedType.schema.type === 'json' ? 
                      '{"type": "object", "properties": {...}}' : 
                      'type: object\nproperties:\n  property1:\n    type: string'
                    }
                    value={selectedType.schema.content}
                    onChange={e => setSelectedType({
                      ...selectedType, 
                      schema: {...selectedType.schema, content: e.target.value}
                    })}
                  />
                </div>
                
                <div className="flex justify-end space-x-2">
                  <Button 
                    variant="outline" 
                    onClick={() => setActiveTab('list')}
                  >
                    取消
                  </Button>
                  <Button 
                    onClick={handleUpdateType}
                    disabled={loading}
                  >
                    {loading ? '更新中...' : '更新'}
                    {!loading && <RefreshCw className="ml-2 h-4 w-4" />}
                  </Button>
                </div>
              </CardContent>
            </Card>
          )}
        </TabsContent>
      </Tabs>
    </div>
  );
} 