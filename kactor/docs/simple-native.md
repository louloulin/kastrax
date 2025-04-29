# 简化的 Native Image 构建

ProtoActor-Kotlin 提供了一种简化的方式来构建 Native Image，使用自定义的 Gradle 插件和简单的构建脚本。这种方法旨在简化 Native Image 的构建过程，减少配置复杂性。

## 目录

- [概述](#概述)
- [要求](#要求)
- [快速开始](#快速开始)
- [配置选项](#配置选项)
- [工作原理](#工作原理)
- [示例项目](#示例项目)
- [故障排除](#故障排除)

## 概述

简化的 Native Image 构建方法使用自定义的 Gradle 插件，提供了以下优势：

- **配置更简单**：只需要在 build.gradle.kts 文件中添加几行配置
- **自动生成配置**：自动使用 GraalVM Agent 生成所需的配置文件
- **一键构建**：单个命令完成所有步骤

## 要求

要使用简化的 Native Image 构建方法，您需要：

- GraalVM CE 17.0.9 或更高版本
- Native Image 工具（可以通过 `gu install native-image` 安装）
- Gradle 8.0 或更高版本

## 快速开始

### 1. 添加插件

在您的 `build.gradle.kts` 文件中添加以下内容：

```kotlin
plugins {
    kotlin("jvm")
    application
    id("actor.proto.gradle.native")
}

application {
    mainClass.set("your.package.MainClassKt")
}

// 配置 Native 编译
nativeCompile {
    mainClass.set("your.package.MainClassKt")
    imageName.set("your-app-name")
    buildArgs.add("--initialize-at-build-time=org.slf4j")
}
```

### 2. 运行构建脚本

使用提供的构建脚本一键构建和运行 Native Image：

```bash
./simple-native-build.sh
```

这个脚本将：
1. 检查是否安装了 GraalVM
2. 使用 GraalVM Agent 运行应用程序以生成配置文件
3. 编译 Native Image
4. 运行生成的本机可执行文件

## 配置选项

简化的 Native Image 构建方法提供了以下配置选项：

```kotlin
nativeCompile {
    // Native Image 的名称，默认为项目名称
    imageName.set("your-app-name")
    
    // 主类，默认为 application.mainClass
    mainClass.set("your.package.MainClassKt")
    
    // 构建参数，传递给 native-image 工具
    buildArgs.add("--initialize-at-build-time=org.slf4j")
    buildArgs.add("--no-fallback")
    
    // 运行参数，传递给生成的本机可执行文件
    runtimeArgs.add("--help")
}
```

## 工作原理

简化的 Native Image 构建方法的工作原理如下：

1. **runWithAgent 任务**：使用 GraalVM Agent 运行应用程序，生成反射配置等元数据
2. **compileNative 任务**：使用生成的配置编译 Native Image
3. **runNative 任务**：运行生成的本机可执行文件
4. **buildNative 任务**：一键执行上述所有步骤

这种方法自动处理了 Native Image 构建过程中的许多复杂细节，使您可以专注于应用程序的开发。

## 示例项目

我们提供了一个示例项目 `simple-native`，演示了如何使用简化的 Native Image 构建方法：

```bash
# 查看示例项目
cd simple-native

# 构建和运行示例
../simple-native-build.sh
```

示例项目展示了：
- 如何配置简化的 Native Image 构建
- 如何使用 GraalVM Agent 自动生成配置
- 如何一键构建和运行 Native Image

## 故障排除

如果您在使用简化的 Native Image 构建方法时遇到问题，请尝试以下解决方案：

### GraalVM 未找到

确保已安装 GraalVM 并设置了 `JAVA_HOME` 环境变量：

```bash
export JAVA_HOME=/path/to/graalvm
```

### native-image 工具未找到

确保已安装 native-image 工具：

```bash
$JAVA_HOME/bin/gu install native-image
```

### 配置生成失败

如果 Agent 无法生成配置，请尝试手动运行应用程序：

```bash
./gradlew :your-module:runWithAgent
```

然后检查 `build/native/config` 目录中的配置文件。

### 编译失败

如果编译失败，请检查错误消息并尝试添加适当的构建参数：

```kotlin
nativeCompile {
    buildArgs.add("--report-unsupported-elements-at-runtime")
    buildArgs.add("-H:+ReportExceptionStackTraces")
}
```
