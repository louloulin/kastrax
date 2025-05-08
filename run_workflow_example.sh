#!/bin/bash

# 设置正确的路径
cd /Users/louloulin/Documents/linchong/agent/kastra/kastrax

# 创建必要的目录
mkdir -p docs reports temp_docs examples_data

# 复制示例数据
cp -r examples/examples_data/*.txt docs/ 2>/dev/null || true

# 设置环境变量
export DEEPSEEK_API_KEY="sk-85e83081df28490b9ae63188f0cb4f79"
export OPENAI_API_KEY="your-openai-api-key-here"

# 编译项目
echo "编译项目..."
./gradlew :examples:build

# 获取完整的类路径
CLASSPATH=$(find examples/build/classes/kotlin/main -type d | tr '\n' ':')
CLASSPATH="$CLASSPATH$(find examples/build/classes/java/main -type d | tr '\n' ':')$(find examples/build/libs -name "*.jar" | tr '\n' ':')"
CLASSPATH="$CLASSPATH$(find kastrax-*/build/libs -name "*.jar" | tr '\n' ':')$(find kactor/*/build/libs -name "*.jar" | tr '\n' ':')"
CLASSPATH="$CLASSPATH$(find fastembed-kotlin/build/libs -name "*.jar" | tr '\n' ':')$(find kastrax-integrations/*/build/libs -name "*.jar" | tr '\n' ':')"

echo "使用类路径: $CLASSPATH"

# 运行DynamicWorkflowExample示例
echo "运行DynamicWorkflowExample示例..."
java -cp "$CLASSPATH" ai.kastrax.examples.workflow.DynamicWorkflowExampleKt
