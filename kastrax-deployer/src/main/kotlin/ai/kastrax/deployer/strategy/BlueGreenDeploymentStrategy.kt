package ai.kastrax.deployer.strategy

import ai.kastrax.deployer.AbstractDeployer
import ai.kastrax.deployer.Deployer
import ai.kastrax.deployer.DeploymentConfig
import ai.kastrax.deployer.DeploymentResult
import ai.kastrax.deployer.DeploymentStatus
import ai.kastrax.deployer.DeploymentStatusUpdate
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * 蓝绿部署配置。
 *
 * @property healthCheckUrl 健康检查 URL
 * @property healthCheckTimeout 健康检查超时时间（秒）
 * @property switchDelay 切换延迟时间（秒）
 * @property autoSwitch 是否自动切换
 */
data class BlueGreenConfig(
    val healthCheckUrl: String? = null,
    val healthCheckTimeout: Int = 60,
    val switchDelay: Int = 10,
    val autoSwitch: Boolean = true
)

/**
 * 蓝绿部署状态。
 *
 * @property activeDeployment 活动部署 ID
 * @property stageDeployment 阶段部署 ID
 * @property previousDeployment 上一个部署 ID
 * @property status 部署状态
 */
data class BlueGreenDeploymentState(
    val activeDeployment: String? = null,
    val stageDeployment: String? = null,
    val previousDeployment: String? = null,
    val status: DeploymentStatus = DeploymentStatus.PREPARING
)

/**
 * 蓝绿部署策略实现。
 *
 * @property deployer 部署器
 * @property config 蓝绿部署配置
 */
class BlueGreenDeploymentStrategy(
    private val deployer: Deployer,
    private val config: BlueGreenConfig = BlueGreenConfig()
) : DeploymentStrategy {

    override val type: DeploymentStrategyType = DeploymentStrategyType.BLUE_GREEN

    private val deploymentStates = ConcurrentHashMap<String, BlueGreenDeploymentState>()
    private val deploymentResults = ConcurrentHashMap<String, DeploymentResult>()

    override fun deploy(projectPath: Path, config: DeploymentConfig): Flow<DeploymentStatusUpdate> = flow {
        val deploymentId = UUID.randomUUID().toString()
        logger.info { "Starting blue-green deployment with ID: $deploymentId" }

        try {
            // 准备阶段
            emit(createStatusUpdate(DeploymentStatus.PREPARING, "Preparing blue-green deployment", 10))

            // 查找当前活动部署
            val activeDeploymentId = findActiveDeployment(config.name)

            // 创建阶段部署配置
            val stageConfig = config.copy(
                name = "${config.name}-stage"
            )

            // 部署阶段环境
            emit(createStatusUpdate(DeploymentStatus.DEPLOYING, "Deploying stage environment", 30))

            var stageDeploymentId: String? = null
            var stageResult: DeploymentResult? = null

            deployer.deploy(projectPath, stageConfig).collect { update ->
                // 转发部署状态更新
                if (update.status != DeploymentStatus.COMPLETED && update.status != DeploymentStatus.FAILED) {
                    emit(update)
                }

                if (update.status == DeploymentStatus.COMPLETED) {
                    stageDeploymentId = deploymentId
                    stageResult = deployer.getResult(deploymentId)
                }
            }

            if (stageDeploymentId == null || stageResult == null || !stageResult!!.success) {
                val errorMessage = "Stage deployment failed"
                logger.error { errorMessage }
                emit(createStatusUpdate(DeploymentStatus.FAILED, errorMessage, 0))

                deploymentResults[deploymentId] = DeploymentResult(
                    success = false,
                    message = errorMessage
                )

                return@flow
            }

            // 健康检查
            if (this@BlueGreenDeploymentStrategy.config.healthCheckUrl != null) {
                emit(createStatusUpdate(DeploymentStatus.TESTING, "Performing health check on stage environment", 60))

                val healthCheckResult = performHealthCheck(stageResult!!.url, this@BlueGreenDeploymentStrategy.config.healthCheckTimeout)
                if (!healthCheckResult) {
                    val errorMessage = "Health check failed for stage environment"
                    logger.error { errorMessage }
                    emit(createStatusUpdate(DeploymentStatus.FAILED, errorMessage, 0))

                    // 清理阶段部署
                    deployer.delete(stageDeploymentId!!)

                    deploymentResults[deploymentId] = DeploymentResult(
                        success = false,
                        message = errorMessage
                    )

                    return@flow
                }
            }

            // 切换流量
            if (this@BlueGreenDeploymentStrategy.config.autoSwitch) {
                emit(createStatusUpdate(DeploymentStatus.CONFIGURING, "Switching traffic to new environment", 80))

                // 等待切换延迟
                delay(this@BlueGreenDeploymentStrategy.config.switchDelay * 1000L)

                // 更新部署状态
                deploymentStates[config.name] = BlueGreenDeploymentState(
                    activeDeployment = stageDeploymentId,
                    stageDeployment = null,
                    previousDeployment = activeDeploymentId,
                    status = DeploymentStatus.COMPLETED
                )

                // 如果有上一个部署，可以选择保留或删除
                if (activeDeploymentId != null) {
                    // 这里选择保留，以便于回滚
                    logger.info { "Previous deployment $activeDeploymentId kept for potential rollback" }
                }
            } else {
                // 如果不自动切换，则保持阶段部署状态
                deploymentStates[config.name] = BlueGreenDeploymentState(
                    activeDeployment = activeDeploymentId,
                    stageDeployment = stageDeploymentId,
                    previousDeployment = null,
                    status = DeploymentStatus.COMPLETED
                )

                emit(createStatusUpdate(
                    DeploymentStatus.COMPLETED,
                    "Stage environment deployed successfully, manual switch required",
                    100
                ))

                deploymentResults[deploymentId] = DeploymentResult(
                    success = true,
                    url = stageResult!!.url,
                    message = "Stage environment deployed successfully, manual switch required",
                    metadata = mapOf(
                        "activeDeployment" to (activeDeploymentId ?: "none"),
                        "stageDeployment" to stageDeploymentId!!
                    )
                )

                return@flow
            }

            // 完成部署
            emit(createStatusUpdate(DeploymentStatus.COMPLETED, "Blue-green deployment completed successfully", 100))

            deploymentResults[deploymentId] = DeploymentResult(
                success = true,
                url = stageResult!!.url,
                message = "Blue-green deployment completed successfully",
                metadata = mapOf(
                    "previousDeployment" to (activeDeploymentId ?: "none"),
                    "activeDeployment" to stageDeploymentId!!
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Blue-green deployment failed" }

            emit(createStatusUpdate(DeploymentStatus.FAILED, "Blue-green deployment failed: ${e.message}", 0))

            deploymentResults[deploymentId] = DeploymentResult(
                success = false,
                message = "Blue-green deployment failed: ${e.message}"
            )
        }
    }

    override suspend fun rollback(deploymentId: String): Boolean {
        val result = deploymentResults[deploymentId] ?: return false

        if (!result.success) {
            logger.error { "Cannot rollback failed deployment: $deploymentId" }
            return false
        }

        val previousDeploymentId = result.metadata["previousDeployment"] ?: return false
        if (previousDeploymentId == "none") {
            logger.error { "No previous deployment to rollback to" }
            return false
        }

        val activeDeploymentId = result.metadata["activeDeployment"] ?: return false

        try {
            // 查找应用名称
            val appName = findApplicationName(deploymentId)
            if (appName == null) {
                logger.error { "Cannot find application name for deployment: $deploymentId" }
                return false
            }

            // 更新部署状态
            deploymentStates[appName] = BlueGreenDeploymentState(
                activeDeployment = previousDeploymentId,
                stageDeployment = null,
                previousDeployment = activeDeploymentId,
                status = DeploymentStatus.COMPLETED
            )

            logger.info { "Rolled back from $activeDeploymentId to $previousDeploymentId" }
            return true
        } catch (e: Exception) {
            logger.error(e) { "Rollback failed" }
            return false
        }
    }

    override suspend fun getResult(deploymentId: String): DeploymentResult {
        return deploymentResults[deploymentId] ?: DeploymentResult(
            success = false,
            message = "Deployment not found"
        )
    }

    /**
     * 查找活动部署 ID。
     *
     * @param appName 应用名称
     * @return 活动部署 ID
     */
    private fun findActiveDeployment(appName: String): String? {
        val state = deploymentStates[appName]
        return state?.activeDeployment
    }

    /**
     * 查找应用名称。
     *
     * @param deploymentId 部署 ID
     * @return 应用名称
     */
    private fun findApplicationName(deploymentId: String): String? {
        for ((appName, state) in deploymentStates) {
            if (state.activeDeployment == deploymentId || state.stageDeployment == deploymentId || state.previousDeployment == deploymentId) {
                return appName
            }
        }
        return null
    }

    /**
     * 执行健康检查。
     *
     * @param url URL
     * @param timeout 超时时间（秒）
     * @return 是否成功
     */
    private suspend fun performHealthCheck(url: String?, timeout: Int): Boolean {
        if (url == null) {
            logger.error { "Health check URL is null" }
            return false
        }

        try {
            // 简单的健康检查实现
            val client = java.net.http.HttpClient.newBuilder().build()
            val request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .timeout(java.time.Duration.ofSeconds(timeout.toLong()))
                .GET()
                .build()

            val response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString())

            return response.statusCode() in 200..299
        } catch (e: Exception) {
            logger.error(e) { "Health check failed" }
            return false
        }
    }

    /**
     * 创建状态更新。
     *
     * @param status 状态
     * @param message 消息
     * @param progress 进度
     * @return 状态更新
     */
    private fun createStatusUpdate(
        status: DeploymentStatus,
        message: String,
        progress: Int
    ): DeploymentStatusUpdate {
        return DeploymentStatusUpdate(
            status = status,
            message = message,
            progress = progress
        )
    }
}
