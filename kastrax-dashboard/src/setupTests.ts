import '@testing-library/jest-dom';
// 导入全局jest-dom断言以增强测试能力
// 这扩展了jest的expect方法，添加了toBeInTheDocument()等DOM相关的匹配器

// 为Jest环境提供TextEncoder和TextDecoder的polyfill
if (typeof TextEncoder === 'undefined') {
  global.TextEncoder = require('util').TextEncoder;
}

if (typeof TextDecoder === 'undefined') {
  global.TextDecoder = require('util').TextDecoder;
}

// 这个文件将在运行测试前自动被Jest加载 