import org.graalvm.buildtools.gradle.tasks.BuildNativeImageTask

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.graalvm.buildtools.native") version "0.9.28"
}

dependencies {
    implementation(project(":kastrax-core"))
    implementation(project(":kastrax-memory-api"))
    implementation(project(":kastrax-memory-impl"))
    implementation(project(":kastrax-rag"))
    implementation(project(":kastrax-integrations:kastrax-deepseek"))
    
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    
    testImplementation(kotlin("test"))
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("kastrax")
            mainClass.set("ai.kastrax.graal.MainKt")
            buildArgs.add("--no-fallback")
            buildArgs.add("-H:+ReportExceptionStackTraces")
            buildArgs.add("-H:ReflectionConfigurationFiles=${project.projectDir}/src/main/resources/META-INF/native-image/reflection-config.json")
            buildArgs.add("-H:ResourceConfigurationFiles=${project.projectDir}/src/main/resources/META-INF/native-image/resource-config.json")
            
            // Enable JNI support for language SDKs
            buildArgs.add("-H:+JNI")
            
            // Optimize for performance
            buildArgs.add("-O2")
            
            // Include all supported platforms
            targetPlatforms.add("linux-amd64")
            targetPlatforms.add("linux-aarch64")
            targetPlatforms.add("macos-amd64")
            targetPlatforms.add("macos-aarch64")
            targetPlatforms.add("windows-amd64")
        }
    }
}

tasks.withType<BuildNativeImageTask> {
    dependsOn(tasks.named("jar"))
}

// Create a task to package the native image with necessary resources
tasks.register<Zip>("packageNative") {
    dependsOn(tasks.named("nativeCompile"))
    
    archiveFileName.set("kastrax-native-${project.version}-${osName()}.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    
    from(layout.buildDirectory.dir("native/nativeCompile")) {
        include("kastrax*")
    }
    
    from(project.projectDir) {
        include("README.md")
        include("LICENSE")
    }
    
    from(layout.projectDirectory.dir("src/main/resources")) {
        include("config/**")
        into("resources")
    }
}

// Helper function to determine OS name
fun osName(): String {
    val os = System.getProperty("os.name").toLowerCase()
    return when {
        os.contains("win") -> "windows"
        os.contains("mac") -> "macos"
        else -> "linux"
    }
}
