package ai.kastrax.deployer.kubernetes

import ai.kastrax.deployer.AbstractDeployer
import ai.kastrax.deployer.DeploymentConfig
import ai.kastrax.deployer.DeploymentResult
import ai.kastrax.deployer.DeploymentStatus
import ai.kastrax.deployer.DeploymentStatusUpdate
import ai.kastrax.deployer.docker.DockerConfig
import ai.kastrax.deployer.docker.DockerDeployer
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kubernetes.client.openapi.ApiClient
import io.kubernetes.client.openapi.ApiException
import io.kubernetes.client.openapi.Configuration
import io.kubernetes.client.openapi.apis.AppsV1Api
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.models.V1Container
import io.kubernetes.client.openapi.models.V1ContainerPort
import io.kubernetes.client.openapi.models.V1Deployment
import io.kubernetes.client.openapi.models.V1DeploymentSpec
import io.kubernetes.client.openapi.models.V1EnvVar
import io.kubernetes.client.openapi.models.V1LabelSelector
import io.kubernetes.client.openapi.models.V1ObjectMeta
import io.kubernetes.client.openapi.models.V1PodSpec
import io.kubernetes.client.openapi.models.V1PodTemplateSpec
import io.kubernetes.client.openapi.models.V1ResourceRequirements
import io.kubernetes.client.openapi.models.V1Service
import io.kubernetes.client.openapi.models.V1ServicePort
import io.kubernetes.client.openapi.models.V1ServiceSpec
import io.kubernetes.client.util.Config
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.nio.file.Path
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * Kubernetes 部署配置。
 *
 * @property namespace 命名空间
 * @property replicas 副本数
 * @property serviceType 服务类型
 * @property dockerConfig Docker 配置
 */
data class KubernetesConfig(
    val namespace: String = "default",
    val replicas: Int = 1,
    val serviceType: String = "ClusterIP",
    val dockerConfig: DockerConfig = DockerConfig()
)

/**
 * Kubernetes 部署器。
 *
 * @property kubernetesConfig Kubernetes 配置
 */
class KubernetesDeployer(
    private val kubernetesConfig: KubernetesConfig = KubernetesConfig()
) : AbstractDeployer() {

    override val name: String = "Kubernetes Deployer"

    private val apiClient: ApiClient
    private val coreV1Api: CoreV1Api
    private val appsV1Api: AppsV1Api
    private val dockerDeployer: DockerDeployer

    private val deploymentResults = mutableMapOf<String, DeploymentResult>()

    init {
        apiClient = Config.defaultClient()
        Configuration.setDefaultApiClient(apiClient)

        coreV1Api = CoreV1Api(apiClient)
        appsV1Api = AppsV1Api(apiClient)

        dockerDeployer = DockerDeployer(kubernetesConfig.dockerConfig)
    }

    override fun deploy(projectPath: Path, config: DeploymentConfig): Flow<DeploymentStatusUpdate> = flow {
        val deploymentId = UUID.randomUUID().toString()
        logger.info { "Starting deployment with ID: $deploymentId" }

        try {
            // 准备阶段
            emit(createStatusUpdate(DeploymentStatus.PREPARING, "Preparing deployment", 10))

            // 构建 Docker 镜像
            emit(createStatusUpdate(DeploymentStatus.BUILDING, "Building Docker image", 20))
            val imageName = "${config.name}:${config.version}"

            // 使用 Docker 部署器构建镜像
            var imageId: String? = null
            dockerDeployer.deploy(projectPath, config).collect { update ->
                if (update.status == DeploymentStatus.COMPLETED) {
                    val result = dockerDeployer.getResult(deploymentId)
                    imageId = result.metadata["imageId"]
                }

                // 转发构建状态更新
                if (update.status == DeploymentStatus.BUILDING) {
                    emit(update)
                }
            }

            if (imageId == null) {
                throw RuntimeException("Failed to build Docker image")
            }

            // 部署到 Kubernetes
            emit(createStatusUpdate(DeploymentStatus.DEPLOYING, "Deploying to Kubernetes", 50))

            // 创建或更新 Deployment
            val deploymentName = "${config.name}-${config.version}".replace(".", "-").lowercase()
            val deployment = createDeployment(deploymentName, imageName, config)

            try {
                // 尝试获取现有 Deployment
                appsV1Api.readNamespacedDeployment(
                    deploymentName,
                    kubernetesConfig.namespace,
                    null
                )

                // 如果存在，则更新
                logger.info { "Updating existing deployment: $deploymentName" }
                appsV1Api.replaceNamespacedDeployment(
                    deploymentName,
                    kubernetesConfig.namespace,
                    deployment,
                    null,
                    null,
                    null,
                    null
                )
            } catch (e: ApiException) {
                if (e.code == 404) {
                    // 如果不存在，则创建
                    logger.info { "Creating new deployment: $deploymentName" }
                    appsV1Api.createNamespacedDeployment(
                        kubernetesConfig.namespace,
                        deployment,
                        null,
                        null,
                        null,
                        null
                    )
                } else {
                    throw e
                }
            }

            // 创建或更新 Service
            emit(createStatusUpdate(DeploymentStatus.DEPLOYING, "Creating Kubernetes service", 70))
            val service = createService(deploymentName, config)

            try {
                // 尝试获取现有 Service
                coreV1Api.readNamespacedService(
                    deploymentName,
                    kubernetesConfig.namespace,
                    null
                )

                // 如果存在，则更新
                logger.info { "Updating existing service: $deploymentName" }
                coreV1Api.replaceNamespacedService(
                    deploymentName,
                    kubernetesConfig.namespace,
                    service,
                    null,
                    null,
                    null,
                    null
                )
            } catch (e: ApiException) {
                if (e.code == 404) {
                    // 如果不存在，则创建
                    logger.info { "Creating new service: $deploymentName" }
                    coreV1Api.createNamespacedService(
                        kubernetesConfig.namespace,
                        service,
                        null,
                        null,
                        null,
                        null
                    )
                } else {
                    throw e
                }
            }

            // 等待 Deployment 就绪
            emit(createStatusUpdate(DeploymentStatus.DEPLOYING, "Waiting for deployment to be ready", 80))
            waitForDeploymentReady(deploymentName)

            // 获取服务 URL
            val serviceUrl = getServiceUrl(deploymentName)

            // 完成阶段
            val result = createResult(
                success = true,
                url = serviceUrl,
                message = "Deployment completed successfully",
                metadata = mapOf(
                    "deploymentName" to deploymentName,
                    "namespace" to kubernetesConfig.namespace,
                    "imageName" to imageName,
                    "serviceType" to kubernetesConfig.serviceType,
                    "serviceUrl" to (serviceUrl ?: "")
                )
            )
            deploymentResults[deploymentId] = result

            emit(createStatusUpdate(DeploymentStatus.COMPLETED, "Deployment completed", 100))
        } catch (e: Exception) {
            logger.error(e) { "Deployment failed" }
            val result = createResult(
                success = false,
                message = "Deployment failed: ${e.message}"
            )
            deploymentResults[deploymentId] = result

            emit(createStatusUpdate(DeploymentStatus.FAILED, "Deployment failed: ${e.message}", 0))
        }
    }

    override suspend fun getResult(deploymentId: String): DeploymentResult {
        return deploymentResults[deploymentId] ?: createResult(
            success = false,
            message = "Deployment not found"
        )
    }

    override suspend fun delete(deploymentId: String): Boolean {
        val result = deploymentResults[deploymentId] ?: return false
        val deploymentName = result.metadata["deploymentName"] ?: return false
        val namespace = result.metadata["namespace"] ?: kubernetesConfig.namespace

        return try {
            // 删除 Service
            try {
                coreV1Api.deleteNamespacedService(
                    deploymentName,
                    namespace,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                )
                logger.info { "Deleted service: $deploymentName" }
            } catch (e: ApiException) {
                if (e.code != 404) {
                    logger.error(e) { "Failed to delete service: $deploymentName" }
                }
            }

            // 删除 Deployment
            try {
                appsV1Api.deleteNamespacedDeployment(
                    deploymentName,
                    namespace,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                )
                logger.info { "Deleted deployment: $deploymentName" }
            } catch (e: ApiException) {
                if (e.code != 404) {
                    logger.error(e) { "Failed to delete deployment: $deploymentName" }
                    return false
                }
            }

            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to delete deployment: $deploymentName" }
            false
        }
    }

    /**
     * 创建 Kubernetes Deployment。
     *
     * @param name Deployment 名称
     * @param imageName 镜像名称
     * @param config 部署配置
     * @return Deployment 对象
     */
    private fun createDeployment(name: String, imageName: String, config: DeploymentConfig): V1Deployment {
        // 创建容器
        val container = V1Container()
            .name(name)
            .image(imageName)
            .ports(listOf(
                V1ContainerPort().containerPort(kubernetesConfig.dockerConfig.port)
            ))
            .env(config.environment.entries.map { (key, value) ->
                V1EnvVar().name(key).value(value)
            })
            .resources(
                V1ResourceRequirements()
                    .putRequestsItem("memory", io.kubernetes.client.custom.Quantity("${config.resources.memory}Mi"))
                    .putRequestsItem("cpu", io.kubernetes.client.custom.Quantity("${config.resources.cpu}"))
                    .putLimitsItem("memory", io.kubernetes.client.custom.Quantity("${config.resources.memory * 2}Mi"))
                    .putLimitsItem("cpu", io.kubernetes.client.custom.Quantity("${config.resources.cpu * 2}"))
            )

        // 创建标签
        val labels = mapOf(
            "app" to name,
            "version" to config.version
        )

        // 创建 Deployment
        return V1Deployment()
            .metadata(
                V1ObjectMeta()
                    .name(name)
                    .labels(labels)
            )
            .spec(
                V1DeploymentSpec()
                    .replicas(kubernetesConfig.replicas)
                    .selector(
                        V1LabelSelector()
                            .matchLabels(labels)
                    )
                    .template(
                        V1PodTemplateSpec()
                            .metadata(
                                V1ObjectMeta()
                                    .labels(labels)
                            )
                            .spec(
                                V1PodSpec()
                                    .containers(listOf(container))
                            )
                    )
            )
    }

    /**
     * 创建 Kubernetes Service。
     *
     * @param name Service 名称
     * @param config 部署配置
     * @return Service 对象
     */
    private fun createService(name: String, config: DeploymentConfig): V1Service {
        // 创建标签
        val labels = mapOf(
            "app" to name,
            "version" to config.version
        )

        // 创建 Service
        return V1Service()
            .metadata(
                V1ObjectMeta()
                    .name(name)
                    .labels(labels)
            )
            .spec(
                V1ServiceSpec()
                    .type(kubernetesConfig.serviceType)
                    .selector(labels)
                    .ports(listOf(
                        V1ServicePort()
                            .port(kubernetesConfig.dockerConfig.port)
                            .targetPort(io.kubernetes.client.custom.IntOrString(kubernetesConfig.dockerConfig.port))
                    ))
            )
    }

    /**
     * 等待 Deployment 就绪。
     *
     * @param name Deployment 名称
     */
    private suspend fun waitForDeploymentReady(name: String) {
        val maxAttempts = 30
        var attempts = 0

        while (attempts < maxAttempts) {
            try {
                val deployment = appsV1Api.readNamespacedDeployment(
                    name,
                    kubernetesConfig.namespace,
                    null
                )

                val availableReplicas = deployment.status?.availableReplicas ?: 0
                val desiredReplicas = deployment.spec?.replicas ?: 0

                if (availableReplicas == desiredReplicas && availableReplicas > 0) {
                    logger.info { "Deployment $name is ready" }
                    return
                }

                logger.info { "Waiting for deployment $name to be ready: $availableReplicas/$desiredReplicas" }
            } catch (e: Exception) {
                logger.error(e) { "Error checking deployment status" }
            }

            attempts++
            delay(2000) // 等待 2 秒
        }

        logger.warn { "Deployment $name did not become ready within the timeout period" }
    }

    /**
     * 获取服务 URL。
     *
     * @param name Service 名称
     * @return 服务 URL
     */
    private fun getServiceUrl(name: String): String? {
        return try {
            val service = coreV1Api.readNamespacedService(
                name,
                kubernetesConfig.namespace,
                null
            )

            when (kubernetesConfig.serviceType) {
                "LoadBalancer" -> {
                    val ingress = service.status?.loadBalancer?.ingress?.firstOrNull()
                    val host = ingress?.ip ?: ingress?.hostname
                    val port = service.spec?.ports?.firstOrNull()?.port

                    if (host != null && port != null) {
                        "http://$host:$port"
                    } else {
                        null
                    }
                }
                "NodePort" -> {
                    val nodePort = service.spec?.ports?.firstOrNull()?.nodePort
                    if (nodePort != null) {
                        "http://localhost:$nodePort"
                    } else {
                        null
                    }
                }
                else -> {
                    // 对于 ClusterIP 类型，返回集群内部 URL
                    "http://$name.${kubernetesConfig.namespace}.svc.cluster.local:${kubernetesConfig.dockerConfig.port}"
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error getting service URL" }
            null
        }
    }
}
