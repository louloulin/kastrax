import React, { ReactNode } from 'react';
import { usePermissionStore } from '../../lib/store/permission-store';

interface PermissionGuardProps {
  /**
   * 需要的权限，可以是单个权限字符串或权限字符串数组
   * 如果是数组，则用户只需要拥有其中一个权限即可
   */
  permission: string | string[];
  
  /**
   * 子组件，当用户有权限时显示
   */
  children: ReactNode;
  
  /**
   * 当用户没有权限时显示的内容，默认为 null
   */
  fallback?: ReactNode;
}

/**
 * 权限守卫组件
 * 根据用户权限控制 UI 元素的显示
 */
export function PermissionGuard({ permission, children, fallback = null }: PermissionGuardProps) {
  const { hasPermission } = usePermissionStore();
  
  // 检查用户是否有权限
  if (hasPermission(permission)) {
    return <>{children}</>;
  }
  
  // 如果没有权限，显示 fallback 内容
  return <>{fallback}</>;
}

export default PermissionGuard;
