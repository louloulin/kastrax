# Kastrax 协程运行时抽象层

## 简介

kastrax-runtime 模块提供了一个跨平台的协程运行时抽象层，支持在不同环境（JVM、IDEA 插件等）中使用协程。

## 模块结构

- **kastrax-runtime-api**: 定义协程运行时的接口和全局访问点
- **kastrax-runtime-jvm**: 提供基于标准 JVM 环境的协程运行时实现
- **kastrax-runtime-idea**: 提供基于 IntelliJ IDEA 插件环境的协程运行时实现
- **kastrax-runtime-test**: 提供用于测试的协程运行时实现和工具

## 使用方法

### 1. 初始化协程运行时

```kotlin
import ai.kastrax.runtime.coroutines.KastraxCoroutineInitializer
import ai.kastrax.runtime.coroutines.jvm.JvmCoroutineRuntime

fun main() {
    // 初始化 kastrax 协程运行时
    KastraxCoroutineInitializer.initialize(JvmCoroutineRuntime())
    
    // 应用代码...
}
```

### 2. 使用协程运行时

```kotlin
import ai.kastrax.runtime.coroutines.KastraxCoroutineGlobal

// 启动协程
KastraxCoroutineGlobal.launch(owner) {
    // 协程代码...
}

// 阻塞执行
val result = KastraxCoroutineGlobal.runBlocking {
    // 协程代码...
    "result"
}

// 切换调度器
KastraxCoroutineGlobal.withIO {
    // IO 操作...
}
```

### 3. 在 Actor 中使用

```kotlin
import actor.proto.Actor
import actor.proto.Context
import ai.kastrax.runtime.coroutines.KastraxCoroutineGlobal

class MyActor : Actor {
    override suspend fun Context.receive(msg: Any) {
        when (msg) {
            is String -> {
                // 使用 IO 调度器执行耗时操作
                val result = KastraxCoroutineGlobal.withIO {
                    // IO 操作...
                    "processed: $msg"
                }
                sender?.tell(result)
            }
        }
    }
}
```

## 扩展自定义运行时

如果需要为特定平台实现自定义的协程运行时，可以实现 `KastraxCoroutineRuntime` 接口：

```kotlin
import ai.kastrax.runtime.coroutines.KastraxCoroutineRuntime
import ai.kastrax.runtime.coroutines.KastraxCoroutineScope

class CustomCoroutineRuntime : KastraxCoroutineRuntime {
    override fun getScope(owner: Any): KastraxCoroutineScope {
        // 实现自定义逻辑
    }
    
    override fun getScope(context: CoroutineContext): KastraxCoroutineScope {
        // 实现自定义逻辑
    }
    
    // 实现其他方法...
}
```

## 注意事项

1. 在应用启动时，确保调用 `KastraxCoroutineInitializer.initialize()` 初始化协程运行时。
2. 在测试中，可以使用 `KastraxCoroutineRuntimeFactory.setRuntime()` 设置测试用的运行时实现。
3. 避免直接使用 kotlinx.coroutines 的 API，而是通过 KastraxCoroutineGlobal 访问协程功能。
