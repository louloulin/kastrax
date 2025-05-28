package ai.kastrax.integrations.qwen

import ai.kastrax.core.llm.LlmProvider
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Qwen 配置类，用于 DSL 构建。
 */
class QwenConfig {
    /** Qwen 模型 */
    var model: String = QwenModel.QWEN2_5_72B_INSTRUCT.id

    /** Qwen API 密钥 */
    var apiKey: String = ""

    /** 采样温度 */
    var temperature: Double? = null

    /** 生成的最大令牌数 */
    var maxTokens: Int? = null

    /** 核采样参数 */
    var topP: Double? = null

    /** 请求超时时间（毫秒） */
    var timeout: Long = 60000

    /**
     * 设置模型。
     *
     * @param model Qwen 模型
     */
    fun model(model: QwenModel) {
        this.model = model.id
    }

    /**
     * 设置自定义模型 ID。
     *
     * @param modelId 自定义模型 ID
     */
    fun model(modelId: String) {
        this.model = modelId
    }

    /**
     * 设置 API 密钥。
     *
     * @param apiKey Qwen API 密钥
     */
    fun apiKey(apiKey: String) {
        this.apiKey = apiKey
    }

    /**
     * 设置采样温度。
     *
     * @param temperature 采样温度，控制输出的随机性
     */
    fun temperature(temperature: Double) {
        this.temperature = temperature
    }

    /**
     * 设置生成的最大令牌数。
     *
     * @param maxTokens 生成的最大令牌数
     */
    fun maxTokens(maxTokens: Int) {
        this.maxTokens = maxTokens
    }

    /**
     * 设置核采样参数。
     *
     * @param topP 核采样参数，用于控制输出的多样性
     */
    fun topP(topP: Double) {
        this.topP = topP
    }

    /**
     * 设置请求超时时间。
     *
     * @param timeout 请求超时时间（秒）
     */
    fun timeout(timeout: Int) {
        this.timeout = timeout * 1000L
    }
}

/**
 * 创建 Qwen 提供商。
 *
 * @param model Qwen 模型
 * @param apiKey Qwen API 密钥（可选，默认从环境变量获取）
 * @return Qwen 提供商
 */
fun qwen(
    model: String = QwenModel.QWEN2_5_72B_INSTRUCT.id,
    apiKey: String = System.getenv("QWEN_API_KEY") ?: ""
): LlmProvider {
    return QwenProvider(model, apiKey)
}

/**
 * 使用 DSL 创建 Qwen 提供商。
 *
 * @param init 配置初始化函数
 * @return Qwen 提供商
 */
fun qwen(init: QwenConfig.() -> Unit): LlmProvider {
    val config = QwenConfig().apply(init)

    // 如果 API 密钥为空，尝试从环境变量获取
    val apiKey = if (config.apiKey.isBlank()) {
        val envApiKey = System.getenv("QWEN_API_KEY")
        logger.debug { "Using API key from environment variable: ${envApiKey?.take(10)}..." }
        envApiKey ?: throw IllegalArgumentException(
            "Qwen API key is required. " +
            "Either provide it explicitly or set the QWEN_API_KEY environment variable."
        )
    } else {
        logger.debug { "Using explicitly provided API key: ${config.apiKey.take(10)}..." }
        config.apiKey
    }

    return QwenProvider(
        model = config.model,
        apiKey = apiKey,
        temperature = config.temperature,
        maxTokens = config.maxTokens,
        topP = config.topP,
        timeout = config.timeout
    )
}