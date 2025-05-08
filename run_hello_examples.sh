#!/bin/bash

# 设置颜色
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

# 运行hello模块的示例
echo -e "${YELLOW}运行hello模块的示例...${NC}"
./gradlew :examples-modules:hello:build
if [ $? -eq 0 ]; then
    echo -e "${GREEN}hello模块编译成功!${NC}"
    ./gradlew :examples-modules:hello:run
else
    echo -e "${RED}hello模块编译失败!${NC}"
fi

# 运行workflow-hello模块的示例
echo -e "\n${YELLOW}运行workflow-hello模块的示例...${NC}"
./gradlew :examples-modules:workflow-hello:build
if [ $? -eq 0 ]; then
    echo -e "${GREEN}workflow-hello模块编译成功!${NC}"
    ./gradlew :examples-modules:workflow-hello:runHelloWorkflow
else
    echo -e "${RED}workflow-hello模块编译失败!${NC}"
fi

# 运行rag-hello模块的示例
echo -e "\n${YELLOW}运行rag-hello模块的示例...${NC}"
./gradlew :examples-modules:rag-hello:build
if [ $? -eq 0 ]; then
    echo -e "${GREEN}rag-hello模块编译成功!${NC}"
    ./gradlew :examples-modules:rag-hello:runHelloRag
else
    echo -e "${RED}rag-hello模块编译失败!${NC}"
fi

echo -e "\n${GREEN}所有Hello示例运行完成!${NC}"
