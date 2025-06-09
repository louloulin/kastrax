#!/bin/bash

# JitPack 发布配置脚本

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查Git状态
check_git_status() {
    log_info "检查Git仓库状态..."
    
    if ! git rev-parse --git-dir > /dev/null 2>&1; then
        log_error "当前目录不是Git仓库"
        exit 1
    fi
    
    # 检查是否有未提交的更改
    if ! git diff-index --quiet HEAD --; then
        log_warning "检测到未提交的更改"
        echo "未提交的文件:"
        git status --porcelain
        echo ""
        read -p "是否继续? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log_info "操作已取消"
            exit 0
        fi
    fi
    
    log_success "Git仓库状态检查通过"
}

# 检查远程仓库
check_remote_repository() {
    log_info "检查远程仓库配置..."
    
    local remote_url=$(git remote get-url origin 2>/dev/null || echo "")
    
    if [[ -z "$remote_url" ]]; then
        log_error "未找到origin远程仓库"
        log_error "请先添加GitHub远程仓库:"
        log_error "git remote add origin https://github.com/louloulin/kastrax.git"
        exit 1
    fi
    
    log_success "远程仓库: $remote_url"
    
    # 检查是否是GitHub仓库
    if [[ "$remote_url" != *"github.com"* ]]; then
        log_warning "远程仓库不是GitHub，JitPack可能无法工作"
    fi
}

# 创建JitPack配置文件
create_jitpack_config() {
    log_info "创建JitPack配置文件..."
    
    # 创建jitpack.yml配置文件
    cat > jitpack.yml << 'EOF'
# JitPack配置文件
# 指定JDK版本
jdk:
  - openjdk17

# 构建前脚本
before_install:
  - echo "准备构建Kastrax项目..."

# 自定义安装脚本
install:
  - echo "开始构建..."
  - ./gradlew publishToMavenLocal -x test

# 构建后脚本
after_success:
  - echo "构建成功完成!"

# 环境变量
env:
  - GRADLE_OPTS="-Xmx2g -XX:MaxMetaspaceSize=512m"
EOF

    log_success "JitPack配置文件已创建: jitpack.yml"
}

# 创建发布标签
create_release_tag() {
    local version=${1:-"v0.1.0"}
    
    log_info "创建发布标签: $version"
    
    # 检查标签是否已存在
    if git tag -l | grep -q "^$version$"; then
        log_warning "标签 $version 已存在"
        read -p "是否删除并重新创建? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            git tag -d "$version"
            git push origin --delete "$version" 2>/dev/null || true
        else
            log_info "使用现有标签"
            return 0
        fi
    fi
    
    # 创建标签
    git tag -a "$version" -m "Release $version - Kastrax AI Framework"
    
    log_success "标签 $version 已创建"
}

# 推送到GitHub
push_to_github() {
    local version=${1:-"v0.1.0"}
    
    log_info "推送代码和标签到GitHub..."
    
    # 推送代码
    git push origin main || git push origin master
    
    # 推送标签
    git push origin "$version"
    
    log_success "代码和标签已推送到GitHub"
}

# 触发JitPack构建
trigger_jitpack_build() {
    local version=${1:-"v0.1.0"}
    local repo="louloulin/kastrax"
    
    log_info "触发JitPack构建..."
    
    local jitpack_url="https://jitpack.io/#$repo/$version"
    
    log_info "JitPack构建URL: $jitpack_url"
    
    # 尝试触发构建
    log_info "正在触发构建..."
    
    # 使用curl触发构建
    local build_url="https://jitpack.io/com/github/$repo/$version/build.log"
    
    if command -v curl >/dev/null 2>&1; then
        log_info "使用curl触发构建..."
        curl -s "$build_url" > /dev/null || true
    fi
    
    log_success "JitPack构建已触发"
    log_info "请访问以下URL查看构建状态:"
    log_info "$jitpack_url"
}

# 生成使用说明
generate_usage_instructions() {
    local version=${1:-"v0.1.0"}
    
    log_info "生成使用说明..."
    
    cat > JITPACK_USAGE.md << EOF
# 📦 Kastrax JitPack 使用指南

## 🚀 快速开始

### 添加仓库
\`\`\`kotlin
// build.gradle.kts
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}
\`\`\`

### 添加依赖
\`\`\`kotlin
dependencies {
    // 核心模块
    implementation("ai.kastrax:kastrax-core:$version")

    // 内存管理
    implementation("ai.kastrax:kastrax-memory-api:$version")

    // RAG系统
    implementation("ai.kastrax:kastrax-rag:$version")

    // 数据验证
    implementation("ai.kastrax:kastrax-zod:$version")

    // 或者使用完整项目 (通过JitPack)
    implementation("com.github.louloulin:kastrax:$version")
}
\`\`\`

## 🔗 链接

- **JitPack页面**: https://jitpack.io/#louloulin/kastrax
- **构建状态**: https://jitpack.io/#louloulin/kastrax/$version
- **GitHub仓库**: https://github.com/louloulin/kastrax

## 📋 可用模块

| 模块 | 依赖 | 描述 |
|------|------|------|
| kastrax-core | \`ai.kastrax:kastrax-core:$version\` | 核心框架 |
| kastrax-memory-api | \`ai.kastrax:kastrax-memory-api:$version\` | 内存管理API |
| kastrax-rag | \`ai.kastrax:kastrax-rag:$version\` | RAG系统 |
| kastrax-zod | \`ai.kastrax:kastrax-zod:$version\` | 数据验证 |

## 🎯 版本信息

- **当前版本**: $version
- **发布时间**: $(date '+%Y-%m-%d %H:%M:%S')
- **JDK要求**: 17+
- **Kotlin版本**: 1.9.25

## 🔄 更新版本

要使用新版本，只需更新版本号:
\`\`\`kotlin
// 通过JitPack使用
implementation("com.github.louloulin:kastrax:NEW_VERSION")

// 或者直接使用发布的包
implementation("ai.kastrax:kastrax-core:NEW_VERSION")
\`\`\`

JitPack会自动构建新版本。

## 🐛 问题排查

如果遇到构建问题:
1. 检查JitPack构建日志
2. 确认版本标签存在
3. 验证GitHub仓库可访问

## 📞 支持

- GitHub Issues: https://github.com/louloulin/kastrax/issues
- JitPack文档: https://jitpack.io/docs/
EOF

    log_success "使用说明已生成: JITPACK_USAGE.md"
}

# 验证JitPack可用性
verify_jitpack_availability() {
    local version=${1:-"v0.1.0"}
    local repo="louloulin/kastrax"
    
    log_info "验证JitPack可用性..."
    
    # 等待一段时间让构建完成
    log_info "等待构建完成 (30秒)..."
    sleep 30
    
    # 检查构建状态
    local status_url="https://jitpack.io/api/builds/com.github.$repo/$version"
    
    if command -v curl >/dev/null 2>&1; then
        log_info "检查构建状态..."
        local status=$(curl -s "$status_url" | grep -o '"status":"[^"]*"' | cut -d'"' -f4 2>/dev/null || echo "unknown")
        
        case $status in
            "ok")
                log_success "JitPack构建成功!"
                ;;
            "building")
                log_info "JitPack正在构建中..."
                ;;
            "error")
                log_warning "JitPack构建失败，请检查构建日志"
                ;;
            *)
                log_info "构建状态: $status"
                ;;
        esac
    fi
    
    log_info "请访问 https://jitpack.io/#$repo 查看详细状态"
}

# 主函数
main() {
    local command=${1:-"setup"}
    local version=${2:-"v0.1.0"}
    
    case $command in
        "setup")
            log_info "🚀 设置JitPack发布..."
            check_git_status
            check_remote_repository
            create_jitpack_config
            create_release_tag "$version"
            push_to_github "$version"
            trigger_jitpack_build "$version"
            generate_usage_instructions "$version"
            verify_jitpack_availability "$version"
            
            echo ""
            log_success "🎉 JitPack发布设置完成!"
            echo ""
            echo "📋 下一步:"
            echo "1. 访问 https://jitpack.io/#louloulin/kastrax 查看构建状态"
            echo "2. 构建成功后，其他项目可以使用以下依赖:"
            echo "   implementation(\"com.github.louloulin:kastrax:$version\")"
            echo "3. 查看 JITPACK_USAGE.md 了解详细使用方法"
            ;;
        "tag")
            log_info "🏷️ 创建新版本标签..."
            check_git_status
            create_release_tag "$version"
            push_to_github "$version"
            trigger_jitpack_build "$version"
            ;;
        "build")
            log_info "🔨 触发JitPack构建..."
            trigger_jitpack_build "$version"
            verify_jitpack_availability "$version"
            ;;
        "usage")
            log_info "📖 生成使用说明..."
            generate_usage_instructions "$version"
            ;;
        "help"|*)
            echo "JitPack发布配置脚本"
            echo ""
            echo "用法: $0 <command> [version]"
            echo ""
            echo "命令:"
            echo "  setup [version]   完整设置JitPack发布 (默认: v0.1.0)"
            echo "  tag [version]     创建新版本标签"
            echo "  build [version]   触发JitPack构建"
            echo "  usage [version]   生成使用说明"
            echo "  help              显示此帮助信息"
            echo ""
            echo "示例:"
            echo "  $0 setup v0.1.0      # 设置v0.1.0版本发布"
            echo "  $0 tag v0.2.0        # 创建v0.2.0标签"
            echo "  $0 build v0.1.0      # 触发v0.1.0构建"
            ;;
    esac
}

# 执行主函数
main "$@"
