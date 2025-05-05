#!/bin/bash

# 替换所有 Markdown 文件中的 "mastra" 为 "kastrax"
find kastrax-doc/src/content -name "*.md" -o -name "*.mdx" | xargs sed -i '' 's/mastra/kastrax/g; s/Mastra/Kastrax/g'

echo "替换完成！"
