import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse, AxiosError, InternalAxiosRequestConfig } from 'axios';
import { getBaseUrl } from './base';
import logger, { LogCategory, LogLevel } from '../utils/logger';
import { jwtFixed } from '../utils/jwt-fixed';

export interface ApiResponse<T = any> {
  code: number;
  msg: string;
  data: T;
  success: boolean;
}

// 扩展 AxiosRequestConfig 添加自定义元数据
declare module 'axios' {
  export interface InternalAxiosRequestConfig {
    metadata?: {
      startTime: number;
      [key: string]: any;
    };
  }
}

/**
 * 统一的API错误处理函数
 */
export function handleApiError(error: any) {
  console.error('API Error:', error);

  if (axios.isAxiosError(error)) {
    const axiosError = error as AxiosError;
    if (axiosError.response) {
      const { status, data } = axiosError.response;
      const errorMsg = typeof data === 'string' ? data : data?.message || data?.error || '';

      // 检查是否是 JWT 相关错误
      const isJwtError = errorMsg.includes('JWT') ||
                         errorMsg.includes('signature') ||
                         errorMsg.includes('token') ||
                         errorMsg.includes('auth');

      // 如果是JWT签名错误或未授权错误，尝试创建新的token
      const isAuthError = isJwtError || error.response?.status === 401;

      if (isAuthError && localStorage.getItem('username')) {
        console.warn('[API Client] Auth error detected, will create new token');
        console.warn('[API Client] Error details:', errorMsg);
        console.warn('[API Client] Status code:', error.response?.status);

        // 打印当前使用的token
        const currentToken = localStorage.getItem('token');
        if (currentToken) {
          console.log('[API Client] Current token:', currentToken);
          try {
            const parts = currentToken.split('.');
            if (parts.length === 3) {
              console.log('[API Client] Token parts:', {
                header: parts[0],
                payload: parts[1],
                signature: parts[2]
              });
            }
          } catch (e) {
            console.error('[API Client] Error parsing token:', e);
          }
        }

        // 删除当前token
        localStorage.removeItem('token');
        // 在下一次请求中会自动创建新token
      }

      if ((status === 401 || status === 403)) {
        // 认证错误
        if (isJwtError) {
          console.warn('[API Client] JWT validation error:', errorMsg);
        } else {
          console.warn('[API Client] Authentication error:', status === 401 ? 'Unauthorized' : 'Forbidden');
        }

        // 清除所有认证相关数据
        localStorage.removeItem('token');
        localStorage.removeItem('username');
        localStorage.removeItem('auth-storage');

        // 重定向到登录页
        if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
          window.location.href = '/login';
        }
      } else if (status === 404) {
        // 处理资源不存在
        console.error('请求的资源不存在');
      } else if (status >= 500) {
        // 处理服务器错误
        console.error('服务器错误，请稍后重试');
      }
    } else if (axiosError.request) {
      // 请求发出但未收到响应
      console.error('网络异常，请检查网络连接');
    } else {
      // 请求设置异常
      console.error('请求错误', axiosError.message);
    }
  } else {
    // 非 Axios 错误
    console.error('未知错误', error);
  }

  return error;
}

/**
 * 创建可配置的API客户端
 * @param config Axios 配置
 * @returns Axios 实例
 */
export function createApiClient(config?: AxiosRequestConfig): AxiosInstance {
  // 创建 axios 实例
  const client = axios.create({
    baseURL: getBaseUrl(),
    timeout: 10000,
    headers: {
      'Content-Type': 'application/json',
    },
    ...config,
  });

  // 在测试环境中跳过拦截器设置
  if (process.env.NODE_ENV !== 'test') {
    // 请求拦截器
    client.interceptors.request.use(
      (config: InternalAxiosRequestConfig) => {
        // 开始计时以便监控请求持续时间
        const startTime = new Date().getTime();
        config.metadata = { startTime };

        // 获取请求信息
        const method = config.method?.toUpperCase() || 'UNKNOWN';
        const url = config.url || 'UNKNOWN';
        const fullUrl = config.baseURL ? `${config.baseURL}${url}` : url;

        // 获取请求参数
        let requestParams = {};
        if (method === 'GET' || method === 'DELETE') {
          requestParams = config.params || {};
        } else if (config.data) {
          requestParams = config.data;
        }

        // 设置认证头
        // 获取并验证 token
        const storedToken = localStorage.getItem('token');
        const username = localStorage.getItem('username');

        // 如果有存储的 token，验证其有效性
        if (storedToken) {
          try {
            // 解码token查看内容
            const decodedToken = jwtFixed.decodeToken(storedToken);
            console.log('[API Client] Token payload:', decodedToken);

            // 检查token是否过期
            if (!jwtFixed.isTokenExpired(storedToken)) {
              // 尝试两种认证头格式，以兼容不同的后端要求
              config.headers.Authorization = `Bearer ${storedToken}`;
              config.headers['X-Token'] = storedToken;

              // 添加调试信息
              console.log('[API Client] Using valid token:', storedToken.substring(0, 20) + '...');
              return config;
            } else {
              console.warn('[API Client] Token is expired, will create new one');
            }
          } catch (error) {
            console.error('[API Client] Error decoding token:', error);
          }
        }

        // 如果没有有效的 token，尝试创建新的 token
        if (username) {
          // 异步创建新的 token
          console.log('[API Client] Creating new token for user:', username);
          return jwtFixed.createToken(username)
            .then(newToken => {
              // 存储新的 token
              localStorage.setItem('token', newToken);

              // 设置认证头
              config.headers.Authorization = `Bearer ${newToken}`;
              config.headers['X-Token'] = newToken;

              // 验证新创建的token
              try {
                const decodedToken = jwtFixed.decodeToken(newToken);
                console.log('[API Client] New token payload:', decodedToken);

                // 打印token的各部分
                const parts = newToken.split('.');
                if (parts.length === 3) {
                  console.log('[API Client] New token parts:', {
                    header: parts[0],
                    payload: parts[1],
                    signature: parts[2]
                  });
                }
              } catch (error) {
                console.error('[API Client] Error decoding new token:', error);
              }

              console.log('[API Client] Created and using new token:', newToken.substring(0, 20) + '...');
              return config;
            })
            .catch(error => {
              console.error('[API Client] Failed to create new token:', error);
              return config;
            });
        } else {
          console.warn('[API Client] No token available and no username to create one');
        }

        // 打印请求日志
        logger.group(`🚀 API Request: ${method} ${fullUrl}`);
        logger.info(LogCategory.API, `${method} ${fullUrl}`, {
          url: fullUrl,
          method,
          headers: config.headers,
          params: requestParams,
          timestamp: new Date().toISOString()
        });
        logger.groupEnd();

        return config;
      },
      (error) => {
        logger.error(LogCategory.API, 'Request error', error);
        return Promise.reject(error);
      }
    );

    // 响应拦截器
    client.interceptors.response.use(
      (response: AxiosResponse<ApiResponse>) => {
        // 获取请求信息
        const method = response.config.method?.toUpperCase() || 'UNKNOWN';
        const url = response.config.url || 'UNKNOWN';
        const fullUrl = response.config.baseURL ? `${response.config.baseURL}${url}` : url;

        // 计算请求持续时间
        let duration = 0;
        if (response.config.metadata) {
          duration = new Date().getTime() - response.config.metadata.startTime;
        }

        const res = response.data;
        const status = response.status;

        // 打印响应日志
        logger.group(`💯 API Response: ${method} ${fullUrl} (${status}) - ${duration}ms`);
        logger.info(LogCategory.API, `Response: ${method} ${fullUrl}`, {
          url: fullUrl,
          method,
          status,
          duration: `${duration}ms`,
          headers: response.headers,
          data: res,
          timestamp: new Date().toISOString()
        });
        logger.groupEnd();

        // 处理API错误码
        if (!res.success && res.code !== 200) {
          logger.group(`❌ API Error: ${method} ${fullUrl}`);
          logger.error(LogCategory.API, `Error Code: ${res.code}, Message: ${res.msg}`, {
            url: fullUrl,
            method,
            errorCode: res.code,
            errorMessage: res.msg,
            timestamp: new Date().toISOString()
          });
          logger.groupEnd();

          // 处理特定错误码
          if ((res.code === 401 || res.code === 403) && typeof window !== 'undefined') {
            logger.warn(LogCategory.AUTH, 'Authentication error, redirecting to login');
            // Token过期或未授权
            localStorage.removeItem('token');
            localStorage.removeItem('username');
            localStorage.removeItem('auth-storage');
            window.location.href = '/login';
            return Promise.reject(new Error('Authentication failed: ' + (res.msg || 'Unauthorized')));
          }
          return Promise.reject(new Error(res.msg || 'API error'));
        }
        return response;
      },
      (error) => {
        // 获取请求信息
        const config = error.config || {};
        const method = config.method?.toUpperCase() || 'UNKNOWN';
        const url = config.url || 'UNKNOWN';
        const fullUrl = config.baseURL ? `${config.baseURL}${url}` : url;

        // 计算请求持续时间
        let duration = 0;
        if (config.metadata) {
          duration = new Date().getTime() - config.metadata.startTime;
        }

        // 打印错误日志
        logger.group(`❌ API Error: ${method} ${fullUrl} - ${duration}ms`);

        const errorData: any = {
          url: fullUrl,
          method,
          duration: `${duration}ms`,
          timestamp: new Date().toISOString(),
          error: error.message
        };

        if (axios.isAxiosError(error)) {
          if (error.response) {
            errorData.status = error.response.status;
            errorData.headers = error.response.headers;
            errorData.data = error.response.data;
            errorData.type = 'Response Error';
          } else if (error.request) {
            errorData.type = 'Request Error (No Response)';
            errorData.request = error.request;
          } else {
            errorData.type = 'Request Setup Error';
          }
        }

        logger.error(LogCategory.API, `Error: ${method} ${fullUrl}`, errorData);
        logger.groupEnd();

        // 处理响应错误
        handleApiError(error);
        return Promise.reject(error);
      }
    );
  }

  return client;
}

// 默认客户端实例
const defaultClient = createApiClient();
export default defaultClient;