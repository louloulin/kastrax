/**
 * Client JWT Tests
 */
import axios from 'axios';
import { createJwtToken } from '../../utils/jwt-utils';
import { handleApiError } from '../client';

// Mock axios
jest.mock('axios');
const mockedAxios = axios as jest.Mocked<typeof axios>;

// Mock localStorage
const localStorageMock = (() => {
  let store: Record<string, string> = {};
  return {
    getItem: (key: string) => store[key] || null,
    setItem: (key: string, value: string) => {
      store[key] = value;
    },
    removeItem: (key: string) => {
      delete store[key];
    },
    clear: () => {
      store = {};
    }
  };
})();
Object.defineProperty(window, 'localStorage', { value: localStorageMock });

describe('Client JWT Error Handling', () => {
  beforeEach(() => {
    localStorageMock.clear();
    jest.clearAllMocks();
  });
  
  it('should handle JWT signature errors and create a new token', () => {
    // Setup
    const username = 'testuser';
    localStorageMock.setItem('username', username);
    
    // Create a mock error response
    const mockError = {
      isAxiosError: true,
      response: {
        status: 500,
        data: {
          timestamp: '2025-04-23T06:27:53.299+00:00',
          status: 500,
          error: 'Internal Server Error',
          message: 'java.rmi.AccessException: JWT signature does not match locally computed signature. JWT validity cannot be asserted and should not be trusted.',
          path: '/api/auth/user/info'
        }
      },
      config: {
        url: '/api/auth/user/info',
        headers: {}
      }
    };
    
    // Call the error handler
    const result = handleApiError(mockError);
    
    // Verify a new token was created
    expect(result).toBeDefined();
    expect(result.shouldRetry).toBe(true);
    expect(result.config).toBe(mockError.config);
    expect(result.newToken).toBeDefined();
    
    // Verify the token was stored in localStorage
    const storedToken = localStorageMock.getItem('token');
    expect(storedToken).toBe(result.newToken);
  });
  
  it('should handle other authentication errors', () => {
    // Setup
    const username = 'testuser';
    localStorageMock.setItem('username', username);
    localStorageMock.setItem('token', 'old-token');
    
    // Create a mock error response for a non-JWT error
    const mockError = {
      isAxiosError: true,
      response: {
        status: 401,
        data: {
          error: 'Unauthorized',
          message: 'Invalid credentials'
        }
      },
      config: {
        url: '/api/some/endpoint',
        headers: {}
      }
    };
    
    // Mock window.location
    const originalLocation = window.location;
    delete window.location;
    window.location = { ...originalLocation, href: '' } as any;
    
    // Call the error handler
    handleApiError(mockError);
    
    // Verify localStorage was cleared
    expect(localStorageMock.getItem('token')).toBeNull();
    expect(localStorageMock.getItem('username')).toBeNull();
    
    // Restore window.location
    window.location = originalLocation;
  });
});
