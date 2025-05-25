import axios from 'axios';
import { getRequest, postRequest, putRequest, deleteRequest } from '../utils';

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

describe('API Utils', () => {
  const mockedAxios = axios as jest.Mocked<typeof axios>;
  let mockClient: any;
  
  beforeEach(() => {
    jest.clearAllMocks();
    // Setup a mock client
    mockClient = mockedAxios.create();
  });

  describe('getRequest', () => {
    it('should call axios.get with the correct parameters', () => {
      // Given
      const url = '/test';
      const params = { id: 1 };
      
      // When
      getRequest(url, params);
      
      // Then
      expect(mockClient.get).toHaveBeenCalledWith(url, { params });
    });
    
    it('should call axios.get without params when not provided', () => {
      // Given
      const url = '/test';
      
      // When
      getRequest(url);
      
      // Then
      expect(mockClient.get).toHaveBeenCalledWith(url, { params: undefined });
    });
  });
  
  describe('postRequest', () => {
    it('should call axios.post with the correct parameters', () => {
      // Given
      const url = '/test';
      const data = { name: 'test' };
      
      // When
      postRequest(url, data);
      
      // Then
      expect(mockClient.post).toHaveBeenCalledWith(url, data);
    });
    
    it('should call axios.post without data when not provided', () => {
      // Given
      const url = '/test';
      
      // When
      postRequest(url);
      
      // Then
      expect(mockClient.post).toHaveBeenCalledWith(url, undefined);
    });
  });
  
  describe('putRequest', () => {
    it('should call axios.put with the correct parameters', () => {
      // Given
      const url = '/test';
      const data = { name: 'test' };
      
      // When
      putRequest(url, data);
      
      // Then
      expect(mockClient.put).toHaveBeenCalledWith(url, data);
    });
  });
  
  describe('deleteRequest', () => {
    it('should call axios.delete with the correct parameters', () => {
      // Given
      const url = '/test';
      const params = { id: 1 };
      
      // When
      deleteRequest(url, params);
      
      // Then
      expect(mockClient.delete).toHaveBeenCalledWith(url, { params });
    });
  });
}); 