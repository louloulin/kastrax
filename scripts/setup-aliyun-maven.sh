#!/bin/bash

# 阿里云Maven仓库发布配置脚本

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

# 创建阿里云Maven配置
create_aliyun_maven_config() {
    log_info "创建阿里云Maven仓库配置..."
    
    # 添加阿里云仓库配置到gradle.properties
    cat >> ~/.gradle/gradle.properties << 'EOF'

# 阿里云Maven仓库配置
aliyunMavenUrl=https://packages.aliyun.com/maven/repository/2421751-release-XXX/
aliyunMavenUsername=YOUR_ALIYUN_USERNAME
aliyunMavenPassword=YOUR_ALIYUN_PASSWORD

# 阿里云私有仓库配置 (可选)
aliyunPrivateUrl=https://packages.aliyun.com/maven/repository/2421751-snapshot-XXX/
aliyunPrivateUsername=YOUR_ALIYUN_USERNAME
aliyunPrivatePassword=YOUR_ALIYUN_PASSWORD
EOF

    log_success "阿里云Maven配置已添加到 ~/.gradle/gradle.properties"
    log_warning "请手动更新以下配置:"
    log_warning "1. aliyunMavenUrl - 您的阿里云Maven仓库URL"
    log_warning "2. aliyunMavenUsername - 您的阿里云用户名"
    log_warning "3. aliyunMavenPassword - 您的阿里云密码"
}

# 创建阿里云发布配置
create_aliyun_publishing_config() {
    log_info "创建阿里云发布配置文件..."
    
    cat > build-scripts/aliyun-publishing.gradle.kts << 'EOF'
// 阿里云Maven仓库发布配置

plugins {
    `maven-publish`
    `signing`
}

publishing {
    repositories {
        maven {
            name = "AliyunMaven"
            url = uri(findProperty("aliyunMavenUrl") as String? ?: "")
            credentials {
                username = findProperty("aliyunMavenUsername") as String?
                password = findProperty("aliyunMavenPassword") as String?
            }
        }
        
        maven {
            name = "AliyunPrivate"
            url = uri(findProperty("aliyunPrivateUrl") as String? ?: "")
            credentials {
                username = findProperty("aliyunPrivateUsername") as String?
                password = findProperty("aliyunPrivatePassword") as String?
            }
        }
    }
    
    publications {
        create<MavenPublication>("aliyunMaven") {
            from(components["java"])
            
            artifact(tasks.named("sourcesJar"))
            artifact(tasks.named("javadocJar"))
            
            pom {
                name.set(project.name)
                description.set(project.description ?: "Kastrax AI Framework Module")
                url.set("https://github.com/louloulin/kastrax")
                
                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }
                
                developers {
                    developer {
                        id.set("louloulin")
                        name.set("louloulin")
                        email.set("729883852@qq.com")
                    }
                }
                
                scm {
                    connection.set("scm:git:https://github.com/louloulin/kastrax.git")
                    developerConnection.set("scm:git:ssh://github.com/louloulin/kastrax.git")
                    url.set("https://github.com/louloulin/kastrax")
                }
            }
        }
    }
}

// 签名配置 (可选)
signing {
    val signingKeyId = findProperty("signing.keyId") as String?
    val signingPassword = findProperty("signing.password") as String?
    val signingSecretKey = findProperty("signing.secretKey") as String?
    
    if (!signingKeyId.isNullOrEmpty() && !signingPassword.isNullOrEmpty()) {
        if (!signingSecretKey.isNullOrEmpty()) {
            useInMemoryPgpKeys(signingKeyId, signingSecretKey, signingPassword)
        } else {
            useGpgCmd()
        }
        sign(publishing.publications["aliyunMaven"])
    }
}

// 发布任务
tasks.register("publishToAliyunMaven") {
    dependsOn("publishAliyunMavenPublicationToAliyunMavenRepository")
    group = "publishing"
    description = "发布到阿里云Maven仓库"
}

tasks.register("publishToAliyunPrivate") {
    dependsOn("publishAliyunMavenPublicationToAliyunPrivateRepository")
    group = "publishing"
    description = "发布到阿里云私有仓库"
}
EOF

    log_success "阿里云发布配置已创建: build-scripts/aliyun-publishing.gradle.kts"
}

# 创建阿里云发布脚本
create_aliyun_publish_script() {
    log_info "创建阿里云发布脚本..."
    
    cat > scripts/publish-to-aliyun.sh << 'EOF'
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
EOF

    chmod +x scripts/publish-to-aliyun.sh
    log_success "阿里云发布脚本已创建: scripts/publish-to-aliyun.sh"
}

# 创建阿里云使用说明
create_aliyun_usage_guide() {
    log_info "创建阿里云使用说明..."
    
    cat > ALIYUN_MAVEN_GUIDE.md << 'EOF'
# 📦 阿里云Maven仓库配置指南

## 🚀 快速开始

### 1. 注册阿里云账户
1. 访问 https://www.aliyun.com/
2. 注册阿里云账户
3. 实名认证

### 2. 创建Maven仓库
1. 登录阿里云控制台
2. 搜索"云效"或"Packages"
3. 创建Maven仓库实例
4. 获取仓库URL和凭据

### 3. 配置发布凭据
编辑 `~/.gradle/gradle.properties`:
```properties
# 阿里云Maven仓库配置
aliyunMavenUrl=https://packages.aliyun.com/maven/repository/YOUR-REPO-ID/
aliyunMavenUsername=YOUR_USERNAME
aliyunMavenPassword=YOUR_PASSWORD
```

### 4. 发布到阿里云
```bash
# 发布核心模块
./scripts/publish-to-aliyun.sh core

# 发布单个模块
./scripts/publish-to-aliyun.sh single kastrax-core

# 发布所有模块
./scripts/publish-to-aliyun.sh all
```

## 📋 使用发布的包

### 添加阿里云仓库
```kotlin
// build.gradle.kts
repositories {
    maven("https://packages.aliyun.com/maven/repository/YOUR-REPO-ID/")
    maven("https://maven.aliyun.com/repository/public") // 阿里云公共镜像
}
```

### 添加依赖
```kotlin
dependencies {
    implementation("ai.kastrax:kastrax-core:0.1.0")
    implementation("ai.kastrax:kastrax-memory-api:0.1.0")
    implementation("ai.kastrax:kastrax-rag:0.1.0")
    implementation("ai.kastrax:kastrax-zod:0.1.0")
}
```

## 🔧 高级配置

### 私有仓库配置
```properties
# 快照版本仓库
aliyunPrivateUrl=https://packages.aliyun.com/maven/repository/YOUR-SNAPSHOT-REPO/
aliyunPrivateUsername=YOUR_USERNAME
aliyunPrivatePassword=YOUR_PASSWORD
```

### 发布快照版本
```bash
./scripts/publish-to-aliyun.sh core snapshot
```

## 🌟 优势

- ✅ **国内访问速度快**: 阿里云CDN加速
- ✅ **稳定可靠**: 阿里云基础设施
- ✅ **免费使用**: 基础功能免费
- ✅ **企业级支持**: 付费版本提供企业级功能

## 📞 支持

- 阿里云文档: https://help.aliyun.com/
- 云效Packages: https://packages.aliyun.com/
- 技术支持: 阿里云工单系统
EOF

    log_success "阿里云使用说明已创建: ALIYUN_MAVEN_GUIDE.md"
}

# 主函数
main() {
    local command=${1:-"setup"}
    
    case $command in
        "setup")
            log_info "🚀 设置阿里云Maven仓库发布..."
            
            # 创建目录
            mkdir -p build-scripts
            mkdir -p scripts
            
            create_aliyun_maven_config
            create_aliyun_publishing_config
            create_aliyun_publish_script
            create_aliyun_usage_guide
            
            echo ""
            log_success "🎉 阿里云Maven配置完成!"
            echo ""
            echo "📋 下一步:"
            echo "1. 注册阿里云账户并创建Maven仓库"
            echo "2. 更新 ~/.gradle/gradle.properties 中的阿里云配置"
            echo "3. 运行 ./scripts/publish-to-aliyun.sh core 发布模块"
            echo "4. 查看 ALIYUN_MAVEN_GUIDE.md 了解详细配置"
            ;;
        "help"|*)
            echo "阿里云Maven仓库配置脚本"
            echo ""
            echo "用法: $0 <command>"
            echo ""
            echo "命令:"
            echo "  setup    设置阿里云Maven发布配置"
            echo "  help     显示此帮助信息"
            ;;
    esac
}

# 执行主函数
main "$@"
