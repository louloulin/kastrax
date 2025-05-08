#!/bin/bash

# 设置颜色
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

# 设置环境变量
export DEEPSEEK_API_KEY="sk-85e83081df28490b9ae63188f0cb4f79"
export OPENAI_API_KEY="your-openai-api-key-here"
export ANTHROPIC_API_KEY="your-anthropic-api-key-here"
export GEMINI_API_KEY="your-gemini-api-key-here"

# 创建必要的目录
mkdir -p docs reports temp_docs examples_data

# 复制示例数据
cp -r examples/examples_data/*.txt docs/ 2>/dev/null || true

# 运行hello模块的示例
echo -e "${YELLOW}运行hello模块的示例...${NC}"
./gradlew :examples-modules:hello:runHelloWorld
if [ $? -eq 0 ]; then
    echo -e "${GREEN}hello模块的示例运行成功!${NC}"
else
    echo -e "${RED}hello模块的示例运行失败!${NC}"
fi

# 运行workflow模块的示例
echo -e "\n${YELLOW}运行workflow模块的示例...${NC}"
./gradlew :examples-modules:workflow:build
if [ $? -eq 0 ]; then
    echo -e "${GREEN}workflow模块编译成功!${NC}"
    
    # 尝试运行各个示例
    echo -e "\n${YELLOW}运行WorkflowExample...${NC}"
    ./gradlew :examples-modules:workflow:runWorkflowExample || echo -e "${RED}WorkflowExample运行失败!${NC}"
    
    echo -e "\n${YELLOW}运行DynamicWorkflowExample...${NC}"
    ./gradlew :examples-modules:workflow:runDynamicWorkflowExample || echo -e "${RED}DynamicWorkflowExample运行失败!${NC}"
    
    echo -e "\n${YELLOW}运行AdvancedWorkflowExample...${NC}"
    ./gradlew :examples-modules:workflow:runAdvancedWorkflowExample || echo -e "${RED}AdvancedWorkflowExample运行失败!${NC}"
    
    echo -e "\n${YELLOW}运行WorkflowRetryExample...${NC}"
    ./gradlew :examples-modules:workflow:runWorkflowRetryExample || echo -e "${RED}WorkflowRetryExample运行失败!${NC}"
else
    echo -e "${RED}workflow模块编译失败!${NC}"
fi

# 运行rag模块的示例
echo -e "\n${YELLOW}运行rag模块的示例...${NC}"
./gradlew :examples-modules:rag:build
if [ $? -eq 0 ]; then
    echo -e "${GREEN}rag模块编译成功!${NC}"
    
    # 尝试运行各个示例
    echo -e "\n${YELLOW}运行RAGExample...${NC}"
    ./gradlew :examples-modules:rag:runRAGExample || echo -e "${RED}RAGExample运行失败!${NC}"
    
    echo -e "\n${YELLOW}运行RAGWorkflowExample...${NC}"
    ./gradlew :examples-modules:rag:runRAGWorkflowExample || echo -e "${RED}RAGWorkflowExample运行失败!${NC}"
    
    echo -e "\n${YELLOW}运行FastEmbedRAGExample...${NC}"
    ./gradlew :examples-modules:rag:runFastEmbedRAGExample || echo -e "${RED}FastEmbedRAGExample运行失败!${NC}"
else
    echo -e "${RED}rag模块编译失败!${NC}"
fi

# 运行memory模块的示例
echo -e "\n${YELLOW}运行memory模块的示例...${NC}"
./gradlew :examples-modules:memory:build
if [ $? -eq 0 ]; then
    echo -e "${GREEN}memory模块编译成功!${NC}"
    
    # 尝试运行各个示例
    echo -e "\n${YELLOW}运行WorkingMemoryExample...${NC}"
    ./gradlew :examples-modules:memory:runWorkingMemoryExample || echo -e "${RED}WorkingMemoryExample运行失败!${NC}"
    
    echo -e "\n${YELLOW}运行MemoryCompressionExample...${NC}"
    ./gradlew :examples-modules:memory:runMemoryCompressionExample || echo -e "${RED}MemoryCompressionExample运行失败!${NC}"
    
    echo -e "\n${YELLOW}运行MemoryManagerExample...${NC}"
    ./gradlew :examples-modules:memory:runMemoryManagerExample || echo -e "${RED}MemoryManagerExample运行失败!${NC}"
    
    echo -e "\n${YELLOW}运行TagsAndSharingExample...${NC}"
    ./gradlew :examples-modules:memory:runTagsAndSharingExample || echo -e "${RED}TagsAndSharingExample运行失败!${NC}"
else
    echo -e "${RED}memory模块编译失败!${NC}"
fi

# 运行tools模块的示例
echo -e "\n${YELLOW}运行tools模块的示例...${NC}"
./gradlew :examples-modules:tools:build
if [ $? -eq 0 ]; then
    echo -e "${GREEN}tools模块编译成功!${NC}"
    
    # 尝试运行各个示例
    echo -e "\n${YELLOW}运行AdvancedZodToolExample...${NC}"
    ./gradlew :examples-modules:tools:runAdvancedZodToolExample || echo -e "${RED}AdvancedZodToolExample运行失败!${NC}"
    
    echo -e "\n${YELLOW}运行DataClassZodToolExample...${NC}"
    ./gradlew :examples-modules:tools:runDataClassZodToolExample || echo -e "${RED}DataClassZodToolExample运行失败!${NC}"
    
    echo -e "\n${YELLOW}运行DateTimeToolExample...${NC}"
    ./gradlew :examples-modules:tools:runDateTimeToolExample || echo -e "${RED}DateTimeToolExample运行失败!${NC}"
    
    echo -e "\n${YELLOW}运行ZodAdvancedToolExample...${NC}"
    ./gradlew :examples-modules:tools:runZodAdvancedToolExample || echo -e "${RED}ZodAdvancedToolExample运行失败!${NC}"
    
    echo -e "\n${YELLOW}运行ZodCalculatorExample...${NC}"
    ./gradlew :examples-modules:tools:runZodCalculatorExample || echo -e "${RED}ZodCalculatorExample运行失败!${NC}"
    
    echo -e "\n${YELLOW}运行ZodCalculatorToolExample...${NC}"
    ./gradlew :examples-modules:tools:runZodCalculatorToolExample || echo -e "${RED}ZodCalculatorToolExample运行失败!${NC}"
else
    echo -e "${RED}tools模块编译失败!${NC}"
fi

# 运行agent模块的示例
echo -e "\n${YELLOW}运行agent模块的示例...${NC}"
./gradlew :examples-modules:agent:build
if [ $? -eq 0 ]; then
    echo -e "${GREEN}agent模块编译成功!${NC}"
    
    # 尝试运行各个示例
    echo -e "\n${YELLOW}运行ZodAgentExample...${NC}"
    ./gradlew :examples-modules:agent:runZodAgentExample || echo -e "${RED}ZodAgentExample运行失败!${NC}"
    
    echo -e "\n${YELLOW}运行AdaptiveAgentExample...${NC}"
    ./gradlew :examples-modules:agent:runAdaptiveAgentExample || echo -e "${RED}AdaptiveAgentExample运行失败!${NC}"
    
    echo -e "\n${YELLOW}运行AdvancedAgentExample...${NC}"
    ./gradlew :examples-modules:agent:runAdvancedAgentExample || echo -e "${RED}AdvancedAgentExample运行失败!${NC}"
    
    echo -e "\n${YELLOW}运行AgentStateExample...${NC}"
    ./gradlew :examples-modules:agent:runAgentStateExample || echo -e "${RED}AgentStateExample运行失败!${NC}"
    
    echo -e "\n${YELLOW}运行AgentVersioningExample...${NC}"
    ./gradlew :examples-modules:agent:runAgentVersioningExample || echo -e "${RED}AgentVersioningExample运行失败!${NC}"
    
    echo -e "\n${YELLOW}运行GoalOrientedAgentExample...${NC}"
    ./gradlew :examples-modules:agent:runGoalOrientedAgentExample || echo -e "${RED}GoalOrientedAgentExample运行失败!${NC}"
    
    echo -e "\n${YELLOW}运行ReflectiveAgentExample...${NC}"
    ./gradlew :examples-modules:agent:runReflectiveAgentExample || echo -e "${RED}ReflectiveAgentExample运行失败!${NC}"
    
    echo -e "\n${YELLOW}运行HierarchicalAgentExample...${NC}"
    ./gradlew :examples-modules:agent:runHierarchicalAgentExample || echo -e "${RED}HierarchicalAgentExample运行失败!${NC}"
    
    echo -e "\n${YELLOW}运行AgentNetworkExample...${NC}"
    ./gradlew :examples-modules:agent:runAgentNetworkExample || echo -e "${RED}AgentNetworkExample运行失败!${NC}"
else
    echo -e "${RED}agent模块编译失败!${NC}"
fi

# 运行other模块的示例
echo -e "\n${YELLOW}运行other模块的示例...${NC}"
./gradlew :examples-modules:other:build
if [ $? -eq 0 ]; then
    echo -e "${GREEN}other模块编译成功!${NC}"
    
    # 尝试运行各个示例
    echo -e "\n${YELLOW}运行DataSourceExample...${NC}"
    ./gradlew :examples-modules:other:runDataSourceExample || echo -e "${RED}DataSourceExample运行失败!${NC}"
    
    echo -e "\n${YELLOW}运行AnthropicDirectStreamingExample...${NC}"
    ./gradlew :examples-modules:other:runAnthropicDirectStreamingExample || echo -e "${RED}AnthropicDirectStreamingExample运行失败!${NC}"
    
    echo -e "\n${YELLOW}运行AnthropicStreamingExample...${NC}"
    ./gradlew :examples-modules:other:runAnthropicStreamingExample || echo -e "${RED}AnthropicStreamingExample运行失败!${NC}"
    
    echo -e "\n${YELLOW}运行GeminiDirectStreamingExample...${NC}"
    ./gradlew :examples-modules:other:runGeminiDirectStreamingExample || echo -e "${RED}GeminiDirectStreamingExample运行失败!${NC}"
    
    echo -e "\n${YELLOW}运行GeminiStreamingExample...${NC}"
    ./gradlew :examples-modules:other:runGeminiStreamingExample || echo -e "${RED}GeminiStreamingExample运行失败!${NC}"
else
    echo -e "${RED}other模块编译失败!${NC}"
fi

# 运行plugin模块的示例
echo -e "\n${YELLOW}运行plugin模块的示例...${NC}"
./gradlew :examples-modules:plugin:build
if [ $? -eq 0 ]; then
    echo -e "${GREEN}plugin模块编译成功!${NC}"
    
    # 尝试运行各个示例
    echo -e "\n${YELLOW}运行HttpConnectorPluginExample...${NC}"
    ./gradlew :examples-modules:plugin:runHttpConnectorPluginExample || echo -e "${RED}HttpConnectorPluginExample运行失败!${NC}"
    
    echo -e "\n${YELLOW}运行HttpStepPluginExample...${NC}"
    ./gradlew :examples-modules:plugin:runHttpStepPluginExample || echo -e "${RED}HttpStepPluginExample运行失败!${NC}"
else
    echo -e "${RED}plugin模块编译失败!${NC}"
fi

echo -e "\n${GREEN}所有示例运行完成!${NC}"
