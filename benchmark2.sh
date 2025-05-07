#!/bin/bash

echo "=== KastraX Native Image 性能测试 ==="
echo

# 测试文件大小
echo "文件大小比较："
echo "简单版本: $(ls -lh ./graal-native/build/native/simple/kastrax-simple | awk '{print $5}')"
echo "优化版本: $(ls -lh ./graal-native/build/native/optimized/kastrax-optimized | awk '{print $5}')"
echo

# 测试启动时间
echo "启动时间比较 (10次平均)："
simple_total=0
optimized_total=0

for i in {1..10}; do
  start=$(date +%s.%N)
  ./graal-native/build/native/simple/kastrax-simple version > /dev/null
  end=$(date +%s.%N)
  simple_time=$(echo "$end - $start" | bc)
  
  start=$(date +%s.%N)
  ./graal-native/build/native/optimized/kastrax-optimized version > /dev/null
  end=$(date +%s.%N)
  optimized_time=$(echo "$end - $start" | bc)
  
  simple_total=$(echo "$simple_total + $simple_time" | bc)
  optimized_total=$(echo "$optimized_total + $optimized_time" | bc)
  
  echo "运行 $i: 简单版本 ${simple_time}s, 优化版本 ${optimized_time}s"
done

simple_avg=$(echo "scale=3; $simple_total / 10" | bc)
optimized_avg=$(echo "scale=3; $optimized_total / 10" | bc)
improvement=$(echo "scale=1; ($simple_avg - $optimized_avg) / $simple_avg * 100" | bc)

echo
echo "平均启动时间:"
echo "简单版本: ${simple_avg}s"
echo "优化版本: ${optimized_avg}s"
echo "性能提升: ${improvement}%"
echo

# 测试CPU使用率
echo "CPU使用率比较 (运行config命令)："
echo "简单版本:"
./graal-native/build/native/simple/kastrax-simple config > /dev/null &
simple_pid=$!
sleep 0.5
ps -p $simple_pid -o %cpu | tail -n 1
kill $simple_pid 2>/dev/null

echo "优化版本:"
./graal-native/build/native/optimized/kastrax-optimized config > /dev/null &
optimized_pid=$!
sleep 0.5
ps -p $optimized_pid -o %cpu | tail -n 1
kill $optimized_pid 2>/dev/null
