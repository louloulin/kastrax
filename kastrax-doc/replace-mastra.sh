#!/bin/bash

# 替换所有文件中的 "mastra" 为 "kastrax"（不区分大小写）
find . -type f -name "*.tsx" -o -name "*.ts" -o -name "*.js" -o -name "*.json" -o -name "*.md" -o -name "*.mdx" | xargs sed -i '' -E 's/mastra/kastrax/gi'

# 替换所有文件中的 "Mastra" 为 "Kastrax"（保持大小写）
find . -type f -name "*.tsx" -o -name "*.ts" -o -name "*.js" -o -name "*.json" -o -name "*.md" -o -name "*.mdx" | xargs sed -i '' -E 's/Mastra/Kastrax/g'

echo "替换完成！"
