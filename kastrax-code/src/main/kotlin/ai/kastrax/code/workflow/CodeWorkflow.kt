package ai.kastrax.code.workflow

import ai.kastrax.code.model.Context

/**
 * 代码工作流接口
 *
 * 定义代码工作流的基本操作
 */
interface CodeWorkflow {
    /**
     * 工作流ID
     */
    val id: String
    
    /**
     * 工作流名称
     */
    val name: String
    
    /**
     * 工作流描述
     */
    val description: String
    
    /**
     * 工作流步骤
     */
    val steps: List<WorkflowStep>
    
    /**
     * 当前步骤索引
     */
    val currentStepIndex: Int
    
    /**
     * 工作流状态
     */
    val status: WorkflowStatus
    
    /**
     * 工作流上下文
     */
    val context: Context
    
    /**
     * 开始工作流
     *
     * @param context 初始上下文
     * @return 是否成功开始
     */
    suspend fun start(context: Context): Boolean
    
    /**
     * 执行下一步
     *
     * @return 是否成功执行
     */
    suspend fun next(): Boolean
    
    /**
     * 执行上一步
     *
     * @return 是否成功执行
     */
    suspend fun previous(): Boolean
    
    /**
     * 跳转到指定步骤
     *
     * @param stepIndex 步骤索引
     * @return 是否成功跳转
     */
    suspend fun goToStep(stepIndex: Int): Boolean
    
    /**
     * 暂停工作流
     *
     * @return 是否成功暂停
     */
    suspend fun pause(): Boolean
    
    /**
     * 恢复工作流
     *
     * @return 是否成功恢复
     */
    suspend fun resume(): Boolean
    
    /**
     * 取消工作流
     *
     * @return 是否成功取消
     */
    suspend fun cancel(): Boolean
    
    /**
     * 完成工作流
     *
     * @return 是否成功完成
     */
    suspend fun complete(): Boolean
    
    /**
     * 获取当前步骤
     *
     * @return 当前步骤
     */
    fun getCurrentStep(): WorkflowStep?
    
    /**
     * 获取工作流结果
     *
     * @return 工作流结果
     */
    fun getResult(): WorkflowResult
}

/**
 * 工作流步骤接口
 *
 * 定义工作流步骤的基本操作
 */
interface WorkflowStep {
    /**
     * 步骤ID
     */
    val id: String
    
    /**
     * 步骤名称
     */
    val name: String
    
    /**
     * 步骤描述
     */
    val description: String
    
    /**
     * 步骤状态
     */
    val status: StepStatus
    
    /**
     * 执行步骤
     *
     * @param context 上下文
     * @return 步骤结果
     */
    suspend fun execute(context: Context): StepResult
    
    /**
     * 回滚步骤
     *
     * @param context 上下文
     * @return 是否成功回滚
     */
    suspend fun rollback(context: Context): Boolean
    
    /**
     * 验证步骤
     *
     * @param context 上下文
     * @return 是否有效
     */
    fun validate(context: Context): Boolean
}

/**
 * 工作流状态
 */
enum class WorkflowStatus {
    /**
     * 未开始
     */
    NOT_STARTED,
    
    /**
     * 运行中
     */
    RUNNING,
    
    /**
     * 暂停
     */
    PAUSED,
    
    /**
     * 已完成
     */
    COMPLETED,
    
    /**
     * 已取消
     */
    CANCELLED,
    
    /**
     * 失败
     */
    FAILED
}

/**
 * 步骤状态
 */
enum class StepStatus {
    /**
     * 未开始
     */
    NOT_STARTED,
    
    /**
     * 运行中
     */
    RUNNING,
    
    /**
     * 已完成
     */
    COMPLETED,
    
    /**
     * 已跳过
     */
    SKIPPED,
    
    /**
     * 失败
     */
    FAILED
}

/**
 * 步骤结果
 *
 * @property success 是否成功
 * @property message 消息
 * @property context 更新后的上下文
 * @property data 结果数据
 */
data class StepResult(
    val success: Boolean,
    val message: String,
    val context: Context,
    val data: Map<String, Any> = emptyMap()
)

/**
 * 工作流结果
 *
 * @property success 是否成功
 * @property message 消息
 * @property context 最终上下文
 * @property data 结果数据
 */
data class WorkflowResult(
    val success: Boolean,
    val message: String,
    val context: Context,
    val data: Map<String, Any> = emptyMap()
)
