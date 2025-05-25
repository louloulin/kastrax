/**
 * 参数转换工具
 *
 * 这个模块提供了各种参数转换函数，用于将 Dataflare-UI 的参数格式
 * 转换为 Datalink-UI 后端期望的格式。
 */

/**
 * 安全转换函数
 * 包裹转换函数，添加错误处理
 * @param transformFn 原始转换函数
 * @returns 包裹后的转换函数
 */
const safeTransform = (transformFn: (params: any) => any) => {
  return (params: any) => {
    try {
      return transformFn(params);
    } catch (error) {
      console.error('参数转换错误:', error);
      // 返回原始参数，确保请求不会因为转换错误而失败
      return params;
    }
  };
};

/**
 * 认证参数转换
 */
export const transformAuthParams = {
  login: safeTransform((params: any) => {
    const { username, password, ...rest } = params;
    return { username, password, ...rest };
  })
};

/**
 * 规则参数转换
 */
export const transformRuleParams = {
  list: safeTransform((params: any) => {
    // 处理分页参数，Datalink-UI 不需要分页参数
    const { page, size, ...rest } = params;
    return rest;
  }),

  create: safeTransform((params: any) => {
    const { graph, type, description, ...rest } = params;
    return {
      ...rest,
      graphJson: graph ? JSON.stringify(graph) : undefined,
      description: description || ''
    };
  }),

  update: safeTransform((params: any) => {
    const { ruleId, graph, ...rest } = params;
    return {
      ...rest,
      ruleId,
      graphJson: graph ? JSON.stringify(graph) : undefined
    };
  }),

  execute: safeTransform((params: any) => {
    const { ruleId, testData, ...rest } = params;
    return {
      ruleId,
      testData: testData || {},
      ...rest
    };
  })
};

/**
 * 资源参数转换
 */
export const transformResourceParams = {
  list: safeTransform((params: any) => {
    // 处理资源列表查询参数
    const { resourceType, resourceName, page, size, ...rest } = params;
    return {
      resourceType,
      resourceName,
      ...rest
    };
  }),

  create: safeTransform((params: any) => {
    // 处理资源创建参数
    const { properties, ...rest } = params;
    return {
      ...rest,
      properties: properties ? JSON.stringify(properties) : undefined
    };
  }),

  update: safeTransform((params: any) => {
    // 处理资源更新参数
    const { resourceId, properties, ...rest } = params;
    return {
      ...rest,
      resourceId,
      properties: properties ? JSON.stringify(properties) : undefined
    };
  }),

  test: safeTransform((params: any) => {
    // 处理资源测试参数
    const { properties, ...rest } = params;
    return {
      ...rest,
      properties: properties ? JSON.stringify(properties) : undefined
    };
  })
};

/**
 * 脚本参数转换
 */
export const transformScriptParams = {
  list: safeTransform((params: any) => {
    // 处理脚本列表查询参数
    const { page, size, ...rest } = params;
    return rest;
  }),

  create: safeTransform((params: any) => {
    // 处理脚本创建参数
    const { content, language, ...rest } = params;
    return {
      ...rest,
      scriptContent: content,
      scriptLanguage: language
    };
  }),

  update: safeTransform((params: any) => {
    // 处理脚本更新参数
    const { scriptId, content, language, ...rest } = params;
    return {
      ...rest,
      scriptId,
      scriptContent: content,
      scriptLanguage: language
    };
  }),

  test: safeTransform((params: any) => {
    // 处理脚本测试参数
    const { scriptId, testData, ...rest } = params;
    return {
      ...rest,
      scriptId,
      testData: testData || {}
    };
  })
};

/**
 * 系统参数转换
 */
export const transformSystemParams = {
  // 目前没有需要转换的系统参数
  info: safeTransform((params: any) => params),
  statistics: safeTransform((params: any) => params)
};
