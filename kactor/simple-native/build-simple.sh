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
echo -e "${BLUE}   ProtoActor 极简 Native 编译脚本   ${NC}"
echo -e "${BLUE}=========================================${NC}"
echo ""

# 清理之前的构建
echo -e "${GREEN}清理之前的构建...${NC}"
rm -rf build

# 创建目录
echo -e "${GREEN}1. 创建目录...${NC}"
mkdir -p build/classes
mkdir -p build/native

# 编译 Kotlin 文件
echo -e "${GREEN}2. 编译 Kotlin 文件...${NC}"

# 使用 Gradle 编译
cd ..
./gradlew :simple-native:compileKotlin
cd simple-native

# 复制编译后的类文件
cp -r ../simple-native/build/classes/kotlin/main/* build/classes/

# 检查编译是否成功
if [ $? -ne 0 ]; then
  echo -e "${YELLOW}Kotlin 编译失败。请检查错误信息。${NC}"
  exit 1
fi

# 查找 Kotlin 标准库
echo -e "${GREEN}3. 查找 Kotlin 标准库...${NC}"
KOTLIN_STDLIB=$(find "$HOME/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin" -name "kotlin-stdlib-*.jar" | grep -v "sources" | grep -v "javadoc" | head -n 1)
KOTLIN_STDLIB_COMMON=$(find "$HOME/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin" -name "kotlin-stdlib-common-*.jar" | grep -v "sources" | grep -v "javadoc" | head -n 1)

echo "Kotlin 标准库: $KOTLIN_STDLIB"
echo "Kotlin 公共库: $KOTLIN_STDLIB_COMMON"

# 编译 Native Image
echo -e "${GREEN}4. 编译 Native Image...${NC}"
$JAVA_HOME/bin/native-image \
  --no-fallback \
  --initialize-at-build-time=kotlin \
  -cp "build/classes:$KOTLIN_STDLIB:$KOTLIN_STDLIB_COMMON" \
  actor.proto.simple.VerySimpleKt \
  build/native/very-simple

# 检查构建是否成功
if [ $? -eq 0 ]; then
  echo -e "${GREEN}Native Image 构建成功!${NC}"

  # 运行 Native Image
  echo -e "${GREEN}5. 运行 Native Image...${NC}"
  chmod +x build/native/very-simple
  build/native/very-simple
else
  echo -e "${YELLOW}Native Image 构建失败。请检查错误信息。${NC}"
  exit 1
fi

echo -e "${BLUE}完成!${NC}"
