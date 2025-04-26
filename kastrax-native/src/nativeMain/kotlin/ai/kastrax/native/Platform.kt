package ai.kastrax.native

import platform.posix.uname
import platform.posix.utsname
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)

/**
 * Native平台实现
 */
actual class Platform {
    actual val name: String = "Native"

    actual fun getSystemInfo(): String {
        return memScoped {
            val utsname = alloc<utsname>()
            uname(utsname.ptr)

            "系统: ${utsname.sysname?.toKString() ?: "Unknown"}\n" +
            "版本: ${utsname.release?.toKString() ?: "Unknown"}\n" +
            "架构: ${utsname.machine?.toKString() ?: "Unknown"}"
        }
    }
}
