import { AxiosRequestConfig, AxiosResponse, AxiosError } from 'axios';

// Extend AxiosRequestConfig to include metadata
declare module 'axios' {
  export interface AxiosRequestConfig {
    metadata?: {
      startTime: number;
    };
  }
}

export interface ApiMetrics {
  url: string;
  method: string;
  duration: number;
  status: number;
  timestamp: number;
  success: boolean;
}

export interface ErrorMetrics {
  url: string;
  method: string;
  errorType: string;
  errorMessage: string;
  timestamp: number;
}

export interface PerformanceMetrics {
  avgResponseTime: number;
  p95ResponseTime: number;
  p99ResponseTime: number;
  errorRate: number;
  totalCalls: number;
  successCalls: number;
  failedCalls: number;
}

class ApiMonitor {
  private metrics: ApiMetrics[] = [];
  private errors: ErrorMetrics[] = [];
  private readonly maxMetricsLength = 1000;
  private readonly maxErrorsLength = 100;

  // Add new API call metrics
  addMetrics(metrics: ApiMetrics): void {
    this.metrics.unshift(metrics);
    if (this.metrics.length > this.maxMetricsLength) {
      this.metrics.pop();
    }
  }

  // Add new error metrics
  addError(error: ErrorMetrics): void {
    this.errors.unshift(error);
    if (this.errors.length > this.maxErrorsLength) {
      this.errors.pop();
    }
  }

  // Calculate performance metrics for a specific endpoint
  getEndpointMetrics(url: string, timeWindow: number = 3600000): PerformanceMetrics {
    const now = Date.now();
    const relevantMetrics = this.metrics.filter(
      m => m.url === url && (now - m.timestamp) <= timeWindow
    );

    if (relevantMetrics.length === 0) {
      return {
        avgResponseTime: 0,
        p95ResponseTime: 0,
        p99ResponseTime: 0,
        errorRate: 0,
        totalCalls: 0,
        successCalls: 0,
        failedCalls: 0
      };
    }

    const durations = relevantMetrics.map(m => m.duration).sort((a, b) => a - b);
    const totalCalls = relevantMetrics.length;
    const successCalls = relevantMetrics.filter(m => m.success).length;

    return {
      avgResponseTime: this.calculateAverage(durations),
      p95ResponseTime: this.calculatePercentile(durations, 95),
      p99ResponseTime: this.calculatePercentile(durations, 99),
      errorRate: (totalCalls - successCalls) / totalCalls,
      totalCalls,
      successCalls,
      failedCalls: totalCalls - successCalls
    };
  }

  // Get all metrics within a time window
  getMetrics(timeWindow: number = 3600000): ApiMetrics[] {
    const now = Date.now();
    return this.metrics.filter(m => (now - m.timestamp) <= timeWindow);
  }

  // Get all errors within a time window
  getErrors(timeWindow: number = 3600000): ErrorMetrics[] {
    const now = Date.now();
    return this.errors.filter(e => (now - e.timestamp) <= timeWindow);
  }

  // Get overall API performance metrics
  getOverallMetrics(timeWindow: number = 3600000): PerformanceMetrics {
    const relevantMetrics = this.getMetrics(timeWindow);
    
    if (relevantMetrics.length === 0) {
      return {
        avgResponseTime: 0,
        p95ResponseTime: 0,
        p99ResponseTime: 0,
        errorRate: 0,
        totalCalls: 0,
        successCalls: 0,
        failedCalls: 0
      };
    }

    const durations = relevantMetrics.map(m => m.duration).sort((a, b) => a - b);
    const totalCalls = relevantMetrics.length;
    const successCalls = relevantMetrics.filter(m => m.success).length;

    return {
      avgResponseTime: this.calculateAverage(durations),
      p95ResponseTime: this.calculatePercentile(durations, 95),
      p99ResponseTime: this.calculatePercentile(durations, 99),
      errorRate: (totalCalls - successCalls) / totalCalls,
      totalCalls,
      successCalls,
      failedCalls: totalCalls - successCalls
    };
  }

  // Calculate average of an array of numbers
  private calculateAverage(numbers: number[]): number {
    return numbers.reduce((a, b) => a + b, 0) / numbers.length;
  }

  // Calculate percentile of an array of numbers
  private calculatePercentile(numbers: number[], percentile: number): number {
    const index = Math.ceil((percentile / 100) * numbers.length) - 1;
    return numbers[index];
  }

  // Clear old metrics
  clearOldMetrics(timeWindow: number = 86400000): void {
    const now = Date.now();
    this.metrics = this.metrics.filter(m => (now - m.timestamp) <= timeWindow);
    this.errors = this.errors.filter(e => (now - e.timestamp) <= timeWindow);
  }
}

// Create singleton instance
export const apiMonitor = new ApiMonitor();

// Axios request interceptor
export function monitorRequest(config: AxiosRequestConfig) {
  config.metadata = { startTime: Date.now() };
  return config;
}

// Axios response interceptor
export function monitorResponse(response: AxiosResponse) {
  const duration = Date.now() - (response.config.metadata?.startTime || Date.now());
  
  apiMonitor.addMetrics({
    url: response.config.url || '',
    method: response.config.method?.toUpperCase() || 'UNKNOWN',
    duration,
    status: response.status,
    timestamp: Date.now(),
    success: true
  });

  return response;
}

// Axios error interceptor
export function monitorError(error: AxiosError) {
  const duration = Date.now() - (error.config?.metadata?.startTime || Date.now());
  
  apiMonitor.addMetrics({
    url: error.config?.url || '',
    method: error.config?.method?.toUpperCase() || 'UNKNOWN',
    duration,
    status: error.response?.status || 0,
    timestamp: Date.now(),
    success: false
  });

  apiMonitor.addError({
    url: error.config?.url || '',
    method: error.config?.method?.toUpperCase() || 'UNKNOWN',
    errorType: error.name,
    errorMessage: error.message,
    timestamp: Date.now()
  });

  throw error;
} 