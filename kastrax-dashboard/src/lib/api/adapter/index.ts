/**
 * API 适配层
 * 
 * 这个模块提供了 Dataflare-UI 和 Datalink-UI 后端之间的适配层，
 * 负责将 Dataflare-UI 的 API 调用转换为 Datalink-UI 后端能够理解的格式。
 */

// 导出所有适配器
export * from './auth-adapter';
export * from './rule-adapter';
export * from './resource-adapter';
export * from './script-adapter';
export * from './system-adapter';

// 导出工具函数
export * from './transform';
export * from './response';
