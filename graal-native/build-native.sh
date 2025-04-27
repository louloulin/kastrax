#!/bin/bash

# 确保脚本在错误时退出
set -e

echo "开始构建 KastraX GraalVM Native Image..."

# 清理之前的构建
echo "清理之前的构建..."
../gradlew :graal-native:clean

# 运行测试
echo "运行测试..."
../gradlew :graal-native:test

# 构建 Native Image
echo "构建 Native Image..."
../gradlew :graal-native:nativeCompile

# 检查构建结果
if [ -f "build/native/nativeCompile/kastrax" ]; then
    echo "构建成功！"
    echo "可执行文件位于: $(pwd)/build/native/nativeCompile/kastrax"
    
    # 创建分发包
    echo "创建分发包..."
    ../gradlew :graal-native:packageNative
    
    echo "分发包位于: $(pwd)/build/distributions/"
    
    # 显示使用说明
    echo ""
    echo "使用方法:"
    echo "  ./build/native/nativeCompile/kastrax help    # 显示帮助信息"
    echo "  ./build/native/nativeCompile/kastrax cli     # 启动命令行界面"
    echo "  ./build/native/nativeCompile/kastrax config  # 显示配置信息"
else
    echo "构建失败！"
    exit 1
fi
