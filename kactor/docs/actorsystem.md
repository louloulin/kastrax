# ProtoActor-Kotlin ActorSystem

本文档描述了 ProtoActor-Kotlin 的 ActorSystem 功能。

## 概述

ActorSystem 是 ProtoActor-Kotlin 的核心组件，负责 Actor 的创建、管理和生命周期。它提供了一个明确的 API 来管理 Actor，替代了旧的全局函数。

## 架构

ActorSystem 由以下组件组成：

1. **ActorSystem**：Actor 系统的主要入口点，负责 Actor 的创建和管理
2. **ProcessRegistry**：进程注册表，管理 PID 映射
3. **RootContext**：根上下文，用于发送消息和创建 Actor
4. **DeadLetterProcess**：死信进程，处理发送到不存在的 Actor 的消息

## 使用方法

### 创建 ActorSystem

```kotlin
val system = ActorSystem("my-system")
```

### 创建 Actor

```kotlin
val props = fromProducer { MyActor() }
val pid = system.actorOf(props)
```

### 创建命名 Actor

```kotlin
val pid = system.actorOf(props, "my-actor")
```

### 发送消息

```kotlin
system.send(pid, "hello")
```

### 请求-响应模式

```kotlin
val response = system.requestAsync<String>(pid, "request", Duration.ofSeconds(5))
```

### 停止 Actor

```kotlin
system.stop(pid)
```

### 发送毒丸消息

```kotlin
system.poison(pid)
```

## 多系统支持

ProtoActor-Kotlin 支持多个 ActorSystem 实例，每个实例都有自己的 Actor 集合：

```kotlin
val system1 = ActorSystem("system1")
val system2 = ActorSystem("system2")

val pid1 = system1.actorOf(props)
val pid2 = system2.actorOf(props)
```

## 默认系统

为了向后兼容，ProtoActor-Kotlin 提供了一个默认的 ActorSystem 实例，可以通过以下方式访问：

```kotlin
val system = ActorSystem.default()
```

旧的全局函数现在委托给默认的 ActorSystem 实例：

```kotlin
// 这些调用使用默认的 ActorSystem
val pid = spawn(props)
send(pid, "hello")
stop(pid)
```

## 与远程通信的集成

ActorSystem 与远程通信功能集成，允许创建和管理远程 Actor：

```kotlin
val system = ActorSystem("my-system")
val remote = Remote.get(system)
remote.start("localhost", 8090)
```

## 监督

ActorSystem 支持监督，允许父 Actor 监督其子 Actor：

```kotlin
class ParentActor : Actor {
    override suspend fun Context.receive(msg: Any) {
        when (msg) {
            is String -> {
                if (msg == "create-child") {
                    val props = fromProducer { ChildActor() }
                    val child = spawnChild(props)
                    watch(child)
                }
            }
            is Terminated -> {
                println("Child terminated: ${msg.who}")
            }
        }
    }
}
```

## 生命周期管理

ActorSystem 负责 Actor 的生命周期管理，包括创建、停止和重启：

```kotlin
// 创建 Actor
val pid = system.actorOf(props)

// 停止 Actor
system.stop(pid)

// 发送毒丸消息
system.poison(pid)
```

## 与 Go 实现的互操作性

ProtoActor-Kotlin 的 ActorSystem 与 ProtoActor-Go 的 ActorSystem 完全兼容，允许 Kotlin 和 Go 实现之间的互操作性：

```kotlin
// Kotlin 代码
val system = ActorSystem("kotlin-system")
val remote = Remote.get(system)
remote.start("localhost", 8090)
```

```go
// Go 代码
system := actor.NewActorSystem()
remote := remote.NewRemote(system, remote.Configure())
remote.Start("go-server", 8090)
```
