import React, { useState, ReactNode } from "react";
import { Bell, Search, User, ChevronDown, LogOut, Settings, UserIcon } from "lucide-react";
import { ThemeSwitcher } from "./theme-switcher";
import { useAuth } from "../lib/context/auth-context";
import { useNavigate } from "react-router-dom";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "./ui/dropdown-menu";
import { Avatar, AvatarFallback } from "./ui/avatar";
import { Button } from "./ui/button";

interface TopNavigationProps {
  children?: ReactNode;
}

export default function TopNavigation({ children }: TopNavigationProps) {
  const [isSearchFocused, setIsSearchFocused] = useState(false);
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  // 获取用户姓名首字母
  const userInitials = user?.username ? user.username.substring(0, 2).toUpperCase() : 'U';
  const userName = user?.username || '用户';
  const userEmail = user?.email || 'admin@example.com';

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <div className="h-16 border-b border-border flex items-center justify-between px-4 bg-card">
      <div className="flex items-center">
        {children}
        <h1 className="text-lg font-medium ml-2">仪表盘</h1>
      </div>

      <div className="flex items-center space-x-3">
        <div
          className={`relative w-64 ${
            isSearchFocused ? "bg-background" : "bg-secondary/50"
          } rounded-md transition-colors duration-200`}
        >
          <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3">
            <Search size={14} className="text-muted-foreground" />
          </div>
          <input
            type="text"
            placeholder="搜索..."
            className="w-full bg-transparent py-1.5 pl-8 text-sm outline-none"
            onFocus={() => setIsSearchFocused(true)}
            onBlur={() => setIsSearchFocused(false)}
          />
        </div>

        <button className="relative p-2 text-muted-foreground hover:text-foreground">
          <Bell size={18} />
          <span className="absolute top-1 right-1 w-2 h-2 bg-primary rounded-full"></span>
        </button>

        <ThemeSwitcher />

        <div className="flex items-center pl-3 ml-3 border-l border-border">
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" className="flex items-center space-x-2 p-1 px-2 -mr-2 h-auto">
                <Avatar className="h-8 w-8">
                  <AvatarFallback>{userInitials}</AvatarFallback>
                </Avatar>
                <div className="hidden md:block text-left">
                  <p className="text-sm font-medium">{userName}</p>
                  <p className="text-xs text-muted-foreground">{userEmail}</p>
                </div>
                <ChevronDown size={14} className="text-muted-foreground ml-1" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent className="w-56" align="end">
              <DropdownMenuLabel className="font-normal">
                <div className="flex flex-col space-y-1">
                  <p className="text-sm font-medium leading-none">{userName}</p>
                  <p className="text-xs leading-none text-muted-foreground">
                    {userEmail}
                  </p>
                </div>
              </DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuGroup>
                <DropdownMenuItem onClick={() => navigate('/profile')}>
                  <UserIcon className="mr-2 h-4 w-4" />
                  <span>个人资料</span>
                </DropdownMenuItem>
                <DropdownMenuItem onClick={() => navigate('/settings')}>
                  <Settings className="mr-2 h-4 w-4" />
                  <span>设置</span>
                </DropdownMenuItem>
              </DropdownMenuGroup>
              <DropdownMenuSeparator />
              <DropdownMenuItem onClick={handleLogout}>
                <LogOut className="mr-2 h-4 w-4" />
                <span>退出登录</span>
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>
    </div>
  );
}