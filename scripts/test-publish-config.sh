#!/bin/bash

# 测试发布配置脚本
# 验证发布配置是否正确，但不实际发布

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

print_info "🧪 测试Kastrax发布配置..."
echo ""

# 检查Gradle配置
print_info "检查Gradle配置..."
if ./gradlew help > /dev/null 2>&1; then
    print_success "Gradle配置正常"
else
    print_error "Gradle配置有问题"
    exit 1
fi

# 检查发布插件
print_info "检查发布插件配置..."
if ./gradlew tasks --all | grep -q "publishToMavenCentral"; then
    print_success "vanniktech maven publish插件配置正常"
else
    print_error "未找到publishToMavenCentral任务，请检查插件配置"
    exit 1
fi

# 检查可发布的模块
print_info "检查可发布的模块..."
publishable_modules=$(./gradlew tasks --all | grep ":.*:publishToMavenCentral" | wc -l)
if [ "$publishable_modules" -gt 0 ]; then
    print_success "找到 $publishable_modules 个可发布的模块"
    ./gradlew tasks --all | grep ":.*:publishToMavenCentral" | head -10
    if [ "$publishable_modules" -gt 10 ]; then
        echo "... 还有 $((publishable_modules - 10)) 个模块"
    fi
else
    print_warning "未找到可发布的模块"
fi

# 检查POM生成
print_info "测试POM文件生成..."
if ./gradlew generatePomFileForMavenPublication > /dev/null 2>&1; then
    print_success "POM文件生成正常"
    
    # 检查生成的POM文件
    pom_files=$(find . -name "pom-default.xml" | head -3)
    if [ -n "$pom_files" ]; then
        print_info "示例POM文件内容:"
        echo "$pom_files" | head -1 | xargs cat | head -20
        echo "..."
    fi
else
    print_warning "POM文件生成可能有问题"
fi

# 检查签名配置
print_info "检查签名配置..."
if [ -n "$SIGNING_KEY_ID" ] && [ -n "$SIGNING_PASSWORD" ] && [ -n "$SIGNING_SECRET_KEY" ]; then
    print_success "签名环境变量已设置"
elif grep -q "signing.keyId" ~/.gradle/gradle.properties 2>/dev/null; then
    print_success "签名配置在gradle.properties中找到"
else
    print_warning "未找到签名配置，发布时需要设置签名参数"
fi

# 检查Maven Central凭据
print_info "检查Maven Central凭据..."
if [ -n "$MAVEN_CENTRAL_USERNAME" ] && [ -n "$MAVEN_CENTRAL_PASSWORD" ]; then
    print_success "Maven Central环境变量已设置"
elif grep -q "mavenCentralUsername" ~/.gradle/gradle.properties 2>/dev/null; then
    print_success "Maven Central凭据在gradle.properties中找到"
else
    print_warning "未找到Maven Central凭据，发布时需要设置"
fi

# 运行构建测试
print_info "运行构建测试..."
if ./gradlew build -x test --dry-run > /dev/null 2>&1; then
    print_success "构建配置正常"
else
    print_error "构建配置有问题"
    exit 1
fi

echo ""
print_success "🎉 发布配置测试完成!"
echo ""
print_info "下一步:"
echo "1. 设置必要的环境变量或gradle.properties"
echo "2. 运行 './scripts/publish.sh snapshot' 测试快照发布"
echo "3. 运行 './scripts/publish.sh release' 进行正式发布"
