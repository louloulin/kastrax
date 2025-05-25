import React from 'react';
import { Navigate } from 'react-router-dom';
import { usePermissionStore } from '../lib/store/permission-store';
import { useAuth } from '../lib/context/auth-context';

interface PermissionGuardProps {
  permission: string | string[];
  children: React.ReactNode;
  redirectTo?: string;
}

/**
 * 权限守卫组件
 *
 * 用于保护需要特定权限的路由或组件
 * 如果用户没有所需权限，将重定向到指定路径或显示无权限提示
 */
export function PermissionGuard({ permission, children, redirectTo = '/' }: PermissionGuardProps) {
  const { isAuthenticated } = useAuth();
  const { hasPermission } = usePermissionStore();

  // 如果未认证，不需要检查权限
  if (!isAuthenticated) {
    return <Navigate to="/login" />;
  }

  // 检查是否有所需权限
  if (!hasPermission(permission)) {
    // 如果提供了重定向路径，则重定向
    if (redirectTo) {
      return <Navigate to={redirectTo} />;
    }

    // 否则显示无权限提示
    return (
      <div className="flex flex-col items-center justify-center h-screen">
        <h1 className="text-2xl font-bold mb-4">无权访问</h1>
        <p className="text-gray-500">您没有访问此页面的权限</p>
      </div>
    );
  }

  // 有权限，显示子组件
  return <>{children}</>;
}
