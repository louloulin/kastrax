// Set the artifact name
// extensions.configure<ArtifactExtension> {
//     name = "Proto.Actor Stream"
// }

dependencies {
    add("api", project(":kactor:proto-actor"))
    add("api", project(":kactor:proto-mailbox"))
    add("implementation", "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    add("testImplementation", "org.slf4j:slf4j-simple:2.0.9")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
