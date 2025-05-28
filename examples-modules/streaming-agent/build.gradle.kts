plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    // 项目依赖
    implementation(project(":kastrax-core"))
    implementation(project(":kastrax-integrations:kastrax-deepseek"))
    implementation(project(":kastrax-zod"))
    
    // Kotlin 序列化依赖
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    
    // 协程依赖
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // 日期时间依赖
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")
    
    // 日志依赖
    implementation("ch.qos.logback:logback-classic:1.4.11")
}