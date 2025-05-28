plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    // 项目依赖
    implementation("ai.kastrax:kastrax-core")
    
    // Ktor 依赖
    implementation("io.ktor:ktor-client-core:3.1.2")
    implementation("io.ktor:ktor-client-cio:3.1.2")
    implementation("io.ktor:ktor-client-content-negotiation:3.1.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.1.2")
    
    // Kotlin 序列化依赖
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    
    // 日志依赖
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    
    // 其他特定依赖
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
}
