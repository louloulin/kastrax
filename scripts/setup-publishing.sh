#!/bin/bash

# Kastrax Maven Central 发布配置脚本
# 此脚本帮助您快速配置发布到 Maven Central 所需的环境

set -e

echo "🚀 Kastrax Maven Central 发布配置向导"
echo "========================================"

# 检查必要的工具
echo "📋 检查必要工具..."

if ! command -v gpg &> /dev/null; then
    echo "❌ GPG 未安装。请先安装 GPG:"
    echo "   macOS: brew install gnupg"
    echo "   Ubuntu: sudo apt-get install gnupg"
    exit 1
fi

if ! command -v gradle &> /dev/null && [ ! -f "./gradlew" ]; then
    echo "❌ Gradle 未找到。请确保项目根目录有 gradlew 文件。"
    exit 1
fi

echo "✅ 工具检查完成"

# 检查是否已有 GPG 密钥
echo ""
echo "🔐 检查 GPG 密钥..."

if gpg --list-secret-keys | grep -q "sec"; then
    echo "✅ 找到现有的 GPG 密钥:"
    gpg --list-secret-keys --keyid-format LONG
    echo ""
    read -p "是否使用现有密钥？(y/n): " use_existing
    
    if [[ $use_existing == "y" || $use_existing == "Y" ]]; then
        echo "请选择要使用的密钥 ID (格式: XXXXXXXXXXXXXXXX):"
        read -p "密钥 ID: " key_id
    else
        echo "📝 生成新的 GPG 密钥..."
        echo "请按照提示输入信息:"
        gpg --gen-key
        
        echo "✅ 密钥生成完成。请选择密钥 ID:"
        gpg --list-secret-keys --keyid-format LONG
        read -p "密钥 ID: " key_id
    fi
else
    echo "📝 未找到 GPG 密钥，正在生成新密钥..."
    echo "请按照提示输入信息:"
    gpg --gen-key
    
    echo "✅ 密钥生成完成。请选择密钥 ID:"
    gpg --list-secret-keys --keyid-format LONG
    read -p "密钥 ID: " key_id
fi

# 导出公钥到密钥服务器
echo ""
echo "📤 上传公钥到密钥服务器..."
gpg --keyserver keyserver.ubuntu.com --send-keys $key_id
echo "✅ 公钥上传完成"

# 导出私钥
echo ""
echo "💾 导出私钥..."
secring_file="$HOME/.gnupg/secring.gpg"
gpg --export-secret-keys $key_id > "$secring_file"
echo "✅ 私钥已导出到: $secring_file"

# 收集 Sonatype 凭据
echo ""
echo "🔑 配置 Sonatype OSSRH 凭据"
echo "请访问 https://issues.sonatype.org/ 注册账户并申请 ai.kastrax 组权限"
read -p "Sonatype 用户名: " ossrh_username
read -s -p "Sonatype 密码: " ossrh_password
echo ""
read -s -p "GPG 密钥密码: " signing_password
echo ""

# 创建 gradle.properties 文件
echo ""
echo "📝 创建 Gradle 配置文件..."

gradle_props="$HOME/.gradle/gradle.properties"
mkdir -p "$HOME/.gradle"

# 备份现有配置
if [ -f "$gradle_props" ]; then
    cp "$gradle_props" "$gradle_props.backup.$(date +%Y%m%d_%H%M%S)"
    echo "✅ 已备份现有配置文件"
fi

# 添加发布配置
cat >> "$gradle_props" << EOF

# Maven Central 发布配置
ossrhUsername=$ossrh_username
ossrhPassword=$ossrh_password
signing.keyId=$key_id
signing.password=$signing_password
signing.secretKeyRingFile=$secring_file
EOF

echo "✅ Gradle 配置已更新: $gradle_props"

# 创建环境变量脚本
echo ""
echo "📝 创建环境变量脚本..."

env_script="$(pwd)/scripts/publishing-env.sh"
cat > "$env_script" << EOF
#!/bin/bash
# Maven Central 发布环境变量
# 使用方法: source scripts/publishing-env.sh

export OSSRH_USERNAME="$ossrh_username"
export OSSRH_PASSWORD="$ossrh_password"
export SIGNING_KEY_ID="$key_id"
export SIGNING_PASSWORD="$signing_password"
export SIGNING_SECRET_KEY_RING_FILE="$secring_file"

echo "✅ Maven Central 发布环境变量已设置"
EOF

chmod +x "$env_script"
echo "✅ 环境变量脚本已创建: $env_script"

# 测试配置
echo ""
echo "🧪 测试发布配置..."
echo "正在执行: ./gradlew publishToMavenLocal --no-configuration-cache"

if ./gradlew publishToMavenLocal --no-configuration-cache; then
    echo "✅ 发布配置测试成功！"
else
    echo "❌ 发布配置测试失败，请检查配置"
    exit 1
fi

# 完成
echo ""
echo "🎉 配置完成！"
echo "========================================"
echo "📋 下一步操作:"
echo "1. 确保已在 Sonatype JIRA 中申请 ai.kastrax 组权限"
echo "2. 等待 Sonatype 团队批准"
echo "3. 使用以下命令发布:"
echo "   ./gradlew publish"
echo ""
echo "📚 更多信息请参考: PUBLISHING.md"
echo "🔧 环境变量脚本: source scripts/publishing-env.sh"
echo "⚙️  Gradle 配置文件: $gradle_props"
echo ""
echo "⚠️  重要提醒:"
echo "   - 请妥善保管您的 GPG 私钥和密码"
echo "   - 不要将凭据提交到版本控制系统"
echo "   - 建议定期备份 GPG 密钥"