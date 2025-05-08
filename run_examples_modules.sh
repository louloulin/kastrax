#!/bin/bash

# 设置正确的路径
cd "$(dirname "$0")"

# 创建必要的目录
mkdir -p docs reports temp_docs examples_data

# 复制示例数据
cp -r examples/examples_data/*.txt docs/ 2>/dev/null || true

# 设置环境变量
export DEEPSEEK_API_KEY="sk-85e83081df28490b9ae63188f0cb4f79"
export OPENAI_API_KEY="your-openai-api-key-here"
export ANTHROPIC_API_KEY="your-anthropic-api-key-here"
export GEMINI_API_KEY="your-gemini-api-key-here"

# 编译项目
echo "编译项目..."
cd examples-modules
./gradlew build

# 运行示例
if [ $# -eq 0 ]; then
    echo "用法: $0 <类别> [示例名称]"
    echo "可用的类别:"
    echo "1. workflow - 运行工作流相关示例"
    echo "2. rag - 运行RAG相关示例"
    echo "3. memory - 运行内存相关示例"
    echo "4. tools - 运行工具相关示例"
    echo "5. agent - 运行Agent相关示例"
    echo "6. other - 运行其他示例"
    echo "7. all - 运行所有示例"
    echo "8. help - 显示此帮助信息"
    exit 1
fi

CATEGORY=$1
shift
EXAMPLE_ARGS="$@"

echo "运行类别: $CATEGORY"
if [ -n "$EXAMPLE_ARGS" ]; then
    echo "示例参数: $EXAMPLE_ARGS"
fi

# 运行示例
./gradlew run --args="$CATEGORY $EXAMPLE_ARGS"

# 检查运行结果
if [ $? -eq 0 ]; then
    echo "示例运行成功!"
else
    echo "示例运行失败!"
fi
