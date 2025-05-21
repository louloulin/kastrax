import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { cn } from '@/lib/utils';
import {
  DashboardIcon,
  GearIcon,
  LayersIcon,
  TableIcon,
  BarChartIcon,
  BackpackIcon,
  FileTextIcon,
  ClockIcon,
  PersonIcon,
  BoxIcon,
  RocketIcon,
  MixerHorizontalIcon,
} from '@radix-ui/react-icons';

interface NavigationItem {
  title: string;
  href: string;
  icon: React.ReactNode;
  description?: string;
}

export function MainNav() {
  const location = useLocation();
  
  const navigationItems: NavigationItem[] = [
    {
      title: '仪表盘',
      href: '/dashboard',
      icon: <DashboardIcon className="h-4 w-4" />,
      description: '系统概览和关键指标',
    },
    {
      title: 'SQL编辑器',
      href: '/sql',
      icon: <FileTextIcon className="h-4 w-4" />,
      description: '执行SQL查询和管理数据库',
    },
    {
      title: '资源管理',
      href: '/resources',
      icon: <BoxIcon className="h-4 w-4" />,
      description: '管理数据源和连接配置',
    },
    {
      title: '规则管理',
      href: '/rules',
      icon: <LayersIcon className="h-4 w-4" />,
      description: '创建和管理工作流规则',
    },
    {
      title: '脚本管理',
      href: '/scripts',
      icon: <FileTextIcon className="h-4 w-4" />,
      description: '管理和执行自定义脚本',
    },
    {
      title: '变量管理',
      href: '/variables',
      icon: <BackpackIcon className="h-4 w-4" />,
      description: '管理系统变量和配置',
    },
    {
      title: '备份管理',
      href: '/backups',
      icon: <ClockIcon className="h-4 w-4" />,
      description: '管理系统备份和恢复',
    },
    {
      title: '数据库管理',
      href: '/database',
      icon: <TableIcon className="h-4 w-4" />,
      description: '管理数据库和表',
    },
    {
      title: '数据可视化',
      href: '/data-visualization',
      icon: <BarChartIcon className="h-4 w-4" />,
      description: '创建和查看数据图表',
    },
    {
      title: '用户管理',
      href: '/users',
      icon: <PersonIcon className="h-4 w-4" />,
      description: '管理用户和权限',
    },
    {
      title: '系统设置',
      href: '/settings',
      icon: <GearIcon className="h-4 w-4" />,
      description: '配置系统参数和偏好',
    },
  ];

  return (
    <nav className="flex items-center space-x-4 lg:space-x-6">
      {navigationItems.map((item) => (
        <Link
          key={item.href}
          to={item.href}
          className={cn(
            "flex items-center text-sm font-medium transition-colors hover:text-primary",
            location.pathname === item.href || location.pathname.startsWith(`${item.href}/`)
              ? "text-primary"
              : "text-muted-foreground"
          )}
        >
          <span className="mr-2">{item.icon}</span>
          <span className="hidden md:inline-block">{item.title}</span>
        </Link>
      ))}
    </nav>
  );
} 