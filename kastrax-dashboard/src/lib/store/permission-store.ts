import { create } from 'zustand';
import { persist } from 'zustand/middleware';

// 定义权限类型
export type Permission = string;

// 定义路由类型
export interface Route {
  path: string;
  name: string;
  component: string;
  meta?: {
    title: string;
    icon?: string;
    permission?: string | string[];
  };
  children?: Route[];
}

interface PermissionState {
  permissions: Permission[];
  routes: Route[];

  // 设置权限
  setPermissions: (permissions: Permission[]) => void;

  // 检查是否有权限
  hasPermission: (permission: string | string[]) => boolean;

  // 生成路由
  generateRoutes: (permissions: Permission[]) => void;

  // 清除权限和路由
  clearPermissions: () => void;
}

// 基础路由，所有用户都可以访问
const constantRoutes: Route[] = [
  {
    path: '/login',
    name: 'login',
    component: 'LoginPage',
    meta: {
      title: '登录'
    }
  },
  {
    path: '/404',
    name: 'notFound',
    component: 'NotFoundPage',
    meta: {
      title: '页面不存在'
    }
  }
];

// 需要权限的路由
const asyncRoutes: Route[] = [
  {
    path: '/',
    name: 'dashboard',
    component: 'DashboardLayout',
    meta: {
      title: '仪表盘',
      permission: 'dashboard'
    },
    children: [
      {
        path: '/dashboard',
        name: 'dashboardIndex',
        component: 'DashboardPage',
        meta: {
          title: '概览',
          icon: 'dashboard',
          permission: 'dashboard'
        }
      }
    ]
  },
  {
    path: '/rules',
    name: 'rules',
    component: 'DashboardLayout',
    meta: {
      title: '规则管理',
      permission: 'rule'
    },
    children: [
      {
        path: '/rules/list',
        name: 'rulesList',
        component: 'RulesListPage',
        meta: {
          title: '规则列表',
          icon: 'rule',
          permission: 'rule'
        }
      },
      {
        path: '/rules/:id',
        name: 'ruleDetail',
        component: 'RuleDetailPage',
        meta: {
          title: '规则详情',
          permission: 'rule'
        }
      }
    ]
  },
  {
    path: '/resources',
    name: 'resources',
    component: 'DashboardLayout',
    meta: {
      title: '资源管理',
      permission: 'resource'
    },
    children: [
      {
        path: '/resources/list',
        name: 'resourcesList',
        component: 'ResourcesListPage',
        meta: {
          title: '资源列表',
          icon: 'resource',
          permission: 'resource'
        }
      },
      {
        path: '/resources/:id',
        name: 'resourceDetail',
        component: 'ResourceDetailPage',
        meta: {
          title: '资源详情',
          permission: 'resource'
        }
      }
    ]
  },
  {
    path: '/system',
    name: 'system',
    component: 'DashboardLayout',
    meta: {
      title: '系统管理',
      permission: 'system'
    },
    children: [
      {
        path: '/system/users',
        name: 'users',
        component: 'UsersPage',
        meta: {
          title: '用户管理',
          icon: 'user',
          permission: 'system'
        }
      },
      {
        path: '/system/settings',
        name: 'settings',
        component: 'SettingsPage',
        meta: {
          title: '系统设置',
          icon: 'setting',
          permission: 'system'
        }
      }
    ]
  }
];

// 权限映射表，将 datalink-ui 的权限映射到 dataflare-ui 的权限
const permissionMap: Record<string, string[]> = {
  'all': ['all', 'dashboard', 'rule', 'resource', 'script', 'variable', 'backup', 'data', 'admin'],
  'dashboard': ['dashboard'],
  'rule': ['rule'],
  'resource': ['resource'],
  'script': ['script'],
  'variable': ['variable'],
  'backup': ['backup'],
  'system': ['admin'],
  'user': ['admin']
};

// 过滤路由，根据权限
const filterRoutes = (routes: Route[], permissions: Permission[]): Route[] => {
  // 将 datalink-ui 的权限映射到 dataflare-ui 的权限
  const mappedPermissions = permissions.flatMap(p => permissionMap[p] || []);

  const hasPermission = (route: Route, perms: Permission[]): boolean => {
    if (route.meta && route.meta.permission) {
      // 如果有 all 权限，直接返回 true
      if (perms.includes('all')) {
        return true;
      }

      // 检查是否有特定权限
      const requiredPermission = route.meta.permission;
      if (typeof requiredPermission === 'string') {
        return perms.includes(requiredPermission);
      } else if (Array.isArray(requiredPermission)) {
        return requiredPermission.some(p => perms.includes(p));
      }
      return false;
    }
    return true; // 没有指定权限要求，默认允许访问
  };

  return routes.filter(route => {
    if (hasPermission(route, permissions)) {
      if (route.children) {
        route.children = filterRoutes(route.children, permissions);
      }
      return true;
    }
    return false;
  });
};

export const usePermissionStore = create<PermissionState>()(
  persist(
    (set, get) => ({
      permissions: [],
      routes: constantRoutes,

      setPermissions: (permissions: Permission[]) => {
        set({ permissions });
        get().generateRoutes(permissions);
      },

      hasPermission: (permission: string | string[]) => {
        const { permissions } = get();

        // 如果有 all 权限，直接返回 true
        if (permissions.includes('all')) {
          return true;
        }

        if (typeof permission === 'string') {
          return permissions.includes(permission);
        } else if (Array.isArray(permission)) {
          return permission.some(p => permissions.includes(p));
        }
        return false;
      },

      generateRoutes: (permissions: Permission[]) => {
        // 将 datalink-ui 的权限映射到 dataflare-ui 的权限
        const mappedPermissions = permissions.flatMap(p => permissionMap[p] || []);
        console.log('[Permission Store] Original permissions:', permissions);
        console.log('[Permission Store] Mapped permissions:', mappedPermissions);

        const filteredRoutes = filterRoutes(asyncRoutes, mappedPermissions);
        set({ routes: [...constantRoutes, ...filteredRoutes] });
      },

      clearPermissions: () => {
        set({ permissions: [], routes: constantRoutes });
      }
    }),
    {
      name: 'permission-storage',
      partialize: (state) => ({ permissions: state.permissions }),
    }
  )
);
