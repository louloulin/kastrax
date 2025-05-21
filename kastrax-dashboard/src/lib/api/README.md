# API 适配层

这个目录包含了 Dataflare-UI 的 API 适配层，用于将 Dataflare-UI 的 API 调用转换为 Datalink-UI 后端能够理解的格式。

## 目录结构

```
src/lib/api/
├── adapter/                 # 适配层核心目录
│   ├── index.ts             # 适配层入口
│   ├── auth-adapter.ts      # 认证适配器
│   ├── rule-adapter.ts      # 规则适配器
│   ├── resource-adapter.ts  # 资源适配器
│   ├── script-adapter.ts    # 脚本适配器
│   ├── system-adapter.ts    # 系统适配器
│   ├── transform.ts         # 参数转换工具
│   └── response.ts          # 响应处理工具
├── config/                  # 配置目录
│   ├── api-mapping.ts       # API 路径映射配置
│   └── feature-flags.ts     # 功能特性开关
├── v1/                      # v1 API 实现
├── v2/                      # v2 API 实现
├── __tests__/               # 测试目录
├── __mocks__/               # 模拟目录
├── auth.ts                  # 认证 API 类型定义
├── rules.ts                 # 规则 API 类型定义
├── types.ts                 # 通用类型定义
├── base.ts                  # 基础工具函数
├── client.ts                # HTTP 客户端
└── index.ts                 # API 入口
```

## 使用方法

### 1. 导入 API 函数

```typescript
// 使用 v1 API
import { getRuleList, getRuleById } from '@/lib/api/v1/rules';

// 使用 v2 API
import { getRuleList, getRuleById } from '@/lib/api/v2/rules';
```

### 2. 调用 API 函数

```typescript
// 获取规则列表
const fetchRules = async () => {
  try {
    const response = await getRuleList({ page: 1, size: 10 });
    if (response.success) {
      // 处理成功响应
      console.log(response.data);
    } else {
      // 处理错误
      console.error(response.msg);
    }
  } catch (error) {
    // 处理异常
    console.error('Failed to fetch rules:', error);
  }
};

// 获取规则详情
const fetchRuleDetail = async (ruleId: string) => {
  try {
    const response = await getRuleById(ruleId);
    if (response.success) {
      // 处理成功响应
      console.log(response.data);
    } else {
      // 处理错误
      console.error(response.msg);
    }
  } catch (error) {
    // 处理异常
    console.error(`Failed to fetch rule ${ruleId}:`, error);
  }
};
```

## API 版本

### v1 API

v1 API 提供了基本的 API 功能，响应格式简单，适合大多数场景。

```typescript
// 响应格式
{
  code: number;      // 状态码
  success: boolean;  // 是否成功
  msg: string;       // 消息
  data: T;           // 数据
}
```

### v2 API

v2 API 提供了更丰富的功能，包括分页、排序、过滤等，适合复杂场景。

```typescript
// 分页响应格式
{
  code: number;      // 状态码
  success: boolean;  // 是否成功
  msg: string;       // 消息
  data: {
    list: T[];       // 数据列表
    total: number;   // 总数
    page: number;    // 当前页码
    size: number;    // 每页大小
  }
}
```

## 适配层工作原理

适配层的主要工作是将 Dataflare-UI 的 API 调用转换为 Datalink-UI 后端能够理解的格式，主要包括以下几个方面：

1. **API 路径映射**：将 RESTful 风格的 API 路径映射到 RPC 风格的 API 路径
2. **HTTP 方法转换**：将 GET、POST、PUT、DELETE 等 HTTP 方法转换为 Datalink-UI 后端期望的方法
3. **参数转换**：将 Dataflare-UI 的参数格式转换为 Datalink-UI 后端期望的格式
4. **响应转换**：将 Datalink-UI 后端的响应格式转换为 Dataflare-UI 期望的格式
5. **功能模拟**：对于 Datalink-UI 后端不支持的功能，提供模拟实现

## 配置

### API 映射配置

API 映射配置定义了 Dataflare-UI 和 Datalink-UI 之间的 API 路径映射关系，位于 `config/api-mapping.ts` 文件中。

```typescript
// 示例
export const apiMapping = {
  rule: {
    list: {
      v1: '/api/v1/rules',
      v2: '/api/v2/rules',
      legacy: '/api/rule/list'
    },
    // 其他 API...
  }
};
```

### 功能特性开关

功能特性开关用于控制高级功能的降级处理，位于 `config/feature-flags.ts` 文件中。

```typescript
// 示例
export const featureFlags = {
  scriptDebug: false,        // 脚本调试功能
  scriptDependencies: false, // 脚本依赖管理
  // 其他功能...
};
```

## 测试

适配层提供了全面的单元测试，位于 `__tests__` 目录中。

```bash
# 运行所有测试
npm test

# 运行特定测试
npm test -- src/lib/api/__tests__/rule-adapter.test.ts
```

## 贡献指南

### 添加新的 API

1. 在 `config/api-mapping.ts` 中添加新的 API 路径映射
2. 在相应的适配器文件中添加新的方法
3. 在 `v1` 和 `v2` 目录中添加新的 API 函数
4. 添加相应的测试

### 修改现有 API

1. 更新 `config/api-mapping.ts` 中的 API 路径映射
2. 更新相应的适配器方法
3. 更新 `v1` 和 `v2` 目录中的 API 函数
4. 更新相应的测试

## 常见问题

### 1. 如何处理 Datalink-UI 后端不支持的功能？

对于 Datalink-UI 后端不支持的功能，可以通过以下方式处理：

1. 使用功能特性开关控制功能的启用/禁用
2. 提供模拟实现，返回合理的默认值
3. 使用降级策略，使用类似的功能代替

### 2. 如何处理 API 错误？

适配层提供了统一的错误处理机制，会将 Datalink-UI 后端的错误转换为 Dataflare-UI 期望的格式。

```typescript
// 错误响应格式
{
  code: number;      // 错误码
  success: false;    // 始终为 false
  msg: string;       // 错误消息
  data: null;        // 始终为 null
}
```

### 3. 如何处理认证？

适配层会自动处理认证相关的逻辑，包括：

1. 在请求头中添加 token
2. 处理认证失败的情况
3. 在登录成功后存储 token
4. 在登出时清除 token

## 更多资源

- [API 接口对比分析报告](../../gj.md)
- [Datalink-UI 文档](https://github.com/example/datalink-ui)
- [Dataflare-UI 文档](https://github.com/example/dataflare-ui)
