rootProject.name = "kastrax"

include(":kastrax-core")
include(":kastrax-zod")
include(":kastrax-memory-api")
include(":kastrax-memory-impl")
include(":kastrax-rag")
include(":kastrax-evals")
include(":kastrax-cli")
include(":kastrax-integrations:kastrax-openai")
include(":kastrax-integrations:kastrax-deepseek")
include(":examples")
include(":zod-test")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
