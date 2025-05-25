/**
 * 用户信息接口
 */
export interface User {
  userId: string;
  username: string;
  email: string;
  role: string;
  status: number;
  avatar?: string;
  description?: string;  // Added from datalink-ui
  permissions: string[]; // Added from datalink-ui
  createTime: string;
  lastLoginTime?: string;
  system?: boolean;      // Added from datalink-ui to mark system users
}

/**
 * 模拟用户数据
 */
export const mockUsers: User[] = [
  {
    userId: 'user-001',
    username: 'admin',
    email: 'admin@example.com',
    role: 'admin',
    status: 1,
    avatar: 'https://via.placeholder.com/150',
    description: '管理员',
    permissions: ['all'],
    createTime: '2023-01-01T00:00:00Z',
    lastLoginTime: '2023-04-01T10:30:00Z',
    system: true
  },
  {
    userId: 'user-002',
    username: 'user',
    email: 'user@example.com',
    role: 'user',
    status: 1,
    description: '普通用户',
    permissions: ['dashboard'],
    createTime: '2023-01-02T00:00:00Z',
    lastLoginTime: '2023-03-30T15:45:00Z',
    system: false
  },
  {
    userId: 'user-003',
    username: 'developer',
    email: 'dev@example.com',
    role: 'developer',
    status: 1,
    avatar: 'https://via.placeholder.com/150',
    description: '开发人员',
    permissions: ['dashboard', 'rule', 'script'],
    createTime: '2023-01-03T00:00:00Z',
    lastLoginTime: '2023-04-05T09:15:00Z',
    system: false
  },
  {
    userId: 'user-004',
    username: 'tester',
    email: 'test@example.com',
    role: 'tester',
    status: 0, // 禁用状态
    description: '测试人员',
    permissions: ['dashboard', 'data'],
    createTime: '2023-01-04T00:00:00Z',
    lastLoginTime: '2023-02-15T11:20:00Z',
    system: false
  }
];