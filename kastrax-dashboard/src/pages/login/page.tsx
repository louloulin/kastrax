import React, { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
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
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "../../components/ui/card";
import { Alert, AlertDescription } from "../../components/ui/alert";
import { Loader2 } from "lucide-react";
import { useAuth } from "../../lib/context/auth-context";

// Form validation schema
const formSchema = z.object({
  username: z.string().min(1, "用户名不能为空"),
  password: z.string().min(6, "密码至少6位字符"),
});

export default function LoginPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const returnUrl = searchParams.get("returnUrl") || "/";

  const { isAuthenticated, isLoading, login, error, clearError } = useAuth();

  // Initialize form with React Hook Form and zod validation
  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      username: "",
      password: "",
    },
  });

  // Redirect if already authenticated
  useEffect(() => {
    console.log('[Login Page] Auth state check - isAuthenticated:', isAuthenticated, 'isLoading:', isLoading);
    if (isAuthenticated && !isLoading) {
      console.log('[Login Page] Already authenticated, redirecting to:', returnUrl);
      navigate(returnUrl);
    }
  }, [isAuthenticated, isLoading, navigate, returnUrl]);

  // Clear error when form values change
  useEffect(() => {
    const subscription = form.watch(() => {
      if (error) {
        clearError();
      }
    });
    return () => subscription.unsubscribe();
  }, [form, error, clearError]);

  // Handle form submission
  const onSubmit = async (values: z.infer<typeof formSchema>) => {
    console.log('[Login Page] Form submitted with username:', values.username);
    try {
      await login(values.username, values.password);
      console.log('[Login Page] Login function completed');
    } catch (err) {
      console.error('[Login Page] Error during login:', err);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-background p-4">
      <div className="w-full max-w-md">
        <Card className="border-border">
          <CardHeader className="space-y-1">
            <div className="flex justify-center mb-6">
              <img
                src="/logo.svg"
                alt="Dataflare Logo"
                className="h-10"
              />
            </div>
            <CardTitle className="text-2xl font-semibold text-center">
              登录 Dataflare
            </CardTitle>
            <CardDescription className="text-center">
              输入您的用户名和密码登录系统
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {error && (
              <Alert variant="destructive">
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}

            <Form {...form}>
              <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                <FormField
                  control={form.control}
                  name="username"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>用户名</FormLabel>
                      <FormControl>
                        <Input placeholder="输入用户名" {...field} />
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
                      <FormLabel>密码</FormLabel>
                      <FormControl>
                        <Input
                          type="password"
                          placeholder="输入密码"
                          {...field}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <Button
                  type="submit"
                  className="w-full"
                  disabled={isLoading}
                >
                  {isLoading ? (
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  ) : null}
                  登录
                </Button>
              </form>
            </Form>
          </CardContent>
          <CardFooter className="flex justify-center text-sm">
            <span className="text-muted-foreground">
              没有账号？请联系管理员创建
            </span>
          </CardFooter>
        </Card>
      </div>
    </div>
  );
}