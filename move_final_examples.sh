#!/bin/bash

# 设置正确的路径
cd "$(dirname "$0")"

# 创建必要的目录
mkdir -p examples-modules/{workflow,rag,memory,tools,agent,other,plugin}/src/main/kotlin/ai/kastrax/examples/{workflow,rag,memory,tools,agent,other,plugin}

# 移动剩余的文件
echo "移动剩余的文件..."

# DeepSeekExample.kt -> other
[ -f examples/src/main/kotlin/ai/kastrax/examples/DeepSeekExample.kt ] && mv -v examples/src/main/kotlin/ai/kastrax/examples/DeepSeekExample.kt examples-modules/other/src/main/kotlin/ai/kastrax/examples/other/

# EnhancedDocumentProcessingExample.kt -> rag
[ -f examples/src/main/kotlin/ai/kastrax/examples/EnhancedDocumentProcessingExample.kt ] && mv -v examples/src/main/kotlin/ai/kastrax/examples/EnhancedDocumentProcessingExample.kt examples-modules/rag/src/main/kotlin/ai/kastrax/examples/rag/

# SimpleExample.kt -> other
[ -f examples/src/main/kotlin/ai/kastrax/examples/SimpleExample.kt ] && mv -v examples/src/main/kotlin/ai/kastrax/examples/SimpleExample.kt examples-modules/other/src/main/kotlin/ai/kastrax/examples/other/

# DeepseekArchitectureExample.kt -> agent
[ -f examples/src/main/kotlin/ai/kastrax/examples/agent/DeepseekArchitectureExample.kt ] && mv -v examples/src/main/kotlin/ai/kastrax/examples/agent/DeepseekArchitectureExample.kt examples-modules/agent/src/main/kotlin/ai/kastrax/examples/agent/

# DeepseekExamples.kt -> agent
[ -f examples/src/main/kotlin/ai/kastrax/examples/agent/DeepseekExamples.kt ] && mv -v examples/src/main/kotlin/ai/kastrax/examples/agent/DeepseekExamples.kt examples-modules/agent/src/main/kotlin/ai/kastrax/examples/agent/

# DeepseekMain.kt -> agent
[ -f examples/src/main/kotlin/ai/kastrax/examples/agent/DeepseekMain.kt ] && mv -v examples/src/main/kotlin/ai/kastrax/examples/agent/DeepseekMain.kt examples-modules/agent/src/main/kotlin/ai/kastrax/examples/agent/

# TagsAndSharingExample.kt -> memory
[ -f examples/src/main/kotlin/ai/kastrax/examples/memory/TagsAndSharingExample.kt ] && mv -v examples/src/main/kotlin/ai/kastrax/examples/memory/TagsAndSharingExample.kt examples-modules/memory/src/main/kotlin/ai/kastrax/examples/memory/

# HttpConnectorPlugin.kt -> plugin
[ -f examples/src/main/kotlin/ai/kastrax/examples/plugin/HttpConnectorPlugin.kt ] && mv -v examples/src/main/kotlin/ai/kastrax/examples/plugin/HttpConnectorPlugin.kt examples-modules/plugin/src/main/kotlin/ai/kastrax/examples/plugin/

# HttpStepPlugin.kt -> plugin
[ -f examples/src/main/kotlin/ai/kastrax/examples/plugin/HttpStepPlugin.kt ] && mv -v examples/src/main/kotlin/ai/kastrax/examples/plugin/HttpStepPlugin.kt examples-modules/plugin/src/main/kotlin/ai/kastrax/examples/plugin/

# PerformanceAnalysisExample.kt -> workflow
[ -f examples/src/main/kotlin/ai/kastrax/examples/workflow/PerformanceAnalysisExample.kt ] && mv -v examples/src/main/kotlin/ai/kastrax/examples/workflow/PerformanceAnalysisExample.kt examples-modules/workflow/src/main/kotlin/ai/kastrax/examples/workflow/

echo "所有剩余文件移动完成!"
