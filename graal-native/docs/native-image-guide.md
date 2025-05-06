# GraalVM 原生镜像支持指南

本文档提供了在 Kastrax 项目中使用 GraalVM 原生镜像的详细指南，包括序列化配置、反射配置以及常见问题的解决方案。

## 目录

1. [简介](#简介)
2. [序列化配置](#序列化配置)
3. [反射配置](#反射配置)
4. [构建原生镜像](#构建原生镜像)
5. [常见问题及解决方案](#常见问题及解决方案)
6. [最佳实践](#最佳实践)

## 简介

GraalVM 原生镜像是一种将 Java 应用程序编译成独立的本地可执行文件的技术，它可以显著减少启动时间和内存占用。然而，由于原生镜像在构建时进行静态分析，它对反射、动态类加载和资源访问有一些限制。

在 Kastrax 项目中，我们使用 GraalVM 原生镜像来提高应用程序的性能和部署便捷性。本指南将帮助你理解和配置 GraalVM 原生镜像支持。

## 序列化配置

在 GraalVM 原生镜像中，kotlinx.serialization 需要特殊配置，因为它依赖于在运行时发现序列化器，而这在原生镜像中是不可靠的。

### SerializationInitializer

我们创建了 `SerializationInitializer` 类来显式注册所有需要序列化的类：

```kotlin
object SerializationInitializer {
    val module = SerializersModule {
        // 可以在这里添加多态序列化配置
    }

    val json = Json {
        serializersModule = module
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
        prettyPrint = false
        coerceInputValues = true
    }

    fun initialize() {
        val serializers = listOf(
            // DeepSeek 聊天完成相关类
            kotlinx.serialization.serializer<DeepSeekChatCompletionRequest>(),
            kotlinx.serialization.serializer<DeepSeekMessage>(),
            // ... 其他序列化器
        )

        // 确保序列化器被加载
        serializers.forEach { serializer ->
            serializer.descriptor
        }
    }
}
```

### 使用 SerializationInitializer

在应用程序启动时，需要调用 `SerializationInitializer.initialize()` 方法来确保所有序列化器被正确加载：

```kotlin
fun main() {
    // 初始化序列化模块
    SerializationInitializer.initialize()
    
    // 使用初始化后的 JSON 实例
    val json = SerializationInitializer.json
    
    // 应用程序逻辑
    // ...
}
```

## 反射配置

GraalVM 原生镜像需要知道哪些类和方法需要通过反射访问。我们通过配置文件来提供这些信息。

### reflection-config.json

这个文件位于 `META-INF/native-image/reflection-config.json`，它列出了所有需要通过反射访问的类：

```json
[
  {
    "name": "ai.kastrax.integrations.deepseek.DeepSeekChatCompletionRequest$serializer",
    "allDeclaredConstructors": true,
    "allPublicConstructors": true,
    "allDeclaredMethods": true,
    "allPublicMethods": true,
    "allDeclaredFields": true,
    "allPublicFields": true
  },
  // ... 其他需要反射的类
]
```

### serialization-config.json

这个文件位于 `META-INF/native-image/serialization-config.json`，它列出了所有需要序列化的类：

```json
[
  {
    "name": "ai.kastrax.integrations.deepseek.DeepSeekChatCompletionRequest",
    "allDeclaredConstructors": true,
    "allPublicConstructors": true,
    "allDeclaredMethods": true,
    "allPublicMethods": true,
    "allDeclaredFields": true,
    "allPublicFields": true
  },
  // ... 其他需要序列化的类
]
```

## 构建原生镜像

要构建 GraalVM 原生镜像，需要使用 GraalVM 的 `native-image` 工具。在 Kastrax 项目中，我们通过 Gradle 插件来简化这个过程。

### 构建命令

```bash
./gradlew :graal-native:nativeCompile
```

这个命令会在 `graal-native/build/native/nativeCompile` 目录下生成一个可执行文件。

### 构建参数

在 `build.gradle.kts` 文件中，我们配置了原生镜像的构建参数：

```kotlin
graalvmNative {
    binaries {
        named("main") {
            imageName.set("kastrax-native")
            mainClass.set("ai.kastrax.graal.Main")
            buildArgs.add("--no-fallback")
            buildArgs.add("-H:+ReportExceptionStackTraces")
            buildArgs.add("-H:+PrintClassInitialization")
            buildArgs.add("-H:ReflectionConfigurationFiles=${project.projectDir}/META-INF/native-image/reflection-config.json")
            buildArgs.add("-H:SerializationConfigurationFiles=${project.projectDir}/META-INF/native-image/serialization-config.json")
            buildArgs.add("-H:ResourceConfigurationFiles=${project.projectDir}/META-INF/native-image/resource-config.json")
        }
    }
}
```

## 常见问题及解决方案

### 1. 序列化错误

**问题**：在原生镜像中运行时出现序列化相关的错误，如 `kotlinx.serialization.SerializationException: Serializer for class 'XXX' is not found`。

**解决方案**：
- 确保所有需要序列化的类都在 `SerializationInitializer` 中注册
- 确保所有需要序列化的类都在 `serialization-config.json` 中列出
- 确保所有序列化器类都在 `reflection-config.json` 中列出

### 2. 反射错误

**问题**：在原生镜像中运行时出现反射相关的错误，如 `java.lang.ClassNotFoundException` 或 `java.lang.NoSuchMethodException`。

**解决方案**：
- 确保所有需要通过反射访问的类都在 `reflection-config.json` 中列出
- 对于需要通过反射访问的方法，确保在 `reflection-config.json` 中设置了 `"allDeclaredMethods": true` 和 `"allPublicMethods": true`

### 3. 资源访问错误

**问题**：在原生镜像中运行时无法访问资源文件，如配置文件或模板文件。

**解决方案**：
- 确保所有需要访问的资源文件都在 `resource-config.json` 中列出
- 使用 `Class.getResourceAsStream()` 或 `ClassLoader.getResourceAsStream()` 来访问资源文件

## 最佳实践

1. **避免运行时反射**：尽量避免在运行时使用反射，特别是动态类加载和方法调用。

2. **显式注册序列化器**：对于所有需要序列化的类，都应该显式注册其序列化器，而不是依赖于运行时发现。

3. **测试原生镜像**：在构建原生镜像之前，先编写测试来验证序列化和反射功能是否正常工作。

4. **使用构建时初始化**：尽量在构建时完成初始化工作，而不是在运行时。

5. **保持配置文件更新**：当添加新的需要序列化或反射的类时，记得更新相应的配置文件。

6. **使用日志记录问题**：在原生镜像中添加详细的日志记录，以便在出现问题时进行调试。

7. **分阶段构建**：先构建一个包含所有功能的 JAR 文件，测试通过后再构建原生镜像。

通过遵循这些最佳实践，你可以确保 Kastrax 项目在 GraalVM 原生镜像中正常运行，并充分利用原生镜像带来的性能优势。
