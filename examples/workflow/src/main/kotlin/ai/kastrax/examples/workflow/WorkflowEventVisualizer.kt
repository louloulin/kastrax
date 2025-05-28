package ai.kastrax.examples.workflow

import ai.kastrax.core.workflow.event.WorkflowEvent
import ai.kastrax.core.workflow.event.WorkflowEventStorage
import ai.kastrax.core.workflow.event.WorkflowEventType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 工作流事件可视化器，用于将工作流事件转换为可视化表示。
 */
class WorkflowEventVisualizer(
    private val eventStorage: WorkflowEventStorage
) {
    /**
     * 生成工作流执行的时间线可视化。
     *
     * @param workflowId 工作流ID
     * @param runId 运行ID
     * @return 时间线可视化字符串
     */
    suspend fun generateTimeline(workflowId: String, runId: String): String {
        // 获取所有事件
        val events = eventStorage.getEvents(workflowId, runId)
        
        // 如果没有事件，返回空字符串
        if (events.isEmpty()) {
            return "没有找到工作流事件。"
        }
        
        // 按时间戳排序
        val sortedEvents = events.sortedBy { it.timestamp }
        
        // 获取开始和结束时间
        val startTime = sortedEvents.first().timestamp
        val endTime = sortedEvents.last().timestamp
        val duration = endTime - startTime
        
        // 创建时间线
        val timelineWidth = 60
        val timelineBuilder = StringBuilder()
        
        // 添加标题
        timelineBuilder.append("工作流执行时间线: $workflowId (运行ID: $runId)\n")
        timelineBuilder.append("总执行时间: ${formatDuration(duration)}\n\n")
        
        // 添加时间轴
        timelineBuilder.append("时间 ".padEnd(12))
        timelineBuilder.append("│ ")
        timelineBuilder.append("事件".padEnd(20))
        timelineBuilder.append("│ ")
        timelineBuilder.append("步骤".padEnd(15))
        timelineBuilder.append("│ ")
        timelineBuilder.append("详情\n")
        
        // 添加分隔线
        timelineBuilder.append("─".repeat(12))
        timelineBuilder.append("┼")
        timelineBuilder.append("─".repeat(22))
        timelineBuilder.append("┼")
        timelineBuilder.append("─".repeat(17))
        timelineBuilder.append("┼")
        timelineBuilder.append("─".repeat(30))
        timelineBuilder.append("\n")
        
        // 添加事件
        for (event in sortedEvents) {
            // 格式化时间
            val timestamp = Instant.ofEpochMilli(event.timestamp)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
            
            // 计算相对位置
            val relativeTime = (event.timestamp - startTime).toDouble() / duration
            val position = (relativeTime * timelineWidth).toInt()
            
            // 添加时间戳
            timelineBuilder.append(timestamp.padEnd(12))
            
            // 添加事件类型
            timelineBuilder.append("│ ")
            timelineBuilder.append(formatEventType(event.type).padEnd(20))
            
            // 添加步骤ID
            timelineBuilder.append("│ ")
            timelineBuilder.append((event.stepId ?: "").padEnd(15))
            
            // 添加详情
            timelineBuilder.append("│ ")
            timelineBuilder.append(formatEventDetails(event))
            timelineBuilder.append("\n")
        }
        
        return timelineBuilder.toString()
    }
    
    /**
     * 生成工作流执行的步骤图可视化。
     *
     * @param workflowId 工作流ID
     * @param runId 运行ID
     * @return 步骤图可视化字符串
     */
    suspend fun generateStepGraph(workflowId: String, runId: String): String {
        // 获取所有事件
        val events = eventStorage.getEvents(workflowId, runId)
        
        // 如果没有事件，返回空字符串
        if (events.isEmpty()) {
            return "没有找到工作流事件。"
        }
        
        // 按时间戳排序
        val sortedEvents = events.sortedBy { it.timestamp }
        
        // 收集步骤信息
        val stepStartEvents = sortedEvents.filter { it.type == WorkflowEventType.STEP_STARTED }
        val stepCompleteEvents = sortedEvents.filter { it.type == WorkflowEventType.STEP_COMPLETED }
        val stepFailEvents = sortedEvents.filter { it.type == WorkflowEventType.STEP_FAILED }
        val stepSkipEvents = sortedEvents.filter { it.type == WorkflowEventType.STEP_SKIPPED }
        
        // 创建步骤图
        val graphBuilder = StringBuilder()
        
        // 添加标题
        graphBuilder.append("工作流步骤图: $workflowId (运行ID: $runId)\n\n")
        
        // 添加步骤
        val allStepIds = (stepStartEvents.mapNotNull { it.stepId } +
                stepCompleteEvents.mapNotNull { it.stepId } +
                stepFailEvents.mapNotNull { it.stepId } +
                stepSkipEvents.mapNotNull { it.stepId }).distinct()
        
        for (stepId in allStepIds) {
            // 获取步骤状态
            val isStarted = stepStartEvents.any { it.stepId == stepId }
            val isCompleted = stepCompleteEvents.any { it.stepId == stepId }
            val isFailed = stepFailEvents.any { it.stepId == stepId }
            val isSkipped = stepSkipEvents.any { it.stepId == stepId }
            
            // 添加步骤状态
            val status = when {
                isSkipped -> "⏭️ 跳过"
                isFailed -> "❌ 失败"
                isCompleted -> "✅ 完成"
                isStarted -> "🔄 开始"
                else -> "❓ 未知"
            }
            
            graphBuilder.append("$status - $stepId\n")
            
            // 添加步骤详情
            if (isCompleted) {
                val completeEvent = stepCompleteEvents.first { it.stepId == stepId }
                val startEvent = stepStartEvents.firstOrNull { it.stepId == stepId }
                
                if (startEvent != null) {
                    val duration = completeEvent.timestamp - startEvent.timestamp
                    graphBuilder.append("  ⏱️ 执行时间: ${formatDuration(duration)}\n")
                }
                
                // 添加输出
                val output = completeEvent.data["result"]
                if (output != null) {
                    graphBuilder.append("  📤 输出: $output\n")
                }
            } else if (isFailed) {
                val failEvent = stepFailEvents.first { it.stepId == stepId }
                val error = failEvent.data["error"]
                graphBuilder.append("  🔴 错误: $error\n")
            } else if (isSkipped) {
                val skipEvent = stepSkipEvents.first { it.stepId == stepId }
                val reason = skipEvent.data["reason"]
                graphBuilder.append("  📝 原因: $reason\n")
            }
            
            graphBuilder.append("\n")
        }
        
        return graphBuilder.toString()
    }
    
    /**
     * 生成工作流执行的统计信息。
     *
     * @param workflowId 工作流ID
     * @param runId 运行ID
     * @return 统计信息字符串
     */
    suspend fun generateStats(workflowId: String, runId: String): String {
        // 获取所有事件
        val events = eventStorage.getEvents(workflowId, runId)
        
        // 如果没有事件，返回空字符串
        if (events.isEmpty()) {
            return "没有找到工作流事件。"
        }
        
        // 按时间戳排序
        val sortedEvents = events.sortedBy { it.timestamp }
        
        // 获取开始和结束时间
        val startTime = sortedEvents.first().timestamp
        val endTime = sortedEvents.last().timestamp
        val duration = endTime - startTime
        
        // 统计事件类型
        val eventTypeCounts = events.groupBy { it.type }.mapValues { it.value.size }
        
        // 创建统计信息
        val statsBuilder = StringBuilder()
        
        // 添加标题
        statsBuilder.append("工作流执行统计: $workflowId (运行ID: $runId)\n\n")
        
        // 添加基本信息
        statsBuilder.append("总执行时间: ${formatDuration(duration)}\n")
        statsBuilder.append("总事件数: ${events.size}\n")
        statsBuilder.append("步骤数: ${events.mapNotNull { it.stepId }.distinct().size}\n\n")
        
        // 添加事件类型统计
        statsBuilder.append("事件类型统计:\n")
        for ((type, count) in eventTypeCounts.entries.sortedByDescending { it.value }) {
            statsBuilder.append("  ${formatEventType(type)}: $count\n")
        }
        
        return statsBuilder.toString()
    }
    
    /**
     * 格式化事件类型。
     *
     * @param type 事件类型
     * @return 格式化后的事件类型字符串
     */
    private fun formatEventType(type: WorkflowEventType): String {
        return when (type) {
            WorkflowEventType.WORKFLOW_STARTED -> "工作流开始"
            WorkflowEventType.WORKFLOW_COMPLETED -> "工作流完成"
            WorkflowEventType.WORKFLOW_FAILED -> "工作流失败"
            WorkflowEventType.WORKFLOW_SUSPENDED -> "工作流暂停"
            WorkflowEventType.WORKFLOW_RESUMED -> "工作流恢复"
            WorkflowEventType.WORKFLOW_CANCELED -> "工作流取消"
            WorkflowEventType.STEP_STARTED -> "步骤开始"
            WorkflowEventType.STEP_COMPLETED -> "步骤完成"
            WorkflowEventType.STEP_FAILED -> "步骤失败"
            WorkflowEventType.STEP_SUSPENDED -> "步骤暂停"
            WorkflowEventType.STEP_RESUMED -> "步骤恢复"
            WorkflowEventType.STEP_SKIPPED -> "步骤跳过"
            WorkflowEventType.ERROR_OCCURRED -> "错误发生"
            WorkflowEventType.RETRY_STARTED -> "重试开始"
            WorkflowEventType.RECOVERY_APPLIED -> "恢复策略应用"
            WorkflowEventType.VARIABLE_CHANGED -> "变量变更"
            WorkflowEventType.CUSTOM -> "自定义事件"
        }
    }
    
    /**
     * 格式化事件详情。
     *
     * @param event 工作流事件
     * @return 格式化后的事件详情字符串
     */
    private fun formatEventDetails(event: WorkflowEvent): String {
        return when (event.type) {
            WorkflowEventType.WORKFLOW_STARTED -> "输入: ${event.data["input"]}"
            WorkflowEventType.WORKFLOW_COMPLETED -> "输出: ${event.data["output"]}"
            WorkflowEventType.WORKFLOW_FAILED -> "错误: ${event.data["error"]}"
            WorkflowEventType.STEP_STARTED -> "输入: ${event.data["input"]}"
            WorkflowEventType.STEP_COMPLETED -> "输出: ${event.data["result"]}"
            WorkflowEventType.STEP_FAILED -> "错误: ${event.data["error"]}"
            WorkflowEventType.STEP_SKIPPED -> "原因: ${event.data["reason"]}"
            WorkflowEventType.ERROR_OCCURRED -> "错误: ${event.data["errorMessage"]}"
            else -> event.data.entries.joinToString(", ") { "${it.key}=${it.value}" }
        }
    }
    
    /**
     * 格式化持续时间。
     *
     * @param millis 毫秒数
     * @return 格式化后的持续时间字符串
     */
    private fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when {
            hours > 0 -> String.format("%d小时 %d分钟 %d秒", hours, minutes % 60, seconds % 60)
            minutes > 0 -> String.format("%d分钟 %d秒", minutes, seconds % 60)
            else -> String.format("%d.%03d秒", seconds, millis % 1000)
        }
    }
}
