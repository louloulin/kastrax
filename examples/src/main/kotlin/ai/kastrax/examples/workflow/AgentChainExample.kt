package ai.kastrax.examples.workflow

import ai.kastrax.core.agent.agent
import ai.kastrax.core.workflow.agentChain
import ai.kastrax.integrations.openai.openAi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking

/**
 * Agent链示例
 */
fun main() = runBlocking {
    // 创建研究规划代理
    val researchPlannerAgent = agent {
        name = "研究规划代理"
        instructions = """
            你是一个研究规划专家。你的工作是：
            1. 分析研究主题，确定需要调查的关键领域
            2. 创建全面的研究计划，包括主要问题和子问题
            3. 确定研究的范围和限制
            4. 提出可能的研究方法和资源
            
            你的输出应该是一个结构化的研究计划，可以指导后续的研究工作。
        """.trimIndent()
        model = openAi("gpt-4o")
    }
    
    // 创建信息收集代理
    val informationGatheringAgent = agent {
        name = "信息收集代理"
        instructions = """
            你是一个信息收集专家。你的工作是：
            1. 基于研究计划收集相关信息
            2. 组织和分类收集到的信息
            3. 评估信息的质量和相关性
            4. 识别信息中的差距和不一致
            
            你的输出应该是一个结构化的信息摘要，包括关键发现和见解。
        """.trimIndent()
        model = openAi("gpt-4o")
    }
    
    // 创建报告生成代理
    val reportGenerationAgent = agent {
        name = "报告生成代理"
        instructions = """
            你是一个报告生成专家。你的工作是：
            1. 基于收集到的信息创建全面的报告
            2. 组织内容以清晰、逻辑的方式呈现
            3. 提供有意义的分析和见解
            4. 确保报告格式一致且专业
            
            你的输出应该是一个结构良好的最终报告，包括引言、主体和结论。
        """.trimIndent()
        model = openAi("gpt-4o")
    }
    
    // 创建Agent链
    val researchChain = agentChain {
        name = "研究链"
        description = "一个用于执行研究任务的代理链"
        
        // 添加代理（按执行顺序）
        agent(researchPlannerAgent)
        agent(informationGatheringAgent)
        agent(reportGenerationAgent)
    }
    
    // 使用Agent链
    println("=== 使用Agent链 ===")
    val topic = "量子计算在密码学中的应用"
    println("研究主题: $topic")
    
    // 流式执行链
    println("\n开始执行研究链...")
    researchChain.streamExecute(topic).collect { update ->
        when (update.status) {
            ai.kastrax.core.workflow.AgentChainStatus.STARTED -> {
                println("研究链开始执行")
            }
            ai.kastrax.core.workflow.AgentChainStatus.IN_PROGRESS -> {
                println("正在执行: ${update.agentName} (进度: ${update.progress}%)")
            }
            ai.kastrax.core.workflow.AgentChainStatus.STEP_COMPLETED -> {
                println("步骤完成: ${update.agentName}")
                println("输出摘要: ${update.step?.output?.take(100)}...")
            }
            ai.kastrax.core.workflow.AgentChainStatus.COMPLETED -> {
                println("\n研究链执行完成 (进度: ${update.progress}%)")
                println("\n最终报告:")
                println(update.output)
            }
            else -> {
                println("状态更新: ${update.status} - ${update.message}")
            }
        }
    }
}
