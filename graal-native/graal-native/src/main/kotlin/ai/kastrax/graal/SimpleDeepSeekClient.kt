package ai.kastrax.graal

/**
 * 一个简化版本的 DeepSeekClient，不使用 Kotlin 序列化
 */
class SimpleDeepSeekClient(
    private val apiKey: String,
    private val baseUrl: String = "https://api.deepseek.com/v1"
) {
    /**
     * 创建聊天完成。
     *
     * @param model 模型名称
     * @param messages 消息列表
     * @return 聊天完成响应
     */
    fun createChatCompletion(model: String, messages: List<Map<String, String>>): String {
        // 在 Native Image 中，我们使用简化的实现
        return "This is a simplified implementation for Native Image"
    }

    /**
     * 创建嵌入。
     *
     * @param model 模型名称
     * @param input 输入文本
     * @return 嵌入响应
     */
    fun createEmbedding(model: String, input: String): String {
        // 在 Native Image 中，我们使用简化的实现
        return "This is a simplified implementation for Native Image"
    }
}
