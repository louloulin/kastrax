#!/bin/bash

# 设置颜色
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查 GraalVM 是否安装
if [ -z "$JAVA_HOME" ] || [[ "$JAVA_HOME" != *"graalvm"* ]]; then
  echo -e "${YELLOW}警告: JAVA_HOME 环境变量未设置或不是指向 GraalVM。${NC}"
  echo -e "${YELLOW}请确保已安装 GraalVM 并设置 JAVA_HOME 环境变量。${NC}"
  
  # 尝试查找 GraalVM 安装
  if [ -d "$HOME/Library/Java/JavaVirtualMachines/graalvm-ce-17.0.9" ]; then
    export JAVA_HOME="$HOME/Library/Java/JavaVirtualMachines/graalvm-ce-17.0.9/Contents/Home"
    echo -e "${GREEN}已自动设置 JAVA_HOME 为: $JAVA_HOME${NC}"
  else
    echo -e "${YELLOW}无法自动找到 GraalVM 安装。请手动设置 JAVA_HOME 环境变量。${NC}"
    exit 1
  fi
fi

# 检查 native-image 是否安装
if [ ! -f "$JAVA_HOME/bin/native-image" ]; then
  echo -e "${YELLOW}安装 native-image 工具...${NC}"
  $JAVA_HOME/bin/gu install native-image
fi

echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}   ProtoActor Native 编译脚本   ${NC}"
echo -e "${BLUE}=========================================${NC}"
echo ""

# 使用 Gradle 构建和运行 Native Image
echo -e "${GREEN}使用 Gradle 构建和运行 ProtoActor Native Image...${NC}"
cd ..
./gradlew :simple-native:buildAndRunNative

echo -e "${BLUE}完成!${NC}"
