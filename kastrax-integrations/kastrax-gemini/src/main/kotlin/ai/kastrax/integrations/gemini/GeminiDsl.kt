package ai.kastrax.integrations.gemini

import ai.kastrax.core.llm.LlmProvider

/**
 * Gemini 配置类，用于 DSL 构建。
 */
class GeminiConfig {
    /** Gemini 模型 */
    var model: String = GeminiModel.GEMINI_1_5_PRO.id
    
    /** Google API 密钥 */
    var apiKey: String = ""
    
    /** 是否使用增强的流式处理 */
    var useEnhancedStreaming: Boolean = false
    
    /** 嵌入模型名称 */
    var embeddingModel: String = "models/embedding-001"
    
    /**
     * 设置模型。
     *
     * @param model Gemini 模型
     */
    fun model(model: GeminiModel) {
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
     * @param apiKey Google API 密钥
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
    
    /**
     * 设置嵌入模型名称。
     *
     * @param model 嵌入模型名称
     */
    fun embeddingModel(model: String) {
        this.embeddingModel = model
    }
}

/**
 * 创建 Gemini 提供商。
 *
 * @param model Gemini 模型
 * @param apiKey Google API 密钥（可选，默认从环境变量获取）
 * @return Gemini 提供商
 */
fun gemini(
    model: String = GeminiModel.GEMINI_1_5_PRO.id,
    apiKey: String = System.getenv("GOOGLE_API_KEY") ?: ""
): LlmProvider {
    return GeminiProvider(model, apiKey)
}

/**
 * 使用 DSL 创建 Gemini 提供商。
 *
 * @param init 配置初始化函数
 * @return Gemini 提供商
 */
fun gemini(init: GeminiConfig.() -> Unit): LlmProvider {
    val config = GeminiConfig().apply(init)
    
    // 如果 API 密钥为空，尝试从环境变量获取
    val apiKey = if (config.apiKey.isBlank()) {
        System.getenv("GOOGLE_API_KEY") ?: throw IllegalArgumentException(
            "Google API key is required. Either provide it explicitly or set the GOOGLE_API_KEY environment variable."
        )
    } else {
        config.apiKey
    }
    
    return GeminiProvider(
        model = config.model,
        apiKey = apiKey,
        useEnhancedStreaming = config.useEnhancedStreaming,
        embeddingModel = config.embeddingModel
    )
}
