#!/bin/bash

# 性能比较脚本，用于比较 JVM 和 Native Image 的性能

# 设置颜色
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查 Native Image 是否已编译
if [ ! -f "build/native/nativeCompile/proto-actor-native" ]; then
  echo -e "${YELLOW}警告: Native Image 尚未编译。请先运行 build-native.sh 脚本。${NC}"
  exit 1
fi

# 创建结果目录
mkdir -p performance-results

# 运行次数
RUNS=5

echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}   ProtoActor JVM vs Native Image 性能比较   ${NC}"
echo -e "${BLUE}=========================================${NC}"
echo ""

# 测量启动时间 - JVM
echo -e "${GREEN}测量 JVM 启动时间...${NC}"
jvm_startup_times=()

for i in $(seq 1 $RUNS); do
  echo -n "运行 $i: "
  start_time=$(date +%s.%N)
  ../gradlew run -q > /dev/null
  end_time=$(date +%s.%N)
  duration=$(echo "$end_time - $start_time" | bc)
  jvm_startup_times+=($duration)
  echo "$duration 秒"
done

# 计算 JVM 平均启动时间
jvm_total=0
for time in "${jvm_startup_times[@]}"; do
  jvm_total=$(echo "$jvm_total + $time" | bc)
done
jvm_avg=$(echo "scale=3; $jvm_total / $RUNS" | bc)

echo ""
echo -e "${GREEN}JVM 平均启动时间: ${YELLOW}$jvm_avg${NC} 秒"
echo ""

# 测量启动时间 - Native Image
echo -e "${GREEN}测量 Native Image 启动时间...${NC}"
native_startup_times=()

for i in $(seq 1 $RUNS); do
  echo -n "运行 $i: "
  start_time=$(date +%s.%N)
  build/native/nativeCompile/proto-actor-native > /dev/null
  end_time=$(date +%s.%N)
  duration=$(echo "$end_time - $start_time" | bc)
  native_startup_times+=($duration)
  echo "$duration 秒"
done

# 计算 Native Image 平均启动时间
native_total=0
for time in "${native_startup_times[@]}"; do
  native_total=$(echo "$native_total + $time" | bc)
done
native_avg=$(echo "scale=3; $native_total / $RUNS" | bc)

echo ""
echo -e "${GREEN}Native Image 平均启动时间: ${YELLOW}$native_avg${NC} 秒"
echo ""

# 计算改进比例
improvement=$(echo "scale=1; ($jvm_avg / $native_avg)" | bc)

echo -e "${BLUE}=========================================${NC}"
echo -e "${GREEN}结果摘要:${NC}"
echo -e "JVM 平均启动时间: ${YELLOW}$jvm_avg${NC} 秒"
echo -e "Native Image 平均启动时间: ${YELLOW}$native_avg${NC} 秒"
echo -e "改进比例: ${YELLOW}${improvement}x${NC} 更快"
echo -e "${BLUE}=========================================${NC}"

# 保存结果
echo "JVM 平均启动时间: $jvm_avg 秒" > performance-results/startup-times.txt
echo "Native Image 平均启动时间: $native_avg 秒" >> performance-results/startup-times.txt
echo "改进比例: ${improvement}x 更快" >> performance-results/startup-times.txt

echo ""
echo -e "${GREEN}结果已保存到 performance-results/startup-times.txt${NC}"
echo ""

# 测量内存使用
echo -e "${BLUE}=========================================${NC}"
echo -e "${GREEN}测量内存使用...${NC}"

# JVM 内存使用
echo -e "${GREEN}运行 JVM 版本并测量内存使用...${NC}"
../gradlew run -q &
JVM_PID=$!
sleep 2 # 给应用一些时间启动

if [[ "$OSTYPE" == "darwin"* ]]; then
  # macOS
  JVM_MEM=$(ps -o rss= -p $JVM_PID | awk '{print $1/1024}')
  echo -e "JVM 内存使用: ${YELLOW}${JVM_MEM}${NC} MB"
else
  # Linux
  JVM_MEM=$(ps -o rss= -p $JVM_PID | awk '{print $1/1024}')
  echo -e "JVM 内存使用: ${YELLOW}${JVM_MEM}${NC} MB"
fi

kill $JVM_PID

# Native Image 内存使用
echo -e "${GREEN}运行 Native Image 版本并测量内存使用...${NC}"
build/native/nativeCompile/proto-actor-native &
NATIVE_PID=$!
sleep 1 # 给应用一些时间启动

if [[ "$OSTYPE" == "darwin"* ]]; then
  # macOS
  NATIVE_MEM=$(ps -o rss= -p $NATIVE_PID | awk '{print $1/1024}')
  echo -e "Native Image 内存使用: ${YELLOW}${NATIVE_MEM}${NC} MB"
else
  # Linux
  NATIVE_MEM=$(ps -o rss= -p $NATIVE_PID | awk '{print $1/1024}')
  echo -e "Native Image 内存使用: ${YELLOW}${NATIVE_MEM}${NC} MB"
fi

kill $NATIVE_PID

# 计算内存改进比例
mem_improvement=$(echo "scale=1; ($JVM_MEM / $NATIVE_MEM)" | bc)

echo -e "${BLUE}=========================================${NC}"
echo -e "${GREEN}内存使用摘要:${NC}"
echo -e "JVM 内存使用: ${YELLOW}${JVM_MEM}${NC} MB"
echo -e "Native Image 内存使用: ${YELLOW}${NATIVE_MEM}${NC} MB"
echo -e "改进比例: ${YELLOW}${mem_improvement}x${NC} 更少内存"
echo -e "${BLUE}=========================================${NC}"

# 保存内存结果
echo "JVM 内存使用: $JVM_MEM MB" > performance-results/memory-usage.txt
echo "Native Image 内存使用: $NATIVE_MEM MB" >> performance-results/memory-usage.txt
echo "改进比例: ${mem_improvement}x 更少内存" >> performance-results/memory-usage.txt

echo ""
echo -e "${GREEN}内存使用结果已保存到 performance-results/memory-usage.txt${NC}"
echo ""

echo -e "${BLUE}性能比较完成!${NC}"
