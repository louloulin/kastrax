#!/bin/bash

# 简化的 Native 编译脚本
# 这个脚本提供了一种简单的方式来构建 Native Image

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
echo -e "${BLUE}   ProtoActor 简化 Native 编译脚本   ${NC}"
echo -e "${BLUE}=========================================${NC}"
echo ""

# 清理之前的构建
echo -e "${GREEN}清理之前的构建...${NC}"
./gradlew :simple-native:clean

# 创建目录
echo -e "${GREEN}1. 创建目录...${NC}"
mkdir -p simple-native/build/native
mkdir -p simple-native/build/native/config

# 编译项目
echo -e "${GREEN}2. 编译项目...${NC}"
./gradlew :simple-native:build

# 创建初始 classpath
CLASSPATH="simple-native/build/classes/kotlin/main:simple-native/build/resources/main"

# 添加所有项目依赖项
for module in proto-actor proto-mailbox proto-router proto-remote; do
  if [ -d "$module/build/libs" ]; then
    for jar in $(find "$module/build/libs" -name "*.jar"); do
      CLASSPATH="$CLASSPATH:$jar"
    done
  fi
done

# 使用 Agent 运行应用程序以生成配置
echo -e "${GREEN}3. 使用 Agent 运行应用程序以生成配置...${NC}"
$JAVA_HOME/bin/java -agentlib:native-image-agent=config-output-dir=simple-native/build/native/config -cp "simple-native/build/classes/kotlin/main:simple-native/build/resources/main" actor.proto.simple.SimpleHelloKt

# 收集所有依赖项
echo -e "${GREEN}4. 收集所有依赖项...${NC}"
./gradlew :simple-native:dependencies > simple-native/build/native/dependencies.txt

# 添加第三方依赖项
echo -e "${GREEN}添加第三方依赖项...${NC}"
for jar in $(find "$HOME/.gradle/caches" -name "*.jar"); do
  # 只添加必要的依赖项
  if [[ $jar == *kotlinx-coroutines* ]] || [[ $jar == *kotlin-stdlib* ]] || [[ $jar == *slf4j* ]] || [[ $jar == *jctools* ]]; then
    CLASSPATH="$CLASSPATH:$jar"
  fi
done

echo -e "${GREEN}最终 classpath: $CLASSPATH${NC}"

# 编译 Native Image
echo -e "${GREEN}5. 编译 Native Image...${NC}"
$JAVA_HOME/bin/native-image \
  --no-fallback \
  --report-unsupported-elements-at-runtime \
  -H:+ReportExceptionStackTraces \
  -H:ConfigurationFileDirectories=simple-native/build/native/config \
  --initialize-at-build-time=kotlin \
  -cp "simple-native/build/classes/kotlin/main:simple-native/build/resources/main" \
  actor.proto.simple.SimpleHelloKt \
  simple-native/build/native/proto-actor-simple

# 检查构建是否成功
if [ $? -eq 0 ]; then
  echo -e "${GREEN}Native Image 构建成功!${NC}"

  # 运行 Native Image
  echo -e "${GREEN}4. 运行 Native Image...${NC}"
  ./simple-native/build/native/proto-actor-simple
else
  echo -e "${YELLOW}Native Image 构建失败。请检查错误信息。${NC}"
  exit 1
fi

echo -e "${BLUE}完成!${NC}"
