import { AxiosError } from 'axios';
import {
  ApiError,
  NetworkError,
  AuthenticationError,
  AuthorizationError,
  NotFoundError,
  ValidationError,
  ServerError,
  withRetry,
  handleAxiosError
} from '../error';

describe('API Error Classes', () => {
  test('ApiError should be created with correct properties', () => {
    const error = new ApiError('Test error', 400, { field: 'test' });
    expect(error.message).toBe('Test error');
    expect(error.code).toBe(400);
    expect(error.success).toBe(false);
    expect(error.data).toEqual({ field: 'test' });
  });

  test('NetworkError should have correct default values', () => {
    const error = new NetworkError();
    expect(error.message).toBe('Network error occurred');
    expect(error.code).toBe(0);
    expect(error.name).toBe('NetworkError');
  });

  test('AuthenticationError should have correct default values', () => {
    const error = new AuthenticationError();
    expect(error.message).toBe('Authentication failed');
    expect(error.code).toBe(401);
    expect(error.name).toBe('AuthenticationError');
  });

  test('AuthorizationError should have correct default values', () => {
    const error = new AuthorizationError();
    expect(error.message).toBe('Authorization failed');
    expect(error.code).toBe(403);
    expect(error.name).toBe('AuthorizationError');
  });

  test('NotFoundError should have correct default values', () => {
    const error = new NotFoundError();
    expect(error.message).toBe('Resource not found');
    expect(error.code).toBe(404);
    expect(error.name).toBe('NotFoundError');
  });

  test('ValidationError should have correct default values', () => {
    const error = new ValidationError();
    expect(error.message).toBe('Validation failed');
    expect(error.code).toBe(400);
    expect(error.name).toBe('ValidationError');
  });

  test('ServerError should have correct default values', () => {
    const error = new ServerError();
    expect(error.message).toBe('Internal server error');
    expect(error.code).toBe(500);
    expect(error.name).toBe('ServerError');
  });
});

describe('withRetry', () => {
  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  test('should retry on network error', async () => {
    const operation = jest.fn()
      .mockRejectedValueOnce(new NetworkError())
      .mockResolvedValueOnce('success');

    const promise = withRetry(operation);
    jest.runAllTimers();
    const result = await promise;

    expect(result).toBe('success');
    expect(operation).toHaveBeenCalledTimes(2);
  });

  test('should not retry on validation error', async () => {
    const operation = jest.fn()
      .mockRejectedValue(new ValidationError());

    await expect(withRetry(operation)).rejects.toThrow(ValidationError);
    expect(operation).toHaveBeenCalledTimes(1);
  });

  test('should respect maxRetries config', async () => {
    const operation = jest.fn()
      .mockRejectedValue(new NetworkError());

    const promise = withRetry(operation, { maxRetries: 2 });
    jest.runAllTimers();
    await expect(promise).rejects.toThrow(NetworkError);

    expect(operation).toHaveBeenCalledTimes(3); // Initial + 2 retries
  });

  test('should respect retryDelay config', async () => {
    const operation = jest.fn()
      .mockRejectedValue(new NetworkError());

    const promise = withRetry(operation, { retryDelay: 2000 });
    jest.advanceTimersByTime(2000);
    try {
      await promise;
    } catch (error) {
      // Expected to fail
    }

    expect(setTimeout).toHaveBeenCalledWith(expect.any(Function), 2000);
  });
});

describe('handleAxiosError', () => {
  test('should handle network error', () => {
    const axiosError = new AxiosError();
    expect(() => handleAxiosError(axiosError)).toThrow(NetworkError);
  });

  test('should handle 400 error', () => {
    const axiosError = new AxiosError();
    axiosError.response = {
      status: 400,
      data: { msg: 'Bad request' }
    } as any;
    expect(() => handleAxiosError(axiosError)).toThrow(ValidationError);
  });

  test('should handle 401 error', () => {
    const axiosError = new AxiosError();
    axiosError.response = {
      status: 401,
      data: { msg: 'Unauthorized' }
    } as any;
    expect(() => handleAxiosError(axiosError)).toThrow(AuthenticationError);
  });

  test('should handle 403 error', () => {
    const axiosError = new AxiosError();
    axiosError.response = {
      status: 403,
      data: { msg: 'Forbidden' }
    } as any;
    expect(() => handleAxiosError(axiosError)).toThrow(AuthorizationError);
  });

  test('should handle 404 error', () => {
    const axiosError = new AxiosError();
    axiosError.response = {
      status: 404,
      data: { msg: 'Not found' }
    } as any;
    expect(() => handleAxiosError(axiosError)).toThrow(NotFoundError);
  });

  test('should handle 500 error', () => {
    const axiosError = new AxiosError();
    axiosError.response = {
      status: 500,
      data: { msg: 'Server error' }
    } as any;
    expect(() => handleAxiosError(axiosError)).toThrow(ServerError);
  });
}); 