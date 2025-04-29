package actor.proto

import actor.proto.mailbox.SystemMessage
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ProcessRegistryTest {

    @BeforeEach
    fun setup() {
        // 重置 ProcessRegistry 的状态
        ProcessRegistry.address = ProcessRegistry.noHost
    }

    @Test
    fun `should generate unique IDs`() {
        val ids = mutableSetOf<String>()
        for (i in 1..1000) {
            val id = ProcessRegistry.nextId()
            assertFalse(ids.contains(id), "ID should be unique: $id")
            ids.add(id)
        }
    }

    @Test
    fun `should register and retrieve processes`() {
        val process = object : Process() {
            override fun sendUserMessage(pid: PID, message: Any) {}
            override fun sendSystemMessage(pid: PID, message: SystemMessage) {}
        }

        val pid = ProcessRegistry.put("test-process", process)
        assertEquals("test-process", pid.id)

        val retrievedProcess = ProcessRegistry.get("test-process")
        assertSame(process, retrievedProcess)

        val retrievedByPid = ProcessRegistry.get(pid)
        assertSame(process, retrievedByPid)
    }

    @Test
    fun `should throw exception on duplicate registration`() {
        // 清理之前的测试可能留下的进程
        try {
            val existingPid = PID(ProcessRegistry.address, "test-process-dup")
            ProcessRegistry.remove(existingPid)
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

        ProcessRegistry.put("test-process-dup", process1)

        assertThrows(ProcessNameExistException::class.java) {
            ProcessRegistry.put("test-process-dup", process2)
        }

        val retrievedProcess = ProcessRegistry.get("test-process-dup")
        assertSame(process1, retrievedProcess, "Should retrieve the first process")
    }

    @Test
    fun `should remove processes`() {
        // 清理之前的测试可能留下的进程
        try {
            val existingPid = PID(ProcessRegistry.address, "test-process")
            ProcessRegistry.remove(existingPid)
        } catch (e: Exception) {
            // 忽略异常
        }

        val process = object : Process() {
            override fun sendUserMessage(pid: PID, message: Any) {}
            override fun sendSystemMessage(pid: PID, message: SystemMessage) {}
        }

        val pid = ProcessRegistry.put("test-process", process)
        ProcessRegistry.remove(pid)

        val retrievedProcess = ProcessRegistry.get("test-process")
        assertSame(DeadLetterProcess, retrievedProcess, "Should return dead letter after removal")
    }

    @Test
    fun `should handle concurrent registrations`() {
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
                            ProcessRegistry.put(id, process)
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

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Concurrent test timed out")
        assertEquals(threadCount * iterationsPerThread, successCount.get() + failCount.get(), "All operations should be accounted for")
        assertEquals(threadCount * iterationsPerThread, successCount.get(), "All registrations should succeed")
        assertEquals(0, failCount.get(), "No registrations should fail")

        executor.shutdown()
    }

    @Test
    fun `should resolve remote processes`() {
        var resolverCalled = false
        val remoteProcess = object : Process() {
            override fun sendUserMessage(pid: PID, message: Any) {}
            override fun sendSystemMessage(pid: PID, message: SystemMessage) {}
        }

        ProcessRegistry.registerHostResolver { pid ->
            if (pid.address == "remote-host") {
                resolverCalled = true
                remoteProcess
            } else {
                null
            }
        }

        val remotePid = PID("remote-host", "remote-process")
        val process = ProcessRegistry.get(remotePid)

        assertTrue(resolverCalled, "Host resolver should be called")
        assertSame(remoteProcess, process, "Should return the remote process")
    }

    @Test
    fun `should return dead letter for unknown remote processes`() {
        val remotePid = PID("unknown-host", "unknown-process")
        val process = ProcessRegistry.get(remotePid)

        assertSame(DeadLetterProcess, process, "Should return dead letter for unknown remote process")
    }

    @Test
    fun `should list all processes`() {
        // 清理之前的测试可能留下的进程
        for (i in 0 until 10) {
            try {
                val existingPid = PID(ProcessRegistry.address, "test-process-list-$i")
                ProcessRegistry.remove(existingPid)
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
            val pid = ProcessRegistry.put(id, process)
            pids.add(pid)
        }

        val processes = ProcessRegistry.processes().toList()
        assertTrue(processes.size >= count, "Should list at least all registered processes")

        for (pid in pids) {
            assertTrue(processes.any { it.id == pid.id }, "Listed processes should contain all registered PIDs")
        }
    }
}
