import React, { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { PlusIcon, CopyIcon, TrashIcon, CheckIcon } from '@radix-ui/react-icons';
import axios from 'axios';
import { format } from 'date-fns';

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../ui/dialog';
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '../ui/form';
import {
  Table,
  TableBody,
  TableCaption,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../ui/table';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '../ui/card';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from '../ui/alert-dialog';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Textarea } from '../ui/textarea';
import { Badge } from '../ui/badge';
import { toast } from '../ui/toast';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../ui/select';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '../ui/popover';
import { Calendar } from '../ui/calendar';

// 定义 API Key 类型
interface ApiKey {
  id: string;
  key?: string;
  name: string;
  description?: string;
  userId: string;
  scopes: string[];
  status: 'ACTIVE' | 'REVOKED';
  createdAt: string;
  expiresAt?: string;
}

// 定义表单验证模式
const apiKeyFormSchema = z.object({
  name: z.string().min(2, {
    message: 'API Key 名称至少需要 2 个字符',
  }),
  description: z.string().optional(),
  scopes: z.array(z.string()).min(1, {
    message: '请至少选择一个权限范围',
  }),
  expiresInDays: z.number().int().min(0).optional(),
});

// 可用的权限范围
const availableScopes = [
  { value: 'resource:read', label: '资源读取' },
  { value: 'resource:write', label: '资源写入' },
  { value: 'resource:execute', label: '资源执行' },
  { value: 'rule:read', label: '规则读取' },
  { value: 'rule:write', label: '规则写入' },
  { value: 'workflow:read', label: '工作流读取' },
  { value: 'workflow:write', label: '工作流写入' },
  { value: 'workflow:execute', label: '工作流执行' },
  { value: 'all', label: '所有权限' },
];

// 过期时间选项
const expirationOptions = [
  { value: 7, label: '7 天' },
  { value: 30, label: '30 天' },
  { value: 90, label: '90 天' },
  { value: 180, label: '180 天' },
  { value: 365, label: '1 年' },
  { value: 0, label: '永不过期' },
];

export function ApiKeyManager() {
  const [apiKeys, setApiKeys] = useState<ApiKey[]>([]);
  const [loading, setLoading] = useState(false);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [newKeyDialogOpen, setNewKeyDialogOpen] = useState(false);
  const [newKey, setNewKey] = useState<ApiKey | null>(null);
  const [copiedKey, setCopiedKey] = useState(false);

  // 初始化表单
  const form = useForm<z.infer<typeof apiKeyFormSchema>>({
    resolver: zodResolver(apiKeyFormSchema),
    defaultValues: {
      name: '',
      description: '',
      scopes: [],
      expiresInDays: 30,
    },
  });

  // 加载 API Keys
  useEffect(() => {
    fetchApiKeys();
  }, []);

  // 获取 API Keys 列表
  const fetchApiKeys = async () => {
    setLoading(true);
    try {
      const response = await axios.get('/api/v1/apikeys');
      if (response.data.code === 0) {
        setApiKeys(response.data.data);
      } else {
        toast({
          title: '获取 API Keys 失败',
          description: response.data.message || '请稍后重试',
          variant: 'destructive',
        });
      }
    } catch (error) {
      console.error('获取 API Keys 失败:', error);
      toast({
        title: '获取 API Keys 失败',
        description: '请检查网络连接',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  // 创建 API Key
  const onSubmit = async (values: z.infer<typeof apiKeyFormSchema>) => {
    setLoading(true);
    try {
      const response = await axios.post('/api/v1/apikeys', values);
      if (response.data.code === 0) {
        // 显示新创建的 API Key
        setNewKey(response.data.data);
        setNewKeyDialogOpen(true);
        setIsDialogOpen(false);
        form.reset();
        fetchApiKeys();
      } else {
        toast({
          title: '创建 API Key 失败',
          description: response.data.message || '请稍后重试',
          variant: 'destructive',
        });
      }
    } catch (error) {
      console.error('创建 API Key 失败:', error);
      toast({
        title: '创建 API Key 失败',
        description: '请检查网络连接',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  // 撤销 API Key
  const revokeApiKey = async (keyId: string) => {
    setLoading(true);
    try {
      const response = await axios.delete(`/api/v1/apikeys/${keyId}`);
      if (response.data.code === 0) {
        toast({
          title: 'API Key 已撤销',
          description: 'API Key 已成功撤销',
        });
        fetchApiKeys();
      } else {
        toast({
          title: '撤销 API Key 失败',
          description: response.data.message || '请稍后重试',
          variant: 'destructive',
        });
      }
    } catch (error) {
      console.error('撤销 API Key 失败:', error);
      toast({
        title: '撤销 API Key 失败',
        description: '请检查网络连接',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  // 复制 API Key 到剪贴板
  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text).then(
      () => {
        setCopiedKey(true);
        setTimeout(() => setCopiedKey(false), 2000);
        toast({
          title: '已复制',
          description: 'API Key 已复制到剪贴板',
        });
      },
      () => {
        toast({
          title: '复制失败',
          description: '无法复制到剪贴板',
          variant: 'destructive',
        });
      }
    );
  };

  // 格式化日期
  const formatDate = (dateString?: string) => {
    if (!dateString) return '永不过期';
    return format(new Date(dateString), 'yyyy-MM-dd HH:mm:ss');
  };

  return (
    <div className="container mx-auto py-6">
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div>
            <CardTitle>API Keys</CardTitle>
            <CardDescription>
              管理您的 API Keys，用于程序化访问 API。
            </CardDescription>
          </div>
          <Button onClick={() => setIsDialogOpen(true)}>
            <PlusIcon className="mr-2 h-4 w-4" />
            创建 API Key
          </Button>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>名称</TableHead>
                <TableHead>描述</TableHead>
                <TableHead>权限范围</TableHead>
                <TableHead>创建时间</TableHead>
                <TableHead>过期时间</TableHead>
                <TableHead>状态</TableHead>
                <TableHead className="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {apiKeys.map((apiKey) => (
                <TableRow key={apiKey.id}>
                  <TableCell className="font-medium">{apiKey.name}</TableCell>
                  <TableCell>{apiKey.description || '-'}</TableCell>
                  <TableCell>
                    <div className="flex flex-wrap gap-1">
                      {apiKey.scopes.slice(0, 3).map((scope) => (
                        <Badge key={scope} variant="outline">
                          {scope}
                        </Badge>
                      ))}
                      {apiKey.scopes.length > 3 && (
                        <Badge variant="outline">
                          +{apiKey.scopes.length - 3}
                        </Badge>
                      )}
                    </div>
                  </TableCell>
                  <TableCell>{formatDate(apiKey.createdAt)}</TableCell>
                  <TableCell>{formatDate(apiKey.expiresAt)}</TableCell>
                  <TableCell>
                    {apiKey.status === 'ACTIVE' ? (
                      <Badge variant="success">活跃</Badge>
                    ) : (
                      <Badge variant="destructive">已撤销</Badge>
                    )}
                  </TableCell>
                  <TableCell className="text-right">
                    <AlertDialog>
                      <AlertDialogTrigger asChild>
                        <Button
                          variant="ghost"
                          size="icon"
                          disabled={apiKey.status !== 'ACTIVE'}
                        >
                          <TrashIcon className="h-4 w-4" />
                        </Button>
                      </AlertDialogTrigger>
                      <AlertDialogContent>
                        <AlertDialogHeader>
                          <AlertDialogTitle>确认撤销</AlertDialogTitle>
                          <AlertDialogDescription>
                            您确定要撤销 API Key "{apiKey.name}" 吗？此操作不可撤销。
                          </AlertDialogDescription>
                        </AlertDialogHeader>
                        <AlertDialogFooter>
                          <AlertDialogCancel>取消</AlertDialogCancel>
                          <AlertDialogAction
                            onClick={() => revokeApiKey(apiKey.id)}
                          >
                            撤销
                          </AlertDialogAction>
                        </AlertDialogFooter>
                      </AlertDialogContent>
                    </AlertDialog>
                  </TableCell>
                </TableRow>
              ))}
              {apiKeys.length === 0 && (
                <TableRow>
                  <TableCell colSpan={7} className="text-center py-4">
                    {loading ? '加载中...' : '暂无 API Keys'}
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      {/* 创建 API Key 对话框 */}
      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent className="sm:max-w-[500px]">
          <DialogHeader>
            <DialogTitle>创建 API Key</DialogTitle>
            <DialogDescription>
              创建一个新的 API Key 用于程序化访问 API。
            </DialogDescription>
          </DialogHeader>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
              <FormField
                control={form.control}
                name="name"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>名称</FormLabel>
                    <FormControl>
                      <Input placeholder="输入 API Key 名称" {...field} />
                    </FormControl>
                    <FormDescription>
                      为您的 API Key 指定一个有意义的名称，以便于识别。
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="description"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>描述</FormLabel>
                    <FormControl>
                      <Textarea
                        placeholder="输入 API Key 描述"
                        {...field}
                        value={field.value || ''}
                      />
                    </FormControl>
                    <FormDescription>
                      描述这个 API Key 的用途。
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="scopes"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>权限范围</FormLabel>
                    <FormControl>
                      <div className="grid grid-cols-2 gap-2">
                        {availableScopes.map((scope) => (
                          <div
                            key={scope.value}
                            className="flex items-center space-x-2"
                          >
                            <input
                              type="checkbox"
                              id={scope.value}
                              checked={field.value.includes(scope.value)}
                              onChange={(e) => {
                                const checked = e.target.checked;
                                if (checked) {
                                  field.onChange([...field.value, scope.value]);
                                } else {
                                  field.onChange(
                                    field.value.filter((value) => value !== scope.value)
                                  );
                                }
                              }}
                              className="h-4 w-4 rounded border-gray-300 text-primary focus:ring-primary"
                            />
                            <label
                              htmlFor={scope.value}
                              className="text-sm font-medium"
                            >
                              {scope.label}
                            </label>
                          </div>
                        ))}
                      </div>
                    </FormControl>
                    <FormDescription>
                      选择此 API Key 可以访问的权限范围。
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="expiresInDays"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>过期时间</FormLabel>
                    <Select
                      onValueChange={(value) => field.onChange(parseInt(value))}
                      defaultValue={field.value?.toString()}
                    >
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="选择过期时间" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {expirationOptions.map((option) => (
                          <SelectItem
                            key={option.value}
                            value={option.value.toString()}
                          >
                            {option.label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormDescription>
                      设置 API Key 的过期时间。过期后，API Key 将自动失效。
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <DialogFooter>
                <Button type="submit" disabled={loading}>
                  {loading ? '创建中...' : '创建 API Key'}
                </Button>
              </DialogFooter>
            </form>
          </Form>
        </DialogContent>
      </Dialog>

      {/* 新 API Key 对话框 */}
      <Dialog open={newKeyDialogOpen} onOpenChange={setNewKeyDialogOpen}>
        <DialogContent className="sm:max-w-[500px]">
          <DialogHeader>
            <DialogTitle>API Key 已创建</DialogTitle>
            <DialogDescription>
              请复制并安全保存您的 API Key。出于安全考虑，我们不会再次显示它。
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="p-4 bg-muted rounded-md">
              <div className="flex items-center space-x-2">
                <Input
                  value={newKey?.key}
                  readOnly
                  className="font-mono"
                />
                <Button
                  size="icon"
                  variant="outline"
                  onClick={() => newKey?.key && copyToClipboard(newKey.key)}
                >
                  {copiedKey ? (
                    <CheckIcon className="h-4 w-4" />
                  ) : (
                    <CopyIcon className="h-4 w-4" />
                  )}
                </Button>
              </div>
            </div>
            <div className="space-y-2">
              <div className="flex justify-between">
                <span className="text-sm font-medium">名称:</span>
                <span className="text-sm">{newKey?.name}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-sm font-medium">权限范围:</span>
                <span className="text-sm">{newKey?.scopes.join(', ')}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-sm font-medium">过期时间:</span>
                <span className="text-sm">
                  {newKey?.expiresAt ? formatDate(newKey.expiresAt) : '永不过期'}
                </span>
              </div>
            </div>
          </div>
          <DialogFooter>
            <Button onClick={() => setNewKeyDialogOpen(false)}>
              关闭
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

export default ApiKeyManager;
