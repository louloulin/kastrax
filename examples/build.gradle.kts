plugins {
    kotlin("jvm")
    application
}

dependencies {
    // Project dependencies
    implementation(project(":kastrax-core"))
    implementation(project(":kastrax-memory-api"))
    implementation(project(":kastrax-memory-impl"))
    implementation(project(":kastrax-integrations:kastrax-openai"))
    implementation(project(":kastrax-integrations:kastrax-deepseek"))
    
    // Kotlin
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    
    // Logging
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.11")
}

kotlin {
    jvmToolchain(17)
}

// 定义示例应用
val examples = listOf(
    "CalculatorExample",
    "MemoryAgentExample",
    "MemorySystemExample",
    "DeepSeekExample"
)

// 为每个示例创建运行任务
examples.forEach { example ->
    tasks.register<JavaExec>("run$example") {
        group = "examples"
        description = "Run the $example example"
        
        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("ai.kastrax.examples.${example}Kt")
        
        // 添加 JVM 参数
        jvmArgs = listOf("-Xms512m", "-Xmx1g")
        
        // 确保示例可以访问环境变量
        environment("OPENAI_API_KEY", System.getenv("OPENAI_API_KEY") ?: "")
        environment("DEEPSEEK_API_KEY", System.getenv("DEEPSEEK_API_KEY") ?: "")
    }
}

// 创建一个任务来列出所有可用的示例
tasks.register("listExamples") {
    group = "examples"
    description = "List all available examples"
    
    doLast {
        println("Available examples:")
        examples.forEach { example ->
            println("  ./gradlew run$example - Run the $example example")
        }
    }
}

// 默认任务
defaultTasks("listExamples")
