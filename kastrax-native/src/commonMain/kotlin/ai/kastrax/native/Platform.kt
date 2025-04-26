package ai.kastrax.native

/**
 * 平台特定功能的接口
 */
expect class Platform() {
    val name: String
    fun getSystemInfo(): String
}

/**
 * 获取当前平台信息
 */
fun getPlatformInfo(): String {
    val platform = Platform()
    return "平台: ${platform.name}\n系统信息: ${platform.getSystemInfo()}"
}
