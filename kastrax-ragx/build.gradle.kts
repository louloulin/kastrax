
group = "ai.kastrax"
version = "0.1.0"

dependencies {
    // 核心依赖
    implementation(project(":kastrax-core"))
    implementation(project(":kastrax-store"))
    
    // 外部依赖
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // 测试依赖
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.mockito:mockito-core:5.5.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
}
