package ai.kastrax.a2x.server

/**
 * A2X 服务器配置
 */
data class A2XServerConfig(
    /**
     * 服务器端口
     */
    val port: Int = 8080,
    
    /**
     * 服务器主机
     */
    val host: String = "0.0.0.0",
    
    /**
     * 是否启用 CORS
     */
    val enableCors: Boolean = true,
    
    /**
     * 是否启用 HTTPS
     */
    val enableHttps: Boolean = false,
    
    /**
     * HTTPS 证书路径
     */
    val certPath: String? = null,
    
    /**
     * HTTPS 密钥路径
     */
    val keyPath: String? = null,
    
    /**
     * 是否启用认证
     */
    val enableAuth: Boolean = false,
    
    /**
     * 是否启用日志
     */
    val enableLogging: Boolean = true,
    
    /**
     * 是否启用指标
     */
    val enableMetrics: Boolean = true,
    
    /**
     * 是否启用跟踪
     */
    val enableTracing: Boolean = false,
    
    /**
     * 是否启用压缩
     */
    val enableCompression: Boolean = true,
    
    /**
     * 是否启用缓存
     */
    val enableCaching: Boolean = true,
    
    /**
     * 是否启用限流
     */
    val enableRateLimiting: Boolean = false,
    
    /**
     * 限流速率（每秒请求数）
     */
    val rateLimitPerSecond: Int = 100,
    
    /**
     * 是否启用超时
     */
    val enableTimeout: Boolean = true,
    
    /**
     * 请求超时（毫秒）
     */
    val requestTimeoutMillis: Long = 30000,
    
    /**
     * 是否启用重试
     */
    val enableRetry: Boolean = true,
    
    /**
     * 最大重试次数
     */
    val maxRetries: Int = 3,
    
    /**
     * 重试间隔（毫秒）
     */
    val retryIntervalMillis: Long = 1000,
    
    /**
     * 是否启用断路器
     */
    val enableCircuitBreaker: Boolean = true,
    
    /**
     * 断路器失败阈值
     */
    val circuitBreakerFailureThreshold: Int = 5,
    
    /**
     * 断路器重置超时（毫秒）
     */
    val circuitBreakerResetTimeoutMillis: Long = 30000
)
