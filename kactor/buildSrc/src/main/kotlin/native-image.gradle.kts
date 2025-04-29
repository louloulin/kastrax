import java.io.File

// 这是一个简化的 Native Image 构建脚本
// 它使用约定优于配置的方式，减少了配置的复杂性

// 创建扩展
val nativeCompile = extensions.create<NativeCompileExtension>("nativeCompile")

// 创建目录
tasks.register("createNativeDirectories") {
    group = "Native"
    description = "创建 Native 编译所需的目录"
    
    doLast {
        File(layout.buildDirectory.get().asFile, "native").mkdirs()
        File(layout.buildDirectory.get().asFile, "native/config").mkdirs()
    }
}

// 使用 Agent 运行应用程序以生成配置
tasks.register<JavaExec>("runWithAgent") {
    group = "Native"
    description = "使用 GraalVM Agent 运行应用程序以生成配置"
    
    dependsOn("createNativeDirectories")
    
    mainClass.set(nativeCompile.mainClass)
    classpath = tasks.getByName("run").inputs.files
    
    doFirst {
        // 设置 Agent 参数
        jvmArgs = listOf(
            "-agentlib:native-image-agent=config-output-dir=${layout.buildDirectory.get().asFile}/native/config",
            "-Dorg.graalvm.nativeimage.imagecode=agent"
        )
    }
}

// 编译 Native Image
tasks.register<Exec>("compileNative") {
    group = "Native"
    description = "编译 Native Image"
    
    dependsOn("jar")
    
    doFirst {
        val nativeImageExecutable = findNativeImage()
        if (nativeImageExecutable == null) {
            throw IllegalStateException("无法找到 native-image 工具。请确保安装了 GraalVM 并运行 'gu install native-image'。")
        }
        
        val jarTask = tasks.getByName("jar")
        val jarFile = jarTask.outputs.files.singleFile
        
        val outputDir = File(layout.buildDirectory.get().asFile, "native")
        val outputFile = File(outputDir, nativeCompile.imageName.get())
        
        val configDir = File(layout.buildDirectory.get().asFile, "native/config")
        
        // 构建命令
        commandLine = listOfNotNull(
            nativeImageExecutable,
            "-cp", jarFile.absolutePath,
            "-H:ConfigurationFileDirectories=${configDir.absolutePath}",
            "--no-fallback",
            "--report-unsupported-elements-at-runtime",
            "-H:+ReportExceptionStackTraces",
            *nativeCompile.buildArgs.get().toTypedArray(),
            "-o", outputFile.absolutePath,
            nativeCompile.mainClass.get()
        )
        
        // 设置工作目录
        workingDir = projectDir
    }
}

// 运行 Native Image
tasks.register<Exec>("runNative") {
    group = "Native"
    description = "运行 Native Image"
    
    dependsOn("compileNative")
    
    doFirst {
        val outputDir = File(layout.buildDirectory.get().asFile, "native")
        val outputFile = File(outputDir, nativeCompile.imageName.get())
        
        if (!outputFile.exists()) {
            throw IllegalStateException("Native Image 不存在。请先运行 compileNative 任务。")
        }
        
        // 确保文件可执行
        outputFile.setExecutable(true)
        
        commandLine = listOf(outputFile.absolutePath, *nativeCompile.runtimeArgs.get().toTypedArray())
    }
}

// 一键构建 Native Image
tasks.register("buildNative") {
    group = "Native"
    description = "一键构建 Native Image（生成配置并编译）"
    
    dependsOn("runWithAgent", "compileNative")
}

// 查找 native-image 工具
fun findNativeImage(): String? {
    val javaHome = System.getProperty("java.home")
    val nativeImageExecutable = File(javaHome, "bin/native-image")
    
    return if (nativeImageExecutable.exists()) {
        nativeImageExecutable.absolutePath
    } else {
        // 尝试在 PATH 中查找
        "which native-image".runCommand()?.trim()
    }
}

// 执行命令
fun String.runCommand(): String? {
    return try {
        val process = ProcessBuilder(*split(" ").toTypedArray())
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()
        
        process.inputStream.bufferedReader().readText()
    } catch (e: Exception) {
        null
    }
}

// Native 编译扩展
open class NativeCompileExtension {
    /**
     * Native Image 的名称
     */
    val imageName = objects.property(String::class.java).convention(project.name)
    
    /**
     * 主类
     */
    val mainClass = objects.property(String::class.java)
    
    /**
     * 构建参数
     */
    val buildArgs = objects.listProperty(String::class.java).convention(listOf())
    
    /**
     * 运行参数
     */
    val runtimeArgs = objects.listProperty(String::class.java).convention(listOf())
}
