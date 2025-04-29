plugins {
    kotlin("jvm")
    id("me.champeau.jmh") version "0.7.1"
    id("org.graalvm.buildtools.native")
    application
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(project(":kactor:proto-actor"))
    implementation(project(":kactor:proto-remote"))
    implementation(project(":kactor:proto-cluster"))
    implementation(project(":kactor:proto-persistence"))

    implementation("io.github.microutils:kotlin-logging:3.0.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    // JMH dependencies
    implementation("org.openjdk.jmh:jmh-core:1.37")
    implementation("org.openjdk.jmh:jmh-generator-annprocess:1.37")
    annotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")
    jmh("org.openjdk.jmh:jmh-core:1.37")
    jmh("org.openjdk.jmh:jmh-generator-annprocess:1.37")

    // Jackson for JSON processing
    implementation("com.fasterxml.jackson.core:jackson-core:2.16.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")

    // Metrics
    implementation("io.dropwizard.metrics:metrics-core:4.2.25")
    implementation("io.dropwizard.metrics:metrics-jmx:4.2.25")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}

jmh {
    warmupIterations.set(3)
    iterations.set(5)
    fork.set(2)
    benchmarkMode.set(listOf("thrpt"))
    timeUnit.set("ms")
    jmhVersion.set("1.37")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("actor.proto.benchmarks.simple.SimpleBenchmarkKt")
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("protoactor-benchmark")
            mainClass.set("actor.proto.benchmarks.simple.SimpleBenchmarkKt")
            buildArgs.add("--no-fallback")
            buildArgs.add("--report-unsupported-elements-at-runtime")
            buildArgs.add("-H:+ReportExceptionStackTraces")
            buildArgs.add("-H:+PrintClassInitialization")
            buildArgs.add("--initialize-at-build-time=org.slf4j,ch.qos.logback")
            buildArgs.add("--initialize-at-run-time=io.grpc,io.netty")
        }
    }
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

// Task to copy all dependencies to a directory for native image building
tasks.register<Copy>("copyDependencies") {
    from(configurations.runtimeClasspath)
    into("${buildDir}/dependencies")
}

tasks.named("build") {
    dependsOn("copyDependencies")
}

// 创建目录用于存放反射配置
tasks.register("createMetadataDirectory") {
    doLast {
        mkdir("src/main/resources/META-INF/native-image/actor.proto/proto-benchmarks")
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
            |=== ProtoActor Benchmarks Native Image 编译帮助 ===
            |
            |1. 使用 Agent 生成配置文件:
            |   ./gradlew -Pagent=standard :proto-benchmarks:run
            |
            |2. 复制生成的配置文件:
            |   ./gradlew :proto-benchmarks:metadataCopy
            |
            |3. 编译 Native Image:
            |   ./gradlew :proto-benchmarks:nativeCompile
            |
            |4. 运行 Native Image:
            |   ./proto-benchmarks/build/native/nativeCompile/protoactor-benchmark
            |
            |注意: 确保已安装 GraalVM 并设置了 JAVA_HOME 环境变量指向 GraalVM 安装目录。
        """.trimMargin())
    }
}
