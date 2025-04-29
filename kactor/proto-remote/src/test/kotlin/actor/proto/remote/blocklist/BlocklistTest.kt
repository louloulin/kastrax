package actor.proto.remote.blocklist

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*
import java.time.Duration
import java.time.Instant

class BlocklistTest {
    private lateinit var blocklist: Blocklist
    
    @BeforeEach
    fun setup() {
        blocklist = DefaultBlocklist()
    }
    
    @Test
    fun `should block and check address`() {
        val address = "localhost:12345"
        
        // 初始状态下地址未被阻止
        assertFalse(blocklist.isBlocked(address))
        
        // 阻止地址
        val result = blocklist.block(address)
        
        // 验证阻止成功
        assertTrue(result)
        assertTrue(blocklist.isBlocked(address))
    }
    
    @Test
    fun `should block with duration and expire`() {
        val address = "localhost:12345"
        val duration = Duration.ofMillis(100) // 短暂的阻止时间
        
        // 阻止地址
        blocklist.block(address, duration)
        
        // 验证阻止成功
        assertTrue(blocklist.isBlocked(address))
        
        // 等待阻止过期
        Thread.sleep(200)
        
        // 验证阻止已过期
        assertFalse(blocklist.isBlocked(address))
    }
    
    @Test
    fun `should unblock address`() {
        val address = "localhost:12345"
        
        // 阻止地址
        blocklist.block(address)
        assertTrue(blocklist.isBlocked(address))
        
        // 解除阻止
        val result = blocklist.unblock(address)
        
        // 验证解除成功
        assertTrue(result)
        assertFalse(blocklist.isBlocked(address))
    }
    
    @Test
    fun `should get blocked addresses`() {
        val address1 = "localhost:12345"
        val address2 = "localhost:12346"
        
        // 阻止地址
        blocklist.block(address1)
        blocklist.block(address2)
        
        // 获取被阻止的地址
        val blockedAddresses = blocklist.getBlockedAddresses()
        
        // 验证结果
        assertEquals(2, blockedAddresses.size)
        assertTrue(blockedAddresses.contains(address1))
        assertTrue(blockedAddresses.contains(address2))
    }
    
    @Test
    fun `should get block info`() {
        val address = "localhost:12345"
        val duration = Duration.ofMinutes(5)
        
        // 阻止地址
        blocklist.block(address, duration)
        
        // 获取阻止信息
        val blockInfo = blocklist.getBlockInfo(address)
        
        // 验证信息
        assertNotNull(blockInfo)
        assertEquals(address, blockInfo?.address)
        assertEquals(duration, blockInfo?.duration)
        assertNotNull(blockInfo?.blockedAt)
        assertNotNull(blockInfo?.expiresAt())
    }
    
    @Test
    fun `should clear expired blocks`() {
        val address1 = "localhost:12345"
        val address2 = "localhost:12346"
        
        // 阻止地址，一个永久阻止，一个短暂阻止
        blocklist.block(address1)
        blocklist.block(address2, Duration.ofMillis(100))
        
        // 等待第二个阻止过期
        Thread.sleep(200)
        
        // 清除过期的阻止
        val clearedCount = blocklist.clearExpired()
        
        // 验证结果
        assertEquals(1, clearedCount)
        assertTrue(blocklist.isBlocked(address1))
        assertFalse(blocklist.isBlocked(address2))
    }
    
    @Test
    fun `should clear all blocks`() {
        val address1 = "localhost:12345"
        val address2 = "localhost:12346"
        
        // 阻止地址
        blocklist.block(address1)
        blocklist.block(address2)
        
        // 清除所有阻止
        blocklist.clearAll()
        
        // 验证结果
        assertTrue(blocklist.getBlockedAddresses().isEmpty())
        assertFalse(blocklist.isBlocked(address1))
        assertFalse(blocklist.isBlocked(address2))
    }
}

class FailureCountingBlocklistTest {
    private lateinit var blocklist: FailureCountingBlocklist
    
    @BeforeEach
    fun setup() {
        blocklist = FailureCountingBlocklist(maxFailures = 3, blockDuration = Duration.ofMinutes(5))
    }
    
    @Test
    fun `should record failures and block after max failures`() {
        val address = "localhost:12345"
        
        // 记录失败，但不超过最大失败次数
        assertFalse(blocklist.recordFailure(address, "Test failure 1"))
        assertFalse(blocklist.recordFailure(address, "Test failure 2"))
        
        // 验证地址未被阻止
        assertFalse(blocklist.isBlocked(address))
        
        // 记录第三次失败，超过最大失败次数
        assertTrue(blocklist.recordFailure(address, "Test failure 3"))
        
        // 验证地址被阻止
        assertTrue(blocklist.isBlocked(address))
        
        // 验证阻止信息
        val blockInfo = blocklist.getBlockInfo(address)
        assertNotNull(blockInfo)
        assertEquals(address, blockInfo?.address)
        assertEquals(3, blockInfo?.failureCount)
        assertEquals("Test failure 3", blockInfo?.reason)
    }
    
    @Test
    fun `should reset failure count`() {
        val address = "localhost:12345"
        
        // 记录失败
        assertFalse(blocklist.recordFailure(address))
        assertEquals(1, blocklist.getFailureCount(address))
        
        // 重置失败计数
        blocklist.resetFailureCount(address)
        
        // 验证失败计数已重置
        assertEquals(0, blocklist.getFailureCount(address))
    }
    
    @Test
    fun `should unblock and reset failure count`() {
        val address = "localhost:12345"
        
        // 记录足够的失败以阻止地址
        assertFalse(blocklist.recordFailure(address))
        assertFalse(blocklist.recordFailure(address))
        assertTrue(blocklist.recordFailure(address))
        
        // 验证地址被阻止
        assertTrue(blocklist.isBlocked(address))
        
        // 解除阻止
        assertTrue(blocklist.unblock(address))
        
        // 验证地址未被阻止且失败计数已重置
        assertFalse(blocklist.isBlocked(address))
        assertEquals(0, blocklist.getFailureCount(address))
    }
}
