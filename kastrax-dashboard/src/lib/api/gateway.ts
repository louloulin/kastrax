import { AxiosRequestConfig, AxiosResponse } from 'axios';
import { createApiClient } from './client';

export interface ServiceConfig {
  name: string;
  baseUrl: string;
  version: string;
  timeout?: number;
  headers?: Record<string, string>;
  healthCheckPath?: string;
  maxRetries?: number;
}

export interface GatewayConfig {
  services: ServiceConfig[];
  defaultTimeout?: number;
  defaultHeaders?: Record<string, string>;
  defaultMaxRetries?: number;
  defaultHealthCheckPath?: string;
}

export interface ServiceRequest {
  service: string;
  path: string;
  method?: string;
  data?: any;
  params?: Record<string, any>;
  headers?: Record<string, string>;
  timeout?: number;
  retries?: number;
}

export interface ServiceMetrics {
  totalRequests: number;
  successfulRequests: number;
  failedRequests: number;
  successRate: number;
  avgResponseTime: number;
  responseTimeHistory: number[];
}

export interface ServiceHealthStatus {
  isHealthy: boolean;
  lastChecked: Date;
  statusCode?: number;
  responseTime?: number;
  error?: string;
}

export class ApiGateway {
  private services: Map<string, ServiceConfig>;
  private clients: Map<string, ReturnType<typeof createApiClient>>;
  private defaultTimeout: number;
  private defaultHeaders: Record<string, string>;
  private defaultMaxRetries: number;
  private defaultHealthCheckPath: string;
  private metrics: Map<string, ServiceMetrics>;
  private healthStatus: Map<string, ServiceHealthStatus>;

  constructor(config: GatewayConfig) {
    this.services = new Map();
    this.clients = new Map();
    this.metrics = new Map();
    this.healthStatus = new Map();
    this.defaultTimeout = config.defaultTimeout || 30000;
    this.defaultHeaders = config.defaultHeaders || {};
    this.defaultMaxRetries = config.defaultMaxRetries || 3;
    this.defaultHealthCheckPath = config.defaultHealthCheckPath || '/health';

    config.services.forEach(service => this.registerService(service));
  }

  // Register a new service
  registerService(config: ServiceConfig): void {
    this.services.set(config.name, config);
    
    const client = createApiClient({
      baseURL: config.baseUrl,
      timeout: config.timeout || this.defaultTimeout,
      headers: {
        ...this.defaultHeaders,
        ...config.headers
      }
    });

    this.clients.set(config.name, client);
    
    // Initialize metrics for the service
    this.metrics.set(config.name, {
      totalRequests: 0,
      successfulRequests: 0,
      failedRequests: 0,
      successRate: 0,
      avgResponseTime: 0,
      responseTimeHistory: []
    });
    
    // Initialize health status
    this.healthStatus.set(config.name, {
      isHealthy: false,
      lastChecked: new Date(0) // Beginning of time, needs check
    });
    
    // Perform initial health check
    this.checkServiceHealth(config.name).catch(() => {
      // Ignore initial health check errors
    });
  }

  // Remove a service
  unregisterService(serviceName: string): void {
    this.services.delete(serviceName);
    this.clients.delete(serviceName);
    this.metrics.delete(serviceName);
    this.healthStatus.delete(serviceName);
  }

  // Get service configuration
  getServiceConfig(serviceName: string): ServiceConfig | undefined {
    return this.services.get(serviceName);
  }

  // List all registered services
  listServices(): ServiceConfig[] {
    return Array.from(this.services.values());
  }

  // Make a request through the gateway with retry mechanism
  async request<T = any>(request: ServiceRequest): Promise<AxiosResponse<T>> {
    const serviceName = request.service;
    const client = this.clients.get(serviceName);
    if (!client) {
      throw new Error(`Service '${serviceName}' not found`);
    }

    const service = this.services.get(serviceName);
    if (!service) {
      throw new Error(`Service configuration for '${serviceName}' not found`);
    }

    const maxRetries = request.retries !== undefined 
      ? request.retries 
      : (service.maxRetries || this.defaultMaxRetries);
    
    const config: AxiosRequestConfig = {
      url: request.path,
      method: request.method || 'GET',
      headers: {
        ...this.defaultHeaders,
        ...request.headers
      },
      timeout: request.timeout || service.timeout || this.defaultTimeout
    };

    if (request.data) {
      config.data = request.data;
    }

    if (request.params) {
      config.params = request.params;
    }

    let attempts = 0;
    let lastError: any;
    const startTime = Date.now();

    // Update total requests count
    const serviceMetrics = this.metrics.get(serviceName);
    if (serviceMetrics) {
      serviceMetrics.totalRequests++;
    }

    while (attempts <= maxRetries) {
      try {
        const response = await client.request<T>(config);
        
        // Update metrics on success
        if (serviceMetrics) {
          const duration = Date.now() - startTime;
          serviceMetrics.successfulRequests++;
          serviceMetrics.responseTimeHistory.push(duration);
          
          // Keep only last 100 response times to limit memory usage
          if (serviceMetrics.responseTimeHistory.length > 100) {
            serviceMetrics.responseTimeHistory.shift();
          }
          
          // Recalculate average response time
          const sum = serviceMetrics.responseTimeHistory.reduce((a, b) => a + b, 0);
          serviceMetrics.avgResponseTime = sum / serviceMetrics.responseTimeHistory.length;
          
          // Update success rate
          serviceMetrics.successRate = serviceMetrics.successfulRequests / serviceMetrics.totalRequests;
        }
        
        return response;
      } catch (error: any) {
        lastError = error;
        attempts++;
        
        // Don't retry on certain status codes (4xx client errors, except 429 too many requests)
        if (error.response && error.response.status >= 400 && error.response.status < 500 && error.response.status !== 429) {
          break;
        }
        
        // Only retry if we have attempts left
        if (attempts <= maxRetries) {
          // Exponential backoff with jitter
          const delay = Math.min(100 * Math.pow(2, attempts) + Math.random() * 100, 3000);
          await new Promise(resolve => setTimeout(resolve, delay));
        }
      }
    }
    
    // Update metrics on failure
    if (serviceMetrics) {
      serviceMetrics.failedRequests++;
      serviceMetrics.successRate = serviceMetrics.successfulRequests / serviceMetrics.totalRequests;
    }
    
    throw lastError || new Error(`Failed to make request to ${serviceName} after ${attempts} attempts`);
  }

  // Health check for a specific service
  async checkServiceHealth(serviceName: string): Promise<ServiceHealthStatus> {
    const service = this.services.get(serviceName);
    const client = this.clients.get(serviceName);
    const status: ServiceHealthStatus = {
      isHealthy: false,
      lastChecked: new Date()
    };
    
    if (!service || !client) {
      status.error = `Service ${serviceName} not found`;
      this.healthStatus.set(serviceName, status);
      return status;
    }

    const healthCheckPath = service.healthCheckPath || this.defaultHealthCheckPath;
    
    try {
      const startTime = Date.now();
      const response = await client.get(healthCheckPath);
      const duration = Date.now() - startTime;
      
      status.isHealthy = response.status >= 200 && response.status < 300;
      status.statusCode = response.status;
      status.responseTime = duration;
    } catch (error: any) {
      status.isHealthy = false;
      status.error = error.message;
      if (error.response) {
        status.statusCode = error.response.status;
      }
    }
    
    this.healthStatus.set(serviceName, status);
    return status;
  }

  // Check health of all services
  async checkAllServicesHealth(): Promise<Record<string, ServiceHealthStatus>> {
    const services = Array.from(this.services.keys());
    const results: Record<string, ServiceHealthStatus> = {};

    await Promise.all(
      services.map(async (serviceName) => {
        results[serviceName] = await this.checkServiceHealth(serviceName);
      })
    );

    return results;
  }

  // Get health status without performing a new check
  getServiceHealthStatus(serviceName: string): ServiceHealthStatus | undefined {
    return this.healthStatus.get(serviceName);
  }

  // Update service configuration
  updateServiceConfig(serviceName: string, config: Partial<ServiceConfig>): void {
    const existingConfig = this.services.get(serviceName);
    if (!existingConfig) {
      throw new Error(`Service '${serviceName}' not found`);
    }

    const updatedConfig = {
      ...existingConfig,
      ...config
    };

    this.registerService(updatedConfig);
  }

  // Get service metrics
  getServiceMetrics(serviceName: string): ServiceMetrics | undefined {
    return this.metrics.get(serviceName);
  }
  
  // Get all service metrics
  getAllServiceMetrics(): Record<string, ServiceMetrics> {
    const allMetrics: Record<string, ServiceMetrics> = {};
    this.metrics.forEach((metrics, serviceName) => {
      allMetrics[serviceName] = metrics;
    });
    return allMetrics;
  }
  
  // Reset metrics for a service
  resetServiceMetrics(serviceName: string): void {
    const service = this.services.get(serviceName);
    if (!service) {
      throw new Error(`Service '${serviceName}' not found`);
    }
    
    this.metrics.set(serviceName, {
      totalRequests: 0,
      successfulRequests: 0,
      failedRequests: 0,
      successRate: 0,
      avgResponseTime: 0,
      responseTimeHistory: []
    });
  }
}

// Create default gateway instance
export const defaultGateway = new ApiGateway({
  services: [
    {
      name: 'core',
      baseUrl: process.env.CORE_API_URL || 'http://localhost:3000',
      version: 'v2'
    }
  ],
  defaultTimeout: 30000,
  defaultHeaders: {
    'Content-Type': 'application/json'
  }
}); 