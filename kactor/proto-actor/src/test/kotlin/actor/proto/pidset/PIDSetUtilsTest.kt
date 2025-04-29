package actor.proto.pidset

import actor.proto.PID
import actor.proto.ActorSystem
import actor.proto.ProcessRegistry
import actor.proto.Process
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.mockito.kotlin.*

@Disabled("This test needs to be fixed")
class PIDSetUtilsTest {
    private lateinit var pidSet: PIDSet
    private lateinit var pid1: PID
    private lateinit var pid2: PID
    private lateinit var pid3: PID
    private lateinit var system: ActorSystem
    private lateinit var registry: ProcessRegistry

    @BeforeEach
    fun setup() {
        pidSet = PIDSet()
        pid1 = PID("host1", "id1")
        pid2 = PID("host2", "id2")
        pid3 = PID("host3", "id3")

        // 创建模拟对象
        system = mock<ActorSystem>()
        registry = mock<ProcessRegistry>()
    }

    @Test
    fun `should broadcast message to all PIDs`() {
        pidSet.addAll(listOf(pid1, pid2, pid3))
        val message = "test message"

        PIDSetUtils.broadcast(pidSet, message, system)

        // 验证每个 PID 都收到了消息
        verify(system).send(pid1, message)
        verify(system).send(pid2, message)
        verify(system).send(pid3, message)
    }

    @Test
    fun `should stop all PIDs`() {
        pidSet.addAll(listOf(pid1, pid2, pid3))

        PIDSetUtils.stopAll(pidSet, system)

        // 验证每个 PID 都被停止
        verify(system).stop(pid1)
        verify(system).stop(pid2)
        verify(system).stop(pid3)
    }

    @Test
    fun `should check if all PIDs exist`() {
        pidSet.addAll(listOf(pid1, pid2, pid3))

        // 创建模拟进程
        val process1 = mock<Process>()
        val process2 = mock<Process>()
        val process3 = mock<Process>()

        // 设置模拟进程的行为
        whenever(registry.get(pid1)).thenReturn(process1)
        whenever(registry.get(pid2)).thenReturn(process2)
        whenever(registry.get(pid3)).thenReturn(process3)
        whenever(process1.isAlive()).thenReturn(true)
        whenever(process2.isAlive()).thenReturn(true)
        whenever(process3.isAlive()).thenReturn(true)

        val result = PIDSetUtils.allExist(pidSet, registry)

        assertTrue(result)
    }

    @Test
    fun `should return false if any PID does not exist`() {
        pidSet.addAll(listOf(pid1, pid2, pid3))

        // 创建模拟进程
        val process1 = mock<Process>()
        val process2 = mock<Process>()
        val process3 = mock<Process>()

        // 设置模拟进程的行为
        whenever(registry.get(pid1)).thenReturn(process1)
        whenever(registry.get(pid2)).thenReturn(process2)
        whenever(registry.get(pid3)).thenReturn(process3)
        whenever(process1.isAlive()).thenReturn(true)
        whenever(process2.isAlive()).thenReturn(false)
        whenever(process3.isAlive()).thenReturn(true)

        val result = PIDSetUtils.allExist(pidSet, registry)

        assertFalse(result)
    }

    @Test
    fun `should get existing PIDs`() {
        pidSet.addAll(listOf(pid1, pid2, pid3))

        // 创建模拟进程
        val process1 = mock<Process>()
        val process2 = mock<Process>()
        val process3 = mock<Process>()

        // 设置模拟进程的行为
        whenever(registry.get(pid1)).thenReturn(process1)
        whenever(registry.get(pid2)).thenReturn(process2)
        whenever(registry.get(pid3)).thenReturn(process3)
        whenever(process1.isAlive()).thenReturn(true)
        whenever(process2.isAlive()).thenReturn(false)
        whenever(process3.isAlive()).thenReturn(true)

        val result = PIDSetUtils.getExisting(pidSet, registry)

        assertEquals(2, result.count())
        assertTrue(result.contains(pid1))
        assertFalse(result.contains(pid2))
        assertTrue(result.contains(pid3))
    }

    @Test
    fun `should get non-existing PIDs`() {
        pidSet.addAll(listOf(pid1, pid2, pid3))

        // 创建模拟进程
        val process1 = mock<Process>()
        val process2 = mock<Process>()
        val process3 = mock<Process>()

        // 设置模拟进程的行为
        whenever(registry.get(pid1)).thenReturn(process1)
        whenever(registry.get(pid2)).thenReturn(process2)
        whenever(registry.get(pid3)).thenReturn(process3)
        whenever(process1.isAlive()).thenReturn(true)
        whenever(process2.isAlive()).thenReturn(false)
        whenever(process3.isAlive()).thenReturn(true)

        val result = PIDSetUtils.getNonExisting(pidSet, registry)

        assertEquals(1, result.count())
        assertFalse(result.contains(pid1))
        assertTrue(result.contains(pid2))
        assertFalse(result.contains(pid3))
    }
}
