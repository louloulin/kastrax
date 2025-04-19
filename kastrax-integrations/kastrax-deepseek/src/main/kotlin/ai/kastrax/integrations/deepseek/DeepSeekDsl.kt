package ai.kastrax.integrations.deepseek

import ai.kastrax.core.llm.LlmProvider

/**
 * DeepSeek 配置类，用于 DSL 构建。
 */
class DeepSeekConfig {
    /** DeepSeek 模型 */
    var model: String = DeepSeekModel.DEEPSEEK_CHAT.id
    
    /** DeepSeek API 密钥 */
    var apiKey: String = ""
    
    /**
     * 设置模型。
     *
     * @param model DeepSeek 模型
     */
    fun model(model: DeepSeekModel) {
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
     * @param apiKey DeepSeek API 密钥
     */
    fun apiKey(apiKey: String) {
        this.apiKey = apiKey
    }
}

/**
 * 创建 DeepSeek 提供商。
 *
 * @param model DeepSeek 模型
 * @param apiKey DeepSeek API 密钥（可选，默认从环境变量获取）
 * @return DeepSeek 提供商
 */
fun deepSeek(
    model: String = DeepSeekModel.DEEPSEEK_CHAT.id,
    apiKey: String = System.getenv("DEEPSEEK_API_KEY") ?: ""
): LlmProvider {
    return DeepSeekProvider(model, apiKey)
}

/**
 * 使用 DSL 创建 DeepSeek 提供商。
 *
 * @param init 配置初始化函数
 * @return DeepSeek 提供商
 */
fun deepSeek(init: DeepSeekConfig.() -> Unit): LlmProvider {
    val config = DeepSeekConfig().apply(init)
    
    // 如果 API 密钥为空，尝试从环境变量获取
    val apiKey = if (config.apiKey.isBlank()) {
        System.getenv("DEEPSEEK_API_KEY") ?: throw IllegalArgumentException(
            "DeepSeek API key is required. Either provide it explicitly or set the DEEPSEEK_API_KEY environment variable."
        )
    } else {
        config.apiKey
    }
    
    return DeepSeekProvider(config.model, apiKey)
}
