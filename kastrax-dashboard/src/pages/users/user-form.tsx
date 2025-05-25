import React, { useState } from "react";
import { z } from "zod";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage
} from "../../components/ui/form";
import { Input } from "../../components/ui/input";
import { Button } from "../../components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../../components/ui/select";
import { Switch } from "../../components/ui/switch";
import { Loader2 } from "lucide-react";
import { User, createUser, updateUser } from "../../lib/api/auth";

// Form validation schema
const formSchema = z.object({
  username: z.string().min(1, "用户名不能为空"),
  email: z.string().email("请输入有效的邮箱地址"),
  password: z.string().min(6, "密码至少6位字符").optional().or(z.literal('')),
  role: z.string().min(1, "请选择用户角色"),
  status: z.number(),
  description: z.string().optional(),
  system: z.boolean().optional(),
});

interface UserFormProps {
  user: User | null;
  onSuccess: () => void;
  onCancel: () => void;
}

export default function UserForm({ user, onSuccess, onCancel }: UserFormProps) {
  const [loading, setLoading] = useState(false);
  const isEditing = !!user;

  // Initialize form with React Hook Form and zod validation
  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      username: user?.username || "",
      email: user?.email || "",
      password: "", // Password is not loaded for security reasons
      role: user?.role || "user",
      status: user?.status || 1,
      description: user?.description || "",
      system: user?.system || false,
    },
  });

  // Handle form submission
  const onSubmit = async (values: z.infer<typeof formSchema>) => {
    setLoading(true);

    try {
      if (isEditing && user) {
        // Update existing user
        const updateData: Partial<User> & { password?: string } = {
          username: user.username, // Include username for datalink-ui compatibility
          email: values.email,
          role: values.role,
          status: values.status,
          description: values.description,
          system: values.system,
        };

        // Only include password if provided
        if (values.password) {
          updateData.password = values.password;
        }

        await updateUser(updateData);
      } else {
        // Create new user
        await createUser({
          username: values.username,
          email: values.email,
          password: values.password as string, // Password is required for new users
          role: values.role,
          status: values.status,
          description: values.description,
          system: values.system,
          permissions: values.system ? ['all'] : ['dashboard'], // System users get all permissions
        } as any); // Use type assertion to bypass TypeScript check
      }

      onSuccess();
    } catch (error) {
      console.error("Failed to save user:", error);
      alert(isEditing ? "更新用户失败" : "创建用户失败");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
        <FormField
          control={form.control}
          name="username"
          render={({ field }) => (
            <FormItem>
              <FormLabel>用户名</FormLabel>
              <FormControl>
                <Input
                  placeholder="输入用户名"
                  {...field}
                  disabled={isEditing} // Username cannot be changed for existing users
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="email"
          render={({ field }) => (
            <FormItem>
              <FormLabel>邮箱</FormLabel>
              <FormControl>
                <Input
                  type="email"
                  placeholder="输入邮箱"
                  {...field}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="password"
          render={({ field }) => (
            <FormItem>
              <FormLabel>{isEditing ? "新密码 (留空不修改)" : "密码"}</FormLabel>
              <FormControl>
                <Input
                  type="password"
                  placeholder={isEditing ? "输入新密码 (可选)" : "输入密码"}
                  {...field}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="role"
          render={({ field }) => (
            <FormItem>
              <FormLabel>角色</FormLabel>
              <Select
                onValueChange={field.onChange}
                defaultValue={field.value}
              >
                <FormControl>
                  <SelectTrigger>
                    <SelectValue placeholder="选择用户角色" />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  <SelectItem value="admin">管理员</SelectItem>
                  <SelectItem value="user">普通用户</SelectItem>
                  <SelectItem value="guest">访客</SelectItem>
                </SelectContent>
              </Select>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="status"
          render={({ field }) => (
            <FormItem className="flex flex-row items-center justify-between rounded-lg border p-3">
              <div className="space-y-0.5">
                <FormLabel>账号状态</FormLabel>
                <div className="text-sm text-muted-foreground">
                  {field.value === 1 ? "启用" : "禁用"}
                </div>
              </div>
              <FormControl>
                <Switch
                  checked={field.value === 1}
                  onCheckedChange={(checked) => field.onChange(checked ? 1 : 0)}
                />
              </FormControl>
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="description"
          render={({ field }) => (
            <FormItem>
              <FormLabel>备注</FormLabel>
              <FormControl>
                <Input
                  placeholder="输入用户备注信息"
                  {...field}
                  value={field.value || ""}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="system"
          render={({ field }) => (
            <FormItem className="flex flex-row items-center justify-between rounded-lg border p-3">
              <div className="space-y-0.5">
                <FormLabel>系统用户</FormLabel>
                <div className="text-sm text-muted-foreground">
                  系统用户拥有全部权限且不可删除
                </div>
              </div>
              <FormControl>
                <Switch
                  checked={field.value === true}
                  onCheckedChange={field.onChange}
                  disabled={isEditing && user?.system} // Cannot change system status for existing system users
                />
              </FormControl>
            </FormItem>
          )}
        />

        <div className="flex justify-end space-x-2 pt-4">
          <Button
            type="button"
            variant="outline"
            onClick={onCancel}
          >
            取消
          </Button>
          <Button
            type="submit"
            disabled={loading}
          >
            {loading ? (
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
            ) : null}
            {isEditing ? "更新" : "创建"}
          </Button>
        </div>
      </form>
    </Form>
  );
}