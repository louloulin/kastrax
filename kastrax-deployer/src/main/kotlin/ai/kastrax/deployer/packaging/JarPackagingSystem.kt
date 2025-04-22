package ai.kastrax.deployer.packaging

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarFile
import java.util.jar.Manifest

private val logger = KotlinLogging.logger {}

/**
 * JAR 打包系统实现。
 */
class JarPackagingSystem : PackagingSystem {
    override val name: String = "JAR Packaging System"

    override fun packageApplication(projectPath: Path, config: PackagingConfig): PackagingResult {
        logger.info { "Packaging application as JAR: ${config.name}-${config.version}" }

        val logs = mutableListOf<String>()

        try {
            // 检查项目路径是否存在
            if (!Files.exists(projectPath)) {
                return PackagingResult(
                    success = false,
                    message = "Project path does not exist: $projectPath"
                )
            }

            // 检查是否是 Gradle 项目
            val isGradleProject = Files.exists(projectPath.resolve("build.gradle")) ||
                                  Files.exists(projectPath.resolve("build.gradle.kts"))

            // 检查是否是 Maven 项目
            val isMavenProject = Files.exists(projectPath.resolve("pom.xml"))

            if (!isGradleProject && !isMavenProject) {
                return PackagingResult(
                    success = false,
                    message = "Not a Gradle or Maven project"
                )
            }

            // 构建项目
            val buildSuccess = if (isGradleProject) {
                buildWithGradle(projectPath, logs)
            } else {
                buildWithMaven(projectPath, logs)
            }

            if (!buildSuccess) {
                return PackagingResult(
                    success = false,
                    message = "Build failed",
                    logs = logs
                )
            }

            // 查找构建产物
            val jarFile = findJarFile(projectPath, config, isGradleProject)

            if (jarFile == null) {
                return PackagingResult(
                    success = false,
                    message = "JAR file not found",
                    logs = logs
                )
            }

            // 如果指定了主类，检查并更新 MANIFEST.MF
            if (config.mainClass != null && !hasMainClass(jarFile, config.mainClass)) {
                updateManifest(jarFile, config.mainClass, logs)
            }

            return PackagingResult(
                success = true,
                packageFile = jarFile,
                message = "Package created successfully: ${jarFile.name}",
                logs = logs
            )
        } catch (e: Exception) {
            logger.error(e) { "Error packaging application" }
            return PackagingResult(
                success = false,
                message = "Error packaging application: ${e.message}",
                logs = logs
            )
        }
    }

    override fun validatePackage(packageFile: File): Boolean {
        if (!packageFile.exists() || !packageFile.isFile || !packageFile.name.endsWith(".jar")) {
            return false
        }

        try {
            // 尝试打开 JAR 文件
            JarFile(packageFile).use { jar ->
                // 检查 MANIFEST.MF 是否存在
                val manifest = jar.manifest
                return manifest != null
            }
        } catch (e: Exception) {
            logger.error(e) { "Error validating JAR file" }
            return false
        }
    }

    /**
     * 使用 Gradle 构建项目。
     *
     * @param projectPath 项目路径
     * @param logs 日志列表
     * @return 是否成功
     */
    private fun buildWithGradle(projectPath: Path, logs: MutableList<String>): Boolean {
        val gradleCommand = if (Files.exists(projectPath.resolve("gradlew"))) {
            "./gradlew"
        } else {
            "gradle"
        }

        return executeCommand(
            command = "$gradleCommand clean build -x test",
            workingDir = projectPath.toFile(),
            logs = logs
        )
    }

    /**
     * 使用 Maven 构建项目。
     *
     * @param projectPath 项目路径
     * @param logs 日志列表
     * @return 是否成功
     */
    private fun buildWithMaven(projectPath: Path, logs: MutableList<String>): Boolean {
        val mvnCommand = if (Files.exists(projectPath.resolve("mvnw"))) {
            "./mvnw"
        } else {
            "mvn"
        }

        return executeCommand(
            command = "$mvnCommand clean package -DskipTests",
            workingDir = projectPath.toFile(),
            logs = logs
        )
    }

    /**
     * 执行命令。
     *
     * @param command 命令
     * @param workingDir 工作目录
     * @param logs 日志列表
     * @return 是否成功
     */
    private fun executeCommand(command: String, workingDir: File, logs: MutableList<String>): Boolean {
        logger.info { "Executing command: $command" }

        val process = ProcessBuilder()
            .command(command.split(" "))
            .directory(workingDir)
            .redirectErrorStream(true)
            .start()

        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let {
                    logs.add(it)
                    logger.debug { it }
                }
            }
        }

        val exitCode = process.waitFor()
        logger.info { "Command completed with exit code: $exitCode" }

        return exitCode == 0
    }

    /**
     * 查找 JAR 文件。
     *
     * @param projectPath 项目路径
     * @param config 打包配置
     * @param isGradleProject 是否是 Gradle 项目
     * @return JAR 文件
     */
    private fun findJarFile(projectPath: Path, config: PackagingConfig, isGradleProject: Boolean): File? {
        val buildDir = if (isGradleProject) {
            projectPath.resolve("build/libs")
        } else {
            projectPath.resolve("target")
        }

        if (!Files.exists(buildDir)) {
            logger.error { "Build directory not found: $buildDir" }
            return null
        }

        // 查找匹配的 JAR 文件
        val jarFiles = Files.list(buildDir)
            .filter { it.toString().endsWith(".jar") }
            .filter { !it.toString().contains("-sources") && !it.toString().contains("-javadoc") }
            .map { it.toFile() }
            .sorted { a, b -> b.lastModified().compareTo(a.lastModified()) } // 按修改时间降序排序
            .toList()

        if (jarFiles.isEmpty()) {
            logger.error { "No JAR files found in $buildDir" }
            return null
        }

        // 尝试找到匹配名称的 JAR 文件
        val namePattern = "${config.name}-${config.version}"
        val matchingJar = jarFiles.find { it.name.contains(namePattern) }

        return matchingJar ?: jarFiles.first() // 如果没有匹配的，返回最新的 JAR 文件
    }

    /**
     * 检查 JAR 文件是否包含指定的主类。
     *
     * @param jarFile JAR 文件
     * @param mainClass 主类名称
     * @return 是否包含
     */
    private fun hasMainClass(jarFile: File, mainClass: String): Boolean {
        try {
            JarFile(jarFile).use { jar ->
                val manifest = jar.manifest ?: return false
                val mainAttributes = manifest.mainAttributes
                val mainClassAttr = mainAttributes.getValue("Main-Class")
                return mainClassAttr == mainClass
            }
        } catch (e: Exception) {
            logger.error(e) { "Error checking main class in JAR" }
            return false
        }
    }

    /**
     * 更新 JAR 文件的 MANIFEST.MF。
     *
     * @param jarFile JAR 文件
     * @param mainClass 主类名称
     * @param logs 日志列表
     * @return 是否成功
     */
    private fun updateManifest(jarFile: File, mainClass: String, logs: MutableList<String>): Boolean {
        // 创建临时目录
        val tempDir = Files.createTempDirectory("jar-update")

        try {
            // 解压 JAR 文件
            val unjarCommand = "jar xf ${jarFile.absolutePath}"
            if (!executeCommand(unjarCommand, tempDir.toFile(), logs)) {
                return false
            }

            // 创建或更新 MANIFEST.MF
            val manifestDir = tempDir.resolve("META-INF")
            Files.createDirectories(manifestDir)

            val manifestFile = manifestDir.resolve("MANIFEST.MF")
            val manifestContent = if (Files.exists(manifestFile)) {
                val manifest = Manifest(Files.newInputStream(manifestFile))
                manifest.mainAttributes.putValue("Main-Class", mainClass)

                val baos = java.io.ByteArrayOutputStream()
                manifest.write(baos)
                String(baos.toByteArray())
            } else {
                "Manifest-Version: 1.0\nMain-Class: $mainClass\n"
            }

            Files.write(manifestFile, manifestContent.toByteArray())

            // 重新打包 JAR 文件
            val rejarCommand = "jar cfm ${jarFile.absolutePath} ${manifestFile} -C ${tempDir.toFile().absolutePath} ."
            return executeCommand(rejarCommand, tempDir.toFile(), logs)
        } finally {
            // 清理临时目录
            tempDir.toFile().deleteRecursively()
        }
    }
}
