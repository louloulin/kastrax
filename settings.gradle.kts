rootProject.name = "kastrax"

include(":kastrax-core")
include(":kastrax-zod")
include(":kastrax-memory-api")
include(":kastrax-memory-impl")
include(":kastrax-rag")
include(":kastrax-evals")
include(":kastrax-cli")
include(":kastrax-deployer")
include(":kastrax-observability")
include(":kastrax-integrations:kastrax-openai")
include(":kastrax-integrations:kastrax-deepseek")
include(":kastrax-integrations:kastrax-anthropic")
include(":kastrax-integrations:kastrax-gemini")

// Data Source modules
include(":kastrax-datasource-common")
include(":kastrax-datasource")
include(":kastrax-datasource:kastrax-database")
include(":kastrax-datasource:kastrax-api")
include(":kastrax-datasource:kastrax-filesystem")
include(":kastrax-datasource:kastrax-nosql")
include(":kastrax-datasource:kastrax-cloud-storage")

include(":fastembed-kotlin")
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
