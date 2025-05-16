package ai.kastrax.code.mock

/**
 * LLM请求
 *
 * @property model 模型
 * @property prompt 提示
 * @property temperature 温度
 * @property maxTokens 最大令牌数
 */
data class LLMRequest(
    val model: String,
    val prompt: String,
    val temperature: Double = 0.7,
    val maxTokens: Int = 1000
)

/**
 * LLM响应
 *
 * @property content 内容
 */
data class LLMResponse(
    val content: String
)

/**
 * LLM提供者
 */
interface LLMProvider {
    /**
     * 完成请求
     *
     * @param request 请求
     * @return 响应
     */
    fun complete(request: LLMRequest): LLMResponse
}

/**
 * DeepSeek提供者
 */
class DeepSeekProvider : LLMProvider {
    private var model: String = DeepSeekModel.DEEPSEEK_CODER
    private var apiKey: String = ""
    private var temperature: Double = 0.7
    private var maxTokens: Int = 1000

    /**
     * 设置模型
     *
     * @param model 模型
     * @return 提供者
     */
    fun model(model: String): DeepSeekProvider {
        this.model = model
        return this
    }

    /**
     * 设置API密钥
     *
     * @param apiKey API密钥
     * @return 提供者
     */
    fun apiKey(apiKey: String): DeepSeekProvider {
        this.apiKey = apiKey
        return this
    }

    /**
     * 设置温度
     *
     * @param temperature 温度
     * @return 提供者
     */
    fun temperature(temperature: Double): DeepSeekProvider {
        this.temperature = temperature
        return this
    }

    /**
     * 设置最大令牌数
     *
     * @param maxTokens 最大令牌数
     * @return 提供者
     */
    fun maxTokens(maxTokens: Int): DeepSeekProvider {
        this.maxTokens = maxTokens
        return this
    }

    /**
     * 完成请求
     *
     * @param request 请求
     * @return 响应
     */
    override fun complete(request: LLMRequest): LLMResponse {
        // 模拟响应
        return LLMResponse(
            content = "这是一个模拟的DeepSeek响应"
        )
    }
}

/**
 * DeepSeek模型
 */
object DeepSeekModel {
    /**
     * DeepSeek Coder
     */
    const val DEEPSEEK_CODER = "deepseek-coder"

    /**
     * DeepSeek Chat
     */
    const val DEEPSEEK_CHAT = "deepseek-chat"
}

/**
 * 创建DeepSeek提供者
 *
 * @param init 初始化函数
 * @return DeepSeek提供者
 */
fun deepSeek(init: DeepSeekProvider.() -> Unit): DeepSeekProvider {
    val provider = DeepSeekProvider()
    provider.init()
    return provider
}
