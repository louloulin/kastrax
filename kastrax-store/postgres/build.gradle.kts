plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("java-library")
    id("maven-publish")
}

group = "ai.kastrax.store"
version = rootProject.version

repositories {
    mavenCentral()
}

dependencies {
    // 核心依赖
    implementation(project(":kastrax-core"))
    implementation(project(":kastrax-rag"))
    implementation(project(":kastrax-store"))
    
    // Kotlin 协程
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // 日志
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    
    // 序列化
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    
    // PostgreSQL 依赖
    implementation("org.postgresql:postgresql:42.6.0")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.jetbrains.exposed:exposed-core:0.45.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.45.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.45.0")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:0.45.0")
    
    // 测试
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testImplementation("org.mockito:mockito-core:5.5.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("org.testcontainers:postgresql:1.19.1")
    testImplementation("org.testcontainers:junit-jupiter:1.19.1")
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
