rootProject.name = "kastrax"

include(":kastrax-core")
include(":kastrax-memory")
include(":kastrax-integrations:kastrax-openai")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
