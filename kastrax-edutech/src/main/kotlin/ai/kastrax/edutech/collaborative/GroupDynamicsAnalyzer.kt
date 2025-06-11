package ai.kastrax.edutech.collaborative

import ai.kastrax.edutech.models.*
import ai.kastrax.edutech.collaborative.*
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.math.*

/**
 * Week 23-24: 小组动态分析器
 * 
 * 功能：
 * - 分析小组凝聚力
 * - 评估沟通效果
 * - 检测冲突水平
 * - 分析领导力分布
 * - 识别协作模式
 */
class GroupDynamicsAnalyzer {
    
    private val networkAnalyzer = SocialNetworkAnalyzer()
    private val communicationAnalyzer = CommunicationAnalyzer()
    private val conflictDetector = ConflictDetector()
    private val leadershipAnalyzer = LeadershipAnalyzer()
    private val patternRecognizer = CollaborationPatternRecognizer()
    
    /**
     * 分析小组动态
     */
    suspend fun analyze(
        sessionId: SessionId,
        participants: List<SessionParticipant>,
        interactions: List<CollaborativeInteraction>,
        activities: List<CollaborativeActivity>
    ): GroupDynamicsAnalysis {
        
        // 1. 计算小组凝聚力
        val cohesionScore = calculateCohesionScore(participants, interactions)
        
        // 2. 评估沟通效果
        val communicationEffectiveness = evaluateCommunicationEffectiveness(interactions)
        
        // 3. 检测冲突水平
        val conflictLevel = detectConflictLevel(interactions)
        
        // 4. 分析领导力分布
        val leadershipDistribution = analyzeLeadershipDistribution(participants, interactions)
        
        // 5. 识别协作模式
        val collaborationPatterns = identifyCollaborationPatterns(interactions, activities)
        
        return GroupDynamicsAnalysis(
            cohesionScore = cohesionScore,
            communicationEffectiveness = communicationEffectiveness,
            conflictLevel = conflictLevel,
            leadershipDistribution = leadershipDistribution,
            collaborationPatterns = collaborationPatterns
        )
    }
    
    /**
     * 计算小组凝聚力分数
     */
    private suspend fun calculateCohesionScore(
        participants: List<SessionParticipant>,
        interactions: List<CollaborativeInteraction>
    ): Double {
        if (participants.size < 2) return 1.0
        
        // 1. 交互密度分析
        val interactionDensity = calculateInteractionDensity(participants, interactions)
        
        // 2. 相互支持度分析
        val mutualSupport = calculateMutualSupport(interactions)
        
        // 3. 共同目标导向分析
        val goalAlignment = calculateGoalAlignment(interactions)
        
        // 4. 参与平衡度分析
        val participationBalance = calculateParticipationBalance(participants, interactions)
        
        // 加权计算凝聚力分数
        return (interactionDensity * 0.3 + 
                mutualSupport * 0.3 + 
                goalAlignment * 0.2 + 
                participationBalance * 0.2)
    }
    
    /**
     * 评估沟通效果
     */
    private suspend fun evaluateCommunicationEffectiveness(
        interactions: List<CollaborativeInteraction>
    ): Double {
        if (interactions.isEmpty()) return 0.0
        
        // 1. 响应及时性
        val responseTimeliness = calculateResponseTimeliness(interactions)
        
        // 2. 信息清晰度
        val informationClarity = calculateInformationClarity(interactions)
        
        // 3. 双向交流程度
        val bidirectionalCommunication = calculateBidirectionalCommunication(interactions)
        
        // 4. 建设性反馈比例
        val constructiveFeedback = calculateConstructiveFeedback(interactions)
        
        return (responseTimeliness * 0.25 + 
                informationClarity * 0.25 + 
                bidirectionalCommunication * 0.25 + 
                constructiveFeedback * 0.25)
    }
    
    /**
     * 检测冲突水平
     */
    private suspend fun detectConflictLevel(interactions: List<CollaborativeInteraction>): ConflictLevel {
        val conflictIndicators = conflictDetector.detectConflictIndicators(interactions)
        
        val conflictScore = conflictIndicators.sumOf { it.severity } / conflictIndicators.size.coerceAtLeast(1)
        
        return when {
            conflictScore >= 0.8 -> ConflictLevel.SEVERE
            conflictScore >= 0.6 -> ConflictLevel.HIGH
            conflictScore >= 0.4 -> ConflictLevel.MODERATE
            conflictScore >= 0.2 -> ConflictLevel.LOW
            else -> ConflictLevel.NONE
        }
    }
    
    /**
     * 分析领导力分布
     */
    private suspend fun analyzeLeadershipDistribution(
        participants: List<SessionParticipant>,
        interactions: List<CollaborativeInteraction>
    ): Map<StudentId, Double> {
        return participants.associate { participant ->
            val leadershipScore = leadershipAnalyzer.calculateLeadershipScore(
                participant.studentId, interactions
            )
            participant.studentId to leadershipScore
        }
    }
    
    /**
     * 识别协作模式
     */
    private suspend fun identifyCollaborationPatterns(
        interactions: List<CollaborativeInteraction>,
        activities: List<CollaborativeActivity>
    ): List<CollaborationPattern> {
        // 需要将activities转换为participants
        val participants = activities.map { activity ->
            SessionParticipant(
                studentId = activity.participantId,
                role = ParticipantRole.MEMBER,
                joinTime = activity.timestamp,
                contributionScore = 10.0,
                engagementLevel = EngagementLevel.MEDIUM
            )
        }.distinctBy { it.studentId }

        return patternRecognizer.recognizePatterns(interactions, participants)
    }
    
    // ==================== 私有计算方法 ====================
    
    private fun calculateInteractionDensity(
        participants: List<SessionParticipant>,
        interactions: List<CollaborativeInteraction>
    ): Double {
        val participantCount = participants.size
        if (participantCount < 2) return 1.0
        
        // 计算理论最大交互数（每对参与者之间的交互）
        val maxPossibleInteractions = participantCount * (participantCount - 1)
        
        // 计算实际交互对数
        val actualInteractionPairs = interactions.flatMap { interaction ->
            interaction.responses.map { response ->
                Pair(interaction.participantId, response.participantId)
            }
        }.distinct().size
        
        return if (maxPossibleInteractions > 0) {
            actualInteractionPairs.toDouble() / maxPossibleInteractions
        } else 0.0
    }
    
    private fun calculateMutualSupport(interactions: List<CollaborativeInteraction>): Double {
        val supportiveInteractions = interactions.count { interaction ->
            interaction.responses.any { response ->
                response.type in setOf(ResponseType.HELPFUL, ResponseType.LIKE, ResponseType.AGREE)
            } || interaction.type == InteractionType.SHARE
        }
        
        return if (interactions.isNotEmpty()) {
            supportiveInteractions.toDouble() / interactions.size
        } else 0.0
    }
    
    private fun calculateGoalAlignment(interactions: List<CollaborativeInteraction>): Double {
        // 分析交互内容中与学习目标相关的关键词
        val goalRelatedKeywords = setOf("目标", "学习", "完成", "任务", "项目", "合作", "一起")
        
        val goalAlignedInteractions = interactions.count { interaction ->
            val content = interaction.content.text?.lowercase() ?: ""
            goalRelatedKeywords.any { keyword -> content.contains(keyword) }
        }
        
        return if (interactions.isNotEmpty()) {
            goalAlignedInteractions.toDouble() / interactions.size
        } else 0.0
    }
    
    private fun calculateParticipationBalance(
        participants: List<SessionParticipant>,
        interactions: List<CollaborativeInteraction>
    ): Double {
        if (participants.isEmpty()) return 1.0
        
        val participationCounts = participants.associate { participant ->
            participant.studentId to interactions.count { it.participantId == participant.studentId }
        }
        
        val counts = participationCounts.values
        if (counts.isEmpty()) return 1.0
        
        val mean = counts.average()
        val variance = counts.map { (it - mean).pow(2) }.average()
        val standardDeviation = sqrt(variance)
        
        // 标准差越小，参与越平衡，分数越高
        val coefficientOfVariation = if (mean > 0) standardDeviation / mean else 1.0
        return maxOf(0.0, 1.0 - coefficientOfVariation)
    }
    
    private fun calculateResponseTimeliness(interactions: List<CollaborativeInteraction>): Double {
        val questionAnswerPairs = findQuestionAnswerPairs(interactions)
        
        if (questionAnswerPairs.isEmpty()) return 0.5 // 默认中等分数
        
        val avgResponseTime = questionAnswerPairs.map { 
            it.responseTime.inWholeMinutes 
        }.average()
        
        return when {
            avgResponseTime <= 5 -> 1.0
            avgResponseTime <= 15 -> 0.8
            avgResponseTime <= 30 -> 0.6
            avgResponseTime <= 60 -> 0.4
            else -> 0.2
        }
    }
    
    private fun calculateInformationClarity(interactions: List<CollaborativeInteraction>): Double {
        val clarityScores = interactions.map { interaction ->
            val content = interaction.content.text ?: ""
            var score = 0.5 // 基础分数
            
            // 检查结构化内容
            if (content.contains("1.") || content.contains("-") || content.contains("•")) score += 0.2
            
            // 检查问题明确性
            if (content.contains("?") && content.length > 10) score += 0.15
            
            // 检查例子和解释
            if (content.contains("例如") || content.contains("比如") || content.contains("就是")) score += 0.15
            
            minOf(1.0, score)
        }
        
        return if (clarityScores.isNotEmpty()) clarityScores.average() else 0.0
    }
    
    private fun calculateBidirectionalCommunication(interactions: List<CollaborativeInteraction>): Double {
        val participantInteractions = interactions.groupBy { it.participantId }
        val participants = participantInteractions.keys
        
        if (participants.size < 2) return 0.0
        
        var bidirectionalPairs = 0
        var totalPairs = 0
        
        for (participant1 in participants) {
            for (participant2 in participants) {
                if (participant1 != participant2) {
                    totalPairs++
                    
                    val hasInteraction1to2 = interactions.any { interaction ->
                        interaction.participantId == participant1 &&
                        interaction.responses.any { it.participantId == participant2 }
                    }
                    
                    val hasInteraction2to1 = interactions.any { interaction ->
                        interaction.participantId == participant2 &&
                        interaction.responses.any { it.participantId == participant1 }
                    }
                    
                    if (hasInteraction1to2 && hasInteraction2to1) {
                        bidirectionalPairs++
                    }
                }
            }
        }
        
        return if (totalPairs > 0) bidirectionalPairs.toDouble() / totalPairs else 0.0
    }
    
    private fun calculateConstructiveFeedback(interactions: List<CollaborativeInteraction>): Double {
        val feedbackInteractions = interactions.filter { 
            it.type == InteractionType.COMMENT || it.responses.isNotEmpty() 
        }
        
        if (feedbackInteractions.isEmpty()) return 0.0
        
        val constructiveFeedback = feedbackInteractions.count { interaction ->
            val content = interaction.content.text?.lowercase() ?: ""
            val constructiveKeywords = setOf("建议", "可以", "试试", "改进", "更好", "帮助")
            constructiveKeywords.any { keyword -> content.contains(keyword) }
        }
        
        return constructiveFeedback.toDouble() / feedbackInteractions.size
    }
    
    private fun findQuestionAnswerPairs(interactions: List<CollaborativeInteraction>): List<QuestionAnswerPair> {
        val pairs = mutableListOf<QuestionAnswerPair>()
        val questions = interactions.filter { it.type == InteractionType.QUESTION }
        val answers = interactions.filter { it.type == InteractionType.ANSWER }
        
        for (question in questions) {
            val relatedAnswers = answers.filter { answer ->
                answer.timestamp > question.timestamp &&
                answer.timestamp <= question.timestamp + 30.minutes
            }
            
            for (answer in relatedAnswers) {
                pairs.add(
                    QuestionAnswerPair(
                        questioner = question.participantId,
                        answerer = answer.participantId,
                        questionTime = question.timestamp,
                        answerTime = answer.timestamp,
                        responseTime = answer.timestamp - question.timestamp
                    )
                )
            }
        }
        
        return pairs
    }
}

/**
 * 社交网络分析器
 */
class SocialNetworkAnalyzer {
    
    /**
     * 分析社交网络结构
     */
    fun analyzeSocialNetwork(interactions: List<CollaborativeInteraction>): SocialNetworkAnalysis {
        val nodes = extractNodes(interactions)
        val edges = extractEdges(interactions)
        
        return SocialNetworkAnalysis(
            nodes = nodes,
            edges = edges,
            density = calculateNetworkDensity(nodes, edges),
            centrality = calculateCentralityMeasures(nodes, edges),
            clusters = identifyClusters(nodes, edges)
        )
    }
    
    private fun extractNodes(interactions: List<CollaborativeInteraction>): List<NetworkNode> {
        return interactions.map { it.participantId }.distinct().map { studentId ->
            NetworkNode(
                id = studentId,
                degree = calculateNodeDegree(studentId, interactions),
                weight = calculateNodeWeight(studentId, interactions)
            )
        }
    }
    
    private fun extractEdges(interactions: List<CollaborativeInteraction>): List<NetworkEdge> {
        val edges = mutableListOf<NetworkEdge>()
        
        for (interaction in interactions) {
            for (response in interaction.responses) {
                edges.add(
                    NetworkEdge(
                        from = interaction.participantId,
                        to = response.participantId,
                        weight = calculateEdgeWeight(interaction, response),
                        type = mapInteractionToEdgeType(interaction.type)
                    )
                )
            }
        }
        
        return edges
    }
    
    private fun calculateNodeDegree(studentId: StudentId, interactions: List<CollaborativeInteraction>): Int {
        val outgoing = interactions.count { it.participantId == studentId }
        val incoming = interactions.sumOf { interaction ->
            interaction.responses.count { it.participantId == studentId }
        }
        return outgoing + incoming
    }
    
    private fun calculateNodeWeight(studentId: StudentId, interactions: List<CollaborativeInteraction>): Double {
        val studentInteractions = interactions.filter { 
            it.participantId == studentId || 
            it.responses.any { response -> response.participantId == studentId }
        }
        
        return studentInteractions.size.toDouble() / interactions.size.coerceAtLeast(1)
    }
    
    private fun calculateEdgeWeight(interaction: CollaborativeInteraction, response: InteractionResponse): Double {
        return when (response.type) {
            ResponseType.HELPFUL -> 1.0
            ResponseType.LIKE, ResponseType.AGREE -> 0.8
            ResponseType.SUGGESTION -> 0.9
            ResponseType.QUESTION -> 0.7
            else -> 0.5
        }
    }
    
    private fun mapInteractionToEdgeType(interactionType: InteractionType): String {
        return when (interactionType) {
            InteractionType.QUESTION -> "question"
            InteractionType.ANSWER -> "answer"
            InteractionType.SHARE -> "share"
            InteractionType.COMMENT -> "comment"
            else -> "general"
        }
    }
    
    private fun calculateNetworkDensity(nodes: List<NetworkNode>, edges: List<NetworkEdge>): Double {
        val nodeCount = nodes.size
        if (nodeCount < 2) return 0.0
        
        val maxPossibleEdges = nodeCount * (nodeCount - 1)
        return edges.size.toDouble() / maxPossibleEdges
    }
    
    private fun calculateCentralityMeasures(nodes: List<NetworkNode>, edges: List<NetworkEdge>): Map<StudentId, CentralityMeasures> {
        return nodes.associate { node ->
            node.id to CentralityMeasures(
                degree = node.degree.toDouble(),
                betweenness = calculateBetweennessCentrality(node.id, nodes, edges),
                closeness = calculateClosenessCentrality(node.id, nodes, edges),
                eigenvector = calculateEigenvectorCentrality(node.id, nodes, edges)
            )
        }
    }
    
    private fun calculateBetweennessCentrality(nodeId: StudentId, nodes: List<NetworkNode>, edges: List<NetworkEdge>): Double {
        // 简化实现：基于节点的连接数
        val nodeConnections = edges.count { it.from == nodeId || it.to == nodeId }
        return nodeConnections.toDouble() / edges.size.coerceAtLeast(1)
    }
    
    private fun calculateClosenessCentrality(nodeId: StudentId, nodes: List<NetworkNode>, edges: List<NetworkEdge>): Double {
        // 简化实现：基于直接连接的节点数
        val directConnections = edges.filter { it.from == nodeId || it.to == nodeId }
            .flatMap { listOf(it.from, it.to) }
            .filter { it != nodeId }
            .distinct()
            .size
        
        return directConnections.toDouble() / (nodes.size - 1).coerceAtLeast(1)
    }
    
    private fun calculateEigenvectorCentrality(nodeId: StudentId, nodes: List<NetworkNode>, edges: List<NetworkEdge>): Double {
        // 简化实现：基于连接节点的重要性
        val connectedNodes = edges.filter { it.from == nodeId || it.to == nodeId }
            .flatMap { listOf(it.from, it.to) }
            .filter { it != nodeId }
            .distinct()
        
        val connectedNodesImportance = connectedNodes.sumOf { connectedNodeId ->
            nodes.find { it.id == connectedNodeId }?.weight ?: 0.0
        }
        
        return connectedNodesImportance / connectedNodes.size.coerceAtLeast(1)
    }
    
    private fun identifyClusters(nodes: List<NetworkNode>, edges: List<NetworkEdge>): List<NetworkCluster> {
        // 简化实现：基于连接密度识别集群
        val clusters = mutableListOf<NetworkCluster>()
        val processedNodes = mutableSetOf<StudentId>()
        
        for (node in nodes) {
            if (node.id in processedNodes) continue
            
            val cluster = findConnectedComponent(node.id, nodes, edges, processedNodes)
            if (cluster.size > 1) {
                clusters.add(
                    NetworkCluster(
                        id = "cluster_${clusters.size + 1}",
                        members = cluster,
                        cohesion = calculateClusterCohesion(cluster, edges)
                    )
                )
            }
            processedNodes.addAll(cluster)
        }
        
        return clusters
    }
    
    private fun findConnectedComponent(
        startNode: StudentId,
        nodes: List<NetworkNode>,
        edges: List<NetworkEdge>,
        processedNodes: MutableSet<StudentId>
    ): List<StudentId> {
        val component = mutableListOf<StudentId>()
        val toProcess = mutableListOf(startNode)
        
        while (toProcess.isNotEmpty()) {
            val currentNode = toProcess.removeAt(0)
            if (currentNode in processedNodes) continue
            
            component.add(currentNode)
            processedNodes.add(currentNode)
            
            val neighbors = edges.filter { 
                (it.from == currentNode || it.to == currentNode) && it.weight > 0.5 
            }.flatMap { 
                listOf(it.from, it.to) 
            }.filter { 
                it != currentNode && it !in processedNodes 
            }
            
            toProcess.addAll(neighbors)
        }
        
        return component
    }
    
    private fun calculateClusterCohesion(members: List<StudentId>, edges: List<NetworkEdge>): Double {
        if (members.size < 2) return 1.0
        
        val internalEdges = edges.count { edge ->
            edge.from in members && edge.to in members
        }
        
        val maxPossibleEdges = members.size * (members.size - 1)
        return if (maxPossibleEdges > 0) internalEdges.toDouble() / maxPossibleEdges else 0.0
    }
}

// ==================== 数据模型 ====================

@Serializable
data class SocialNetworkAnalysis(
    val nodes: List<NetworkNode>,
    val edges: List<NetworkEdge>,
    val density: Double,
    val centrality: Map<StudentId, CentralityMeasures>,
    val clusters: List<NetworkCluster>
)

@Serializable
data class NetworkNode(
    val id: StudentId,
    val degree: Int,
    val weight: Double
)

@Serializable
data class NetworkEdge(
    val from: StudentId,
    val to: StudentId,
    val weight: Double,
    val type: String
)

@Serializable
data class CentralityMeasures(
    val degree: Double,
    val betweenness: Double,
    val closeness: Double,
    val eigenvector: Double
)

@Serializable
data class NetworkCluster(
    val id: String,
    val members: List<StudentId>,
    val cohesion: Double
)

/**
 * 沟通分析器
 */
class CommunicationAnalyzer {

    /**
     * 分析沟通模式
     */
    fun analyzeCommunicationPatterns(interactions: List<CollaborativeInteraction>): CommunicationPatternAnalysis {
        return CommunicationPatternAnalysis(
            messageFlow = analyzeMessageFlow(interactions),
            responsePatterns = analyzeResponsePatterns(interactions),
            communicationStyle = analyzeCommunicationStyle(interactions),
            topicProgression = analyzeTopicProgression(interactions)
        )
    }

    private fun analyzeMessageFlow(interactions: List<CollaborativeInteraction>): MessageFlowAnalysis {
        val timeWindows = groupInteractionsByTimeWindow(interactions, 5) // 5分钟窗口

        return MessageFlowAnalysis(
            peakActivityPeriods = identifyPeakPeriods(timeWindows),
            averageResponseTime = calculateAverageResponseTime(interactions),
            messageDistribution = calculateMessageDistribution(interactions),
            conversationThreads = identifyConversationThreads(interactions)
        )
    }

    private fun analyzeResponsePatterns(interactions: List<CollaborativeInteraction>): ResponsePatternAnalysis {
        val responseTypes = interactions.flatMap { it.responses }.groupBy { it.type }

        return ResponsePatternAnalysis(
            responseTypeDistribution = responseTypes.mapValues { it.value.size },
            averageResponsesPerMessage = interactions.map { it.responses.size }.average(),
            mostResponsiveParticipants = identifyMostResponsiveParticipants(interactions),
            responseQuality = assessResponseQuality(interactions)
        )
    }

    private fun analyzeCommunicationStyle(interactions: List<CollaborativeInteraction>): CommunicationStyleAnalysis {
        return CommunicationStyleAnalysis(
            formalityLevel = assessFormalityLevel(interactions),
            emotionalTone = assessEmotionalTone(interactions),
            questionToStatementRatio = calculateQuestionToStatementRatio(interactions),
            collaborativeLanguageUsage = assessCollaborativeLanguage(interactions)
        )
    }

    private fun analyzeTopicProgression(interactions: List<CollaborativeInteraction>): TopicProgressionAnalysis {
        return TopicProgressionAnalysis(
            topicShifts = identifyTopicShifts(interactions),
            focusConsistency = calculateFocusConsistency(interactions),
            knowledgeBuildingSequences = identifyKnowledgeBuildingSequences(interactions)
        )
    }

    // 实现细节方法...
    private fun groupInteractionsByTimeWindow(interactions: List<CollaborativeInteraction>, windowMinutes: Int): Map<Int, List<CollaborativeInteraction>> {
        return interactions.groupBy { interaction ->
            (interaction.timestamp.epochSeconds / (windowMinutes * 60)).toInt()
        }
    }

    private fun identifyPeakPeriods(timeWindows: Map<Int, List<CollaborativeInteraction>>): List<PeakActivityPeriod> {
        val avgActivity = timeWindows.values.map { it.size }.average()

        return timeWindows.filter { it.value.size > avgActivity * 1.5 }.map { (window, interactions) ->
            PeakActivityPeriod(
                startTime = interactions.minOf { it.timestamp },
                endTime = interactions.maxOf { it.timestamp },
                activityLevel = interactions.size.toDouble() / avgActivity,
                participants = interactions.map { it.participantId }.distinct()
            )
        }
    }

    private fun calculateAverageResponseTime(interactions: List<CollaborativeInteraction>): Duration {
        val responseTimes = mutableListOf<Duration>()

        for (interaction in interactions) {
            for (response in interaction.responses) {
                responseTimes.add(response.timestamp - interaction.timestamp)
            }
        }

        return if (responseTimes.isNotEmpty()) {
            Duration.parse("PT${responseTimes.map { it.inWholeMinutes }.average().toLong()}M")
        } else Duration.ZERO
    }

    private fun calculateMessageDistribution(interactions: List<CollaborativeInteraction>): Map<StudentId, Int> {
        return interactions.groupBy { it.participantId }.mapValues { it.value.size }
    }

    private fun identifyConversationThreads(interactions: List<CollaborativeInteraction>): List<ConversationThread> {
        // 简化实现：基于时间邻近性和参与者重叠识别对话线程
        val threads = mutableListOf<ConversationThread>()
        val processedInteractions = mutableSetOf<String>()

        for (interaction in interactions) {
            if (interaction.id in processedInteractions) continue

            val thread = buildConversationThread(interaction, interactions, processedInteractions)
            if (thread.interactions.size > 1) {
                threads.add(thread)
            }
        }

        return threads
    }

    private fun buildConversationThread(
        startInteraction: CollaborativeInteraction,
        allInteractions: List<CollaborativeInteraction>,
        processedInteractions: MutableSet<String>
    ): ConversationThread {
        val threadInteractions = mutableListOf(startInteraction)
        processedInteractions.add(startInteraction.id)

        val relatedInteractions = allInteractions.filter { interaction ->
            interaction.id != startInteraction.id &&
            interaction.id !in processedInteractions &&
            (interaction.timestamp - startInteraction.timestamp).inWholeMinutes <= 10 &&
            (interaction.participantId == startInteraction.participantId ||
             interaction.responses.any { it.participantId == startInteraction.participantId } ||
             startInteraction.responses.any { it.participantId == interaction.participantId })
        }

        threadInteractions.addAll(relatedInteractions)
        processedInteractions.addAll(relatedInteractions.map { it.id })

        return ConversationThread(
            id = "thread_${startInteraction.id}",
            interactions = threadInteractions,
            participants = threadInteractions.map { it.participantId }.distinct(),
            startTime = threadInteractions.minOf { it.timestamp },
            endTime = threadInteractions.maxOf { it.timestamp },
            topic = extractThreadTopic(threadInteractions)
        )
    }

    private fun extractThreadTopic(interactions: List<CollaborativeInteraction>): String {
        // 简化实现：使用第一个交互的内容作为主题
        return interactions.firstOrNull()?.content?.text?.take(50) ?: "未知主题"
    }

    private fun identifyMostResponsiveParticipants(interactions: List<CollaborativeInteraction>): List<StudentId> {
        val responseCounts = mutableMapOf<StudentId, Int>()

        for (interaction in interactions) {
            for (response in interaction.responses) {
                responseCounts[response.participantId] = responseCounts.getOrDefault(response.participantId, 0) + 1
            }
        }

        val avgResponses = responseCounts.values.average()
        return responseCounts.filter { it.value > avgResponses }.keys.toList()
    }

    private fun assessResponseQuality(interactions: List<CollaborativeInteraction>): Double {
        val allResponses = interactions.flatMap { it.responses }
        if (allResponses.isEmpty()) return 0.0

        val qualityScores = allResponses.map { response ->
            when (response.type) {
                ResponseType.HELPFUL -> 1.0
                ResponseType.SUGGESTION -> 0.9
                ResponseType.QUESTION -> 0.8
                ResponseType.AGREE -> 0.7
                ResponseType.LIKE -> 0.6
                else -> 0.4
            }
        }

        return qualityScores.average()
    }

    private fun assessFormalityLevel(interactions: List<CollaborativeInteraction>): FormalityLevel {
        val formalIndicators = listOf("您", "请", "谢谢", "不好意思", "麻烦")
        val informalIndicators = listOf("哈哈", "嗯", "哦", "呀", "吧")

        var formalCount = 0
        var informalCount = 0

        for (interaction in interactions) {
            val content = interaction.content.text?.lowercase() ?: ""
            formalCount += formalIndicators.count { content.contains(it) }
            informalCount += informalIndicators.count { content.contains(it) }
        }

        val totalIndicators = formalCount + informalCount
        if (totalIndicators == 0) return FormalityLevel.SEMI_FORMAL

        val formalRatio = formalCount.toDouble() / totalIndicators

        return when {
            formalRatio >= 0.8 -> FormalityLevel.VERY_FORMAL
            formalRatio >= 0.6 -> FormalityLevel.FORMAL
            formalRatio >= 0.4 -> FormalityLevel.SEMI_FORMAL
            formalRatio >= 0.2 -> FormalityLevel.INFORMAL
            else -> FormalityLevel.VERY_INFORMAL
        }
    }

    private fun assessEmotionalTone(interactions: List<CollaborativeInteraction>): EmotionalTone {
        val positiveIndicators = listOf("好", "棒", "赞", "谢谢", "喜欢", "开心")
        val negativeIndicators = listOf("不", "难", "问题", "困难", "担心", "不好")

        var positiveCount = 0
        var negativeCount = 0

        for (interaction in interactions) {
            val content = interaction.content.text?.lowercase() ?: ""
            positiveCount += positiveIndicators.count { content.contains(it) }
            negativeCount += negativeIndicators.count { content.contains(it) }
        }

        val totalEmotional = positiveCount + negativeCount
        if (totalEmotional == 0) return EmotionalTone.NEUTRAL

        val positiveRatio = positiveCount.toDouble() / totalEmotional

        return when {
            positiveRatio >= 0.7 -> EmotionalTone.VERY_POSITIVE
            positiveRatio >= 0.55 -> EmotionalTone.POSITIVE
            positiveRatio >= 0.45 -> EmotionalTone.NEUTRAL
            positiveRatio >= 0.3 -> EmotionalTone.NEGATIVE
            else -> EmotionalTone.VERY_NEGATIVE
        }
    }

    private fun calculateQuestionToStatementRatio(interactions: List<CollaborativeInteraction>): Double {
        val questions = interactions.count { it.type == InteractionType.QUESTION }
        val statements = interactions.count { it.type == InteractionType.MESSAGE }

        return if (statements > 0) questions.toDouble() / statements else 0.0
    }

    private fun assessCollaborativeLanguage(interactions: List<CollaborativeInteraction>): Double {
        val collaborativeKeywords = listOf("我们", "一起", "共同", "合作", "帮助", "支持", "分享")

        val collaborativeCount = interactions.sumOf { interaction ->
            val content = interaction.content.text?.lowercase() ?: ""
            collaborativeKeywords.count { content.contains(it) }
        }

        return if (interactions.isNotEmpty()) {
            collaborativeCount.toDouble() / interactions.size
        } else 0.0
    }

    private fun identifyTopicShifts(interactions: List<CollaborativeInteraction>): List<TopicShift> {
        // 简化实现：基于关键词变化识别主题转换
        val shifts = mutableListOf<TopicShift>()

        if (interactions.size < 2) return shifts

        var currentTopic = extractKeywords(interactions.first().content.text ?: "")
        var topicStartIndex = 0

        for (i in 1 until interactions.size) {
            val newKeywords = extractKeywords(interactions[i].content.text ?: "")
            val similarity = calculateKeywordSimilarity(currentTopic, newKeywords)

            if (similarity < 0.3) { // 主题发生显著变化
                shifts.add(
                    TopicShift(
                        fromTopic = currentTopic.joinToString(", "),
                        toTopic = newKeywords.joinToString(", "),
                        shiftTime = interactions[i].timestamp,
                        initiator = interactions[i].participantId
                    )
                )
                currentTopic = newKeywords
                topicStartIndex = i
            }
        }

        return shifts
    }

    private fun calculateFocusConsistency(interactions: List<CollaborativeInteraction>): Double {
        if (interactions.isEmpty()) return 1.0

        val allKeywords = interactions.flatMap {
            extractKeywords(it.content.text ?: "")
        }

        val keywordFrequency = allKeywords.groupBy { it }.mapValues { it.value.size }
        val topKeywords = keywordFrequency.entries.sortedByDescending { it.value }.take(5).map { it.key }

        val focusedInteractions = interactions.count { interaction ->
            val keywords = extractKeywords(interaction.content.text ?: "")
            keywords.any { it in topKeywords }
        }

        return focusedInteractions.toDouble() / interactions.size
    }

    private fun identifyKnowledgeBuildingSequences(interactions: List<CollaborativeInteraction>): List<KnowledgeBuildingSequence> {
        // 识别知识构建序列：问题 -> 回答 -> 扩展 -> 总结
        val sequences = mutableListOf<KnowledgeBuildingSequence>()

        val questions = interactions.filter { it.type == InteractionType.QUESTION }

        for (question in questions) {
            val relatedInteractions = interactions.filter { interaction ->
                interaction.timestamp > question.timestamp &&
                interaction.timestamp <= question.timestamp + Duration.parse("PT30M") &&
                (interaction.type == InteractionType.ANSWER ||
                 interaction.type == InteractionType.SHARE ||
                 interaction.type == InteractionType.COMMENT)
            }.sortedBy { it.timestamp }

            if (relatedInteractions.isNotEmpty()) {
                sequences.add(
                    KnowledgeBuildingSequence(
                        initiatingQuestion = question,
                        buildingInteractions = relatedInteractions,
                        participants = (listOf(question) + relatedInteractions).map { it.participantId }.distinct(),
                        knowledgeDepth = calculateKnowledgeDepth(relatedInteractions)
                    )
                )
            }
        }

        return sequences
    }

    private fun extractKeywords(text: String): List<String> {
        // 简化的关键词提取
        return text.lowercase()
            .split(Regex("[\\s\\p{Punct}]+"))
            .filter { it.length > 2 }
            .distinct()
    }

    private fun calculateKeywordSimilarity(keywords1: List<String>, keywords2: List<String>): Double {
        if (keywords1.isEmpty() && keywords2.isEmpty()) return 1.0
        if (keywords1.isEmpty() || keywords2.isEmpty()) return 0.0

        val intersection = keywords1.intersect(keywords2.toSet()).size
        val union = keywords1.union(keywords2).size

        return intersection.toDouble() / union
    }

    private fun calculateKnowledgeDepth(interactions: List<CollaborativeInteraction>): Double {
        // 基于交互类型和内容长度评估知识深度
        val depthScores = interactions.map { interaction ->
            val baseScore = when (interaction.type) {
                InteractionType.SHARE -> 0.9
                InteractionType.ANSWER -> 0.8
                InteractionType.COMMENT -> 0.6
                else -> 0.4
            }

            val contentLength = interaction.content.text?.length ?: 0
            val lengthBonus = minOf(0.3, contentLength / 200.0) // 200字符为满分

            baseScore + lengthBonus
        }

        return if (depthScores.isNotEmpty()) depthScores.average() else 0.0
    }
}

// ==================== 沟通分析数据模型 ====================

@Serializable
data class CommunicationPatternAnalysis(
    val messageFlow: MessageFlowAnalysis,
    val responsePatterns: ResponsePatternAnalysis,
    val communicationStyle: CommunicationStyleAnalysis,
    val topicProgression: TopicProgressionAnalysis
)

@Serializable
data class MessageFlowAnalysis(
    val peakActivityPeriods: List<PeakActivityPeriod>,
    val averageResponseTime: Duration,
    val messageDistribution: Map<StudentId, Int>,
    val conversationThreads: List<ConversationThread>
)

@Serializable
data class PeakActivityPeriod(
    val startTime: Instant,
    val endTime: Instant,
    val activityLevel: Double,
    val participants: List<StudentId>
)

@Serializable
data class ConversationThread(
    val id: String,
    val interactions: List<CollaborativeInteraction>,
    val participants: List<StudentId>,
    val startTime: Instant,
    val endTime: Instant,
    val topic: String
)

@Serializable
data class ResponsePatternAnalysis(
    val responseTypeDistribution: Map<ResponseType, Int>,
    val averageResponsesPerMessage: Double,
    val mostResponsiveParticipants: List<StudentId>,
    val responseQuality: Double
)

@Serializable
data class CommunicationStyleAnalysis(
    val formalityLevel: FormalityLevel,
    val emotionalTone: EmotionalTone,
    val questionToStatementRatio: Double,
    val collaborativeLanguageUsage: Double
)

@Serializable
enum class EmotionalTone {
    VERY_POSITIVE,
    POSITIVE,
    NEUTRAL,
    NEGATIVE,
    VERY_NEGATIVE
}

@Serializable
data class TopicProgressionAnalysis(
    val topicShifts: List<TopicShift>,
    val focusConsistency: Double,
    val knowledgeBuildingSequences: List<KnowledgeBuildingSequence>
)

@Serializable
data class TopicShift(
    val fromTopic: String,
    val toTopic: String,
    val shiftTime: Instant,
    val initiator: StudentId
)

@Serializable
data class KnowledgeBuildingSequence(
    val initiatingQuestion: CollaborativeInteraction,
    val buildingInteractions: List<CollaborativeInteraction>,
    val participants: List<StudentId>,
    val knowledgeDepth: Double
)

/**
 * 协作模式识别器
 */
class CollaborationPatternRecognizer {

    /**
     * 识别协作模式
     */
    suspend fun recognizePatterns(
        interactions: List<CollaborativeInteraction>,
        participants: List<SessionParticipant>
    ): List<CollaborationPattern> {
        val patterns = mutableListOf<CollaborationPattern>()

        // 1. 识别领导模式
        val leadershipPattern = recognizeLeadershipPattern(interactions, participants)
        if (leadershipPattern != null) {
            patterns.add(leadershipPattern)
        }

        // 2. 识别协作模式
        val collaborationPattern = recognizeCollaborationPattern(interactions)
        if (collaborationPattern != null) {
            patterns.add(collaborationPattern)
        }

        // 3. 识别沟通模式
        val communicationPattern = recognizeCommunicationPattern(interactions)
        if (communicationPattern != null) {
            patterns.add(communicationPattern)
        }

        return patterns
    }

    private fun recognizeLeadershipPattern(
        interactions: List<CollaborativeInteraction>,
        participants: List<SessionParticipant>
    ): CollaborationPattern? {
        val leaders = participants.filter { it.role == ParticipantRole.LEADER }
        if (leaders.isEmpty()) return null

        return CollaborationPattern(
            type = PatternType.LEADERSHIP,
            participants = leaders.map { it.studentId },
            impact = 0.8,
            frequency = leaders.size.toDouble() / participants.size
        )
    }

    private fun recognizeCollaborationPattern(
        interactions: List<CollaborativeInteraction>
    ): CollaborationPattern? {
        val collaborativeInteractions = interactions.filter {
            it.type in listOf(InteractionType.SHARE, InteractionType.COMMENT, InteractionType.QUESTION)
        }

        if (collaborativeInteractions.size < interactions.size * 0.3) return null

        return CollaborationPattern(
            type = PatternType.COLLABORATION,
            participants = collaborativeInteractions.map { it.participantId }.distinct(),
            impact = collaborativeInteractions.size.toDouble() / interactions.size,
            frequency = 1.0
        )
    }

    private fun recognizeCommunicationPattern(
        interactions: List<CollaborativeInteraction>
    ): CollaborationPattern? {
        val communicationInteractions = interactions.filter {
            it.type in listOf(InteractionType.MESSAGE, InteractionType.COMMENT)
        }

        if (communicationInteractions.size < interactions.size * 0.5) return null

        return CollaborationPattern(
            type = PatternType.COMMUNICATION,
            participants = communicationInteractions.map { it.participantId }.distinct(),
            impact = communicationInteractions.size.toDouble() / interactions.size,
            frequency = 1.0
        )
    }
}



/**
 * 小组动态分析异常
 */
class GroupDynamicsAnalysisException(message: String, cause: Throwable? = null) : Exception(message, cause)
