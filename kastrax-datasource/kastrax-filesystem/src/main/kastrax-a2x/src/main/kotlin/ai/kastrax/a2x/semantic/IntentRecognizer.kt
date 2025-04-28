package ai.kastrax.a2x.semantic

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 意图识别器，负责识别消息的意图
 */
class IntentRecognizer {
    /**
     * 意图映射
     */
    private val intents = ConcurrentHashMap<String, Intent>()
    
    /**
     * 意图模式映射
     */
    private val intentPatterns = ConcurrentHashMap<String, List<IntentPattern>>()
    
    /**
     * 注册意图
     */
    fun registerIntent(intent: Intent, patterns: List<IntentPattern>) {
        intents[intent.id] = intent
        intentPatterns[intent.id] = patterns
    }
    
    /**
     * 注销意图
     */
    fun unregisterIntent(intentId: String) {
        intents.remove(intentId)
        intentPatterns.remove(intentId)
    }
    
    /**
     * 获取意图
     */
    fun getIntent(intentId: String): Intent? {
        return intents[intentId]
    }
    
    /**
     * 获取所有意图
     */
    fun getAllIntents(): List<Intent> {
        return intents.values.toList()
    }
    
    /**
     * 获取意图模式
     */
    fun getIntentPatterns(intentId: String): List<IntentPattern> {
        return intentPatterns[intentId] ?: emptyList()
    }
    
    /**
     * 识别意图
     */
    fun recognizeIntent(text: String): IntentRecognitionResult {
        val candidates = mutableListOf<IntentCandidate>()
        
        // 对每个意图进行匹配
        intents.forEach { (intentId, intent) ->
            val patterns = intentPatterns[intentId] ?: emptyList()
            
            // 对每个模式进行匹配
            patterns.forEach { pattern ->
                val confidence = matchPattern(text, pattern)
                if (confidence > 0.0) {
                    candidates.add(
                        IntentCandidate(
                            intent = intent,
                            confidence = confidence,
                            entities = extractEntities(text, pattern)
                        )
                    )
                }
            }
        }
        
        // 按置信度排序
        candidates.sortByDescending { it.confidence }
        
        // 返回结果
        return IntentRecognitionResult(
            text = text,
            candidates = candidates,
            topIntent = candidates.firstOrNull()?.intent,
            topConfidence = candidates.firstOrNull()?.confidence ?: 0.0,
            entities = candidates.firstOrNull()?.entities ?: emptyMap()
        )
    }
    
    /**
     * 匹配模式
     */
    private fun matchPattern(text: String, pattern: IntentPattern): Double {
        return when (pattern.type) {
            "exact" -> matchExactPattern(text, pattern)
            "regex" -> matchRegexPattern(text, pattern)
            "keyword" -> matchKeywordPattern(text, pattern)
            else -> 0.0
        }
    }
    
    /**
     * 匹配精确模式
     */
    private fun matchExactPattern(text: String, pattern: IntentPattern): Double {
        return if (text.equals(pattern.pattern, ignoreCase = true)) {
            1.0
        } else {
            0.0
        }
    }
    
    /**
     * 匹配正则表达式模式
     */
    private fun matchRegexPattern(text: String, pattern: IntentPattern): Double {
        val regex = pattern.pattern.toRegex(RegexOption.IGNORE_CASE)
        return if (regex.matches(text)) {
            1.0
        } else {
            0.0
        }
    }
    
    /**
     * 匹配关键词模式
     */
    private fun matchKeywordPattern(text: String, pattern: IntentPattern): Double {
        val keywords = pattern.pattern.split(",").map { it.trim() }
        val matchedKeywords = keywords.count { keyword ->
            text.contains(keyword, ignoreCase = true)
        }
        
        return if (matchedKeywords > 0) {
            matchedKeywords.toDouble() / keywords.size
        } else {
            0.0
        }
    }
    
    /**
     * 提取实体
     */
    private fun extractEntities(text: String, pattern: IntentPattern): Map<String, JsonElement> {
        val entities = mutableMapOf<String, JsonElement>()
        
        // 只有正则表达式模式支持实体提取
        if (pattern.type == "regex") {
            val regex = pattern.pattern.toRegex(RegexOption.IGNORE_CASE)
            val matchResult = regex.find(text)
            
            if (matchResult != null) {
                // 提取命名捕获组
                matchResult.groups.forEachIndexed { index, group ->
                    if (index > 0 && group != null) {
                        val name = regex.pattern.split("\\(\\?<").getOrNull(index)?.split(">")?.getOrNull(0)
                        if (name != null) {
                            entities[name] = JsonPrimitive(group.value)
                        }
                    }
                }
            }
        }
        
        return entities
    }
    
    /**
     * 创建意图
     */
    fun createIntent(name: String, description: String): Intent {
        val intent = Intent(
            id = "intent-${UUID.randomUUID()}",
            name = name,
            description = description
        )
        
        intents[intent.id] = intent
        intentPatterns[intent.id] = emptyList()
        
        return intent
    }
    
    /**
     * 添加意图模式
     */
    fun addIntentPattern(intentId: String, type: String, pattern: String): IntentPattern? {
        val intent = intents[intentId] ?: return null
        
        val intentPattern = IntentPattern(
            id = "pattern-${UUID.randomUUID()}",
            intentId = intentId,
            type = type,
            pattern = pattern
        )
        
        val patterns = intentPatterns[intentId]?.toMutableList() ?: mutableListOf()
        patterns.add(intentPattern)
        intentPatterns[intentId] = patterns
        
        return intentPattern
    }
    
    /**
     * 删除意图模式
     */
    fun removeIntentPattern(intentId: String, patternId: String): Boolean {
        val patterns = intentPatterns[intentId]?.toMutableList() ?: return false
        val removed = patterns.removeIf { it.id == patternId }
        if (removed) {
            intentPatterns[intentId] = patterns
        }
        return removed
    }
}

/**
 * 意图
 */
@Serializable
data class Intent(
    /**
     * 意图 ID
     */
    val id: String,
    
    /**
     * 意图名称
     */
    val name: String,
    
    /**
     * 意图描述
     */
    val description: String,
    
    /**
     * 意图元数据
     */
    val metadata: Map<String, String> = emptyMap()
)

/**
 * 意图模式
 */
@Serializable
data class IntentPattern(
    /**
     * 模式 ID
     */
    val id: String,
    
    /**
     * 意图 ID
     */
    val intentId: String,
    
    /**
     * 模式类型
     */
    val type: String,
    
    /**
     * 模式表达式
     */
    val pattern: String,
    
    /**
     * 模式元数据
     */
    val metadata: Map<String, String> = emptyMap()
)

/**
 * 意图候选
 */
@Serializable
data class IntentCandidate(
    /**
     * 意图
     */
    val intent: Intent,
    
    /**
     * 置信度
     */
    val confidence: Double,
    
    /**
     * 实体
     */
    val entities: Map<String, JsonElement>
)

/**
 * 意图识别结果
 */
@Serializable
data class IntentRecognitionResult(
    /**
     * 输入文本
     */
    val text: String,
    
    /**
     * 候选意图
     */
    val candidates: List<IntentCandidate>,
    
    /**
     * 最佳意图
     */
    val topIntent: Intent?,
    
    /**
     * 最佳置信度
     */
    val topConfidence: Double,
    
    /**
     * 实体
     */
    val entities: Map<String, JsonElement>
)
