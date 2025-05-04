# 数据源集成

## 概述

Kastrax 提供了与多种数据源的集成，使 Agent 能够访问和处理来自不同来源的数据。这些数据源可以用于 RAG 系统、记忆存储、向量数据库等。本文档介绍 Kastrax 支持的数据源类型、集成方式以及最佳实践。

## 数据源类型

Kastrax 支持以下类型的数据源：

1. **文件系统**：访问本地或远程文件系统中的文件
2. **数据库**：关系型数据库和 NoSQL 数据库
3. **向量数据库**：专门用于存储和检索向量的数据库
4. **云存储**：云服务提供商的存储服务
5. **API 服务**：通过 API 访问的外部服务

## 文件系统集成

### 本地文件系统

Kastrax 提供了访问本地文件系统的功能，可以用于加载文档、存储数据等：

```kotlin
import ai.kastrax.rag.document.DirectoryDocumentLoader
import ai.kastrax.rag.document.FileDocumentLoader
import ai.kastrax.rag.document.RecursiveCharacterTextSplitter

// 加载单个文件
val fileLoader = FileDocumentLoader("path/to/document.pdf")
val documents = fileLoader.load()

// 加载目录中的所有文件
val directoryLoader = DirectoryDocumentLoader(
    directoryPath = "path/to/documents/",
    recursive = true,
    fileExtensions = listOf("pdf", "txt", "md", "docx")
)
val allDocuments = directoryLoader.load()

// 分割文档
val splitter = RecursiveCharacterTextSplitter(
    chunkSize = 1000,
    chunkOverlap = 200
)
val chunks = splitter.splitDocuments(allDocuments)
```

### 远程文件系统

Kastrax 也支持访问远程文件系统，如 SFTP、FTP 等：

```kotlin
import ai.kastrax.datasource.filesystem.SftpFileSystem
import ai.kastrax.rag.document.RemoteDocumentLoader

// 配置 SFTP 文件系统
val sftpConfig = SftpFileSystemConfig(
    host = "example.com",
    port = 22,
    username = "user",
    password = "password" // 或使用 privateKeyPath
)
val sftpFileSystem = SftpFileSystem(sftpConfig)

// 创建远程文档加载器
val remoteLoader = RemoteDocumentLoader(
    fileSystem = sftpFileSystem,
    remotePath = "/path/to/documents/",
    recursive = true,
    fileExtensions = listOf("pdf", "txt")
)

// 加载文档
val remoteDocuments = remoteLoader.load()
```

## 数据库集成

### 关系型数据库

Kastrax 支持与关系型数据库的集成，如 MySQL、PostgreSQL、SQLite 等：

```kotlin
import ai.kastrax.datasource.database.JdbcDataSource
import ai.kastrax.memory.impl.SqliteMemoryStorage

// 创建 JDBC 数据源
val jdbcDataSource = JdbcDataSource(
    url = "jdbc:mysql://localhost:3306/mydb",
    username = "user",
    password = "password",
    driverClassName = "com.mysql.cj.jdbc.Driver"
)

// 执行 SQL 查询
val results = jdbcDataSource.executeQuery("SELECT * FROM users WHERE age > ?", listOf(18))

// 使用 SQLite 作为记忆存储
val sqliteStorage = SqliteMemoryStorage("memory.db")
val memory = MemoryFactory.createWithSqliteStorage(sqliteStorage)
```

### NoSQL 数据库

Kastrax 也支持 NoSQL 数据库，如 MongoDB、Redis 等：

```kotlin
import ai.kastrax.datasource.database.MongoDataSource
import ai.kastrax.datasource.database.RedisDataSource
import ai.kastrax.memory.impl.MongoMemoryStorage

// 创建 MongoDB 数据源
val mongoDataSource = MongoDataSource(
    connectionString = "mongodb://localhost:27017",
    database = "mydb"
)

// 查询文档
val documents = mongoDataSource.findDocuments("users", mapOf("age" to mapOf("\$gt" to 18)))

// 创建 Redis 数据源
val redisDataSource = RedisDataSource(
    host = "localhost",
    port = 6379,
    password = "password"
)

// 存储和检索数据
redisDataSource.set("key", "value")
val value = redisDataSource.get("key")

// 使用 MongoDB 作为记忆存储
val mongoStorage = MongoMemoryStorage(
    connectionString = "mongodb://localhost:27017",
    database = "memory",
    collection = "messages"
)
val memory = MemoryFactory.createWithMongoStorage(mongoStorage)
```

## 向量数据库集成

Kastrax 支持多种向量数据库，用于存储和检索向量嵌入：

### Qdrant

```kotlin
import ai.kastrax.rag.vectorstore.QdrantVectorStore
import ai.kastrax.rag.embedding.OpenAIEmbeddingService

// 创建 Qdrant 向量存储
val qdrantStore = QdrantVectorStore(
    url = "http://localhost:6333",
    collectionName = "documents",
    apiKey = "your-qdrant-api-key" // 可选
)

// 创建嵌入服务
val embeddingService = OpenAIEmbeddingService(
    apiKey = "your-openai-api-key",
    model = "text-embedding-ada-002"
)

// 添加文档
val documents = directoryLoader.load()
qdrantStore.addDocuments(documents, embeddingService)

// 相似度搜索
val query = "人工智能的应用"
val results = qdrantStore.similaritySearch(query, embeddingService, limit = 5)
```

### Pinecone

```kotlin
import ai.kastrax.rag.vectorstore.PineconeVectorStore
import ai.kastrax.rag.embedding.OpenAIEmbeddingService

// 创建 Pinecone 向量存储
val pineconeStore = PineconeVectorStore(
    apiKey = "your-pinecone-api-key",
    environment = "us-west1-gcp",
    indexName = "documents"
)

// 添加文档
val documents = directoryLoader.load()
pineconeStore.addDocuments(documents, embeddingService)

// 相似度搜索
val query = "人工智能的应用"
val results = pineconeStore.similaritySearch(query, embeddingService, limit = 5)
```

### Milvus

```kotlin
import ai.kastrax.rag.vectorstore.MilvusVectorStore
import ai.kastrax.rag.embedding.OpenAIEmbeddingService

// 创建 Milvus 向量存储
val milvusStore = MilvusVectorStore(
    host = "localhost",
    port = 19530,
    collectionName = "documents"
)

// 添加文档
val documents = directoryLoader.load()
milvusStore.addDocuments(documents, embeddingService)

// 相似度搜索
val query = "人工智能的应用"
val results = milvusStore.similaritySearch(query, embeddingService, limit = 5)
```

### 内存向量存储

对于简单场景，Kastrax 提供了内存向量存储：

```kotlin
import ai.kastrax.rag.vectorstore.InMemoryVectorStore
import ai.kastrax.rag.embedding.OpenAIEmbeddingService

// 创建内存向量存储
val inMemoryStore = InMemoryVectorStore()

// 添加文档
val documents = directoryLoader.load()
inMemoryStore.addDocuments(documents, embeddingService)

// 相似度搜索
val query = "人工智能的应用"
val results = inMemoryStore.similaritySearch(query, embeddingService, limit = 5)
```

## 云存储集成

Kastrax 支持与各种云存储服务的集成：

### Amazon S3

```kotlin
import ai.kastrax.datasource.cloud.S3Storage
import ai.kastrax.rag.document.S3DocumentLoader

// 创建 S3 存储
val s3Storage = S3Storage(
    accessKey = "your-access-key",
    secretKey = "your-secret-key",
    region = "us-west-2",
    bucket = "my-bucket"
)

// 上传文件
s3Storage.uploadFile("local/path/to/file.txt", "remote/path/file.txt")

// 下载文件
s3Storage.downloadFile("remote/path/file.txt", "local/download/file.txt")

// 使用 S3 文档加载器
val s3Loader = S3DocumentLoader(
    s3Storage = s3Storage,
    prefix = "documents/",
    fileExtensions = listOf("pdf", "txt")
)
val s3Documents = s3Loader.load()
```

### Google Cloud Storage

```kotlin
import ai.kastrax.datasource.cloud.GcsStorage
import ai.kastrax.rag.document.GcsDocumentLoader

// 创建 GCS 存储
val gcsStorage = GcsStorage(
    credentialsPath = "path/to/credentials.json",
    bucket = "my-bucket"
)

// 上传文件
gcsStorage.uploadFile("local/path/to/file.txt", "remote/path/file.txt")

// 下载文件
gcsStorage.downloadFile("remote/path/file.txt", "local/download/file.txt")

// 使用 GCS 文档加载器
val gcsLoader = GcsDocumentLoader(
    gcsStorage = gcsStorage,
    prefix = "documents/",
    fileExtensions = listOf("pdf", "txt")
)
val gcsDocuments = gcsLoader.load()
```

### Azure Blob Storage

```kotlin
import ai.kastrax.datasource.cloud.AzureBlobStorage
import ai.kastrax.rag.document.AzureDocumentLoader

// 创建 Azure Blob 存储
val azureStorage = AzureBlobStorage(
    connectionString = "your-connection-string",
    containerName = "my-container"
)

// 上传文件
azureStorage.uploadFile("local/path/to/file.txt", "remote/path/file.txt")

// 下载文件
azureStorage.downloadFile("remote/path/file.txt", "local/download/file.txt")

// 使用 Azure 文档加载器
val azureLoader = AzureDocumentLoader(
    azureStorage = azureStorage,
    prefix = "documents/",
    fileExtensions = listOf("pdf", "txt")
)
val azureDocuments = azureLoader.load()
```

## API 服务集成

Kastrax 提供了与外部 API 服务集成的功能：

```kotlin
import ai.kastrax.datasource.api.RestApiClient
import ai.kastrax.datasource.api.GraphQLClient

// 创建 REST API 客户端
val restClient = RestApiClient(
    baseUrl = "https://api.example.com",
    headers = mapOf("Authorization" to "Bearer your-token")
)

// 发送 GET 请求
val response = restClient.get("/users", mapOf("page" to "1", "limit" to "10"))

// 发送 POST 请求
val createResponse = restClient.post(
    "/users",
    mapOf("name" to "John Doe", "email" to "john@example.com")
)

// 创建 GraphQL 客户端
val graphQLClient = GraphQLClient(
    url = "https://api.example.com/graphql",
    headers = mapOf("Authorization" to "Bearer your-token")
)

// 执行查询
val queryResult = graphQLClient.query("""
    query {
        users(page: 1, limit: 10) {
            id
            name
            email
        }
    }
""")

// 执行变更
val mutationResult = graphQLClient.mutate("""
    mutation {
        createUser(input: {name: "John Doe", email: "john@example.com"}) {
            id
            name
            email
        }
    }
""")
```

## 自定义数据源

如果需要集成其他数据源，可以实现相应的接口：

```kotlin
import ai.kastrax.datasource.DataSource
import ai.kastrax.rag.vectorstore.VectorStore
import ai.kastrax.rag.document.DocumentLoader

// 自定义数据源
class CustomDataSource : DataSource {
    // 实现数据源接口
}

// 自定义向量存储
class CustomVectorStore : VectorStore {
    // 实现向量存储接口
}

// 自定义文档加载器
class CustomDocumentLoader : DocumentLoader {
    // 实现文档加载器接口
}
```

## 数据源工厂

Kastrax 提供了数据源工厂，用于创建和管理数据源：

```kotlin
import ai.kastrax.datasource.DataSourceFactory

// 创建数据源
val jdbcDataSource = DataSourceFactory.createJdbcDataSource(
    url = "jdbc:mysql://localhost:3306/mydb",
    username = "user",
    password = "password"
)

val mongoDataSource = DataSourceFactory.createMongoDataSource(
    connectionString = "mongodb://localhost:27017",
    database = "mydb"
)

val s3Storage = DataSourceFactory.createS3Storage(
    accessKey = "your-access-key",
    secretKey = "your-secret-key",
    region = "us-west-2",
    bucket = "my-bucket"
)
```

## 数据源配置

数据源配置可以通过代码、配置文件或环境变量设置：

```kotlin
// 通过代码配置
val jdbcConfig = JdbcDataSourceConfig(
    url = "jdbc:mysql://localhost:3306/mydb",
    username = "user",
    password = "password",
    driverClassName = "com.mysql.cj.jdbc.Driver"
)
val jdbcDataSource = JdbcDataSource(jdbcConfig)

// 通过配置文件配置
val config = ConfigLoader.loadYaml("datasource-config.yaml")
val mongoDataSource = MongoDataSource(config.getConfig("mongo"))

// 通过环境变量配置
val s3Storage = S3Storage(
    accessKey = System.getenv("AWS_ACCESS_KEY"),
    secretKey = System.getenv("AWS_SECRET_KEY"),
    region = System.getenv("AWS_REGION"),
    bucket = System.getenv("AWS_BUCKET")
)
```

## 数据源连接池

对于需要频繁访问的数据源，Kastrax 提供了连接池支持：

```kotlin
import ai.kastrax.datasource.database.JdbcConnectionPool
import ai.kastrax.datasource.database.MongoConnectionPool

// 创建 JDBC 连接池
val jdbcPool = JdbcConnectionPool(
    url = "jdbc:mysql://localhost:3306/mydb",
    username = "user",
    password = "password",
    driverClassName = "com.mysql.cj.jdbc.Driver",
    maxPoolSize = 10,
    minIdle = 2,
    idleTimeout = 60000
)

// 获取连接
val connection = jdbcPool.getConnection()
try {
    // 使用连接
    val statement = connection.prepareStatement("SELECT * FROM users")
    val resultSet = statement.executeQuery()
    // 处理结果...
} finally {
    // 归还连接
    connection.close()
}

// 创建 MongoDB 连接池
val mongoPool = MongoConnectionPool(
    connectionString = "mongodb://localhost:27017",
    maxPoolSize = 10,
    minIdle = 2,
    idleTimeout = 60000
)

// 获取客户端
val client = mongoPool.getClient()
try {
    // 使用客户端
    val database = client.getDatabase("mydb")
    val collection = database.getCollection("users")
    // 处理集合...
} finally {
    // 归还客户端
    mongoPool.releaseClient(client)
}
```

## 最佳实践

### 1. 连接管理

正确管理数据源连接，避免连接泄漏：

```kotlin
// 使用 try-finally 确保连接关闭
val connection = dataSource.getConnection()
try {
    // 使用连接
} finally {
    connection.close()
}

// 或使用 Kotlin 的 use 扩展函数
dataSource.getConnection().use { connection ->
    // 使用连接
}
```

### 2. 错误处理

处理数据源操作中可能出现的错误：

```kotlin
try {
    val documents = documentLoader.load()
    // 处理文档
} catch (e: IOException) {
    logger.error("加载文档失败: ${e.message}")
    // 错误处理
} catch (e: Exception) {
    logger.error("发生错误: ${e.message}")
    // 错误处理
}
```

### 3. 批量操作

对于大量数据，使用批量操作提高性能：

```kotlin
// 批量添加文档
val documents = documentLoader.load()
vectorStore.addDocumentsBatch(documents, embeddingService, batchSize = 100)

// 批量执行 SQL
jdbcDataSource.executeBatch(
    "INSERT INTO users (name, email) VALUES (?, ?)",
    listOf(
        listOf("John Doe", "john@example.com"),
        listOf("Jane Smith", "jane@example.com"),
        // 更多数据...
    )
)
```

### 4. 缓存

对频繁访问的数据实施缓存：

```kotlin
import ai.kastrax.datasource.cache.CacheManager

// 创建缓存管理器
val cacheManager = CacheManager.create(
    maxSize = 1000,
    expireAfterWrite = 60 * 60 * 1000 // 1小时
)

// 使用缓存
val data = cacheManager.getOrPut("key") {
    // 如果缓存中没有，执行这个函数获取数据
    dataSource.fetchData()
}
```

### 5. 数据验证

在存储数据前进行验证：

```kotlin
import ai.kastrax.core.schema.validate

// 定义验证模式
val userSchema = objectSchema {
    stringField("name", "用户名") {
        minLength(3)
        maxLength(50)
    }
    stringField("email", "电子邮件") {
        email()
    }
    numberField("age", "年龄") {
        min(18)
    }
}

// 验证数据
val userData = mapOf(
    "name" to "John Doe",
    "email" to "john@example.com",
    "age" to 25
)

val validationResult = userSchema.validate(userData)
if (validationResult.isValid) {
    // 数据有效，可以存储
    dataSource.storeUser(userData)
} else {
    // 数据无效，处理错误
    validationResult.errors.forEach { error ->
        println("验证错误: ${error.path} - ${error.message}")
    }
}
```

### 6. 数据转换

在数据源之间转换数据：

```kotlin
import ai.kastrax.datasource.transform.DataTransformer

// 创建数据转换器
val transformer = DataTransformer()

// 定义转换规则
transformer.addRule("user") { input ->
    mapOf(
        "fullName" to "${input["firstName"]} ${input["lastName"]}",
        "emailAddress" to input["email"],
        "userAge" to input["age"]
    )
}

// 转换数据
val sourceData = mapOf(
    "firstName" to "John",
    "lastName" to "Doe",
    "email" to "john@example.com",
    "age" to 25
)

val transformedData = transformer.transform("user", sourceData)
// 结果: {"fullName": "John Doe", "emailAddress": "john@example.com", "userAge": 25}
```

## 总结

Kastrax 提供了与多种数据源的集成，包括文件系统、数据库、向量数据库、云存储和 API 服务。通过这些集成，Agent 可以访问和处理来自不同来源的数据，用于 RAG 系统、记忆存储、向量数据库等。遵循最佳实践，可以更有效地使用数据源集成，提高应用的性能和可靠性。
