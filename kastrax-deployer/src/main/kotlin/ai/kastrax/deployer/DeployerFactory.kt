package ai.kastrax.deployer

import ai.kastrax.deployer.config.ConfigValidator
import ai.kastrax.deployer.docker.DockerConfig
import ai.kastrax.deployer.docker.DockerDeployer
import ai.kastrax.deployer.kubernetes.KubernetesConfig
import ai.kastrax.deployer.kubernetes.KubernetesDeployer
import ai.kastrax.deployer.packaging.JarPackagingSystem
import ai.kastrax.deployer.packaging.PackagingSystem
import ai.kastrax.deployer.serverless.LambdaConfig
import ai.kastrax.deployer.serverless.LambdaDeployer
import ai.kastrax.deployer.strategy.BlueGreenConfig
import ai.kastrax.deployer.strategy.BlueGreenDeploymentStrategy
import ai.kastrax.deployer.strategy.DeploymentStrategy
import ai.kastrax.deployer.strategy.DeploymentStrategyType
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * 部署器类型。
 */
enum class DeployerType {
    DOCKER,
    KUBERNETES,
    LAMBDA
}

/**
 * 打包系统类型。
 */
enum class PackagingSystemType {
    JAR
}

/**
 * 部署器工厂，用于创建不同类型的部署器。
 */
object DeployerFactory {

    /**
     * 创建部署器。
     *
     * @param type 部署器类型
     * @param configPath 配置文件路径，可选
     * @return 部署器
     */
    fun createDeployer(type: DeployerType, configPath: String? = null): Deployer {
        val config = loadConfig(configPath)

        return when (type) {
            DeployerType.DOCKER -> createDockerDeployer(config)
            DeployerType.KUBERNETES -> createKubernetesDeployer(config)
            DeployerType.LAMBDA -> createLambdaDeployer(config)
        }
    }

    /**
     * 创建部署策略。
     *
     * @param type 策略类型
     * @param deployer 部署器
     * @param configPath 配置文件路径，可选
     * @return 部署策略
     */
    fun createDeploymentStrategy(
        type: DeploymentStrategyType,
        deployer: Deployer,
        configPath: String? = null
    ): DeploymentStrategy {
        val config = loadConfig(configPath)

        return when (type) {
            DeploymentStrategyType.BLUE_GREEN -> createBlueGreenStrategy(deployer, config)
            DeploymentStrategyType.CANARY -> throw UnsupportedOperationException("Canary deployment not implemented yet")
            DeploymentStrategyType.DIRECT -> throw UnsupportedOperationException("Direct deployment strategy not implemented yet")
        }
    }

    /**
     * 创建打包系统。
     *
     * @param type 打包系统类型
     * @return 打包系统
     */
    fun createPackagingSystem(type: PackagingSystemType): PackagingSystem {
        return when (type) {
            PackagingSystemType.JAR -> JarPackagingSystem()
        }
    }

    /**
     * 加载配置。
     *
     * @param configPath 配置文件路径，可选
     * @return 配置对象
     */
    private fun loadConfig(configPath: String?): Config {
        return if (configPath != null && File(configPath).exists()) {
            logger.info { "Loading config from: $configPath" }
            ConfigFactory.parseFile(File(configPath))
        } else {
            logger.info { "Using default config" }
            ConfigFactory.load()
        }
    }

    /**
     * 创建 Docker 部署器。
     *
     * @param config 配置对象
     * @return Docker 部署器
     */
    private fun createDockerDeployer(config: Config): DockerDeployer {
        val dockerConfig = if (config.hasPath("docker")) {
            val dockerConf = config.getConfig("docker")
            DockerConfig(
                baseImage = dockerConf.getString("baseImage"),
                port = dockerConf.getInt("port"),
                hostPort = dockerConf.getInt("hostPort"),
                dockerfilePath = if (dockerConf.hasPath("dockerfilePath")) {
                    dockerConf.getString("dockerfilePath")
                } else {
                    null
                }
            )
        } else {
            DockerConfig()
        }

        // 验证配置
        val validationResult = ConfigValidator.validateDockerConfig(dockerConfig)
        if (!validationResult.valid) {
            logger.warn { "Docker configuration validation failed: ${validationResult.errors.joinToString()}" }
            if (validationResult.warnings.isNotEmpty()) {
                logger.warn { "Warnings: ${validationResult.warnings.joinToString()}" }
            }
        }

        return DockerDeployer(dockerConfig)
    }

    /**
     * 创建 Kubernetes 部署器。
     *
     * @param config 配置对象
     * @return Kubernetes 部署器
     */
    private fun createKubernetesDeployer(config: Config): KubernetesDeployer {
        val kubernetesConfig = if (config.hasPath("kubernetes")) {
            val k8sConf = config.getConfig("kubernetes")
            KubernetesConfig(
                namespace = k8sConf.getString("namespace"),
                replicas = k8sConf.getInt("replicas"),
                serviceType = k8sConf.getString("serviceType"),
                dockerConfig = if (k8sConf.hasPath("docker")) {
                    val dockerConf = k8sConf.getConfig("docker")
                    DockerConfig(
                        baseImage = dockerConf.getString("baseImage"),
                        port = dockerConf.getInt("port"),
                        hostPort = dockerConf.getInt("hostPort"),
                        dockerfilePath = if (dockerConf.hasPath("dockerfilePath")) {
                            dockerConf.getString("dockerfilePath")
                        } else {
                            null
                        }
                    )
                } else {
                    DockerConfig()
                }
            )
        } else {
            KubernetesConfig()
        }

        // 验证配置
        val validationResult = ConfigValidator.validateKubernetesConfig(kubernetesConfig)
        if (!validationResult.valid) {
            logger.warn { "Kubernetes configuration validation failed: ${validationResult.errors.joinToString()}" }
            if (validationResult.warnings.isNotEmpty()) {
                logger.warn { "Warnings: ${validationResult.warnings.joinToString()}" }
            }
        }

        return KubernetesDeployer(kubernetesConfig)
    }

    /**
     * 创建 Lambda 部署器。
     *
     * @param config 配置对象
     * @return Lambda 部署器
     */
    private fun createLambdaDeployer(config: Config): LambdaDeployer {
        val lambdaConfig = if (config.hasPath("lambda")) {
            val lambdaConf = config.getConfig("lambda")
            LambdaConfig(
                region = lambdaConf.getString("region"),
                runtime = lambdaConf.getString("runtime"),
                handler = lambdaConf.getString("handler"),
                role = lambdaConf.getString("role"),
                bucketName = lambdaConf.getString("bucketName")
            )
        } else {
            LambdaConfig()
        }

        // 验证配置
        val validationResult = ConfigValidator.validateLambdaConfig(lambdaConfig)
        if (!validationResult.valid) {
            logger.warn { "Lambda configuration validation failed: ${validationResult.errors.joinToString()}" }
            if (validationResult.warnings.isNotEmpty()) {
                logger.warn { "Warnings: ${validationResult.warnings.joinToString()}" }
            }
        }

        return LambdaDeployer(lambdaConfig)
    }

    /**
     * 创建蓝绿部署策略。
     *
     * @param deployer 部署器
     * @param config 配置对象
     * @return 蓝绿部署策略
     */
    private fun createBlueGreenStrategy(deployer: Deployer, config: Config): BlueGreenDeploymentStrategy {
        val blueGreenConfig = if (config.hasPath("blueGreen")) {
            val bgConf = config.getConfig("blueGreen")
            BlueGreenConfig(
                healthCheckUrl = if (bgConf.hasPath("healthCheckUrl")) bgConf.getString("healthCheckUrl") else null,
                healthCheckTimeout = if (bgConf.hasPath("healthCheckTimeout")) bgConf.getInt("healthCheckTimeout") else 60,
                switchDelay = if (bgConf.hasPath("switchDelay")) bgConf.getInt("switchDelay") else 10,
                autoSwitch = if (bgConf.hasPath("autoSwitch")) bgConf.getBoolean("autoSwitch") else true
            )
        } else {
            BlueGreenConfig()
        }

        return BlueGreenDeploymentStrategy(deployer, blueGreenConfig)
    }
}
