import React, { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { PlusIcon, Pencil1Icon, TrashIcon } from '@radix-ui/react-icons';
import axios from 'axios';

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
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
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from '../ui/tabs';
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
import { Checkbox } from '../ui/checkbox';
import { Badge } from '../ui/badge';
import { ScrollArea } from '../ui/scroll-area';
import { Separator } from '../ui/separator';
import { toast } from '../ui/toast';

// 定义角色类型
interface Role {
  id: string;
  name: string;
  description: string;
  permissions: string[];
  system?: boolean;
  tenantId?: string;
  createdAt?: string;
  updatedAt?: string;
}

// 定义权限类型
interface Permission {
  id: string;
  name: string;
  description: string;
  expression: string;
  resource?: string;
  action?: string;
  system?: boolean;
}

// 定义表单验证模式
const roleFormSchema = z.object({
  name: z.string().min(2, {
    message: '角色名称至少需要 2 个字符',
  }),
  description: z.string().optional(),
  permissions: z.array(z.string()).min(1, {
    message: '请至少选择一个权限',
  }),
});

// 权限分类
const permissionCategories = [
  { id: 'resource', name: '资源管理' },
  { id: 'rule', name: '规则管理' },
  { id: 'workflow', name: '工作流管理' },
  { id: 'system', name: '系统管理' },
  { id: 'user', name: '用户管理' },
  { id: 'tenant', name: '租户管理' },
  { id: 'api', name: 'API 管理' },
];

export function RoleManager() {
  const [roles, setRoles] = useState<Role[]>([]);
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [loading, setLoading] = useState(false);
  const [editingRole, setEditingRole] = useState<Role | null>(null);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [activeTab, setActiveTab] = useState('roles');

  // 初始化表单
  const form = useForm<z.infer<typeof roleFormSchema>>({
    resolver: zodResolver(roleFormSchema),
    defaultValues: {
      name: '',
      description: '',
      permissions: [],
    },
  });

  // 加载角色和权限
  useEffect(() => {
    fetchRoles();
    fetchPermissions();
  }, []);

  // 当编辑角色时，更新表单值
  useEffect(() => {
    if (editingRole) {
      form.reset({
        name: editingRole.name,
        description: editingRole.description,
        permissions: editingRole.permissions,
      });
    } else {
      form.reset({
        name: '',
        description: '',
        permissions: [],
      });
    }
  }, [editingRole, form]);

  // 获取角色列表
  const fetchRoles = async () => {
    setLoading(true);
    try {
      const response = await axios.get('/api/v1/roles');
      if (response.data.code === 0) {
        setRoles(response.data.data);
      } else {
        toast({
          title: '获取角色失败',
          description: response.data.message || '请稍后重试',
          variant: 'destructive',
        });
      }
    } catch (error) {
      console.error('获取角色失败:', error);
      toast({
        title: '获取角色失败',
        description: '请检查网络连接',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  // 获取权限列表
  const fetchPermissions = async () => {
    try {
      const response = await axios.get('/api/v1/permissions');
      if (response.data.code === 0) {
        setPermissions(response.data.data);
      } else {
        toast({
          title: '获取权限失败',
          description: response.data.message || '请稍后重试',
          variant: 'destructive',
        });
      }
    } catch (error) {
      console.error('获取权限失败:', error);
      toast({
        title: '获取权限失败',
        description: '请检查网络连接',
        variant: 'destructive',
      });
    }
  };

  // 保存角色
  const onSubmit = async (values: z.infer<typeof roleFormSchema>) => {
    setLoading(true);
    try {
      let response;
      if (editingRole) {
        // 更新现有角色
        response = await axios.put(`/api/v1/roles/${editingRole.id}`, {
          ...values,
          id: editingRole.id,
        });
      } else {
        // 创建新角色
        response = await axios.post('/api/v1/roles', values);
      }

      if (response.data.code === 0) {
        toast({
          title: editingRole ? '角色更新成功' : '角色创建成功',
          description: `角色 "${values.name}" ${editingRole ? '已更新' : '已创建'}`,
        });
        fetchRoles();
        setIsDialogOpen(false);
        setEditingRole(null);
      } else {
        toast({
          title: editingRole ? '更新角色失败' : '创建角色失败',
          description: response.data.message || '请稍后重试',
          variant: 'destructive',
        });
      }
    } catch (error) {
      console.error(editingRole ? '更新角色失败:' : '创建角色失败:', error);
      toast({
        title: editingRole ? '更新角色失败' : '创建角色失败',
        description: '请检查网络连接',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  // 删除角色
  const deleteRole = async (roleId: string) => {
    setLoading(true);
    try {
      const response = await axios.delete(`/api/v1/roles/${roleId}`);
      if (response.data.code === 0) {
        toast({
          title: '角色删除成功',
          description: '角色已成功删除',
        });
        fetchRoles();
      } else {
        toast({
          title: '删除角色失败',
          description: response.data.message || '请稍后重试',
          variant: 'destructive',
        });
      }
    } catch (error) {
      console.error('删除角色失败:', error);
      toast({
        title: '删除角色失败',
        description: '请检查网络连接',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  // 编辑角色
  const handleEditRole = (role: Role) => {
    setEditingRole(role);
    setIsDialogOpen(true);
  };

  // 创建新角色
  const handleCreateRole = () => {
    setEditingRole(null);
    form.reset({
      name: '',
      description: '',
      permissions: [],
    });
    setIsDialogOpen(true);
  };

  // 按分类获取权限
  const getPermissionsByCategory = (categoryId: string) => {
    return permissions.filter(p => {
      if (categoryId === 'resource') {
        return p.expression.startsWith('resource:');
      } else if (categoryId === 'rule') {
        return p.expression.startsWith('rule:');
      } else if (categoryId === 'workflow') {
        return p.expression.startsWith('workflow:');
      } else if (categoryId === 'system') {
        return p.expression.startsWith('system:');
      } else if (categoryId === 'user') {
        return p.expression.startsWith('user:');
      } else if (categoryId === 'tenant') {
        return p.expression.startsWith('tenant:');
      } else if (categoryId === 'api') {
        return p.expression.startsWith('api:');
      }
      return false;
    });
  };

  return (
    <div className="container mx-auto py-6">
      <Tabs defaultValue="roles" value={activeTab} onValueChange={setActiveTab}>
        <div className="flex justify-between items-center mb-4">
          <TabsList>
            <TabsTrigger value="roles">角色管理</TabsTrigger>
            <TabsTrigger value="permissions">权限管理</TabsTrigger>
          </TabsList>
          {activeTab === 'roles' && (
            <Button onClick={handleCreateRole}>
              <PlusIcon className="mr-2 h-4 w-4" />
              创建角色
            </Button>
          )}
        </div>

        <TabsContent value="roles">
          <Card>
            <CardHeader>
              <CardTitle>角色列表</CardTitle>
              <CardDescription>
                管理系统中的角色及其权限。系统角色不可编辑或删除。
              </CardDescription>
            </CardHeader>
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>角色名称</TableHead>
                    <TableHead>描述</TableHead>
                    <TableHead>权限</TableHead>
                    <TableHead>类型</TableHead>
                    <TableHead className="text-right">操作</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {roles.map((role) => (
                    <TableRow key={role.id}>
                      <TableCell className="font-medium">{role.name}</TableCell>
                      <TableCell>{role.description}</TableCell>
                      <TableCell>
                        <div className="flex flex-wrap gap-1">
                          {role.permissions.slice(0, 3).map((permission) => (
                            <Badge key={permission} variant="outline">
                              {permission}
                            </Badge>
                          ))}
                          {role.permissions.length > 3 && (
                            <Badge variant="outline">
                              +{role.permissions.length - 3}
                            </Badge>
                          )}
                        </div>
                      </TableCell>
                      <TableCell>
                        {role.system ? (
                          <Badge>系统</Badge>
                        ) : (
                          <Badge variant="outline">自定义</Badge>
                        )}
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex justify-end gap-2">
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => handleEditRole(role)}
                            disabled={role.system}
                          >
                            <Pencil1Icon className="h-4 w-4" />
                          </Button>
                          <AlertDialog>
                            <AlertDialogTrigger asChild>
                              <Button
                                variant="ghost"
                                size="icon"
                                disabled={role.system}
                              >
                                <TrashIcon className="h-4 w-4" />
                              </Button>
                            </AlertDialogTrigger>
                            <AlertDialogContent>
                              <AlertDialogHeader>
                                <AlertDialogTitle>确认删除</AlertDialogTitle>
                                <AlertDialogDescription>
                                  您确定要删除角色 "{role.name}" 吗？此操作不可撤销。
                                </AlertDialogDescription>
                              </AlertDialogHeader>
                              <AlertDialogFooter>
                                <AlertDialogCancel>取消</AlertDialogCancel>
                                <AlertDialogAction
                                  onClick={() => deleteRole(role.id)}
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
                  {roles.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={5} className="text-center py-4">
                        {loading ? '加载中...' : '暂无角色数据'}
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="permissions">
          <Card>
            <CardHeader>
              <CardTitle>权限列表</CardTitle>
              <CardDescription>
                系统中所有可用的权限。这些权限可以分配给角色。
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-6">
                {permissionCategories.map((category) => {
                  const categoryPermissions = getPermissionsByCategory(category.id);
                  if (categoryPermissions.length === 0) return null;
                  
                  return (
                    <div key={category.id} className="space-y-2">
                      <h3 className="text-lg font-medium">{category.name}</h3>
                      <Separator />
                      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                        {categoryPermissions.map((permission) => (
                          <Card key={permission.id}>
                            <CardHeader className="py-4">
                              <CardTitle className="text-sm font-medium">
                                {permission.name}
                              </CardTitle>
                            </CardHeader>
                            <CardContent className="py-2">
                              <p className="text-sm text-muted-foreground">
                                {permission.description}
                              </p>
                              <Badge className="mt-2" variant="outline">
                                {permission.expression}
                              </Badge>
                            </CardContent>
                          </Card>
                        ))}
                      </div>
                    </div>
                  );
                })}
                {permissions.length === 0 && (
                  <div className="text-center py-4">
                    {loading ? '加载中...' : '暂无权限数据'}
                  </div>
                )}
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* 角色表单对话框 */}
      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent className="sm:max-w-[600px]">
          <DialogHeader>
            <DialogTitle>{editingRole ? '编辑角色' : '创建角色'}</DialogTitle>
            <DialogDescription>
              {editingRole
                ? '修改角色信息和权限分配。'
                : '创建新角色并分配权限。'}
            </DialogDescription>
          </DialogHeader>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
              <FormField
                control={form.control}
                name="name"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>角色名称</FormLabel>
                    <FormControl>
                      <Input placeholder="输入角色名称" {...field} />
                    </FormControl>
                    <FormDescription>
                      角色名称应该简洁明了，表明角色的主要职责。
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
                    <FormLabel>角色描述</FormLabel>
                    <FormControl>
                      <Input placeholder="输入角色描述" {...field} />
                    </FormControl>
                    <FormDescription>
                      详细描述这个角色的职责和用途。
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="permissions"
                render={() => (
                  <FormItem>
                    <div className="mb-4">
                      <FormLabel>权限分配</FormLabel>
                      <FormDescription>
                        选择要分配给该角色的权限。
                      </FormDescription>
                    </div>
                    <ScrollArea className="h-[300px] rounded-md border p-4">
                      <div className="space-y-6">
                        {permissionCategories.map((category) => {
                          const categoryPermissions = getPermissionsByCategory(category.id);
                          if (categoryPermissions.length === 0) return null;
                          
                          return (
                            <div key={category.id} className="space-y-2">
                              <h4 className="font-medium">{category.name}</h4>
                              <Separator />
                              <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                                {categoryPermissions.map((permission) => (
                                  <FormField
                                    key={permission.id}
                                    control={form.control}
                                    name="permissions"
                                    render={({ field }) => {
                                      return (
                                        <FormItem
                                          key={permission.id}
                                          className="flex flex-row items-start space-x-3 space-y-0"
                                        >
                                          <FormControl>
                                            <Checkbox
                                              checked={field.value?.includes(permission.expression)}
                                              onCheckedChange={(checked) => {
                                                return checked
                                                  ? field.onChange([...field.value, permission.expression])
                                                  : field.onChange(
                                                      field.value?.filter(
                                                        (value) => value !== permission.expression
                                                      )
                                                    );
                                              }}
                                            />
                                          </FormControl>
                                          <div className="space-y-1 leading-none">
                                            <FormLabel className="text-sm font-normal">
                                              {permission.name}
                                            </FormLabel>
                                            <p className="text-xs text-muted-foreground">
                                              {permission.expression}
                                            </p>
                                          </div>
                                        </FormItem>
                                      );
                                    }}
                                  />
                                ))}
                              </div>
                            </div>
                          );
                        })}
                      </div>
                    </ScrollArea>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <DialogFooter>
                <Button type="submit" disabled={loading}>
                  {loading ? '保存中...' : editingRole ? '更新角色' : '创建角色'}
                </Button>
              </DialogFooter>
            </form>
          </Form>
        </DialogContent>
      </Dialog>
    </div>
  );
}

export default RoleManager;
