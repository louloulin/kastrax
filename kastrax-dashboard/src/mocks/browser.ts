import { setupWorker } from 'msw';
import { handlers } from './handlers';

// 创建 Mock Service Worker 实例
export const worker = setupWorker(...handlers);

// 导出函数供入口文件调用
export async function setupMockServiceWorker() {
  try {
    console.log('[MSW] 正在启动模拟API服务...');

    // 检查服务器是否可用
    const publicUrl = window.location.origin;
    const workerUrl = `${publicUrl}/mockServiceWorker.js`;

    // 检查服务工作者文件是否存在
    try {
      const response = await fetch(workerUrl, { method: 'HEAD' });
      if (!response.ok) {
        throw new Error(`Service Worker script does not exist at ${workerUrl}`);
      }
      console.log('[MSW] Service Worker script found at', workerUrl);
    } catch (error) {
      console.warn('[MSW] Could not verify Service Worker script:', error);
      console.log('[MSW] Attempting to start worker anyway');
    }

    // 启动 worker
    await worker.start({
      onUnhandledRequest: 'bypass', // 不处理的请求直接通过
      serviceWorker: {
        url: '/mockServiceWorker.js',
      }
    });

    console.log('[MSW] 模拟API服务已启动成功');
  } catch (error) {
    console.error('[MSW] 启动失败:', error);
    throw error;
  }
}