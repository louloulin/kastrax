package ai.kastrax.cli.server

import ai.kastrax.cli.api.ApiDefinition
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.http.ContentType
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import com.fasterxml.jackson.databind.SerializationFeature
import java.io.File
import java.util.concurrent.atomic.AtomicReference

private val logger = KotlinLogging.logger {}

/**
 * 开发服务器，用于提供 API 和静态文件服务。
 */
class DevServer(private val port: Int, initialApis: List<ApiDefinition>) {
    private val server: NettyApplicationEngine
    private val apis = AtomicReference(initialApis)

    init {
        server = embeddedServer(Netty, port = port) {
            install(CORS) {
                anyHost()
                allowMethod(HttpMethod.Options)
                allowMethod(HttpMethod.Get)
                allowMethod(HttpMethod.Post)
                allowMethod(HttpMethod.Put)
                allowMethod(HttpMethod.Delete)
                allowHeader(HttpHeaders.ContentType)
                allowHeader(HttpHeaders.Authorization)
            }

            install(ContentNegotiation) {
                jackson {
                    enable(SerializationFeature.INDENT_OUTPUT)
                }
            }

            routing {
                // 静态文件服务
                get("/") {
                    val html = """
                        <!DOCTYPE html>
                        <html lang="zh-CN">
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <title>KastraX Dev Server</title>
                            <style>
                                body {
                                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
                                    line-height: 1.6;
                                    color: #333;
                                    max-width: 1200px;
                                    margin: 0 auto;
                                    padding: 20px;
                                }
                                h1 {
                                    color: #0066cc;
                                    border-bottom: 1px solid #eee;
                                    padding-bottom: 10px;
                                }
                                h2 {
                                    color: #0066cc;
                                    margin-top: 30px;
                                }
                                .api-list {
                                    margin-top: 20px;
                                }
                                .api-item {
                                    background-color: #f8f9fa;
                                    border-radius: 5px;
                                    padding: 15px;
                                    margin-bottom: 10px;
                                    border-left: 4px solid #0066cc;
                                }
                                .api-path {
                                    font-weight: bold;
                                    font-family: monospace;
                                    font-size: 16px;
                                }
                                .api-method {
                                    display: inline-block;
                                    padding: 3px 8px;
                                    border-radius: 3px;
                                    font-size: 12px;
                                    font-weight: bold;
                                    margin-right: 10px;
                                }
                                .get {
                                    background-color: #61affe;
                                    color: white;
                                }
                                .post {
                                    background-color: #49cc90;
                                    color: white;
                                }
                                .put {
                                    background-color: #fca130;
                                    color: white;
                                }
                                .delete {
                                    background-color: #f93e3e;
                                    color: white;
                                }
                                .api-description {
                                    margin-top: 5px;
                                    color: #555;
                                }
                                .links {
                                    margin-top: 20px;
                                }
                                .links a {
                                    display: inline-block;
                                    margin-right: 15px;
                                    color: #0066cc;
                                    text-decoration: none;
                                }
                                .links a:hover {
                                    text-decoration: underline;
                                }
                            </style>
                        </head>
                        <body>
                            <h1>KastraX Dev Server</h1>
                            <p>KastraX 开发服务器已成功启动。您可以通过以下链接访问 API 文档和工具。</p>

                            <div class="links">
                                <a href="/openapi.json" target="_blank">OpenAPI 规范</a>
                                <a href="/api" target="_blank">API 列表</a>
                                <a href="/api-validation" target="_blank">API 验证报告</a>
                            </div>

                            <h2>可用 API 端点</h2>
                            <div class="api-list" id="apiList">
                                <p>加载中...</p>
                            </div>

                            <script>
                                // 获取 API 列表
                                fetch('/api')
                                    .then(response => response.json())
                                    .then(data => {
                                        const apiList = document.getElementById('apiList');
                                        apiList.innerHTML = '';

                                        if (data.apis && data.apis.length > 0) {
                                            data.apis.forEach(api => {
                                                const apiItem = document.createElement('div');
                                                apiItem.className = 'api-item';

                                                const methodSpan = document.createElement('span');
                                                methodSpan.className = `api-method $\{api.method.toLowerCase()}`;
                                                methodSpan.textContent = api.method;

                                                const pathSpan = document.createElement('span');
                                                pathSpan.className = 'api-path';
                                                pathSpan.textContent = api.path;

                                                const descDiv = document.createElement('div');
                                                descDiv.className = 'api-description';
                                                descDiv.textContent = api.description;

                                                apiItem.appendChild(methodSpan);
                                                apiItem.appendChild(pathSpan);
                                                apiItem.appendChild(descDiv);

                                                apiList.appendChild(apiItem);
                                            });
                                        } else {
                                            apiList.innerHTML = '<p>没有找到 API 端点。</p>';
                                        }
                                    })
                                    .catch(error => {
                                        const apiList = document.getElementById('apiList');
                                        apiList.innerHTML = `<p>加载 API 列表时出错: $\{error.message}</p>`;
                                    });
                            </script>
                        </body>
                        </html>
                    """.trimIndent()
                    call.respondText(html, ContentType.Text.Html)
                }

                // OpenAPI 规范
                get("/openapi.json") {
                    val openApiFile = File(".kastrax/openapi.json")
                    if (openApiFile.exists()) {
                        call.respondFile(openApiFile)
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf(
                            "error" to "OpenAPI 规范未生成",
                            "message" to "请确保已运行 kastrax dev 命令并成功生成 API"
                        ))
                    }
                }

                // API 验证报告
                get("/api-validation") {
                    val validationFile = File(".kastrax/api-validation.json")
                    if (validationFile.exists()) {
                        call.respondFile(validationFile)
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf(
                            "error" to "API 验证报告未生成",
                            "message" to "请确保已运行 kastrax dev 命令并成功生成 API"
                        ))
                    }
                }

                // API 列表
                get("/api") {
                    call.respond(mapOf(
                        "apis" to apis.get().map { api ->
                            mapOf(
                                "path" to api.path,
                                "method" to api.method,
                                "description" to api.description,
                                "parameters" to api.parameters.map { param ->
                                    mapOf(
                                        "name" to param.name,
                                        "type" to param.type,
                                        "description" to param.description,
                                        "required" to param.required
                                    )
                                }
                            )
                        },
                        "count" to apis.get().size,
                        "types" to apis.get().groupBy {
                            when {
                                it.path.contains("/agents/") -> "agent"
                                it.path.contains("/tools/") -> "tool"
                                it.path.contains("/zodtools/") -> "zodtool"
                                it.path.contains("/workflows/") -> "workflow"
                                it.path.contains("/networks/") -> "network"
                                it.path.contains("/routers/") -> "router"
                                else -> "other"
                            }
                        }.mapValues { it.value.size }
                    ))
                }

                // 刷新端点
                post("/__refresh") {
                    call.respond(mapOf(
                        "status" to "ok",
                        "timestamp" to System.currentTimeMillis(),
                        "apiCount" to apis.get().size
                    ))
                }

                // 健康检查端点
                get("/health") {
                    call.respond(mapOf(
                        "status" to "up",
                        "timestamp" to System.currentTimeMillis(),
                        "apiCount" to apis.get().size
                    ))
                }

                // 动态路由
                route("/api") {
                    for (api in apis.get()) {
                        when (api.method) {
                            ai.kastrax.cli.api.HttpMethod.GET -> {
                                get(api.path.removePrefix("/api")) {
                                    call.respond(mapOf(
                                        "message" to "这是一个模拟响应",
                                        "path" to api.path,
                                        "method" to api.method,
                                        "description" to api.description
                                    ))
                                }
                            }
                            ai.kastrax.cli.api.HttpMethod.POST -> {
                                post(api.path.removePrefix("/api")) {
                                    call.respond(mapOf(
                                        "message" to "这是一个模拟响应",
                                        "path" to api.path,
                                        "method" to api.method,
                                        "description" to api.description
                                    ))
                                }
                            }
                            ai.kastrax.cli.api.HttpMethod.PUT -> {
                                put(api.path.removePrefix("/api")) {
                                    call.respond(mapOf(
                                        "message" to "这是一个模拟响应",
                                        "path" to api.path,
                                        "method" to api.method,
                                        "description" to api.description
                                    ))
                                }
                            }
                            ai.kastrax.cli.api.HttpMethod.DELETE -> {
                                delete(api.path.removePrefix("/api")) {
                                    call.respond(mapOf(
                                        "message" to "这是一个模拟响应",
                                        "path" to api.path,
                                        "method" to api.method,
                                        "description" to api.description
                                    ))
                                }
                            }
                            else -> {
                                // 不支持的方法
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 启动服务器。
     */
    fun start() {
        server.start(wait = false)
        logger.info { "开发服务器已启动，端口: $port" }
    }

    /**
     * 停止服务器。
     */
    fun stop() {
        server.stop(1000, 2000)
        logger.info { "开发服务器已停止" }
    }

    /**
     * 更新 API 定义。
     */
    fun updateApis(newApis: List<ApiDefinition>) {
        apis.set(newApis)
        logger.info { "API 定义已更新，共 ${newApis.size} 个端点" }
    }


}
