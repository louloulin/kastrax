rootProject.name = "kastrax"

include(":kastrax-core")
include(":kastrax-zod")
include(":kastrax-memory-api")
include(":kastrax-memory-impl")
include(":kastrax-rag")
include(":kastrax-evals")
include(":kastrax-cli")
include(":kastrax-deployer")
include(":kastrax-integrations:kastrax-openai")
include(":kastrax-integrations:kastrax-deepseek")
// 已修复 AdvancedWorkflowExample.kt 文件的编译错误
// 其他示例文件还有错误，但我们只需要编译这个文件
include(":examples")
include(":zod-test")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
