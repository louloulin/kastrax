plugins {
    kotlin("jvm")
    id("io.gitlab.arturbosch.detekt")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Project dependencies
    implementation(project(":kastrax-core"))

    // Kotlin
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    implementation("io.github.microutils:kotlin-logging:3.0.5")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
    testImplementation("io.mockk:mockk:1.13.5")
}

tasks.test {
    useJUnitPlatform()
}
