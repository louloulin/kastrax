package ai.kastrax.deployer

import ai.kastrax.deployer.docker.DockerConfig
import ai.kastrax.deployer.docker.DockerDeployer
import ai.kastrax.deployer.kubernetes.KubernetesConfig
import ai.kastrax.deployer.kubernetes.KubernetesDeployer
import ai.kastrax.deployer.serverless.LambdaConfig
import ai.kastrax.deployer.serverless.LambdaDeployer
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
        
        return LambdaDeployer(lambdaConfig)
    }
}
