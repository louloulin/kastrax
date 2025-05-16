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
 * 查询响应智能体
 *
 * 负责回答用户的一般性编程问题
 */
@Service(Service.Level.PROJECT)
class QueryResponseAgent(
    private val project: Project
) : KastraXCodeBase(component = "QUERY_RESPONSE_AGENT"), CodeAgent {

    // 底层智能体
    private val agent: Agent by lazy {
        agent {
            name = "查询响应智能体"
            instructions = "你是一个代码智能助手，可以回答各种编程相关的问题。"
            model = llmProvider
        }
    }

    // DeepSeek提供者
    private val llmProvider: LlmProvider by lazy {
        ai.kastrax.integrations.deepseek.deepSeek {
            model(ai.kastrax.integrations.deepseek.DeepSeekModel.DEEPSEEK_CODER)
            apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "")
            temperature(0.7)
            maxTokens(1000)
        }
    }

    /**
     * 回答查询
     *
     * @param request 用户请求
     * @param context 上下文
     * @return 响应
     */
    suspend fun answerQuery(request: String, context: String): String = withContext(Dispatchers.IO) {
        try {
            // 创建消息
            val messages = listOf(
                LlmMessage(
                    role = LlmMessageRole.USER,
                    content = "$request\n\n上下文信息：\n$context"
                )
            )

            // 生成响应
            val options = AgentGenerateOptions(
                temperature = 0.7,
                maxTokens = 1000
            )

            return@withContext agent.generate(messages, options).text
        } catch (e: Exception) {
            logger.error("回答查询时出错: $request", e)
            return@withContext "处理请求时出错: ${e.message}"
        }
    }

    companion object {
        /**
         * 获取实例
         */
        fun getInstance(project: Project): QueryResponseAgent {
            return project.service<QueryResponseAgent>()
        }
    }
}
