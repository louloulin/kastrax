# KastraX Go SDK

KastraX的Go语言SDK，允许在Go应用程序中使用KastraX AI Agent功能。

## 安装

```bash
go get github.com/kastrax/kastrax-go
```

## 使用示例

```go
package main

import (
    "fmt"
    "github.com/kastrax/kastrax-go"
)

func main() {
    // 初始化SDK
    config := kastrax.Config{
        APIKey:   "your-api-key",
        LogLevel: "info",
    }
    
    if err := kastrax.Init(config); err != nil {
        panic(err)
    }
    defer kastrax.Shutdown()
    
    // 创建Agent
    agent, err := kastrax.NewAgent(kastrax.AgentConfig{
        Name:  "my-agent",
        Model: "deepseek-coder",
        Options: kastrax.AgentOptions{
            Temperature: 0.7,
            MaxTokens:   1000,
        },
    })
    if err != nil {
        panic(err)
    }
    
    // 添加工具
    err = agent.AddTool(kastrax.Tool{
        Name: "calculator",
        Function: func(input string) (string, error) {
            // 工具实现
            return "4", nil
        },
    })
    if err != nil {
        panic(err)
    }
    
    // 运行Agent
    response, err := agent.Run("Calculate 2+2")
    if err != nil {
        panic(err)
    }
    
    fmt.Println(response)
}
```

## API参考

### Agent

创建和管理AI Agent的主要结构体。

```go
agent, err := kastrax.NewAgent(kastrax.AgentConfig{
    Name:  "my-agent",
    Model: "deepseek-coder",
    Options: kastrax.AgentOptions{
        Temperature: 0.7,
        MaxTokens:   1000,
    },
})
```

#### 方法

- `Run(input string) (string, error)` - 使用给定输入运行Agent
- `AddTool(tool Tool) error` - 向Agent添加工具
- `SetMemory(memory Memory) error` - 设置Agent的记忆
- `GetState() AgentState` - 获取Agent的当前状态

### Tool

定义Agent可以使用的工具的结构体。

```go
tool := kastrax.Tool{
    Name: "calculator",
    Description: "执行计算",
    Function: func(input string) (string, error) {
        // 工具实现
        return "4", nil
    },
}
```

### Memory

管理Agent记忆的结构体。

```go
memory := kastrax.NewMemory(kastrax.MemoryConfig{
    Type: "inmemory",
    Options: kastrax.MemoryOptions{
        MaxMessages: 100,
    },
})
```

## 示例

请参阅`examples`目录获取完整的使用示例。

## 构建说明

### 前提条件

- Go 1.18或更高版本
- JDK 17或更高版本（用于JNI）
- GCC或其他C编译器（用于CGO）

### 构建步骤

```bash
# 克隆仓库
git clone https://github.com/kastrax/kastrax.git
cd kastrax

# 构建Go SDK
cd graal-native/sdk-go/go
go build -buildmode=c-shared -o libkastrax_go.so .
```

## 许可证

MIT
