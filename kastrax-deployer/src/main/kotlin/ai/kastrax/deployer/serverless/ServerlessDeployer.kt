package ai.kastrax.deployer.serverless

import ai.kastrax.deployer.AbstractDeployer
import ai.kastrax.deployer.DeploymentConfig
import ai.kastrax.deployer.DeploymentResult
import ai.kastrax.deployer.DeploymentStatus
import ai.kastrax.deployer.DeploymentStatusUpdate
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.CreateFunctionRequest
import software.amazon.awssdk.services.lambda.model.DeleteFunctionRequest
import software.amazon.awssdk.services.lambda.model.Environment
import software.amazon.awssdk.services.lambda.model.FunctionCode
import software.amazon.awssdk.services.lambda.model.GetFunctionRequest
import software.amazon.awssdk.services.lambda.model.ResourceNotFoundException
import software.amazon.awssdk.services.lambda.model.Runtime
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private val logger = KotlinLogging.logger {}

/**
 * AWS Lambda 部署配置。
 *
 * @property region AWS 区域
 * @property runtime Lambda 运行时
 * @property handler 处理函数
 * @property role IAM 角色
 * @property bucketName S3 存储桶名称
 */
data class LambdaConfig(
    val region: String = "us-east-1",
    val runtime: String = "java11",
    val handler: String = "ai.kastrax.Handler::handleRequest",
    val role: String = "arn:aws:iam::123456789012:role/lambda-role",
    val bucketName: String = "kastrax-deployments"
)

/**
 * AWS Lambda 部署器。
 *
 * @property lambdaConfig Lambda 配置
 */
class LambdaDeployer(
    private val lambdaConfig: LambdaConfig = LambdaConfig()
) : AbstractDeployer() {
    
    override val name: String = "AWS Lambda Deployer"
    
    private val region = Region.of(lambdaConfig.region)
    private val s3Client = S3Client.builder()
        .region(region)
        .credentialsProvider(DefaultCredentialsProvider.create())
        .build()
    
    private val lambdaClient = LambdaClient.builder()
        .region(region)
        .credentialsProvider(DefaultCredentialsProvider.create())
        .build()
    
    private val deploymentResults = mutableMapOf<String, DeploymentResult>()
    
    override fun deploy(projectPath: Path, config: DeploymentConfig): Flow<DeploymentStatusUpdate> = flow {
        val deploymentId = UUID.randomUUID().toString()
        logger.info { "Starting deployment with ID: $deploymentId" }
        
        try {
            // 准备阶段
            emit(createStatusUpdate(DeploymentStatus.PREPARING, "Preparing deployment", 10))
            
            // 确保 S3 存储桶存在
            ensureBucketExists(lambdaConfig.bucketName)
            
            // 构建阶段
            emit(createStatusUpdate(DeploymentStatus.BUILDING, "Building project", 20))
            val buildResult = buildProject(projectPath)
            if (!buildResult) {
                val result = createResult(
                    success = false,
                    message = "Build failed"
                )
                deploymentResults[deploymentId] = result
                emit(createStatusUpdate(DeploymentStatus.FAILED, "Build failed", 0))
                return@flow
            }
            
            // 打包阶段
            emit(createStatusUpdate(DeploymentStatus.BUILDING, "Packaging application", 40))
            val zipFile = packageApplication(projectPath)
            
            // 上传阶段
            emit(createStatusUpdate(DeploymentStatus.UPLOADING, "Uploading to S3", 60))
            val s3Key = "deployments/${config.name}/${config.version}/${zipFile.name}"
            uploadToS3(zipFile, lambdaConfig.bucketName, s3Key)
            
            // 部署阶段
            emit(createStatusUpdate(DeploymentStatus.DEPLOYING, "Deploying to Lambda", 80))
            val functionName = "${config.name}-${config.version}".replace(".", "-")
            val lambdaArn = deployToLambda(functionName, lambdaConfig.bucketName, s3Key, config)
            
            // 完成阶段
            val apiUrl = "https://${functionName}.lambda-url.${lambdaConfig.region}.on.aws/"
            val result = createResult(
                success = true,
                url = apiUrl,
                message = "Deployment completed successfully",
                metadata = mapOf(
                    "functionName" to functionName,
                    "functionArn" to lambdaArn,
                    "region" to lambdaConfig.region,
                    "s3Bucket" to lambdaConfig.bucketName,
                    "s3Key" to s3Key
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
        val functionName = result.metadata["functionName"] ?: return false
        
        return try {
            val request = DeleteFunctionRequest.builder()
                .functionName(functionName)
                .build()
            
            lambdaClient.deleteFunction(request)
            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to delete function: $functionName" }
            false
        }
    }
    
    /**
     * 确保 S3 存储桶存在。
     *
     * @param bucketName 存储桶名称
     */
    private fun ensureBucketExists(bucketName: String) {
        try {
            val headBucketRequest = HeadBucketRequest.builder()
                .bucket(bucketName)
                .build()
            
            s3Client.headBucket(headBucketRequest)
            logger.info { "Bucket $bucketName already exists" }
        } catch (e: NoSuchBucketException) {
            logger.info { "Creating bucket: $bucketName" }
            val createBucketRequest = CreateBucketRequest.builder()
                .bucket(bucketName)
                .build()
            
            s3Client.createBucket(createBucketRequest)
        }
    }
    
    /**
     * 构建项目。
     *
     * @param projectPath 项目路径
     * @return 是否成功
     */
    private fun buildProject(projectPath: Path): Boolean {
        // 在实际实现中，这里应该调用 Gradle 或 Maven 构建项目
        // 这里简化为检查项目是否存在
        return Files.exists(projectPath)
    }
    
    /**
     * 打包应用。
     *
     * @param projectPath 项目路径
     * @return ZIP 文件
     */
    private fun packageApplication(projectPath: Path): File {
        val buildDir = projectPath.resolve("build")
        val zipFile = File.createTempFile("deployment", ".zip")
        
        ZipOutputStream(Files.newOutputStream(zipFile.toPath())).use { zipOut ->
            // 在实际实现中，这里应该添加构建产物到 ZIP 文件
            // 这里简化为创建一个空的 ZIP 文件
            zipOut.putNextEntry(ZipEntry("placeholder.txt"))
            zipOut.write("Placeholder content".toByteArray())
            zipOut.closeEntry()
        }
        
        return zipFile
    }
    
    /**
     * 上传到 S3。
     *
     * @param file 文件
     * @param bucketName 存储桶名称
     * @param key 对象键
     */
    private fun uploadToS3(file: File, bucketName: String, key: String) {
        val request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .build()
        
        s3Client.putObject(request, file.toPath())
        logger.info { "Uploaded file to S3: s3://$bucketName/$key" }
    }
    
    /**
     * 部署到 Lambda。
     *
     * @param functionName 函数名称
     * @param bucketName 存储桶名称
     * @param s3Key 对象键
     * @param config 部署配置
     * @return Lambda ARN
     */
    private fun deployToLambda(
        functionName: String,
        bucketName: String,
        s3Key: String,
        config: DeploymentConfig
    ): String {
        // 检查函数是否已存在
        try {
            val getFunctionRequest = GetFunctionRequest.builder()
                .functionName(functionName)
                .build()
            
            lambdaClient.getFunction(getFunctionRequest)
            logger.info { "Function $functionName already exists, updating..." }
            
            // 在实际实现中，这里应该更新函数
            // 这里简化为返回函数 ARN
            return "arn:aws:lambda:${lambdaConfig.region}:123456789012:function:$functionName"
        } catch (e: ResourceNotFoundException) {
            // 函数不存在，创建新函数
            logger.info { "Creating new function: $functionName" }
            
            val environment = Environment.builder()
                .variables(config.environment)
                .build()
            
            val functionCode = FunctionCode.builder()
                .s3Bucket(bucketName)
                .s3Key(s3Key)
                .build()
            
            val createFunctionRequest = CreateFunctionRequest.builder()
                .functionName(functionName)
                .description("KastraX application: ${config.name}")
                .runtime(Runtime.fromValue(lambdaConfig.runtime))
                .handler(lambdaConfig.handler)
                .role(lambdaConfig.role)
                .code(functionCode)
                .environment(environment)
                .memorySize(config.resources.memory)
                .timeout(config.resources.timeout)
                .build()
            
            val createFunctionResponse = lambdaClient.createFunction(createFunctionRequest)
            logger.info { "Created function: ${createFunctionResponse.functionArn()}" }
            
            return createFunctionResponse.functionArn()
        }
    }
}
