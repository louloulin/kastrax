plugins {
    kotlin("jvm")
    application
}

application {
    mainClass.set("actor.proto.benchmark.InProcessBenchmarkKt")
}

dependencies {
    // ProtoActor 依赖项
    implementation(project(":kactor:proto-actor"))
    implementation(project(":kactor:proto-mailbox"))
    implementation(project(":kastrax-runtime:kastrax-runtime-api"))
    implementation(project(":kastrax-runtime:kastrax-runtime-jvm"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.slf4j:slf4j-simple:2.0.9")
}

// 创建反射配置目录
tasks.register("createReflectionConfig") {
    group = "Native"
    description = "创建反射配置文件"

    doLast {
        mkdir("src/main/resources/META-INF/native-image")

        // 创建反射配置文件
        val reflectConfig = file("src/main/resources/META-INF/native-image/reflect-config.json")
        if (!reflectConfig.exists()) {
            reflectConfig.writeText("""
                [
                  {
                    "name": "actor.proto.PID",
                    "allDeclaredFields": true,
                    "allDeclaredMethods": true,
                    "allPublicMethods": true
                  },
                  {
                    "name": "actor.proto.java.Actor",
                    "allDeclaredMethods": true,
                    "allPublicMethods": true
                  },
                  {
                    "name": "actor.proto.java.Context",
                    "allDeclaredFields": true,
                    "allDeclaredMethods": true,
                    "allPublicMethods": true
                  },
                  {
                    "name": "actor.proto.benchmark.EchoActor",
                    "allDeclaredFields": true,
                    "allDeclaredMethods": true,
                    "allPublicMethods": true
                  },
                  {
                    "name": "actor.proto.benchmark.PingActor",
                    "allDeclaredFields": true,
                    "allDeclaredMethods": true,
                    "allPublicMethods": true
                  },
                  {
                    "name": "actor.proto.benchmark.Msg",
                    "allDeclaredFields": true,
                    "allDeclaredMethods": true,
                    "allPublicMethods": true
                  },
                  {
                    "name": "actor.proto.benchmark.Start",
                    "allDeclaredFields": true,
                    "allDeclaredMethods": true,
                    "allPublicMethods": true
                  },
                  {
                    "name": "actor.proto.benchmark.InProcessBenchmarkKt",
                    "methods": [
                      { "name": "main", "parameterTypes": [] }
                    ]
                  },
                  {
                    "name": "actor.proto.mailbox.DefaultDispatcher",
                    "allDeclaredFields": true,
                    "allDeclaredMethods": true,
                    "allPublicMethods": true
                  },
                  {
                    "name": "java.util.concurrent.CountDownLatch",
                    "allDeclaredFields": true,
                    "allDeclaredMethods": true,
                    "allPublicMethods": true
                  },
                  {
                    "name": "java.util.concurrent.CompletableFuture",
                    "allDeclaredFields": true,
                    "allDeclaredMethods": true,
                    "allPublicMethods": true
                  }
                ]
            """.trimIndent())
        }

        // 创建资源配置文件
        val resourceConfig = file("src/main/resources/META-INF/native-image/resource-config.json")
        if (!resourceConfig.exists()) {
            resourceConfig.writeText("""
                {
                  "resources": {
                    "includes": [
                      {
                        "pattern": ".*\\.properties"
                      },
                      {
                        "pattern": "META-INF/services/.*"
                      },
                      {
                        "pattern": "META-INF/native/.*"
                      },
                      {
                        "pattern": "org/slf4j/impl/StaticLoggerBinder.class"
                      }
                    ]
                  },
                  "bundles": []
                }
            """.trimIndent())
        }
    }
}

// 添加 Native 编译任务
tasks.register<Exec>("nativeCompile") {
    group = "Native"
    description = "编译 Native Image"

    dependsOn("build", "createReflectionConfig")

    doFirst {
        // 检查 GraalVM 是否安装
        val javaHome = System.getProperty("java.home")
        val nativeImageExecutable = File(javaHome, "bin/native-image")

        if (!nativeImageExecutable.exists()) {
            throw IllegalStateException("无法找到 native-image 工具。请确保安装了 GraalVM 并运行 'gu install native-image'。")
        }

        // 创建目录
        File(layout.buildDirectory.get().asFile, "native").mkdirs()

        // 收集依赖项
        val classpath = mutableListOf<String>()
        classpath.add("${layout.buildDirectory.get().asFile}/classes/kotlin/main")
        classpath.add("${layout.buildDirectory.get().asFile}/resources/main")

        // 添加项目依赖项
        val projectDeps = configurations.runtimeClasspath.get().files
            .filter { it.name.endsWith(".jar") }
            .map { it.absolutePath }
        classpath.addAll(projectDeps)

        // 构建命令
        commandLine = listOf(
            nativeImageExecutable.absolutePath,
            "--no-fallback",
            "--report-unsupported-elements-at-runtime",
            "-H:+ReportExceptionStackTraces",
            "--initialize-at-build-time=org.slf4j,kotlin",
            "-cp", classpath.joinToString(":"),
            "actor.proto.benchmark.InProcessBenchmarkKt",
            "-o", "${layout.buildDirectory.get().asFile}/native/inprocess-benchmark"
        )
    }
}

// 运行 Native Image
tasks.register<Exec>("runNative") {
    group = "Native"
    description = "运行 Native Image"

    dependsOn("nativeCompile")

    doFirst {
        val outputFile = File(layout.buildDirectory.get().asFile, "native/inprocess-benchmark")

        if (!outputFile.exists()) {
            throw IllegalStateException("Native Image 不存在。请先运行 nativeCompile 任务。")
        }

        // 确保文件可执行
        outputFile.setExecutable(true)

        commandLine = listOf(outputFile.absolutePath)
    }
}

// 一键构建和运行 Native Image
tasks.register("buildAndRunNative") {
    group = "Native"
    description = "构建并运行 Native Image"

    dependsOn("runNative")
}
