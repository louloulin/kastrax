# KastraX 打包系统

## 1. 概述

KastraX 打包系统是 KastraX 部署器的核心组件之一，用于将应用程序打包成可部署的格式。目前支持 JAR 打包，未来将支持更多打包格式。

打包系统的设计目标是提供一个统一的接口，使开发者能够轻松地将 KastraX 应用打包成不同的格式，而无需了解每种格式的具体细节。

## 2. 核心组件

### 2.1 打包系统接口（PackagingSystem）

所有打包系统都实现了 `PackagingSystem` 接口：

```kotlin
interface PackagingSystem {
    val name: String
    
    fun packageApplication(projectPath: Path, config: PackagingConfig): PackagingResult
    
    fun validatePackage(packageFile: File): Boolean
}
```

### 2.2 打包配置（PackagingConfig）

打包配置用于指定打包的参数：

```kotlin
data class PackagingConfig(
    val name: String,
    val version: String = "1.0.0",
    val mainClass: String? = null,
    val includeResources: Boolean = true,
    val excludePatterns: List<String> = emptyList()
)
```

### 2.3 打包结果（PackagingResult）

打包结果包含打包的状态和结果：

```kotlin
data class PackagingResult(
    val success: Boolean,
    val packageFile: File? = null,
    val message: String = "",
    val logs: List<String> = emptyList()
)
```

## 3. 内置打包系统

### 3.1 JAR 打包系统（JarPackagingSystem）

JAR 打包系统用于将 Java/Kotlin 应用打包为 JAR 文件：

```kotlin
val packagingSystem = DeployerFactory.createPackagingSystem(PackagingSystemType.JAR)
```

功能特点：
- 自动检测 Gradle 或 Maven 项目
- 使用项目的构建工具构建应用
- 支持指定主类
- 支持验证 JAR 文件

## 4. 使用示例

### 4.1 基本用法

```kotlin
import ai.kastrax.deployer.DeployerFactory
import ai.kastrax.deployer.PackagingSystemType
import ai.kastrax.deployer.packaging.PackagingConfig
import java.nio.file.Paths

fun main() {
    // 创建打包系统
    val packagingSystem = DeployerFactory.createPackagingSystem(PackagingSystemType.JAR)
    
    // 创建打包配置
    val config = PackagingConfig(
        name = "my-app",
        version = "1.0.0",
        mainClass = "com.example.MainKt"
    )
    
    // 打包应用
    val projectPath = Paths.get("/path/to/project")
    val result = packagingSystem.packageApplication(projectPath, config)
    
    if (result.success) {
        println("打包成功：${result.packageFile?.absolutePath}")
    } else {
        println("打包失败：${result.message}")
        result.logs.forEach { println(it) }
    }
}
```

### 4.2 验证打包结果

```kotlin
val isValid = packagingSystem.validatePackage(result.packageFile!!)
if (isValid) {
    println("打包文件有效")
} else {
    println("打包文件无效")
}
```

## 5. 扩展打包系统

要创建自定义打包系统，只需实现 `PackagingSystem` 接口：

```kotlin
class CustomPackagingSystem : PackagingSystem {
    override val name: String = "Custom Packaging System"
    
    override fun packageApplication(projectPath: Path, config: PackagingConfig): PackagingResult {
        // 实现打包逻辑
    }
    
    override fun validatePackage(packageFile: File): Boolean {
        // 实现验证逻辑
    }
}
```

## 6. 最佳实践

### 6.1 错误处理

在使用打包系统时，应该处理可能出现的异常：

```kotlin
try {
    val result = packagingSystem.packageApplication(projectPath, config)
    // 处理结果
} catch (e: Exception) {
    println("打包过程中发生错误：${e.message}")
}
```

### 6.2 日志处理

打包结果包含详细的日志，可以用于调试：

```kotlin
if (!result.success) {
    println("打包失败：${result.message}")
    println("详细日志：")
    result.logs.forEach { println(it) }
}
```

### 6.3 配置管理

为不同的环境创建不同的打包配置：

```kotlin
val devConfig = PackagingConfig(
    name = "my-app",
    version = "1.0.0-SNAPSHOT",
    mainClass = "com.example.MainKt"
)

val prodConfig = PackagingConfig(
    name = "my-app",
    version = "1.0.0",
    mainClass = "com.example.MainKt"
)
```
