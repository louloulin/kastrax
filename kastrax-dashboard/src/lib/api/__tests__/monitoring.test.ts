import { AxiosResponse, AxiosError } from 'axios';
import {
  apiMonitor,
  monitorRequest,
  monitorResponse,
  monitorError,
  ApiMetrics,
  ErrorMetrics,
  PerformanceMetrics
} from '../monitoring';

describe('API Monitoring', () => {
  beforeEach(() => {
    // Clear all metrics before each test
    (apiMonitor as any).metrics = [];
    (apiMonitor as any).errors = [];
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  describe('Request Monitoring', () => {
    test('monitorRequest should add startTime to config', () => {
      const config = {};
      const result = monitorRequest(config);
      expect(result.metadata?.startTime).toBeDefined();
    });
  });

  describe('Response Monitoring', () => {
    test('monitorResponse should track successful API calls', () => {
      const response = {
        config: {
          url: '/test',
          method: 'get',
          metadata: { startTime: Date.now() - 100 }
        },
        status: 200
      } as AxiosResponse;

      monitorResponse(response);
      const metrics = (apiMonitor as any).metrics;
      
      expect(metrics).toHaveLength(1);
      expect(metrics[0]).toMatchObject({
        url: '/test',
        method: 'GET',
        status: 200,
        success: true
      });
      expect(metrics[0].duration).toBeGreaterThanOrEqual(0);
    });
  });

  describe('Error Monitoring', () => {
    test('monitorError should track failed API calls', () => {
      const error = new AxiosError();
      error.config = {
        url: '/test',
        method: 'post',
        metadata: { startTime: Date.now() - 100 }
      };
      error.response = {
        status: 500
      } as any;

      try {
        monitorError(error);
      } catch (e) {
        // Expected to throw
      }

      const metrics = (apiMonitor as any).metrics;
      const errors = (apiMonitor as any).errors;
      
      expect(metrics).toHaveLength(1);
      expect(metrics[0]).toMatchObject({
        url: '/test',
        method: 'POST',
        status: 500,
        success: false
      });
      
      expect(errors).toHaveLength(1);
      expect(errors[0]).toMatchObject({
        url: '/test',
        method: 'POST',
        errorType: 'AxiosError'
      });
    });
  });

  describe('Performance Metrics', () => {
    beforeEach(() => {
      // Add some test metrics
      const baseTime = Date.now();
      const testMetrics: ApiMetrics[] = [
        {
          url: '/test',
          method: 'GET',
          duration: 100,
          status: 200,
          timestamp: baseTime,
          success: true
        },
        {
          url: '/test',
          method: 'GET',
          duration: 200,
          status: 200,
          timestamp: baseTime,
          success: true
        },
        {
          url: '/test',
          method: 'GET',
          duration: 300,
          status: 500,
          timestamp: baseTime,
          success: false
        }
      ];

      testMetrics.forEach(metric => (apiMonitor as any).addMetrics(metric));
    });

    test('getEndpointMetrics should calculate correct metrics', () => {
      const metrics = apiMonitor.getEndpointMetrics('/test');
      
      expect(metrics).toMatchObject({
        avgResponseTime: 200,
        p95ResponseTime: 300,
        p99ResponseTime: 300,
        errorRate: 1/3,
        totalCalls: 3,
        successCalls: 2,
        failedCalls: 1
      });
    });

    test('getOverallMetrics should calculate metrics for all endpoints', () => {
      const metrics = apiMonitor.getOverallMetrics();
      
      expect(metrics).toMatchObject({
        avgResponseTime: 200,
        p95ResponseTime: 300,
        p99ResponseTime: 300,
        errorRate: 1/3,
        totalCalls: 3,
        successCalls: 2,
        failedCalls: 1
      });
    });

    test('metrics should respect time window', () => {
      const baseTime = Date.now();
      jest.setSystemTime(baseTime + 4 * 3600000); // Advance 4 hours
      
      const metrics = apiMonitor.getOverallMetrics(3600000); // 1 hour window
      
      expect(metrics).toMatchObject({
        totalCalls: 0,
        successCalls: 0,
        failedCalls: 0,
        errorRate: 0
      });
    });
  });

  describe('Memory Management', () => {
    test('should limit the number of stored metrics', () => {
      const maxMetrics = (apiMonitor as any).maxMetricsLength;
      
      // Add more metrics than the limit
      for (let i = 0; i < maxMetrics + 10; i++) {
        apiMonitor.addMetrics({
          url: '/test',
          method: 'GET',
          duration: 100,
          status: 200,
          timestamp: Date.now(),
          success: true
        });
      }

      expect((apiMonitor as any).metrics.length).toBe(maxMetrics);
    });

    test('should limit the number of stored errors', () => {
      const maxErrors = (apiMonitor as any).maxErrorsLength;
      
      // Add more errors than the limit
      for (let i = 0; i < maxErrors + 10; i++) {
        apiMonitor.addError({
          url: '/test',
          method: 'GET',
          errorType: 'TestError',
          errorMessage: 'Test error',
          timestamp: Date.now()
        });
      }

      expect((apiMonitor as any).errors.length).toBe(maxErrors);
    });

    test('clearOldMetrics should remove expired metrics', () => {
      const baseTime = Date.now();
      
      // Add some old metrics
      apiMonitor.addMetrics({
        url: '/test',
        method: 'GET',
        duration: 100,
        status: 200,
        timestamp: baseTime - 48 * 3600000, // 48 hours ago
        success: true
      });

      // Add some recent metrics
      apiMonitor.addMetrics({
        url: '/test',
        method: 'GET',
        duration: 100,
        status: 200,
        timestamp: baseTime,
        success: true
      });

      apiMonitor.clearOldMetrics(24 * 3600000); // 24 hour window
      
      expect((apiMonitor as any).metrics.length).toBe(1);
    });
  });
}); 