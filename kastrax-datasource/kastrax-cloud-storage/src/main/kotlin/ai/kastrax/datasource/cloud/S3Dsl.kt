package ai.kastrax.datasource.cloud

import ai.kastrax.datasource.common.CloudStorageConnector

/**
 * S3 连接器 DSL 构建器。
 */
class S3Builder {
    private var name: String = "s3"
    private var bucketName: String = ""
    private var region: String = "us-east-1"
    private var accessKey: String = ""
    private var secretKey: String = ""
    private var endpoint: String? = null
    private var pathStyleAccessEnabled: Boolean = false

    /**
     * 设置连接器名称。
     *
     * @param name 连接器名称。
     * @return 构建器实例。
     */
    fun name(name: String): S3Builder {
        this.name = name
        return this
    }

    /**
     * 设置存储桶名称。
     *
     * @param bucketName 存储桶名称。
     * @return 构建器实例。
     */
    fun bucket(bucketName: String): S3Builder {
        this.bucketName = bucketName
        return this
    }

    /**
     * 设置区域。
     *
     * @param region 区域。
     * @return 构建器实例。
     */
    fun region(region: String): S3Builder {
        this.region = region
        return this
    }

    /**
     * 设置访问凭证。
     *
     * @param accessKey 访问密钥。
     * @param secretKey 秘密密钥。
     * @return 构建器实例。
     */
    fun credentials(accessKey: String, secretKey: String): S3Builder {
        this.accessKey = accessKey
        this.secretKey = secretKey
        return this
    }

    /**
     * 设置自定义端点。
     *
     * @param endpoint 端点 URL。
     * @return 构建器实例。
     */
    fun endpoint(endpoint: String): S3Builder {
        this.endpoint = endpoint
        return this
    }

    /**
     * 启用路径样式访问。
     *
     * @param enabled 是否启用。
     * @return 构建器实例。
     */
    fun pathStyleAccess(enabled: Boolean): S3Builder {
        this.pathStyleAccessEnabled = enabled
        return this
    }

    /**
     * 构建 S3 连接器。
     *
     * @return S3 连接器实例。
     */
    fun build(): CloudStorageConnector {
        return S3Connector(
            name = name,
            bucketName = bucketName,
            region = region,
            accessKey = accessKey,
            secretKey = secretKey,
            endpoint = endpoint,
            pathStyleAccessEnabled = pathStyleAccessEnabled
        )
    }
}

/**
 * 创建 S3 连接器的 DSL 函数。
 *
 * @param init 初始化函数。
 * @return S3 连接器实例。
 */
fun s3(init: S3Builder.() -> Unit): CloudStorageConnector {
    val builder = S3Builder()
    builder.init()
    return builder.build()
}
