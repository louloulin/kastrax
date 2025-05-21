import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../lib/context/auth-context';
import {
  AvatarIcon,
  ExitIcon,
  GearIcon,
  PersonIcon,
} from '@radix-ui/react-icons';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';

export function UserNav() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  // 获取用户姓名首字母
  const userInitials = user?.username ? user.username.substring(0, 2).toUpperCase() : 'U';
  const userName = user?.username || '用户';
  const userEmail = user?.email || '';

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" className="relative h-8 w-8 rounded-full">
          <Avatar className="h-8 w-8">
            <AvatarFallback>{userInitials}</AvatarFallback>
          </Avatar>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent className="w-56" align="end" forceMount>
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
          <DropdownMenuItem>
            <PersonIcon className="mr-2 h-4 w-4" />
            <span>个人资料</span>
          </DropdownMenuItem>
          <DropdownMenuItem>
            <Link to="/settings" className="flex items-center w-full">
              <GearIcon className="mr-2 h-4 w-4" />
              <span>设置</span>
            </Link>
          </DropdownMenuItem>
        </DropdownMenuGroup>
        <DropdownMenuSeparator />
        <DropdownMenuItem onClick={handleLogout}>
          <ExitIcon className="mr-2 h-4 w-4" />
          <span>退出登录</span>
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}