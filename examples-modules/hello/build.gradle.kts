plugins {
    kotlin("jvm")
    application
}

dependencies {
    // 只包含基本依赖
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.slf4j:slf4j-simple:2.0.9")
}

application {
    mainClass.set("ai.kastrax.examples.hello.HelloWorldKt")
}

tasks.register<JavaExec>("runHelloWorld") {
    group = "examples"
    description = "Run the HelloWorld example"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.hello.HelloWorldKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
}
