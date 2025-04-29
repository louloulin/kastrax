plugins {
    kotlin("jvm")
    application
    id("org.graalvm.buildtools.native")
}

application {
    mainClass.set("actor.proto.native.HelloNativeKt")
}

// 创建多个可执行程序的配置
tasks.register<JavaExec>("runComplex") {
    group = "application"
    description = "运行复杂示例"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("actor.proto.native.ComplexExampleKt")
}

dependencies {
    implementation(project(":kactor:proto-actor"))
    implementation(project(":kactor:proto-mailbox"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.slf4j:slf4j-simple:2.0.9")
}

// 配置 GraalVM Native Image
graalvmNative {
    binaries {
        named("main") {
            // 设置生成的可执行文件名称
            imageName.set("proto-actor-native")

            // 设置主类
            mainClass.set("actor.proto.native.HelloNativeKt")

            // 构建参数
            buildArgs.add("--no-fallback")
            buildArgs.add("--report-unsupported-elements-at-runtime")
            buildArgs.add("-H:+ReportExceptionStackTraces")

            // 初始化设置
            buildArgs.add("--initialize-at-build-time=org.slf4j")

            // 调试选项（可选）
            // debug.set(true)
            // verbose.set(true)
        }

        // 添加复杂示例的 native image 配置
        create("complex") {
            // 设置生成的可执行文件名称
            imageName.set("proto-actor-complex")

            // 设置主类
            mainClass.set("actor.proto.native.ComplexExampleKt")

            // 构建参数
            buildArgs.add("--no-fallback")
            buildArgs.add("--report-unsupported-elements-at-runtime")
            buildArgs.add("-H:+ReportExceptionStackTraces")

            // 初始化设置
            buildArgs.add("--initialize-at-build-time=org.slf4j")
            buildArgs.add("--initialize-at-build-time=kotlin")

            // 调试选项
            debug.set(true)
        }
    }

    // 启用元数据仓库支持
    metadataRepository {
        enabled.set(true)
    }

    // 配置 Agent 支持，用于自动生成反射配置
    agent {
        enabled.set(true)
        defaultMode.set("standard")

        metadataCopy {
            inputTaskNames.add("run")
            outputDirectories.add("src/main/resources/META-INF/native-image/")
            mergeWithExisting.set(true)
        }
    }
}

// 创建目录用于存放反射配置
tasks.register("createMetadataDirectory") {
    doLast {
        mkdir("src/main/resources/META-INF/native-image/actor.proto/native-example")
    }
}

// 确保在运行前创建元数据目录
tasks.named("run") {
    dependsOn("createMetadataDirectory")
}

// 添加任务说明
tasks.register("nativeHelp") {
    group = "Native Image"
    description = "显示 Native Image 编译帮助信息"

    doLast {
        println("""
            |=== ProtoActor Native Image 编译帮助 ===
            |
            |1. 使用 Agent 生成配置文件:
            |   ./gradlew -Pagent=standard :native-example:run
            |   ./gradlew -Pagent=standard :native-example:runComplex
            |
            |2. 复制生成的配置文件:
            |   ./gradlew :native-example:metadataCopy
            |
            |3. 编译 Native Image:
            |   ./gradlew :native-example:nativeCompile                  # 简单示例
            |   ./gradlew :native-example:nativeCompile -PimageName=complex  # 复杂示例
            |
            |4. 运行 Native Image:
            |   ./native-example/build/native/nativeCompile/proto-actor-native    # 简单示例
            |   ./native-example/build/native/nativeCompile/proto-actor-complex   # 复杂示例
            |
            |注意: 确保已安装 GraalVM 并设置了 JAVA_HOME 环境变量指向 GraalVM 安装目录。
        """.trimMargin())
    }
}

// 添加任务用于编译复杂示例的 Native Image
tasks.register("nativeCompileComplex") {
    group = "Native Image"
    description = "编译复杂示例的 Native Image"

    doLast {
        exec {
            commandLine("./gradlew", ":native-example:nativeCompile", "-PimageName=complex")
        }
    }
}
