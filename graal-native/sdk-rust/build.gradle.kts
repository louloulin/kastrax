plugins {
    kotlin("jvm")
    id("org.gradle.java-library")
}

dependencies {
    implementation(project(":graal-native"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    
    testImplementation(kotlin("test"))
}

// Task to build Rust library
tasks.register<Exec>("buildRustLib") {
    workingDir = file("${projectDir}/rust")
    commandLine = listOf("cargo", "build", "--release")
}

// Task to copy Rust library to resources
tasks.register<Copy>("copyRustLib") {
    dependsOn("buildRustLib")
    
    from("${projectDir}/rust/target/release") {
        include("*.dll")
        include("*.so")
        include("*.dylib")
    }
    
    into("${projectDir}/src/main/resources")
}

// Add dependency to build task
tasks.named("build") {
    dependsOn("copyRustLib")
}
