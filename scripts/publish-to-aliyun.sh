#!/bin/bash

# 发布到阿里云Maven仓库

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

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查配置
check_aliyun_config() {
    log_info "检查阿里云配置..."
    
    local config_file="$HOME/.gradle/gradle.properties"
    
    if [[ ! -f "$config_file" ]]; then
        log_error "未找到gradle.properties文件"
        exit 1
    fi
    
    if ! grep -q "aliyunMavenUsername" "$config_file"; then
        log_error "未找到阿里云配置，请先运行 setup-aliyun-maven.sh"
        exit 1
    fi
    
    local username=$(grep "aliyunMavenUsername=" "$config_file" | cut -d'=' -f2)
    if [[ "$username" == "YOUR_ALIYUN_USERNAME" ]]; then
        log_error "请先配置阿里云用户名和密码"
        exit 1
    fi
    
    log_success "阿里云配置检查通过"
}

# 发布模块
publish_module() {
    local module=$1
    local target=${2:-"release"}
    
    log_info "发布模块 $module 到阿里云..."
    
    case $target in
        "release")
            ./gradlew ":$module:publishToAliyunMaven"
            ;;
        "snapshot")
            ./gradlew ":$module:publishToAliyunPrivate"
            ;;
        *)
            log_error "未知的发布目标: $target"
            exit 1
            ;;
    esac
    
    log_success "模块 $module 发布完成"
}

# 主函数
main() {
    local command=${1:-"help"}
    local module=${2:-"kastrax-core"}
    local target=${3:-"release"}
    
    case $command in
        "single")
            check_aliyun_config
            publish_module "$module" "$target"
            ;;
        "core")
            check_aliyun_config
            for mod in "kastrax-core" "kastrax-memory-api" "kastrax-rag" "kastrax-zod"; do
                publish_module "$mod" "$target"
            done
            ;;
        "all")
            check_aliyun_config
            ./gradlew publishToAliyunMaven
            ;;
        "help"|*)
            echo "阿里云Maven发布脚本"
            echo ""
            echo "用法: $0 <command> [module] [target]"
            echo ""
            echo "命令:"
            echo "  single <module> [target]  发布单个模块"
            echo "  core [target]             发布核心模块"
            echo "  all [target]              发布所有模块"
            echo ""
            echo "目标:"
            echo "  release                   发布到正式仓库"
            echo "  snapshot                  发布到快照仓库"
            echo ""
            echo "示例:"
            echo "  $0 single kastrax-core release"
            echo "  $0 core snapshot"
            echo "  $0 all release"
            ;;
    esac
}

main "$@"
