#!/bin/bash

# 设置正确的路径
cd "$(dirname "$0")"

# 创建必要的目录
mkdir -p examples-modules/{workflow,rag,memory,tools,agent,other}/src/main/kotlin/ai/kastrax/examples/{workflow,rag,memory,tools,agent,other}

# 移动工作流相关示例
echo "移动工作流相关示例..."
cp -v examples/src/main/kotlin/ai/kastrax/examples/WorkflowExample.kt examples-modules/workflow/src/main/kotlin/ai/kastrax/examples/workflow/
cp -v examples/src/main/kotlin/ai/kastrax/examples/workflow/WorkflowRetryExample.kt examples-modules/workflow/src/main/kotlin/ai/kastrax/examples/workflow/
cp -v examples/src/main/kotlin/ai/kastrax/examples/workflow/AdvancedWorkflowExample.kt examples-modules/workflow/src/main/kotlin/ai/kastrax/examples/workflow/
cp -v examples/src/main/kotlin/ai/kastrax/examples/workflow/DynamicWorkflowExample.kt examples-modules/workflow/src/main/kotlin/ai/kastrax/examples/workflow/

# 移动RAG相关示例
echo "移动RAG相关示例..."
cp -v examples/src/main/kotlin/ai/kastrax/examples/RAGExample.kt examples-modules/rag/src/main/kotlin/ai/kastrax/examples/rag/
cp -v examples/src/main/kotlin/ai/kastrax/examples/RAGWorkflowExample.kt examples-modules/rag/src/main/kotlin/ai/kastrax/examples/rag/
cp -v examples/src/main/kotlin/ai/kastrax/examples/FastEmbedRAGExample.kt examples-modules/rag/src/main/kotlin/ai/kastrax/examples/rag/

# 移动内存相关示例
echo "移动内存相关示例..."
cp -v examples/src/main/kotlin/ai/kastrax/examples/memory/WorkingMemoryExample.kt examples-modules/memory/src/main/kotlin/ai/kastrax/examples/memory/
cp -v examples/src/main/kotlin/ai/kastrax/examples/memory/MemoryCompressionExample.kt examples-modules/memory/src/main/kotlin/ai/kastrax/examples/memory/ 2>/dev/null || echo "MemoryCompressionExample.kt 不存在"
cp -v examples/src/main/kotlin/ai/kastrax/examples/memory/MemoryManagerExample.kt examples-modules/memory/src/main/kotlin/ai/kastrax/examples/memory/ 2>/dev/null || echo "MemoryManagerExample.kt 不存在"
cp -v examples/src/main/kotlin/ai/kastrax/examples/memory/TagsAndSharingExample.kt examples-modules/memory/src/main/kotlin/ai/kastrax/examples/memory/ 2>/dev/null || echo "TagsAndSharingExample.kt 不存在"

# 移动工具相关示例
echo "移动工具相关示例..."
cp -v examples/src/main/kotlin/ai/kastrax/examples/AdvancedZodToolExample.kt examples-modules/tools/src/main/kotlin/ai/kastrax/examples/tools/ 2>/dev/null || echo "AdvancedZodToolExample.kt 不存在"
cp -v examples/src/main/kotlin/ai/kastrax/examples/DataClassZodToolExample.kt examples-modules/tools/src/main/kotlin/ai/kastrax/examples/tools/ 2>/dev/null || echo "DataClassZodToolExample.kt 不存在"
cp -v examples/src/main/kotlin/ai/kastrax/examples/DateTimeToolExample.kt examples-modules/tools/src/main/kotlin/ai/kastrax/examples/tools/ 2>/dev/null || echo "DateTimeToolExample.kt 不存在"
cp -v examples/src/main/kotlin/ai/kastrax/examples/ZodAdvancedToolExample.kt examples-modules/tools/src/main/kotlin/ai/kastrax/examples/tools/ 2>/dev/null || echo "ZodAdvancedToolExample.kt 不存在"
cp -v examples/src/main/kotlin/ai/kastrax/examples/ZodCalculatorExample.kt examples-modules/tools/src/main/kotlin/ai/kastrax/examples/tools/ 2>/dev/null || echo "ZodCalculatorExample.kt 不存在"
cp -v examples/src/main/kotlin/ai/kastrax/examples/ZodCalculatorToolExample.kt examples-modules/tools/src/main/kotlin/ai/kastrax/examples/tools/ 2>/dev/null || echo "ZodCalculatorToolExample.kt 不存在"

# 移动Agent相关示例
echo "移动Agent相关示例..."
cp -v examples/src/main/kotlin/ai/kastrax/examples/ZodAgentExample.kt examples-modules/agent/src/main/kotlin/ai/kastrax/examples/agent/ 2>/dev/null || echo "ZodAgentExample.kt 不存在"
cp -v examples/src/main/kotlin/ai/kastrax/examples/agent/AdaptiveAgentExample.kt examples-modules/agent/src/main/kotlin/ai/kastrax/examples/agent/ 2>/dev/null || echo "AdaptiveAgentExample.kt 不存在"
cp -v examples/src/main/kotlin/ai/kastrax/examples/agent/AdvancedAgentExample.kt examples-modules/agent/src/main/kotlin/ai/kastrax/examples/agent/ 2>/dev/null || echo "AdvancedAgentExample.kt 不存在"
cp -v examples/src/main/kotlin/ai/kastrax/examples/agent/AgentStateExample.kt examples-modules/agent/src/main/kotlin/ai/kastrax/examples/agent/ 2>/dev/null || echo "AgentStateExample.kt 不存在"
cp -v examples/src/main/kotlin/ai/kastrax/examples/agent/AgentVersioningExample.kt examples-modules/agent/src/main/kotlin/ai/kastrax/examples/agent/ 2>/dev/null || echo "AgentVersioningExample.kt 不存在"
cp -v examples/src/main/kotlin/ai/kastrax/examples/agent/GoalOrientedAgentExample.kt examples-modules/agent/src/main/kotlin/ai/kastrax/examples/agent/ 2>/dev/null || echo "GoalOrientedAgentExample.kt 不存在"
cp -v examples/src/main/kotlin/ai/kastrax/examples/agent/ReflectiveAgentExample.kt examples-modules/agent/src/main/kotlin/ai/kastrax/examples/agent/ 2>/dev/null || echo "ReflectiveAgentExample.kt 不存在"
cp -v examples/src/main/kotlin/ai/kastrax/examples/agent/HierarchicalAgentExample.kt examples-modules/agent/src/main/kotlin/ai/kastrax/examples/agent/ 2>/dev/null || echo "HierarchicalAgentExample.kt 不存在"
cp -v examples/src/main/kotlin/ai/kastrax/examples/agent/AgentNetworkExample.kt examples-modules/agent/src/main/kotlin/ai/kastrax/examples/agent/ 2>/dev/null || echo "AgentNetworkExample.kt 不存在"

# 移动其他示例
echo "移动其他示例..."
cp -v examples/src/main/kotlin/ai/kastrax/examples/DataSourceExample.kt examples-modules/other/src/main/kotlin/ai/kastrax/examples/other/ 2>/dev/null || echo "DataSourceExample.kt 不存在"
cp -v examples/src/main/kotlin/ai/kastrax/examples/AnthropicDirectStreamingExample.kt examples-modules/other/src/main/kotlin/ai/kastrax/examples/other/ 2>/dev/null || echo "AnthropicDirectStreamingExample.kt 不存在"
cp -v examples/src/main/kotlin/ai/kastrax/examples/AnthropicStreamingExample.kt examples-modules/other/src/main/kotlin/ai/kastrax/examples/other/ 2>/dev/null || echo "AnthropicStreamingExample.kt 不存在"
cp -v examples/src/main/kotlin/ai/kastrax/examples/GeminiDirectStreamingExample.kt examples-modules/other/src/main/kotlin/ai/kastrax/examples/other/ 2>/dev/null || echo "GeminiDirectStreamingExample.kt 不存在"
cp -v examples/src/main/kotlin/ai/kastrax/examples/GeminiStreamingExample.kt examples-modules/other/src/main/kotlin/ai/kastrax/examples/other/ 2>/dev/null || echo "GeminiStreamingExample.kt 不存在"

echo "所有示例移动完成!"
