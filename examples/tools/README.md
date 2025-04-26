# KastraX 工具示例

本目录包含了 KastraX 工具系统的示例代码，展示了工具系统的各种功能和用法。

## 主要功能

KastraX 工具系统提供了以下主要功能：

1. **基本工具创建**
   - 工具定义和配置
   - 输入/输出模式
   - 执行逻辑

2. **内置工具**
   - Web 搜索工具
   - 文件系统工具
   - 计算工具

3. **自定义工具**
   - 创建专用工具
   - 工具链组合
   - 工具版本控制

4. **工具调用**
   - 同步调用
   - 异步调用
   - 流式响应

## 示例说明

### BasicToolExample

`BasicToolExample.kt` 展示了基本工具的创建和使用，包括：

- 定义工具
- 配置输入/输出模式
- 实现执行逻辑
- 与代理集成

### WebSearchToolExample

`WebSearchToolExample.kt` 展示了 Web 搜索工具的使用，包括：

- 配置搜索参数
- 执行搜索查询
- 处理搜索结果
- 结果格式化

### FileSystemToolExample

`FileSystemToolExample.kt` 展示了文件系统工具的使用，包括：

- 文件读写
- 目录操作
- 文件搜索
- 权限管理

### CustomToolExample

`CustomToolExample.kt` 展示了自定义工具的创建，包括：

- 创建专用工具
- 工具链组合
- 工具版本控制
- 错误处理

## 使用方法

要运行示例，请执行以下命令：

```bash
./gradlew :examples:run --args="ai.kastrax.examples.tools.BasicToolExampleKt"
./gradlew :examples:run --args="ai.kastrax.examples.tools.WebSearchToolExampleKt"
./gradlew :examples:run --args="ai.kastrax.examples.tools.FileSystemToolExampleKt"
./gradlew :examples:run --args="ai.kastrax.examples.tools.CustomToolExampleKt"
```
