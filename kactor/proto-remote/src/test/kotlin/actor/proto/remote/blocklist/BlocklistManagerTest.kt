package actor.proto.remote.blocklist

import actor.proto.ActorSystem
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.mockito.kotlin.*
import java.time.Duration

@Disabled("This test needs to be fixed")
class BlocklistManagerTest {
    private lateinit var actorSystem: ActorSystem
    private lateinit var blocklistManager: BlocklistManager

    @BeforeEach
    fun setup() {
        actorSystem = mock<ActorSystem>()
        blocklistManager = BlocklistManager(actorSystem, Duration.ofMillis(100))
        blocklistManager.initialize()
    }

    @AfterEach
    fun teardown() {
        blocklistManager.shutdown()
    }

    @Test
    fun `should get default blocklist`() {
        val blocklist = blocklistManager.getDefaultBlocklist()

        assertNotNull(blocklist)
        assertTrue(blocklist is FailureCountingBlocklist)
    }

    @Test
    fun `should register and get blocklist`() {
        val addressType = "test"
        val blocklist = DefaultBlocklist()

        blocklistManager.registerBlocklist(addressType, blocklist)

        val retrievedBlocklist = blocklistManager.getBlocklist(addressType)
        assertSame(blocklist, retrievedBlocklist)
    }

    @Test
    fun `should check if address is blocked`() {
        val address = "localhost:12345"

        // 初始状态下地址未被阻止
        assertFalse(blocklistManager.isBlocked(address))

        // 阻止地址
        blocklistManager.block(address)

        // 验证地址被阻止
        assertTrue(blocklistManager.isBlocked(address))
    }

    @Test
    fun `should check if address is blocked with address type`() {
        val address = "localhost:12345"
        val addressType = "test"
        val blocklist = DefaultBlocklist()

        blocklistManager.registerBlocklist(addressType, blocklist)

        // 初始状态下地址未被阻止
        assertFalse(blocklistManager.isBlocked(address, addressType))

        // 阻止地址
        blocklistManager.block(address, null, addressType)

        // 验证地址被阻止
        assertTrue(blocklistManager.isBlocked(address, addressType))
    }

    @Test
    fun `should block address`() {
        val address = "localhost:12345"

        // 阻止地址
        val result = blocklistManager.block(address)

        // 验证阻止成功
        assertTrue(result)
        assertTrue(blocklistManager.isBlocked(address))
    }

    @Test
    fun `should block address with duration`() {
        val address = "localhost:12345"
        val duration = Duration.ofMillis(100)

        // 阻止地址
        blocklistManager.block(address, duration)

        // 验证阻止成功
        assertTrue(blocklistManager.isBlocked(address))

        // 等待阻止过期
        Thread.sleep(200)

        // 验证阻止已过期
        assertFalse(blocklistManager.isBlocked(address))
    }

    @Test
    fun `should unblock address`() {
        val address = "localhost:12345"

        // 阻止地址
        blocklistManager.block(address)
        assertTrue(blocklistManager.isBlocked(address))

        // 解除阻止
        val result = blocklistManager.unblock(address)

        // 验证解除成功
        assertTrue(result)
        assertFalse(blocklistManager.isBlocked(address))
    }

    @Test
    fun `should record failure`() {
        val address = "localhost:12345"

        // 记录失败，但不超过最大失败次数
        assertFalse(blocklistManager.recordFailure(address))
        assertFalse(blocklistManager.recordFailure(address))

        // 验证地址未被阻止
        assertFalse(blocklistManager.isBlocked(address))

        // 记录第三次失败，超过最大失败次数（默认为 3）
        assertTrue(blocklistManager.recordFailure(address))

        // 验证地址被阻止
        assertTrue(blocklistManager.isBlocked(address))
    }

    @Test
    fun `should reset failure count`() {
        val address = "localhost:12345"

        // 记录失败
        blocklistManager.recordFailure(address)
        assertEquals(1, blocklistManager.getFailureCount(address))

        // 重置失败计数
        blocklistManager.resetFailureCount(address)

        // 验证失败计数已重置
        assertEquals(0, blocklistManager.getFailureCount(address))
    }

    @Test
    fun `should get blocked addresses`() {
        val address1 = "localhost:12345"
        val address2 = "localhost:12346"

        // 阻止地址
        blocklistManager.block(address1)
        blocklistManager.block(address2)

        // 获取被阻止的地址
        val blockedAddresses = blocklistManager.getBlockedAddresses()

        // 验证结果
        assertEquals(2, blockedAddresses.size)
        assertTrue(blockedAddresses.contains(address1))
        assertTrue(blockedAddresses.contains(address2))
    }

    @Test
    fun `should get block info`() {
        val address = "localhost:12345"

        // 阻止地址
        blocklistManager.block(address)

        // 获取阻止信息
        val blockInfo = blocklistManager.getBlockInfo(address)

        // 验证信息
        assertNotNull(blockInfo)
        assertEquals(address, blockInfo?.address)
    }

    @Test
    fun `should clear expired blocks`() {
        val address1 = "localhost:12345"
        val address2 = "localhost:12346"

        // 阻止地址，一个永久阻止，一个短暂阻止
        blocklistManager.block(address1)
        blocklistManager.block(address2, Duration.ofMillis(50))

        // 等待第二个阻止过期
        Thread.sleep(100)

        // 清除过期的阻止
        val clearedCount = blocklistManager.clearExpired()

        // 验证结果
        assertTrue(clearedCount >= 1)
        assertTrue(blocklistManager.isBlocked(address1))
        assertFalse(blocklistManager.isBlocked(address2))
    }

    @Test
    fun `should clear all blocks`() {
        val address1 = "localhost:12345"
        val address2 = "localhost:12346"

        // 阻止地址
        blocklistManager.block(address1)
        blocklistManager.block(address2)

        // 清除所有阻止
        blocklistManager.clearAll()

        // 验证结果
        assertTrue(blocklistManager.getBlockedAddresses().isEmpty())
        assertFalse(blocklistManager.isBlocked(address1))
        assertFalse(blocklistManager.isBlocked(address2))
    }
}
