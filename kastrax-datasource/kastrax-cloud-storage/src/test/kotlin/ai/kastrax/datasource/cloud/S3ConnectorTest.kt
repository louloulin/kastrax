package ai.kastrax.datasource.cloud

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class S3ConnectorTest {

    companion object {
        @JvmStatic
        @Container
        val localstack = LocalStackContainer(DockerImageName.parse("localstack/localstack:2.1.0"))
            .withServices(LocalStackContainer.Service.S3)
    }

    private lateinit var connector: S3Connector
    private val testBucketName = "test-bucket"
    private val testObjectKey = "test-file.txt"
    private val testObjectContent = "Hello, S3!"
    private val testFolderKey = "test-folder/"

    @BeforeAll
    fun setup() {
        localstack.start()

        // 创建 S3 连接器
        connector = S3Connector(
            name = "test-s3",
            bucketName = testBucketName,
            region = localstack.region,
            accessKey = localstack.accessKey,
            secretKey = localstack.secretKey,
            endpoint = localstack.getEndpointOverride(LocalStackContainer.Service.S3).toString(),
            pathStyleAccessEnabled = true
        )

        runBlocking {
            // 连接到 S3
            connector.connect()

            // 创建测试桶
            val s3Client = software.amazon.awssdk.services.s3.S3Client.builder()
                .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.S3))
                .region(software.amazon.awssdk.regions.Region.of(localstack.region))
                .credentialsProvider(
                    software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(
                        software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(
                            localstack.accessKey,
                            localstack.secretKey
                        )
                    )
                )
                .build()

            s3Client.createBucket { builder ->
                builder.bucket(testBucketName)
            }

            s3Client.close()
        }
    }

    @AfterAll
    fun tearDown() {
        runBlocking {
            connector.disconnect()
        }
        localstack.stop()
    }

    @Test
    fun `test write and read file`() = runBlocking {
        // 写入文件
        val writeResult = connector.writeTextFile(testObjectKey, testObjectContent)
        assertTrue(writeResult)

        // 读取文件
        val content = connector.readTextFile(testObjectKey)
        assertEquals(testObjectContent, content)
    }

    @Test
    fun `test create directory`() = runBlocking {
        // 创建目录
        val createResult = connector.createDirectory(testFolderKey)
        assertTrue(createResult)

        // 检查目录是否存在
        val exists = connector.exists(testFolderKey)
        assertTrue(exists)
    }

    @Test
    fun `test list directory`() = runBlocking {
        // 创建测试文件
        connector.writeTextFile("$testFolderKey/file1.txt", "File 1 content")
        connector.writeTextFile("$testFolderKey/file2.txt", "File 2 content")

        // 列出目录内容
        val files = connector.listDirectory(testFolderKey)

        // 验证结果
        assertEquals(2, files.size)
        assertTrue(files.any { it.name == "file1.txt" })
        assertTrue(files.any { it.name == "file2.txt" })
    }

    @Test
    fun `test copy file`() = runBlocking {
        // 创建源文件
        val sourceKey = "source.txt"
        connector.writeTextFile(sourceKey, "Source content")

        // 复制文件
        val destinationKey = "destination.txt"
        val copyResult = connector.copy(sourceKey, destinationKey)
        assertTrue(copyResult)

        // 验证目标文件内容
        val content = connector.readTextFile(destinationKey)
        assertEquals("Source content", content)
    }

    @Test
    fun `test move file`() = runBlocking {
        // 创建源文件
        val sourceKey = "move-source.txt"
        connector.writeTextFile(sourceKey, "Move content")

        // 移动文件
        val destinationKey = "move-destination.txt"
        val moveResult = connector.move(sourceKey, destinationKey)
        assertTrue(moveResult)

        // 验证源文件不存在
        val sourceExists = connector.exists(sourceKey)
        assertFalse(sourceExists)

        // 验证目标文件内容
        val content = connector.readTextFile(destinationKey)
        assertEquals("Move content", content)
    }

    @Test
    fun `test delete file`() = runBlocking {
        // 创建文件
        val key = "delete-file.txt"
        connector.writeTextFile(key, "Delete me")

        // 删除文件
        val deleteResult = connector.delete(key)
        assertTrue(deleteResult)

        // 验证文件不存在
        val exists = connector.exists(key)
        assertFalse(exists)
    }

    @Test
    fun `test get file info`() = runBlocking {
        // 创建文件
        val key = "info-file.txt"
        val content = "File info test"
        connector.writeTextFile(key, content)

        // 获取文件信息
        val info = connector.getInfo(key)

        // 验证信息
        assertEquals(key, info.path)
        assertEquals("info-file.txt", info.name)
        assertFalse(info.isDirectory)
        assertEquals(content.length.toLong(), info.size)
    }

    @Test
    fun `test get public url`() = runBlocking {
        // 创建文件
        val key = "public-file.txt"
        connector.writeTextFile(key, "Public content")

        // 获取公共 URL
        val url = connector.getPublicUrl(key)

        // 验证 URL
        assertNotNull(url)
        assertTrue(url.contains(testBucketName))
        assertTrue(url.contains(key))
    }

    @Test
    fun `test object metadata`() = runBlocking {
        // 创建文件
        val key = "metadata-file.txt"
        connector.writeTextFile(key, "Metadata test")

        // 获取元数据
        val metadata = connector.getObjectMetadata(key)

        // 验证元数据
        assertNotNull(metadata["Content-Type"])
        assertNotNull(metadata["Content-Length"])
        assertNotNull(metadata["Last-Modified"])

        // 设置元数据
        val newMetadata = mapOf(
            "x-amz-meta-custom" to "custom-value",
            "Content-Type" to "text/plain"
        )
        val setResult = connector.setObjectMetadata(key, newMetadata)
        assertTrue(setResult)

        // 获取更新后的元数据
        val updatedMetadata = connector.getObjectMetadata(key)
        assertEquals("custom-value", updatedMetadata["x-amz-meta-custom"])
        assertEquals("text/plain", updatedMetadata["Content-Type"])
    }
}
