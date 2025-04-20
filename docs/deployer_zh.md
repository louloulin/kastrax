# KastraX 部署器

## 1. 概述

KastraX 部署器（kastrax-deployer）是一个用于部署 KastraX 应用的工具。它提供了多种部署目标的支持，包括：

- Docker 容器
- Kubernetes 集群
- AWS Lambda 无服务器函数

部署器的设计目标是提供一个统一的接口，使开发者能够轻松地将 KastraX 应用部署到不同的环境中，而无需了解每个环境的具体细节。

## 2. 核心组件

### 2.1 部署器接口（Deployer）

所有部署器都实现了 `Deployer` 接口：

```kotlin
interface Deployer {
    val name: String
    
    fun deploy(projectPath: Path, config: DeploymentConfig): Flow<DeploymentStatusUpdate>
    
    suspend fun getResult(deploymentId: String): DeploymentResult
    
    suspend fun delete(deploymentId: String): Boolean
}
```

其中：
- `name`：部署器的名称
- `deploy`：部署应用，返回部署状态更新流
- `getResult`：获取部署结果
- `delete`：删除部署

### 2.2 部署配置（DeploymentConfig）

部署配置包含应用的基本信息和资源配置：

```kotlin
data class DeploymentConfig(
    val name: String,
    val version: String = "1.0.0",
    val environment: Map<String, String> = emptyMap(),
    val resources: ResourceConfig = ResourceConfig()
)

data class ResourceConfig(
    val memory: Int = 512,
    val cpu: Int = 1,
    val timeout: Int = 30,
    val concurrency: Int = 10
)
```

### 2.3 部署状态（DeploymentStatus）

部署状态表示部署过程中的不同阶段：

```kotlin
enum class DeploymentStatus {
    PREPARING,
    BUILDING,
    UPLOADING,
    DEPLOYING,
    CONFIGURING,
    TESTING,
    COMPLETED,
    FAILED
}
```

### 2.4 部署结果（DeploymentResult）

部署结果包含部署的最终状态和相关信息：

```kotlin
data class DeploymentResult(
    val success: Boolean,
    val url: String? = null,
    val message: String = "",
    val logs: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)
```

### 2.5 部署器工厂（DeployerFactory）

部署器工厂用于创建不同类型的部署器：

```kotlin
object DeployerFactory {
    fun createDeployer(type: DeployerType, configPath: String? = null): Deployer
}

enum class DeployerType {
    DOCKER,
    KUBERNETES,
    LAMBDA
}
```

## 3. 内置部署器

### 3.1 Docker 部署器

Docker 部署器用于将应用部署为 Docker 容器：

```kotlin
val dockerDeployer = DeployerFactory.createDeployer(DeployerType.DOCKER)
```

配置选项：
- `baseImage`：基础镜像，默认为 "openjdk:17-slim"
- `port`：容器端口，默认为 8080
- `hostPort`：主机端口，默认为 8080
- `dockerfilePath`：Dockerfile 路径，默认为项目根目录下的 "Dockerfile"

### 3.2 Kubernetes 部署器

Kubernetes 部署器用于将应用部署到 Kubernetes 集群：

```kotlin
val kubernetesDeployer = DeployerFactory.createDeployer(DeployerType.KUBERNETES)
```

配置选项：
- `namespace`：命名空间，默认为 "default"
- `replicas`：副本数，默认为 1
- `serviceType`：服务类型，默认为 "ClusterIP"
- `dockerConfig`：Docker 配置，用于构建镜像

### 3.3 Lambda 部署器

Lambda 部署器用于将应用部署为 AWS Lambda 函数：

```kotlin
val lambdaDeployer = DeployerFactory.createDeployer(DeployerType.LAMBDA)
```

配置选项：
- `region`：AWS 区域，默认为 "us-east-1"
- `runtime`：Lambda 运行时，默认为 "java11"
- `handler`：处理函数，默认为 "ai.kastrax.Handler::handleRequest"
- `role`：IAM 角色，默认为 "arn:aws:iam::123456789012:role/lambda-role"
- `bucketName`：S3 存储桶名称，默认为 "kastrax-deployments"

## 4. 使用方法

### 4.1 基本用法

```kotlin
import ai.kastrax.deployer.DeployerFactory
import ai.kastrax.deployer.DeployerType
import ai.kastrax.deployer.DeploymentConfig
import ai.kastrax.deployer.ResourceConfig
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import java.nio.file.Paths

fun main() = runBlocking {
    // 创建部署器
    val deployer = DeployerFactory.createDeployer(DeployerType.DOCKER)
    
    // 创建部署配置
    val config = DeploymentConfig(
        name = "my-kastrax-app",
        version = "1.0.0",
        environment = mapOf(
            "OPENAI_API_KEY" to System.getenv("OPENAI_API_KEY"),
            "LOG_LEVEL" to "INFO"
        ),
        resources = ResourceConfig(
            memory = 1024,
            cpu = 2,
            timeout = 60,
            concurrency = 10
        )
    )
    
    // 部署应用
    val projectPath = Paths.get("/path/to/project")
    
    deployer.deploy(projectPath, config).collect { update ->
        println("${update.status}: ${update.message} (${update.progress}%)")
    }
    
    // 获取部署结果
    val result = deployer.getResult("deployment-id")
    
    if (result.success) {
        println("Deployment successful!")
        println("URL: ${result.url}")
    } else {
        println("Deployment failed: ${result.message}")
    }
}
```

### 4.2 使用配置文件

可以使用配置文件来配置部署器：

```kotlin
val deployer = DeployerFactory.createDeployer(
    DeployerType.KUBERNETES,
    "/path/to/config.conf"
)
```

配置文件示例（HOCON 格式）：

```hocon
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
```

### 4.3 监听部署状态

可以使用 Flow 来监听部署状态：

```kotlin
deployer.deploy(projectPath, config).collect { update ->
    when (update.status) {
        DeploymentStatus.PREPARING -> println("准备部署...")
        DeploymentStatus.BUILDING -> println("构建应用...")
        DeploymentStatus.UPLOADING -> println("上传文件...")
        DeploymentStatus.DEPLOYING -> println("部署应用...")
        DeploymentStatus.CONFIGURING -> println("配置应用...")
        DeploymentStatus.TESTING -> println("测试应用...")
        DeploymentStatus.COMPLETED -> println("部署完成！")
        DeploymentStatus.FAILED -> println("部署失败：${update.message}")
    }
    
    println("进度：${update.progress}%")
}
```

### 4.4 删除部署

可以使用 `delete` 方法删除部署：

```kotlin
val success = deployer.delete("deployment-id")
if (success) {
    println("部署已删除")
} else {
    println("删除部署失败")
}
```

## 5. 自定义部署器

可以通过实现 `Deployer` 接口或继承 `AbstractDeployer` 类来创建自定义部署器：

```kotlin
class CustomDeployer : AbstractDeployer() {
    override val name: String = "Custom Deployer"
    
    override fun deploy(projectPath: Path, config: DeploymentConfig) = flow {
        emit(createStatusUpdate(DeploymentStatus.PREPARING, "准备部署", 10))
        
        // 实现自定义部署逻辑
        
        emit(createStatusUpdate(DeploymentStatus.COMPLETED, "部署完成", 100))
    }
    
    override suspend fun getResult(deploymentId: String): DeploymentResult {
        // 实现获取结果的逻辑
        return createResult(true, "https://example.com", "部署成功")
    }
    
    override suspend fun delete(deploymentId: String): Boolean {
        // 实现删除部署的逻辑
        return true
    }
}
```

## 6. 最佳实践

### 6.1 错误处理

在使用部署器时，应该处理可能出现的异常：

```kotlin
try {
    deployer.deploy(projectPath, config).collect { update ->
        // 处理状态更新
    }
} catch (e: Exception) {
    println("部署过程中发生错误：${e.message}")
}
```

### 6.2 资源配置

根据应用的需求配置适当的资源：

```kotlin
val config = DeploymentConfig(
    name = "my-app",
    resources = ResourceConfig(
        memory = 1024,  // 1GB 内存
        cpu = 2,        // 2 核 CPU
        timeout = 60,   // 60 秒超时
        concurrency = 10 // 10 并发
    )
)
```

### 6.3 环境变量

使用环境变量来配置应用，而不是硬编码敏感信息：

```kotlin
val config = DeploymentConfig(
    name = "my-app",
    environment = mapOf(
        "API_KEY" to System.getenv("API_KEY"),
        "DATABASE_URL" to System.getenv("DATABASE_URL")
    )
)
```

### 6.4 版本管理

使用语义化版本号来管理应用版本：

```kotlin
val config = DeploymentConfig(
    name = "my-app",
    version = "1.2.3" // 主版本.次版本.修订版本
)
```

## 7. 故障排除

### 7.1 常见问题

#### 问题：部署失败，无法构建 Docker 镜像

可能的原因：
- Docker 守护进程未运行
- Dockerfile 有语法错误
- 构建上下文中缺少必要的文件

解决方案：
- 确保 Docker 守护进程正在运行
- 检查 Dockerfile 的语法
- 确保所有必要的文件都在项目目录中

#### 问题：Kubernetes 部署失败

可能的原因：
- Kubernetes 集群未配置或不可访问
- 没有足够的权限
- 资源配额不足

解决方案：
- 检查 Kubernetes 配置
- 确保有足够的权限
- 检查资源配额

#### 问题：Lambda 部署失败

可能的原因：
- AWS 凭证无效或过期
- S3 存储桶不存在或没有权限
- Lambda 函数超过大小限制

解决方案：
- 更新 AWS 凭证
- 检查 S3 存储桶权限
- 减小部署包大小

### 7.2 日志

部署器会记录详细的日志，可以通过查看日志来诊断问题：

```kotlin
// 获取部署结果，包含日志
val result = deployer.getResult("deployment-id")
result.logs.forEach { log ->
    println(log)
}
```

## 8. 总结

KastraX 部署器提供了一种简单、统一的方式来部署 KastraX 应用到不同的环境中。通过使用部署器，开发者可以专注于应用的开发，而不必担心部署的细节。

部署器支持多种部署目标，包括 Docker、Kubernetes 和 AWS Lambda，并提供了灵活的配置选项，以满足不同的部署需求。
