# KastraX Native 集成指南

本文档概述了KastraX的原生打包方法，并提供了使用Rust、Go和JavaScript SDK的指导。

## 目录

1. [概述](#概述)
2. [GraalVM Native Image](#graalvm-native-image)
3. [SDK架构](#sdk架构)
4. [Rust SDK](#rust-sdk)
5. [Go SDK](#go-sdk)
6. [JavaScript SDK](#javascript-sdk)
7. [从源代码构建](#从源代码构建)
8. [集成示例](#集成示例)
9. [故障排除](#故障排除)

## 概述

KastraX提供两种主要的原生集成方式：

1. **Kotlin Multiplatform Native**: 通过Kotlin/Native直接编译为原生代码
2. **GraalVM Native Image**: 通过GraalVM将JVM代码编译为原生可执行文件

此外，KastraX还提供了多种语言的SDK，使您可以从Rust、Go和JavaScript等语言中使用KastraX：

- **Rust SDK**: 通过JNI与KastraX交互
- **Go SDK**: 通过JNI与KastraX交互
- **JavaScript SDK**: 通过Kotlin/JS直接编译为JavaScript

这种双重方法既允许高性能的独立应用程序，又允许与各种语言的现有代码库无缝集成。

## GraalVM Native Image

### 前提条件

- GraalVM 22.3.0或更高版本
- Native Image工具 (`gu install native-image`)
- 适合您平台的构建工具（Visual Studio、XCode、GCC等）

### 构建Native Image [已实现 ✅]

```bash
# 使用Gradle直接构建简单的Hello World示例
./gradlew :graal-native:buildHelloWorldNative

# 运行生成的Native可执行文件
graal-native/build/native/hello-world/hello-world
```

或者手动构建：

```bash
# 构建项目
./gradlew :graal-native:nativeCompile

# 创建分发包
./gradlew :graal-native:packageNative

# 在build/native/nativeCompile/目录中找到可执行文件
```

### 配置 [已实现 ✅]

KastraX提供以下GraalVM配置文件：

- `reflection-config.json`: 配置用于反射的类
- `resource-config.json`: 指定要包含的资源
- `native-image.properties`: 默认的native-image参数

使用Gradle构建时，这些文件会自动包含。

### 功能特性 [已实现 ✅]

- 简单的Hello World示例
- 使用Gradle直接构建原生可执行文件
- 支持多平台构建

### 限制

使用GraalVM Native Image时，请注意以下限制：

- 动态类加载受限
- 反射需要显式配置
- 某些JVM功能不可用
- 启动更快，但峰值性能可能与JVM不同

## SDK架构

所有SDK遵循共同的架构：

1. **核心桥接层**: 与KastraX的底层FFI/JNI接口
2. **高级API**: 每种语言的惯用API
3. **序列化层**: 处理语言之间的数据转换
4. **资源管理**: 管理KastraX对象的内存和生命周期

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  语言API        │────▶│ 序列化          │────▶│   核心桥接      │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                                                         │
                                                         ▼
                                               ┌─────────────────┐
                                               │  KastraX核心    │
                                               └─────────────────┘
```

### 实现细节

#### 核心桥接层

核心桥接层使用不同的技术为每种语言实现：

- **Rust**: 使用JNI（Java原生接口）与KastraX JVM通信
- **Go**: 使用CGO和JNI在Go和KastraX JVM之间搭建桥梁
- **JavaScript**: 使用Kotlin/JS直接将KastraX代码编译为JavaScript

#### 序列化层

SDK和KastraX核心之间传递的所有数据都使用JSON序列化。这提供了一种语言中立的格式，可以被所有支持的语言轻松处理。序列化层处理：

- 将特定语言的对象转换为JSON
- 将JSON转换为特定语言的对象
- 处理类型转换和验证

#### 资源管理

每个SDK都实现了适当的资源管理，以确保原生资源得到正确清理：

- **Rust**: 使用RAII（资源获取即初始化）模式和Drop特性
- **Go**: 使用defer语句和终结器
- **JavaScript**: 使用JavaScript的垃圾收集和显式清理方法

### SDK开发

要添加对新语言的支持：

1. 在KastraX项目中创建一个新模块
2. 为该语言实现核心桥接层
3. 为该语言创建惯用的API包装器
4. 实现序列化和资源管理
5. 创建示例和文档

## Rust SDK

### 安装

```bash
# 添加到Cargo.toml
[dependencies]
kastrax = "0.1.0"
```

### 基本用法

```rust
use kastrax::{Agent, Tool};

fn main() {
    // 创建Agent
    let mut agent = Agent::builder()
        .name("my-agent")
        .model("deepseek-coder")
        .build();

    // 添加工具
    agent.add_tool(Tool::new("calculator", |input| {
        // 工具实现
    }));

    // 运行Agent
    let response = agent.run("Calculate 2+2");
    println!("{}", response);
}
```

### 高级功能

Rust SDK提供对KastraX全部功能的访问：

- Agent创建和管理
- 工具注册和执行
- 记忆和状态管理
- RAG功能
- 模型集成

## Go SDK

### 安装

```bash
go get github.com/kastrax/kastrax-go
```

### 基本用法

```go
package main

import (
    "fmt"
    "github.com/kastrax/kastrax-go"
)

func main() {
    // 创建Agent
    agent := kastrax.NewAgent(kastrax.AgentConfig{
        Name: "my-agent",
        Model: "deepseek-coder",
    })

    // 添加工具
    agent.AddTool(kastrax.Tool{
        Name: "calculator",
        Function: func(input string) (string, error) {
            // 工具实现
            return "4", nil
        },
    })

    // 运行Agent
    response, err := agent.Run("Calculate 2+2")
    if err != nil {
        panic(err)
    }

    fmt.Println(response)
}
```

### 高级功能

Go SDK通过惯用的Go API提供对所有KastraX功能的访问。

## JavaScript SDK

### 安装

```bash
npm install kastrax
```

### 基本用法

```javascript
import { Agent, Tool } from 'kastrax';

// 创建Agent
const agent = new Agent({
  name: 'my-agent',
  model: 'deepseek-coder'
});

// 添加工具
agent.addTool(new Tool({
  name: 'calculator',
  function: (input) => {
    // 工具实现
    return '4';
  }
}));

// 运行Agent
agent.run('Calculate 2+2')
  .then(response => console.log(response))
  .catch(error => console.error(error));
```

### 浏览器支持

JavaScript SDK同时支持Node.js和浏览器环境。对于浏览器，使用：

```html
<script src="https://unpkg.com/kastrax@0.1.0/dist/kastrax.min.js"></script>
<script>
  const agent = new kastrax.Agent({
    name: 'my-agent',
    model: 'deepseek-coder'
  });

  // 使用Agent...
</script>
```

## 从源代码构建 [已实现 ✅]

### 前提条件

- JDK 17或更高版本
- Gradle 7.3或更高版本
- Rust（用于Rust SDK）
- Go 1.18或更高版本（用于Go SDK）
- Node.js 16或更高版本（用于JavaScript SDK）
- GraalVM 22.3.0或更高版本（用于native image）

### 构建命令

```bash
# 构建简单的Hello World示例
./gradlew :graal-native:buildHelloWorldNative

# 运行生成的原生可执行文件
graal-native/build/native/hello-world/hello-world

# 构建所有内容
./gradlew build

# 构建特定组件
./gradlew :graal-native:build
./gradlew :graal-native:sdk-rust:build
./gradlew :graal-native:sdk-go:build
./gradlew :graal-native:sdk-js:build

# 构建native image
./gradlew :graal-native:nativeCompile
```

## 集成示例

请参阅`examples`目录获取完整的集成示例：

- `examples/rust/`: Rust SDK示例
- `examples/go/`: Go SDK示例
- `examples/js/`: JavaScript SDK示例
- `examples/native/`: Native image示例

## 故障排除

### 常见问题

1. **JNI/FFI错误**: 确保原生库位于正确的位置并且可访问
2. **内存管理**: 使用SDK时注意内存泄漏
3. **GraalVM兼容性**: 某些库可能与GraalVM native-image不兼容

### 获取帮助

- 在GitHub上提交问题
- 加入KastraX社区Discord
- 查看详细文档：https://kastrax.ai/docs/native
