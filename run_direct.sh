#!/bin/bash

# 创建必要的目录
mkdir -p docs reports temp_docs examples_data

# 复制示例数据
cp -r examples/examples_data/*.txt docs/ 2>/dev/null || true

# 设置环境变量
export DEEPSEEK_API_KEY="sk-85e83081df28490b9ae63188f0cb4f79"
export OPENAI_API_KEY="your-openai-api-key-here"

# 检查是否提供了示例名称
if [ $# -eq 0 ]; then
    echo "用法: $0 <示例名称>"
    echo "示例: $0 workflow.DynamicWorkflowExample"
    exit 1
fi

EXAMPLE_NAME=$1

# 编译项目
echo "编译项目..."
./gradlew :examples:build

# 运行示例
echo "正在运行示例: $EXAMPLE_NAME"

# 获取完整的类路径
CLASSPATH=$(find examples/build/libs -name "*.jar" | tr '\n' ':')
CLASSPATH="$CLASSPATH$(find kastrax-*/build/libs -name "*.jar" | tr '\n' ':')"
CLASSPATH="$CLASSPATH$(find kactor/*/build/libs -name "*.jar" | tr '\n' ':')"
CLASSPATH="$CLASSPATH$(find fastembed-kotlin/build/libs -name "*.jar" | tr '\n' ':')"
CLASSPATH="$CLASSPATH$(find kastrax-integrations/*/build/libs -name "*.jar" | tr '\n' ':')"

echo "使用类路径: $CLASSPATH"

# 直接运行示例
case "$EXAMPLE_NAME" in
    "workflow.DynamicWorkflowExample")
        echo "运行动态工作流示例..."
        java -cp "$CLASSPATH" ai.kastrax.examples.workflow.DynamicWorkflowExampleKt
        ;;
    "WorkflowExample")
        echo "运行基础工作流示例..."
        java -cp "$CLASSPATH" ai.kastrax.examples.WorkflowExampleKt
        ;;
    "RAGExample")
        echo "运行RAG示例..."
        java -cp "$CLASSPATH" ai.kastrax.examples.RAGExampleKt
        ;;
    "RAGWorkflowExample")
        echo "运行RAG工作流示例..."
        java -cp "$CLASSPATH" ai.kastrax.examples.RAGWorkflowExampleKt
        ;;
    "FastEmbedRAGExample")
        echo "运行快速嵌入RAG示例..."
        java -cp "$CLASSPATH" ai.kastrax.examples.FastEmbedRAGExampleKt
        ;;
    "memory.WorkingMemoryExample")
        echo "运行工作内存示例..."
        java -cp "$CLASSPATH" ai.kastrax.examples.memory.WorkingMemoryExampleKt
        ;;
    *)
        echo "未知示例: $EXAMPLE_NAME"
        echo "请提供有效的示例名称"
        exit 1
        ;;
esac

# 检查运行结果
if [ $? -eq 0 ]; then
    echo "示例运行成功!"

    # 更新ex.md文件
    sed -i '' "s/- \[.\] $EXAMPLE_NAME.*$/- [x] $EXAMPLE_NAME - 已成功运行/" ex.md 2>/dev/null || true
else
    echo "示例运行失败!"
fi
