plugins {
    kotlin("jvm")
}

group = "actor.proto.plugin.examples"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":kactor:proto-actor"))
    implementation(project(":kactor:proto-plugin"))
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    // PF4J
    implementation("org.pf4j:pf4j:3.9.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.0")
}

// 创建插件描述文件
tasks.register<Copy>("createPluginMetadata") {
    from("src/main/resources/plugin.properties")
    into("$buildDir/resources/main")
}

tasks.named("processResources") {
    dependsOn("createPluginMetadata")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}

tasks.withType<JavaCompile> {
    targetCompatibility = JavaVersion.VERSION_17.toString()
    sourceCompatibility = JavaVersion.VERSION_17.toString()
}

// 添加构建 jvm-libp2p 的任务
tasks.register("buildLibp2p") {
    group = "build"
    description = "Builds jvm-libp2p from source and copies JAR files to libs directory"

    doLast {
        exec {
            commandLine("./build-libp2p.sh")
        }
    }
}

// 让 proto-cluster-libp2p 模块的构建依赖于 buildLibp2p 任务
subprojects {
    if (name == "proto-cluster-libp2p") {
        tasks.named("compileKotlin") {
            dependsOn(rootProject.tasks.named("buildLibp2p"))
        }
    }
}
