@echo off
echo 开始构建 KastraX GraalVM Native Image...

REM 清理之前的构建
echo 清理之前的构建...
call ..\gradlew :graal-native:clean

REM 运行测试
echo 运行测试...
call ..\gradlew :graal-native:test

REM 构建 Native Image
echo 构建 Native Image...
call ..\gradlew :graal-native:nativeCompile

REM 检查构建结果
if exist "build\native\nativeCompile\kastrax.exe" (
    echo 构建成功！
    echo 可执行文件位于: %CD%\build\native\nativeCompile\kastrax.exe
    
    REM 创建分发包
    echo 创建分发包...
    call ..\gradlew :graal-native:packageNative
    
    echo 分发包位于: %CD%\build\distributions\
    
    REM 显示使用说明
    echo.
    echo 使用方法:
    echo   .\build\native\nativeCompile\kastrax.exe help    # 显示帮助信息
    echo   .\build\native\nativeCompile\kastrax.exe cli     # 启动命令行界面
    echo   .\build\native\nativeCompile\kastrax.exe config  # 显示配置信息
) else (
    echo 构建失败！
    exit /b 1
)
