package ai.kastrax.code.agent

/**
 * 代理上下文类
 *
 * 表示代理执行的上下文，包含输入和元数据
 */
data class AgentContext(
    /**
     * 输入文本
     */
    val input: String,
    
    /**
     * 元数据
     */
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * 转换为字符串
     */
    override fun toString(): String {
        val sb = StringBuilder()
        sb.appendLine("输入: $input")
        
        if (metadata.isNotEmpty()) {
            sb.appendLine("元数据:")
            metadata.forEach { (key, value) ->
                sb.appendLine("  $key: $value")
            }
        }
        
        return sb.toString()
    }
    
    /**
     * 处理上下文
     */
    fun process(processor: (String) -> String): String {
        return processor(toString())
    }
    
    /**
     * 获取内容
     */
    fun getContent(): String {
        return toString()
    }
}
