plugins {
    kotlin("jvm")
    application
}

dependencies {
    // 项目依赖
    implementation("ai.kastrax:kastrax-core")
    implementation("ai.kastrax:kastrax-memory-api")
    implementation("ai.kastrax:kastrax-memory-impl")
    implementation("ai.kastrax:kastrax-zod")
    implementation("ai.kastrax:kastrax-deepseek")

    // Kotlin 依赖
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")

    // 日志
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.11")
}

application {
    mainClass.set("ai.kastrax.examples.agent.HelloAgentKt")
}

// 为每个示例创建单独的运行任务
tasks.register<JavaExec>("runZodAgentExample") {
    group = "examples"
    description = "Run the ZodAgentExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.agent.ZodAgentExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
}

tasks.register<JavaExec>("runAdaptiveAgentExample") {
    group = "examples"
    description = "Run the AdaptiveAgentExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.agent.AdaptiveAgentExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
}

tasks.register<JavaExec>("runAdvancedAgentExample") {
    group = "examples"
    description = "Run the AdvancedAgentExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.agent.AdvancedAgentExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
}

tasks.register<JavaExec>("runAgentStateExample") {
    group = "examples"
    description = "Run the AgentStateExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.agent.AgentStateExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
}

tasks.register<JavaExec>("runAgentVersioningExample") {
    group = "examples"
    description = "Run the AgentVersioningExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.agent.AgentVersioningExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
}

tasks.register<JavaExec>("runGoalOrientedAgentExample") {
    group = "examples"
    description = "Run the GoalOrientedAgentExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.agent.GoalOrientedAgentExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
}

tasks.register<JavaExec>("runReflectiveAgentExample") {
    group = "examples"
    description = "Run the ReflectiveAgentExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.agent.ReflectiveAgentExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
}

tasks.register<JavaExec>("runHierarchicalAgentExample") {
    group = "examples"
    description = "Run the HierarchicalAgentExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.agent.HierarchicalAgentExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
}

tasks.register<JavaExec>("runAgentNetworkExample") {
    group = "examples"
    description = "Run the AgentNetworkExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.agent.AgentNetworkExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
}

tasks.register<JavaExec>("runHelloAgent") {
    group = "examples"
    description = "Run the HelloAgent example"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.agent.HelloAgentKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
}
