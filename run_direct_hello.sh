#!/bin/bash

# 设置正确的路径
cd /Users/louloulin/Documents/linchong/agent/kastra/kastrax

# 编译项目
echo "编译项目..."
./gradlew :examples:build

# 运行HelloKastrax示例
echo "运行HelloKastrax示例..."
java -cp examples/build/classes/kotlin/main ai.kastrax.examples.HelloKastrax
