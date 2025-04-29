plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    // kastrax 依赖
    implementation(project(":kastrax-core"))

    // kactor 依赖
    implementation(project(":kactor:proto-actor"))
    implementation(project(":kactor:proto-mailbox"))
    implementation(project(":kactor:proto-remote"))
    implementation(project(":kactor:proto-cluster"))
    // Kotlin 协程
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // 序列化
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // 日志
    implementation("io.github.microutils:kotlin-logging:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.7")

    // 测试
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
}

tasks.test {
    useJUnitPlatform()
}
