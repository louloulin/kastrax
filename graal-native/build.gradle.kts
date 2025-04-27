import org.graalvm.buildtools.gradle.tasks.BuildNativeImageTask

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.graalvm.buildtools.native")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(project(":kastrax-core"))
    implementation(project(":kastrax-memory-api"))
    implementation(project(":kastrax-memory-impl"))
    implementation(project(":kastrax-rag"))
    implementation(project(":kastrax-integrations:kastrax-deepseek"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation("ch.qos.logback:logback-classic:1.4.11")

    // 排除 Truffle API 依赖项
    configurations.all {
        exclude(group = "org.graalvm.truffle", module = "truffle-api")
    }

    testImplementation(kotlin("test"))
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("kastrax")
            mainClass.set("ai.kastrax.graal.MainKt")
            buildArgs.add("--no-fallback")
            buildArgs.add("-H:+ReportExceptionStackTraces")
            buildArgs.add("-H:ReflectionConfigurationFiles=${project.projectDir}/src/main/resources/META-INF/native-image/reflection-config.json")
            buildArgs.add("-H:ResourceConfigurationFiles=${project.projectDir}/src/main/resources/META-INF/native-image/resource-config.json")

            // Enable JNI support for language SDKs
            buildArgs.add("-H:+JNI")

            // Optimize for performance
            buildArgs.add("-O2")

            // 在新版本中，不再需要显式指定目标平台
            // 插件会自动为当前平台构建
        }
    }

    // 配置工具链
    toolchainDetection.set(false)
}

tasks.withType<BuildNativeImageTask> {
    dependsOn(tasks.named("jar"))
}

// 自定义任务，手动构建 Native Image
tasks.register<Exec>("buildNativeManually") {
    dependsOn(tasks.named("jar"))

    val outputDir = layout.buildDirectory.dir("native/manual").get().asFile
    outputDir.mkdirs()

    val graalVmHome = System.getenv("GRAALVM_HOME") ?: "${System.getProperty("user.home")}/Library/Java/JavaVirtualMachines/graalvm-ce-17.0.9/Contents/Home"
    val nativeImage = "$graalVmHome/bin/native-image"

    // 收集所有运行时依赖项
    val runtimeClasspath = configurations.runtimeClasspath.get().files

    // 添加 kastrax-deepseek 模块
    val deepseekJar = project(":kastrax-integrations:kastrax-deepseek").tasks.named("jar").get().outputs.files

    // 添加当前模块的jar
    val jarFile = tasks.jar.get().archiveFile.get().asFile

    // 组合所有类路径
    val classpathFiles = runtimeClasspath + deepseekJar + jarFile
    val classpathString = classpathFiles.joinToString(":")

    commandLine = listOf(
        nativeImage,
        "--no-fallback",
        "-H:+AllowDeprecatedBuilderClassesOnImageClasspath",
        "--initialize-at-build-time=kotlin.DeprecationLevel",
        "-H:+ReportExceptionStackTraces",
        "-H:ReflectionConfigurationFiles=${project.projectDir}/src/main/resources/META-INF/native-image/reflection-config.json",
        "-H:ResourceConfigurationFiles=${project.projectDir}/src/main/resources/META-INF/native-image/resource-config.json",
        "-H:+JNI",
        "-O2",
        "-cp", classpathString,
        "ai.kastrax.graal.MainKt",
        "-o", "$outputDir/kastrax"
    )

    doFirst {
        println("Building native image using: $nativeImage")
        println("Output will be in: $outputDir/kastrax")
        println("Using classpath: ${classpathString}")
    }
}

// 创建一个更简单的示例程庋来测试 GraalVM Native Image
tasks.register<JavaExec>("createHelloWorldJar") {
    mainClass.set("ai.kastrax.graal.HelloWorldKt")
    classpath = sourceSets["main"].runtimeClasspath

    doFirst {
        // 创建 HelloWorld.kt 文件
        val helloWorldFile = File("${project.projectDir}/src/main/kotlin/ai/kastrax/graal/HelloWorld.kt")
        helloWorldFile.parentFile.mkdirs()
        helloWorldFile.writeText("""
            package ai.kastrax.graal

            fun main() {
                println("Hello, Native World!")
            }
        """.trimIndent())
    }
}

tasks.register<Jar>("helloWorldJar") {
    dependsOn("compileKotlin")
    archiveBaseName.set("hello-world")
    destinationDirectory.set(layout.buildDirectory.dir("libs"))

    from(sourceSets["main"].output) {
        include("ai/kastrax/graal/HelloWorld*")
    }

    manifest {
        attributes(mapOf(
            "Main-Class" to "ai.kastrax.graal.HelloWorldKt"
        ))
    }
}

tasks.register<Exec>("buildHelloWorldNative") {
    dependsOn("helloWorldJar")

    val outputDir = layout.buildDirectory.dir("native/hello-world").get().asFile
    outputDir.mkdirs()

    val graalVmHome = System.getenv("GRAALVM_HOME") ?: "${System.getProperty("user.home")}/Library/Java/JavaVirtualMachines/graalvm-ce-17.0.9/Contents/Home"
    val nativeImage = "$graalVmHome/bin/native-image"
    val jarFile = tasks.named<Jar>("helloWorldJar").get().archiveFile.get().asFile

    commandLine = listOf(
        nativeImage,
        "--no-fallback",
        "-jar", jarFile.absolutePath,
        "-o", "$outputDir/hello-world"
    )

    doFirst {
        println("Building Hello World native image using: $nativeImage")
        println("Output will be in: $outputDir/hello-world")
    }
}

// Create a task to package the native image with necessary resources
tasks.register<Zip>("packageNative") {
    dependsOn(tasks.named("nativeCompile"))

    archiveFileName.set("kastrax-native-${project.version}-${osName()}.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))

    from(layout.buildDirectory.dir("native/nativeCompile")) {
        include("kastrax*")
    }

    from(project.projectDir) {
        include("README.md")
        include("LICENSE")
    }

    from(layout.projectDirectory.dir("src/main/resources")) {
        include("config/**")
        into("resources")
    }
}

// Helper function to determine OS name
fun osName(): String {
    val os = System.getProperty("os.name").lowercase()
    return when {
        os.contains("win") -> "windows"
        os.contains("mac") -> "macos"
        else -> "linux"
    }
}
