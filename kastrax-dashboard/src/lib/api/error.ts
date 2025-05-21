import { AxiosError } from 'axios';

export class ApiError extends Error {
  code: number;
  success: boolean;
  data: any;

  constructor(message: string, code: number = 500, data?: any) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
    this.success = false;
    this.data = data;
  }
}

export class NetworkError extends ApiError {
  constructor(message: string = 'Network error occurred') {
    super(message, 0);
    this.name = 'NetworkError';
  }
}

export class AuthenticationError extends ApiError {
  constructor(message: string = 'Authentication failed') {
    super(message, 401);
    this.name = 'AuthenticationError';
  }
}

export class AuthorizationError extends ApiError {
  constructor(message: string = 'Authorization failed') {
    super(message, 403);
    this.name = 'AuthorizationError';
  }
}

export class NotFoundError extends ApiError {
  constructor(message: string = 'Resource not found') {
    super(message, 404);
    this.name = 'NotFoundError';
  }
}

export class ValidationError extends ApiError {
  constructor(message: string = 'Validation failed', data?: any) {
    super(message, 400, data);
    this.name = 'ValidationError';
  }
}

export class ServerError extends ApiError {
  constructor(message: string = 'Internal server error') {
    super(message, 500);
    this.name = 'ServerError';
  }
}

export interface RetryConfig {
  maxRetries?: number;
  retryDelay?: number;
  shouldRetry?: (error: Error) => boolean;
}

const defaultRetryConfig: Required<RetryConfig> = {
  maxRetries: 3,
  retryDelay: 1000,
  shouldRetry: (error: Error) => {
    if (error instanceof NetworkError) return true;
    if (error instanceof ServerError) return true;
    if (error instanceof ApiError) return error.code >= 500;
    return false;
  }
};

export async function withRetry<T>(
  operation: () => Promise<T>,
  config: RetryConfig = {}
): Promise<T> {
  const finalConfig = { ...defaultRetryConfig, ...config };
  let lastError: Error;
  
  for (let attempt = 0; attempt <= finalConfig.maxRetries; attempt++) {
    try {
      return await operation();
    } catch (error) {
      lastError = error as Error;
      
      if (attempt === finalConfig.maxRetries) break;
      if (!finalConfig.shouldRetry(lastError)) throw lastError;
      
      await new Promise(resolve => setTimeout(resolve, finalConfig.retryDelay));
    }
  }
  
  throw lastError!;
}

export function handleAxiosError(error: AxiosError): never {
  if (!error.response) {
    throw new NetworkError(error.message);
  }

  const status = error.response.status;
  const data = error.response.data as any;
  const message = data?.msg || error.message;

  switch (status) {
    case 400:
      throw new ValidationError(message, data);
    case 401:
      throw new AuthenticationError(message);
    case 403:
      throw new AuthorizationError(message);
    case 404:
      throw new NotFoundError(message);
    case 500:
    default:
      throw new ServerError(message);
  }
} 