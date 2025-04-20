package ai.kastrax.deployer

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class DeployerTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `test deployer interface`() {
        // 创建一个简单的部署器实现
        val deployer = object : AbstractDeployer() {
            override val name: String = "Test Deployer"

            override fun deploy(projectPath: Path, config: DeploymentConfig) = kotlinx.coroutines.flow.flow {
                emit(createStatusUpdate(DeploymentStatus.PREPARING, "Preparing", 10))
                emit(createStatusUpdate(DeploymentStatus.COMPLETED, "Completed", 100))
            }

            override suspend fun getResult(deploymentId: String): DeploymentResult {
                return createResult(true, "http://example.com", "Success")
            }

            override suspend fun delete(deploymentId: String): Boolean {
                return true
            }
        }

        // 验证部署器名称
        assertEquals("Test Deployer", deployer.name)
    }

    @Test
    fun `test deployment status updates`() = runBlocking {
        // 创建一个简单的部署器实现
        val deployer = object : AbstractDeployer() {
            override val name: String = "Test Deployer"

            override fun deploy(projectPath: Path, config: DeploymentConfig) = kotlinx.coroutines.flow.flow {
                emit(createStatusUpdate(DeploymentStatus.PREPARING, "Preparing", 10))
                emit(createStatusUpdate(DeploymentStatus.BUILDING, "Building", 30))
                emit(createStatusUpdate(DeploymentStatus.DEPLOYING, "Deploying", 70))
                emit(createStatusUpdate(DeploymentStatus.COMPLETED, "Completed", 100))
            }

            override suspend fun getResult(deploymentId: String): DeploymentResult {
                return createResult(true, "http://example.com", "Success")
            }

            override suspend fun delete(deploymentId: String): Boolean {
                return true
            }
        }

        // 创建测试项目目录
        val projectDir = tempDir.resolve("test-project")
        Files.createDirectories(projectDir)

        // 创建部署配置
        val config = DeploymentConfig(
            name = "test-app",
            version = "1.0.0",
            environment = mapOf("ENV" to "test"),
            resources = ResourceConfig(
                memory = 512,
                cpu = 1,
                timeout = 30,
                concurrency = 10
            )
        )

        // 执行部署
        val updates = deployer.deploy(projectDir, config).toList()

        // 验证状态更新
        assertEquals(4, updates.size)
        assertEquals(DeploymentStatus.PREPARING, updates[0].status)
        assertEquals(DeploymentStatus.BUILDING, updates[1].status)
        assertEquals(DeploymentStatus.DEPLOYING, updates[2].status)
        assertEquals(DeploymentStatus.COMPLETED, updates[3].status)

        assertEquals(10, updates[0].progress)
        assertEquals(30, updates[1].progress)
        assertEquals(70, updates[2].progress)
        assertEquals(100, updates[3].progress)
    }

    @Test
    fun `test deployment result`() = runBlocking {
        // 创建一个简单的部署器实现
        val deployer = object : AbstractDeployer() {
            override val name: String = "Test Deployer"

            override fun deploy(projectPath: Path, config: DeploymentConfig) = kotlinx.coroutines.flow.flow {
                emit(createStatusUpdate(DeploymentStatus.COMPLETED, "Completed", 100))
            }

            override suspend fun getResult(deploymentId: String): DeploymentResult {
                return createResult(
                    success = true,
                    url = "http://example.com",
                    message = "Deployment successful",
                    logs = listOf("Log 1", "Log 2"),
                    metadata = mapOf("key" to "value")
                )
            }

            override suspend fun delete(deploymentId: String): Boolean {
                return true
            }
        }

        // 获取部署结果
        val result = deployer.getResult("test-id")

        // 验证结果
        assertTrue(result.success)
        assertEquals("http://example.com", result.url)
        assertEquals("Deployment successful", result.message)
        assertEquals(2, result.logs.size)
        assertEquals("Log 1", result.logs[0])
        assertEquals("Log 2", result.logs[1])
        assertEquals(1, result.metadata.size)
        assertEquals("value", result.metadata["key"])
    }

    // 暂时禁用工厂测试，因为它依赖于外部服务
    // @Test
    // fun `test deployer factory`() {
    //     // 创建不同类型的部署器
    //     val dockerDeployer = DeployerFactory.createDeployer(DeployerType.DOCKER)
    //     val kubernetesDeployer = DeployerFactory.createDeployer(DeployerType.KUBERNETES)
    //     val lambdaDeployer = DeployerFactory.createDeployer(DeployerType.LAMBDA)
    //
    //     // 验证部署器类型
    //     assertEquals("Docker Deployer", dockerDeployer.name)
    //     assertEquals("Kubernetes Deployer", kubernetesDeployer.name)
    //     assertEquals("AWS Lambda Deployer", lambdaDeployer.name)
    // }
}
