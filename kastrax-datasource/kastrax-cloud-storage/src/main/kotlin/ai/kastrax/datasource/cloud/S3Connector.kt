package ai.kastrax.datasource.cloud

import ai.kastrax.datasource.common.CloudStorageConnector
import ai.kastrax.datasource.common.DataSourceBase
import ai.kastrax.datasource.common.DataSourceType
import ai.kastrax.datasource.common.FileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.charset.Charset
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture

private val logger = KotlinLogging.logger {}

/**
 * Amazon S3 连接器，提供了与 S3 存储服务交互的功能。
 */
class S3Connector(
    name: String,
    private val bucketName: String,
    private val region: String,
    private val accessKey: String,
    private val secretKey: String,
    private val endpoint: String? = null,
    private val pathStyleAccessEnabled: Boolean = false
) : DataSourceBase(name, DataSourceType.CLOUD_STORAGE), CloudStorageConnector {

    private var s3Client: S3AsyncClient? = null

    override suspend fun doConnect(): Boolean {
        return try {
            val credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
            )

            val builder = S3AsyncClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider)
                .overrideConfiguration { config ->
                    config.apiCallTimeout(Duration.ofSeconds(30))
                    config.apiCallAttemptTimeout(Duration.ofSeconds(20))
                }

            if (endpoint != null) {
                builder.endpointOverride(URI.create(endpoint))
            }

            if (pathStyleAccessEnabled) {
                builder.serviceConfiguration { config ->
                    config.pathStyleAccessEnabled(true)
                }
            }

            s3Client = builder.build()

            // 验证连接
            s3Client?.headBucket(HeadBucketRequest.builder().bucket(bucketName).build())?.await()
            true
        } catch (e: Exception) {
            logger.error(e) { "Error connecting to S3: $bucketName in region $region" }
            false
        }
    }

    override suspend fun doDisconnect(): Boolean {
        return try {
            s3Client?.close()
            s3Client = null
            true
        } catch (e: Exception) {
            logger.error(e) { "Error disconnecting from S3: $bucketName" }
            false
        }
    }

    override suspend fun readFile(path: String): ByteArray = withContext(Dispatchers.IO) {
        logger.debug { "Reading file from S3: $path" }

        val client = s3Client ?: throw IllegalStateException("Not connected to S3")

        try {
            val request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(normalizePath(path))
                .build()

            // Create a temporary file to store the S3 object
            val tempFile = java.nio.file.Files.createTempFile("s3-download-", ".tmp")

            // Download the object to the temporary file
            client.getObject(request, tempFile).await()

            // Read the file content
            val content = java.nio.file.Files.readAllBytes(tempFile)

            // Delete the temporary file
            java.nio.file.Files.delete(tempFile)

            content
        } catch (e: Exception) {
            logger.error(e) { "Error reading file from S3: $path" }
            throw e
        }
    }

    override suspend fun readTextFile(path: String, charset: String): String = withContext(Dispatchers.IO) {
        val bytes = readFile(path)
        String(bytes, Charset.forName(charset))
    }

    override suspend fun writeFile(path: String, content: ByteArray, overwrite: Boolean): Boolean = withContext(Dispatchers.IO) {
        logger.debug { "Writing file to S3: $path" }

        val client = s3Client ?: throw IllegalStateException("Not connected to S3")
        val key = normalizePath(path)

        try {
            // 检查文件是否存在
            if (!overwrite) {
                val exists = try {
                    client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(key).build())
                        .await()
                    true
                } catch (e: NoSuchKeyException) {
                    false
                } catch (e: Exception) {
                    logger.error(e) { "Error checking if file exists: $path" }
                    throw e
                }

                if (exists) {
                    logger.warn { "File already exists and overwrite is false: $path" }
                    return@withContext false
                }
            }

            // 上传文件
            val request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build()

            client.putObject(request, AsyncRequestBody.fromBytes(content))
                .await()

            true
        } catch (e: Exception) {
            logger.error(e) { "Error writing file to S3: $path" }
            false
        }
    }

    override suspend fun writeTextFile(path: String, content: String, charset: String, overwrite: Boolean): Boolean = withContext(Dispatchers.IO) {
        val bytes = content.toByteArray(Charset.forName(charset))
        writeFile(path, bytes, overwrite)
    }

    override suspend fun createDirectory(path: String, createParents: Boolean): Boolean = withContext(Dispatchers.IO) {
        logger.debug { "Creating directory in S3: $path" }

        val client = s3Client ?: throw IllegalStateException("Not connected to S3")
        val key = normalizePath(path) + "/"

        try {
            // S3 没有真正的目录概念，我们创建一个空对象来表示目录
            val request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build()

            client.putObject(request, AsyncRequestBody.fromBytes(ByteArray(0)))
                .await()

            true
        } catch (e: Exception) {
            logger.error(e) { "Error creating directory in S3: $path" }
            false
        }
    }

    override suspend fun delete(path: String, recursive: Boolean): Boolean = withContext(Dispatchers.IO) {
        logger.debug { "Deleting from S3: $path, recursive: $recursive" }

        val client = s3Client ?: throw IllegalStateException("Not connected to S3")
        val key = normalizePath(path)

        try {
            if (recursive) {
                // 列出所有以该路径为前缀的对象
                val listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(key)
                    .build()

                val response = client.listObjectsV2(listRequest).await()
                val objects = response.contents()

                if (objects.isEmpty()) {
                    return@withContext false
                }

                // 删除所有对象
                val objectIdentifiers = objects.map { obj ->
                    ObjectIdentifier.builder().key(obj.key()).build()
                }

                val deleteRequest = DeleteObjectsRequest.builder()
                    .bucket(bucketName)
                    .delete { builder ->
                        builder.objects(objectIdentifiers)
                    }
                    .build()

                client.deleteObjects(deleteRequest).await()
            } else {
                // 删除单个对象
                val deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build()

                client.deleteObject(deleteRequest).await()
            }

            true
        } catch (e: Exception) {
            logger.error(e) { "Error deleting from S3: $path" }
            false
        }
    }

    override suspend fun copy(source: String, destination: String, overwrite: Boolean): Boolean = withContext(Dispatchers.IO) {
        logger.debug { "Copying in S3 from $source to $destination" }

        val client = s3Client ?: throw IllegalStateException("Not connected to S3")
        val sourceKey = normalizePath(source)
        val destinationKey = normalizePath(destination)

        try {
            // 检查目标文件是否存在
            if (!overwrite) {
                val exists = try {
                    client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(destinationKey).build())
                        .await()
                    true
                } catch (e: NoSuchKeyException) {
                    false
                } catch (e: Exception) {
                    logger.error(e) { "Error checking if destination exists: $destination" }
                    throw e
                }

                if (exists) {
                    logger.warn { "Destination already exists and overwrite is false: $destination" }
                    return@withContext false
                }
            }

            // 复制对象
            val request = CopyObjectRequest.builder()
                .sourceBucket(bucketName)
                .sourceKey(sourceKey)
                .destinationBucket(bucketName)
                .destinationKey(destinationKey)
                .build()

            client.copyObject(request).await()

            true
        } catch (e: Exception) {
            logger.error(e) { "Error copying in S3 from $source to $destination" }
            false
        }
    }

    override suspend fun move(source: String, destination: String, overwrite: Boolean): Boolean = withContext(Dispatchers.IO) {
        // 先复制，然后删除源
        val copied = copy(source, destination, overwrite)
        if (copied) {
            delete(source, false)
        }
        copied
    }

    override suspend fun listDirectory(path: String): List<FileInfo> = withContext(Dispatchers.IO) {
        logger.debug { "Listing directory in S3: $path" }

        val client = s3Client ?: throw IllegalStateException("Not connected to S3")
        val prefix = normalizePath(path)
        val normalizedPrefix = if (prefix.isEmpty() || prefix.endsWith("/")) prefix else "$prefix/"

        try {
            val request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(normalizedPrefix)
                .delimiter("/")
                .build()

            val response = client.listObjectsV2(request).await()
            val result = mutableListOf<FileInfo>()

            // 添加子目录
            response.commonPrefixes().forEach { prefix ->
                val name = prefix.prefix().removeSuffix("/").substringAfterLast('/')
                result.add(
                    FileInfo(
                        path = prefix.prefix(),
                        name = name,
                        isDirectory = true,
                        size = 0,
                        lastModified = 0,
                        creationTime = 0,
                        lastAccessTime = 0
                    )
                )
            }

            // 添加文件
            response.contents().forEach { obj ->
                // 跳过表示目录的对象
                if (obj.key() == normalizedPrefix || obj.key().endsWith("/")) {
                    return@forEach
                }

                val name = obj.key().substringAfterLast('/')
                result.add(
                    FileInfo(
                        path = obj.key(),
                        name = name,
                        isDirectory = false,
                        size = obj.size(),
                        lastModified = obj.lastModified().toEpochMilli(),
                        creationTime = obj.lastModified().toEpochMilli(),
                        lastAccessTime = obj.lastModified().toEpochMilli()
                    )
                )
            }

            result
        } catch (e: Exception) {
            logger.error(e) { "Error listing directory in S3: $path" }
            throw e
        }
    }

    override suspend fun exists(path: String): Boolean = withContext(Dispatchers.IO) {
        logger.debug { "Checking if exists in S3: $path" }

        val client = s3Client ?: throw IllegalStateException("Not connected to S3")
        val key = normalizePath(path)

        try {
            // 检查是否是文件
            try {
                client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(key).build())
                    .await()
                return@withContext true
            } catch (e: NoSuchKeyException) {
                // 不是文件，继续检查是否是目录
            } catch (e: Exception) {
                logger.error(e) { "Error checking if file exists: $path" }
                throw e
            }

            // 检查是否是目录
            val dirKey = if (key.endsWith("/")) key else "$key/"
            val request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(dirKey)
                .maxKeys(1)
                .build()

            val response = client.listObjectsV2(request).await()
            response.keyCount() > 0
        } catch (e: Exception) {
            logger.error(e) { "Error checking if exists in S3: $path" }
            false
        }
    }

    override suspend fun getInfo(path: String): FileInfo = withContext(Dispatchers.IO) {
        logger.debug { "Getting info from S3: $path" }

        val client = s3Client ?: throw IllegalStateException("Not connected to S3")
        val key = normalizePath(path)

        try {
            // 尝试获取文件信息
            try {
                val response = client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(key).build())
                    .await()

                val name = key.substringAfterLast('/')
                return@withContext FileInfo(
                    path = key,
                    name = name,
                    isDirectory = false,
                    size = response.contentLength(),
                    lastModified = response.lastModified().toEpochMilli(),
                    creationTime = response.lastModified().toEpochMilli(),
                    lastAccessTime = response.lastModified().toEpochMilli()
                )
            } catch (e: NoSuchKeyException) {
                // 不是文件，继续检查是否是目录
            } catch (e: Exception) {
                logger.error(e) { "Error getting file info: $path" }
                throw e
            }

            // 检查是否是目录
            val dirKey = if (key.endsWith("/")) key else "$key/"
            val request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(dirKey)
                .maxKeys(1)
                .build()

            val response = client.listObjectsV2(request).await()
            if (response.keyCount() > 0) {
                val name = dirKey.removeSuffix("/").substringAfterLast('/')
                return@withContext FileInfo(
                    path = dirKey,
                    name = name,
                    isDirectory = true,
                    size = 0,
                    lastModified = 0,
                    creationTime = 0,
                    lastAccessTime = 0
                )
            }

            throw NoSuchKeyException.builder().message("Object not found: $path").build()
        } catch (e: Exception) {
            logger.error(e) { "Error getting info from S3: $path" }
            throw e
        }
    }

    override suspend fun getPublicUrl(path: String, expirationSeconds: Int): String = withContext(Dispatchers.IO) {
        logger.debug { "Getting public URL for S3 object: $path" }

        val client = s3Client ?: throw IllegalStateException("Not connected to S3")
        val key = normalizePath(path)

        try {
            val request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build()

            val presignRequest = software.amazon.awssdk.services.s3.presigner.S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .build()
                .presignGetObject { presignRequest ->
                    presignRequest.signatureDuration(Duration.ofSeconds(expirationSeconds.toLong()))
                    presignRequest.getObjectRequest(request)
                }

            presignRequest.url().toString()
        } catch (e: Exception) {
            logger.error(e) { "Error getting public URL for S3 object: $path" }
            throw e
        }
    }

    override suspend fun getObjectMetadata(path: String): Map<String, String> = withContext(Dispatchers.IO) {
        logger.debug { "Getting metadata for S3 object: $path" }

        val client = s3Client ?: throw IllegalStateException("Not connected to S3")
        val key = normalizePath(path)

        try {
            val request = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build()

            val response = client.headObject(request).await()

            val metadata = mutableMapOf<String, String>()
            metadata["Content-Type"] = response.contentType() ?: ""
            metadata["Content-Length"] = response.contentLength().toString()
            metadata["Last-Modified"] = response.lastModified().toString()
            metadata["ETag"] = response.eTag() ?: ""

            // 添加用户自定义元数据
            response.metadata().forEach { (key, value) ->
                metadata[key] = value
            }

            metadata
        } catch (e: Exception) {
            logger.error(e) { "Error getting metadata for S3 object: $path" }
            throw e
        }
    }

    override suspend fun setObjectMetadata(path: String, metadata: Map<String, String>): Boolean = withContext(Dispatchers.IO) {
        logger.debug { "Setting metadata for S3 object: $path" }

        val client = s3Client ?: throw IllegalStateException("Not connected to S3")
        val key = normalizePath(path)

        try {
            // S3 不支持直接更新元数据，需要复制对象到自身
            val request = CopyObjectRequest.builder()
                .sourceBucket(bucketName)
                .sourceKey(key)
                .destinationBucket(bucketName)
                .destinationKey(key)
                .metadata(metadata)
                .metadataDirective(MetadataDirective.REPLACE)
                .build()

            client.copyObject(request).await()
            true
        } catch (e: Exception) {
            logger.error(e) { "Error setting metadata for S3 object: $path" }
            false
        }
    }

    override fun getBucketName(): String {
        return bucketName
    }

    override fun getRegion(): String {
        return region
    }

    /**
     * 规范化路径，移除前导斜杠。
     *
     * @param path 原始路径。
     * @return 规范化后的路径。
     */
    private fun normalizePath(path: String): String {
        return path.trimStart('/')
    }
}
