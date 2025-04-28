package ai.kastrax.a2x.examples

import ai.kastrax.a2a.a2a
import ai.kastrax.a2a.dsl.a2aAgent
import ai.kastrax.a2x.a2x
import ai.kastrax.a2x.model.EntityType
import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

/**
 * A2X 语义示例
 */
fun main() = runBlocking {
    // 创建模拟的 kastrax 代理
    val assistantAgent = mockk<Agent>()

    // 配置模拟代理的行为
    coEvery { assistantAgent.name } returns "助手代理"
    coEvery { assistantAgent.generate(any<String>()) } returns AgentResponse(
        text = "这是一个模拟响应",
        toolCalls = emptyList()
    )

    // 创建 A2X 实例
    val a2xInstance = a2x {
        // 注册 kastrax 代理
        agent(assistantAgent)

        // 配置服务器
        server {
            port = 8080
            enableCors = true
        }

        // 配置语义
        semantic {
            // 创建天气本体
            ontology {
                id = "ontology-weather"
                name = "天气本体"
                description = "天气领域本体"
                version = "1.0.0"

                // 添加天气概念
                concept {
                    id = "concept-weather"
                    name = "天气"
                    description = "天气概念"
                    type = "entity"

                    // 添加温度属性
                    property {
                        name = "temperature"
                        type = "number"
                        description = "温度"
                    }

                    // 添加天气状况属性
                    property {
                        name = "condition"
                        type = "string"
                        description = "天气状况"
                    }

                    // 添加湿度属性
                    property {
                        name = "humidity"
                        type = "number"
                        description = "湿度"
                    }
                }

                // 添加位置概念
                concept {
                    id = "concept-location"
                    name = "位置"
                    description = "位置概念"
                    type = "entity"

                    // 添加名称属性
                    property {
                        name = "name"
                        type = "string"
                        description = "位置名称"
                    }

                    // 添加纬度属性
                    property {
                        name = "latitude"
                        type = "number"
                        description = "纬度"
                    }

                    // 添加经度属性
                    property {
                        name = "longitude"
                        type = "number"
                        description = "经度"
                    }
                }

                // 添加时间概念
                concept {
                    id = "concept-time"
                    name = "时间"
                    description = "时间概念"
                    type = "entity"

                    // 添加日期属性
                    property {
                        name = "date"
                        type = "string"
                        description = "日期"
                    }

                    // 添加时间属性
                    property {
                        name = "time"
                        type = "string"
                        description = "时间"
                    }
                }

                // 添加位置有天气关系
                relation {
                    id = "relation-has-weather"
                    name = "有天气"
                    description = "位置有天气"
                    type = "association"
                    sourceConcept = "concept-location"
                    targetConcept = "concept-weather"
                }

                // 添加天气在时间关系
                relation {
                    id = "relation-at-time"
                    name = "在时间"
                    description = "天气在时间"
                    type = "association"
                    sourceConcept = "concept-weather"
                    targetConcept = "concept-time"
                }
            }

            // 创建问候意图
            intent("问候", "用户问候") {
                keywordPattern(listOf("你好", "您好", "早上好", "下午好", "晚上好", "嗨", "哈喽", "hello", "hi"))
            }

            // 创建天气查询意图
            intent("天气查询", "查询天气") {
                keywordPattern(listOf("天气", "气温", "温度", "下雨", "下雪", "晴天", "阴天", "多云"))
            }

            // 创建告别意图
            intent("告别", "用户告别") {
                keywordPattern(listOf("再见", "拜拜", "拜", "goodbye", "bye", "下次见", "回头见"))
            }

            // 创建位置实体类型
            entityType("位置", "位置实体") {
                dictionaryExtractor("城市提取器", listOf("北京", "上海", "广州", "深圳", "杭州", "南京", "成都", "重庆", "武汉", "西安"))
            }

            // 创建时间实体类型
            entityType("时间", "时间实体") {
                dictionaryExtractor("时间提取器", listOf("今天", "明天", "后天", "昨天", "前天", "上午", "下午", "晚上", "早上", "中午"))
            }

            // 创建天气实体类型
            entityType("天气", "天气实体") {
                dictionaryExtractor("天气提取器", listOf("晴天", "阴天", "多云", "下雨", "下雪", "雾", "霾", "冰雹", "台风", "龙卷风"))
            }

            // 创建对话上下文
            val context = context("对话上下文", "用户与系统的对话上下文", "conversation")
        }
    }

    // 创建对话上下文
    val context = a2xInstance.createContext("对话上下文", "用户与系统的对话上下文", "conversation")

    // 创建会话
    val session = a2xInstance.createSession(context.id, "user-123")

    if (session != null) {
        // 处理消息
        val messages = listOf(
            "你好，我想查询天气",
            "北京明天的天气怎么样？",
            "谢谢，再见"
        )

        messages.forEach { message ->
            println("\n用户: $message")

            // 处理消息
            val result = a2xInstance.processMessage(message, session.id)

            // 打印意图
            println("识别的意图: ${result.intentResult.topIntent?.name ?: "未识别"} (置信度: ${result.intentResult.topConfidence})")

            // 打印实体
            result.entities.forEach { (type, entities) ->
                println("识别的实体 ($type): ${entities.map { it.text }}")
            }

            // 生成响应
            val response = a2xInstance.generateResponse(result, session.id)
            println("系统: ${response.text}")
        }

        // 打印会话历史
        val updatedSession = a2xInstance.getSemanticService().getSession(session.id)
        println("\n会话历史:")
        updatedSession?.history?.forEach { entry ->
            val text = when (entry.type) {
                "user" -> "用户: ${(entry.content as? kotlinx.serialization.json.JsonObject)?.get("text")?.toString()?.replace("\"", "")}"
                "system" -> "系统: ${(entry.content as? kotlinx.serialization.json.JsonObject)?.get("text")?.toString()?.replace("\"", "")}"
                else -> "${entry.type}: ${entry.content}"
            }
            println(text)
        }
    }

    // 停止服务器
    a2xInstance.stopServer()
}
