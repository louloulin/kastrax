plugins {
    kotlin("jvm") version "1.9.0"
    application
}

group = "ai.kastrax.app"
version = "0.1.0"

repositories {
    mavenCentral()
    // 如果需要，添加本地仓库
    // mavenLocal()
}

dependencies {
    // KastraX 核心依赖
    implementation("ai.kastrax:kastrax-core:0.1.0")
    implementation("ai.kastrax:kastrax-memory-api:0.1.0")
    implementation("ai.kastrax:kastrax-memory-impl:0.1.0")
    implementation("ai.kastrax:kastrax-rag:0.1.0")
    implementation("ai.kastrax:kastrax-zod:0.1.0")
    implementation("ai.kastrax:kastrax-server:0.1.0")
    
    // KastraX 集成
    implementation("ai.kastrax.integrations:kastrax-openai:0.1.0")
    implementation("ai.kastrax.integrations:kastrax-anthropic:0.1.0")
    
    // 日志
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    
    // 工具库
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // 测试
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.13.8")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("ai.kastrax.app.MainKt")
}
