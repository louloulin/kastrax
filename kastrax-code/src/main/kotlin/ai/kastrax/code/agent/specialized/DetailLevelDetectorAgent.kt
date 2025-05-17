package ai.kastrax.code.agent.specialized

import ai.kastrax.code.agent.CodeAgent
import ai.kastrax.code.common.KastraXCodeBase
import ai.kastrax.code.model.DetailLevel
import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.agent
import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import ai.kastrax.core.llm.LlmProvider
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 详细程度检测智能体
 *
 * 负责从用户请求中检测详细程度
 */
@Service(Service.Level.PROJECT)
class DetailLevelDetectorAgent(
    private val project: Project
) : KastraXCodeBase(component = "DETAIL_LEVEL_DETECTOR_AGENT"), CodeAgent {

    // 底层智能体
    private val agent: Agent by lazy {
        agent {
            name = "详细程度检测智能体"
            instructions = """
                你是一个详细程度检测器，需要从用户请求中检测出用户想要的详细程度。
                请从以下选项中选择一个：
                BRIEF - 简要解释
                NORMAL - 正常详细程度的解释
                DETAILED - 非常详细的解释

                如果用户请求中包含"详细"、"详尽"、"detailed"等词，请选择 DETAILED。
                如果用户请求中包含"简要"、"简单"、"brief"等词，请选择 BRIEF。
                如果无法确定，请选择 NORMAL。

                请只返回选项名称，不要有其他内容。
            """.trimIndent()
            model = llmProvider
        }
    }

    // DeepSeek提供者
    private val llmProvider: LlmProvider by lazy {
        ai.kastrax.integrations.deepseek.deepSeek {
            model(ai.kastrax.integrations.deepseek.DeepSeekModel.DEEPSEEK_CODER)
            apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "")
            temperature(0.0)
            maxTokens(10)
        }
    }

    /**
     * 检测详细程度
     *
     * @param request 用户请求
     * @return 详细程度
     */
    suspend fun detectDetailLevel(request: String): DetailLevel = withContext(Dispatchers.IO) {
        try {
            // 创建消息
            val messages = listOf(
                LlmMessage(
                    role = LlmMessageRole.USER,
                    content = request
                )
            )

            // 生成响应
            val options = AgentGenerateOptions(
                temperature = 0.0,
                maxTokens = 10
            )

            val response = agent.generate(messages, options).text.trim().uppercase()

            return@withContext when {
                response.contains("DETAILED") -> DetailLevel.DETAILED
                response.contains("BRIEF") -> DetailLevel.BRIEF
                else -> DetailLevel.NORMAL
            }
        } catch (e: Exception) {
            logger.error("检测详细程度时出错: $request", e)
            return@withContext DetailLevel.NORMAL
        }
    }

    /**
     * 生成代码
     *
     * @param prompt 提示文本
     * @param language 编程语言
     * @return 生成的代码
     */
    override suspend fun generateCode(prompt: String, language: String): String = withContext(Dispatchers.IO) {
        try {
            // 创建消息
            val messages = listOf(
                LlmMessage(
                    role = LlmMessageRole.SYSTEM,
                    content = "你是一个代码生成器，根据用户的请求生成${language}代码。"
                ),
                LlmMessage(
                    role = LlmMessageRole.USER,
                    content = prompt
                )
            )

            // 生成响应
            val options = AgentGenerateOptions(
                temperature = 0.2,
                maxTokens = 1000
            )

            return@withContext agent.generate(messages, options).text
        } catch (e: Exception) {
            logger.error("生成代码时出错: $prompt", e)
            return@withContext "生成代码时出错: ${e.message}"
        }
    }



    companion object {
        /**
         * 获取实例
         */
        fun getInstance(project: Project): DetailLevelDetectorAgent {
            return project.service<DetailLevelDetectorAgent>()
        }
    }
}
