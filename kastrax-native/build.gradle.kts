import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

// 添加到gradle.properties文件中禁用默认层次模板
project.extensions.extraProperties["kotlin.mpp.applyDefaultHierarchyTemplate"] = "false"

group = "ai.kastrax.native"
version = "0.1.0"

// 定义全局变量
val hostOs = System.getProperty("os.name")
val isArm64 = System.getProperty("os.arch") == "aarch64"
val isMingwX64 = hostOs.startsWith("Windows")

// 定义全局变量
var nativeTarget: org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget? = null

// 配置 JVM 工具链
kotlin {
    jvmToolchain(17)

    // 根据操作系统配置目标平台
    // 使用安全的方式检测平台
    try {
        nativeTarget = when {
            hostOs == "Mac OS X" && isArm64 -> macosArm64("native")
            hostOs == "Mac OS X" && !isArm64 -> macosX64("native")
            hostOs == "Linux" && isArm64 -> linuxArm64("native")
            hostOs == "Linux" && !isArm64 -> linuxX64("native")
            isMingwX64 -> mingwX64("native")
            else -> {
                println("\n\u8b66告: 不支持的操作系统: $hostOs. 仅构建JVM目标.")
                null
            }
        }
    } catch (e: Exception) {
        println("\n\u8b66告: 无法初始化Native目标: ${e.message}. 仅构建JVM目标.")
        nativeTarget = null
    }

    // 配置JVM目标
    jvm {
        withJava()
    }

    // 配置Native目标
    nativeTarget?.apply {
        binaries {
            executable {
                entryPoint = "ai.kastrax.native.main"
                baseName = "kastrax-native"

                // 优化设置
                freeCompilerArgs += listOf(
                    "-opt-in=kotlin.ExperimentalStdlibApi",
                    "-opt-in=kotlin.ExperimentalUnsignedTypes",
                    "-opt-in=kotlinx.cinterop.ExperimentalForeignApi"
                )
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // 公共依赖
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")
                implementation("io.ktor:ktor-client-core:3.1.2")
                implementation("io.ktor:ktor-client-content-negotiation:3.1.2")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.1.2")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }

        val jvmMain by getting {
            dependencies {
                // JVM特定依赖
                implementation(project(":kastrax-core"))
                implementation(project(":kastrax-memory-api"))
                implementation(project(":kastrax-memory-impl"))
                implementation(project(":kastrax-integrations:kastrax-deepseek"))
                implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
                implementation("ch.qos.logback:logback-classic:1.4.11")
                implementation("io.ktor:ktor-client-java:3.1.2")
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
                implementation("org.junit.jupiter:junit-jupiter-engine:5.10.0")
                implementation("io.mockk:mockk:1.13.8")
            }
        }

        val nativeMain by getting {
            dependencies {
                // Native特定依赖
                implementation("io.ktor:ktor-client-curl:3.1.2")
            }
        }

        val nativeTest by getting
    }
}

// 配置Native二进制文件的输出目录
if (nativeTarget != null) {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink>().configureEach {
        val outputDir = layout.buildDirectory.dir("bin").get().asFile
        destinationDirectory.set(outputDir)
    }
}

// 配置JVM JAR
tasks.register<Jar>("fatJar") {
    dependsOn(tasks.named("jvmJar"))
    archiveBaseName.set("${project.name}-full")
    from(kotlin.targets.getByName("jvm").compilations.getByName("main").output)

    // 添加依赖
    val dependencies = configurations.named("jvmRuntimeClasspath").get().map { if (it.isDirectory) it else zipTree(it) }
    from(dependencies)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // 添加清单
    manifest {
        attributes(
            "Main-Class" to "ai.kastrax.native.MainKt",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version
        )
    }
}

// 创建一个任务来构建所有平台的可执行文件
tasks.register("buildAllExecutables") {
    group = "build"
    description = "Build executables for all platforms"

    if (nativeTarget != null) {
        dependsOn("linkReleaseExecutableNative")
    } else {
        doLast {
            println("\u8b66告: Native目标不可用，跳过Native构建")
        }
    }
}

// 创建一个任务来运行Native可执行文件
tasks.register("runNative") {
    group = "application"
    description = "Run the native executable"

    if (nativeTarget != null) {
        dependsOn("linkReleaseExecutableNative")

        doLast {
            val hostOs = System.getProperty("os.name")
            val isArm64 = System.getProperty("os.arch") == "aarch64"
            val isMingwX64 = hostOs.startsWith("Windows")

            val outputDir = layout.buildDirectory.dir("bin").get().asFile

            val executablePath = when {
                hostOs == "Mac OS X" && isArm64 -> "$outputDir/native/releaseExecutable/kastrax-native.kexe"
                hostOs == "Mac OS X" && !isArm64 -> "$outputDir/native/releaseExecutable/kastrax-native.kexe"
                hostOs == "Linux" && isArm64 -> "$outputDir/native/releaseExecutable/kastrax-native.kexe"
                hostOs == "Linux" && !isArm64 -> "$outputDir/native/releaseExecutable/kastrax-native.kexe"
                isMingwX64 -> "$outputDir/native/releaseExecutable/kastrax-native.exe"
                else -> {
                    println("\u4e0d支持的操作系统: $hostOs")
                    return@doLast
                }
            }

            println("Native executable path: $executablePath")

            val execFile = File(executablePath)
            if (execFile.exists()) {
                execFile.setExecutable(true)
                println("\n请手动运行可执行文件: $executablePath")
            } else {
                println("Executable file not found: $executablePath")
            }
        }
    } else {
        doLast {
            println("\u8b66告: Native目标不可用，无法运行Native可执行文件")
        }
    }
}
