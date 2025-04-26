package ai.kastrax.examples.agent

import ai.kastrax.core.agent.agent
import ai.kastrax.core.agent.agentNetwork
import ai.kastrax.core.tools.tool
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import java.io.File

/**
 * 协作型Agent网络示例，展示上下文感知路由和可视化功能
 */
fun main() = runBlocking {
    // 创建研究代理
    val researchAgent = agent {
        name = "研究代理"
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
            apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "your-api-key-here")
        }
    }

    // 创建网络搜索代理
    val webSearchAgent = agent {
        name = "网络搜索代理"
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
            apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "your-api-key-here")
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
                    val query = input.jsonObject["query"]?.jsonPrimitive?.contentOrNull ?: "未知查询"
                    buildJsonObject {
                        put("results", """
                            这是关于"$query"的搜索结果：
                            
                            1. 人工智能在医疗保健中的应用 - 最新研究报告 (2023)
                               来源: https://example.com/ai-healthcare-2023
                               摘要: 该报告详细介绍了AI在诊断、药物开发和个性化医疗方面的最新应用。
                            
                            2. 机器学习如何改变医疗影像分析 - 医学期刊文章
                               来源: https://example.com/ml-medical-imaging
                               摘要: 研究表明，AI辅助诊断系统在某些情况下准确率超过了人类放射科医生。
                            
                            3. 医疗保健中的自然语言处理应用 - 技术评论
                               来源: https://example.com/nlp-healthcare
                               摘要: NLP技术正在帮助医生从电子健康记录中提取有价值的信息。
                        """.trimIndent())
                    }
                }
            }
        }
    }

    // 创建数据分析代理
    val dataAnalysisAgent = agent {
        name = "数据分析代理"
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
            apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "your-api-key-here")
        }

        // 添加数据分析工具
        tools {
            tool {
                id = "analyze_data"
                name = "数据分析"
                description = "分析数据集并提供见解"
                inputSchema = buildJsonObject {
                    put("type", "object")
                    putJsonObject("properties") {
                        putJsonObject("dataset") {
                            put("type", "string")
                            put("description", "要分析的数据集描述")
                        }
                        putJsonObject("analysis_type") {
                            put("type", "string")
                            put("description", "分析类型（趋势、比较、预测等）")
                        }
                    }
                    putJsonArray("required") {
                        add("dataset")
                    }
                }
                execute = { input ->
                    // 模拟数据分析结果
                    val dataset = input.jsonObject["dataset"]?.jsonPrimitive?.contentOrNull ?: "未知数据集"
                    val analysisType = input.jsonObject["analysis_type"]?.jsonPrimitive?.contentOrNull ?: "趋势分析"
                    
                    buildJsonObject {
                        put("results", """
                            对"$dataset"的"$analysisType"分析结果：
                            
                            1. 主要发现:
                               - AI在医疗诊断中的应用增长率为每年35%
                               - 医疗机构采用AI技术的比例从2020年的15%增加到2023年的42%
                               - 患者对AI辅助诊断的接受度提高了28%
                            
                            2. 趋势分析:
                               - 远程医疗AI应用增长最快，年增长率达到48%
                               - 药物发现中的AI应用投资增加了3倍
                               - 医疗影像分析仍然是最成熟的AI应用领域
                            
                            3. 预测:
                               - 到2025年，约70%的医疗机构将在某种形式上采用AI技术
                               - 个性化医疗将成为AI应用的下一个主要增长点
                               - 监管框架将成为AI医疗应用扩展的主要挑战
                        """.trimIndent())
                    }
                }
            }
        }
    }

    // 创建代理网络，使用上下文感知路由和可视化
    val researchNetwork = agentNetwork {
        name = "协作研究网络"
        instructions = """
            你是一个研究协调系统，负责将查询路由到适当的专业代理。

            根据查询的性质，你可以调用以下专业代理：
            1. 研究代理 - 用于综合信息和协调研究
            2. 网络搜索代理 - 用于查找最新的在线信息
            3. 数据分析代理 - 用于分析和解释数据

            你的目标是提供全面、准确的研究结果。
            
            使用协作模式来最大化研究质量：
            - 顺序协作：一个代理接一个代理工作
            - 并行协作：多个代理同时工作，然后综合结果
            - 专家小组：让多个代理评估同一问题
            - 迭代改进：让一个代理工作，然后让另一个代理改进结果
        """.trimIndent()
        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "your-api-key-here")
        }

        // 添加专业代理
        agent(researchAgent)
        agent(webSearchAgent)
        agent(dataAnalysisAgent)
        
        // 使用上下文感知路由策略
        useContextAwareRouting()
        
        // 启用交互可视化
        enableVisualization()
    }

    // 使用代理网络
    println("=== 使用协作型代理网络 ===")
    val query = "分析人工智能在医疗保健中的最新应用和趋势，特别关注诊断和药物开发领域"
    println("查询: $query")

    val response = researchNetwork.generate(query)
    println("\n响应:")
    println(response.text)

    // 打印代理交互历史
    println("\n代理交互历史:")
    println(researchNetwork.getAgentInteractionSummary())
    
    // 生成并保存可视化
    val visualization = researchNetwork.getAgentInteractionVisualization()
    if (visualization != null) {
        val visualizationFile = File("agent_network_visualization.html")
        visualizationFile.writeText(visualization)
        println("\n可视化已保存到: ${visualizationFile.absolutePath}")
    }
}
