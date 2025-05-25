import { rest } from 'msw';
import { setupServer } from 'msw/node';
import client from '../client';
import { getRequest, postRequest, putRequest, deleteRequest } from '../utils';

// Direct imports for v1 API
import {
  getResourceList as v1GetResourceList,
  getResourceById as v1GetResourceById,
  createResource as v1CreateResource,
  updateResource as v1UpdateResource,
  deleteResource as v1DeleteResource,
  testResourceConnection as v1TestResourceConnection,
  testResourceConfig as v1TestResourceConfig,
  getResourceTypes as v1GetResourceTypes
} from '../resources';

// Direct imports for v2 API
import {
  getResourceList as v2GetResourceList,
  getResourceById as v2GetResourceById,
  createResource as v2CreateResource,
  updateResource as v2UpdateResource,
  deleteResource as v2DeleteResource,
  testResourceConnection as v2TestResourceConnection,
  testResourceConfig as v2TestResourceConfig,
  getResourceTypes as v2GetResourceTypes
} from '../v2/resources';

// Mock the client and utility functions
jest.mock('../client', () => ({
  __esModule: true,
  default: {
    get: jest.fn().mockImplementation(() => Promise.resolve({ data: { success: true } })),
    post: jest.fn().mockImplementation(() => Promise.resolve({ data: { success: true } })),
    put: jest.fn().mockImplementation(() => Promise.resolve({ data: { success: true } })),
    delete: jest.fn().mockImplementation(() => Promise.resolve({ data: { success: true } })),
  },
}));

jest.mock('../utils', () => ({
  getRequest: jest.fn().mockImplementation(() => Promise.resolve({ data: { success: true } })),
  postRequest: jest.fn().mockImplementation(() => Promise.resolve({ data: { success: true } })),
  putRequest: jest.fn().mockImplementation(() => Promise.resolve({ data: { success: true } })),
  deleteRequest: jest.fn().mockImplementation(() => Promise.resolve({ data: { success: true } })),
}));

// Test data
const mockResource = {
  resourceId: 'resource-123',
  resourceName: 'Test Resource',
  resourceType: 'database',
  status: 1,
  properties: { host: 'localhost', port: 5432 },
  createTime: '2023-06-15T00:00:00Z',
  updateTime: '2023-06-15T00:00:00Z'
};

describe('Resources API', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('v1 Resources API', () => {
    test('getResourceList should call client.post with correct path and data', async () => {
      const params = { resourceType: 'database', page: 1 };
      await v1GetResourceList(params);
      expect(client.post).toHaveBeenCalledWith('/resource/list', params);
    });

    test('getResourceById should call client.get with correct path', async () => {
      await v1GetResourceById('resource-123');
      expect(client.get).toHaveBeenCalledWith('/resource/resource-123');
    });

    test('createResource should call client.post with correct path and data', async () => {
      const resourceData = {
        resourceName: 'Test Database',
        resourceType: 'database',
        properties: { host: 'localhost', port: 5432 }
      };
      await v1CreateResource(resourceData);
      expect(client.post).toHaveBeenCalledWith('/resource/add', resourceData);
    });

    test('updateResource should call client.post with correct path and data', async () => {
      const updateData = {
        resourceId: 'resource-123',
        resourceName: 'Updated Resource'
      };
      await v1UpdateResource(updateData);
      expect(client.post).toHaveBeenCalledWith('/resource/update', updateData);
    });

    test('deleteResource should call client.post with correct path and data', async () => {
      await v1DeleteResource('resource-123');
      expect(client.post).toHaveBeenCalledWith('/resource/delete', { resourceId: 'resource-123' });
    });

    test('testResourceConnection should call client.post with correct path and data', async () => {
      await v1TestResourceConnection('resource-123');
      expect(client.post).toHaveBeenCalledWith('/resource/test', { resourceId: 'resource-123' });
    });

    test('testResourceConfig should call client.post with correct path and data', async () => {
      const configData = {
        resourceName: 'Test Config',
        resourceType: 'database',
        properties: { host: 'localhost', port: 5432 }
      };
      await v1TestResourceConfig(configData);
      expect(client.post).toHaveBeenCalledWith('/resource/test-config', configData);
    });

    test('getResourceTypes should call client.get with correct path', async () => {
      await v1GetResourceTypes();
      expect(client.get).toHaveBeenCalledWith('/resource/types');
    });
  });

  describe('v2 Resources API', () => {
    test('v2GetResourceList should use getRequest utility', async () => {
      const params = { resourceType: 'database', page: 1 };
      await v2GetResourceList(params);
      expect(getRequest).toHaveBeenCalledWith('/api/v2/resources', params);
    });

    test('v2GetResourceById should use getRequest utility', async () => {
      await v2GetResourceById('resource-123');
      expect(getRequest).toHaveBeenCalledWith('/api/v2/resources/resource-123');
    });

    test('v2CreateResource should use postRequest utility', async () => {
      const resourceData = {
        resourceName: 'Test Database',
        resourceType: 'database',
        properties: { host: 'localhost', port: 5432 }
      };
      await v2CreateResource(resourceData);
      expect(postRequest).toHaveBeenCalledWith('/api/v2/resources', resourceData);
    });

    test('v2UpdateResource should use putRequest utility', async () => {
      const resourceId = 'resource-123';
      const updateData = {
        resourceName: 'Updated Resource'
      };
      await v2UpdateResource(resourceId, updateData);
      expect(putRequest).toHaveBeenCalledWith('/api/v2/resources/resource-123', updateData);
    });

    test('v2DeleteResource should use deleteRequest utility', async () => {
      await v2DeleteResource('resource-123');
      expect(deleteRequest).toHaveBeenCalledWith('/api/v2/resources/resource-123');
    });

    test('v2TestResourceConnection should use postRequest utility', async () => {
      await v2TestResourceConnection('resource-123');
      expect(postRequest).toHaveBeenCalledWith('/api/v2/resources/resource-123/test');
    });

    test('v2TestResourceConfig should use postRequest utility', async () => {
      const configData = {
        resourceName: 'Test Config',
        resourceType: 'database',
        properties: { host: 'localhost', port: 5432 }
      };
      await v2TestResourceConfig(configData);
      expect(postRequest).toHaveBeenCalledWith('/api/v2/resources/test-config', configData);
    });

    test('v2GetResourceTypes should use getRequest utility', async () => {
      await v2GetResourceTypes();
      expect(getRequest).toHaveBeenCalledWith('/api/v2/resources/types');
    });
  });
}); 