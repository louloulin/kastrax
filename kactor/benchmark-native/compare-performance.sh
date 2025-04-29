#!/bin/bash

# 设置颜色
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查 Native Image 是否已编译
if [ ! -f "build/native/inprocess-benchmark" ]; then
  echo -e "${YELLOW}警告: Native Image 尚未编译。请先运行 benchmark-native.sh 脚本。${NC}"
  exit 1
fi

# 创建结果目录
mkdir -p performance-results

echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}   ProtoActor JVM vs Native 性能比较   ${NC}"
echo -e "${BLUE}=========================================${NC}"
echo ""

# 测量启动时间 - JVM
echo -e "${GREEN}测量 JVM 启动时间...${NC}"
jvm_startup_times=()

for i in $(seq 1 3); do
  echo -n "运行 $i: "
  start_time=$(date +%s.%N)
  ../gradlew :benchmark-native:run -q --args="startup" > /dev/null
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
jvm_avg=$(echo "scale=3; $jvm_total / 3" | bc)

echo ""
echo -e "${GREEN}JVM 平均启动时间: ${YELLOW}$jvm_avg${NC} 秒"
echo ""

# 测量启动时间 - Native Image
echo -e "${GREEN}测量 Native Image 启动时间...${NC}"
native_startup_times=()

for i in $(seq 1 3); do
  echo -n "运行 $i: "
  start_time=$(date +%s.%N)
  ./build/native/inprocess-benchmark startup > /dev/null
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
native_avg=$(echo "scale=3; $native_total / 3" | bc)

echo ""
echo -e "${GREEN}Native Image 平均启动时间: ${YELLOW}$native_avg${NC} 秒"
echo ""

# 计算改进比例
improvement=$(echo "scale=1; ($jvm_avg / $native_avg)" | bc)

echo -e "${BLUE}=========================================${NC}"
echo -e "${GREEN}启动时间结果摘要:${NC}"
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

# 运行完整基准测试 - JVM
echo -e "${BLUE}=========================================${NC}"
echo -e "${GREEN}运行 JVM 基准测试...${NC}"
../gradlew :benchmark-native:run -q > performance-results/jvm-benchmark.txt

# 运行完整基准测试 - Native
echo -e "${GREEN}运行 Native Image 基准测试...${NC}"
./build/native/inprocess-benchmark > performance-results/native-benchmark.txt

# 分析结果
echo -e "${BLUE}=========================================${NC}"
echo -e "${GREEN}分析基准测试结果...${NC}"

# 提取 JVM 和 Native 的消息吸吐量
JVM_THROUGHPUT=$(grep -o '[0-9]\+$' performance-results/jvm-benchmark.txt | sort -nr | head -n 1)
NATIVE_THROUGHPUT=$(grep -o '[0-9]\+$' performance-results/native-benchmark.txt | sort -nr | head -n 1)

# 计算改进比例
if [ -n "$JVM_THROUGHPUT" ] && [ -n "$NATIVE_THROUGHPUT" ]; then
  THROUGHPUT_RATIO=$(echo "scale=2; $NATIVE_THROUGHPUT / $JVM_THROUGHPUT" | bc)

  echo -e "${BLUE}=========================================${NC}"
  echo -e "${GREEN}消息吸吐量结果比较:${NC}"
  echo -e "JVM 最大消息吸吐量: ${YELLOW}$JVM_THROUGHPUT${NC} 消息/秒"
  echo -e "Native Image 最大消息吸吐量: ${YELLOW}$NATIVE_THROUGHPUT${NC} 消息/秒"
  echo -e "Native/JVM 比例: ${YELLOW}${THROUGHPUT_RATIO}x${NC}"

  # 保存结果
  echo "JVM 最大消息吸吐量: $JVM_THROUGHPUT 消息/秒" > performance-results/throughput-comparison.txt
  echo "Native Image 最大消息吸吐量: $NATIVE_THROUGHPUT 消息/秒" >> performance-results/throughput-comparison.txt
  echo "Native/JVM 比例: ${THROUGHPUT_RATIO}x" >> performance-results/throughput-comparison.txt
fi

echo -e "${BLUE}性能比较完成!${NC}"
echo -e "${GREEN}基准测试结果已保存到 performance-results 目录${NC}"
