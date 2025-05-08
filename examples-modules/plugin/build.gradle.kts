plugins {
    kotlin("jvm")
    application
}

dependencies {
    // 插件特定依赖
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("io.ktor:ktor-client-core:3.1.2")
    implementation("io.ktor:ktor-client-okhttp:3.1.2")
}

application {
    mainClass.set("ai.kastrax.examples.plugin.HelloPluginKt")
}

// 为每个示例创建单独的运行任务
tasks.register<JavaExec>("runHttpConnectorPluginExample") {
    group = "examples"
    description = "Run the HttpConnectorPluginExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.plugin.PluginExamplesKt")
    args = listOf("http-connector")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
}

tasks.register<JavaExec>("runHttpStepPluginExample") {
    group = "examples"
    description = "Run the HttpStepPluginExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.plugin.PluginExamplesKt")
    args = listOf("http-step")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
}

tasks.register<JavaExec>("runHelloPlugin") {
    group = "examples"
    description = "Run the HelloPlugin example"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.plugin.HelloPluginKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
}
