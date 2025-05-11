package ai.kastrax.codebase.indexing.distributed

import actor.proto.ActorSystem
import actor.proto.PID
import actor.proto.fromProducer
import ai.kastrax.codebase.indexing.IndexTask
import ai.kastrax.codebase.indexing.IndexTaskProcessor
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

private val logger = KotlinLogging.logger {}

/**
 * 分布式索引系统
 *
 * 使用 Actor 模型实现的分布式索引系统
 *
 * @property actorSystem Actor 系统
 * @property taskProcessor 任务处理器
 * @property coordinatorConfig 协调者配置
 */
class DistributedIndexSystem(
    private val actorSystem: ActorSystem,
    private val taskProcessor: IndexTaskProcessor,
    private val coordinatorConfig: IndexCoordinatorConfig = IndexCoordinatorConfig()
) : AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // 协调者 Actor
    private lateinit var coordinatorActor: IndexCoordinatorActor
    private lateinit var coordinatorPid: PID
    
    // 运行状态
    private val isRunning = AtomicBoolean(false)
    
    /**
     * 启动分布式索引系统
     */
    suspend fun start() = withContext(Dispatchers.Default) {
        if (isRunning.getAndSet(true)) {
            logger.warn { "分布式索引系统已经在运行" }
            return@withContext
        }
        
        logger.info { "启动分布式索引系统" }
        
        // 创建协调者 Actor
        coordinatorActor = IndexCoordinatorActor(
            system = actorSystem,
            taskProcessor = taskProcessor,
            config = coordinatorConfig
        )
        
        val props = fromProducer { coordinatorActor }
        coordinatorPid = actorSystem.root.spawn(props)
        
        // 监听协调者事件
        scope.launch {
            coordinatorActor.events.collect { event ->
                when (event) {
                    is IndexCoordinatorEvent.TaskSubmitted -> 
                        logger.debug { "任务已提交: ${event.taskId}" }
                    is IndexCoordinatorEvent.TaskAssigned -> 
                        logger.debug { "任务已分配: ${event.taskId} 到工作者: ${event.workerId}" }
                    is IndexCoordinatorEvent.TaskCompleted -> 
                        logger.debug { "任务已完成: ${event.taskId} 由工作者: ${event.workerId}" }
                    is IndexCoordinatorEvent.TaskFailed -> 
                        logger.error { "任务失败: ${event.taskId} 由工作者: ${event.workerId}, 错误: ${event.error}" }
                    is IndexCoordinatorEvent.WorkerRegistered -> 
                        logger.info { "工作者已注册: ${event.workerId}" }
                    is IndexCoordinatorEvent.WorkerUnregistered -> 
                        logger.info { "工作者已注销: ${event.workerId}" }
                }
            }
        }
        
        logger.info { "分布式索引系统已启动" }
    }
    
    /**
     * 停止分布式索引系统
     */
    suspend fun stop() = withContext(Dispatchers.Default) {
        if (!isRunning.getAndSet(false)) {
            logger.warn { "分布式索引系统已经停止" }
            return@withContext
        }
        
        logger.info { "停止分布式索引系统" }
        
        // 停止协调者 Actor
        actorSystem.stop(coordinatorPid)
        
        logger.info { "分布式索引系统已停止" }
    }
    
    /**
     * 提交任务
     *
     * @param task 索引任务
     */
    suspend fun submitTask(task: IndexTask) = withContext(Dispatchers.Default) {
        if (!isRunning.get()) {
            logger.warn { "分布式索引系统未运行，无法提交任务" }
            return@withContext false
        }
        
        actorSystem.root.send(coordinatorPid, IndexCoordinatorMessage.SubmitTask(task))
        return@withContext true
    }
    
    /**
     * 批量提交任务
     *
     * @param tasks 任务列表
     * @return 成功提交的任务数量
     */
    suspend fun submitBatch(tasks: List<IndexTask>) = withContext(Dispatchers.Default) {
        if (!isRunning.get()) {
            logger.warn { "分布式索引系统未运行，无法提交任务" }
            return@withContext 0
        }
        
        if (tasks.isEmpty()) {
            return@withContext 0
        }
        
        actorSystem.root.send(coordinatorPid, IndexCoordinatorMessage.SubmitBatch(tasks))
        return@withContext tasks.size
    }
    
    /**
     * 获取系统状态
     *
     * @return 系统状态
     */
    suspend fun getStatus(): IndexCoordinatorMessage.StatusResponse = withContext(Dispatchers.Default) {
        if (!isRunning.get()) {
            return@withContext IndexCoordinatorMessage.StatusResponse(0, 0, 0, 0, 0)
        }
        
        return@withContext actorSystem.root.requestAwait(coordinatorPid, IndexCoordinatorMessage.GetStatus)
    }
    
    /**
     * 获取协调者事件流
     *
     * @return 事件流
     */
    fun getEventFlow(): SharedFlow<IndexCoordinatorEvent> {
        return coordinatorActor.events
    }
    
    /**
     * 关闭分布式索引系统
     */
    override fun close() {
        runBlocking {
            stop()
        }
    }
    
    companion object {
        /**
         * 创建分布式索引系统
         *
         * @param taskProcessor 任务处理器
         * @param actorSystem Actor 系统，如果为 null，则创建新的 Actor 系统
         * @param coordinatorConfig 协调者配置
         * @return 分布式索引系统
         */
        suspend fun create(
            taskProcessor: IndexTaskProcessor,
            actorSystem: ActorSystem? = null,
            coordinatorConfig: IndexCoordinatorConfig = IndexCoordinatorConfig()
        ): DistributedIndexSystem {
            val system = actorSystem ?: ActorSystem("distributed-index-system")
            
            val indexSystem = DistributedIndexSystem(
                actorSystem = system,
                taskProcessor = taskProcessor,
                coordinatorConfig = coordinatorConfig
            )
            
            indexSystem.start()
            
            return indexSystem
        }
    }
}
