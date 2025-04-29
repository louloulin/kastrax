package actor.proto.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import java.io.File

/**
 * 简化的 Native 编译插件
 * 
 * 这个插件提供了一种简单的方式来构建 Native Image，无需复杂的配置
 */
class NativePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // 创建扩展
        val extension = project.extensions.create<NativeExtension>("nativeCompile")
        
        // 创建目录
        project.tasks.register("createNativeDirectories") {
            group = "Native"
            description = "创建 Native 编译所需的目录"
            
            doLast {
                File(project.layout.buildDirectory.get().asFile, "native").mkdirs()
                File(project.layout.buildDirectory.get().asFile, "native/config").mkdirs()
            }
        }
        
        // 使用 Agent 运行应用程序以生成配置
        project.tasks.register<JavaExec>("runWithAgent") {
            group = "Native"
            description = "使用 GraalVM Agent 运行应用程序以生成配置"
            
            dependsOn("createNativeDirectories")
            
            mainClass.set(extension.mainClass)
            classpath = project.tasks.getByName("run").inputs.files
            
            doFirst {
                // 设置 Agent 参数
                jvmArgs = listOf(
                    "-agentlib:native-image-agent=config-output-dir=${project.layout.buildDirectory.get().asFile}/native/config",
                    "-Dorg.graalvm.nativeimage.imagecode=agent"
                )
            }
        }
        
        // 编译 Native Image
        project.tasks.register<Exec>("compileNative") {
            group = "Native"
            description = "编译 Native Image"
            
            dependsOn("jar")
            
            doFirst {
                val nativeImageExecutable = findNativeImage()
                if (nativeImageExecutable == null) {
                    throw IllegalStateException("无法找到 native-image 工具。请确保安装了 GraalVM 并运行 'gu install native-image'。")
                }
                
                val jarTask = project.tasks.getByName("jar")
                val jarFile = jarTask.outputs.files.singleFile
                
                val outputDir = File(project.layout.buildDirectory.get().asFile, "native")
                val outputFile = File(outputDir, extension.imageName.get())
                
                val configDir = File(project.layout.buildDirectory.get().asFile, "native/config")
                
                // 构建命令
                commandLine = listOfNotNull(
                    nativeImageExecutable,
                    "-cp", jarFile.absolutePath,
                    "-H:ConfigurationFileDirectories=${configDir.absolutePath}",
                    "--no-fallback",
                    "--report-unsupported-elements-at-runtime",
                    "-H:+ReportExceptionStackTraces",
                    *extension.buildArgs.get().toTypedArray(),
                    "-o", outputFile.absolutePath,
                    extension.mainClass.get()
                )
                
                // 设置工作目录
                workingDir = project.projectDir
            }
        }
        
        // 运行 Native Image
        project.tasks.register<Exec>("runNative") {
            group = "Native"
            description = "运行 Native Image"
            
            dependsOn("compileNative")
            
            doFirst {
                val outputDir = File(project.layout.buildDirectory.get().asFile, "native")
                val outputFile = File(outputDir, extension.imageName.get())
                
                if (!outputFile.exists()) {
                    throw IllegalStateException("Native Image 不存在。请先运行 compileNative 任务。")
                }
                
                // 确保文件可执行
                outputFile.setExecutable(true)
                
                commandLine = listOf(outputFile.absolutePath, *extension.runtimeArgs.get().toTypedArray())
            }
        }
        
        // 一键构建 Native Image
        project.tasks.register("buildNative") {
            group = "Native"
            description = "一键构建 Native Image（生成配置并编译）"
            
            dependsOn("runWithAgent", "compileNative")
        }
    }
    
    private fun findNativeImage(): String? {
        val javaHome = System.getProperty("java.home")
        val nativeImageExecutable = File(javaHome, "bin/native-image")
        
        return if (nativeImageExecutable.exists()) {
            nativeImageExecutable.absolutePath
        } else {
            // 尝试在 PATH 中查找
            "which native-image".runCommand()?.trim()
        }
    }
    
    private fun String.runCommand(): String? {
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
}

/**
 * Native 编译扩展
 */
open class NativeExtension(project: Project) {
    /**
     * Native Image 的名称
     */
    val imageName = project.objects.property(String::class.java).convention(project.name)
    
    /**
     * 主类
     */
    val mainClass = project.objects.property(String::class.java)
    
    /**
     * 构建参数
     */
    val buildArgs = project.objects.listProperty(String::class.java).convention(listOf())
    
    /**
     * 运行参数
     */
    val runtimeArgs = project.objects.listProperty(String::class.java).convention(listOf())
}
