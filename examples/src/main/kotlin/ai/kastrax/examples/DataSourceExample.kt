package ai.kastrax.examples

import ai.kastrax.datasource.DataSourceManager
import ai.kastrax.datasource.api.ApiConnector
import ai.kastrax.datasource.database.DatabaseConnector
import ai.kastrax.datasource.filesystem.FileSystemConnector
import ai.kastrax.datasource.localFileSystem
import ai.kastrax.datasource.mysql
import ai.kastrax.datasource.restApi
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Paths

/**
 * 数据源示例
 * 这个示例展示了如何使用数据源集成。
 */
fun main() = runBlocking {
    println("开始执行DataSourceExample...")
    
    // 获取数据源管理器实例
    val dataSourceManager = DataSourceManager.getInstance()
    
    // 创建本地文件系统连接器
    val fileSystemConnector = localFileSystem {
        name("local-fs")
        rootPath("./data")
    }
    
    // 注册本地文件系统连接器
    dataSourceManager.registerDataSource(fileSystemConnector)
    
    // 创建 RESTful API 连接器
    val apiConnector = restApi {
        name("github-api")
        baseUrl("https://api.github.com")
        header("Accept", "application/vnd.github.v3+json")
        
        // 如果有 GitHub 令牌，可以添加认证
        // bearerAuth(System.getenv("GITHUB_TOKEN") ?: "")
    }
    
    // 注册 RESTful API 连接器
    dataSourceManager.registerDataSource(apiConnector)
    
    // 创建 MySQL 数据库连接器
    // 注意：这里使用了示例配置，实际使用时需要替换为真实的数据库配置
    val databaseConnector = mysql {
        name("mysql-db")
        host("localhost")
        port(3306)
        database("kastrax")
        username("root")
        password("password")
    }
    
    // 注册 MySQL 数据库连接器
    dataSourceManager.registerDataSource(databaseConnector)
    
    // 连接所有数据源
    println("连接所有数据源...")
    val connectedCount = dataSourceManager.connectAll()
    println("成功连接 $connectedCount 个数据源")
    
    // 使用本地文件系统连接器
    println("\n使用本地文件系统连接器...")
    val fs = dataSourceManager.getDataSource("local-fs") as FileSystemConnector
    
    // 确保数据目录存在
    if (!Files.exists(Paths.get("./data"))) {
        Files.createDirectories(Paths.get("./data"))
    }
    
    // 创建测试文件
    fs.writeTextFile("test.txt", "Hello, KastraX Data Source!", overwrite = true)
    println("创建测试文件: test.txt")
    
    // 读取测试文件
    val content = fs.readTextFile("test.txt")
    println("读取测试文件内容: $content")
    
    // 列出目录内容
    val files = fs.listDirectory("")
    println("目录内容:")
    files.forEach { file ->
        println("- ${file.name} (${if (file.isDirectory) "目录" else "文件"}, ${file.size} 字节)")
    }
    
    // 使用 RESTful API 连接器
    println("\n使用 RESTful API 连接器...")
    val api = dataSourceManager.getDataSource("github-api") as ApiConnector
    
    // 获取 GitHub 用户信息
    val response = api.get("users/octocat")
    println("GitHub API 响应状态码: ${response.statusCode}")
    println("GitHub 用户信息:")
    response.json?.let { json ->
        println("- 用户名: ${json.jsonObject["login"]?.toString()?.replace("\"", "")}")
        println("- 名称: ${json.jsonObject["name"]?.toString()?.replace("\"", "")}")
        println("- 公共仓库数: ${json.jsonObject["public_repos"]}")
        println("- 关注者数: ${json.jsonObject["followers"]}")
    }
    
    // 使用 MySQL 数据库连接器
    // 注意：这里只是示例，实际使用时需要确保数据库已经创建并且可以连接
    println("\n使用 MySQL 数据库连接器...")
    val db = dataSourceManager.getDataSource("mysql-db") as DatabaseConnector
    
    // 由于这只是示例，我们不会实际执行数据库操作
    println("MySQL 数据库连接器已创建，但不会实际执行数据库操作")
    println("在实际应用中，可以使用以下方法:")
    println("- executeQuery(): 执行 SQL 查询")
    println("- executeUpdate(): 执行 SQL 更新")
    println("- getTables(): 获取数据库中的表列表")
    println("- getColumns(): 获取表的列信息")
    
    // 断开所有数据源的连接
    println("\n断开所有数据源的连接...")
    val disconnectedCount = dataSourceManager.disconnectAll()
    println("成功断开 $disconnectedCount 个数据源的连接")
    
    println("\n示例结束")
}
