import axios from 'axios';
import { createApiClient, handleApiError } from '../client';

// Mock axios
jest.mock('axios', () => ({
  create: jest.fn(() => ({
    get: jest.fn(),
    post: jest.fn(),
    put: jest.fn(),
    delete: jest.fn(),
    interceptors: {
      request: { use: jest.fn() },
      response: { use: jest.fn() },
    },
  })),
  isAxiosError: jest.fn(),
}));

// Mock window.location
const originalLocation = window.location;
// Replace location with a writable object
Object.defineProperty(window, 'location', {
  writable: true,
  value: { href: '' }
});

// Mock localStorage
const localStorageMock = {
  getItem: jest.fn(),
  setItem: jest.fn(),
  removeItem: jest.fn(),
};
Object.defineProperty(window, 'localStorage', { 
  value: localStorageMock,
  writable: true 
});

describe('API Client', () => {
  const mockedAxios = axios as jest.Mocked<typeof axios>;
  let mockClient: any;
  let requestInterceptor: Function;
  let requestErrorInterceptor: Function;
  let responseInterceptor: Function;
  let responseErrorInterceptor: Function;
  
  beforeEach(() => {
    jest.clearAllMocks();
    
    // Setup mock for axios.create
    mockClient = {
      interceptors: {
        request: { use: jest.fn() },
        response: { use: jest.fn() },
      },
    };
    mockedAxios.create.mockReturnValue(mockClient as any);
    
    // Create a client to test (which sets up interceptors)
    createApiClient();
    
    // Extract the interceptor functions
    [[requestInterceptor, requestErrorInterceptor]] = mockClient.interceptors.request.use.mock.calls;
    [[responseInterceptor, responseErrorInterceptor]] = mockClient.interceptors.response.use.mock.calls;
  });
  
  afterAll(() => {
    // Restore original location
    Object.defineProperty(window, 'location', {
      writable: true,
      value: originalLocation
    });
  });

  describe('createApiClient', () => {
    it('should create an axios instance with default configs', () => {
      // When
      createApiClient();
      
      // Then
      expect(mockedAxios.create).toHaveBeenCalledWith(expect.objectContaining({
        timeout: 10000,
        headers: {
          'Content-Type': 'application/json',
        },
      }));
    });
    
    it('should create an axios instance with custom configs', () => {
      // Given
      const customConfig = {
        timeout: 5000,
        baseURL: 'https://custom-api.com',
      };
      
      // When
      createApiClient(customConfig);
      
      // Then
      expect(mockedAxios.create).toHaveBeenCalledWith(expect.objectContaining({
        timeout: 5000,
        baseURL: 'https://custom-api.com',
        headers: {
          'Content-Type': 'application/json',
        },
      }));
    });
  });
  
  describe('request interceptor', () => {
    it('should add auth token to request headers', () => {
      // Given
      const config = { headers: {} };
      localStorageMock.getItem.mockReturnValue('test-token');
      
      // When
      const result = requestInterceptor(config);
      
      // Then
      expect(result.headers.Authorization).toBe('Bearer test-token');
      expect(result.metadata).toBeDefined();
      expect(result.metadata.startTime).toBeDefined();
    });
    
    it('should not add auth token if not available', () => {
      // Given
      const config = { headers: {} };
      localStorageMock.getItem.mockReturnValue(null);
      
      // When
      const result = requestInterceptor(config);
      
      // Then
      expect(result.headers.Authorization).toBeUndefined();
    });
  });
  
  describe('handleApiError', () => {
    it('should handle unauthorized error (401)', () => {
      // Given
      const error = {
        response: { status: 401 },
      };
      mockedAxios.isAxiosError.mockReturnValue(true);
      
      // When
      handleApiError(error);
      
      // Then
      expect(localStorageMock.removeItem).toHaveBeenCalledWith('token');
      expect(window.location.href).toBe('/login');
    });
    
    it('should handle server error (500)', () => {
      // Given
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
      const error = {
        response: { status: 500 },
      };
      mockedAxios.isAxiosError.mockReturnValue(true);
      
      // When
      handleApiError(error);
      
      // Then
      expect(consoleSpy).toHaveBeenCalledWith('服务器错误，请稍后重试');
      consoleSpy.mockRestore();
    });
    
    it('should handle network error', () => {
      // Given
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
      const error = {
        request: {},
        response: undefined,
      };
      mockedAxios.isAxiosError.mockReturnValue(true);
      
      // When
      handleApiError(error);
      
      // Then
      expect(consoleSpy).toHaveBeenCalledWith('网络异常，请检查网络连接');
      consoleSpy.mockRestore();
    });
  });
}); 