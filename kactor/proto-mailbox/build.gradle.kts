// Set the artifact name
// extensions.configure<ArtifactExtension> {
//    name = "Proto.Actor Mailbox"
// }

dependencies {
    add("api", "org.jctools:jctools-core:4.0.1")
    add("implementation", "org.slf4j:slf4j-api:2.0.9")
    add("implementation", "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    add("implementation", project(":kastrax-runtime:kastrax-runtime-api"))
    add("testImplementation", project(":kastrax-runtime:kastrax-runtime-jvm"))
    add("testImplementation", "org.slf4j:slf4j-simple:2.0.9")
    add("testImplementation", "org.awaitility:awaitility:4.2.0")
    add("testImplementation", "org.mockito:mockito-core:4.11.0")
    add("testImplementation", "org.mockito.kotlin:mockito-kotlin:4.1.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
