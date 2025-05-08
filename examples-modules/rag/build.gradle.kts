plugins {
    kotlin("jvm")
    application
}

dependencies {
    // RAG特定依赖
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("io.ktor:ktor-client-core:3.1.2")
    implementation("io.ktor:ktor-client-okhttp:3.1.2")
}

application {
    mainClass.set("ai.kastrax.examples.rag.HelloRagKt")
}

// 为每个示例创建单独的运行任务
tasks.register<JavaExec>("runRAGExample") {
    group = "examples"
    description = "Run the RAGExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.rag.RAGExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
}

tasks.register<JavaExec>("runRAGWorkflowExample") {
    group = "examples"
    description = "Run the RAGWorkflowExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.rag.RAGWorkflowExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
}

tasks.register<JavaExec>("runFastEmbedRAGExample") {
    group = "examples"
    description = "Run the FastEmbedRAGExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.rag.FastEmbedRAGExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
}

tasks.register<JavaExec>("runHelloRag") {
    group = "examples"
    description = "Run the HelloRag example"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.rag.HelloRagKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
}
