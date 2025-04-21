# 工作流重试机制

本文档详细介绍了 KastraX 工作流引擎中的重试机制，包括配置、策略和最佳实践。

## 1. 概述

工作流重试机制允许自动重试失败的步骤，提高工作流的可靠性和鲁棒性。这对于处理临时性故障（如网络问题、API 限流或暂时性服务不可用）特别有用。

## 2. 重试配置

### 2.1 基本配置

可以通过两种方式为工作流步骤配置重试机制：

#### 使用 DSL 方法

```kotlin
agentStep(myAgent) {
    id = "my_step"
    retry(maxRetries = 3, initialDelay = Duration.ofMillis(100))
}
```

#### 使用配置对象

```kotlin
agentStep(myAgent) {
    id = "my_step"
    config = StepConfig(
        retryConfig = RetryConfig(
            maxRetries = 3,
            initialDelay = Duration.ofMillis(100),
            maxDelay = Duration.ofSeconds(30),
            backoffFactor = 2.0,
            jitter = 0.1,
            retryableExceptions = setOf(IOException::class.java, TimeoutException::class.java)
        )
    )
}
```

### 2.2 配置参数

重试配置支持以下参数：

| 参数 | 描述 | 默认值 |
|------|------|--------|
| `maxRetries` | 最大重试次数 | 3 |
| `initialDelay` | 第一次重试前的等待时间 | 100ms |
| `maxDelay` | 重试延迟的上限 | 30s |
| `backoffFactor` | 每次重试后延迟时间的增长倍数 | 2.0 |
| `jitter` | 延迟时间的随机变化因子 | 0.1 |
| `retryableExceptions` | 可触发重试的异常类型集合 | `setOf(Exception::class.java)` |

## 3. 重试策略

### 3.1 指数退避算法

重试机制使用指数退避算法计算重试间隔，公式如下：

```
delay = min(maxDelay, initialDelay * (backoffFactor ^ (attempt - 1)) * (1 ± jitter))
```

这种算法确保：
- 每次重试的等待时间逐渐增加
- 添加随机抖动以避免多个重试同时发生（雪崩效应）
- 延迟时间不会超过设定的最大值

### 3.2 可重试异常

通过 `retryableExceptions` 参数，可以指定哪些类型的异常应该触发重试。默认情况下，所有 `Exception` 类型的异常都会触发重试。

例如，只对网络和超时异常进行重试：

```kotlin
retryableExceptions = setOf(
    IOException::class.java,
    SocketTimeoutException::class.java,
    ConnectException::class.java
)
```

## 4. 示例

### 4.1 基本重试示例

```kotlin
val workflow = workflow {
    name = "重试示例工作流"
    
    agentStep(unreliableAgent) {
        id = "unreliable_step"
        retry(maxRetries = 3)
    }
}
```

### 4.2 高级重试配置

```kotlin
val workflow = workflow {
    name = "高级重试示例工作流"
    
    agentStep(unreliableAgent) {
        id = "api_call_step"
        config = StepConfig(
            retryConfig = RetryConfig(
                maxRetries = 5,
                initialDelay = Duration.ofMillis(200),
                maxDelay = Duration.ofSeconds(10),
                backoffFactor = 1.5,
                jitter = 0.2,
                retryableExceptions = setOf(
                    IOException::class.java,
                    TimeoutException::class.java
                )
            )
        )
    }
}
```

### 4.3 完整示例

完整的工作流重试示例可以在 `examples/src/main/kotlin/ai/kastrax/examples/WorkflowRetryExample.kt` 中找到。

## 5. 最佳实践

### 5.1 何时使用重试

重试机制最适合处理以下类型的错误：

- **临时性网络问题**：连接超时、网络波动等
- **资源限制**：API 限流、服务暂时过载等
- **暂时性服务不可用**：依赖服务的短暂中断

### 5.2 何时避免重试

以下情况应避免使用重试：

- **永久性错误**：认证失败、权限不足、资源不存在等
- **数据验证错误**：输入数据格式错误、业务逻辑错误等
- **非幂等操作**：如果操作不是幂等的，重试可能导致意外结果

### 5.3 配置建议

- **设置合理的最大重试次数**：通常 3-5 次足够处理大多数临时性问题
- **使用指数退避**：避免对服务造成额外负担
- **添加适当的抖动**：防止多个客户端同时重试（雪崩效应）
- **设置最大延迟上限**：确保重试不会无限期延迟
- **指定可重试的异常类型**：只对真正可能通过重试解决的问题进行重试

## 6. 故障排除

### 6.1 常见问题

1. **重试不生效**：确保异常类型包含在 `retryableExceptions` 中
2. **重试次数过多**：检查 `maxRetries` 设置，考虑是否是永久性错误
3. **重试间隔过长**：调整 `initialDelay`、`backoffFactor` 和 `maxDelay` 参数

### 6.2 调试技巧

启用详细日志记录，以便更好地理解重试行为：

```kotlin
System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "DEBUG")
```

## 7. 参考

- [指数退避算法](https://en.wikipedia.org/wiki/Exponential_backoff)
- [重试模式](https://docs.microsoft.com/en-us/azure/architecture/patterns/retry)
- [KastraX 工作流引擎文档](workflow-engine.md)
