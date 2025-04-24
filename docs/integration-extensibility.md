# KastraX 集成和扩展性

本文档介绍了 KastraX 工作流系统的集成和扩展性功能，包括插件系统、自定义步骤、外部系统连接器和工作流导入/导出功能。

## 目录

- [插件系统架构](#插件系统架构)
- [标准集成接口](#标准集成接口)
- [自定义步骤类型支持](#自定义步骤类型支持)
- [外部系统集成连接器](#外部系统集成连接器)
- [工作流导入/导出功能](#工作流导入导出功能)
- [示例](#示例)

## 插件系统架构

KastraX 插件系统提供了一种扩展工作流系统功能的标准方式，允许开发者添加新的步骤类型、工具、连接器和存储实现。

### 核心组件

- **Plugin 接口**: 所有插件必须实现的基本接口，定义了插件的生命周期方法和元数据。
- **PluginManager**: 负责插件的注册、加载、启动和停止。
- **PluginContext**: 提供插件与系统交互的上下文环境。
- **PluginConfig**: 管理插件配置的接口。

### 插件生命周期

插件生命周期包括以下阶段：

1. **注册**: 插件被注册到系统中，但尚未初始化。
2. **初始化**: 插件进行初始化操作，如加载配置、准备资源等。
3. **启动**: 插件开始运行，提供其功能。
4. **停止**: 插件停止运行，释放资源。
5. **卸载**: 插件从系统中移除。

### 插件发现和加载

KastraX 支持多种插件发现和加载机制：

- **ServiceLoader**: 使用 Java 的 ServiceLoader 机制自动发现和加载插件。
- **目录扫描**: 从指定目录加载插件 JAR 文件。
- **手动注册**: 通过代码显式注册插件。

## 标准集成接口

KastraX 提供了多种标准集成接口，用于不同类型的扩展：

### 插件类型

- **StepPlugin**: 提供自定义工作流步骤类型。
- **ToolPlugin**: 提供工具功能，如数据处理、外部服务调用等。
- **ConnectorPlugin**: 提供与外部系统的集成。
- **StoragePlugin**: 提供自定义存储实现，如状态存储、事件存储等。

### 插件配置和上下文

每个插件都可以有自己的配置和上下文：

- **PluginConfig**: 管理插件的配置属性。
- **PluginContext**: 提供插件与系统交互的接口，包括获取服务、访问其他插件等。

### 插件版本管理

KastraX 支持插件版本管理，确保插件兼容性：

- 每个插件都有版本信息
- 系统可以检查插件版本兼容性
- 支持多版本插件共存

## 自定义步骤类型支持

KastraX 允许开发者创建自定义工作流步骤类型，扩展工作流系统的功能。

### 步骤注册表

**StepRegistry** 负责管理自定义步骤类型：

- 注册和注销步骤类型
- 创建步骤实例
- 验证步骤配置

### 步骤类型定义

步骤类型通过 **StepType** 类定义，包括：

- 基本信息（ID、名称、描述等）
- 配置模式，描述步骤配置的结构
- 输入/输出模式，描述步骤输入/输出的结构
- 分类和标签，用于组织和查找步骤类型

### 步骤配置验证

KastraX 提供了步骤配置验证机制，确保步骤配置的正确性：

- 验证必需字段
- 验证字段类型
- 验证字段值（范围、格式等）

## 外部系统集成连接器

KastraX 提供了连接器框架，用于与外部系统集成。

### 连接器注册表

**ConnectorRegistry** 负责管理连接器：

- 注册和注销连接器类型
- 创建连接器实例
- 管理连接器生命周期

### 连接器类型

连接器类型通过 **ConnectorType** 类定义，包括：

- 基本信息（ID、名称、描述等）
- 配置模式，描述连接器配置的结构
- 支持的操作列表
- 分类和标签

### 连接器生命周期

连接器生命周期包括以下阶段：

1. **创建**: 连接器实例被创建。
2. **连接**: 连接器连接到外部系统。
3. **使用**: 连接器执行操作。
4. **断开连接**: 连接器断开与外部系统的连接。

## 工作流导入/导出功能

KastraX 提供了工作流导入/导出功能，支持工作流的序列化和反序列化。

### 工作流序列化

**WorkflowIO** 类提供了工作流序列化功能：

- 将工作流导出为 JSON 或 YAML 格式
- 将工作流导出到文件
- 将工作流导出为 JSON 元素

### 工作流反序列化

**WorkflowIO** 类也提供了工作流反序列化功能：

- 从 JSON 或 YAML 字符串导入工作流
- 从文件导入工作流
- 从 JSON 元素导入工作流

### 步骤提供者

工作流导入时，可以使用 **StepProvider** 创建自定义步骤：

- 根据步骤类型创建步骤实例
- 设置步骤属性（ID、名称、描述等）
- 配置步骤（变量、配置等）

## 示例

### 创建自定义步骤插件

```kotlin
class HttpStepPlugin : AbstractStepPlugin(
    id = "ai.kastrax.plugin.step.http",
    name = "HTTP Step",
    description = "Provides steps for making HTTP requests",
    version = "1.0.0",
    author = "KastraX Team"
) {
    override fun getStepTypes(): List<StepType> {
        return listOf(
            StepType(
                id = "http-get",
                name = "HTTP GET",
                description = "Make an HTTP GET request",
                // ... 配置模式、输入/输出模式等
            ),
            StepType(
                id = "http-post",
                name = "HTTP POST",
                description = "Make an HTTP POST request",
                // ... 配置模式、输入/输出模式等
            )
        )
    }
    
    override fun createStep(stepType: String, config: Map<String, Any?>): WorkflowStep? {
        return when (stepType) {
            "http-get" -> {
                // 创建 HTTP GET 步骤
                HttpGetStep(/* ... */)
            }
            "http-post" -> {
                // 创建 HTTP POST 步骤
                HttpPostStep(/* ... */)
            }
            else -> null
        }
    }
}
```

### 创建外部系统连接器

```kotlin
class HttpConnectorPlugin : AbstractConnectorPlugin(
    id = "ai.kastrax.plugin.connector.http",
    name = "HTTP Connector",
    description = "Provides integration with HTTP services",
    version = "1.0.0",
    author = "KastraX Team"
) {
    override fun getConnectorTypes(): List<ConnectorType> {
        return listOf(
            ConnectorType(
                id = "http",
                name = "HTTP",
                description = "Connect to HTTP services",
                // ... 配置模式、操作列表等
            )
        )
    }
    
    override fun createConnector(connectorType: String, config: Map<String, Any?>): Connector? {
        if (connectorType != "http") {
            return null
        }
        
        // 创建 HTTP 连接器
        return HttpConnector(/* ... */)
    }
}
```

### 导入/导出工作流

```kotlin
// 创建工作流 IO
val workflowIO = WorkflowIO()

// 导出工作流
val json = workflowIO.exportToJson(workflow)
workflowIO.exportToFile(workflow, File("workflow.json"))

// 导入工作流
val importedWorkflow = workflowIO.importFromJson(json)
val importedWorkflowFromFile = workflowIO.importFromFile(File("workflow.json"))
```

## 结论

KastraX 的集成和扩展性功能提供了一种灵活、标准化的方式来扩展工作流系统的功能。通过插件系统、自定义步骤、外部系统连接器和工作流导入/导出功能，KastraX 可以适应各种工作流场景和集成需求。
