package ai.kastrax.a2a.task

import ai.kastrax.a2a.agent.A2AAgent
import ai.kastrax.a2a.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 任务管理器，用于管理任务的生命周期
 */
class TaskManager(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    /**
     * 任务映射
     */
    private val tasks = ConcurrentHashMap<String, Task>()
    
    /**
     * 任务推送通知配置映射
     */
    private val taskPushNotifications = ConcurrentHashMap<String, PushNotificationConfig>()
    
    /**
     * 任务状态更新事件流
     */
    private val _taskStatusEvents = MutableSharedFlow<TaskStatusUpdateEvent>()
    
    /**
     * 任务状态更新事件流（只读）
     */
    val taskStatusEvents = _taskStatusEvents.asSharedFlow()
    
    /**
     * 任务产物更新事件流
     */
    private val _taskArtifactEvents = MutableSharedFlow<TaskArtifactUpdateEvent>()
    
    /**
     * 任务产物更新事件流（只读）
     */
    val taskArtifactEvents = _taskArtifactEvents.asSharedFlow()
    
    /**
     * 创建任务
     */
    fun createTask(message: Message, sessionId: String? = null): Task {
        val taskId = UUID.randomUUID().toString()
        val task = Task(
            id = taskId,
            sessionId = sessionId,
            status = TaskStatus(state = TaskState.SUBMITTED),
            history = listOf(message)
        )
        
        tasks[taskId] = task
        return task
    }
    
    /**
     * 获取任务
     */
    fun getTask(taskId: String): Task? {
        return tasks[taskId]
    }
    
    /**
     * 更新任务状态
     */
    suspend fun updateTaskStatus(taskId: String, state: TaskState, message: String? = null, final: Boolean = false) {
        val task = tasks[taskId] ?: return
        
        val status = TaskStatus(
            state = state,
            message = message
        )
        
        val updatedTask = task.copy(status = status)
        tasks[taskId] = updatedTask
        
        val event = TaskStatusUpdateEvent(
            id = taskId,
            status = status,
            final = final
        )
        
        _taskStatusEvents.emit(event)
        
        // 如果配置了推送通知，则发送通知
        taskPushNotifications[taskId]?.let { config ->
            sendPushNotification(config, event)
        }
    }
    
    /**
     * 添加任务产物
     */
    suspend fun addTaskArtifact(taskId: String, artifact: Artifact) {
        val task = tasks[taskId] ?: return
        
        val artifacts = task.artifacts?.toMutableList() ?: mutableListOf()
        artifacts.add(artifact)
        
        val updatedTask = task.copy(artifacts = artifacts)
        tasks[taskId] = updatedTask
        
        val event = TaskArtifactUpdateEvent(
            id = taskId,
            artifact = artifact
        )
        
        _taskArtifactEvents.emit(event)
        
        // 如果配置了推送通知，则发送通知
        taskPushNotifications[taskId]?.let { config ->
            sendPushNotification(config, event)
        }
    }
    
    /**
     * 添加任务历史
     */
    fun addTaskHistory(taskId: String, message: Message) {
        val task = tasks[taskId] ?: return
        
        val history = task.history?.toMutableList() ?: mutableListOf()
        history.add(message)
        
        val updatedTask = task.copy(history = history)
        tasks[taskId] = updatedTask
    }
    
    /**
     * 取消任务
     */
    suspend fun cancelTask(taskId: String): Task? {
        val task = tasks[taskId] ?: return null
        
        if (task.status.state == TaskState.COMPLETED || 
            task.status.state == TaskState.CANCELED || 
            task.status.state == TaskState.FAILED) {
            return task
        }
        
        updateTaskStatus(taskId, TaskState.CANCELED, "Task canceled by user", true)
        return tasks[taskId]
    }
    
    /**
     * 设置任务推送通知配置
     */
    fun setTaskPushNotification(taskId: String, config: PushNotificationConfig): TaskPushNotificationConfig {
        taskPushNotifications[taskId] = config
        return TaskPushNotificationConfig(taskId, config)
    }
    
    /**
     * 获取任务推送通知配置
     */
    fun getTaskPushNotification(taskId: String): TaskPushNotificationConfig? {
        val config = taskPushNotifications[taskId] ?: return null
        return TaskPushNotificationConfig(taskId, config)
    }
    
    /**
     * 处理任务
     */
    suspend fun processTask(agent: A2AAgent, task: Task): Flow<TaskStatusUpdateEvent> = flow {
        // 更新任务状态为处理中
        updateTaskStatus(task.id, TaskState.WORKING, "Task is being processed")
        emit(TaskStatusUpdateEvent(task.id, TaskStatus(state = TaskState.WORKING)))
        
        try {
            // 从任务历史中获取最后一条消息
            val message = task.history?.lastOrNull()
            
            if (message != null) {
                // 创建调用请求
                val request = InvokeRequest(
                    id = UUID.randomUUID().toString(),
                    capabilityId = "process_task", // 假设代理有一个处理任务的能力
                    parameters = mapOf(
                        "prompt" to JsonPrimitive(getPromptFromMessage(message)),
                        "task_id" to JsonPrimitive(task.id)
                    )
                )
                
                // 调用代理处理任务
                val response = agent.invoke(request)
                
                // 创建任务产物
                val artifact = Artifact(
                    name = "result",
                    parts = listOf(
                        TextPart(
                            text = response.result.toString()
                        )
                    )
                )
                
                // 添加任务产物
                addTaskArtifact(task.id, artifact)
                
                // 更新任务状态为已完成
                updateTaskStatus(task.id, TaskState.COMPLETED, "Task completed successfully", true)
                emit(TaskStatusUpdateEvent(task.id, TaskStatus(state = TaskState.COMPLETED), true))
            } else {
                // 如果没有消息，则更新任务状态为失败
                updateTaskStatus(task.id, TaskState.FAILED, "No message found in task history", true)
                emit(TaskStatusUpdateEvent(task.id, TaskStatus(state = TaskState.FAILED), true))
            }
        } catch (e: Exception) {
            // 如果处理过程中出现异常，则更新任务状态为失败
            updateTaskStatus(task.id, TaskState.FAILED, "Error processing task: ${e.message}", true)
            emit(TaskStatusUpdateEvent(task.id, TaskStatus(state = TaskState.FAILED), true))
        }
    }
    
    /**
     * 从消息中获取提示
     */
    private fun getPromptFromMessage(message: Message): String {
        return message.parts
            .filterIsInstance<TextPart>()
            .joinToString("\n") { it.text }
    }
    
    /**
     * 发送推送通知
     */
    private fun sendPushNotification(config: PushNotificationConfig, event: Any) {
        // 在实际实现中，这里应该使用 HTTP 客户端发送通知
        // 为了简化，这里只是打印日志
        println("Sending push notification to ${config.url}: $event")
    }
    
    /**
     * 处理发送任务请求
     */
    suspend fun onSendTask(agent: A2AAgent, params: TaskSendParams): Task {
        // 创建任务
        val task = createTask(params.message, params.sessionId)
        
        // 如果配置了推送通知，则设置推送通知
        params.pushNotification?.let { config ->
            setTaskPushNotification(task.id, config)
        }
        
        // 启动协程处理任务
        scope.launch {
            processTask(agent, task)
        }
        
        return task
    }
    
    /**
     * 处理发送任务并订阅更新请求
     */
    suspend fun onSendTaskSubscribe(agent: A2AAgent, params: TaskSendParams): Flow<Any> = flow {
        // 创建任务
        val task = createTask(params.message, params.sessionId)
        
        // 如果配置了推送通知，则设置推送通知
        params.pushNotification?.let { config ->
            setTaskPushNotification(task.id, config)
        }
        
        // 处理任务并发送更新
        processTask(agent, task).collect { event ->
            emit(event)
        }
    }
}
