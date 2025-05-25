plugins {
    kotlin("jvm")
}

repositories {
    // ...
    maven { url = uri("https://dl.cloudsmith.io/public/libp2p/jvm-libp2p/maven/") }
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://artifacts.consensys.net/public/maven/maven/") }
}

dependencies {
    api(project(":kactor:proto-actor"))
    api(project(":kactor:proto-remote"))
    api(project(":kactor:proto-cluster"))


    // 暂时移除 libp2p 依赖，简化实现

    // 其他依赖
    implementation("io.github.microutils:kotlin-logging:2.0.11")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.8.0")

    // kastrax-runtime 依赖
    implementation(project(":kastrax-runtime:kastrax-runtime-api"))
    implementation(project(":kastrax-runtime:kastrax-runtime-jvm"))

    // 测试依赖
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
    testImplementation("org.mockito:mockito-core:4.11.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
    testImplementation("org.mockito:mockito-inline:4.11.0") // For mocking final classes
}

tasks.test {
    useJUnitPlatform()
}
