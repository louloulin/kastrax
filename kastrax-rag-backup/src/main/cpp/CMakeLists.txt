cmake_minimum_required(VERSION 3.10)
project(faiss_jni)

# 设置 C++ 标准
set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 查找 JNI 包
find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

# 查找 FAISS 库
find_package(faiss REQUIRED)

# 添加库
add_library(faiss_jni SHARED faiss_jni.cpp)

# 链接 FAISS 和 JNI 库
target_link_libraries(faiss_jni ${JNI_LIBRARIES} faiss)

# 设置输出目录
set_target_properties(faiss_jni PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/lib"
    RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/bin"
)

# 安装目标
install(TARGETS faiss_jni
    LIBRARY DESTINATION lib
    RUNTIME DESTINATION bin
)
