# KastraX 配置管理

## 1. 概述

KastraX 配置管理是 KastraX 部署器的核心组件之一，用于加载、验证和管理部署配置。配置管理系统支持从文件加载配置、环境变量注入和配置验证。

配置管理的设计目标是提供一个灵活且强大的配置系统，使开发者能够轻松地配置 KastraX 应用的部署，同时确保配置的正确性和安全性。

## 2. 核心组件

### 2.1 配置验证器（ConfigValidator）

配置验证器用于验证各种配置的有效性：

```kotlin
object ConfigValidator {
    fun validateDeploymentConfig(config: DeploymentConfig): ValidationResult
    fun validateResourceConfig(config: ResourceConfig): ValidationResult
    fun validateDockerConfig(config: DockerConfig): ValidationResult
    fun validateKubernetesConfig(config: KubernetesConfig): ValidationResult
    fun validateLambdaConfig(config: LambdaConfig): ValidationResult
}
```

### 2.2 验证结果（ValidationResult）

验证结果包含验证的状态、错误和警告：

```kotlin
data class ValidationResult(
    val valid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)
```

### 2.3 部署配置（DeploymentConfig）

部署配置用于指定部署的参数：

```kotlin
data class DeploymentConfig(
    val name: String,
    val version: String = "1.0.0",
    val environment: Map<String, String> = emptyMap(),
    val resources: ResourceConfig = ResourceConfig()
)
```

### 2.4 资源配置（ResourceConfig）

资源配置用于指定部署所需的资源：

```kotlin
data class ResourceConfig(
    val memory: Int = 512,
    val cpu: Int = 1,
    val timeout: Int = 30,
    val concurrency: Int = 10
)
```

## 3. 配置加载

### 3.1 从文件加载配置

KastraX 使用 Typesafe Config 库加载配置文件：

```kotlin
val config = ConfigFactory.parseFile(File("/path/to/config.conf"))
```

支持的配置格式：
- HOCON（Human-Optimized Config Object Notation）
- JSON
- Properties

### 3.2 配置文件示例

```hocon
# 部署配置
docker {
    baseImage = "openjdk:17-slim"
    port = 8080
    hostPort = 8080
}

kubernetes {
    namespace = "kastrax"
    replicas = 2
    serviceType = "LoadBalancer"
    
    docker {
        baseImage = "openjdk:17-slim"
        port = 8080
        hostPort = 8080
    }
}

lambda {
    region = "us-east-1"
    runtime = "java11"
    handler = "ai.kastrax.Handler::handleRequest"
    role = "arn:aws:iam::123456789012:role/lambda-role"
    bucketName = "kastrax-deployments"
}

blueGreen {
    healthCheckUrl = "http://localhost:8080/health"
    healthCheckTimeout = 60
    switchDelay = 10
    autoSwitch = true
}
```

## 4. 配置验证

### 4.1 验证部署配置

```kotlin
val config = DeploymentConfig(
    name = "my-app",
    version = "1.0.0",
    environment = mapOf(
        "JAVA_OPTS" to "-Xmx512m",
        "LOG_LEVEL" to "INFO"
    ),
    resources = ResourceConfig(
        memory = 1024,
        cpu = 2,
        timeout = 60,
        concurrency = 10
    )
)

val result = ConfigValidator.validateDeploymentConfig(config)
if (!result.valid) {
    println("配置验证失败：")
    result.errors.forEach { println("错误：$it") }
    result.warnings.forEach { println("警告：$it") }
}
```

### 4.2 验证特定配置

```kotlin
// 验证 Docker 配置
val dockerConfig = DockerConfig(
    baseImage = "openjdk:17-slim",
    port = 8080,
    hostPort = 8080
)
val dockerResult = ConfigValidator.validateDockerConfig(dockerConfig)

// 验证 Kubernetes 配置
val kubernetesConfig = KubernetesConfig(
    namespace = "default",
    replicas = 2,
    serviceType = "ClusterIP"
)
val k8sResult = ConfigValidator.validateKubernetesConfig(kubernetesConfig)

// 验证 Lambda 配置
val lambdaConfig = LambdaConfig(
    region = "us-east-1",
    runtime = "java11",
    handler = "ai.kastrax.Handler::handleRequest",
    role = "arn:aws:iam::123456789012:role/lambda-role",
    bucketName = "kastrax-deployments"
)
val lambdaResult = ConfigValidator.validateLambdaConfig(lambdaConfig)
```

## 5. 环境变量支持

### 5.1 在部署配置中使用环境变量

```kotlin
val config = DeploymentConfig(
    name = "my-app",
    version = "1.0.0",
    environment = mapOf(
        "API_KEY" to System.getenv("API_KEY"),
        "DATABASE_URL" to System.getenv("DATABASE_URL"),
        "LOG_LEVEL" to "INFO"
    )
)
```

### 5.2 在配置文件中引用环境变量

```hocon
docker {
    baseImage = ${?DOCKER_BASE_IMAGE}
    port = 8080
    hostPort = ${?DOCKER_HOST_PORT}
}
```

## 6. 最佳实践

### 6.1 配置分层

为不同的环境创建不同的配置文件：

```
config/
├── application.conf       # 基础配置
├── application-dev.conf   # 开发环境配置
├── application-test.conf  # 测试环境配置
└── application-prod.conf  # 生产环境配置
```

### 6.2 敏感信息处理

不要在配置文件中硬编码敏感信息，使用环境变量或安全的密钥管理系统：

```kotlin
val config = DeploymentConfig(
    name = "my-app",
    environment = mapOf(
        "API_KEY" to System.getenv("API_KEY"),
        "DATABASE_PASSWORD" to System.getenv("DB_PASSWORD")
    )
)
```

### 6.3 配置验证

始终验证配置，特别是在生产环境中：

```kotlin
val result = ConfigValidator.validateDeploymentConfig(config)
if (!result.valid) {
    throw IllegalArgumentException("Invalid configuration: ${result.errors.joinToString()}")
}
```
