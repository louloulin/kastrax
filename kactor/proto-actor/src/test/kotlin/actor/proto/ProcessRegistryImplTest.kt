package actor.proto

import actor.proto.mailbox.SystemMessage
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class ProcessRegistryImplTest {

    @Test
    fun `should register and retrieve processes`() {
        val system = ActorSystem("test")
        val registry = system.processRegistry()

        val process = object : Process() {
            override fun sendUserMessage(pid: PID, message: Any) {}
            override fun sendSystemMessage(pid: PID, message: SystemMessage) {}
        }

        val pid = registry.put("test-process", process)
        assertEquals("test-process", pid.id)

        val retrievedProcess = registry.get("test-process")
        assertSame(process, retrievedProcess)

        val retrievedByPid = registry.get(pid)
        assertSame(process, retrievedByPid)
    }

    @Test
    fun `should handle duplicate registration`() {
        val system = ActorSystem("test")
        val registry = system.processRegistry()

        // 清理之前的测试可能留下的进程
        try {
            val existingPid = PID(registry.address, "test-process-dup")
            registry.remove(existingPid)
        } catch (e: Exception) {
            // 忽略异常
        }

        val process1 = object : Process() {
            override fun sendUserMessage(pid: PID, message: Any) {}
            override fun sendSystemMessage(pid: PID, message: SystemMessage) {}
        }

        val process2 = object : Process() {
            override fun sendUserMessage(pid: PID, message: Any) {}
            override fun sendSystemMessage(pid: PID, message: SystemMessage) {}
        }

        registry.put("test-process-dup", process1)

        try {
            registry.put("test-process-dup", process2)
            fail("Expected ProcessNameExistException")
        } catch (e: ProcessNameExistException) {
            // Expected
        }

        val retrievedProcess = registry.get("test-process-dup")
        assertSame(process1, retrievedProcess, "Should retrieve the first process")
    }

    @Test
    fun `should remove processes`() {
        val system = ActorSystem("test")
        val registry = system.processRegistry()

        // 清理之前的测试可能留下的进程
        try {
            val existingPid = PID(registry.address, "test-process-remove")
            registry.remove(existingPid)
        } catch (e: Exception) {
            // 忽略异常
        }

        val process = object : Process() {
            override fun sendUserMessage(pid: PID, message: Any) {}
            override fun sendSystemMessage(pid: PID, message: SystemMessage) {}
        }

        val pid = registry.put("test-process-remove", process)
        registry.remove(pid)

        val retrievedProcess = registry.get("test-process-remove")
        assertSame(system.deadLetter, retrievedProcess, "Should return dead letter after removal")
    }

    @Test
    fun `should handle concurrent registrations`() {
        val system = ActorSystem("test")
        val registry = system.processRegistry()

        val threadCount = 10
        val iterationsPerThread = 100
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        for (i in 0 until threadCount) {
            executor.submit {
                try {
                    for (j in 0 until iterationsPerThread) {
                        val process = object : Process() {
                            override fun sendUserMessage(pid: PID, message: Any) {}
                            override fun sendSystemMessage(pid: PID, message: SystemMessage) {}
                        }

                        val id = "concurrent-${i}-${j}"
                        try {
                            registry.put(id, process)
                            successCount.incrementAndGet()
                        } catch (e: ProcessNameExistException) {
                            failCount.incrementAndGet()
                        }
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        assertEquals(threadCount * iterationsPerThread, successCount.get() + failCount.get())
        assertEquals(threadCount * iterationsPerThread, successCount.get(), "All registrations should succeed")
    }

    @Test
    fun `should generate unique IDs`() {
        val system = ActorSystem("test")
        val registry = system.processRegistry()

        val count = 1000
        val ids = mutableSetOf<String>()

        for (i in 0 until count) {
            val id = registry.nextId()
            assertFalse(ids.contains(id), "ID should be unique")
            ids.add(id)
        }

        assertEquals(count, ids.size, "Should generate the expected number of unique IDs")
    }

    @Test
    fun `should list all processes`() {
        val system = ActorSystem("test")
        val registry = system.processRegistry()

        // 清理之前的测试可能留下的进程
        for (i in 0 until 10) {
            try {
                val existingPid = PID(registry.address, "test-process-list-$i")
                registry.remove(existingPid)
            } catch (e: Exception) {
                // 忽略异常
            }
        }

        val count = 10
        val pids = mutableListOf<PID>()

        for (i in 0 until count) {
            val process = object : Process() {
                override fun sendUserMessage(pid: PID, message: Any) {}
                override fun sendSystemMessage(pid: PID, message: SystemMessage) {}
            }

            val id = "test-process-list-$i"
            val pid = registry.put(id, process)
            pids.add(pid)
        }

        val processes = registry.processes().toList()
        assertTrue(processes.size >= count, "Should list at least all registered processes")

        for (pid in pids) {
            assertTrue(processes.any { it.id == pid.id }, "Listed processes should contain all registered PIDs")
        }
    }

    @Test
    fun `should mark LocalProcess as dead when removed`() {
        val system = ActorSystem("test")
        val registry = system.processRegistry()

        val mailbox = actor.proto.mailbox.newUnboundedMailbox()
        val process = LocalProcess(mailbox)

        val pid = registry.put("test-process-dead", process)
        registry.remove(pid)

        assertTrue(process.isDead(), "Process should be marked as dead")
    }
}
