plugins {
    id("io.micronaut.application") version "4.2.1"
    id("io.micronaut.aot") version "4.2.1"
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.allopen") version "1.9.21"
    kotlin("kapt") version "1.9.21"
}

version = "0.1"
group = "com.kastrax.ai2db"

repositories {
    mavenCentral()
}

dependencies {
    // Micronaut 核心
    implementation("io.micronaut:micronaut-http-server-netty")
    implementation("io.micronaut:micronaut-jackson-databind")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    implementation("io.micronaut.kotlin:micronaut-kotlin-extension-functions")
    
    // 数据访问 - 使用JDBC（性能最佳）
    implementation("io.micronaut.data:micronaut-data-jdbc")
    implementation("io.micronaut.sql:micronaut-jdbc-hikari")
    implementation("io.micronaut.flyway:micronaut-flyway")
    
    // 数据库驱动
    runtimeOnly("mysql:mysql-connector-java:8.0.33")
    runtimeOnly("org.postgresql:postgresql:42.6.0")
    runtimeOnly("com.microsoft.sqlserver:mssql-jdbc:12.2.0.jre11")
    
    // 安全
    implementation("io.micronaut.security:micronaut-security-jwt")
    
    // 插件系统
    implementation("io.micronaut:micronaut-inject")
    
    // 协程支持
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive")
    
    // 验证
    implementation("io.micronaut.validation:micronaut-validation")
    
    // JSON处理
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    
    // 日志
    implementation("ch.qos.logback:logback-classic")
    
    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")
    
    // 测试
    testImplementation("io.micronaut.test:micronaut-test-kotlintest")
    testImplementation("io.mockk:mockk")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:mysql")
    
    // 编译时处理
    kapt("io.micronaut:micronaut-http-validation")
    kapt("io.micronaut.data:micronaut-data-processor")
    kapt("io.micronaut.security:micronaut-security-annotations")
}

application {
    mainClass.set("com.kastrax.ai2db.ApplicationKt")
}

java {
    sourceCompatibility = JavaVersion.toVersion("17")
}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
    compileTestKotlin {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
}

graalvmNative.toolchainDetection.set(false)

micronaut {
    runtime("netty")
    testRuntime("kotlintest")
    processing {
        incremental(true)
        annotations("com.kastrax.ai2db.*")
    }
    aot {
        optimizeServiceLoading.set(false)
        convertYamlToJava.set(false)
        precomputeOperations.set(true)
        cacheEnvironment.set(true)
        optimizeClassLoading.set(true)
        deduceEnvironment.set(true)
        optimizeNetty.set(true)
    }
}