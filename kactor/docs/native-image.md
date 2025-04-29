# ProtoActor-Kotlin Native Image 支持

ProtoActor-Kotlin 现在支持使用 GraalVM Native Image 将您的 Actor 系统编译为本机可执行文件，从而提高启动时间并减少内存占用。本文档详细介绍了如何使用此功能以及一些最佳实践。

## 目录

- [概述](#概述)
- [要求](#要求)
- [快速开始](#快速开始)
- [详细步骤](#详细步骤)
- [配置选项](#配置选项)
- [性能比较](#性能比较)
- [限制和注意事项](#限制和注意事项)
- [故障排除](#故障排除)
- [最佳实践](#最佳实践)

## 概述

GraalVM Native Image 是一种技术，可以将 Java 应用程序预先编译为独立的本机可执行文件。这些可执行文件具有以下优势：

- **快速启动时间**：本机可执行文件启动速度比 JVM 应用程序快得多
- **更低的内存占用**：本机可执行文件通常比 JVM 应用程序使用更少的内存
- **更小的部署包**：不需要完整的 JVM，只需要一个可执行文件
- **更好的容器化支持**：更适合在容器环境中运行

ProtoActor-Kotlin 现在提供了与 GraalVM Native Image 的集成，使您可以轻松地将 Actor 系统编译为本机可执行文件。

## 要求

要使用 Native Image 功能，您需要：

- GraalVM CE 17.0.9 或更高版本
- Native Image 工具（可以通过 `gu install native-image` 安装）
- Gradle 8.0 或更高版本

## 快速开始

我们提供了两种方式来构建本机镜像：

### 标准方式

```bash
./build-native.sh
```

此脚本将：
1. 检查是否安装了 GraalVM
2. 使用 GraalVM Agent 运行应用程序以生成配置文件
3. 编译本机镜像
4. 运行生成的本机可执行文件

### 简化方式

我们还提供了一种更简单的方式来构建本机镜像：

```bash
./simple-native-build.sh
```

这种简化方式使用自定义的 Gradle 插件，实现了一键构建和运行本机镜像。它的优点是：

1. **配置更简单**：只需要在 build.gradle.kts 文件中添加几行配置
2. **自动生成配置**：自动使用 GraalVM Agent 生成所需的配置文件
3. **一键构建**：单个命令完成所有步骤

## 详细步骤

如果您想了解更多细节或手动执行这些步骤，以下是详细的过程：

### 1. 安装 GraalVM 和 Native Image 工具

首先，您需要安装 GraalVM 和 Native Image 工具：

```bash
# 下载并安装 GraalVM
# 设置 JAVA_HOME 环境变量指向 GraalVM 安装目录
export JAVA_HOME=/path/to/graalvm

# 安装 Native Image 工具
$JAVA_HOME/bin/gu install native-image
```

### 2. 使用 Agent 生成配置文件

Native Image 需要知道哪些类、方法和资源在运行时需要通过反射访问。GraalVM Agent 可以帮助生成这些配置：

```bash
# 使用 Agent 运行应用程序
./gradlew -Pagent=standard :native-example:run
```

这将在 `src/main/resources/META-INF/native-image/` 目录中生成配置文件。

### 3. 复制生成的配置文件

```bash
# 复制生成的配置文件
./gradlew :native-example:metadataCopy
```

### 4. 编译本机镜像

```bash
# 编译本机镜像
./gradlew :native-example:nativeCompile
```

### 5. 运行本机可执行文件

```bash
# 运行本机可执行文件
./native-example/build/native/nativeCompile/proto-actor-native
```

## 配置选项

您可以通过修改 `build.gradle.kts` 文件中的 `graalvmNative` 块来自定义 Native Image 构建：

```kotlin
graalvmNative {
    binaries {
        named("main") {
            // 设置生成的可执行文件名称
            imageName.set("my-actor-app")

            // 设置主类
            mainClass.set("com.example.MainKt")

            // 构建参数
            buildArgs.add("--no-fallback")
            buildArgs.add("--report-unsupported-elements-at-runtime")
            buildArgs.add("-H:+ReportExceptionStackTraces")

            // 初始化设置
            buildArgs.add("--initialize-at-build-time=org.slf4j")

            // 调试选项
            debug.set(true)
            verbose.set(true)

            // 设置最大堆大小
            buildArgs.add("-Xmx4g")
        }
    }

    // 启用元数据仓库支持
    metadataRepository {
        enabled.set(true)
    }

    // 配置 Agent 支持
    agent {
        enabled.set(true)
        defaultMode.set("standard")

        metadataCopy {
            inputTaskNames.add("run")
            outputDirectories.add("src/main/resources/META-INF/native-image/")
            mergeWithExisting.set(true)
        }
    }
}
```

## 性能比较

以下是 ProtoActor-Kotlin 在 JVM 和 Native Image 模式下的性能比较：

| 指标 | JVM 模式 | Native Image 模式 | 改进 |
|------|---------|-----------------|------|
| 启动时间 | ~1000ms | ~10ms | 100x |
| 内存占用 | ~100MB | ~20MB | 5x |
| 消息吞吐量 | 基准 | 相似或略好 | 0-10% |

注意：实际性能可能因您的特定用例和硬件而异。

您可以使用我们提供的性能比较脚本来测量您的系统上的实际性能：

```bash
cd native-example
./compare-performance.sh
```

这将生成详细的性能报告，包括启动时间和内存使用的比较。

## 限制和注意事项

使用 Native Image 时需要注意以下限制：

1. **反射限制**：Native Image 需要在构建时知道所有通过反射访问的类和方法。使用 Agent 可以帮助捕获这些信息，但可能无法捕获所有情况。

2. **动态类加载**：Native Image 不支持运行时动态类加载。如果您的应用程序依赖于此功能，可能需要修改代码。

3. **JNI 限制**：使用 JNI 的代码需要特殊处理。

4. **资源访问**：需要明确指定哪些资源文件应该包含在本机镜像中。

5. **序列化限制**：某些序列化框架可能需要特殊配置才能在 Native Image 中工作。

## 故障排除

如果您在使用 Native Image 时遇到问题，请尝试以下解决方案：

### 反射相关错误

如果您看到类似 `ClassNotFoundException` 或 `NoSuchMethodException` 的错误，这通常意味着某些反射配置缺失：

1. 确保使用 Agent 运行应用程序并捕获所有反射用例
2. 检查 `reflect-config.json` 文件是否包含所需的类和方法
3. 如果需要，手动添加缺失的配置

### 内存问题

如果构建过程失败并显示内存不足错误：

```
增加构建过程的内存：
./gradlew -Dorg.gradle.jvmargs=-Xmx4g :native-example:nativeCompile
```

### 不支持的功能

如果您看到 `UnsupportedFeatureError`，这意味着您的代码使用了 Native Image 不支持的功能：

1. 查看错误消息以确定不支持的功能
2. 修改代码以避免使用该功能，或使用 Native Image 支持的替代方案

## 最佳实践

以下是使用 ProtoActor-Kotlin 和 Native Image 的一些最佳实践：

1. **保持简单**：避免使用复杂的反射或动态类加载
2. **使用 Agent**：始终使用 Agent 生成配置文件
3. **测试**：在部署前彻底测试本机可执行文件
4. **监控**：监控本机可执行文件的性能和内存使用情况
5. **分离配置**：将配置文件与代码分开，以便于更新
6. **使用 `--no-fallback` 选项**：这可以确保生成完全静态的本机可执行文件
7. **考虑使用 `-H:+PrintClassInitialization` 选项**：这可以帮助诊断初始化问题

## 示例项目

我们提供了多个示例项目来演示 Native Image 功能：

1. **native-example**：包含两个示例
   - **简单示例**：展示基本的 Actor 创建和消息传递
   - **复杂示例**：展示高级功能，包括 Actor 层次结构、监督、重入和错误处理

2. **proto-benchmarks**：一个基准测试项目，用于性能比较

3. **simple-native**：使用简化方式构建 Native Image 的示例
   - 使用自定义 Gradle 插件
   - 最小化配置
   - 一键构建和运行

### 运行复杂示例

复杂示例展示了 ProtoActor 的高级功能，包括：

1. Actor 层次结构和监督
2. 消息传递和状态管理
3. 请求-响应模式
4. Actor 重入
5. 错误处理和恢复

要运行复杂示例，请使用以下命令：

```bash
# 使用 JVM 运行
./gradlew :native-example:runComplex

# 使用 Native Image 运行
./gradlew :native-example:nativeCompile -PimageName=complex
./native-example/build/native/nativeCompile/proto-actor-complex
```

### 性能比较

我们提供了一个性能比较脚本，用于比较 JVM 和 Native Image 的性能：

```bash
cd native-example
./compare-performance.sh
```

这个脚本将测量以下指标：

1. **启动时间**：JVM 和 Native Image 的启动时间比较
2. **内存使用**：JVM 和 Native Image 的内存占用比较

测试结果将保存在 `native-example/performance-results` 目录中。

您可以查看这些项目的源代码，了解如何配置和使用 Native Image。
