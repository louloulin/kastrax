# 数据源集成文档

本文档详细介绍了 KastraX 框架中的数据源集成功能，包括数据库连接器、API集成和文件系统集成。

## 1. 概述

数据源集成是 KastraX 框架的一部分，允许开发者连接和操作各种数据源，包括数据库、API和文件系统。该集成提供了以下核心功能：

- 数据库连接器：连接和操作关系型数据库，如MySQL
- API集成：与RESTful API进行交互
- 文件系统集成：读取和写入本地文件系统

## 2. 数据源管理器

数据源管理器是数据源集成的核心组件，用于管理所有数据源。

### 2.1 获取数据源管理器实例

```kotlin
val dataSourceManager = DataSourceManager.getInstance()
```

### 2.2 注册数据源

```kotlin
val dataSource = // 创建数据源
dataSourceManager.registerDataSource(dataSource)
```

### 2.3 获取数据源

```kotlin
val dataSource = dataSourceManager.getDataSource("data-source-name")
```

### 2.4 连接和断开连接

```kotlin
// 连接所有数据源
val connectedCount = dataSourceManager.connectAll()

// 断开所有数据源的连接
val disconnectedCount = dataSourceManager.disconnectAll()
```

## 3. 数据库连接器

数据库连接器用于连接和操作关系型数据库。

### 3.1 创建MySQL数据库连接器

#### 使用工厂方法

```kotlin
val databaseConnector = DataSourceFactory.createMySqlConnector(
    name = "mysql-db",
    host = "localhost",
    port = 3306,
    database = "kastrax",
    username = "root",
    password = "password"
)
```

#### 使用DSL

```kotlin
val databaseConnector = mysql {
    name("mysql-db")
    host("localhost")
    port(3306)
    database("kastrax")
    username("root")
    password("password")
}
```

### 3.2 执行SQL查询

```kotlin
val results = databaseConnector.executeQuery(
    "SELECT * FROM users WHERE age > ?",
    mapOf("age" to 18)
)

results.forEach { row ->
    println("User: ${row["name"]}, Age: ${row["age"]}")
}
```

### 3.3 执行SQL更新

```kotlin
val affectedRows = databaseConnector.executeUpdate(
    "UPDATE users SET age = ? WHERE id = ?",
    mapOf("age" to 25, "id" to 1)
)

println("Affected rows: $affectedRows")
```

### 3.4 获取表和列信息

```kotlin
// 获取所有表
val tables = databaseConnector.getTables()
println("Tables: $tables")

// 获取表的列信息
val columns = databaseConnector.getColumns("users")
columns.forEach { column ->
    println("Column: ${column["name"]}, Type: ${column["type"]}")
}
```

## 4. API集成

API集成用于与RESTful API进行交互。

### 4.1 创建RESTful API连接器

#### 使用工厂方法

```kotlin
val apiConnector = DataSourceFactory.createRestApiConnector(
    name = "github-api",
    baseUrl = "https://api.github.com",
    defaultHeaders = mapOf("Accept" to "application/vnd.github.v3+json"),
    authType = RestApiConnector.AuthType.BEARER,
    authToken = "your-token"
)
```

#### 使用DSL

```kotlin
val apiConnector = restApi {
    name("github-api")
    baseUrl("https://api.github.com")
    header("Accept", "application/vnd.github.v3+json")
    bearerAuth("your-token")
}
```

### 4.2 发送GET请求

```kotlin
val response = apiConnector.get(
    path = "users/octocat",
    queryParams = mapOf("page" to "1", "per_page" to "10"),
    headers = mapOf("X-Custom-Header" to "value")
)

println("Status code: ${response.statusCode}")
println("Response: ${response.content}")

// 如果响应是JSON，可以使用json属性
response.json?.let { json ->
    println("User name: ${json.jsonObject["name"]}")
}
```

### 4.3 发送POST请求

```kotlin
val body = mapOf(
    "name" to "New Repository",
    "description" to "This is a new repository",
    "private" to false
)

val response = apiConnector.post(
    path = "user/repos",
    body = body,
    headers = mapOf("X-Custom-Header" to "value")
)

println("Status code: ${response.statusCode}")
println("Response: ${response.content}")
```

### 4.4 发送PUT和DELETE请求

```kotlin
// PUT请求
val putResponse = apiConnector.put(
    path = "repos/octocat/hello-world",
    body = mapOf("name" to "Updated Repository"),
    headers = mapOf("X-Custom-Header" to "value")
)

// DELETE请求
val deleteResponse = apiConnector.delete(
    path = "repos/octocat/hello-world",
    headers = mapOf("X-Custom-Header" to "value")
)
```

## 5. 文件系统集成

文件系统集成用于读取和写入本地文件系统。

### 5.1 创建本地文件系统连接器

#### 使用工厂方法

```kotlin
val fileSystemConnector = DataSourceFactory.createLocalFileSystemConnector(
    name = "local-fs",
    rootPath = "./data"
)
```

#### 使用DSL

```kotlin
val fileSystemConnector = localFileSystem {
    name("local-fs")
    rootPath("./data")
}
```

### 5.2 读取和写入文件

```kotlin
// 写入文本文件
fileSystemConnector.writeTextFile(
    path = "test.txt",
    content = "Hello, KastraX Data Source!",
    charset = "UTF-8",
    overwrite = true
)

// 读取文本文件
val content = fileSystemConnector.readTextFile(
    path = "test.txt",
    charset = "UTF-8"
)

println("File content: $content")

// 写入二进制文件
val bytes = "Binary data".toByteArray()
fileSystemConnector.writeFile(
    path = "binary.dat",
    content = bytes,
    overwrite = true
)

// 读取二进制文件
val binaryContent = fileSystemConnector.readFile("binary.dat")
```

### 5.3 目录操作

```kotlin
// 创建目录
fileSystemConnector.createDirectory(
    path = "new-directory",
    createParents = true
)

// 列出目录内容
val files = fileSystemConnector.listDirectory("new-directory")
files.forEach { file ->
    println("${file.name} (${if (file.isDirectory) "Directory" else "File"}, ${file.size} bytes)")
}

// 检查文件或目录是否存在
val exists = fileSystemConnector.exists("test.txt")
println("File exists: $exists")

// 获取文件或目录信息
val fileInfo = fileSystemConnector.getInfo("test.txt")
println("File name: ${fileInfo.name}")
println("File size: ${fileInfo.size} bytes")
println("Last modified: ${java.util.Date(fileInfo.lastModified)}")
```

### 5.4 复制、移动和删除

```kotlin
// 复制文件
fileSystemConnector.copy(
    source = "test.txt",
    destination = "test-copy.txt",
    overwrite = true
)

// 移动文件
fileSystemConnector.move(
    source = "test-copy.txt",
    destination = "new-directory/test-moved.txt",
    overwrite = true
)

// 删除文件
fileSystemConnector.delete(
    path = "test.txt",
    recursive = false
)

// 删除目录及其内容
fileSystemConnector.delete(
    path = "new-directory",
    recursive = true
)
```

## 6. 高级用法

### 6.1 自定义数据源

可以通过实现 `DataSource` 接口或扩展 `DataSourceBase` 类来创建自定义数据源：

```kotlin
class CustomDataSource(name: String) : DataSourceBase(name, DataSourceType.API) {
    override suspend fun doConnect(): Boolean {
        // 实现连接逻辑
        return true
    }
    
    override suspend fun doDisconnect(): Boolean {
        // 实现断开连接逻辑
        return true
    }
    
    // 添加自定义方法
    suspend fun customOperation(): String {
        // 实现自定义操作
        return "Custom operation result"
    }
}
```

### 6.2 事务支持

对于数据库连接器，可以使用事务来确保操作的原子性：

```kotlin
// 注意：这是一个概念示例，实际实现可能不同
val databaseConnector = dataSourceManager.getDataSource("mysql-db") as DatabaseConnector

// 开始事务
databaseConnector.executeUpdate("START TRANSACTION")

try {
    // 执行多个操作
    databaseConnector.executeUpdate("INSERT INTO users (name, age) VALUES (?, ?)", mapOf("name" to "Alice", "age" to 30))
    databaseConnector.executeUpdate("UPDATE accounts SET balance = balance - ? WHERE user_id = ?", mapOf("amount" to 100, "user_id" to 1))
    
    // 提交事务
    databaseConnector.executeUpdate("COMMIT")
} catch (e: Exception) {
    // 回滚事务
    databaseConnector.executeUpdate("ROLLBACK")
    throw e
}
```

## 7. 最佳实践

1. **连接管理**：使用 `DataSourceManager` 管理所有数据源，确保在应用程序启动时连接数据源，在关闭时断开连接。

2. **错误处理**：始终包含适当的错误处理，特别是在处理外部数据源时：
   ```kotlin
   try {
       val results = databaseConnector.executeQuery("SELECT * FROM users")
       // 处理结果
   } catch (e: Exception) {
       logger.error("Error executing query", e)
       // 处理错误
   }
   ```

3. **资源释放**：确保在不再需要数据源时断开连接，释放资源：
   ```kotlin
   dataSourceManager.disconnectAll()
   ```

4. **参数化查询**：使用参数化查询防止SQL注入：
   ```kotlin
   // 好的做法
   databaseConnector.executeQuery("SELECT * FROM users WHERE name = ?", mapOf("name" to userName))
   
   // 不好的做法
   databaseConnector.executeQuery("SELECT * FROM users WHERE name = '$userName'")
   ```

5. **批量操作**：对于大量数据，使用批量操作提高性能：
   ```kotlin
   // 注意：这是一个概念示例，实际实现可能不同
   val batchParams = listOf(
       mapOf("name" to "Alice", "age" to 30),
       mapOf("name" to "Bob", "age" to 25),
       mapOf("name" to "Charlie", "age" to 35)
   )
   
   batchParams.forEach { params ->
       databaseConnector.executeUpdate("INSERT INTO users (name, age) VALUES (?, ?)", params)
   }
   ```

## 8. 示例

完整的示例可以在 `examples` 目录中找到：

- `DataSourceExample.kt`：展示如何使用数据源集成

## 9. 故障排除

### 9.1 常见问题

1. **连接失败**：确保提供的连接参数（如主机名、端口、用户名和密码）是正确的。

2. **权限问题**：确保用户有足够的权限执行所需的操作。

3. **资源泄漏**：确保在不再需要数据源时断开连接，避免资源泄漏。

### 9.2 调试技巧

1. 启用详细日志记录，以便更好地理解数据源操作：
   ```kotlin
   // 配置日志级别
   System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "DEBUG")
   ```

2. 使用 `isConnected()` 方法检查数据源是否已连接：
   ```kotlin
   if (!dataSource.isConnected()) {
       println("Data source is not connected")
       dataSource.connect()
   }
   ```

## 10. 参考

- [MySQL 文档](https://dev.mysql.com/doc/)
- [RESTful API 设计指南](https://restfulapi.net/)
- [Java NIO 文档](https://docs.oracle.com/javase/8/docs/api/java/nio/file/package-summary.html)
- [KastraX 框架文档](https://kastrax.ai/docs)
