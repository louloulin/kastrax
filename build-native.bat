@echo off
REM 构建KastraX Native应用的Windows脚本

echo 清理之前的构建...
call gradlew.bat :kastrax-native:clean

echo 构建Native应用...
call gradlew.bat :kastrax-native:buildAllExecutables --info

set BINARY_PATH=kastrax-native\build\bin\native\releaseExecutable\kastrax-native.exe

REM 检查构建是否成功
if exist "%BINARY_PATH%" (
    echo 构建成功！
    echo 二进制文件位置: %BINARY_PATH%

    REM 复制到更方便的位置
    if not exist build\native mkdir build\native
    copy "%BINARY_PATH%" build\native\kastrax-native.exe
    echo 已复制到: build\native\kastrax-native.exe

    echo 可以使用以下命令运行应用:
    echo build\native\kastrax-native.exe
) else (
    echo 没有找到Native可执行文件。
    echo 这可能是因为:
    echo 1. 当前平台不支持Native编译
    echo 2. 缺少必要的开发工具（如Visual Studio）
    echo 3. 编译过程中出现错误

    echo.
    echo 尝试构建JVM版本...
    call gradlew.bat :kastrax-native:fatJar

    REM 获取版本号
    set VERSION=0.1.0
    set JVM_JAR=kastrax-native\build\libs\kastrax-native-full-%VERSION%.jar

    if exist "%JVM_JAR%" (
        echo 成功构建JVM版本！
        echo JVM JAR文件位置: %JVM_JAR%

        REM 复制到更方便的位置
        if not exist build\jvm mkdir build\jvm
        copy "%JVM_JAR%" build\jvm\kastrax-native.jar
        echo 已复制到: build\jvm\kastrax-native.jar

        echo 可以使用以下命令运行应用:
        echo java -jar build\jvm\kastrax-native.jar
    ) else (
        echo 构建JVM版本也失败了。
        echo 请检查构建日志获取更多信息。
        exit /b 1
    )
)
