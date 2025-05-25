import React, { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { PlusIcon, Pencil1Icon, TrashIcon, SwitchIcon } from '@radix-ui/react-icons';
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
  CardFooter,
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

// 定义租户类型
interface Tenant {
  id: string;
  name: string;
  description?: string;
  configuration?: Record<string, any>;
  createdAt: string;
  updatedAt: string;
}

// 定义表单验证模式
const tenantFormSchema = z.object({
  name: z.string().min(2, {
    message: '租户名称至少需要 2 个字符',
  }),
  description: z.string().optional(),
  configuration: z.string().refine(
    (value) => {
      try {
        if (value) {
          JSON.parse(value);
        }
        return true;
      } catch (error) {
        return false;
      }
    },
    {
      message: '配置必须是有效的 JSON 格式',
    }
  ),
});

export function TenantManager() {
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [currentTenant, setCurrentTenant] = useState<Tenant | null>(null);
  const [loading, setLoading] = useState(false);
  const [editingTenant, setEditingTenant] = useState<Tenant | null>(null);
  const [isDialogOpen, setIsDialogOpen] = useState(false);

  // 初始化表单
  const form = useForm<z.infer<typeof tenantFormSchema>>({
    resolver: zodResolver(tenantFormSchema),
    defaultValues: {
      name: '',
      description: '',
      configuration: '{}',
    },
  });

  // 加载租户
  useEffect(() => {
    fetchTenants();
    fetchCurrentTenant();
  }, []);

  // 当编辑租户时，更新表单值
  useEffect(() => {
    if (editingTenant) {
      form.reset({
        name: editingTenant.name,
        description: editingTenant.description || '',
        configuration: editingTenant.configuration
          ? JSON.stringify(editingTenant.configuration, null, 2)
          : '{}',
      });
    } else {
      form.reset({
        name: '',
        description: '',
        configuration: '{}',
      });
    }
  }, [editingTenant, form]);

  // 获取租户列表
  const fetchTenants = async () => {
    setLoading(true);
    try {
      const response = await axios.get('/api/v1/tenants');
      if (response.data.code === 0) {
        setTenants(response.data.data);
      } else {
        toast({
          title: '获取租户失败',
          description: response.data.message || '请稍后重试',
          variant: 'destructive',
        });
      }
    } catch (error) {
      console.error('获取租户失败:', error);
      toast({
        title: '获取租户失败',
        description: '请检查网络连接',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  // 获取当前租户
  const fetchCurrentTenant = async () => {
    try {
      const response = await axios.get('/api/v1/tenants/current');
      if (response.data.code === 0) {
        setCurrentTenant(response.data.data);
      }
    } catch (error) {
      console.error('获取当前租户失败:', error);
    }
  };

  // 保存租户
  const onSubmit = async (values: z.infer<typeof tenantFormSchema>) => {
    setLoading(true);
    try {
      // 解析配置 JSON
      const configuration = values.configuration ? JSON.parse(values.configuration) : {};
      
      let response;
      if (editingTenant) {
        // 更新现有租户
        response = await axios.put(`/api/v1/tenants/${editingTenant.id}`, {
          ...values,
          id: editingTenant.id,
          configuration,
        });
      } else {
        // 创建新租户
        response = await axios.post('/api/v1/tenants', {
          ...values,
          configuration,
        });
      }

      if (response.data.code === 0) {
        toast({
          title: editingTenant ? '租户更新成功' : '租户创建成功',
          description: `租户 "${values.name}" ${editingTenant ? '已更新' : '已创建'}`,
        });
        fetchTenants();
        setIsDialogOpen(false);
        setEditingTenant(null);
      } else {
        toast({
          title: editingTenant ? '更新租户失败' : '创建租户失败',
          description: response.data.message || '请稍后重试',
          variant: 'destructive',
        });
      }
    } catch (error) {
      console.error(editingTenant ? '更新租户失败:' : '创建租户失败:', error);
      toast({
        title: editingTenant ? '更新租户失败' : '创建租户失败',
        description: '请检查网络连接或配置格式',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  // 删除租户
  const deleteTenant = async (tenantId: string) => {
    setLoading(true);
    try {
      const response = await axios.delete(`/api/v1/tenants/${tenantId}`);
      if (response.data.code === 0) {
        toast({
          title: '租户删除成功',
          description: '租户已成功删除',
        });
        fetchTenants();
      } else {
        toast({
          title: '删除租户失败',
          description: response.data.message || '请稍后重试',
          variant: 'destructive',
        });
      }
    } catch (error) {
      console.error('删除租户失败:', error);
      toast({
        title: '删除租户失败',
        description: '请检查网络连接',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  // 切换租户
  const switchTenant = async (tenantId: string) => {
    setLoading(true);
    try {
      const response = await axios.post(`/api/v1/tenants/switch/${tenantId}`);
      if (response.data.code === 0) {
        toast({
          title: '切换租户成功',
          description: `已切换到租户 "${response.data.data.name}"`,
        });
        setCurrentTenant(response.data.data);
        
        // 刷新页面以应用新租户的配置
        setTimeout(() => {
          window.location.reload();
        }, 1000);
      } else {
        toast({
          title: '切换租户失败',
          description: response.data.message || '请稍后重试',
          variant: 'destructive',
        });
      }
    } catch (error) {
      console.error('切换租户失败:', error);
      toast({
        title: '切换租户失败',
        description: '请检查网络连接',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  // 编辑租户
  const handleEditTenant = (tenant: Tenant) => {
    setEditingTenant(tenant);
    setIsDialogOpen(true);
  };

  // 创建新租户
  const handleCreateTenant = () => {
    setEditingTenant(null);
    form.reset({
      name: '',
      description: '',
      configuration: '{}',
    });
    setIsDialogOpen(true);
  };

  // 格式化日期
  const formatDate = (dateString: string) => {
    return format(new Date(dateString), 'yyyy-MM-dd HH:mm:ss');
  };

  return (
    <div className="container mx-auto py-6">
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div>
            <CardTitle>租户管理</CardTitle>
            <CardDescription>
              管理系统中的租户。每个租户可以有独立的用户、角色和资源。
            </CardDescription>
          </div>
          <Button onClick={handleCreateTenant}>
            <PlusIcon className="mr-2 h-4 w-4" />
            创建租户
          </Button>
        </CardHeader>
        <CardContent>
          {currentTenant && (
            <div className="mb-6 p-4 bg-muted rounded-md">
              <div className="flex items-center space-x-4">
                <div className="flex-1">
                  <h3 className="text-lg font-medium">当前租户: {currentTenant.name}</h3>
                  <p className="text-sm text-muted-foreground">
                    {currentTenant.description || '无描述'}
                  </p>
                </div>
                <Badge>当前</Badge>
              </div>
            </div>
          )}

          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>ID</TableHead>
                <TableHead>名称</TableHead>
                <TableHead>描述</TableHead>
                <TableHead>创建时间</TableHead>
                <TableHead>更新时间</TableHead>
                <TableHead className="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {tenants.map((tenant) => (
                <TableRow key={tenant.id}>
                  <TableCell className="font-mono">{tenant.id}</TableCell>
                  <TableCell className="font-medium">{tenant.name}</TableCell>
                  <TableCell>{tenant.description || '-'}</TableCell>
                  <TableCell>{formatDate(tenant.createdAt)}</TableCell>
                  <TableCell>{formatDate(tenant.updatedAt)}</TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-2">
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => switchTenant(tenant.id)}
                        disabled={currentTenant?.id === tenant.id}
                        title="切换到此租户"
                      >
                        <SwitchIcon className="h-4 w-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => handleEditTenant(tenant)}
                        title="编辑租户"
                      >
                        <Pencil1Icon className="h-4 w-4" />
                      </Button>
                      <AlertDialog>
                        <AlertDialogTrigger asChild>
                          <Button
                            variant="ghost"
                            size="icon"
                            disabled={tenant.id === 'default'}
                            title="删除租户"
                          >
                            <TrashIcon className="h-4 w-4" />
                          </Button>
                        </AlertDialogTrigger>
                        <AlertDialogContent>
                          <AlertDialogHeader>
                            <AlertDialogTitle>确认删除</AlertDialogTitle>
                            <AlertDialogDescription>
                              您确定要删除租户 "{tenant.name}" 吗？此操作不可撤销，并且会删除与此租户关联的所有数据。
                            </AlertDialogDescription>
                          </AlertDialogHeader>
                          <AlertDialogFooter>
                            <AlertDialogCancel>取消</AlertDialogCancel>
                            <AlertDialogAction
                              onClick={() => deleteTenant(tenant.id)}
                            >
                              删除
                            </AlertDialogAction>
                          </AlertDialogFooter>
                        </AlertDialogContent>
                      </AlertDialog>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
              {tenants.length === 0 && (
                <TableRow>
                  <TableCell colSpan={6} className="text-center py-4">
                    {loading ? '加载中...' : '暂无租户数据'}
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      {/* 租户表单对话框 */}
      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent className="sm:max-w-[600px]">
          <DialogHeader>
            <DialogTitle>{editingTenant ? '编辑租户' : '创建租户'}</DialogTitle>
            <DialogDescription>
              {editingTenant
                ? '修改租户信息和配置。'
                : '创建新租户并设置配置。'}
            </DialogDescription>
          </DialogHeader>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
              <FormField
                control={form.control}
                name="name"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>租户名称</FormLabel>
                    <FormControl>
                      <Input placeholder="输入租户名称" {...field} />
                    </FormControl>
                    <FormDescription>
                      租户名称应该简洁明了，表明租户的主要用途。
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
                    <FormLabel>租户描述</FormLabel>
                    <FormControl>
                      <Textarea
                        placeholder="输入租户描述"
                        {...field}
                        value={field.value || ''}
                      />
                    </FormControl>
                    <FormDescription>
                      详细描述这个租户的用途和特点。
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="configuration"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>配置 (JSON)</FormLabel>
                    <FormControl>
                      <Textarea
                        placeholder="输入 JSON 格式的配置"
                        className="font-mono h-40"
                        {...field}
                      />
                    </FormControl>
                    <FormDescription>
                      租户特定的配置，必须是有效的 JSON 格式。
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <DialogFooter>
                <Button type="submit" disabled={loading}>
                  {loading ? '保存中...' : editingTenant ? '更新租户' : '创建租户'}
                </Button>
              </DialogFooter>
            </form>
          </Form>
        </DialogContent>
      </Dialog>
    </div>
  );
}

export default TenantManager;
