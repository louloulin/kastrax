rootProject.name = "kastrax"

include(":kastrax-core")
include(":kastrax-memory-api")
include(":kastrax-memory-impl")
include(":kastrax-integrations:kastrax-openai")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
