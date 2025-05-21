import { rest } from 'msw';
import { API_ENDPOINTS } from '../lib/api/types';
import { mockRules } from './data/rules';
import { mockScripts } from './data/scripts';
import { mockWorkflows } from './data/workflows';
import { mockUsers } from './data/users';

/**
 * MSW API模拟处理程序
 */
export const handlers = [
  // 认证相关
  rest.post(API_ENDPOINTS.AUTH.LOGIN, (req, res, ctx) => {
    const { username, password } = req.body as { username: string; password: string };

    // 简单认证逻辑
    const user = mockUsers.find(u => u.username === username);
    if (user && password === 'test-password') { // 在真实环境下应该验证哈希密码
      return res(
        ctx.status(200),
        ctx.json({
          code: 200,
          success: true,
          msg: '登录成功',
          data: {
            token: 'mock-jwt-token',
            user
          }
        })
      );
    }

    return res(
      ctx.status(401),
      ctx.json({
        code: 401,
        success: false,
        msg: '用户名或密码错误',
        data: null
      })
    );
  }),

  rest.get(API_ENDPOINTS.AUTH.USER_INFO, (req, res, ctx) => {
    // 检查认证头
    const authHeader = req.headers.get('Authorization');
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res(
        ctx.status(401),
        ctx.json({
          code: 401,
          success: false,
          msg: '未授权访问',
          data: null
        })
      );
    }

    return res(
      ctx.status(200),
      ctx.json({
        code: 200,
        success: true,
        msg: 'success',
        data: mockUsers[0] // 返回第一个用户作为当前用户
      })
    );
  }),

  // 规则相关
  rest.get(API_ENDPOINTS.RULES.BASE, (req, res, ctx) => {
    // 获取查询参数
    const page = parseInt(req.url.searchParams.get('page') || '1');
    const size = parseInt(req.url.searchParams.get('size') || '10');
    const ruleName = req.url.searchParams.get('ruleName');
    const type = req.url.searchParams.get('type');
    const status = req.url.searchParams.get('status');

    // 筛选规则
    let filteredRules = [...mockRules];

    if (ruleName) {
      filteredRules = filteredRules.filter(rule =>
        rule.ruleName.toLowerCase().includes(ruleName.toLowerCase())
      );
    }

    if (type) {
      filteredRules = filteredRules.filter(rule => rule.type === type);
    }

    if (status) {
      filteredRules = filteredRules.filter(rule => rule.status === status);
    }

    // 分页
    const start = (page - 1) * size;
    const end = start + size;
    const pagedRules = filteredRules.slice(start, end);

    return res(
      ctx.status(200),
      ctx.json({
        code: 200,
        success: true,
        msg: 'success',
        data: {
          list: pagedRules,
          total: filteredRules.length,
          page,
          size
        }
      })
    );
  }),

  rest.get(`${API_ENDPOINTS.RULES.BASE}/:ruleId`, (req, res, ctx) => {
    const { ruleId } = req.params;
    const rule = mockRules.find(r => r.ruleId === ruleId);

    if (rule) {
      return res(
        ctx.status(200),
        ctx.json({
          code: 200,
          success: true,
          msg: 'success',
          data: rule
        })
      );
    }

    return res(
      ctx.status(404),
      ctx.json({
        code: 404,
        success: false,
        msg: '规则不存在',
        data: null
      })
    );
  }),

  rest.post(API_ENDPOINTS.RULES.BASE, (req, res, ctx) => {
    const ruleData = req.body as any;

    // 创建新规则
    const newRule = {
      ruleId: `rule-${Date.now()}`,
      ...ruleData,
      version: 1,
      createTime: new Date().toISOString(),
      updateTime: new Date().toISOString()
    };

    // 添加到模拟数据集
    mockRules.push(newRule);

    return res(
      ctx.status(200),
      ctx.json({
        code: 200,
        success: true,
        msg: '规则创建成功',
        data: { ruleId: newRule.ruleId }
      })
    );
  }),

  // 脚本相关
  rest.get(API_ENDPOINTS.SCRIPTS.BASE, (req, res, ctx) => {
    // 分页参数
    const page = parseInt(req.url.searchParams.get('page') || '1');
    const size = parseInt(req.url.searchParams.get('size') || '10');
    const scriptName = req.url.searchParams.get('scriptName');
    const language = req.url.searchParams.get('language');

    // 筛选脚本
    let filteredScripts = [...mockScripts];

    if (scriptName) {
      filteredScripts = filteredScripts.filter(script =>
        script.scriptName.toLowerCase().includes(scriptName.toLowerCase())
      );
    }

    if (language) {
      filteredScripts = filteredScripts.filter(script => script.language === language);
    }

    // 分页
    const start = (page - 1) * size;
    const end = start + size;
    const pagedScripts = filteredScripts.slice(start, end);

    return res(
      ctx.status(200),
      ctx.json({
        code: 200,
        success: true,
        msg: 'success',
        data: {
          list: pagedScripts,
          total: filteredScripts.length,
          page,
          size
        }
      })
    );
  }),

  // 工作流相关
  rest.get(API_ENDPOINTS.WORKFLOWS.BASE, (req, res, ctx) => {
    // 分页参数
    const page = parseInt(req.url.searchParams.get('page') || '1');
    const size = parseInt(req.url.searchParams.get('size') || '10');
    const workflowName = req.url.searchParams.get('workflowName');
    const status = req.url.searchParams.get('status');

    // 筛选工作流
    let filteredWorkflows = [...mockWorkflows];

    if (workflowName) {
      filteredWorkflows = filteredWorkflows.filter(workflow =>
        workflow.workflowName.toLowerCase().includes(workflowName.toLowerCase())
      );
    }

    if (status) {
      filteredWorkflows = filteredWorkflows.filter(workflow => workflow.status === status);
    }

    // 分页
    const start = (page - 1) * size;
    const end = start + size;
    const pagedWorkflows = filteredWorkflows.slice(start, end);

    return res(
      ctx.status(200),
      ctx.json({
        code: 200,
        success: true,
        msg: 'success',
        data: {
          list: pagedWorkflows,
          total: filteredWorkflows.length,
          page,
          size
        }
      })
    );
  }),

  // Legacy API endpoints for Datalink-UI compatibility
  rest.get('/api/auth/user/info', (req, res, ctx) => {
    // 检查认证头
    const authHeader = req.headers.get('Authorization');
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res(
        ctx.status(401),
        ctx.json({
          code: 401,
          success: false,
          msg: '未授权访问',
          data: null
        })
      );
    }

    // 检查 token 格式
    const token = authHeader.substring(7);
    if (token.split('.').length !== 3) {
      return res(
        ctx.status(401),
        ctx.json({
          code: 401,
          success: false,
          msg: 'JWT strings must contain exactly 2 period characters',
          data: null
        })
      );
    }

    // 尝试从 token 中提取用户名
    let username = 'admin';
    try {
      const payload = token.split('.')[1];
      const decodedPayload = JSON.parse(atob(payload));
      username = decodedPayload.sub || decodedPayload.name || 'admin';
    } catch (error) {
      console.warn('[MSW] Failed to decode JWT payload:', error);
    }

    return res(
      ctx.status(200),
      ctx.json({
        code: 200,
        success: true,
        msg: 'success',
        data: {
          userId: `user-${Date.now()}`,
          username: username,
          email: `${username}@example.com`,
          role: 'admin',
          status: 1,
          createTime: new Date().toISOString(),
          permissions: ['all']
        }
      })
    );
  }),

  rest.post('/api/auth/user/login', (req, res, ctx) => {
    // 获取请求体
    const body = req.body as any;
    const username = body?.username || 'admin';

    // 创建标准的 JWT token
    const token = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbiIsIm5hbWUiOiJhZG1pbiIsImlhdCI6MTUxNjIzOTAyMn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c';

    return res(
      ctx.status(200),
      ctx.json({
        code: 200,
        success: true,
        msg: '登录成功',
        data: {
          token: token,
          accessToken: token,
          username: username,
          permissions: ['all']
        }
      })
    );
  }),

  rest.post('/api/resource/list', (req, res, ctx) => {
    return res(
      ctx.status(200),
      ctx.json({
        code: 200,
        success: true,
        msg: 'success',
        data: [
          {
            resourceId: 'resource-1',
            resourceName: 'MySQL Database',
            resourceType: 'MYSQL',
            status: 1,
            properties: {
              ip: 'localhost',
              port: 3306,
              databaseName: 'test_db',
              username: 'root',
              password: '******'
            },
            createTime: new Date().toISOString(),
            updateTime: new Date().toISOString()
          },
          {
            resourceId: 'resource-2',
            resourceName: 'MQTT Broker',
            resourceType: 'MQTT',
            status: 1,
            properties: {
              url: 'mqtt://localhost:1883',
              topic: 'test/topic'
            },
            createTime: new Date().toISOString(),
            updateTime: new Date().toISOString()
          }
        ]
      })
    );
  }),

  // User Management Endpoints
  // 注释掉 /api/user/list 处理程序，让请求通过到真实后端
  // rest.post('/api/user/list', (req, res, ctx) => {
  //   // Get filter parameters from request body
  //   const body = req.body as any;
  //   let filteredUsers = [...mockUsers];
  //
  //   // Apply filters if provided
  //   if (body && body.username) {
  //     filteredUsers = filteredUsers.filter(user =>
  //       user.username.toLowerCase().includes(body.username.toLowerCase())
  //     );
  //   }
  //
  //   if (body && body.description) {
  //     filteredUsers = filteredUsers.filter(user =>
  //       user.description && user.description.toLowerCase().includes(body.description.toLowerCase())
  //     );
  //   }
  //
  //   return res(
  //     ctx.status(200),
  //     ctx.json({
  //       code: 200,
  //       success: true,
  //       msg: 'success',
  //       data: filteredUsers
  //     })
  //   );
  // }),

  rest.get('/api/user/info', (req, res, ctx) => {
    const username = req.url.searchParams.get('username');
    if (!username) {
      return res(
        ctx.status(400),
        ctx.json({
          code: 400,
          success: false,
          msg: '缺少用户名参数',
          data: null
        })
      );
    }

    const user = mockUsers.find(u => u.username === username);
    if (!user) {
      return res(
        ctx.status(404),
        ctx.json({
          code: 404,
          success: false,
          msg: '用户不存在',
          data: null
        })
      );
    }

    return res(
      ctx.status(200),
      ctx.json({
        code: 200,
        success: true,
        msg: 'success',
        data: user
      })
    );
  }),

  rest.post('/api/user/add', (req, res, ctx) => {
    const userData = req.body as any;

    // Validate required fields
    if (!userData.username || !userData.password) {
      return res(
        ctx.status(400),
        ctx.json({
          code: 400,
          success: false,
          msg: '用户名和密码不能为空',
          data: null
        })
      );
    }

    // Check if username already exists
    if (mockUsers.some(u => u.username === userData.username)) {
      return res(
        ctx.status(400),
        ctx.json({
          code: 400,
          success: false,
          msg: '用户名已存在',
          data: null
        })
      );
    }

    // Create a new user with default values for missing fields
    const newUser = {
      userId: `user-${Date.now()}`,
      username: userData.username,
      email: userData.email || `${userData.username}@example.com`,
      role: userData.role || 'user',
      status: userData.status !== undefined ? userData.status : 1,
      description: userData.description || '',
      permissions: userData.permissions || ['dashboard'],
      createTime: new Date().toISOString(),
      system: userData.system || false
    };

    // Add to mock users array
    mockUsers.push(newUser as any);

    return res(
      ctx.status(200),
      ctx.json({
        code: 200,
        success: true,
        msg: '用户创建成功',
        data: null
      })
    );
  }),

  rest.put('/api/user/update', (req, res, ctx) => {
    const userData = req.body as any;

    // Validate username
    if (!userData.username) {
      return res(
        ctx.status(400),
        ctx.json({
          code: 400,
          success: false,
          msg: '缺少用户名',
          data: null
        })
      );
    }

    // Find user index
    const userIndex = mockUsers.findIndex(u => u.username === userData.username);
    if (userIndex === -1) {
      return res(
        ctx.status(404),
        ctx.json({
          code: 404,
          success: false,
          msg: '用户不存在',
          data: null
        })
      );
    }

    // Update user data (except username which is the key)
    const updatedUser = { ...mockUsers[userIndex], ...userData };
    mockUsers[userIndex] = updatedUser as any;

    return res(
      ctx.status(200),
      ctx.json({
        code: 200,
        success: true,
        msg: '用户更新成功',
        data: null
      })
    );
  }),

  rest.post('/api/user/remove', (req, res, ctx) => {
    const { username } = req.body as { username: string };

    // Validate username
    if (!username) {
      return res(
        ctx.status(400),
        ctx.json({
          code: 400,
          success: false,
          msg: '缺少用户名',
          data: null
        })
      );
    }

    // Find user
    const userIndex = mockUsers.findIndex(u => u.username === username);
    if (userIndex === -1) {
      return res(
        ctx.status(404),
        ctx.json({
          code: 404,
          success: false,
          msg: '用户不存在',
          data: null
        })
      );
    }

    // Check if it's a system user
    if (mockUsers[userIndex].system) {
      return res(
        ctx.status(403),
        ctx.json({
          code: 403,
          success: false,
          msg: '系统用户不能删除',
          data: null
        })
      );
    }

    // Remove user
    mockUsers.splice(userIndex, 1);

    return res(
      ctx.status(200),
      ctx.json({
        code: 200,
        success: true,
        msg: '用户删除成功',
        data: null
      })
    );
  }),

  rest.get('/api/user/permissions', (req, res, ctx) => {
    const username = req.url.searchParams.get('username');
    if (!username) {
      return res(
        ctx.status(400),
        ctx.json({
          code: 400,
          success: false,
          msg: '缺少用户名参数',
          data: null
        })
      );
    }

    const user = mockUsers.find(u => u.username === username);
    if (!user) {
      return res(
        ctx.status(404),
        ctx.json({
          code: 404,
          success: false,
          msg: '用户不存在',
          data: null
        })
      );
    }

    return res(
      ctx.status(200),
      ctx.json({
        code: 200,
        success: true,
        msg: 'success',
        data: user.permissions
      })
    );
  }),

  rest.post('/api/user/permissions', (req, res, ctx) => {
    const { username, permissions } = req.body as { username: string, permissions: string[] };

    // Validate parameters
    if (!username || !permissions) {
      return res(
        ctx.status(400),
        ctx.json({
          code: 400,
          success: false,
          msg: '缺少必要参数',
          data: null
        })
      );
    }

    // Find user
    const userIndex = mockUsers.findIndex(u => u.username === username);
    if (userIndex === -1) {
      return res(
        ctx.status(404),
        ctx.json({
          code: 404,
          success: false,
          msg: '用户不存在',
          data: null
        })
      );
    }

    // Update permissions
    mockUsers[userIndex].permissions = permissions;

    return res(
      ctx.status(200),
      ctx.json({
        code: 200,
        success: true,
        msg: '权限更新成功',
        data: null
      })
    );
  }),

  rest.get('/api/user/reset', (req, res, ctx) => {
    const username = req.url.searchParams.get('username');
    if (!username) {
      return res(
        ctx.status(400),
        ctx.json({
          code: 400,
          success: false,
          msg: '缺少用户名参数',
          data: null
        })
      );
    }

    const user = mockUsers.find(u => u.username === username);
    if (!user) {
      return res(
        ctx.status(404),
        ctx.json({
          code: 404,
          success: false,
          msg: '用户不存在',
          data: null
        })
      );
    }

    return res(
      ctx.status(200),
      ctx.json({
        code: 200,
        success: true,
        msg: '密码重置成功，新密码已发送到用户邮箱',
        data: null
      })
    );
  }),

  rest.get('/api/user/password', (req, res, ctx) => {
    const username = req.url.searchParams.get('username');
    const oldPassword = req.url.searchParams.get('oldPassword');
    const newPassword = req.url.searchParams.get('newPassword');

    // Validate parameters
    if (!username || !oldPassword || !newPassword) {
      return res(
        ctx.status(400),
        ctx.json({
          code: 400,
          success: false,
          msg: '缺少必要参数',
          data: null
        })
      );
    }

    // Find user
    const user = mockUsers.find(u => u.username === username);
    if (!user) {
      return res(
        ctx.status(404),
        ctx.json({
          code: 404,
          success: false,
          msg: '用户不存在',
          data: null
        })
      );
    }

    // In a real implementation, we would verify the old password
    // Here we just simulate success

    return res(
      ctx.status(200),
      ctx.json({
        code: 200,
        success: true,
        msg: '密码修改成功',
        data: null
      })
    );
  }),

  // Font file handler
  rest.get('*/Inter-VariableFont_slnt,wght.ttf', (req, res, ctx) => {
    // Just pass through font requests - MSW doesn't need to mock binary files
    return res(ctx.status(200));
  }),

  // 通用错误处理示例
  rest.all('*', (req, res, ctx) => {
    console.warn(`未实现的mock路由: ${req.url.toString()}`);

    return res(
      ctx.status(501),
      ctx.json({
        code: 501,
        success: false,
        msg: '接口未实现',
        data: null
      })
    );
  })
];