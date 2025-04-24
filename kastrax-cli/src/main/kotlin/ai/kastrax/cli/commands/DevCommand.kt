package ai.kastrax.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.terminal.Terminal
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import ai.kastrax.cli.dsl.KastraxDslScanner
import ai.kastrax.cli.api.ApiGenerator
import ai.kastrax.cli.server.DevServer

private val logger = KotlinLogging.logger {}

/**
 * 开发模式命令，启动本地开发服务器，支持热重载。
 */
class DevCommand : CliktCommand(
    name = "dev",
    help = "启动开发服务器，支持热重载"
) {
    private val port by option("-p", "--port", help = "开发服务器端口").int().default(4111)

    private val dir by option("-d", "--dir", help = "源代码目录").default("src/main/kotlin")

    private val watch by option("-w", "--watch", help = "启用文件监视和热重载").flag().default(true)

    private val terminal = Terminal()
    private var server: DevServer? = null
    private var fileWatcher: FileWatcher? = null

    override fun run() = runBlocking {
        val sourceDir = File(dir)
        if (!sourceDir.exists() || !sourceDir.isDirectory) {
            terminal.println(TextColors.red("错误: 源代码目录 '$dir' 不存在或不是一个目录。"))
            return@runBlocking
        }

        terminal.println(TextColors.brightBlue("启动 KastraX 开发服务器..."))
        terminal.println("端口: $port")
        terminal.println("源代码目录: ${sourceDir.absolutePath}")
        terminal.println("文件监视: ${if (watch) "启用" else "禁用"}")
        terminal.println()

        try {
            // 扫描 DSL 并生成 API
            val dslScanner = KastraxDslScanner(sourceDir)
            val apiGenerator = ApiGenerator(dslScanner)

            terminal.println(TextColors.yellow("扫描 KastraX DSL..."))
            val apiDefinitions = apiGenerator.generateApis()
            terminal.println(TextColors.green("✓ 发现 ${apiDefinitions.size} 个 API 端点"))

            // 启动开发服务器
            server = DevServer(port, apiDefinitions)
            server?.start()

            terminal.println(TextColors.green("✓ 开发服务器已启动"))
            terminal.println("  访问地址: http://localhost:$port")
            terminal.println()

            // 设置文件监视器
            if (watch) {
                terminal.println(TextColors.yellow("启动文件监视器..."))
                fileWatcher = FileWatcher(sourceDir) {
                    terminal.println(TextColors.yellow("检测到文件变化，重新生成 API..."))
                    val updatedApiDefinitions = apiGenerator.generateApis()
                    server?.updateApis(updatedApiDefinitions)
                    terminal.println(TextColors.green("✓ API 已更新"))
                }
                fileWatcher?.start()
                terminal.println(TextColors.green("✓ 文件监视器已启动"))
                terminal.println()
            }

            terminal.println("按 Ctrl+C 停止服务器")

            // 保持进程运行
            while (true) {
                Thread.sleep(1000)
            }
        } catch (e: InterruptedException) {
            shutdown()
            terminal.println()
            terminal.println(TextColors.green("✓ 开发服务器已停止"))
        } catch (e: Exception) {
            shutdown()
            logger.error(e) { "启动开发服务器时发生错误" }
            terminal.println(TextColors.red("错误: 启动开发服务器时发生错误: ${e.message}"))
        }
    }

    private fun shutdown() {
        fileWatcher?.stop()
        server?.stop()
    }
}

/**
 * 文件监视器，用于监视文件变化并触发回调。
 */
class FileWatcher(private val directory: File, private val callback: () -> Unit) {
    private val watchService = FileSystems.getDefault().newWatchService()
    private val watchKeys = mutableMapOf<WatchKey, Path>()
    private var running = false
    private val logger = KotlinLogging.logger {}

    fun start() {
        if (running) return
        running = true

        // 注册所有子目录
        Files.walkFileTree(directory.toPath(), object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                registerDirectory(dir)
                return FileVisitResult.CONTINUE
            }
        })

        // 启动监视线程
        Thread {
            try {
                while (running) {
                    val key = watchService.take()
                    val dir = watchKeys[key]

                    if (dir != null) {
                        var shouldCallback = false

                        for (event in key.pollEvents()) {
                            val kind = event.kind()

                            if (kind == OVERFLOW) continue

                            val fileName = event.context() as Path
                            val fullPath = dir.resolve(fileName)

                            logger.debug { "文件变化: $kind $fullPath" }

                            // 如果是新目录，注册它
                            if (kind == ENTRY_CREATE && Files.isDirectory(fullPath)) {
                                registerDirectory(fullPath)
                            }

                            // 只对 Kotlin 文件变化触发回调
                            if (fileName.toString().endsWith(".kt")) {
                                shouldCallback = true
                            }
                        }

                        if (shouldCallback) {
                            callback()
                        }

                        if (!key.reset()) {
                            watchKeys.remove(key)
                            if (watchKeys.isEmpty()) break
                        }
                    }
                }
            } catch (e: InterruptedException) {
                // 线程被中断，停止监视
            } catch (e: Exception) {
                logger.error(e) { "文件监视器发生错误" }
            }
        }.apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        running = false
        watchService.close()
    }

    private fun registerDirectory(dir: Path) {
        if (!dir.exists() || !dir.isDirectory()) return

        val key = dir.register(
            watchService,
            ENTRY_CREATE,
            ENTRY_DELETE,
            ENTRY_MODIFY
        )
        watchKeys[key] = dir
        logger.debug { "注册目录: $dir" }
    }
}
