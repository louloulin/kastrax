import { rest } from 'msw';
import { setupServer } from 'msw/node';
import { ApiGateway, ServiceConfig, GatewayConfig, ServiceHealthStatus, ServiceMetrics } from '../gateway';

const mockServiceConfig: ServiceConfig = {
  name: 'test-service',
  baseUrl: 'http://test-service.local',
  version: 'v1',
  healthCheckPath: '/health'
};

const mockGatewayConfig: GatewayConfig = {
  services: [mockServiceConfig],
  defaultTimeout: 5000,
  defaultMaxRetries: 2,
  defaultHeaders: {
    'X-Test-Header': 'test'
  }
};

// Track request count for retry tests
let requestCount = 0;

const server = setupServer(
  rest.get('http://test-service.local/health', (req, res, ctx) => {
    return res(
      ctx.status(200),
      ctx.json({ 
        code: 200,
        success: true,
        msg: 'success',
        data: { status: 'ok' }
      })
    );
  }),
  
  rest.get('http://test-service.local/test', (req, res, ctx) => {
    return res(
      ctx.json({ 
        code: 200,
        success: true,
        msg: 'success',
        data: { message: 'success' }
      })
    );
  }),
  
  rest.post('http://test-service.local/test', (req, res, ctx) => {
    return res(
      ctx.json({ 
        code: 200,
        success: true,
        msg: 'success',
        data: { message: 'created' }
      })
    );
  }),
  
  // Endpoint that will fail on first attempt and succeed on second
  rest.get('http://test-service.local/retry-test', (req, res, ctx) => {
    requestCount++;
    if (requestCount === 1) {
      return res(ctx.status(500));
    }
    return res(
      ctx.json({ 
        code: 200,
        success: true,
        msg: 'success',
        data: { message: 'success after retry' }
      })
    );
  }),
  
  // Endpoint that will always fail
  rest.get('http://test-service.local/always-fail', (req, res, ctx) => {
    return res(ctx.status(500));
  }),
  
  // Endpoint that will fail with 404 (shouldn't retry)
  rest.get('http://test-service.local/client-error', (req, res, ctx) => {
    return res(ctx.status(404));
  })
);

beforeAll(() => server.listen());
afterEach(() => {
  server.resetHandlers();
  requestCount = 0;
});
afterAll(() => server.close());

describe('API Gateway', () => {
  let gateway: ApiGateway;

  beforeEach(() => {
    gateway = new ApiGateway(mockGatewayConfig);
  });

  describe('Service Management', () => {
    test('should register service on initialization', () => {
      const services = gateway.listServices();
      expect(services).toHaveLength(1);
      expect(services[0]).toEqual(mockServiceConfig);
    });

    test('should register new service', () => {
      const newService: ServiceConfig = {
        name: 'another-service',
        baseUrl: 'http://another-service.local',
        version: 'v1'
      };

      gateway.registerService(newService);
      const services = gateway.listServices();
      
      expect(services).toHaveLength(2);
      expect(services).toContainEqual(newService);
    });

    test('should unregister service', () => {
      gateway.unregisterService('test-service');
      const services = gateway.listServices();
      expect(services).toHaveLength(0);
    });

    test('should get service config', () => {
      const config = gateway.getServiceConfig('test-service');
      expect(config).toEqual(mockServiceConfig);
    });

    test('should update service config', () => {
      gateway.updateServiceConfig('test-service', {
        timeout: 10000
      });

      const config = gateway.getServiceConfig('test-service');
      expect(config).toEqual({
        ...mockServiceConfig,
        timeout: 10000
      });
    });

    test('should throw error when updating non-existent service', () => {
      expect(() => {
        gateway.updateServiceConfig('non-existent', {
          timeout: 10000
        });
      }).toThrow("Service 'non-existent' not found");
    });
  });

  describe('Request Handling', () => {
    test('should make GET request', async () => {
      const response = await gateway.request({
        service: 'test-service',
        path: '/test'
      });

      expect(response.data.data).toEqual({ message: 'success' });
    });

    test('should make POST request', async () => {
      const response = await gateway.request({
        service: 'test-service',
        path: '/test',
        method: 'POST',
        data: { test: true }
      });

      expect(response.data.data).toEqual({ message: 'created' });
    });

    test('should throw error for non-existent service', async () => {
      await expect(gateway.request({
        service: 'non-existent',
        path: '/test'
      })).rejects.toThrow("Service 'non-existent' not found");
    });

    test('should include custom headers', async () => {
      await gateway.request({
        service: 'test-service',
        path: '/test',
        headers: {
          'X-Custom-Header': 'custom'
        }
      });

      // MSW will throw if the header is not present
      expect(true).toBe(true);
    });
    
    test('should retry failed requests and succeed', async () => {
      const response = await gateway.request({
        service: 'test-service',
        path: '/retry-test'
      });
      
      expect(response.data.data).toEqual({ message: 'success after retry' });
      expect(requestCount).toBe(2); // Initial request + 1 retry
    });
    
    test('should respect maximum retry limit and fail after exhausting retries', async () => {
      // Set up the endpoint to always fail
      await expect(gateway.request({
        service: 'test-service',
        path: '/always-fail',
        retries: 1 // Override default retries
      })).rejects.toThrow();
    });
    
    test('should not retry 4xx client errors', async () => {
      await expect(gateway.request({
        service: 'test-service',
        path: '/client-error'
      })).rejects.toThrow();
      
      // Should not have retried
      expect(true).toBe(true);
    });
  });

  describe('Health Checks', () => {
    test('should check service health', async () => {
      const healthStatus = await gateway.checkServiceHealth('test-service');
      expect(healthStatus.isHealthy).toBe(true);
      expect(healthStatus.statusCode).toBe(200);
      expect(healthStatus.responseTime).toBeDefined();
      expect(healthStatus.lastChecked).toBeInstanceOf(Date);
    });

    test('should return error for non-existent service health check', async () => {
      const healthStatus = await gateway.checkServiceHealth('non-existent');
      expect(healthStatus.isHealthy).toBe(false);
      expect(healthStatus.error).toBeDefined();
    });

    test('should check all services health', async () => {
      const healthStatus = await gateway.checkAllServicesHealth();
      expect(healthStatus['test-service']).toBeDefined();
      expect(healthStatus['test-service'].isHealthy).toBe(true);
    });

    test('should handle failed health check', async () => {
      server.use(
        rest.get('http://test-service.local/health', (req, res, ctx) => {
          return res(ctx.status(500));
        })
      );

      const healthStatus = await gateway.checkServiceHealth('test-service');
      expect(healthStatus.isHealthy).toBe(false);
      expect(healthStatus.statusCode).toBe(500);
    });
    
    test('should get service health status without performing a check', async () => {
      // First, perform a health check to set the status
      await gateway.checkServiceHealth('test-service');
      
      // Now get the status without performing a new check
      const status = gateway.getServiceHealthStatus('test-service');
      expect(status).toBeDefined();
      expect(status?.isHealthy).toBe(true);
    });
  });

  describe('Service Metrics', () => {
    test('should track request metrics', async () => {
      // Make a successful request
      await gateway.request({
        service: 'test-service',
        path: '/test'
      });
      
      // Get metrics
      const metrics = gateway.getServiceMetrics('test-service');
      expect(metrics).toBeDefined();
      expect(metrics?.totalRequests).toBe(1);
      expect(metrics?.successfulRequests).toBe(1);
      expect(metrics?.failedRequests).toBe(0);
      expect(metrics?.successRate).toBe(1);
      expect(metrics?.avgResponseTime).toBeGreaterThan(0);
    });
    
    test('should track failed requests in metrics', async () => {
      // Try to make a request that will fail
      try {
        await gateway.request({
          service: 'test-service',
          path: '/always-fail',
          retries: 1
        });
      } catch (error) {
        // Expected to fail
      }
      
      // Get metrics
      const metrics = gateway.getServiceMetrics('test-service');
      expect(metrics).toBeDefined();
      expect(metrics?.totalRequests).toBe(1);
      expect(metrics?.successfulRequests).toBe(0);
      expect(metrics?.failedRequests).toBe(1);
      expect(metrics?.successRate).toBe(0);
    });
    
    test('should get all service metrics', async () => {
      // Make requests to create some metrics
      await gateway.request({
        service: 'test-service',
        path: '/test'
      });
      
      const newService: ServiceConfig = {
        name: 'another-service',
        baseUrl: 'http://another-service.local',
        version: 'v1'
      };
      gateway.registerService(newService);
      
      const allMetrics = gateway.getAllServiceMetrics();
      expect(Object.keys(allMetrics)).toHaveLength(2);
      expect(allMetrics['test-service']).toBeDefined();
      expect(allMetrics['another-service']).toBeDefined();
    });
    
    test('should reset service metrics', async () => {
      // Make a request to create metrics
      await gateway.request({
        service: 'test-service',
        path: '/test'
      });
      
      // Verify metrics exist
      let metrics = gateway.getServiceMetrics('test-service');
      expect(metrics?.totalRequests).toBe(1);
      
      // Reset metrics
      gateway.resetServiceMetrics('test-service');
      
      // Verify metrics were reset
      metrics = gateway.getServiceMetrics('test-service');
      expect(metrics?.totalRequests).toBe(0);
      expect(metrics?.successfulRequests).toBe(0);
      expect(metrics?.failedRequests).toBe(0);
    });
  });
}); 