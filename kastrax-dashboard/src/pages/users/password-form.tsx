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
import { Loader2 } from "lucide-react";
import { changePassword } from "../../lib/api/auth";

// Form validation schema
const passwordFormSchema = z.object({
  oldPassword: z.string().min(1, "请输入原密码"),
  newPassword: z.string().min(6, "新密码至少6位字符"),
  confirmPassword: z.string().min(6, "确认密码至少6位字符"),
}).refine((data) => data.newPassword === data.confirmPassword, {
  message: "新密码和确认密码不匹配",
  path: ["confirmPassword"],
});

interface PasswordFormProps {
  username: string;
  onSuccess: () => void;
  onCancel: () => void;
}

export default function PasswordForm({ username, onSuccess, onCancel }: PasswordFormProps) {
  const [loading, setLoading] = useState(false);

  // Initialize form with React Hook Form and zod validation
  const form = useForm<z.infer<typeof passwordFormSchema>>({
    resolver: zodResolver(passwordFormSchema),
    defaultValues: {
      oldPassword: "",
      newPassword: "",
      confirmPassword: "",
    },
  });

  // Handle form submission
  const onSubmit = async (values: z.infer<typeof passwordFormSchema>) => {
    setLoading(true);
    
    try {
      await changePassword(username, values.oldPassword, values.newPassword);
      onSuccess();
    } catch (error) {
      console.error("Failed to change password:", error);
      form.setError("root", { 
        message: "密码修改失败，请检查原密码是否正确" 
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
        <FormField
          control={form.control}
          name="oldPassword"
          render={({ field }) => (
            <FormItem>
              <FormLabel>原密码</FormLabel>
              <FormControl>
                <Input 
                  type="password" 
                  placeholder="请输入原密码" 
                  {...field} 
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="newPassword"
          render={({ field }) => (
            <FormItem>
              <FormLabel>新密码</FormLabel>
              <FormControl>
                <Input 
                  type="password" 
                  placeholder="请输入新密码" 
                  {...field} 
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="confirmPassword"
          render={({ field }) => (
            <FormItem>
              <FormLabel>确认新密码</FormLabel>
              <FormControl>
                <Input 
                  type="password" 
                  placeholder="请再次输入新密码" 
                  {...field} 
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        {form.formState.errors.root && (
          <div className="text-red-500 text-sm">
            {form.formState.errors.root.message}
          </div>
        )}

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
            确认修改
          </Button>
        </div>
      </form>
    </Form>
  );
}
