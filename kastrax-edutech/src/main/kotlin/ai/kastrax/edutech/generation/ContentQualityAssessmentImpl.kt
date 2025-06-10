package ai.kastrax.edutech.generation

import ai.kastrax.edutech.models.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.min

/**
 * 内容质量评估实现
 * 
 * 实现ed2.md第二阶段Week 7-8内容质量评估功能
 */
class ContentQualityAssessmentImpl : ContentQualityAssessment {
    private val assessmentHistory = mutableListOf<ContentQualityScore>()
    private val mutex = Mutex()
    
    override suspend fun assessContent(content: GeneratedContent): ContentQualityScore {
        return mutex.withLock {
            val accuracyScore = assessAccuracy(content)
            val clarityScore = assessClarity(content)
            val relevanceScore = assessRelevance(content)
            val engagementScore = assessEngagement(content)
            val educationalValueScore = assessEducationalValue(content)
            
            val overallScore = calculateOverallScore(
                accuracyScore,
                clarityScore,
                relevanceScore,
                engagementScore,
                educationalValueScore
            )
            
            val feedback = generateFeedback(
                overallScore,
                accuracyScore,
                clarityScore,
                relevanceScore,
                engagementScore,
                educationalValueScore
            )
            
            val suggestions = generateSuggestions(
                content,
                accuracyScore,
                clarityScore,
                relevanceScore,
                engagementScore,
                educationalValueScore
            )
            
            val qualityScore = ContentQualityScore(
                overallScore = overallScore,
                accuracyScore = accuracyScore,
                clarityScore = clarityScore,
                relevanceScore = relevanceScore,
                engagementScore = engagementScore,
                educationalValueScore = educationalValueScore,
                feedback = feedback,
                suggestions = suggestions
            )
            
            assessmentHistory.add(qualityScore)
            qualityScore
        }
    }
    
    override suspend fun assessBatch(contents: List<GeneratedContent>): List<ContentQualityScore> {
        return contents.map { assessContent(it) }
    }
    
    override suspend fun getQualityMetrics(): QualityMetrics {
        return mutex.withLock {
            if (assessmentHistory.isEmpty()) {
                return QualityMetrics(
                    averageQualityScore = 0.0,
                    totalAssessments = 0,
                    highQualityCount = 0,
                    lowQualityCount = 0,
                    improvementTrends = emptyList()
                )
            }
            
            val averageScore = assessmentHistory.map { it.overallScore }.average()
            val highQualityCount = assessmentHistory.count { it.overallScore >= 0.8 }
            val lowQualityCount = assessmentHistory.count { it.overallScore < 0.5 }
            
            val trends = calculateQualityTrends()
            
            QualityMetrics(
                averageQualityScore = averageScore,
                totalAssessments = assessmentHistory.size,
                highQualityCount = highQualityCount,
                lowQualityCount = lowQualityCount,
                improvementTrends = trends
            )
        }
    }
    
    // 私有评估方法
    
    private fun assessAccuracy(content: GeneratedContent): Double {
        var score = 0.8 // 基础分数
        
        // 检查内容长度是否合适
        val contentLength = content.content.length
        if (contentLength < 50) {
            score -= 0.3 // 内容太短
        } else if (contentLength > 5000) {
            score -= 0.1 // 内容可能过长
        }
        
        // 检查是否包含学习目标相关内容
        val objectiveKeywords = content.learningObjectives.flatMap { 
            it.split(" ", "，", "、").filter { word -> word.length > 1 }
        }
        val matchingKeywords = objectiveKeywords.count { keyword ->
            content.content.contains(keyword, ignoreCase = true)
        }
        
        if (objectiveKeywords.isNotEmpty()) {
            val keywordMatchRatio = matchingKeywords.toDouble() / objectiveKeywords.size
            score += keywordMatchRatio * 0.2
        }
        
        return score.coerceIn(0.0, 1.0)
    }
    
    private fun assessClarity(content: GeneratedContent): Double {
        var score = 0.7 // 基础分数
        
        val sentences = content.content.split("。", "！", "？").filter { it.trim().isNotEmpty() }
        
        // 检查句子长度分布
        val averageSentenceLength = sentences.map { it.length }.average()
        if (averageSentenceLength in 20.0..80.0) {
            score += 0.1 // 句子长度适中
        }
        
        // 检查段落结构
        val paragraphs = content.content.split("\n\n", "\n").filter { it.trim().isNotEmpty() }
        if (paragraphs.size >= 2) {
            score += 0.1 // 有段落结构
        }
        
        // 检查是否有标题或小标题
        if (content.content.contains("##") || content.content.contains("**") || 
            content.content.contains("一、") || content.content.contains("1.")) {
            score += 0.1 // 有结构化标题
        }
        
        return score.coerceIn(0.0, 1.0)
    }
    
    private fun assessRelevance(content: GeneratedContent): Double {
        var score = 0.7 // 基础分数
        
        // 检查主题相关性
        val topicKeywords = content.topic.value.split(" ", "，", "、")
        val topicMatchCount = topicKeywords.count { keyword ->
            content.content.contains(keyword, ignoreCase = true)
        }
        
        if (topicKeywords.isNotEmpty()) {
            val topicRelevance = topicMatchCount.toDouble() / topicKeywords.size
            score += topicRelevance * 0.2
        }
        
        // 检查学科相关性
        val subjectKeywords = getSubjectKeywords(content.subject)
        val subjectMatchCount = subjectKeywords.count { keyword ->
            content.content.contains(keyword, ignoreCase = true)
        }
        
        if (subjectKeywords.isNotEmpty()) {
            val subjectRelevance = subjectMatchCount.toDouble() / subjectKeywords.size
            score += subjectRelevance * 0.1
        }
        
        return score.coerceIn(0.0, 1.0)
    }
    
    private fun assessEngagement(content: GeneratedContent): Double {
        var score = 0.6 // 基础分数
        
        // 检查是否包含问题
        val questionMarkers = listOf("？", "?", "思考", "问题", "练习")
        val hasQuestions = questionMarkers.any { marker ->
            content.content.contains(marker)
        }
        if (hasQuestions) {
            score += 0.1
        }
        
        // 检查是否包含例子
        val exampleMarkers = listOf("例如", "比如", "举例", "案例", "实例")
        val hasExamples = exampleMarkers.any { marker ->
            content.content.contains(marker)
        }
        if (hasExamples) {
            score += 0.1
        }
        
        // 检查是否包含互动元素
        val interactiveMarkers = listOf("试试", "尝试", "练习", "动手", "实践")
        val hasInteractive = interactiveMarkers.any { marker ->
            content.content.contains(marker)
        }
        if (hasInteractive) {
            score += 0.1
        }
        
        // 检查语言风格
        val encouragingWords = listOf("很好", "棒", "优秀", "加油", "继续")
        val hasEncouragement = encouragingWords.any { word ->
            content.content.contains(word)
        }
        if (hasEncouragement) {
            score += 0.1
        }
        
        return score.coerceIn(0.0, 1.0)
    }
    
    private fun assessEducationalValue(content: GeneratedContent): Double {
        var score = 0.7 // 基础分数
        
        // 检查是否有明确的学习目标
        if (content.learningObjectives.isNotEmpty()) {
            score += 0.1
        }
        
        // 检查是否有总结
        val summaryMarkers = listOf("总结", "小结", "要点", "重点", "关键")
        val hasSummary = summaryMarkers.any { marker ->
            content.content.contains(marker)
        }
        if (hasSummary) {
            score += 0.1
        }
        
        // 检查是否有循序渐进的结构
        val structureMarkers = listOf("首先", "然后", "接下来", "最后", "第一", "第二")
        val hasStructure = structureMarkers.any { marker ->
            content.content.contains(marker)
        }
        if (hasStructure) {
            score += 0.1
        }
        
        return score.coerceIn(0.0, 1.0)
    }
    
    private fun calculateOverallScore(
        accuracy: Double,
        clarity: Double,
        relevance: Double,
        engagement: Double,
        educationalValue: Double
    ): Double {
        // 加权平均
        return (accuracy * 0.25 + 
                clarity * 0.2 + 
                relevance * 0.25 + 
                engagement * 0.15 + 
                educationalValue * 0.15).coerceIn(0.0, 1.0)
    }
    
    private fun generateFeedback(
        overall: Double,
        accuracy: Double,
        clarity: Double,
        relevance: Double,
        engagement: Double,
        educationalValue: Double
    ): String {
        return when {
            overall >= 0.9 -> "优秀的内容！各方面表现都很出色。"
            overall >= 0.8 -> "很好的内容，质量较高，有少量改进空间。"
            overall >= 0.7 -> "良好的内容，整体质量不错，建议在某些方面进行优化。"
            overall >= 0.6 -> "一般的内容，需要在多个方面进行改进。"
            else -> "内容质量较低，需要大幅改进。"
        }
    }
    
    private fun generateSuggestions(
        content: GeneratedContent,
        accuracy: Double,
        clarity: Double,
        relevance: Double,
        engagement: Double,
        educationalValue: Double
    ): List<String> {
        val suggestions = mutableListOf<String>()
        
        if (accuracy < 0.7) {
            suggestions.add("提高内容准确性，确保与学习目标一致")
        }
        
        if (clarity < 0.7) {
            suggestions.add("改善内容结构，使用更清晰的段落和标题")
        }
        
        if (relevance < 0.7) {
            suggestions.add("增强内容与主题的相关性")
        }
        
        if (engagement < 0.7) {
            suggestions.add("添加更多互动元素，如问题、例子和练习")
        }
        
        if (educationalValue < 0.7) {
            suggestions.add("明确学习目标，添加总结和要点回顾")
        }
        
        if (content.content.length < 100) {
            suggestions.add("增加内容长度，提供更详细的说明")
        }
        
        return suggestions
    }
    
    private fun getSubjectKeywords(subject: Subject): List<String> {
        return when (subject) {
            Subject.MATHEMATICS -> listOf("数学", "计算", "公式", "定理", "证明", "解题")
            Subject.PHYSICS -> listOf("物理", "力", "能量", "运动", "实验", "定律")
            Subject.CHEMISTRY -> listOf("化学", "反应", "元素", "分子", "实验", "方程式")
            Subject.BIOLOGY -> listOf("生物", "细胞", "基因", "进化", "生态", "器官")
            Subject.COMPUTER_SCIENCE -> listOf("计算机", "编程", "算法", "数据", "软件", "代码")
            Subject.LANGUAGE_ARTS -> listOf("语言", "文学", "写作", "阅读", "语法", "修辞")
            Subject.HISTORY -> listOf("历史", "事件", "人物", "时代", "文化", "社会")
            Subject.GEOGRAPHY -> listOf("地理", "地图", "气候", "地形", "国家", "城市")
            Subject.ART -> listOf("艺术", "绘画", "色彩", "设计", "创作", "美学")
            Subject.MUSIC -> listOf("音乐", "节奏", "旋律", "乐器", "作曲", "演奏")
            else -> emptyList()
        }
    }
    
    private fun calculateQualityTrends(): List<QualityTrend> {
        val groupedByDate = assessmentHistory.groupBy { assessment ->
            assessment.assessedAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
        }
        
        return groupedByDate.map { (date, assessments) ->
            QualityTrend(
                date = date,
                averageScore = assessments.map { it.overallScore }.average(),
                assessmentCount = assessments.size
            )
        }.sortedBy { it.date }
    }
}
