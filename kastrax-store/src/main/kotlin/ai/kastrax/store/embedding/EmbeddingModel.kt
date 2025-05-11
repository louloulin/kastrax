package ai.kastrax.store.embedding

/**
 * 嵌入模型接口，定义了嵌入模型的基本属性和方法。
 */
interface EmbeddingModel {
    /**
     * 模型名称
     */
    val name: String
    
    /**
     * 模型维度
     */
    val dimension: Int
    
    /**
     * 模型类型
     */
    val type: String
    
    /**
     * 模型版本
     */
    val version: String
    
    /**
     * 模型描述
     */
    val description: String
    
    /**
     * 模型是否支持批处理
     */
    val supportsBatch: Boolean
    
    /**
     * 最大批处理大小
     */
    val maxBatchSize: Int
    
    /**
     * 最大输入长度
     */
    val maxInputLength: Int
    
    /**
     * 模型是否支持多语言
     */
    val supportsMultilingual: Boolean
    
    /**
     * 支持的语言列表
     */
    val supportedLanguages: List<String>
    
    /**
     * 模型是否支持代码
     */
    val supportsCode: Boolean
    
    /**
     * 支持的编程语言列表
     */
    val supportedProgrammingLanguages: List<String>
}

/**
 * 基础嵌入模型实现
 */
data class BaseEmbeddingModel(
    override val name: String,
    override val dimension: Int,
    override val type: String = "text",
    override val version: String = "1.0",
    override val description: String = "",
    override val supportsBatch: Boolean = true,
    override val maxBatchSize: Int = 32,
    override val maxInputLength: Int = 8192,
    override val supportsMultilingual: Boolean = false,
    override val supportedLanguages: List<String> = listOf("en"),
    override val supportsCode: Boolean = false,
    override val supportedProgrammingLanguages: List<String> = emptyList()
) : EmbeddingModel

/**
 * 代码嵌入模型实现
 */
data class CodeEmbeddingModel(
    override val name: String,
    override val dimension: Int,
    override val type: String = "code",
    override val version: String = "1.0",
    override val description: String = "",
    override val supportsBatch: Boolean = true,
    override val maxBatchSize: Int = 32,
    override val maxInputLength: Int = 8192,
    override val supportsMultilingual: Boolean = false,
    override val supportedLanguages: List<String> = listOf("en"),
    override val supportsCode: Boolean = true,
    override val supportedProgrammingLanguages: List<String> = listOf(
        "java", "kotlin", "python", "javascript", "typescript", "go", "rust", "c", "cpp", "csharp"
    )
) : EmbeddingModel

/**
 * 注册模型
 * 
 * @param model 嵌入模型
 */
fun registerModel(model: EmbeddingModel) {
    EmbeddingModelRegistry.registerModel(model)
}

/**
 * 嵌入模型注册表
 */
object EmbeddingModelRegistry {
    private val models = mutableMapOf<String, EmbeddingModel>()
    
    /**
     * 注册模型
     * 
     * @param model 嵌入模型
     */
    fun registerModel(model: EmbeddingModel) {
        models[model.name] = model
    }
    
    /**
     * 获取模型
     * 
     * @param name 模型名称
     * @return 嵌入模型
     */
    fun getModel(name: String): EmbeddingModel? {
        return models[name]
    }
    
    /**
     * 获取所有模型
     * 
     * @return 所有嵌入模型
     */
    fun getAllModels(): List<EmbeddingModel> {
        return models.values.toList()
    }
    
    /**
     * 获取默认模型
     * 
     * @return 默认嵌入模型
     */
    fun getDefaultModel(): EmbeddingModel {
        return models.values.firstOrNull() ?: BaseEmbeddingModel(
            name = "default",
            dimension = 1536
        )
    }
}
