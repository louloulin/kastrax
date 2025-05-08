plugins {
    kotlin("jvm")
    application
}

dependencies {
    // 其他特定依赖
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("io.ktor:ktor-client-core:3.1.2")
    implementation("io.ktor:ktor-client-okhttp:3.1.2")
}

application {
    mainClass.set("ai.kastrax.examples.other.HelloOtherKt")
}

// 为每个示例创建单独的运行任务
tasks.register<JavaExec>("runDataSourceExample") {
    group = "examples"
    description = "Run the DataSourceExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.other.DataSourceExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
}

tasks.register<JavaExec>("runAnthropicDirectStreamingExample") {
    group = "examples"
    description = "Run the AnthropicDirectStreamingExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.other.AnthropicDirectStreamingExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    environment("ANTHROPIC_API_KEY", "your-anthropic-api-key")
}

tasks.register<JavaExec>("runAnthropicStreamingExample") {
    group = "examples"
    description = "Run the AnthropicStreamingExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.other.AnthropicStreamingExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    environment("ANTHROPIC_API_KEY", "your-anthropic-api-key")
}

tasks.register<JavaExec>("runGeminiDirectStreamingExample") {
    group = "examples"
    description = "Run the GeminiDirectStreamingExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.other.GeminiDirectStreamingExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    environment("GEMINI_API_KEY", "your-gemini-api-key")
}

tasks.register<JavaExec>("runGeminiStreamingExample") {
    group = "examples"
    description = "Run the GeminiStreamingExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.other.GeminiStreamingExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    environment("GEMINI_API_KEY", "your-gemini-api-key")
}

tasks.register<JavaExec>("runHelloOther") {
    group = "examples"
    description = "Run the HelloOther example"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.other.HelloOtherKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
}
