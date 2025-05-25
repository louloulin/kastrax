import client from './client';

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

export interface LoginParams {
  username: string;
  password: string;
}

export interface LoginResult {
  token: string;
  user: User;
}

export interface RegisterParams {
  username: string;
  password: string;
  email: string;
  role?: string;
}

// Authentication functions
export const login = (data: LoginParams) => {
  return client.post<LoginResult>('/api/v1/auth/login', data);
};

export const register = (data: RegisterParams) => {
  return client.post('/api/v1/auth/register', data);
};

export const logout = () => {
  return client.post('/api/v1/auth/logout');
};

export const getInfo = () => {
  return client.get<User>('/api/v1/auth/info');
};

export const refreshToken = () => {
  return client.post('/api/v1/auth/refresh-token');
};

// User management functions - aligned with datalink-ui
export const getUserList = (params?: any) => {
  console.log('Calling getUserList API with params:', params || {});
  return client.post('/api/user/list', params || {})
    .then(response => {
      console.log('getUserList API response:', response);

      // 检查响应数据结构
      if (response.data) {
        if (Array.isArray(response.data)) {
          console.log('Response data is an array with', response.data.length, 'items');
          if (response.data.length > 0) {
            console.log('First item keys:', Object.keys(response.data[0]));
            // 检查是否有username字段
            if (response.data[0].username) {
              console.log('Username field exists:', response.data[0].username);
            } else if (response.data[0].name) {
              console.log('Name field exists instead of username:', response.data[0].name);
            } else {
              console.warn('Neither username nor name field exists in the response data');
            }
          }
        } else if (typeof response.data === 'object') {
          console.log('Response data is an object with keys:', Object.keys(response.data));
          // 检查是否有data字段
          if (response.data.data) {
            console.log('Data field exists in response.data');
            if (Array.isArray(response.data.data)) {
              console.log('response.data.data is an array with', response.data.data.length, 'items');
              if (response.data.data.length > 0) {
                console.log('First item keys:', Object.keys(response.data.data[0]));
              }
            }
          }
        }
      }

      return response;
    })
    .catch(error => {
      console.error('getUserList API error:', error);
      throw error;
    });
};

export const getUserDetail = (username: string) => {
  return client.get('/api/user/info', { params: { username } });
};

export const createUser = (data: Omit<User, 'userId' | 'createTime' | 'lastLoginTime'>) => {
  return client.post('/api/user/add', data);
};

export const updateUser = (data: Partial<User>) => {
  return client.put('/api/user/update', data);
};

export const deleteUser = (username: string) => {
  return client.post('/api/user/remove', { username });
};

export const getUserPermissions = (username: string) => {
  return client.get('/api/user/permissions', { params: { username } });
};

export const updateUserPermissions = (username: string, permissions: string[]) => {
  return client.post('/api/user/permissions', { username, permissions });
};

export const changePassword = (username: string, oldPassword: string, newPassword: string) => {
  return client.get('/api/user/password', {
    params: {
      username,
      oldPassword,
      newPassword
    }
  });
};

export const resetPassword = (username: string) => {
  return client.get('/api/user/reset', { params: { username } });
};