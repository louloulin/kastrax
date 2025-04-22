# KastraX 部署策略

## 1. 概述

KastraX 部署策略是 KastraX 部署器的高级功能，用于实现不同的部署模式，如蓝绿部署、金丝雀部署等。部署策略可以帮助开发者实现零停机部署、灰度发布等高级部署场景。

部署策略的设计目标是提供一个统一的接口，使开发者能够轻松地实现不同的部署模式，而无需了解每种模式的具体细节。

## 2. 核心组件

### 2.1 部署策略接口（DeploymentStrategy）

所有部署策略都实现了 `DeploymentStrategy` 接口：

```kotlin
interface DeploymentStrategy {
    val type: DeploymentStrategyType
    
    fun deploy(projectPath: Path, config: DeploymentConfig): Flow<DeploymentStatusUpdate>
    
    suspend fun rollback(deploymentId: String): Boolean
    
    suspend fun getResult(deploymentId: String): DeploymentResult
}
```

### 2.2 部署策略类型（DeploymentStrategyType）

部署策略类型枚举定义了支持的部署策略：

```kotlin
enum class DeploymentStrategyType {
    DIRECT,      // 直接部署
    BLUE_GREEN,  // 蓝绿部署
    CANARY       // 金丝雀部署
}
```

## 3. 内置部署策略

### 3.1 蓝绿部署策略（BlueGreenDeploymentStrategy）

蓝绿部署策略实现了蓝绿部署模式，即同时维护两个环境（蓝环境和绿环境），一个用于生产，一个用于准备下一个版本：

```kotlin
val deployer = DeployerFactory.createDeployer(DeployerType.DOCKER)
val strategy = DeployerFactory.createDeploymentStrategy(
    DeploymentStrategyType.BLUE_GREEN,
    deployer
)
```

蓝绿部署配置：

```kotlin
data class BlueGreenConfig(
    val healthCheckUrl: String? = null,
    val healthCheckTimeout: Int = 60,
    val switchDelay: Int = 10,
    val autoSwitch: Boolean = true
)
```

功能特点：
- 支持自动或手动切换流量
- 支持健康检查
- 支持回滚到上一个版本
- 支持自定义切换延迟

## 4. 使用示例

### 4.1 蓝绿部署

```kotlin
import ai.kastrax.deployer.DeployerFactory
import ai.kastrax.deployer.DeployerType
import ai.kastrax.deployer.DeploymentConfig
import ai.kastrax.deployer.strategy.DeploymentStrategyType
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import java.nio.file.Paths

fun main() = runBlocking {
    // 创建部署器
    val deployer = DeployerFactory.createDeployer(DeployerType.DOCKER)
    
    // 创建蓝绿部署策略
    val strategy = DeployerFactory.createDeploymentStrategy(
        DeploymentStrategyType.BLUE_GREEN,
        deployer
    )
    
    // 创建部署配置
    val config = DeploymentConfig(
        name = "my-app",
        version = "1.0.0",
        environment = mapOf(
            "LOG_LEVEL" to "INFO"
        )
    )
    
    // 执行部署
    val projectPath = Paths.get("/path/to/project")
    
    var deploymentId: String? = null
    
    strategy.deploy(projectPath, config).collect { update ->
        println("${update.status}: ${update.message} (${update.progress}%)")
        
        // 保存部署 ID，用于后续操作
        deploymentId = update.timestamp.toString()
    }
    
    // 获取部署结果
    val result = strategy.getResult(deploymentId!!)
    
    if (result.success) {
        println("部署成功！")
        println("URL: ${result.url}")
        
        // 如果需要回滚
        // val rollbackSuccess = strategy.rollback(deploymentId!!)
        // if (rollbackSuccess) {
        //     println("回滚成功")
        // } else {
        //     println("回滚失败")
        // }
    } else {
        println("部署失败: ${result.message}")
    }
}
```

### 4.2 自定义蓝绿部署配置

```kotlin
import ai.kastrax.deployer.strategy.BlueGreenConfig

// 创建蓝绿部署配置
val blueGreenConfig = BlueGreenConfig(
    healthCheckUrl = "http://localhost:8080/health",
    healthCheckTimeout = 30,
    switchDelay = 20,
    autoSwitch = false // 手动切换
)

// 使用配置文件
val strategy = DeployerFactory.createDeploymentStrategy(
    DeploymentStrategyType.BLUE_GREEN,
    deployer,
    "/path/to/config.conf" // 包含 blueGreen 部分的配置文件
)
```

## 5. 扩展部署策略

要创建自定义部署策略，只需实现 `DeploymentStrategy` 接口：

```kotlin
class CustomDeploymentStrategy(
    private val deployer: Deployer
) : DeploymentStrategy {
    override val type: DeploymentStrategyType = DeploymentStrategyType.DIRECT
    
    override fun deploy(projectPath: Path, config: DeploymentConfig): Flow<DeploymentStatusUpdate> {
        // 实现部署逻辑
    }
    
    override suspend fun rollback(deploymentId: String): Boolean {
        // 实现回滚逻辑
    }
    
    override suspend fun getResult(deploymentId: String): DeploymentResult {
        // 实现获取结果逻辑
    }
}
```

## 6. 最佳实践

### 6.1 健康检查

在蓝绿部署中，始终配置健康检查以确保新环境正常运行：

```kotlin
val blueGreenConfig = BlueGreenConfig(
    healthCheckUrl = "http://localhost:8080/health",
    healthCheckTimeout = 60
)
```

### 6.2 手动切换

对于关键系统，考虑使用手动切换模式，以便在切换前进行额外的验证：

```kotlin
val blueGreenConfig = BlueGreenConfig(
    autoSwitch = false
)
```

### 6.3 回滚计划

始终准备好回滚计划，以便在部署出现问题时快速恢复：

```kotlin
// 部署
val deploymentId = // 从部署结果中获取

// 如果需要回滚
val rollbackSuccess = strategy.rollback(deploymentId)
```
