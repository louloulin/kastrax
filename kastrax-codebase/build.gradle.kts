plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jetbrains.dokka")
}

dependencies {
    // KastraX 核心依赖
    implementation(project(":kastrax-core"))
    implementation(project(":kastrax-memory-api"))
    implementation(project(":kastrax-store"))
    // Removed kastrax-rag dependency to avoid circular dependency
    implementation(project(":kastrax-datasource"))
    implementation(project(":kastrax-integrations:kastrax-openai"))
    implementation(project(":kastrax-integrations:kastrax-deepseek"))

    // 文件系统监控依赖
    implementation("io.methvin:directory-watcher:0.17.1")
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.5.0.202303070854-r")

    // 代码解析依赖
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.9.20")
    implementation("com.github.javaparser:javaparser-core:3.25.5")

    // Chapi 代码分析依赖
    implementation("com.phodal.chapi:chapi-domain:2.1.1")
    implementation("com.phodal.chapi:chapi-ast-java:2.1.1")
    implementation("com.phodal.chapi:chapi-ast-kotlin:2.1.1")
    implementation("com.phodal.chapi:chapi-ast-python:2.1.1")
    implementation("com.phodal.chapi:chapi-ast-typescript:2.1.1")
    implementation("com.phodal.chapi:chapi-ast-go:2.1.1")

    // 向量存储依赖
    implementation(project(":kastrax-store:lancedb"))
    implementation(project(":kastrax-store:memory"))
    implementation(project(":kastrax-store:chroma"))

    // 嵌入模型依赖
    implementation(project(":fastembed-kotlin"))

    // 工具依赖
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("io.ktor:ktor-client-core:3.1.2")
    implementation("io.ktor:ktor-client-cio:3.1.2")
    implementation("io.ktor:ktor-client-content-negotiation:3.1.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.1.2")

    // 测试依赖
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
}
