dependencies {
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.8.20")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
