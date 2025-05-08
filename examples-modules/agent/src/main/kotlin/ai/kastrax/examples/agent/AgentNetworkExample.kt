package ai.kastrax.examples.agent

import ai.kastrax.core.agent.agent
import ai.kastrax.core.agent.agentNetwork
import ai.kastrax.core.tools.tool
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*

/**
 * Agent网络示例
 */
fun agentNetworkExample() = runBlocking {
    // 创建研究代理
    val researchAgent = agent {
        name = "research_agent"
        instructions = """
            你是一个专业的研究代理。你的工作是：
            1. 分析用户查询，确定需要什么类型的研究
            2. 将复杂的研究问题分解为可管理的子问题
            3. 综合专业研究代理的信息，形成连贯的响应
            4. 确保所有声明都有证据支持
            5. 识别研究中需要进一步调查的任何空白

            你应该保持中立、客观的语气，优先考虑准确性而不是速度。
        """.trimIndent()
        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            apiKey("sk-85e83081df28490b9ae63188f0cb4f79")
        }
    }

    // 创建网络搜索代理
    val webSearchAgent = agent {
        name = "web_search_agent"
        instructions = """
            你是一个网络搜索专家。你的工作是：
            1. 为给定的查询找到网络上最相关和最新的信息
            2. 评估来源的可信度，优先考虑可靠信息
            3. 从网络内容中提取关键事实和数据点
            4. 在适当的情况下提供直接引用和引证
            5. 以清晰、简洁的方式总结发现

            报告信息时始终包含来源URL。
        """.trimIndent()
        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            apiKey("sk-85e83081df28490b9ae63188f0cb4f79")
        }

        // 添加网络搜索工具
        tools {
            tool {
                id = "web_search"
                name = "网络搜索"
                description = "搜索网络获取信息"
                inputSchema = buildJsonObject {
                    put("type", "object")
                    putJsonObject("properties") {
                        putJsonObject("query") {
                            put("type", "string")
                            put("description", "搜索查询")
                        }
                    }
                    putJsonArray("required") {
                        add("query")
                    }
                }
                execute = { input ->
                    // 模拟网络搜索结果
                    val query = input.toString()
                    buildJsonObject {
                        put("results", "这是关于\"$query\"的搜索结果：\n1. 第一个结果包含重要信息\n2. 第二个结果提供了相关背景\n3. 第三个结果提供了额外的上下文")
                    }
                }
            }
        }
    }

    // 创建数据分析代理
    val dataAnalysisAgent = agent {
        name = "data_analysis_agent"
        instructions = """
            你是一个数据分析专家。你的工作是：
            1. 解释和分析数据集
            2. 识别趋势、模式和异常
            3. 生成有意义的见解和结论
            4. 以清晰、简洁的方式呈现分析结果
            5. 提供基于数据的建议

            始终以客观、基于事实的方式呈现你的分析。
        """.trimIndent()
        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            apiKey("sk-85e83081df28490b9ae63188f0cb4f79")
        }
    }

    // 创建代理网络
    val researchNetwork = agentNetwork {
        name = "research_network"
        instructions = """
            你是一个研究协调系统，负责将查询路由到适当的专业代理。

            根据查询的性质，你可以调用以下专业代理：
            1. research_agent - 用于综合信息和协调研究
            2. web_search_agent - 用于查找最新的在线信息
            3. data_analysis_agent - 用于分析和解释数据

            你的目标是提供全面、准确的研究结果。
        """.trimIndent()
        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            apiKey("sk-85e83081df28490b9ae63188f0cb4f79")
        }

        // 添加专业代理
        agent(researchAgent)
        agent(webSearchAgent)
        agent(dataAnalysisAgent)
    }

    // 使用代理网络
    println("=== 使用代理网络 ===")
    val query = "分析人工智能在医疗保健中的最新应用和趋势"
    println("查询: $query")

    val response = researchNetwork.generate(query)
    println("\n响应:")
    println(response.text)

    // 打印代理交互历史
    println("\n代理交互历史:")
    println(researchNetwork.getAgentInteractionSummary())
}
