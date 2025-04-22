package ai.kastrax.deployer.strategy

import ai.kastrax.deployer.DeploymentConfig
import ai.kastrax.deployer.DeploymentResult
import ai.kastrax.deployer.DeploymentStatus
import ai.kastrax.deployer.DeploymentStatusUpdate
import kotlinx.coroutines.flow.Flow
import java.nio.file.Path

/**
 * 部署策略类型。
 */
enum class DeploymentStrategyType {
    DIRECT,      // 直接部署
    BLUE_GREEN,  // 蓝绿部署
    CANARY       // 金丝雀部署
}

/**
 * 部署策略接口。
 */
interface DeploymentStrategy {
    /**
     * 策略类型。
     */
    val type: DeploymentStrategyType
    
    /**
     * 执行部署。
     *
     * @param projectPath 项目路径
     * @param config 部署配置
     * @return 部署状态更新流
     */
    fun deploy(projectPath: Path, config: DeploymentConfig): Flow<DeploymentStatusUpdate>
    
    /**
     * 回滚部署。
     *
     * @param deploymentId 部署 ID
     * @return 是否成功
     */
    suspend fun rollback(deploymentId: String): Boolean
    
    /**
     * 获取部署结果。
     *
     * @param deploymentId 部署 ID
     * @return 部署结果
     */
    suspend fun getResult(deploymentId: String): DeploymentResult
}
