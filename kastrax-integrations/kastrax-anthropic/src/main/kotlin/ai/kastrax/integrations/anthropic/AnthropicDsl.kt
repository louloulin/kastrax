package ai.kastrax.integrations.anthropic

import ai.kastrax.core.llm.LlmProvider

/**
 * Anthropic 配置类，用于 DSL 构建。
 */
class AnthropicConfig {
    /** Anthropic 模型 */
    var model: String = AnthropicModel.CLAUDE_3_SONNET.id
    
    /** Anthropic API 密钥 */
    var apiKey: String = ""
    
    /** 是否使用增强的流式处理 */
    var useEnhancedStreaming: Boolean = false
    
    /**
     * 设置模型。
     *
     * @param model Anthropic 模型
     */
    fun model(model: AnthropicModel) {
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
     * @param apiKey Anthropic API 密钥
     */
    fun apiKey(apiKey: String) {
        this.apiKey = apiKey
    }
    
    /**
     * 设置是否使用增强的流式处理。
     *
     * @param useEnhanced 是否使用增强的流式处理
     */
    fun useEnhancedStreaming(useEnhanced: Boolean) {
        this.useEnhancedStreaming = useEnhanced
    }
}

/**
 * 创建 Anthropic 提供商。
 *
 * @param model Anthropic 模型
 * @param apiKey Anthropic API 密钥（可选，默认从环境变量获取）
 * @return Anthropic 提供商
 */
fun anthropic(
    model: String = AnthropicModel.CLAUDE_3_SONNET.id,
    apiKey: String = System.getenv("ANTHROPIC_API_KEY") ?: ""
): LlmProvider {
    return AnthropicProvider(model, apiKey)
}

/**
 * 使用 DSL 创建 Anthropic 提供商。
 *
 * @param init 配置初始化函数
 * @return Anthropic 提供商
 */
fun anthropic(init: AnthropicConfig.() -> Unit): LlmProvider {
    val config = AnthropicConfig().apply(init)
    
    // 如果 API 密钥为空，尝试从环境变量获取
    val apiKey = if (config.apiKey.isBlank()) {
        System.getenv("ANTHROPIC_API_KEY") ?: throw IllegalArgumentException(
            "Anthropic API key is required. Either provide it explicitly or set the ANTHROPIC_API_KEY environment variable."
        )
    } else {
        config.apiKey
    }
    
    return AnthropicProvider(
        model = config.model,
        apiKey = apiKey,
        useEnhancedStreaming = config.useEnhancedStreaming
    )
}
