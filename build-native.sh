#!/bin/bash

# 构建KastraX Native应用的脚本

# 确定操作系统类型
OS=$(uname -s)
ARCH=$(uname -m)

echo "检测到操作系统: $OS ($ARCH)"

# 设置Gradle命令
GRADLE="./gradlew"

# 清理之前的构建
echo "清理之前的构建..."
$GRADLE :kastrax-native:clean

# 构建Native应用
echo "构建Native应用..."
$GRADLE :kastrax-native:buildAllExecutables --info

# 确定输出路径
if [ "$OS" = "Darwin" ]; then
    if [ "$ARCH" = "arm64" ]; then
        BINARY_PATH="kastrax-native/build/bin/native/releaseExecutable/kastrax-native.kexe"
        TARGET="macosArm64"
    else
        BINARY_PATH="kastrax-native/build/bin/native/releaseExecutable/kastrax-native.kexe"
        TARGET="macosX64"
    fi
elif [ "$OS" = "Linux" ]; then
    if [ "$ARCH" = "aarch64" ]; then
        BINARY_PATH="kastrax-native/build/bin/native/releaseExecutable/kastrax-native.kexe"
        TARGET="linuxArm64"
    else
        BINARY_PATH="kastrax-native/build/bin/native/releaseExecutable/kastrax-native.kexe"
        TARGET="linuxX64"
    fi
else
    echo "不支持的操作系统: $OS"
    exit 1
fi

# 检查构建是否成功
if [ -f "$BINARY_PATH" ]; then
    echo "构建成功！"
    echo "二进制文件位置: $BINARY_PATH"

    # 复制到更方便的位置
    mkdir -p build/native
    cp "$BINARY_PATH" build/native/kastrax-native
    echo "已复制到: build/native/kastrax-native"

    # 设置可执行权限
    chmod +x build/native/kastrax-native

    echo "可以使用以下命令运行应用:"
    echo "./build/native/kastrax-native"
else
    echo "没有找到Native可执行文件。"
    echo "这可能是因为:"
    echo "1. 当前平台不支持Native编译"
    echo "2. 缺少必要的开发工具（如Xcode命令行工具）"
    echo "3. 编译过程中出现错误"

    echo "
尝试构建JVM版本..."
    $GRADLE :kastrax-native:fatJar

    # 获取版本号
    VERSION="0.1.0"
    JVM_JAR="kastrax-native/build/libs/kastrax-native-full-$VERSION.jar"
    if [ -f "$JVM_JAR" ]; then
        echo "成功构建JVM版本！"
        echo "JVM JAR文件位置: $JVM_JAR"

        # 复制到更方便的位置
        mkdir -p build/jvm
        cp "$JVM_JAR" build/jvm/kastrax-native.jar
        echo "已复制到: build/jvm/kastrax-native.jar"

        echo "可以使用以下命令运行应用:"
        echo "java -jar build/jvm/kastrax-native.jar"
    else
        echo "构建JVM版本也失败了。"
        echo "请检查构建日志获取更多信息。"
        exit 1
    fi
fi
