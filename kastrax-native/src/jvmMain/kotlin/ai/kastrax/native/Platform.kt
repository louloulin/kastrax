package ai.kastrax.native

/**
 * JVM平台实现
 */
actual class Platform {
    actual val name: String = "JVM"
    
    actual fun getSystemInfo(): String {
        return "Java ${System.getProperty("java.version")} (${System.getProperty("java.vendor")})\n" +
               "OS: ${System.getProperty("os.name")} ${System.getProperty("os.version")} (${System.getProperty("os.arch")})"
    }
}
