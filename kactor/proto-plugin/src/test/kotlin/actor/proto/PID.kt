package actor.proto

/**
 * 测试用的PID类
 * @param address 地址
 * @param id ID
 */
data class TestPID(val address: String, val id: String)

/**
 * 创建PID实例
 * @param address 地址
 * @param id ID
 * @return PID实例
 */
fun createPID(address: String, id: String): TestPID {
    return TestPID(address, id)
}
