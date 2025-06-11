package ai.kastrax.edutech.assistant

import ai.kastrax.edutech.llm.LLMService
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/**
 * 情绪检测服务
 * 分析学生消息中的情绪状态，为虚拟助手提供情绪感知能力
 */
class EmotionDetectionService(
    private val llmService: LLMService
) {
    private val emotionPatterns = initializeEmotionPatterns()
    private val emotionHistory = mutableMapOf<String, List<EmotionRecord>>()

    /**
     * 检测文本中的情绪
     */
    suspend fun detectEmotion(text: String, userId: String? = null): DetectedEmotion {
        // 1. 基于规则的初步检测
        val ruleBasedEmotion = detectEmotionByRules(text)
        
        // 2. 使用LLM进行深度情绪分析
        val llmBasedEmotion = detectEmotionByLLM(text)
        
        // 3. 结合历史情绪模式
        val historicalContext = userId?.let { getEmotionHistory(it) }
        
        // 4. 融合多种检测结果
        val finalEmotion = fuseEmotionResults(ruleBasedEmotion, llmBasedEmotion, historicalContext)
        
        // 5. 记录情绪历史
        userId?.let { recordEmotion(it, finalEmotion, text) }
        
        return finalEmotion
    }

    /**
     * 分析情绪变化趋势
     */
    fun analyzeEmotionTrend(userId: String, timeWindow: Int = 10): EmotionTrend {
        val recentEmotions = emotionHistory[userId]?.takeLast(timeWindow) ?: emptyList()
        
        if (recentEmotions.isEmpty()) {
            return EmotionTrend(
                trend = TrendDirection.STABLE,
                dominantEmotion = Emotion.NEUTRAL,
                volatility = 0.0f,
                confidence = 0.0f
            )
        }
        
        val emotionCounts = recentEmotions.groupBy { it.emotion.primary }.mapValues { it.value.size }
        val dominantEmotion = emotionCounts.maxByOrNull { it.value }?.key ?: Emotion.NEUTRAL
        
        val intensityTrend = calculateIntensityTrend(recentEmotions)
        val volatility = calculateEmotionVolatility(recentEmotions)
        
        return EmotionTrend(
            trend = intensityTrend,
            dominantEmotion = dominantEmotion,
            volatility = volatility,
            confidence = calculateTrendConfidence(recentEmotions)
        )
    }

    /**
     * 获取情绪建议
     */
    fun getEmotionBasedSuggestions(emotion: DetectedEmotion): List<EmotionSuggestion> {
        return when (emotion.primary) {
            Emotion.FRUSTRATED -> listOf(
                EmotionSuggestion(
                    type = SuggestionType.ENCOURAGEMENT,
                    message = "我理解这可能有些困难。让我们一步一步来解决这个问题。",
                    action = "提供更简单的解释"
                ),
                EmotionSuggestion(
                    type = SuggestionType.BREAK_SUGGESTION,
                    message = "也许我们可以先休息一下，然后从不同的角度来看这个问题。",
                    action = "建议短暂休息"
                )
            )
            
            Emotion.CONFUSED -> listOf(
                EmotionSuggestion(
                    type = SuggestionType.CLARIFICATION,
                    message = "让我用更简单的方式来解释这个概念。",
                    action = "提供更清晰的解释"
                ),
                EmotionSuggestion(
                    type = SuggestionType.EXAMPLE_REQUEST,
                    message = "我来给你举个具体的例子来说明这个概念。",
                    action = "提供具体例子"
                )
            )
            
            Emotion.BORED -> listOf(
                EmotionSuggestion(
                    type = SuggestionType.ENGAGEMENT_BOOST,
                    message = "让我们尝试一些更有趣的练习来学习这个概念！",
                    action = "增加互动性"
                ),
                EmotionSuggestion(
                    type = SuggestionType.GAMIFICATION,
                    message = "我们可以通过游戏的方式来学习这个内容。",
                    action = "引入游戏化元素"
                )
            )
            
            Emotion.EXCITED -> listOf(
                EmotionSuggestion(
                    type = SuggestionType.CHALLENGE_INCREASE,
                    message = "看起来你掌握得很好！我们来尝试一些更有挑战性的问题。",
                    action = "提高难度"
                ),
                EmotionSuggestion(
                    type = SuggestionType.EXPLORATION,
                    message = "你的热情很棒！让我们探索这个主题的更多方面。",
                    action = "扩展学习内容"
                )
            )
            
            Emotion.ANXIOUS -> listOf(
                EmotionSuggestion(
                    type = SuggestionType.REASSURANCE,
                    message = "不用担心，学习是一个过程。我们慢慢来，你一定可以掌握的。",
                    action = "提供安慰和支持"
                ),
                EmotionSuggestion(
                    type = SuggestionType.CONFIDENCE_BUILDING,
                    message = "让我们从你已经掌握的知识开始，逐步建立信心。",
                    action = "从简单内容开始"
                )
            )
            
            else -> listOf(
                EmotionSuggestion(
                    type = SuggestionType.GENERAL_SUPPORT,
                    message = "我在这里帮助你学习。有任何问题都可以问我。",
                    action = "提供一般性支持"
                )
            )
        }
    }

    // 私有方法
    private fun detectEmotionByRules(text: String): DetectedEmotion {
        val normalizedText = text.lowercase()
        var maxScore = 0.0f
        var detectedEmotion = Emotion.NEUTRAL
        
        for ((emotion, patterns) in emotionPatterns) {
            val score = patterns.sumOf { pattern ->
                when {
                    normalizedText.contains(pattern.keyword) -> pattern.weight
                    else -> 0.0
                }
            }.toFloat()
            
            if (score > maxScore) {
                maxScore = score
                detectedEmotion = emotion
            }
        }
        
        val confidence = minOf(maxScore / 10.0f, 1.0f) // 归一化到0-1
        val intensity = minOf(maxScore / 5.0f, 1.0f)
        
        return DetectedEmotion(
            primary = detectedEmotion,
            secondary = emptyList(),
            intensity = intensity,
            confidence = confidence
        )
    }

    private suspend fun detectEmotionByLLM(text: String): DetectedEmotion {
        val prompt = """
        请分析以下文本中的情绪状态：
        
        文本: "$text"
        
        请识别：
        1. 主要情绪（从以下选择：开心、沮丧、困惑、兴奋、无聊、焦虑、自信、好奇、不知所措、满意、失望、中性）
        2. 情绪强度（0.0-1.0）
        3. 置信度（0.0-1.0）
        
        请以JSON格式返回结果：
        {
            "primary_emotion": "情绪名称",
            "intensity": 0.0-1.0,
            "confidence": 0.0-1.0
        }
        """.trimIndent()
        
        try {
            val response = llmService.generate(prompt)
            // 简化处理，实际应该解析JSON
            return DetectedEmotion(
                primary = Emotion.NEUTRAL,
                secondary = emptyList(),
                intensity = 0.5f,
                confidence = 0.8f
            )
        } catch (e: Exception) {
            // 降级到规则检测
            return detectEmotionByRules(text)
        }
    }

    private fun fuseEmotionResults(
        ruleBasedEmotion: DetectedEmotion,
        llmBasedEmotion: DetectedEmotion,
        historicalContext: List<EmotionRecord>?
    ): DetectedEmotion {
        // 简单的融合策略：优先使用置信度更高的结果
        val finalEmotion = if (ruleBasedEmotion.confidence > llmBasedEmotion.confidence) {
            ruleBasedEmotion
        } else {
            llmBasedEmotion
        }
        
        // 考虑历史情绪模式进行调整
        val adjustedIntensity = historicalContext?.let { history ->
            val recentEmotions = history.takeLast(3)
            if (recentEmotions.any { it.emotion.primary == finalEmotion.primary }) {
                // 如果最近有相同情绪，增强强度
                minOf(finalEmotion.intensity * 1.2f, 1.0f)
            } else {
                finalEmotion.intensity
            }
        } ?: finalEmotion.intensity
        
        return finalEmotion.copy(intensity = adjustedIntensity)
    }

    private fun getEmotionHistory(userId: String): List<EmotionRecord> {
        return emotionHistory[userId] ?: emptyList()
    }

    private fun recordEmotion(userId: String, emotion: DetectedEmotion, text: String) {
        val record = EmotionRecord(
            emotion = emotion,
            text = text,
            timestamp = Clock.System.now()
        )
        
        val currentHistory = emotionHistory[userId] ?: emptyList()
        val updatedHistory = (currentHistory + record).takeLast(50) // 保留最近50条记录
        
        emotionHistory[userId] = updatedHistory
    }

    private fun calculateIntensityTrend(emotions: List<EmotionRecord>): TrendDirection {
        if (emotions.size < 2) return TrendDirection.STABLE
        
        val intensities = emotions.map { it.emotion.intensity }
        val firstHalf = intensities.take(intensities.size / 2).average()
        val secondHalf = intensities.drop(intensities.size / 2).average()
        
        return when {
            secondHalf > firstHalf + 0.1 -> TrendDirection.IMPROVING
            secondHalf < firstHalf - 0.1 -> TrendDirection.DECLINING
            else -> TrendDirection.STABLE
        }
    }

    private fun calculateEmotionVolatility(emotions: List<EmotionRecord>): Float {
        if (emotions.size < 2) return 0.0f
        
        val intensities = emotions.map { it.emotion.intensity }
        val mean = intensities.average().toFloat()
        val variance = intensities.map { (it - mean) * (it - mean) }.average().toFloat()
        
        return kotlin.math.sqrt(variance)
    }

    private fun calculateTrendConfidence(emotions: List<EmotionRecord>): Float {
        return when {
            emotions.size >= 10 -> 0.9f
            emotions.size >= 5 -> 0.7f
            emotions.size >= 3 -> 0.5f
            else -> 0.3f
        }
    }

    private fun initializeEmotionPatterns(): Map<Emotion, List<EmotionPattern>> {
        return mapOf(
            Emotion.FRUSTRATED to listOf(
                EmotionPattern("不懂", 3.0),
                EmotionPattern("太难", 3.0),
                EmotionPattern("搞不懂", 3.0),
                EmotionPattern("烦", 2.0),
                EmotionPattern("复杂", 2.0),
                EmotionPattern("困难", 2.0)
            ),
            Emotion.CONFUSED to listOf(
                EmotionPattern("什么意思", 3.0),
                EmotionPattern("不明白", 3.0),
                EmotionPattern("糊涂", 2.0),
                EmotionPattern("不清楚", 2.0),
                EmotionPattern("疑惑", 2.0)
            ),
            Emotion.EXCITED to listOf(
                EmotionPattern("太好了", 3.0),
                EmotionPattern("棒", 2.0),
                EmotionPattern("厉害", 2.0),
                EmotionPattern("有趣", 2.0),
                EmotionPattern("喜欢", 2.0)
            ),
            Emotion.BORED to listOf(
                EmotionPattern("无聊", 3.0),
                EmotionPattern("没意思", 2.0),
                EmotionPattern("枯燥", 2.0),
                EmotionPattern("乏味", 2.0)
            ),
            Emotion.ANXIOUS to listOf(
                EmotionPattern("担心", 3.0),
                EmotionPattern("紧张", 3.0),
                EmotionPattern("害怕", 2.0),
                EmotionPattern("不安", 2.0),
                EmotionPattern("忧虑", 2.0)
            ),
            Emotion.CONFIDENT to listOf(
                EmotionPattern("明白了", 3.0),
                EmotionPattern("懂了", 3.0),
                EmotionPattern("简单", 2.0),
                EmotionPattern("容易", 2.0),
                EmotionPattern("会了", 2.0)
            )
        )
    }
}

// 辅助数据类
@Serializable
data class EmotionRecord(
    val emotion: DetectedEmotion,
    val text: String,
    val timestamp: kotlinx.datetime.Instant
)

@Serializable
data class EmotionTrend(
    val trend: TrendDirection,
    val dominantEmotion: Emotion,
    val volatility: Float,
    val confidence: Float
)

@Serializable
data class EmotionSuggestion(
    val type: SuggestionType,
    val message: String,
    val action: String
)

@Serializable
data class EmotionPattern(
    val keyword: String,
    val weight: Double
)

enum class TrendDirection {
    IMPROVING, DECLINING, STABLE
}

enum class SuggestionType {
    ENCOURAGEMENT, CLARIFICATION, EXAMPLE_REQUEST, BREAK_SUGGESTION,
    ENGAGEMENT_BOOST, GAMIFICATION, CHALLENGE_INCREASE, EXPLORATION,
    REASSURANCE, CONFIDENCE_BUILDING, GENERAL_SUPPORT
}

// 添加中性情绪
val Emotion.Companion.NEUTRAL: Emotion get() = Emotion.SATISFIED // 使用现有的满意情绪作为中性
