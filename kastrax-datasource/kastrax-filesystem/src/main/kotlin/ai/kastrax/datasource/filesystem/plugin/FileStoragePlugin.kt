package ai.kastrax.datasource.filesystem.plugin

import ai.kastrax.core.plugin.*
import ai.kastrax.core.workflow.state.WorkflowState
import ai.kastrax.core.workflow.state.WorkflowStateStorage
import ai.kastrax.datasource.filesystem.plugin.storage.LocalFileStorage
import ai.kastrax.datasource.filesystem.plugin.storage.S3FileStorage
import java.util.UUID

/**
 * 文件存储插件，提供基于文件系统的存储实现。
 */
class FileStoragePlugin : AbstractStoragePlugin(
    id = "ai.kastrax.plugins.storage.file",
    name = "File Storage Plugin",
    description = "提供基于文件系统的存储实现，包括本地文件系统和S3兼容存储",
    version = "1.0.0",
    author = "KastraX Team"
) {
    private val storageTypes = listOf(
        StorageType(
            id = "local-file",
            name = "本地文件存储",
            description = "基于本地文件系统的存储实现",
            configSchema = mapOf(
                "rootPath" to ConfigField("rootPath", "根目录路径", "string", true),
                "createIfNotExists" to ConfigField("createIfNotExists", "如果目录不存在则创建", "boolean", false, "true")
            ),
            capabilities = setOf(
                StorageCapability.WORKFLOW_STATE,
                StorageCapability.EVENT,
                StorageCapability.DATA
            ),
            category = "文件系统",
            tags = listOf("本地", "文件系统")
        ),
        StorageType(
            id = "s3",
            name = "S3兼容存储",
            description = "基于S3兼容存储的实现",
            configSchema = mapOf(
                "endpoint" to ConfigField("endpoint", "S3端点", "string", true),
                "region" to ConfigField("region", "区域", "string", true),
                "bucket" to ConfigField("bucket", "存储桶", "string", true),
                "accessKey" to ConfigField("accessKey", "访问密钥", "string", true),
                "secretKey" to ConfigField("secretKey", "秘密密钥", "string", true, null, true),
                "prefix" to ConfigField("prefix", "对象前缀", "string", false, "")
            ),
            capabilities = setOf(
                StorageCapability.WORKFLOW_STATE,
                StorageCapability.EVENT,
                StorageCapability.DATA
            ),
            category = "云存储",
            tags = listOf("S3", "云", "对象存储")
        )
    )

    override fun getStorageTypes(): List<StorageType> {
        return storageTypes
    }

    override fun createWorkflowStateStorage(storageType: String, config: Map<String, Any?>): WorkflowStateStorage? {
        return when (storageType) {
            "local-file" -> createLocalFileStateStorage(config)
            "s3" -> createS3StateStorage(config)
            else -> {
                logger.warn { "不支持的存储类型: $storageType" }
                null
            }
        }
    }

    override fun createEventStorage(storageType: String, config: Map<String, Any?>): EventStorage? {
        return when (storageType) {
            "local-file" -> createLocalFileEventStorage(config)
            "s3" -> createS3EventStorage(config)
            else -> {
                logger.warn { "不支持的存储类型: $storageType" }
                null
            }
        }
    }

    override fun createDataStorage(storageType: String, config: Map<String, Any?>): DataStorage? {
        return when (storageType) {
            "local-file" -> createLocalFileDataStorage(config)
            "s3" -> createS3DataStorage(config)
            else -> {
                logger.warn { "不支持的存储类型: $storageType" }
                null
            }
        }
    }

    private fun createLocalFileStateStorage(config: Map<String, Any?>): WorkflowStateStorage {
        val rootPath = config["rootPath"] as String
        val createIfNotExists = config["createIfNotExists"] as? Boolean ?: true
        
        val id = config["id"] as? String ?: "local-file-state-${UUID.randomUUID()}"
        val name = config["name"] as? String ?: "Local File State Storage"
        
        return LocalFileStorage.createStateStorage(
            id = id,
            name = name,
            rootPath = rootPath,
            createIfNotExists = createIfNotExists
        )
    }

    private fun createS3StateStorage(config: Map<String, Any?>): WorkflowStateStorage {
        val endpoint = config["endpoint"] as String
        val region = config["region"] as String
        val bucket = config["bucket"] as String
        val accessKey = config["accessKey"] as String
        val secretKey = config["secretKey"] as String
        val prefix = config["prefix"] as? String ?: ""
        
        val id = config["id"] as? String ?: "s3-state-${UUID.randomUUID()}"
        val name = config["name"] as? String ?: "S3 State Storage"
        
        return S3FileStorage.createStateStorage(
            id = id,
            name = name,
            endpoint = endpoint,
            region = region,
            bucket = bucket,
            accessKey = accessKey,
            secretKey = secretKey,
            prefix = prefix
        )
    }

    private fun createLocalFileEventStorage(config: Map<String, Any?>): EventStorage {
        val rootPath = config["rootPath"] as String
        val createIfNotExists = config["createIfNotExists"] as? Boolean ?: true
        
        val id = config["id"] as? String ?: "local-file-event-${UUID.randomUUID()}"
        val name = config["name"] as? String ?: "Local File Event Storage"
        
        return LocalFileStorage.createEventStorage(
            id = id,
            name = name,
            rootPath = rootPath,
            createIfNotExists = createIfNotExists
        )
    }

    private fun createS3EventStorage(config: Map<String, Any?>): EventStorage {
        val endpoint = config["endpoint"] as String
        val region = config["region"] as String
        val bucket = config["bucket"] as String
        val accessKey = config["accessKey"] as String
        val secretKey = config["secretKey"] as String
        val prefix = config["prefix"] as? String ?: ""
        
        val id = config["id"] as? String ?: "s3-event-${UUID.randomUUID()}"
        val name = config["name"] as? String ?: "S3 Event Storage"
        
        return S3FileStorage.createEventStorage(
            id = id,
            name = name,
            endpoint = endpoint,
            region = region,
            bucket = bucket,
            accessKey = accessKey,
            secretKey = secretKey,
            prefix = prefix
        )
    }

    private fun createLocalFileDataStorage(config: Map<String, Any?>): DataStorage {
        val rootPath = config["rootPath"] as String
        val createIfNotExists = config["createIfNotExists"] as? Boolean ?: true
        
        val id = config["id"] as? String ?: "local-file-data-${UUID.randomUUID()}"
        val name = config["name"] as? String ?: "Local File Data Storage"
        
        return LocalFileStorage.createDataStorage(
            id = id,
            name = name,
            rootPath = rootPath,
            createIfNotExists = createIfNotExists
        )
    }

    private fun createS3DataStorage(config: Map<String, Any?>): DataStorage {
        val endpoint = config["endpoint"] as String
        val region = config["region"] as String
        val bucket = config["bucket"] as String
        val accessKey = config["accessKey"] as String
        val secretKey = config["secretKey"] as String
        val prefix = config["prefix"] as? String ?: ""
        
        val id = config["id"] as? String ?: "s3-data-${UUID.randomUUID()}"
        val name = config["name"] as? String ?: "S3 Data Storage"
        
        return S3FileStorage.createDataStorage(
            id = id,
            name = name,
            endpoint = endpoint,
            region = region,
            bucket = bucket,
            accessKey = accessKey,
            secretKey = secretKey,
            prefix = prefix
        )
    }
}

/**
 * 配置字段，描述了存储配置中的一个字段。
 */
data class ConfigField(
    val name: String,
    val description: String,
    val type: String,
    val required: Boolean = false,
    val defaultValue: String? = null,
    val sensitive: Boolean = false
)
