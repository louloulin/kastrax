# KastraX 内存系统示例

本目录包含了 KastraX 内存系统的示例代码，展示了内存系统的各种功能和用法。

## 主要功能

KastraX 内存系统提供了以下主要功能：

1. **基本内存管理**
   - 消息存储和检索
   - 对话线程管理
   - 上下文窗口控制

2. **语义搜索**
   - 向量嵌入生成
   - 相似度搜索
   - 混合搜索（关键词+语义）

3. **工作内存**
   - 结构化信息存储
   - 动态更新
   - 模板支持

4. **持久化存储**
   - 数据库集成
   - 分布式存储
   - 备份和恢复

## 示例说明

### BasicMemoryExample

`BasicMemoryExample.kt` 展示了基本内存系统的使用，包括：

- 创建内存系统
- 存储和检索消息
- 管理对话线程
- 使用上下文窗口

### SemanticSearchExample

`SemanticSearchExample.kt` 展示了语义搜索功能，包括：

- 创建向量嵌入
- 执行相似度搜索
- 混合搜索实现
- 结果重排序

### WorkingMemoryExample

`WorkingMemoryExample.kt` 展示了工作内存功能，包括：

- 创建工作内存
- 使用内存模板
- 动态更新内存
- 与代理集成

### PersistentMemoryExample

`PersistentMemoryExample.kt` 展示了持久化内存存储，包括：

- 数据库存储配置
- 数据持久化
- 恢复对话历史
- 分布式存储选项

## 使用方法

要运行示例，请执行以下命令：

```bash
./gradlew :examples:run --args="ai.kastrax.examples.memory.BasicMemoryExampleKt"
./gradlew :examples:run --args="ai.kastrax.examples.memory.SemanticSearchExampleKt"
./gradlew :examples:run --args="ai.kastrax.examples.memory.WorkingMemoryExampleKt"
./gradlew :examples:run --args="ai.kastrax.examples.memory.PersistentMemoryExampleKt"
```
