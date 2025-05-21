import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { usePermissionStore } from '../../lib/store/permission-store';
import { useAuth } from '../../lib/context/auth-context';

interface PermissionRouteProps {
  /**
   * 需要的权限，可以是单个权限字符串或权限字符串数组
   * 如果是数组，则用户只需要拥有其中一个权限即可
   */
  permission?: string | string[];
  
  /**
   * 当用户有权限时渲染的组件
   */
  element: React.ReactNode;
  
  /**
   * 当用户没有权限时重定向的路径，默认为 "/unauthorized"
   */
  redirectTo?: string;
}

/**
 * 权限路由守卫组件
 * 根据用户权限控制路由访问
 */
export function PermissionRoute({ 
  permission, 
  element, 
  redirectTo = "/unauthorized" 
}: PermissionRouteProps) {
  const { hasPermission } = usePermissionStore();
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();
  
  // 如果正在加载，显示加载状态
  if (isLoading) {
    return <div className="flex items-center justify-center h-screen">加载中...</div>;
  }
  
  // 如果未认证，重定向到登录页面
  if (!isAuthenticated) {
    return <Navigate to={`/login?returnUrl=${encodeURIComponent(location.pathname)}`} replace />;
  }
  
  // 如果没有指定权限要求，直接渲染组件
  if (!permission) {
    return <>{element}</>;
  }
  
  // 检查用户是否有权限
  if (hasPermission(permission)) {
    return <>{element}</>;
  }
  
  // 如果没有权限，重定向到指定路径
  return <Navigate to={redirectTo} replace />;
}

export default PermissionRoute;
