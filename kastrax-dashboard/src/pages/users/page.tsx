import React, { useState, useEffect } from "react";
import SupabaseSidebar from "../../components/supabase-sidebar";
import TopNavigation from "../../components/top-navigation";
import DataTable from "../../components/data-table";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import { toast } from "../../components/ui/use-toast";
import {
  Plus,
  Search,
  Trash2,
  Edit,
  RefreshCw,
  Menu,
  Lock,
  UserCog,
  Shield,
} from "lucide-react";
import {
  getUserList,
  User,
  resetPassword,
  deleteUser
} from "../../lib/api/auth";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "../../components/ui/dialog";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "../../components/ui/dropdown-menu";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "../../components/ui/tooltip";
import UserForm from "./user-form";
import PasswordForm from "./password-form";
import PermissionForm from "./permission-form";

// Column definition for users
const userColumns = [
  { id: "username", header: "用户名", accessorKey: "username",
    cell: ({ row }: any) => {
      // 打印行数据以调试
      console.log('Username cell row data:', row);

      if (!row?.original) {
        console.warn('Row original is undefined');
        return '-';
      }

      // 打印用户名信息
      console.log('Username value:', row.original.username);

      return (
        <div className="flex items-center">
          <span>{row.original.username || '-'}</span>
          {row.original.system && (
            <span className="ml-2 px-1.5 py-0.5 text-xs bg-yellow-100 text-yellow-800 rounded">系统</span>
          )}
        </div>
      );
    }
  },
  { id: "email", header: "邮箱", accessorKey: "email" },
  { id: "description", header: "备注", accessorKey: "description",
    cell: ({ row }: any) => {
      if (!row?.original) return '-';
      return <span>{row.original.description || '-'}</span>;
    }
  },
  { id: "role", header: "角色", accessorKey: "role",
    cell: ({ row }: any) => {
      if (!row?.original) return '-';

      const roleLabels: Record<string, React.ReactNode> = {
        admin: <span className="text-purple-500 font-semibold">管理员</span>,
        user: <span className="text-blue-500 font-semibold">普通用户</span>,
        guest: <span className="text-gray-500 font-semibold">访客</span>,
      };

      const role = row.original.role || 'user';
      return roleLabels[role.toLowerCase()] || role;
    }
  },
  { id: "status", header: "状态", accessorKey: "status",
    cell: ({ row }: any) => {
      if (!row?.original) return '-';
      return (
        <div className="flex items-center">
          <div className={`h-2 w-2 rounded-full mr-2 ${(row.original.status ?? 0) === 1 ? 'bg-green-500' : 'bg-gray-500'}`}></div>
          <span>{(row.original.status ?? 0) === 1 ? '启用' : '禁用'}</span>
        </div>
      );
    }
  },
  { id: "lastLoginTime", header: "最后登录", accessorKey: "lastLoginTime",
    cell: ({ row }: any) => {
      if (!row?.original) return '-';
      return row.original.lastLoginTime ? new Date(row.original.lastLoginTime).toLocaleString() : '-';
    }
  },
  { id: "createTime", header: "创建时间", accessorKey: "createTime",
    cell: ({ row }: any) => {
      if (!row?.original) return '-';
      return row.original.createTime ? new Date(row.original.createTime).toLocaleString() : '-';
    }
  },
];

export default function UsersPage() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [showUserForm, setShowUserForm] = useState(false);
  const [showPasswordForm, setShowPasswordForm] = useState(false);
  const [showPermissionForm, setShowPermissionForm] = useState(false);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<string | null>(null);

  // Load users on component mount
  useEffect(() => {
    fetchUsers();
  }, []);

  // 处理用户数据，确保格式一致
  const normalizeUserData = (userData: any): User[] => {
    if (!userData) return [];

    // 打印原始数据，帮助调试
    console.log('Normalizing user data:', userData);

    // 如果是数组，处理每个用户对象
    if (Array.isArray(userData)) {
      return userData.map(user => {
        // 打印单个用户数据
        console.log('Processing user:', user);

        // 检查用户名字段
        if (!user.username && user.name) {
          console.log('Found name field instead of username:', user.name);
        }

        // 创建标准化的用户对象
        const normalizedUser = {
          ...user,
          // 确保必要字段存在
          id: user.userId || user.username || user.name || `user-${Math.random().toString(36).substr(2, 9)}`,
          userId: user.userId || user.username || user.name || '',
          // 如果没有username字段，尝试使用name字段
          username: user.username || user.name || '',
          email: user.email || '',
          role: user.role || 'user',
          status: typeof user.status === 'number' ? user.status : 0,
          description: user.description || '',
          permissions: Array.isArray(user.permissions) ? user.permissions : [],
          createTime: user.createTime || new Date().toISOString(),
          lastLoginTime: user.lastLoginTime || null,
          system: !!user.system
        };

        console.log('Normalized user:', normalizedUser);
        return normalizedUser;
      });
    }

    // 如果是对象且有data字段，尝试使用data
    if (userData.data && (Array.isArray(userData.data) || typeof userData.data === 'object')) {
      return normalizeUserData(userData.data);
    }

    // 如果是单个用户对象，包装为数组
    if (userData.username) {
      return normalizeUserData([userData]);
    }

    return [];
  };

  // Function to fetch users from API
  const fetchUsers = async () => {
    setLoading(true);
    try {
      const response = await getUserList();
      console.log("User list response:", response);

      // 处理不同的响应格式
      if (Array.isArray(response.data)) {
        console.log("Detected array response format");
        setUsers(normalizeUserData(response.data));
      } else if (response?.data?.success) {
        // 兼容RestResult格式
        console.log("Detected RestResult format");
        setUsers(normalizeUserData(response.data.data));
      } else if (response?.data) {
        // 尝试其他可能的格式
        console.log("Trying to parse unknown format");
        const normalizedUsers = normalizeUserData(response.data);
        setUsers(normalizedUsers);

        if (normalizedUsers.length > 0) {
          console.log("Successfully parsed user data", normalizedUsers);
        } else {
          console.warn("Could not parse user data from response", response.data);
          toast({
            title: "加载用户列表失败",
            description: "服务器返回的数据格式不正确",
            variant: "destructive"
          });
        }
      } else {
        console.warn("User list response not in expected format:", response);
        setUsers([]);
        toast({
          title: "加载用户列表失败",
          description: "服务器返回的数据为空",
          variant: "destructive"
        });
      }
    } catch (error) {
      console.error("Failed to fetch users:", error);
      setUsers([]);
      toast({
        title: "加载用户列表失败",
        description: `错误: ${(error as Error).message}`,
        variant: "destructive"
      });
    } finally {
      setLoading(false);
    }
  };

  // Function to handle user form submission
  const handleUserFormSubmit = () => {
    setShowUserForm(false);
    setSelectedUser(null);
    fetchUsers();
  };

  // Function to handle user edit
  const handleEditUser = (user: User) => {
    setSelectedUser(user);
    setShowUserForm(true);
  };

  // Function to handle password change
  const handlePasswordChange = (user: User) => {
    setSelectedUser(user);
    setShowPasswordForm(true);
  };

  // Function to handle permission management
  const handlePermissions = (user: User) => {
    setSelectedUser(user);
    setShowPermissionForm(true);
  };

  // Function to handle user reset password
  const handleResetPassword = async (username: string) => {
    try {
      await resetPassword(username);
      alert("密码已重置，请检查用户邮箱");
    } catch (error) {
      console.error("Failed to reset password:", error);
      alert("密码重置失败");
    }
  };

  // Function to handle user deletion
  const handleDeleteUser = async (username: string) => {
    try {
      await deleteUser(username);
      fetchUsers();
      setConfirmDelete(null);
    } catch (error) {
      console.error("Failed to delete user:", error);
      alert("删除用户失败");
    }
  };

  // Filter users based on search query
  const filteredUsers = users.filter(user => {
    // 防止空值引用
    const username = user.username || '';
    const email = user.email || '';
    const role = user.role || '';
    const description = user.description || '';

    return username.toLowerCase().includes(searchQuery.toLowerCase()) ||
           email.toLowerCase().includes(searchQuery.toLowerCase()) ||
           role.toLowerCase().includes(searchQuery.toLowerCase()) ||
           description.toLowerCase().includes(searchQuery.toLowerCase());
  });

  // 打印过滤后的用户数据，帮助调试
  console.log('Filtered users:', filteredUsers);

  return (
    <div className="flex min-h-screen bg-background">
      <SupabaseSidebar collapsed={sidebarCollapsed} setCollapsed={setSidebarCollapsed} />

      <div className="flex-1 flex flex-col">
        <TopNavigation>
          <Button variant="ghost" size="sm" onClick={() => setSidebarCollapsed(!sidebarCollapsed)}>
            <Menu size={16} />
          </Button>
        </TopNavigation>

        <div className="flex-1 p-6 overflow-auto">
          <div className="flex justify-between items-center mb-6">
            <div>
              <h1 className="text-2xl font-medium">用户管理</h1>
              <p className="text-muted-foreground">管理用户账号和权限</p>
            </div>

            <div className="flex gap-2">
              <Button variant="outline" size="sm" onClick={fetchUsers}>
                <RefreshCw size={16} className="mr-2" />
                刷新
              </Button>
              <Button size="sm" onClick={() => {
                setSelectedUser(null);
                setShowUserForm(true);
              }}>
                <Plus size={16} className="mr-2" />
                添加用户
              </Button>
            </div>
          </div>

          <div className="mb-6">
            <div className="relative">
              <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
              <Input
                type="search"
                placeholder="搜索用户名、邮箱或角色..."
                className="pl-8"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>
          </div>

          {/* 添加调试信息，在生产环境中可以移除 */}
          <div className="mb-4 p-4 bg-yellow-50 border border-yellow-200 rounded-md">
            <details>
              <summary className="font-medium cursor-pointer">调试信息 (点击展开)</summary>
              <div className="mt-2 overflow-auto max-h-60">
                <p className="text-sm font-medium">API返回的用户数量: {users.length}</p>
                <p className="text-sm font-medium">过滤后的用户数量: {filteredUsers.length}</p>

                {users.length > 0 && (
                  <>
                    <h3 className="text-sm font-medium mt-2">第一个用户对象:</h3>
                    <pre className="text-xs mt-1 p-2 bg-gray-100 rounded">
                      {JSON.stringify(users[0], null, 2)}
                    </pre>

                    <h3 className="text-sm font-medium mt-2">用户字段检查:</h3>
                    <ul className="text-xs mt-1 p-2 bg-gray-100 rounded">
                      <li>id: {users[0].id || '缺失'}</li>
                      <li>userId: {users[0].userId || '缺失'}</li>
                      <li>username: {users[0].username || '缺失'}</li>
                      <li>name: {(users[0] as any).name || '缺失'}</li>
                      <li>email: {users[0].email || '缺失'}</li>
                      <li>role: {users[0].role || '缺失'}</li>
                    </ul>
                  </>
                )}

                {filteredUsers.length > 0 && (
                  <>
                    <h3 className="text-sm font-medium mt-2">过滤后的第一个用户对象:</h3>
                    <pre className="text-xs mt-1 p-2 bg-gray-100 rounded">
                      {JSON.stringify(filteredUsers[0], null, 2)}
                    </pre>
                  </>
                )}
              </div>
            </details>
          </div>

          <DataTable
            title="用户列表"
            description="系统用户和权限"
            columns={userColumns}
            data={filteredUsers}
            isLoading={loading}
            actions={(row: User) => (
              <div className="flex gap-2">
                <TooltipProvider>
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button variant="ghost" size="sm" className="h-8 w-8 p-0">
                        <UserCog className="h-4 w-4" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end">
                      <DropdownMenuItem onClick={() => handleEditUser(row)}>
                        <Edit className="mr-2 h-4 w-4" />
                        编辑用户
                      </DropdownMenuItem>
                      <DropdownMenuItem onClick={() => handlePasswordChange(row)}>
                        <Lock className="mr-2 h-4 w-4" />
                        修改密码
                      </DropdownMenuItem>
                      <DropdownMenuItem onClick={() => handleResetPassword(row.username)}>
                        <Lock className="mr-2 h-4 w-4" />
                        重置密码
                      </DropdownMenuItem>
                      <DropdownMenuItem onClick={() => handlePermissions(row)}>
                        <Shield className="mr-2 h-4 w-4" />
                        管理权限
                      </DropdownMenuItem>
                      <DropdownMenuItem
                        className="text-red-600"
                        onClick={() => setConfirmDelete(row.username)}
                        disabled={row.system} // System users cannot be deleted
                      >
                        <Trash2 className="mr-2 h-4 w-4" />
                        删除用户
                        {row.system && <span className="ml-2 text-xs">(系统用户)</span>}
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </TooltipProvider>
              </div>
            )}
          />
        </div>
      </div>

      {/* Create/Edit User Dialog */}
      <Dialog open={showUserForm} onOpenChange={setShowUserForm}>
        <DialogContent className="sm:max-w-[500px]">
          <DialogHeader>
            <DialogTitle>{selectedUser ? "编辑用户" : "创建新用户"}</DialogTitle>
            <DialogDescription>
              {selectedUser ? "更新用户信息和权限" : "添加新用户到系统"}
            </DialogDescription>
          </DialogHeader>
          <UserForm
            user={selectedUser}
            onSuccess={handleUserFormSubmit}
            onCancel={() => setShowUserForm(false)}
          />
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <Dialog open={!!confirmDelete} onOpenChange={() => setConfirmDelete(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>确认删除用户</DialogTitle>
            <DialogDescription>
              此操作无法撤销，用户数据将被永久删除。
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setConfirmDelete(null)}>
              取消
            </Button>
            <Button
              variant="destructive"
              onClick={() => confirmDelete && handleDeleteUser(confirmDelete)}
            >
              删除
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Password Change Dialog */}
      <Dialog open={showPasswordForm} onOpenChange={setShowPasswordForm}>
        <DialogContent className="sm:max-w-[500px]">
          <DialogHeader>
            <DialogTitle>修改密码</DialogTitle>
            <DialogDescription>
              为用户 {selectedUser?.username} 修改密码
            </DialogDescription>
          </DialogHeader>
          {selectedUser && (
            <PasswordForm
              username={selectedUser.username}
              onSuccess={() => {
                setShowPasswordForm(false);
                setSelectedUser(null);
              }}
              onCancel={() => setShowPasswordForm(false)}
            />
          )}
        </DialogContent>
      </Dialog>

      {/* Permission Management Dialog */}
      <Dialog open={showPermissionForm} onOpenChange={setShowPermissionForm}>
        <DialogContent className="sm:max-w-[500px]">
          <DialogHeader>
            <DialogTitle>管理权限</DialogTitle>
            <DialogDescription>
              设置用户 {selectedUser?.username} 的权限
            </DialogDescription>
          </DialogHeader>
          {selectedUser && (
            <PermissionForm
              user={selectedUser}
              onSuccess={() => {
                setShowPermissionForm(false);
                setSelectedUser(null);
                fetchUsers(); // Refresh user list to get updated permissions
              }}
              onCancel={() => setShowPermissionForm(false)}
            />
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}