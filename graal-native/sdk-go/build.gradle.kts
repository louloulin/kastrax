plugins {
    kotlin("jvm")
    id("org.gradle.java-library")
}

dependencies {
    implementation(project(":graal-native"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    
    testImplementation(kotlin("test"))
}

// Task to build Go library
tasks.register<Exec>("buildGoLib") {
    workingDir = file("${projectDir}/go")
    commandLine = listOf("go", "build", "-buildmode=c-shared", "-o", "libkastrax_go.so", ".")
}

// Task to copy Go library to resources
tasks.register<Copy>("copyGoLib") {
    dependsOn("buildGoLib")
    
    from("${projectDir}/go") {
        include("*.dll")
        include("*.so")
        include("*.dylib")
    }
    
    into("${projectDir}/src/main/resources")
}

// Add dependency to build task
tasks.named("build") {
    dependsOn("copyGoLib")
}
