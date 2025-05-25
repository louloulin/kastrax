/**
 * 功能特性开关
 * 
 * 这个配置文件定义了各种功能特性的开关状态，
 * 用于控制高级功能的降级处理。
 */

export const featureFlags = {
  // 脚本功能
  scriptDebug: false,        // 脚本调试功能
  scriptDependencies: false, // 脚本依赖管理
  
  // 规则功能
  ruleExecution: true,       // 规则执行
  ruleHistory: false,        // 规则执行历史
  
  // 资源功能
  resourceTypes: true,       // 资源类型列表
  resourceTest: true,        // 资源连接测试
  
  // 系统功能
  systemStatistics: true,    // 系统统计信息
  
  // 认证功能
  userNav: true,             // 用户导航菜单
};
