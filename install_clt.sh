#!/bin/bash

# 创建一个临时文件，触发Xcode命令行工具安装
touch /tmp/.com.apple.dt.CommandLineTools.installondemand.in-progress

# 获取可用的命令行工具包
PROD=$(softwareupdate -l | grep "\*.*Command Line" | sort | tail -n 1 | sed 's/^[^C]* //')

if [ -z "$PROD" ]; then
  echo "没有找到可用的命令行工具包"
  exit 1
fi

echo "找到命令行工具包: $PROD"
echo "正在安装..."

# 安装命令行工具
softwareupdate -i "$PROD" --verbose

# 删除临时文件
rm /tmp/.com.apple.dt.CommandLineTools.installondemand.in-progress

echo "安装完成，请检查是否成功"
