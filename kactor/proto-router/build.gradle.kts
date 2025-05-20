// Set the artifact name
// extensions.configure<ArtifactExtension> {
//    name = "Proto.Actor Router"
// }

dependencies {
    add("api", project(":kactor:proto-actor"))
    add("api", project(":kactor:proto-mailbox"))
    add("api", "com.google.protobuf:protobuf-java:3.25.1")
    add("implementation", "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // 添加 kastrax-runtime 依赖
    add("implementation", project(":kastrax-runtime:kastrax-runtime-api"))
    add("implementation", project(":kastrax-runtime:kastrax-runtime-jvm"))

    add("testImplementation", "org.slf4j:slf4j-simple:2.0.9")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
