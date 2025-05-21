import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../lib/context/auth-context";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import { Label } from "../../components/ui/label";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "../../components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../../components/ui/tabs";
import { Avatar, AvatarFallback, AvatarImage } from "../../components/ui/avatar";
import { Separator } from "../../components/ui/separator";
import { Alert, AlertDescription } from "../../components/ui/alert";
import { Menu, User, Mail, Key, Shield, Bell, Clock, Save } from "lucide-react";
import { Switch } from "../../components/ui/switch";
import TopNavigation from "../../components/top-navigation";
import SupabaseSidebar from "../../components/supabase-sidebar";

export default function ProfilePage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  
  // Form states
  const [formData, setFormData] = useState({
    username: user?.username || "",
    email: user?.email || "",
    fullName: "",
    bio: "",
    currentPassword: "",
    newPassword: "",
    confirmPassword: "",
  });
  
  const [notifications, setNotifications] = useState({
    emailNotifications: true,
    pushNotifications: false,
    weeklyDigest: true,
    marketingEmails: false,
  });
  
  const [successMessage, setSuccessMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");
  
  // Handle input changes
  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };
  
  // Handle notification toggle
  const handleNotificationToggle = (key: keyof typeof notifications) => {
    setNotifications(prev => ({ ...prev, [key]: !prev[key] }));
  };
  
  // Handle profile update
  const handleProfileUpdate = (e: React.FormEvent) => {
    e.preventDefault();
    // Here you would typically call an API to update the user profile
    setSuccessMessage("个人资料已更新");
    setTimeout(() => setSuccessMessage(""), 3000);
  };
  
  // Handle password update
  const handlePasswordUpdate = (e: React.FormEvent) => {
    e.preventDefault();
    
    if (formData.newPassword !== formData.confirmPassword) {
      setErrorMessage("新密码和确认密码不匹配");
      setTimeout(() => setErrorMessage(""), 3000);
      return;
    }
    
    // Here you would typically call an API to update the password
    setSuccessMessage("密码已更新");
    setTimeout(() => setSuccessMessage(""), 3000);
  };
  
  // Get user initials for avatar
  const userInitials = user?.username ? user.username.substring(0, 2).toUpperCase() : "U";
  
  return (
    <div className="flex min-h-screen bg-background">
      <SupabaseSidebar collapsed={sidebarCollapsed} setCollapsed={setSidebarCollapsed} />
      
      <div className="flex-1 flex flex-col">
        <TopNavigation>
          <Button 
            variant="ghost" 
            size="sm" 
            onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
          >
            <Menu size={16} />
          </Button>
        </TopNavigation>
        
        <div className="flex-1 p-6 overflow-auto">
          <div className="max-w-5xl mx-auto">
            <div className="flex flex-col md:flex-row items-start md:items-center justify-between mb-6">
              <div>
                <h1 className="text-2xl font-bold">个人资料</h1>
                <p className="text-muted-foreground">管理您的账户信息和偏好设置</p>
              </div>
              
              <div className="mt-4 md:mt-0">
                <Button onClick={() => navigate(-1)}>返回</Button>
              </div>
            </div>
            
            <div className="flex flex-col md:flex-row gap-6">
              {/* Sidebar */}
              <div className="w-full md:w-1/4">
                <Card>
                  <CardContent className="p-6">
                    <div className="flex flex-col items-center">
                      <Avatar className="h-24 w-24 mb-4">
                        <AvatarFallback className="text-xl">{userInitials}</AvatarFallback>
                      </Avatar>
                      <h3 className="text-lg font-medium">{user?.username}</h3>
                      <p className="text-sm text-muted-foreground">{user?.email}</p>
                      <p className="text-sm text-muted-foreground mt-1">角色: {user?.role || "用户"}</p>
                      
                      <Button variant="outline" className="mt-4 w-full">
                        更换头像
                      </Button>
                    </div>
                    
                    <Separator className="my-6" />
                    
                    <div className="space-y-1">
                      <p className="text-sm text-muted-foreground">账户创建于</p>
                      <p className="text-sm">{new Date(user?.createTime || Date.now()).toLocaleDateString()}</p>
                    </div>
                  </CardContent>
                </Card>
              </div>
              
              {/* Main content */}
              <div className="flex-1">
                <Tabs defaultValue="profile" className="w-full">
                  <TabsList className="mb-4">
                    <TabsTrigger value="profile" className="flex items-center">
                      <User className="mr-2 h-4 w-4" />
                      个人信息
                    </TabsTrigger>
                    <TabsTrigger value="security" className="flex items-center">
                      <Key className="mr-2 h-4 w-4" />
                      安全设置
                    </TabsTrigger>
                    <TabsTrigger value="notifications" className="flex items-center">
                      <Bell className="mr-2 h-4 w-4" />
                      通知设置
                    </TabsTrigger>
                  </TabsList>
                  
                  {/* Profile Tab */}
                  <TabsContent value="profile">
                    <Card>
                      <CardHeader>
                        <CardTitle>个人信息</CardTitle>
                        <CardDescription>更新您的个人信息和公开资料</CardDescription>
                      </CardHeader>
                      <CardContent>
                        {successMessage && (
                          <Alert className="mb-4">
                            <AlertDescription>{successMessage}</AlertDescription>
                          </Alert>
                        )}
                        
                        <form onSubmit={handleProfileUpdate}>
                          <div className="grid gap-4">
                            <div className="grid gap-2">
                              <Label htmlFor="username">用户名</Label>
                              <Input
                                id="username"
                                name="username"
                                value={formData.username}
                                onChange={handleInputChange}
                                disabled
                              />
                              <p className="text-xs text-muted-foreground">
                                用户名不可更改
                              </p>
                            </div>
                            
                            <div className="grid gap-2">
                              <Label htmlFor="email">电子邮箱</Label>
                              <Input
                                id="email"
                                name="email"
                                type="email"
                                value={formData.email}
                                onChange={handleInputChange}
                              />
                            </div>
                            
                            <div className="grid gap-2">
                              <Label htmlFor="fullName">全名</Label>
                              <Input
                                id="fullName"
                                name="fullName"
                                value={formData.fullName}
                                onChange={handleInputChange}
                                placeholder="输入您的全名"
                              />
                            </div>
                            
                            <div className="grid gap-2">
                              <Label htmlFor="bio">个人简介</Label>
                              <textarea
                                id="bio"
                                name="bio"
                                value={formData.bio}
                                onChange={handleInputChange}
                                placeholder="简单介绍一下自己"
                                className="min-h-[100px] rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                              />
                            </div>
                          </div>
                          
                          <div className="mt-6">
                            <Button type="submit" className="flex items-center">
                              <Save className="mr-2 h-4 w-4" />
                              保存更改
                            </Button>
                          </div>
                        </form>
                      </CardContent>
                    </Card>
                  </TabsContent>
                  
                  {/* Security Tab */}
                  <TabsContent value="security">
                    <Card>
                      <CardHeader>
                        <CardTitle>安全设置</CardTitle>
                        <CardDescription>管理您的密码和账户安全选项</CardDescription>
                      </CardHeader>
                      <CardContent>
                        {successMessage && (
                          <Alert className="mb-4">
                            <AlertDescription>{successMessage}</AlertDescription>
                          </Alert>
                        )}
                        
                        {errorMessage && (
                          <Alert className="mb-4" variant="destructive">
                            <AlertDescription>{errorMessage}</AlertDescription>
                          </Alert>
                        )}
                        
                        <form onSubmit={handlePasswordUpdate}>
                          <div className="grid gap-4">
                            <div className="grid gap-2">
                              <Label htmlFor="currentPassword">当前密码</Label>
                              <Input
                                id="currentPassword"
                                name="currentPassword"
                                type="password"
                                value={formData.currentPassword}
                                onChange={handleInputChange}
                              />
                            </div>
                            
                            <div className="grid gap-2">
                              <Label htmlFor="newPassword">新密码</Label>
                              <Input
                                id="newPassword"
                                name="newPassword"
                                type="password"
                                value={formData.newPassword}
                                onChange={handleInputChange}
                              />
                              <p className="text-xs text-muted-foreground">
                                密码至少包含8个字符，包括字母和数字
                              </p>
                            </div>
                            
                            <div className="grid gap-2">
                              <Label htmlFor="confirmPassword">确认新密码</Label>
                              <Input
                                id="confirmPassword"
                                name="confirmPassword"
                                type="password"
                                value={formData.confirmPassword}
                                onChange={handleInputChange}
                              />
                            </div>
                          </div>
                          
                          <div className="mt-6">
                            <Button type="submit" className="flex items-center">
                              <Key className="mr-2 h-4 w-4" />
                              更新密码
                            </Button>
                          </div>
                        </form>
                        
                        <Separator className="my-6" />
                        
                        <div>
                          <h3 className="text-lg font-medium mb-4">双因素认证</h3>
                          <div className="flex items-center justify-between">
                            <div>
                              <p className="font-medium">双因素认证</p>
                              <p className="text-sm text-muted-foreground">
                                为您的账户添加额外的安全层
                              </p>
                            </div>
                            <Button variant="outline">设置</Button>
                          </div>
                        </div>
                      </CardContent>
                    </Card>
                  </TabsContent>
                  
                  {/* Notifications Tab */}
                  <TabsContent value="notifications">
                    <Card>
                      <CardHeader>
                        <CardTitle>通知设置</CardTitle>
                        <CardDescription>配置您希望接收的通知类型</CardDescription>
                      </CardHeader>
                      <CardContent>
                        <div className="space-y-4">
                          <div className="flex items-center justify-between">
                            <div>
                              <p className="font-medium">电子邮件通知</p>
                              <p className="text-sm text-muted-foreground">
                                接收有关账户活动的电子邮件通知
                              </p>
                            </div>
                            <Switch
                              checked={notifications.emailNotifications}
                              onCheckedChange={() => handleNotificationToggle("emailNotifications")}
                            />
                          </div>
                          
                          <Separator />
                          
                          <div className="flex items-center justify-between">
                            <div>
                              <p className="font-medium">推送通知</p>
                              <p className="text-sm text-muted-foreground">
                                接收实时推送通知
                              </p>
                            </div>
                            <Switch
                              checked={notifications.pushNotifications}
                              onCheckedChange={() => handleNotificationToggle("pushNotifications")}
                            />
                          </div>
                          
                          <Separator />
                          
                          <div className="flex items-center justify-between">
                            <div>
                              <p className="font-medium">每周摘要</p>
                              <p className="text-sm text-muted-foreground">
                                接收每周活动摘要邮件
                              </p>
                            </div>
                            <Switch
                              checked={notifications.weeklyDigest}
                              onCheckedChange={() => handleNotificationToggle("weeklyDigest")}
                            />
                          </div>
                          
                          <Separator />
                          
                          <div className="flex items-center justify-between">
                            <div>
                              <p className="font-medium">营销邮件</p>
                              <p className="text-sm text-muted-foreground">
                                接收产品更新和营销信息
                              </p>
                            </div>
                            <Switch
                              checked={notifications.marketingEmails}
                              onCheckedChange={() => handleNotificationToggle("marketingEmails")}
                            />
                          </div>
                        </div>
                        
                        <div className="mt-6">
                          <Button className="flex items-center">
                            <Save className="mr-2 h-4 w-4" />
                            保存通知设置
                          </Button>
                        </div>
                      </CardContent>
                    </Card>
                  </TabsContent>
                </Tabs>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
