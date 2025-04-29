plugins {
    kotlin("jvm")
    id("org.graalvm.buildtools.native")
    application
}

group = "com.dataflare"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.yaml:snakeyaml:2.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.16.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    // Logging - using minimal logging to avoid JMX issues
    implementation("org.slf4j:slf4j-simple:2.0.12")
}

// 配置资源文件
tasks.withType<ProcessResources> {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

// 设置应用程序主类
application {
    mainClass.set("com.dataflare.native.NativeApp")
}

// GraalVM Native Image 配置
graalvmNative {
    binaries {
        named("main") {
            imageName.set("dataflare")
            mainClass.set("com.dataflare.native.NativeApp")
            debug.set(true) // 开发阶段启用调试信息
            buildArgs.add("--verbose")
            buildArgs.add("--no-fallback")
            buildArgs.add("-H:+ReportExceptionStackTraces")
            buildArgs.add("-H:IncludeResources=workflows/.*\\.(yaml|dsl)|input/.*\\.csv")
            buildArgs.add("-H:+PrintClassInitialization")
            buildArgs.add("-H:+PrintAnalysisCallTree")
            buildArgs.add("-H:Log=registerResource")
            buildArgs.add("-H:+IncludeAllTimeZones")
            buildArgs.add("-H:ResourceConfigurationFiles=${project.projectDir}/src/main/resources/META-INF/native-image/resource-config.json")
            buildArgs.add("-H:ReflectionConfigurationFiles=${project.projectDir}/src/main/resources/META-INF/native-image/reflect-config.json")
            buildArgs.add("-H:JNIConfigurationFiles=${project.projectDir}/src/main/resources/META-INF/native-image/jni-config.json")
            buildArgs.add("-H:DynamicProxyConfigurationFiles=${project.projectDir}/src/main/resources/META-INF/native-image/proxy-config.json")
            buildArgs.add("-H:+JNI")
            buildArgs.add("-H:+ReportUnsupportedElementsAtRuntime")
            buildArgs.add("-H:+AllowIncompleteClasspath")
            buildArgs.add("--initialize-at-build-time=org.slf4j,ch.qos.logback,com.fasterxml.jackson,org.yaml")
            buildArgs.add("--initialize-at-run-time=io.netty,com.sun.jmx,com.sun.management,kotlinx.coroutines")
            buildArgs.add("--allow-incomplete-classpath")

            // 特别处理 Kotlin 协程
            buildArgs.add("-H:+AddAllCharsets")
            buildArgs.add("-H:TraceClassInitialization=true")
        }
    }
}
