# Kastrax 示例执行计划 (第二阶段)

本文档记录了 Kastrax 示例程序的执行计划和状态。

## 执行计划概述

我们将按照以下模块分类执行示例：

1. 基础示例 (Hello 系列)
2. Agent 示例
3. 工具 (Tools) 示例
4. 工作流 (Workflow) 示例
5. RAG 示例
6. 内存 (Memory) 示例
7. 其他示例

## 基础示例 (Hello 系列)

这些是最基本的示例，用于验证框架的基本功能：

- [x] examples-modules/hello/src/main/kotlin/ai/kastrax/examples/hello/HelloKastrax.kt - 已修改为使用模拟响应，成功运行
- [ ] examples-modules/hello/src/main/kotlin/ai/kastrax/examples/hello/HelloAgent.kt
- [ ] examples-modules/hello/src/main/kotlin/ai/kastrax/examples/hello/HelloWorld.kt
- [ ] examples-modules/hello-fixed/src/main/kotlin/ai/kastrax/examples/hello/HelloKastrax.kt

## Agent 示例

这些示例展示了 Kastrax 的 Agent 功能：

- [ ] examples-modules/agent/src/main/kotlin/ai/kastrax/examples/agent/HelloAgent.kt
- [ ] examples-modules/agent/src/main/kotlin/ai/kastrax/examples/agent/SimpleAgent.kt
- [ ] examples-modules/agent/src/main/kotlin/ai/kastrax/examples/agent/DeepseekAgentExample.kt
- [ ] examples-modules/agent/src/main/kotlin/ai/kastrax/examples/agent/DeepseekArchitectureExample.kt
- [ ] examples-modules/agent/src/main/kotlin/ai/kastrax/examples/agent/GoalOrientedAgentExample.kt
- [ ] examples-modules/agent/src/main/kotlin/ai/kastrax/examples/agent/CreativeAgentExample.kt
- [ ] examples-modules/agent/src/main/kotlin/ai/kastrax/examples/agent/ReflectiveAgentExample.kt
- [ ] examples-modules/agent/src/main/kotlin/ai/kastrax/examples/agent/HierarchicalAgentExample.kt
- [ ] examples-modules/agent/src/main/kotlin/ai/kastrax/examples/agent/AgentNetworkExample.kt
- [ ] examples-modules/agent/src/main/kotlin/ai/kastrax/examples/agent/CollaborativeAgentNetworkExample.kt
- [ ] examples-modules/agent/src/main/kotlin/ai/kastrax/examples/agent/AgentStateExample.kt
- [ ] examples-modules/agent/src/main/kotlin/ai/kastrax/examples/agent/AgentVersioningExample.kt
- [ ] examples-modules/agent/src/main/kotlin/ai/kastrax/examples/agent/AdaptiveAgentExample.kt
- [ ] examples-modules/agent-hello/src/main/kotlin/ai/kastrax/examples/agent/hello/HelloAgent.kt
- [ ] examples-modules/agent-simple/src/main/kotlin/ai/kastrax/examples/agent/HelloAgent.kt

## 工具 (Tools) 示例

这些示例展示了 Kastrax 的工具功能：

- [ ] examples-modules/tools/src/main/kotlin/ai/kastrax/examples/tools/HelloTools.kt
- [ ] examples-modules/tools/src/main/kotlin/ai/kastrax/examples/tools/CalculatorExample.kt
- [ ] examples-modules/tools/src/main/kotlin/ai/kastrax/examples/tools/DateTimeToolExample.kt
- [ ] examples-modules/tools/src/main/kotlin/ai/kastrax/examples/tools/FileOperationToolExample.kt
- [ ] examples-modules/tools/src/main/kotlin/ai/kastrax/examples/tools/SimpleZodToolExample.kt
- [ ] examples-modules/tools/src/main/kotlin/ai/kastrax/examples/tools/ZodCalculatorExample.kt
- [ ] examples-modules/tools/src/main/kotlin/ai/kastrax/examples/tools/ZodCalculatorToolExample.kt
- [ ] examples-modules/tools/src/main/kotlin/ai/kastrax/examples/tools/ZodDataClassExample.kt
- [ ] examples-modules/tools/src/main/kotlin/ai/kastrax/examples/tools/ZodAdvancedToolExample.kt
- [ ] examples-modules/tools/src/main/kotlin/ai/kastrax/examples/tools/ZodAgentExample.kt
- [ ] examples-modules/tools/src/main/kotlin/ai/kastrax/examples/tools/DeepseekToolAgentExample.kt
- [ ] examples-modules/tools-hello/src/main/kotlin/ai/kastrax/examples/tools/hello/HelloTools.kt
- [ ] examples-modules/tools-simple/src/main/kotlin/ai/kastrax/examples/tools/HelloTools.kt

## 工作流 (Workflow) 示例

这些示例展示了 Kastrax 的工作流功能：

- [ ] examples-modules/workflow/src/main/kotlin/ai/kastrax/examples/workflow/HelloWorkflow.kt
- [ ] examples-modules/workflow/src/main/kotlin/ai/kastrax/examples/workflow/SimpleWorkflow.kt
- [ ] examples-modules/workflow/src/main/kotlin/ai/kastrax/examples/workflow/SimpleWorkflowExample.kt
- [ ] examples-modules/workflow/src/main/kotlin/ai/kastrax/examples/workflow/ConditionalWorkflow.kt
- [ ] examples-modules/workflow/src/main/kotlin/ai/kastrax/examples/workflow/ConditionalWorkflowExample.kt
- [ ] examples-modules/workflow/src/main/kotlin/ai/kastrax/examples/workflow/ParallelWorkflow.kt
- [ ] examples-modules/workflow/src/main/kotlin/ai/kastrax/examples/workflow/ParallelWorkflowExample.kt
- [ ] examples-modules/workflow/src/main/kotlin/ai/kastrax/examples/workflow/DynamicWorkflowExample.kt
- [ ] examples-modules/workflow/src/main/kotlin/ai/kastrax/examples/workflow/WorkflowTemplateExample.kt
- [ ] examples-modules/workflow/src/main/kotlin/ai/kastrax/examples/workflow/EnhancedWorkflowExample.kt
- [ ] examples-modules/workflow/src/main/kotlin/ai/kastrax/examples/workflow/AgentChainExample.kt
- [ ] examples-modules/workflow/src/main/kotlin/ai/kastrax/examples/workflow/RAGWorkflowExample.kt
- [ ] examples-modules/workflow/src/main/kotlin/ai/kastrax/examples/workflow/WorkflowMonitoringExample.kt
- [ ] examples-modules/workflow/src/main/kotlin/ai/kastrax/examples/workflow/WorkflowVersioningExample.kt
- [ ] examples-modules/workflow/src/main/kotlin/ai/kastrax/examples/workflow/WorkflowVisualizationExample.kt
- [ ] examples-modules/workflow/src/main/kotlin/ai/kastrax/examples/workflow/PerformanceAnalysisExample.kt
- [ ] examples-modules/workflow-hello/src/main/kotlin/ai/kastrax/examples/workflow/hello/HelloWorkflow.kt
- [ ] examples-modules/workflow-simple/src/main/kotlin/ai/kastrax/examples/workflow/HelloWorkflow.kt
- [ ] examples-modules/workflow-simple/src/main/kotlin/ai/kastrax/examples/workflow/SimpleWorkflow.kt
- [ ] examples-modules/workflow-simple/src/main/kotlin/ai/kastrax/examples/workflow/ConditionalWorkflow.kt
- [ ] examples-modules/workflow-simple/src/main/kotlin/ai/kastrax/examples/workflow/ParallelWorkflow.kt

## RAG 示例

这些示例展示了 Kastrax 的 RAG (检索增强生成) 功能：

- [ ] examples-modules/rag/src/main/kotlin/ai/kastrax/examples/rag/HelloRag.kt
- [ ] examples-modules/rag/src/main/kotlin/ai/kastrax/examples/rag/RAGExample.kt
- [ ] examples-modules/rag/src/main/kotlin/ai/kastrax/examples/rag/EnhancedRagExample.kt
- [ ] examples-modules/rag/src/main/kotlin/ai/kastrax/examples/rag/EnhancedRetrievalExample.kt
- [ ] examples-modules/rag/src/main/kotlin/ai/kastrax/examples/rag/EnhancedDocumentProcessingExample.kt
- [ ] examples-modules/rag/src/main/kotlin/ai/kastrax/examples/rag/FastEmbedRAGExample.kt
- [ ] examples-modules/rag-hello/src/main/kotlin/ai/kastrax/examples/rag/hello/HelloRag.kt

## 内存 (Memory) 示例

这些示例展示了 Kastrax 的内存功能：

- [ ] examples-modules/memory/src/main/kotlin/ai/kastrax/examples/memory/HelloMemory.kt
- [ ] examples-modules/memory/src/main/kotlin/ai/kastrax/examples/memory/MemoryAgentExample.kt
- [ ] examples-modules/memory/src/main/kotlin/ai/kastrax/examples/memory/MemorySystemExample.kt
- [ ] examples-modules/memory/src/main/kotlin/ai/kastrax/examples/memory/DeepseekMemoryExample.kt
- [ ] examples-modules/memory/src/main/kotlin/ai/kastrax/examples/memory/EnhancedMemoryExample.kt
- [ ] examples-modules/memory/src/main/kotlin/ai/kastrax/examples/memory/MemoryCompressionExample.kt
- [ ] examples-modules/memory/src/main/kotlin/ai/kastrax/examples/memory/MemoryManagerExample.kt
- [ ] examples-modules/memory/src/main/kotlin/ai/kastrax/examples/memory/SemanticSearchExample.kt
- [ ] examples-modules/memory/src/main/kotlin/ai/kastrax/examples/memory/TagsAndSharingExample.kt
- [ ] examples-modules/memory/src/main/kotlin/ai/kastrax/examples/memory/WorkingMemoryExample.kt

## 其他示例

这些示例展示了 Kastrax 的其他功能：

- [ ] examples-modules/other/src/main/kotlin/ai/kastrax/examples/other/HelloOther.kt
- [ ] examples-modules/other/src/main/kotlin/ai/kastrax/examples/other/SimpleExample.kt
- [ ] examples-modules/other/src/main/kotlin/ai/kastrax/examples/other/DeepSeekExample.kt
- [ ] examples-modules/other/src/main/kotlin/ai/kastrax/examples/other/DeepSeekStreamingExample.kt
- [ ] examples-modules/other/src/main/kotlin/ai/kastrax/examples/other/DeepSeekDirectStreamingExample.kt
- [ ] examples-modules/plugin/src/main/kotlin/ai/kastrax/examples/plugin/HelloPlugin.kt
- [ ] examples-modules/plugin/src/main/kotlin/ai/kastrax/examples/plugin/HttpConnectorPlugin.kt
- [ ] examples-modules/plugin/src/main/kotlin/ai/kastrax/examples/plugin/HttpStepPlugin.kt

## 执行顺序

我们将按照以下顺序执行示例：

1. 基础示例 (Hello 系列)
2. 简单的 Agent 示例
3. 简单的工具示例
4. 简单的工作流示例
5. 简单的 RAG 示例
6. 简单的内存示例
7. 其他简单示例
8. 高级 Agent 示例
9. 高级工具示例
10. 高级工作流示例
11. 高级 RAG 示例
12. 高级内存示例
13. 其他高级示例

## 执行状态记录

在执行每个示例后，我们将在上面的列表中将对应的复选框标记为已完成，并添加执行结果的简要说明。
