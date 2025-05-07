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
    implementation("io.ktor:ktor-client-okhttp:3.1.2")
    implementation("io.ktor:ktor-client-core:3.1.2")

    // 移除 Kotlin 反射依赖
    // implementation("org.jetbrains.kotlin:kotlin-reflect:2.1.10")

    // 排除 Truffle API 依赖项
    configurations.all {
        exclude(group = "org.graalvm.truffle", module = "truffle-api")
    }

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
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

tasks.jar {
    manifest {
        attributes(mapOf(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Main-Class" to "ai.kastrax.graal.MainNoReflection"
        ))
    }
}

tasks.test {
    useJUnitPlatform()
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
        "--initialize-at-build-time=kotlin,kotlin.jvm.internal.TypeParameterReference,ai.kastrax.graal.serialization",
        "--initialize-at-run-time=kotlin.reflect,kotlinx.datetime.serializers,kotlinx.serialization,ai.kastrax.integrations.deepseek.DeepSeekStreamChunk,kotlinx.serialization.json.Json,kotlinx.serialization.modules.SerializersModuleKt",
        "--trace-class-initialization=kotlin.reflect.KVariance",
        "--report-unsupported-elements-at-runtime",
        "--allow-incomplete-classpath",
        "-H:+ReportExceptionStackTraces",
        "-H:ReflectionConfigurationFiles=${project.projectDir}/META-INF/native-image/reflection-config.json",
        "-H:SerializationConfigurationResources=META-INF/native-image/serialization-config.json",
        "-H:ResourceConfigurationFiles=${project.projectDir}/META-INF/native-image/resource-config.json",
        "-H:+JNI",
        "-H:+AddAllCharsets",
        "-H:+PrintClassInitialization",
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

// 自定义任务，构建 DeepSeekAgentMain
tasks.register<Exec>("buildDeepSeekAgentNative") {
    dependsOn(tasks.named("jar"))

    val outputDir = layout.buildDirectory.dir("native/deepseek-agent").get().asFile
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
        "--initialize-at-build-time=kotlin,kotlin.jvm.internal.TypeParameterReference,ai.kastrax.graal.serialization",
        "--initialize-at-run-time=kotlinx.datetime.serializers,kotlinx.serialization,ai.kastrax.integrations.deepseek.DeepSeekStreamChunk,kotlinx.serialization.json.Json,kotlinx.serialization.modules.SerializersModuleKt",
        "--trace-class-initialization=kotlin.reflect.KVariance",
        "--report-unsupported-elements-at-runtime",
        "--allow-incomplete-classpath",
        "-H:+ReportExceptionStackTraces",
        "-H:ReflectionConfigurationFiles=${project.projectDir}/META-INF/native-image/reflection-config.json",
        "-H:SerializationConfigurationResources=META-INF/native-image/serialization-config.json",
        "-H:ResourceConfigurationFiles=${project.projectDir}/META-INF/native-image/resource-config.json",
        "-H:+JNI",
        "-H:+AddAllCharsets",
        "-H:+PrintClassInitialization",
        "-O2",
        "-cp", classpathString,
        "ai.kastrax.graal.agent.DeepSeekAgentMain",
        "-o", "$outputDir/deepseek-agent"
    )

    doFirst {
        println("Building DeepSeek Agent native image using: $nativeImage")
        println("Output will be in: $outputDir/deepseek-agent")
        println("Using classpath: ${classpathString}")
    }
}

// 自定义任务，构建带反射的 Native Image
tasks.register<Exec>("buildReflectionNative") {
    dependsOn(tasks.named("jar"))

    val outputDir = layout.buildDirectory.dir("native/reflection").get().asFile
    outputDir.mkdirs()

    val graalVmHome = System.getenv("GRAALVM_HOME") ?: "${System.getProperty("user.home")}/Library/Java/JavaVirtualMachines/graalvm-ce-17.0.9/Contents/Home"
    val nativeImage = "$graalVmHome/bin/native-image"

    // 收集所有运行时依赖项
    val runtimeClasspath = configurations.runtimeClasspath.get().files

    // 添加 kotlin-reflect 模块
    val kotlinReflectJar = configurations.runtimeClasspath.get().files.filter { it.name.contains("kotlin-reflect") }

    // 添加当前模块的jar
    val jarFile = tasks.jar.get().archiveFile.get().asFile

    // 组合所有类路径
    val classpathFiles = runtimeClasspath + kotlinReflectJar + jarFile
    val classpathString = classpathFiles.joinToString(":")

    commandLine = listOf(
        nativeImage,
        "--no-fallback",
        "-H:+AllowDeprecatedBuilderClassesOnImageClasspath",
        "--initialize-at-build-time",
        "--initialize-at-run-time=kotlin.reflect,kotlin.reflect.jvm.internal,kotlin.reflect.full,kotlin.reflect.KVariance",
        "--trace-class-initialization=kotlin.reflect.KVariance",

        "--report-unsupported-elements-at-runtime",
        "--allow-incomplete-classpath",
        "-H:+ReportExceptionStackTraces",
        "-H:ReflectionConfigurationFiles=${project.projectDir}/src/main/resources/META-INF/native-image/kotlin-reflect-config.json",
        "-H:+JNI",
        "-H:+AddAllCharsets",
        "-H:+PrintClassInitialization",
        "-O2",
        "-cp", classpathString,
        "ai.kastrax.graal.MainWithReflection",
        "-o", "$outputDir/kastrax-reflection"
    )

    doFirst {
        println("Building reflection native image using: $nativeImage")
        println("Output will be in: $outputDir/kastrax-reflection")
        println("Using classpath: ${classpathString}")
    }
}

// 自定义任务，构建简化版本的 Native Image
tasks.register<Exec>("buildSimpleNative") {
    dependsOn(tasks.named("jar"))

    val outputDir = layout.buildDirectory.dir("native/simple").get().asFile
    outputDir.mkdirs()

    val graalVmHome = System.getenv("GRAALVM_HOME") ?: "${System.getProperty("user.home")}/Library/Java/JavaVirtualMachines/graalvm-ce-17.0.9/Contents/Home"
    val nativeImage = "$graalVmHome/bin/native-image"

    // 收集所有运行时依赖项
    val runtimeClasspath = configurations.runtimeClasspath.get().files

    // 添加当前模块的jar
    val jarFile = tasks.jar.get().archiveFile.get().asFile

    // 组合所有类路径
    val classpathFiles = runtimeClasspath + jarFile
    val classpathString = classpathFiles.joinToString(":")

    commandLine = listOf(
        nativeImage,
        "--no-fallback",
        "-H:+AllowDeprecatedBuilderClassesOnImageClasspath",
        "--initialize-at-build-time=kotlin",
        "--initialize-at-run-time=kotlin.reflect,kotlinx.serialization",
        "--report-unsupported-elements-at-runtime",
        "--allow-incomplete-classpath",
        "-H:+ReportExceptionStackTraces",
        "-H:ResourceConfigurationFiles=${project.projectDir}/src/main/resources/META-INF/native-image/resource-config.json",
        "-H:+JNI",
        "-H:+AddAllCharsets",
        "-O2",
        "-cp", classpathString,
        "ai.kastrax.graal.SimpleNativeMain",
        "-o", "$outputDir/kastrax-simple"
    )

    doFirst {
        println("Building simple native image using: $nativeImage")
        println("Output will be in: $outputDir/kastrax-simple")
        println("Using classpath: ${classpathString}")
    }
}

// 自定义任务，构建不使用反射的 Native Image
tasks.register<Exec>("buildNoReflectionNative") {
    dependsOn(tasks.named("jar"))

    val outputDir = layout.buildDirectory.dir("native/no-reflection").get().asFile
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
        "--initialize-at-build-time=kotlin",
        "--initialize-at-run-time=kotlin.reflect,kotlinx.serialization,ai.kastrax.integrations.deepseek.DeepSeekStreamChunk",
        "--initialize-at-run-time=kotlin.jvm.internal.TypeParameterReference${'$'}Companion${'$'}WhenMappings",
        "-H:+ReportExceptionStackTraces",
        "-H:ResourceConfigurationFiles=${project.projectDir}/src/main/resources/META-INF/native-image/resource-config.json",
        "-H:SerializationConfigurationResources=META-INF/native-image/serialization-config.json",
        "-H:+JNI",
        "-H:+AddAllCharsets",
        "-O2",
        "-cp", classpathString,
        "ai.kastrax.graal.MainNoReflection",
        "-o", "$outputDir/kastrax-no-reflection"
    )

    doFirst {
        println("Building no-reflection native image using: $nativeImage")
        println("Output will be in: $outputDir/kastrax-no-reflection")
        println("Using classpath: ${classpathString}")
    }
}

// 自定义任务，构建全部初始化的 Native Image
tasks.register<Exec>("buildAllInitNative") {
    dependsOn(tasks.named("jar"))

    val outputDir = layout.buildDirectory.dir("native/all-init").get().asFile
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
        "--initialize-at-build-time=kotlin,kotlin.jvm.internal,kotlinx",
        "-H:+AllowDeprecatedInitializeAllClassesAtBuildTime",
        "-H:+ReportExceptionStackTraces",
        "-H:ResourceConfigurationFiles=${project.projectDir}/src/main/resources/META-INF/native-image/resource-config.json",
        "-H:+JNI",
        "-H:+AddAllCharsets",
        "-O2",
        "-cp", classpathString,
        "ai.kastrax.graal.MainKt",
        "-o", "$outputDir/kastrax-all-init"
    )

    doFirst {
        println("Building all-init native image using: $nativeImage")
        println("Output will be in: $outputDir/kastrax-all-init")
        println("Using classpath: ${classpathString}")
    }
}

// 自定义任务，使用更简单的方法构建 Native Image
tasks.register<Exec>("buildSimpleNative2") {
    dependsOn(tasks.named("jar"))

    val outputDir = layout.buildDirectory.dir("native/simple").get().asFile
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
        "--initialize-at-build-time=kotlin",
        "--initialize-at-run-time=kotlin.reflect,kotlinx.serialization",
        "--initialize-at-run-time=kotlin.jvm.internal.TypeParameterReference",
        "--initialize-at-run-time=ai.kastrax.integrations.deepseek.DeepSeekStreamChunk",
        "-H:+ReportExceptionStackTraces",
        "-cp", classpathString,
        "ai.kastrax.graal.MainNoReflection",
        "-o", "$outputDir/kastrax-simple"
    )

    doFirst {
        println("Building simple native image using: $nativeImage")
        println("Output will be in: $outputDir/kastrax-simple")
        println("Using classpath: ${classpathString}")
    }
}

// 自定义任务，构建完整功能的 KastraX Native Image
tasks.register<Exec>("buildKastraxNative") {
    dependsOn(tasks.named("jar"))

    val outputDir = layout.buildDirectory.dir("native/kastrax").get().asFile
    outputDir.mkdirs()

    val graalVmHome = System.getenv("GRAALVM_HOME") ?: "${System.getProperty("user.home")}/Library/Java/JavaVirtualMachines/graalvm-ce-17.0.9/Contents/Home"
    val nativeImage = "$graalVmHome/bin/native-image"

    // 收集所有运行时依赖项
    val runtimeClasspath = configurations.runtimeClasspath.get().files

    // 添加 kastrax-core 模块
    val coreJar = project(":kastrax-core").tasks.named("jar").get().outputs.files

    // 添加 kastrax-deepseek 模块
    val deepseekJar = project(":kastrax-integrations:kastrax-deepseek").tasks.named("jar").get().outputs.files

    // 添加当前模块的jar
    val jarFile = tasks.jar.get().archiveFile.get().asFile

    // 组合所有类路径
    val classpathFiles = runtimeClasspath + coreJar + deepseekJar + jarFile
    val classpathString = classpathFiles.joinToString(":")

    commandLine = listOf(
        nativeImage,
        "--no-fallback",
        "-H:+AllowDeprecatedBuilderClassesOnImageClasspath",
        "--initialize-at-build-time=kotlin",
        "--initialize-at-run-time=kotlin.reflect,kotlinx.serialization",
        "--initialize-at-run-time=kotlin.jvm.internal.TypeParameterReference",
        "--initialize-at-run-time=ai.kastrax.integrations.deepseek.DeepSeekStreamChunk",
        "--initialize-at-run-time=ai.kastrax.core.agent",
        "--initialize-at-run-time=ai.kastrax.core.tools",
        "--initialize-at-run-time=io.ktor.client.engine.okhttp",
        "--initialize-at-run-time=okhttp3",
        "-H:+ReportExceptionStackTraces",
        "-H:ResourceConfigurationFiles=${project.projectDir}/src/main/resources/META-INF/native-image/resource-config.json",
        "-H:SerializationConfigurationResources=META-INF/native-image/serialization-config.json",
        "-H:+JNI",
        "-H:+AddAllCharsets",
        "-O2",
        "-cp", classpathString,
        "ai.kastrax.graal.KastraxNative",
        "-o", "$outputDir/kastrax"
    )

    doFirst {
        println("Building KastraX native image using: $nativeImage")
        println("Output will be in: $outputDir/kastrax")
        println("Using classpath: ${classpathString}")
    }
}

// 自定义任务，构建优化版的 Native Image
tasks.register<Exec>("buildOptimizedNative") {
    dependsOn(tasks.named("jar"))

    val outputDir = layout.buildDirectory.dir("native/optimized").get().asFile
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
        "--initialize-at-build-time=kotlin",
        "--initialize-at-run-time=kotlin.reflect,kotlinx.serialization",
        "--initialize-at-run-time=kotlin.jvm.internal.TypeParameterReference",
        "--initialize-at-run-time=ai.kastrax.integrations.deepseek.DeepSeekStreamChunk",
        // 性能优化选项
        "-O3",                          // 最高级别的优化
        "-march=native",                // 使用本地CPU特性
        "--gc=serial",                  // 使用串行垃圾收集器，适合小型应用
        "-R:MaxHeapSize=64m",           // 设置最大堆大小
        // 减小镜像大小的选项
        "-H:+RemoveUnusedSymbols",      // 移除未使用的符号
        "-H:+FoldSecurityManagerGetter", // 折叠SecurityManager getter
        "-H:+RemoveSaturatedTypeFlows", // 移除饱和类型流
        "-H:-SpawnIsolates",            // 禁用生成隔离区
        "-H:+ReportExceptionStackTraces",
        "-cp", classpathString,
        "ai.kastrax.graal.MainNoReflection",
        "-o", "$outputDir/kastrax-optimized"
    )

    doFirst {
        println("Building optimized native image using: $nativeImage")
        println("Output will be in: $outputDir/kastrax-optimized")
        println("Using classpath: ${classpathString}")
    }
}

// 自定义任务，构建纯 Java 版本的 Native Image
tasks.register<Exec>("buildJavaNative") {
    dependsOn(tasks.named("jar"))

    val outputDir = layout.buildDirectory.dir("native/java").get().asFile
    outputDir.mkdirs()

    val graalVmHome = System.getenv("GRAALVM_HOME") ?: "${System.getProperty("user.home")}/Library/Java/JavaVirtualMachines/graalvm-ce-17.0.9/Contents/Home"
    val nativeImage = "$graalVmHome/bin/native-image"

    // 添加当前模块的jar
    val jarFile = tasks.jar.get().archiveFile.get().asFile

    commandLine = listOf(
        nativeImage,
        "--no-fallback",
        "-H:+ReportExceptionStackTraces",
        "-H:+JNI",
        "-H:+AddAllCharsets",
        "-O2",
        "-cp", jarFile.absolutePath,
        "ai.kastrax.graal.JavaMain",
        "-o", "$outputDir/kastrax-java"
    )

    doFirst {
        println("Building Java native image using: $nativeImage")
        println("Output will be in: $outputDir/kastrax-java")
        println("Using jar: ${jarFile.absolutePath}")
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

tasks.register<Jar>("simpleHelloJar") {
    dependsOn("compileKotlin")
    archiveBaseName.set("simple-hello")
    destinationDirectory.set(layout.buildDirectory.dir("libs"))

    from(sourceSets["main"].output) {
        include("ai/kastrax/graal/SimpleHello*")
    }

    manifest {
        attributes(mapOf(
            "Main-Class" to "ai.kastrax.graal.SimpleHello"
        ))
    }
}

tasks.register<Jar>("javaHelloJar") {
    dependsOn("compileJava")
    archiveBaseName.set("java-hello")
    destinationDirectory.set(layout.buildDirectory.dir("libs"))

    from(sourceSets["main"].output) {
        include("ai/kastrax/graal/JavaHello*")
    }

    manifest {
        attributes(mapOf(
            "Main-Class" to "ai.kastrax.graal.JavaHello"
        ))
    }
}

tasks.register<Exec>("buildJavaHelloNative") {
    dependsOn("javaHelloJar")

    val outputDir = layout.buildDirectory.dir("native/java-hello").get().asFile
    outputDir.mkdirs()

    val graalVmHome = System.getenv("GRAALVM_HOME") ?: "${System.getProperty("user.home")}/Library/Java/JavaVirtualMachines/graalvm-ce-17.0.9/Contents/Home"
    val nativeImage = "$graalVmHome/bin/native-image"
    val jarFile = tasks.named<Jar>("javaHelloJar").get().archiveFile.get().asFile

    commandLine = listOf(
        nativeImage,
        "--no-fallback",
        "-H:+ReportExceptionStackTraces",
        "-jar", jarFile.absolutePath,
        "-o", "$outputDir/java-hello"
    )

    doFirst {
        println("Building Java Hello native image using: $nativeImage")
        println("Output will be in: $outputDir/java-hello")
    }
}

tasks.register<Exec>("buildSimpleHelloNative") {
    dependsOn("simpleHelloJar")

    val outputDir = layout.buildDirectory.dir("native/simple-hello").get().asFile
    outputDir.mkdirs()

    val graalVmHome = System.getenv("GRAALVM_HOME") ?: "${System.getProperty("user.home")}/Library/Java/JavaVirtualMachines/graalvm-ce-17.0.9/Contents/Home"
    val nativeImage = "$graalVmHome/bin/native-image"
    val jarFile = tasks.named<Jar>("simpleHelloJar").get().archiveFile.get().asFile

    commandLine = listOf(
        nativeImage,
        "--no-fallback",
        "--initialize-at-build-time=kotlin",
        "-H:+ReportExceptionStackTraces",
        "-jar", jarFile.absolutePath,
        "-o", "$outputDir/simple-hello"
    )

    doFirst {
        println("Building Simple Hello native image using: $nativeImage")
        println("Output will be in: $outputDir/simple-hello")
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
