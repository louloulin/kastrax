plugins {
    kotlin("jvm")
    application
    id("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
    // 工作流特定依赖
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}

application {
    mainClass.set("ai.kastrax.examples.workflow.HelloWorkflowKt")
}

// 为每个示例创建单独的运行任务
tasks.register<JavaExec>("runWorkflowExample") {
    group = "examples"
    description = "Run the WorkflowExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.workflow.WorkflowExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
}

tasks.register<JavaExec>("runDynamicWorkflowExample") {
    group = "examples"
    description = "Run the DynamicWorkflowExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.workflow.DynamicWorkflowExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
}

tasks.register<JavaExec>("runAdvancedWorkflowExample") {
    group = "examples"
    description = "Run the AdvancedWorkflowExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.workflow.AdvancedWorkflowExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
}

tasks.register<JavaExec>("runWorkflowRetryExample") {
    group = "examples"
    description = "Run the WorkflowRetryExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.workflow.WorkflowRetryExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
}

tasks.register<JavaExec>("runHelloWorkflow") {
    group = "examples"
    description = "Run the HelloWorkflow example"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.workflow.HelloWorkflowKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
}
