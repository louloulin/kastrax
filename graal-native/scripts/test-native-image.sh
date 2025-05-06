#!/bin/bash

# 测试 GraalVM 原生镜像构建和运行脚本
# 此脚本用于验证 GraalVM 原生镜像的构建和运行是否正常

set -e  # 遇到错误立即退出

echo "===== 开始测试 GraalVM 原生镜像 ====="

# 检查 GraalVM 是否安装
if ! command -v native-image &> /dev/null; then
    echo "错误: GraalVM native-image 工具未找到"
    echo "请安装 GraalVM 并确保 native-image 工具在 PATH 中"
    exit 1
fi

echo "GraalVM native-image 工具已找到"

# 切换到项目根目录
cd "$(dirname "$0")/../.." || exit 1
echo "当前目录: $(pwd)"

# 清理之前的构建
echo "清理之前的构建..."
./gradlew clean

# 运行测试
echo "运行测试..."
./gradlew :graal-native:test --tests "ai.kastrax.graal.serialization.SerializationTest"

if [ $? -ne 0 ]; then
    echo "错误: 测试失败"
    exit 1
fi

echo "测试通过"

# 构建原生镜像
echo "构建原生镜像..."
./gradlew :graal-native:nativeCompile

if [ $? -ne 0 ]; then
    echo "错误: 原生镜像构建失败"
    exit 1
fi

echo "原生镜像构建成功"

# 检查原生镜像是否存在
NATIVE_IMAGE="./graal-native/build/native/nativeCompile/kastrax-native"
if [ ! -f "$NATIVE_IMAGE" ]; then
    echo "错误: 原生镜像文件未找到: $NATIVE_IMAGE"
    exit 1
fi

echo "原生镜像文件已找到: $NATIVE_IMAGE"

# 运行原生镜像
echo "运行原生镜像..."
"$NATIVE_IMAGE" --help

if [ $? -ne 0 ]; then
    echo "错误: 原生镜像运行失败"
    exit 1
fi

echo "原生镜像运行成功"

# 测试序列化功能
echo "测试原生镜像中的序列化功能..."
"$NATIVE_IMAGE" --test-serialization

if [ $? -ne 0 ]; then
    echo "错误: 原生镜像序列化测试失败"
    exit 1
fi

echo "原生镜像序列化测试通过"

echo "===== GraalVM 原生镜像测试完成 ====="
echo "所有测试通过!"
