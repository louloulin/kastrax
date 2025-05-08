plugins {
    kotlin("jvm")
    application
}

dependencies {
    // 工具特定依赖
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}

application {
    mainClass.set("ai.kastrax.examples.tools.HelloToolsKt")
}

// 为每个示例创建单独的运行任务
tasks.register<JavaExec>("runAdvancedZodToolExample") {
    group = "examples"
    description = "Run the AdvancedZodToolExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.tools.AdvancedZodToolExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
}

tasks.register<JavaExec>("runDataClassZodToolExample") {
    group = "examples"
    description = "Run the DataClassZodToolExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.tools.DataClassZodToolExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
}

tasks.register<JavaExec>("runDateTimeToolExample") {
    group = "examples"
    description = "Run the DateTimeToolExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.tools.DateTimeToolExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
}

tasks.register<JavaExec>("runZodAdvancedToolExample") {
    group = "examples"
    description = "Run the ZodAdvancedToolExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.tools.ZodAdvancedToolExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
}

tasks.register<JavaExec>("runZodCalculatorExample") {
    group = "examples"
    description = "Run the ZodCalculatorExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.tools.ZodCalculatorExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
}

tasks.register<JavaExec>("runZodCalculatorToolExample") {
    group = "examples"
    description = "Run the ZodCalculatorToolExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.tools.ZodCalculatorToolExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
}

tasks.register<JavaExec>("runHelloTools") {
    group = "examples"
    description = "Run the HelloTools example"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.tools.HelloToolsKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
}
