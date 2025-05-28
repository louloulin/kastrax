plugins {
    kotlin("jvm")
    application
}

dependencies {
    // 项目依赖
    implementation("ai.kastrax:kastrax-core")
    implementation("ai.kastrax:kastrax-memory-api")
    implementation("ai.kastrax:kastrax-memory-impl")
    implementation("ai.kastrax:kastrax-deepseek")
    implementation("ai.kastrax:kastrax-openai")

    // Kotlin 依赖
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")

    // 日志
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.11")
}

application {
    mainClass.set("ai.kastrax.examples.memory.HelloMemoryKt")
}

// 为每个示例创建单独的运行任务
tasks.register<JavaExec>("runWorkingMemoryExample") {
    group = "examples"
    description = "Run the WorkingMemoryExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.memory.WorkingMemoryExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
}

tasks.register<JavaExec>("runMemoryCompressionExample") {
    group = "examples"
    description = "Run the MemoryCompressionExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.memory.MemoryCompressionExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
}

tasks.register<JavaExec>("runMemoryManagerExample") {
    group = "examples"
    description = "Run the MemoryManagerExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.memory.MemoryManagerExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
}

tasks.register<JavaExec>("runTagsAndSharingExample") {
    group = "examples"
    description = "Run the TagsAndSharingExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.memory.TagsAndSharingExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
}

tasks.register<JavaExec>("runHelloMemory") {
    group = "examples"
    description = "Run the HelloMemory example"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.memory.HelloMemoryKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
}
