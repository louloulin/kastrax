#!/bin/bash

# 检查 GraalVM 是否安装
if [ -z "$JAVA_HOME" ] || [[ "$JAVA_HOME" != *"graalvm"* ]]; then
  echo "警告: JAVA_HOME 环境变量未设置或不是指向 GraalVM。"
  echo "请确保已安装 GraalVM 并设置 JAVA_HOME 环境变量。"

  # 尝试查找 GraalVM 安装
  if [ -d "$HOME/Library/Java/JavaVirtualMachines/graalvm-ce-17.0.9" ]; then
    export JAVA_HOME="$HOME/Library/Java/JavaVirtualMachines/graalvm-ce-17.0.9/Contents/Home"
    echo "已自动设置 JAVA_HOME 为: $JAVA_HOME"
  else
    echo "无法自动找到 GraalVM 安装。请手动设置 JAVA_HOME 环境变量。"
    exit 1
  fi
fi

# 检查 native-image 是否安装
if [ ! -f "$JAVA_HOME/bin/native-image" ]; then
  echo "安装 native-image 工具..."
  $JAVA_HOME/bin/gu install native-image
fi

# 显示帮助信息
echo "=== ProtoActor Native 编译脚本 ==="
echo "此脚本将执行以下步骤:"
echo "1. 使用 GraalVM Agent 运行应用程序以生成配置"
echo "2. 复制生成的配置文件"
echo "3. 编译 Native Image"
echo "4. 运行生成的 Native Image"
echo ""

# 询问用户要编译哪个模块
echo "请选择要编译的模块:"
echo "1) proto-benchmarks (基准测试)"
echo "2) native-example (简单示例)"
echo "3) native-example (复杂示例)"
read -p "请输入选项 (1/2/3): " module_choice

case $module_choice in
  1)
    MODULE="proto-benchmarks"
    EXECUTABLE="protoactor-benchmark"
    TASK="run"
    IMAGE_NAME=""
    ;;
  2)
    MODULE="native-example"
    EXECUTABLE="proto-actor-native"
    TASK="run"
    IMAGE_NAME=""
    ;;
  3)
    MODULE="native-example"
    EXECUTABLE="proto-actor-complex"
    TASK="runComplex"
    IMAGE_NAME="-PimageName=complex"
    ;;
  *)
    echo "无效选项，默认使用 proto-benchmarks"
    MODULE="proto-benchmarks"
    EXECUTABLE="protoactor-benchmark"
    TASK="run"
    IMAGE_NAME=""
    ;;
esac

echo "选择了模块: $MODULE"
echo "可执行文件: $EXECUTABLE"
echo ""

# 步骤 1: 使用 Agent 运行应用程序
echo "步骤 1: 使用 GraalVM Agent 运行应用程序以生成配置..."
./gradlew -Pagent=standard :$MODULE:$TASK

# 步骤 2: 复制生成的配置文件
echo "步骤 2: 复制生成的配置文件..."
./gradlew :$MODULE:metadataCopy

# 步骤 3: 编译 Native Image
echo "步骤 3: 编译 Native Image..."
./gradlew :$MODULE:nativeCompile $IMAGE_NAME

# 检查编译是否成功
if [ $? -eq 0 ]; then
  echo "Native Image 编译成功!"

  # 步骤 4: 运行生成的 Native Image
  echo "步骤 4: 运行生成的 Native Image..."
  ./$MODULE/build/native/nativeCompile/$EXECUTABLE
else
  echo "Native Image 编译失败。请检查错误信息。"
  exit 1
fi

echo "完成!"
