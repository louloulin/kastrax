package ai.kastrax.a2a.task

import ai.kastrax.a2a.agent.A2AAgent
import ai.kastrax.a2a.model.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 任务管理器测试
 */
class TaskManagerTest {
    /**
     * 任务管理器
     */
    private lateinit var taskManager: TaskManager
    
    /**
     * 模拟的 A2A 代理
     */
    private lateinit var mockAgent: A2AAgent
    
    @BeforeEach
    fun setup() {
        // 创建任务管理器
        taskManager = TaskManager()
        
        // 创建模拟的 A2A 代理
        mockAgent = mockk<A2AAgent>()
        
        // 配置模拟代理的行为
        coEvery { mockAgent.invoke(any()) } returns InvokeResponse(
            id = "test-response",
            result = JsonPrimitive("这是一个测试响应")
        )
    }
    
    @Test
    fun `test create task`() {
        // 创建任务消息
        val message = Message(
            role = "user",
            parts = listOf(
                TextPart(
                    text = "你好，请帮我分析这些数据"
                )
            )
        )
        
        // 创建任务
        val task = taskManager.createTask(message)
        
        // 验证任务
        assertNotNull(task)
        assertEquals(TaskState.SUBMITTED, task.status.state)
        assertEquals(1, task.history?.size)
        assertEquals("user", task.history?.first()?.role)
    }
    
    @Test
    fun `test process task`() = runBlocking {
        // 创建任务消息
        val message = Message(
            role = "user",
            parts = listOf(
                TextPart(
                    text = "你好，请帮我分析这些数据"
                )
            )
        )
        
        // 创建任务
        val task = taskManager.createTask(message)
        
        // 收集任务状态更新事件
        val statusEvents = mutableListOf<TaskStatusUpdateEvent>()
        val statusJob = taskManager.taskStatusEvents.toList(statusEvents)
        
        // 收集任务产物更新事件
        val artifactEvents = mutableListOf<TaskArtifactUpdateEvent>()
        val artifactJob = taskManager.taskArtifactEvents.toList(artifactEvents)
        
        try {
            // 处理任务
            val events = taskManager.processTask(mockAgent, task).toList()
            
            // 验证事件
            assertTrue(events.isNotEmpty())
            assertEquals(task.id, events.first().id)
            assertEquals(TaskState.WORKING, events.first().status.state)
            assertEquals(task.id, events.last().id)
            assertEquals(TaskState.COMPLETED, events.last().status.state)
            assertTrue(events.last().final)
            
            // 获取更新后的任务
            val updatedTask = taskManager.getTask(task.id)
            
            // 验证任务状态
            assertNotNull(updatedTask)
            assertEquals(TaskState.COMPLETED, updatedTask.status.state)
            
            // 验证任务产物
            assertNotNull(updatedTask.artifacts)
            assertTrue(updatedTask.artifacts!!.isNotEmpty())
            assertEquals("result", updatedTask.artifacts!!.first().name)
            assertTrue(updatedTask.artifacts!!.first().parts.isNotEmpty())
            assertTrue(updatedTask.artifacts!!.first().parts.first() is TextPart)
        } finally {
            // 取消事件收集
            statusJob.cancel()
            artifactJob.cancel()
        }
    }
    
    @Test
    fun `test cancel task`() = runBlocking {
        // 创建任务消息
        val message = Message(
            role = "user",
            parts = listOf(
                TextPart(
                    text = "你好，请帮我分析这些数据"
                )
            )
        )
        
        // 创建任务
        val task = taskManager.createTask(message)
        
        // 取消任务
        val canceledTask = taskManager.cancelTask(task.id)
        
        // 验证任务状态
        assertNotNull(canceledTask)
        assertEquals(TaskState.CANCELED, canceledTask.status.state)
    }
    
    @Test
    fun `test push notification`() {
        // 创建任务消息
        val message = Message(
            role = "user",
            parts = listOf(
                TextPart(
                    text = "你好，请帮我分析这些数据"
                )
            )
        )
        
        // 创建任务
        val task = taskManager.createTask(message)
        
        // 创建推送通知配置
        val config = PushNotificationConfig(
            url = "https://example.com/webhook",
            token = "test-token"
        )
        
        // 设置任务推送通知
        val pushConfig = taskManager.setTaskPushNotification(task.id, config)
        
        // 验证推送通知配置
        assertNotNull(pushConfig)
        assertEquals(task.id, pushConfig.id)
        assertEquals("https://example.com/webhook", pushConfig.pushNotificationConfig.url)
        assertEquals("test-token", pushConfig.pushNotificationConfig.token)
        
        // 获取任务推送通知
        val retrievedConfig = taskManager.getTaskPushNotification(task.id)
        
        // 验证获取的推送通知配置
        assertNotNull(retrievedConfig)
        assertEquals(task.id, retrievedConfig.id)
        assertEquals("https://example.com/webhook", retrievedConfig.pushNotificationConfig.url)
        assertEquals("test-token", retrievedConfig.pushNotificationConfig.token)
    }
}
