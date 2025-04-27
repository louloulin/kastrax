# KastraX Rust SDK

KastraX的Rust语言SDK，允许在Rust应用程序中使用KastraX AI Agent功能。

## 安装

将以下内容添加到您的`Cargo.toml`文件中：

```toml
[dependencies]
kastrax = "0.1.0"
```

## 使用示例

```rust
use kastrax::{Agent, Tool, Config};

fn main() -> Result<(), Box<dyn std::error::Error>> {
    // 初始化SDK
    let config = Config::new()
        .with_api_key("your-api-key")
        .with_log_level("info");
    
    kastrax::init(config)?;
    
    // 创建Agent
    let mut agent = Agent::builder()
        .name("my-agent")
        .model("deepseek-coder")
        .build()?;
    
    // 添加工具
    agent.add_tool(Tool::new("calculator", |input| {
        // 工具实现
        Ok("4".to_string())
    }))?;
    
    // 运行Agent
    let response = agent.run("Calculate 2+2")?;
    println!("{}", response);
    
    // 清理资源
    kastrax::shutdown();
    
    Ok(())
}
```

## API参考

### Agent

创建和管理AI Agent的主要结构体。

```rust
let agent = Agent::builder()
    .name("my-agent")
    .model("deepseek-coder")
    .temperature(0.7)
    .max_tokens(1000)
    .build()?;
```

#### 方法

- `run(&self, input: &str) -> Result<String, Error>` - 使用给定输入运行Agent
- `add_tool(&mut self, tool: Tool) -> Result<(), Error>` - 向Agent添加工具
- `set_memory(&mut self, memory: Memory) -> Result<(), Error>` - 设置Agent的记忆
- `get_state(&self) -> AgentState` - 获取Agent的当前状态

### Tool

定义Agent可以使用的工具的结构体。

```rust
let tool = Tool::new("calculator", |input| {
    // 工具实现
    Ok("4".to_string())
});
```

### Memory

管理Agent记忆的结构体。

```rust
let memory = Memory::new(MemoryType::InMemory)
    .with_max_messages(100);
```

## 示例

请参阅`examples`目录获取完整的使用示例。

## 构建说明

### 前提条件

- Rust 1.65.0或更高版本
- Cargo
- JDK 17或更高版本（用于JNI）

### 构建步骤

```bash
# 克隆仓库
git clone https://github.com/kastrax/kastrax.git
cd kastrax

# 构建Rust SDK
cd graal-native/sdk-rust/rust
cargo build --release
```

## 许可证

MIT
