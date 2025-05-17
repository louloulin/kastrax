package ai.kastrax.code.agent.specialized

import ai.kastrax.code.agent.CodeAgent
import ai.kastrax.code.common.KastraXCodeBase
import ai.kastrax.code.model.TaskType
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
 * 任务分类智能体
 *
 * 负责将用户请求分类为不同的任务类型
 */
@Service(Service.Level.PROJECT)
class TaskClassifierAgent(
    private val project: Project
) : KastraXCodeBase(component = "TASK_CLASSIFIER_AGENT"), CodeAgent {

    // 底层智能体
    private val agent: Agent by lazy {
        agent {
            name = "任务分类智能体"
            instructions = """
                你是一个任务分类器，需要将用户请求分类为以下任务类型之一：
                1. CODE_GENERATION - 生成代码
                2. CODE_EXPLANATION - 解释代码
                3. CODE_REFACTORING - 重构代码
                4. TEST_GENERATION - 生成测试
                5. UNKNOWN - 未知任务

                请只返回任务类型，不要有其他内容。
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
     * 分析任务类型
     *
     * @param request 用户请求
     * @return 任务类型
     */
    suspend fun analyzeTaskType(request: String): TaskType = withContext(Dispatchers.IO) {
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

            val response = agent.generate(messages, options).text.trim()

            // 解析任务类型
            return@withContext when {
                response.contains("CODE_GENERATION", ignoreCase = true) -> TaskType.CODE_GENERATION
                response.contains("CODE_EXPLANATION", ignoreCase = true) -> TaskType.CODE_EXPLANATION
                response.contains("CODE_REFACTORING", ignoreCase = true) -> TaskType.CODE_REFACTORING
                response.contains("TEST_GENERATION", ignoreCase = true) -> TaskType.TEST_GENERATION
                else -> TaskType.UNKNOWN
            }
        } catch (e: Exception) {
            logger.error("分析任务类型时出错: $request", e)
            return@withContext TaskType.UNKNOWN
        }
    }



    companion object {
        /**
         * 获取实例
         */
        fun getInstance(project: Project): TaskClassifierAgent {
            return project.service<TaskClassifierAgent>()
        }
    }
}
