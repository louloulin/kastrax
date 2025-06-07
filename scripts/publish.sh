#!/bin/bash

# Kastrax Maven Central 发布脚本
# 使用方法: ./scripts/publish.sh [snapshot|release]

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的消息
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

# 检查必需的环境变量
check_env_vars() {
    print_info "检查环境变量..."
    
    local required_vars=(
        "MAVEN_CENTRAL_USERNAME"
        "MAVEN_CENTRAL_PASSWORD"
        "SIGNING_KEY_ID"
        "SIGNING_PASSWORD"
        "SIGNING_SECRET_KEY"
    )
    
    local missing_vars=()
    
    for var in "${required_vars[@]}"; do
        if [[ -z "${!var}" ]]; then
            missing_vars+=("$var")
        fi
    done
    
    if [[ ${#missing_vars[@]} -gt 0 ]]; then
        print_error "缺少必需的环境变量:"
        for var in "${missing_vars[@]}"; do
            echo "  - $var"
        done
        echo ""
        echo "请设置这些环境变量或在 ~/.gradle/gradle.properties 中配置:"
        echo "  mavenCentralUsername=your-username"
        echo "  mavenCentralPassword=your-password"
        echo "  signing.keyId=your-key-id"
        echo "  signing.password=your-key-password"
        echo "  signing.secretKey=your-secret-key"
        exit 1
    fi
    
    print_success "环境变量检查通过"
}

# 检查Git状态
check_git_status() {
    print_info "检查Git状态..."
    
    if [[ -n $(git status --porcelain) ]]; then
        print_warning "工作目录有未提交的更改"
        git status --short
        echo ""
        read -p "是否继续发布? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            print_info "发布已取消"
            exit 0
        fi
    fi
    
    print_success "Git状态检查通过"
}

# 运行测试
run_tests() {
    print_info "运行测试..."
    
    if ! ./gradlew test; then
        print_error "测试失败，发布已取消"
        exit 1
    fi
    
    print_success "所有测试通过"
}

# 发布到Maven Central
publish_to_maven_central() {
    local publish_type=$1
    
    if [[ "$publish_type" == "snapshot" ]]; then
        print_info "发布SNAPSHOT版本到Maven Central..."
        ./gradlew publishToMavenCentral
    else
        print_info "发布RELEASE版本到Maven Central..."
        ./gradlew publishToMavenCentral --no-configuration-cache
    fi
    
    print_success "发布完成!"
}

# 主函数
main() {
    local publish_type=${1:-release}
    
    print_info "开始Kastrax发布流程 (类型: $publish_type)"
    echo ""
    
    # 检查发布类型
    if [[ "$publish_type" != "snapshot" && "$publish_type" != "release" ]]; then
        print_error "无效的发布类型: $publish_type"
        echo "使用方法: $0 [snapshot|release]"
        exit 1
    fi
    
    # 执行检查和发布
    check_env_vars
    check_git_status
    run_tests
    publish_to_maven_central "$publish_type"
    
    echo ""
    print_success "🎉 发布完成!"
    
    if [[ "$publish_type" == "release" ]]; then
        echo ""
        print_info "下一步:"
        echo "1. 访问 https://central.sonatype.com/"
        echo "2. 登录并检查发布状态"
        echo "3. 如果启用了自动发布，包将在几分钟内同步到Maven Central"
        echo "4. 否则，您需要在Portal中手动发布"
    fi
}

# 运行主函数
main "$@"
