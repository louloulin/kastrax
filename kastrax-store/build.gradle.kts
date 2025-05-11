plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("java-library")
    id("maven-publish")
}

group = "ai.kastrax"
version = rootProject.version

repositories {
    mavenCentral()
}

dependencies {
    // 核心依赖
    implementation(project(":kastrax-core"))

    // Kotlin 协程
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // 日志
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")

    // 序列化
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // 测试
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testImplementation("org.mockito:mockito-core:5.5.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation(project(":kastrax-store:memory"))
}

tasks.test {
    useJUnitPlatform()
}

java {
    withSourcesJar()
    withJavadocJar()
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
