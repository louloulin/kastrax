# 代码流分析模块

本模块实现了代码库的控制流和数据流分析功能，用于深入理解代码的执行路径和数据依赖关系。

## 功能特点

- **控制流分析**：分析代码的执行路径，包括条件分支、循环和异常处理
- **数据流分析**：分析变量的定义和使用，跟踪数据依赖关系
- **语言特定分析**：针对不同编程语言（如 Java/Kotlin）提供特定的分析逻辑
- **流图可视化**：将分析结果渲染为可视化图形，支持 DOT、SVG 等格式
- **流图探索**：提供查询和分析流图的工具，如查找路径、循环和死代码

## 架构设计

代码流分析模块由以下组件组成：

1. **核心接口 (CodeFlowAnalyzer)**：
   - 定义代码流分析的通用接口
   - 支持不同类型的代码元素分析

2. **控制流分析器 (ControlFlowAnalyzerImpl)**：
   - 分析代码的执行路径
   - 生成控制流图

3. **数据流分析器 (DataFlowAnalyzerImpl)**：
   - 分析变量的定义和使用
   - 跟踪数据依赖关系
   - 生成数据流图

4. **综合流分析器 (CodeFlowAnalyzerImpl)**：
   - 集成控制流和数据流分析
   - 提供统一的分析接口

5. **语言特定分析器 (JavaKotlinFlowAnalyzer)**：
   - 针对特定编程语言提供定制化分析
   - 处理语言特有的结构和特性

6. **流图可视化 (FlowGraphRenderer)**：
   - 将流图渲染为不同格式
   - 支持 DOT、JSON 等格式

7. **流图探索 (FlowGraphExplorer)**：
   - 提供查询和分析流图的工具
   - 支持查找路径、循环和死代码

## 使用示例

```kotlin
// 创建流分析器
val controlFlowAnalyzer = ControlFlowAnalyzerImpl()
val dataFlowAnalyzer = DataFlowAnalyzerImpl()
val codeFlowAnalyzer = CodeFlowAnalyzerImpl(
    controlFlowAnalyzer = controlFlowAnalyzer,
    dataFlowAnalyzer = dataFlowAnalyzer
)

// 分析代码元素
val flowGraph = codeFlowAnalyzer.analyzeFlow(codeElement)

// 渲染流图
val renderer = FlowGraphRenderer()
val dotOutput = renderer.render(flowGraph)
File("flow-graph.dot").writeText(dotOutput)

// 探索流图
val explorer = FlowGraphExplorer()
val cycles = explorer.findCycles(flowGraph)
val deadCode = explorer.findDeadCode(flowGraph)
```

## 支持的语言

当前版本支持以下编程语言的流分析：

- Java
- Kotlin
- 其他语言的基本支持（通用分析）

## 未来计划

1. **扩展语言支持**：添加对 Python、JavaScript 等更多语言的特定支持
2. **增强分析能力**：实现更复杂的控制流和数据流分析
3. **优化性能**：提高大型代码库的分析效率
4. **集成 IDE**：与 VS Code、IntelliJ IDEA 等 IDE 集成

## 贡献

欢迎贡献代码、报告问题或提出改进建议。请参阅 [CONTRIBUTING.md](../../../../../../../CONTRIBUTING.md) 了解更多信息。
