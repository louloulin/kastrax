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
    echo "正在运行示例列表..."
    ./gradlew :examples:run
    exit 0
fi

EXAMPLE_NAME=$1

# 运行示例
echo "正在运行示例: $EXAMPLE_NAME"
./gradlew :examples:run --args="$EXAMPLE_NAME"

# 检查运行结果
if [ $? -eq 0 ]; then
    echo "示例运行成功!"
    
    # 更新ex.md文件
    sed -i '' "s/- \[.\] $EXAMPLE_NAME.*$/- [x] $EXAMPLE_NAME - 已成功运行/" ex.md 2>/dev/null || true
else
    echo "示例运行失败!"
fi
