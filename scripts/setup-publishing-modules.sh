#!/bin/bash

# 为核心模块批量设置发布配置的脚本

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 定义需要发布的核心模块
CORE_MODULES=(
    "kastrax-memory-impl:Kastrax Memory Implementation:Memory management implementation for the Kastrax AI Framework"
    "kastrax-rag:Kastrax RAG:Retrieval-Augmented Generation components for Kastrax"
    "kastrax-zod:Kastrax Zod:Schema validation and type safety for Kastrax"
    "kastrax-evals:Kastrax Evals:Evaluation framework for AI models in Kastrax"
    "kastrax-cli:Kastrax CLI:Command-line interface for Kastrax"
    "kastrax-deployer:Kastrax Deployer:Deployment tools for Kastrax applications"
    "kastrax-observability:Kastrax Observability:Monitoring and observability tools for Kastrax"
    "kastrax-mcp:Kastrax MCP:Model Control Protocol implementation for Kastrax"
    "kastrax-codebase:Kastrax Codebase:Code understanding and analysis tools for Kastrax"
    "kastrax-datasource-common:Kastrax Datasource Common:Common data source interfaces for Kastrax"
)

# 集成模块
INTEGRATION_MODULES=(
    "kastrax-integrations/kastrax-openai:Kastrax OpenAI:OpenAI integration for Kastrax"
    "kastrax-integrations/kastrax-deepseek:Kastrax DeepSeek:DeepSeek integration for Kastrax"
    "kastrax-integrations/kastrax-anthropic:Kastrax Anthropic:Anthropic integration for Kastrax"
    "kastrax-integrations/kastrax-gemini:Kastrax Gemini:Google Gemini integration for Kastrax"
    "kastrax-integrations/kastrax-qwen:Kastrax Qwen:Qwen integration for Kastrax"
)

# 服务器模块
SERVER_MODULES=(
    "kastrax-server/common:Kastrax Server Common:Common server components for Kastrax"
    "kastrax-server/spring:Kastrax Server Spring:Spring Boot server implementation for Kastrax"
    "kastrax-server/ktor:Kastrax Server Ktor:Ktor server implementation for Kastrax"
    "kastrax-server/quarkus:Kastrax Server Quarkus:Quarkus server implementation for Kastrax"
)

# 运行时模块
RUNTIME_MODULES=(
    "kastrax-runtime/kastrax-runtime-api:Kastrax Runtime API:Runtime API for Kastrax"
    "kastrax-runtime/kastrax-runtime-jvm:Kastrax Runtime JVM:JVM runtime implementation for Kastrax"
    "kastrax-runtime/kastrax-runtime-test:Kastrax Runtime Test:Testing utilities for Kastrax runtime"
)

# 添加发布配置到模块
add_publishing_config() {
    local module_path=$1
    local module_name=$2
    local module_description=$3
    local build_file="${module_path}/build.gradle.kts"
    
    if [[ ! -f "$build_file" ]]; then
        print_warning "跳过 $module_path: build.gradle.kts 不存在"
        return
    fi
    
    print_info "配置模块: $module_path"
    
    # 检查是否已经有 vanniktech 插件
    if grep -q "com.vanniktech.maven.publish" "$build_file"; then
        print_warning "模块 $module_path 已经配置了发布插件"
        return
    fi
    
    # 检查是否有 maven-publish 插件需要替换
    if grep -q "\`maven-publish\`" "$build_file"; then
        sed -i '' 's/`maven-publish`/id("com.vanniktech.maven.publish")/g' "$build_file"
        print_info "替换了 maven-publish 插件"
    elif grep -q "maven-publish" "$build_file"; then
        sed -i '' 's/id("maven-publish")/id("com.vanniktech.maven.publish")/g' "$build_file"
        print_info "替换了 maven-publish 插件"
    else
        # 在 plugins 块中添加插件
        if grep -q "plugins {" "$build_file"; then
            sed -i '' '/plugins {/a\
    id("com.vanniktech.maven.publish")
' "$build_file"
            print_info "添加了 vanniktech 插件"
        else
            print_warning "无法找到 plugins 块，跳过 $module_path"
            return
        fi
    fi
    
    # 添加发布配置
    cat >> "$build_file" << EOF

// 配置 vanniktech maven publish 插件
mavenPublishing {
    pom {
        name.set("$module_name")
        description.set("$module_description")
    }
}
EOF
    
    print_success "已配置模块: $module_path"
}

# 主函数
main() {
    print_info "开始为Kastrax模块配置发布设置..."
    echo ""
    
    # 配置核心模块
    print_info "配置核心模块..."
    for module_info in "${CORE_MODULES[@]}"; do
        IFS=':' read -r module_path module_name module_description <<< "$module_info"
        add_publishing_config "$module_path" "$module_name" "$module_description"
    done
    
    echo ""
    
    # 配置集成模块
    print_info "配置集成模块..."
    for module_info in "${INTEGRATION_MODULES[@]}"; do
        IFS=':' read -r module_path module_name module_description <<< "$module_info"
        add_publishing_config "$module_path" "$module_name" "$module_description"
    done
    
    echo ""
    
    # 配置服务器模块
    print_info "配置服务器模块..."
    for module_info in "${SERVER_MODULES[@]}"; do
        IFS=':' read -r module_path module_name module_description <<< "$module_info"
        add_publishing_config "$module_path" "$module_name" "$module_description"
    done
    
    echo ""
    
    # 配置运行时模块
    print_info "配置运行时模块..."
    for module_info in "${RUNTIME_MODULES[@]}"; do
        IFS=':' read -r module_path module_name module_description <<< "$module_info"
        add_publishing_config "$module_path" "$module_name" "$module_description"
    done
    
    echo ""
    print_success "🎉 发布配置完成!"
    echo ""
    print_info "下一步:"
    echo "1. 运行 './scripts/test-publish-config.sh' 测试配置"
    echo "2. 设置发布凭据"
    echo "3. 运行 './scripts/publish.sh snapshot' 测试发布"
}

# 运行主函数
main "$@"
