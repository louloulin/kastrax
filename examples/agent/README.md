# KastraX 代理示例

本目录包含了 KastraX 代理系统的示例代码，展示了代理系统的各种功能和用法。

## 主要功能

KastraX 代理系统提供了以下主要功能：

1. **基本代理创建和配置**
   - 支持多种LLM模型
   - 自定义系统指令
   - 工具集成

2. **多代理协作**
   - 代理间通信
   - 任务分配和协调
   - 结果聚合

3. **代理网络**
   - 复杂代理拓扑结构
   - 层次化代理系统
   - 专家代理网络

## 示例说明

### SimpleAgentExample

`SimpleAgentExample.kt` 展示了一个基本的代理示例，包括：

- 创建和配置代理
- 添加工具
- 生成回复
- 处理流式响应

### MultiAgentExample

`MultiAgentExample.kt` 展示了多个代理协同工作的示例，包括：

- 创建多个专业代理
- 代理间通信
- 任务分配和结果聚合

### AgentNetworkExample

`AgentNetworkExample.kt` 展示了代理网络的示例，包括：

- 创建代理网络拓扑
- 层次化代理系统
- 专家代理协作

## 使用方法

要运行示例，请执行以下命令：

```bash
./gradlew :examples:run --args="ai.kastrax.examples.agent.SimpleAgentExampleKt"
./gradlew :examples:run --args="ai.kastrax.examples.agent.MultiAgentExampleKt"
./gradlew :examples:run --args="ai.kastrax.examples.agent.AgentNetworkExampleKt"
```
