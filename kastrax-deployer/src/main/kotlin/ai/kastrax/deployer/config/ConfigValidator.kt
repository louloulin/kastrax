package ai.kastrax.deployer.config

import ai.kastrax.deployer.DeploymentConfig
import ai.kastrax.deployer.ResourceConfig
import ai.kastrax.deployer.docker.DockerConfig
import ai.kastrax.deployer.kubernetes.KubernetesConfig
import ai.kastrax.deployer.serverless.LambdaConfig
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 配置验证结果。
 *
 * @property valid 是否有效
 * @property errors 错误列表
 * @property warnings 警告列表
 */
data class ValidationResult(
    val valid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

/**
 * 配置验证器。
 */
object ConfigValidator {
    
    /**
     * 验证部署配置。
     *
     * @param config 部署配置
     * @return 验证结果
     */
    fun validateDeploymentConfig(config: DeploymentConfig): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // 验证应用名称
        if (config.name.isBlank()) {
            errors.add("Application name cannot be empty")
        } else if (!isValidName(config.name)) {
            errors.add("Invalid application name: ${config.name}. Name should contain only letters, numbers, hyphens, and underscores.")
        }
        
        // 验证版本
        if (config.version.isBlank()) {
            errors.add("Version cannot be empty")
        } else if (!isValidVersion(config.version)) {
            errors.add("Invalid version format: ${config.version}. Version should follow semantic versioning (e.g., 1.0.0).")
        }
        
        // 验证资源配置
        validateResourceConfig(config.resources).let {
            errors.addAll(it.errors)
            warnings.addAll(it.warnings)
        }
        
        // 验证环境变量
        for ((key, value) in config.environment) {
            if (key.isBlank()) {
                errors.add("Environment variable key cannot be empty")
            }
            
            if (key.contains(" ")) {
                errors.add("Environment variable key cannot contain spaces: $key")
            }
        }
        
        return ValidationResult(
            valid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * 验证资源配置。
     *
     * @param config 资源配置
     * @return 验证结果
     */
    fun validateResourceConfig(config: ResourceConfig): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // 验证内存
        if (config.memory <= 0) {
            errors.add("Memory must be greater than 0")
        } else if (config.memory < 128) {
            warnings.add("Memory is very low (${config.memory} MB), recommended minimum is 128 MB")
        }
        
        // 验证 CPU
        if (config.cpu <= 0) {
            errors.add("CPU must be greater than 0")
        }
        
        // 验证超时
        if (config.timeout <= 0) {
            errors.add("Timeout must be greater than 0")
        } else if (config.timeout > 900) {
            warnings.add("Timeout is very high (${config.timeout} seconds), some platforms may not support timeouts longer than 15 minutes")
        }
        
        // 验证并发
        if (config.concurrency <= 0) {
            errors.add("Concurrency must be greater than 0")
        } else if (config.concurrency > 100) {
            warnings.add("Concurrency is very high (${config.concurrency}), consider if this is intentional")
        }
        
        return ValidationResult(
            valid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * 验证 Docker 配置。
     *
     * @param config Docker 配置
     * @return 验证结果
     */
    fun validateDockerConfig(config: DockerConfig): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // 验证基础镜像
        if (config.baseImage.isBlank()) {
            errors.add("Base image cannot be empty")
        }
        
        // 验证端口
        if (config.port <= 0 || config.port > 65535) {
            errors.add("Port must be between 1 and 65535")
        }
        
        if (config.hostPort <= 0 || config.hostPort > 65535) {
            errors.add("Host port must be between 1 and 65535")
        }
        
        // 检查常见端口冲突
        val commonPorts = listOf(80, 443, 8080, 3000, 5000)
        if (config.hostPort in commonPorts) {
            warnings.add("Host port ${config.hostPort} is commonly used by other services, potential conflict")
        }
        
        // 验证 Dockerfile 路径
        if (config.dockerfilePath != null && config.dockerfilePath.isBlank()) {
            errors.add("Dockerfile path cannot be empty if provided")
        }
        
        return ValidationResult(
            valid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * 验证 Kubernetes 配置。
     *
     * @param config Kubernetes 配置
     * @return 验证结果
     */
    fun validateKubernetesConfig(config: KubernetesConfig): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // 验证命名空间
        if (config.namespace.isBlank()) {
            errors.add("Namespace cannot be empty")
        } else if (!isValidName(config.namespace)) {
            errors.add("Invalid namespace: ${config.namespace}. Namespace should contain only lowercase letters, numbers, and hyphens.")
        }
        
        // 验证副本数
        if (config.replicas <= 0) {
            errors.add("Replicas must be greater than 0")
        } else if (config.replicas == 1) {
            warnings.add("Single replica deployment will not have high availability")
        }
        
        // 验证服务类型
        val validServiceTypes = listOf("ClusterIP", "NodePort", "LoadBalancer", "ExternalName")
        if (config.serviceType !in validServiceTypes) {
            errors.add("Invalid service type: ${config.serviceType}. Valid types are: ${validServiceTypes.joinToString()}")
        }
        
        // 验证 Docker 配置
        validateDockerConfig(config.dockerConfig).let {
            errors.addAll(it.errors)
            warnings.addAll(it.warnings)
        }
        
        return ValidationResult(
            valid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * 验证 Lambda 配置。
     *
     * @param config Lambda 配置
     * @return 验证结果
     */
    fun validateLambdaConfig(config: LambdaConfig): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // 验证区域
        if (config.region.isBlank()) {
            errors.add("Region cannot be empty")
        }
        
        // 验证运行时
        val validRuntimes = listOf("java8", "java11", "java17")
        if (config.runtime !in validRuntimes) {
            errors.add("Invalid runtime: ${config.runtime}. Valid runtimes are: ${validRuntimes.joinToString()}")
        }
        
        // 验证处理函数
        if (config.handler.isBlank()) {
            errors.add("Handler cannot be empty")
        } else if (!config.handler.contains("::")) {
            errors.add("Invalid handler format: ${config.handler}. Format should be 'package.Class::method'")
        }
        
        // 验证角色
        if (config.role.isBlank()) {
            errors.add("Role cannot be empty")
        } else if (!config.role.startsWith("arn:aws:iam::")) {
            errors.add("Invalid role ARN format: ${config.role}")
        }
        
        // 验证存储桶名称
        if (config.bucketName.isBlank()) {
            errors.add("Bucket name cannot be empty")
        } else if (!isValidBucketName(config.bucketName)) {
            errors.add("Invalid bucket name: ${config.bucketName}. Bucket names must be between 3 and 63 characters long and can contain only lowercase letters, numbers, periods, and hyphens.")
        }
        
        return ValidationResult(
            valid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * 检查名称是否有效。
     *
     * @param name 名称
     * @return 是否有效
     */
    private fun isValidName(name: String): Boolean {
        return name.matches(Regex("^[a-zA-Z0-9_-]+$"))
    }
    
    /**
     * 检查版本是否有效。
     *
     * @param version 版本
     * @return 是否有效
     */
    private fun isValidVersion(version: String): Boolean {
        return version.matches(Regex("^\\d+\\.\\d+\\.\\d+(-[a-zA-Z0-9]+)?\$"))
    }
    
    /**
     * 检查存储桶名称是否有效。
     *
     * @param bucketName 存储桶名称
     * @return 是否有效
     */
    private fun isValidBucketName(bucketName: String): Boolean {
        return bucketName.matches(Regex("^[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]$"))
    }
}
