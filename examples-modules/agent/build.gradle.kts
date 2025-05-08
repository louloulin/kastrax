plugins {
    kotlin("jvm")
    application
}

dependencies {
    // Agent特定依赖
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}

application {
    mainClass.set("ai.kastrax.examples.agent.AgentExamplesKt")
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
