package ai.kastrax.code.agent.specialized

import ai.kastrax.code.agent.CodeAgent
import ai.kastrax.code.common.KastraXCodeBase
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
 * 语言检测智能体
 *
 * 负责从用户请求中检测编程语言
 */
@Service(Service.Level.PROJECT)
class LanguageDetectorAgent(
    private val project: Project
) : KastraXCodeBase(component = "LANGUAGE_DETECTOR_AGENT"), CodeAgent {

    // 底层智能体
    private val agent: Agent by lazy {
        agent {
            name = "语言检测智能体"
            instructions = """
                你是一个编程语言检测器，需要从用户请求中检测出用户想要使用的编程语言。
                请从以下语言中选择一个：
                kotlin, java, python, javascript, typescript, html, css, cpp, csharp, go, rust, php, ruby, swift

                如果无法确定，请返回 kotlin。
                请只返回语言名称，不要有其他内容。
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
     * 检测语言
     *
     * @param request 用户请求
     * @return 语言
     */
    suspend fun detectLanguage(request: String): String = withContext(Dispatchers.IO) {
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

            val response = agent.generate(messages, options).text.trim().lowercase()

            // 验证语言
            val validLanguages = setOf(
                "kotlin", "java", "python", "javascript", "typescript",
                "html", "css", "cpp", "csharp", "go", "rust", "php", "ruby", "swift"
            )

            return@withContext if (response in validLanguages) response else "kotlin"
        } catch (e: Exception) {
            logger.error("检测语言时出错: $request", e)
            return@withContext "kotlin"
        }
    }



    companion object {
        /**
         * 获取实例
         */
        fun getInstance(project: Project): LanguageDetectorAgent {
            return project.service<LanguageDetectorAgent>()
        }
    }
}
