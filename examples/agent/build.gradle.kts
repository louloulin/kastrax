plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
    application
}

dependencies {
    // 项目依赖
    implementation(project(":kastrax-core"))
    implementation(project(":kastrax-integrations:kastrax-deepseek"))
    implementation(project(":kastrax-zod"))

    // Kotlin 序列化依赖
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // Kotlin 日期时间依赖
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")

    // 日志依赖
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.11")

    // 其他特定依赖
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
}

application {
    mainClass.set("ai.kastrax.examples.agent.SimpleStreamingExampleKt")
}