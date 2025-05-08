#!/bin/bash

# 更新ex.md文件中的示例状态
update_ex_md() {
  local example=$1
  local status=$2
  local message=$3
  
  # 使用sed更新ex.md文件中的示例状态
  sed -i '' "s/- \[.\] $example.*$/- [$status] $example - $message/" ex.md 2>/dev/null || true
}

# 更新工作流相关示例
echo "更新工作流相关示例状态..."

# WorkflowExample
update_ex_md "WorkflowExample" "~" "已分析，但由于编译问题无法运行。该示例实现了内容创作工作流，包括研究、写作和编辑三个步骤。"

# WorkflowRetryExample
update_ex_md "WorkflowRetryExample" "~" "已分析，但由于编译问题无法运行。该示例实现了工作流重试机制，可以在步骤失败时自动重试。"

# AdvancedWorkflowExample
update_ex_md "AdvancedWorkflowExample" "~" "已分析，但由于编译问题无法运行。该示例实现了高级工作流功能，包括内容生成、审核、改进、并行处理和最终处理步骤。"

# DynamicWorkflowExample
update_ex_md "DynamicWorkflowExample" "~" "已分析，但由于编译问题无法运行。该示例实现了动态工作流，可以在运行时生成和组合工作流。"

# 更新RAG相关示例
echo "更新RAG相关示例状态..."

# RAGExample
update_ex_md "RAGExample" "~" "已分析，但由于编译问题无法运行。该示例实现了基础RAG系统，可以从文档中检索信息并生成回答。"

# RAGWorkflowExample
update_ex_md "RAGWorkflowExample" "~" "已分析，但由于编译问题无法运行。该示例实现了RAG工作流，包含研究、分析和报告生成步骤。"

# FastEmbedRAGExample
update_ex_md "FastEmbedRAGExample" "~" "已分析，但由于编译问题无法运行。该示例实现了使用本地嵌入模型的RAG系统，无需依赖外部API。"

# 更新内存相关示例
echo "更新内存相关示例状态..."

# WorkingMemoryExample
update_ex_md "WorkingMemoryExample" "~" "已分析，但由于编译问题无法运行。该示例实现了工作内存功能，可以记录和更新用户信息和对话上下文。"

# 更新工具相关示例
echo "更新工具相关示例状态..."

# AdvancedZodToolExample
update_ex_md "AdvancedZodToolExample" "~" "已分析，但由于编译问题无法运行。该示例实现了高级Zod工具，包括复杂数据结构的验证和转换。"

# DataClassZodToolExample
update_ex_md "DataClassZodToolExample" "~" "已分析，但由于编译问题无法运行。该示例实现了使用数据类的Zod工具，包括用户数据验证。"

# ZodAdvancedToolExample
update_ex_md "ZodAdvancedToolExample" "~" "已分析，但由于编译问题无法运行。该示例实现了高级Zod工具，包括用户搜索功能和复杂数据结构处理。"

# ZodAgentExample
update_ex_md "ZodAgentExample" "~" "已分析，但由于编译问题无法运行。该示例实现了使用Zod工具的Agent，可以执行数学计算和日期时间处理。"

# ZodCalculatorExample
update_ex_md "ZodCalculatorExample" "~" "已分析，但由于编译问题无法运行。该示例实现了计算器工具，可以执行基本的数学运算。"

# ZodCalculatorToolExample
update_ex_md "ZodCalculatorToolExample" "~" "已分析，但由于编译问题无法运行。该示例实现了使用数据类的计算器工具，包括输入验证和结果格式化。"

# 更新HelloWorld示例
update_ex_md "HelloWorld" "x" "已成功运行。这是一个简单的Java示例，用于测试编译和运行。"

echo "所有示例状态更新完成。"
