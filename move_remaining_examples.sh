#!/bin/bash

# 设置正确的路径
cd "$(dirname "$0")"

# 创建必要的目录
mkdir -p examples-modules/{workflow,rag,memory,tools,agent,other}/src/main/kotlin/ai/kastrax/examples/{workflow,rag,memory,tools,agent,other}

# 移动工作流相关示例
echo "移动工作流相关示例..."
# 检查文件是否存在，如果存在则移动
[ -f examples/src/main/kotlin/ai/kastrax/examples/workflow/AgentChainExample.kt ] && mv -v examples/src/main/kotlin/ai/kastrax/examples/workflow/AgentChainExample.kt examples-modules/workflow/src/main/kotlin/ai/kastrax/examples/workflow/
[ -f examples/src/main/kotlin/ai/kastrax/examples/workflow/DataFlowExample.kt ] && mv -v examples/src/main/kotlin/ai/kastrax/examples/workflow/DataFlowExample.kt examples-modules/workflow/src/main/kotlin/ai/kastrax/examples/workflow/
[ -f examples/src/main/kotlin/ai/kastrax/examples/workflow/ErrorHandlingWorkflowExample.kt ] && mv -v examples/src/main/kotlin/ai/kastrax/examples/workflow/ErrorHandlingWorkflowExample.kt examples-modules/workflow/src/main/kotlin/ai/kastrax/examples/workflow/
[ -f examples/src/main/kotlin/ai/kastrax/examples/workflow/EventCallbackWorkflowExample.kt ] && mv -v examples/src/main/kotlin/ai/kastrax/examples/workflow/EventCallbackWorkflowExample.kt examples-modules/workflow/src/main/kotlin/ai/kastrax/examples/workflow/
[ -f examples/src/main/kotlin/ai/kastrax/examples/workflow/WorkflowMonitoringExample.kt ] && mv -v examples/src/main/kotlin/ai/kastrax/examples/workflow/WorkflowMonitoringExample.kt examples-modules/workflow/src/main/kotlin/ai/kastrax/examples/workflow/
[ -f examples/src/main/kotlin/ai/kastrax/examples/workflow/WorkflowVersioningExample.kt ] && mv -v examples/src/main/kotlin/ai/kastrax/examples/workflow/WorkflowVersioningExample.kt examples-modules/workflow/src/main/kotlin/ai/kastrax/examples/workflow/
[ -f examples/src/main/kotlin/ai/kastrax/examples/workflow/WorkflowVisualizationExample.kt ] && mv -v examples/src/main/kotlin/ai/kastrax/examples/workflow/WorkflowVisualizationExample.kt examples-modules/workflow/src/main/kotlin/ai/kastrax/examples/workflow/
[ -f examples/src/main/kotlin/ai/kastrax/examples/workflow/WorkflowRetryExample.kt ] && mv -v examples/src/main/kotlin/ai/kastrax/examples/workflow/WorkflowRetryExample.kt examples-modules/workflow/src/main/kotlin/ai/kastrax/examples/workflow/

# 移动Agent相关示例
echo "移动Agent相关示例..."
[ -f examples/src/main/kotlin/ai/kastrax/examples/agent/CollaborativeAgentNetworkExample.kt ] && mv -v examples/src/main/kotlin/ai/kastrax/examples/agent/CollaborativeAgentNetworkExample.kt examples-modules/agent/src/main/kotlin/ai/kastrax/examples/agent/

# 移动工具相关示例
echo "移动工具相关示例..."
[ -f examples/src/main/kotlin/ai/kastrax/examples/FileOperationToolExample.kt ] && mv -v examples/src/main/kotlin/ai/kastrax/examples/FileOperationToolExample.kt examples-modules/tools/src/main/kotlin/ai/kastrax/examples/tools/

# 查找并移动其他可能的例子
echo "查找并移动其他可能的例子..."

# 查找工作流相关示例
find examples/src/main/kotlin -name "*Workflow*.kt" | while read file; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        echo "发现工作流相关示例: $filename"
        mv -v "$file" examples-modules/workflow/src/main/kotlin/ai/kastrax/examples/workflow/
    fi
done

# 查找RAG相关示例
find examples/src/main/kotlin -name "*RAG*.kt" -o -name "*Rag*.kt" -o -name "*Retrieval*.kt" | while read file; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        echo "发现RAG相关示例: $filename"
        mv -v "$file" examples-modules/rag/src/main/kotlin/ai/kastrax/examples/rag/
    fi
done

# 查找内存相关示例
find examples/src/main/kotlin -name "*Memory*.kt" -o -name "*Semantic*.kt" | while read file; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        echo "发现内存相关示例: $filename"
        mv -v "$file" examples-modules/memory/src/main/kotlin/ai/kastrax/examples/memory/
    fi
done

# 查找工具相关示例
find examples/src/main/kotlin -name "*Tool*.kt" -o -name "*Zod*.kt" -o -name "*Calculator*.kt" | while read file; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        echo "发现工具相关示例: $filename"
        mv -v "$file" examples-modules/tools/src/main/kotlin/ai/kastrax/examples/tools/
    fi
done

# 查找Agent相关示例
find examples/src/main/kotlin -name "*Agent*.kt" | while read file; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        echo "发现Agent相关示例: $filename"
        mv -v "$file" examples-modules/agent/src/main/kotlin/ai/kastrax/examples/agent/
    fi
done

# 查找其他示例
find examples/src/main/kotlin -name "*Streaming*.kt" -o -name "*DataSource*.kt" -o -name "*Anthropic*.kt" -o -name "*Gemini*.kt" | while read file; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        echo "发现其他示例: $filename"
        mv -v "$file" examples-modules/other/src/main/kotlin/ai/kastrax/examples/other/
    fi
done

echo "所有示例移动完成!"
