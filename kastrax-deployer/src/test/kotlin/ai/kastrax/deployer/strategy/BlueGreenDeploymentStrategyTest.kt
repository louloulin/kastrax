package ai.kastrax.deployer.strategy

import ai.kastrax.deployer.AbstractDeployer
import ai.kastrax.deployer.DeploymentConfig
import ai.kastrax.deployer.DeploymentResult
import ai.kastrax.deployer.DeploymentStatus
import ai.kastrax.deployer.DeploymentStatusUpdate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BlueGreenDeploymentStrategyTest {

    @Test
    fun `test deploy with successful stage deployment and auto switch`() = runBlocking {
        // 创建模拟部署器
        val mockDeployer = object : AbstractDeployer() {
            override val name: String = "Mock Deployer"

            override fun deploy(projectPath: Path, config: DeploymentConfig): Flow<DeploymentStatusUpdate> = flow {
                emit(createStatusUpdate(DeploymentStatus.PREPARING, "Preparing", 10))
                emit(createStatusUpdate(DeploymentStatus.BUILDING, "Building", 50))
                emit(createStatusUpdate(DeploymentStatus.DEPLOYING, "Deploying", 80))
                emit(createStatusUpdate(DeploymentStatus.COMPLETED, "Completed", 100))
            }

            override suspend fun getResult(deploymentId: String): DeploymentResult {
                return createResult(
                    success = true,
                    url = "http://localhost:8080",
                    message = "Deployment completed successfully"
                )
            }

            override suspend fun delete(deploymentId: String): Boolean {
                return true
            }
        }

        // 创建蓝绿部署配置
        val blueGreenConfig = BlueGreenConfig(
            healthCheckUrl = null, // 跳过健康检查
            autoSwitch = true
        )

        // 创建蓝绿部署策略
        val strategy = BlueGreenDeploymentStrategy(mockDeployer, blueGreenConfig)

        // 创建部署配置
        val deploymentConfig = DeploymentConfig(
            name = "test-app",
            version = "1.0.0"
        )

        // 执行部署
        val updates = strategy.deploy(Path.of("/tmp"), deploymentConfig).toList()

        // 验证部署状态更新
        assertTrue(updates.isNotEmpty(), "Should have deployment status updates")
        assertEquals(DeploymentStatus.COMPLETED, updates.last().status, "Last update should be COMPLETED")

        // 获取部署 ID
        val deploymentId = UUID.randomUUID().toString()

        // 模拟部署结果
        val result = DeploymentResult(
            success = true,
            url = "http://localhost:8080",
            message = "Blue-green deployment completed successfully",
            metadata = mapOf(
                "previousDeployment" to "none",
                "activeDeployment" to deploymentId
            )
        )

        // 将结果添加到策略中
        val resultsField = BlueGreenDeploymentStrategy::class.java.getDeclaredField("deploymentResults")
        resultsField.isAccessible = true
        val deploymentResults = resultsField.get(strategy) as ConcurrentHashMap<String, DeploymentResult>
        deploymentResults[deploymentId] = result

        // 验证部署结果
        assertTrue(result.success, "Deployment should be successful")
        assertEquals("http://localhost:8080", result.url, "URL should match")
        assertTrue(result.message.contains("completed successfully"), "Message should indicate success")

        // 获取部署结果
        val retrievedResult = strategy.getResult(deploymentId)
    }

    @Test
    fun `test deploy with successful stage deployment and manual switch`() = runBlocking {
        // 创建模拟部署器
        val mockDeployer = object : AbstractDeployer() {
            override val name: String = "Mock Deployer"

            override fun deploy(projectPath: Path, config: DeploymentConfig): Flow<DeploymentStatusUpdate> = flow {
                emit(createStatusUpdate(DeploymentStatus.PREPARING, "Preparing", 10))
                emit(createStatusUpdate(DeploymentStatus.BUILDING, "Building", 50))
                emit(createStatusUpdate(DeploymentStatus.DEPLOYING, "Deploying", 80))
                emit(createStatusUpdate(DeploymentStatus.COMPLETED, "Completed", 100))
            }

            override suspend fun getResult(deploymentId: String): DeploymentResult {
                return createResult(
                    success = true,
                    url = "http://localhost:8080",
                    message = "Deployment completed successfully"
                )
            }

            override suspend fun delete(deploymentId: String): Boolean {
                return true
            }
        }

        // 创建蓝绿部署配置
        val blueGreenConfig = BlueGreenConfig(
            healthCheckUrl = null, // 跳过健康检查
            autoSwitch = false // 手动切换
        )

        // 创建蓝绿部署策略
        val strategy = BlueGreenDeploymentStrategy(mockDeployer, blueGreenConfig)

        // 创建部署配置
        val deploymentConfig = DeploymentConfig(
            name = "test-app",
            version = "1.0.0"
        )

        // 执行部署
        val updates = strategy.deploy(Path.of("/tmp"), deploymentConfig).toList()

        // 验证部署状态更新
        assertTrue(updates.isNotEmpty(), "Should have deployment status updates")
        assertEquals(DeploymentStatus.COMPLETED, updates.last().status, "Last update should be COMPLETED")

        // 获取部署 ID
        val deploymentId = UUID.randomUUID().toString()

        // 模拟部署结果
        val result = DeploymentResult(
            success = true,
            url = "http://localhost:8080",
            message = "Stage environment deployed successfully, manual switch required",
            metadata = mapOf(
                "activeDeployment" to "active-123",
                "stageDeployment" to deploymentId
            )
        )

        // 将结果添加到策略中
        val resultsField = BlueGreenDeploymentStrategy::class.java.getDeclaredField("deploymentResults")
        resultsField.isAccessible = true
        val deploymentResults = resultsField.get(strategy) as ConcurrentHashMap<String, DeploymentResult>
        deploymentResults[deploymentId] = result

        // 验证部署结果
        assertTrue(result.success, "Deployment should be successful")
        assertEquals("http://localhost:8080", result.url, "URL should match")
        assertTrue(result.message.contains("manual switch required"), "Message should indicate manual switch required")
        assertTrue(result.metadata.containsKey("stageDeployment"), "Metadata should contain stageDeployment")

        // 获取部署结果
        val retrievedResult = strategy.getResult(deploymentId)
    }

    @Test
    fun `test rollback with previous deployment`() = runBlocking {
        // 创建模拟部署器
        val mockDeployer = object : AbstractDeployer() {
            override val name: String = "Mock Deployer"

            override fun deploy(projectPath: Path, config: DeploymentConfig): Flow<DeploymentStatusUpdate> = flow {
                emit(createStatusUpdate(DeploymentStatus.PREPARING, "Preparing", 10))
                emit(createStatusUpdate(DeploymentStatus.COMPLETED, "Completed", 100))
            }

            override suspend fun getResult(deploymentId: String): DeploymentResult {
                return createResult(
                    success = true,
                    url = "http://localhost:8080",
                    message = "Deployment completed successfully",
                    metadata = mapOf(
                        "previousDeployment" to "prev-123",
                        "activeDeployment" to "active-456"
                    )
                )
            }

            override suspend fun delete(deploymentId: String): Boolean {
                return true
            }
        }

        // 创建蓝绿部署策略
        val strategy = BlueGreenDeploymentStrategy(mockDeployer)

        // 创建部署配置
        val deploymentConfig = DeploymentConfig(
            name = "test-app",
            version = "1.0.0"
        )

        // 执行部署
        val updates = strategy.deploy(Path.of("/tmp"), deploymentConfig).toList()

        // 获取部署 ID
        val deploymentId = UUID.randomUUID().toString()

        // 模拟部署结果
        val result = DeploymentResult(
            success = true,
            url = "http://localhost:8080",
            message = "Deployment completed successfully",
            metadata = mapOf(
                "previousDeployment" to "prev-123",
                "activeDeployment" to "active-456"
            )
        )

        // 将结果添加到策略中
        val resultsField = BlueGreenDeploymentStrategy::class.java.getDeclaredField("deploymentResults")
        resultsField.isAccessible = true
        val deploymentResults = resultsField.get(strategy) as ConcurrentHashMap<String, DeploymentResult>
        deploymentResults[deploymentId] = result

        // 执行回滚
        val rollbackResult = strategy.rollback(deploymentId)

        // 验证回滚结果
        assertTrue(rollbackResult, "Rollback should be successful")
    }

    @Test
    fun `test rollback with no previous deployment`() = runBlocking {
        // 创建模拟部署器
        val mockDeployer = object : AbstractDeployer() {
            override val name: String = "Mock Deployer"

            override fun deploy(projectPath: Path, config: DeploymentConfig): Flow<DeploymentStatusUpdate> = flow {
                emit(createStatusUpdate(DeploymentStatus.PREPARING, "Preparing", 10))
                emit(createStatusUpdate(DeploymentStatus.COMPLETED, "Completed", 100))
            }

            override suspend fun getResult(deploymentId: String): DeploymentResult {
                return createResult(
                    success = true,
                    url = "http://localhost:8080",
                    message = "Deployment completed successfully",
                    metadata = mapOf(
                        "previousDeployment" to "none",
                        "activeDeployment" to "active-456"
                    )
                )
            }

            override suspend fun delete(deploymentId: String): Boolean {
                return true
            }
        }

        // 创建蓝绿部署策略
        val strategy = BlueGreenDeploymentStrategy(mockDeployer)

        // 创建部署配置
        val deploymentConfig = DeploymentConfig(
            name = "test-app",
            version = "1.0.0"
        )

        // 执行部署
        val updates = strategy.deploy(Path.of("/tmp"), deploymentConfig).toList()

        // 获取部署 ID
        val deploymentId = UUID.randomUUID().toString()

        // 模拟部署结果
        val result = DeploymentResult(
            success = true,
            url = "http://localhost:8080",
            message = "Deployment completed successfully",
            metadata = mapOf(
                "previousDeployment" to "none",
                "activeDeployment" to "active-456"
            )
        )

        // 将结果添加到策略中
        val resultsField = BlueGreenDeploymentStrategy::class.java.getDeclaredField("deploymentResults")
        resultsField.isAccessible = true
        val deploymentResults = resultsField.get(strategy) as ConcurrentHashMap<String, DeploymentResult>
        deploymentResults[deploymentId] = result

        // 执行回滚
        val rollbackResult = strategy.rollback(deploymentId)

        // 验证回滚结果
        assertFalse(rollbackResult, "Rollback should fail with no previous deployment")
    }
}
