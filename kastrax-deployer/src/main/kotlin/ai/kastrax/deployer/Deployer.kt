package ai.kastrax.deployer

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

/**
 * 部署配置。
 *
 * @property name 应用名称
 * @property version 应用版本
 * @property environment 环境变量
 * @property resources 资源配置
 */
data class DeploymentConfig(
    val name: String,
    val version: String = "1.0.0",
    val environment: Map<String, String> = emptyMap(),
    val resources: ResourceConfig = ResourceConfig()
)

/**
 * 资源配置。
 *
 * @property memory 内存大小（MB）
 * @property cpu CPU 核心数
 * @property timeout 超时时间（秒）
 * @property concurrency 并发数
 */
data class ResourceConfig(
    val memory: Int = 512,
    val cpu: Int = 1,
    val timeout: Int = 30,
    val concurrency: Int = 10
)

/**
 * 部署状态。
 */
enum class DeploymentStatus {
    PREPARING,
    BUILDING,
    UPLOADING,
    DEPLOYING,
    CONFIGURING,
    TESTING,
    COMPLETED,
    FAILED
}

/**
 * 部署状态更新。
 *
 * @property status 部署状态
 * @property message 状态消息
 * @property progress 进度（0-100）
 * @property timestamp 时间戳
 */
data class DeploymentStatusUpdate(
    val status: DeploymentStatus,
    val message: String,
    val progress: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 部署结果。
 *
 * @property success 是否成功
 * @property url 部署 URL
 * @property message 结果消息
 * @property logs 部署日志
 * @property metadata 元数据
 */
data class DeploymentResult(
    val success: Boolean,
    val url: String? = null,
    val message: String = "",
    val logs: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)

/**
 * 部署器接口。
 */
interface Deployer {
    /**
     * 部署器名称。
     */
    val name: String
    
    /**
     * 部署应用。
     *
     * @param projectPath 项目路径
     * @param config 部署配置
     * @return 部署状态更新流
     */
    fun deploy(projectPath: Path, config: DeploymentConfig): Flow<DeploymentStatusUpdate>
    
    /**
     * 获取部署结果。
     *
     * @param deploymentId 部署 ID
     * @return 部署结果
     */
    suspend fun getResult(deploymentId: String): DeploymentResult
    
    /**
     * 删除部署。
     *
     * @param deploymentId 部署 ID
     * @return 是否成功
     */
    suspend fun delete(deploymentId: String): Boolean
}

/**
 * 抽象部署器实现。
 */
abstract class AbstractDeployer : Deployer {
    /**
     * 发出部署状态更新。
     *
     * @param status 部署状态
     * @param message 状态消息
     * @param progress 进度（0-100）
     * @return 部署状态更新
     */
    protected fun createStatusUpdate(
        status: DeploymentStatus,
        message: String,
        progress: Int = 0
    ): DeploymentStatusUpdate {
        return DeploymentStatusUpdate(
            status = status,
            message = message,
            progress = progress
        )
    }
    
    /**
     * 创建部署结果。
     *
     * @param success 是否成功
     * @param url 部署 URL
     * @param message 结果消息
     * @param logs 部署日志
     * @param metadata 元数据
     * @return 部署结果
     */
    protected fun createResult(
        success: Boolean,
        url: String? = null,
        message: String = "",
        logs: List<String> = emptyList(),
        metadata: Map<String, String> = emptyMap()
    ): DeploymentResult {
        return DeploymentResult(
            success = success,
            url = url,
            message = message,
            logs = logs,
            metadata = metadata
        )
    }
}
