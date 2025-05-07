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
  simple_time=$({ time ./graal-native/build/native/simple/kastrax-simple version > /dev/null; } 2>&1 | grep real | awk '{print $2}' | sed 's/0m//g' | sed 's/s//g')
  optimized_time=$({ time ./graal-native/build/native/optimized/kastrax-optimized version > /dev/null; } 2>&1 | grep real | awk '{print $2}' | sed 's/0m//g' | sed 's/s//g')
  
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

# 测试内存使用
echo "内存使用比较："
simple_mem=$(ps -o rss= -p $(pgrep -f "./graal-native/build/native/simple/kastrax-simple deepseek") | awk '{print $1/1024 " MB"}')
optimized_mem=$(ps -o rss= -p $(pgrep -f "./graal-native/build/native/optimized/kastrax-optimized deepseek") | awk '{print $1/1024 " MB"}')

echo "简单版本: $simple_mem"
echo "优化版本: $optimized_mem"
