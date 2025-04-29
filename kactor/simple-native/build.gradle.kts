plugins {
    kotlin("jvm")
    application
}

application {
    mainClass.set("actor.proto.simple.ProtoActorExampleKt")
}

dependencies {
    // ProtoActor 依赖项
    implementation(project(":kactor:proto-actor"))
    implementation(project(":kactor:proto-mailbox"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.slf4j:slf4j-simple:2.0.9")
}

// 添加 Native 编译任务
tasks.register<Exec>("nativeCompile") {
    group = "Native"
    description = "编译 Native Image"

    dependsOn("build")

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
            "actor.proto.simple.ProtoActorExampleKt",
            "-o", "${layout.buildDirectory.get().asFile}/native/proto-actor-example"
        )
    }
}

// 运行 Native Image
tasks.register<Exec>("runNative") {
    group = "Native"
    description = "运行 Native Image"

    dependsOn("nativeCompile")

    doFirst {
        val outputFile = File(layout.buildDirectory.get().asFile, "native/proto-actor-example")

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
