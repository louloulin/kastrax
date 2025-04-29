package actor.proto.pidset

import actor.proto.PID
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*

class PIDSetExtensionsTest {
    private lateinit var pid1: PID
    private lateinit var pid2: PID
    private lateinit var pid3: PID
    
    @BeforeEach
    fun setup() {
        pid1 = PID("host1", "id1")
        pid2 = PID("host2", "id2")
        pid3 = PID("host3", "id3")
    }
    
    @Test
    fun `should convert collection to PIDSet`() {
        val collection = listOf(pid1, pid2, pid3)
        
        val pidSet = collection.toPIDSet()
        
        assertEquals(3, pidSet.count())
        assertTrue(pidSet.contains(pid1))
        assertTrue(pidSet.contains(pid2))
        assertTrue(pidSet.contains(pid3))
    }
    
    @Test
    fun `should convert array to PIDSet`() {
        val array = arrayOf(pid1, pid2, pid3)
        
        val pidSet = array.toPIDSet()
        
        assertEquals(3, pidSet.count())
        assertTrue(pidSet.contains(pid1))
        assertTrue(pidSet.contains(pid2))
        assertTrue(pidSet.contains(pid3))
    }
    
    @Test
    fun `should add PID to PIDSet using plus operator`() {
        val pidSet = PIDSet.of(pid1, pid2)
        
        val result = pidSet + pid3
        
        assertEquals(3, result.count())
        assertTrue(result.contains(pid1))
        assertTrue(result.contains(pid2))
        assertTrue(result.contains(pid3))
        
        // 原始集合不应该被修改
        assertEquals(2, pidSet.count())
    }
    
    @Test
    fun `should add PIDSet to PIDSet using plus operator`() {
        val pidSet1 = PIDSet.of(pid1)
        val pidSet2 = PIDSet.of(pid2, pid3)
        
        val result = pidSet1 + pidSet2
        
        assertEquals(3, result.count())
        assertTrue(result.contains(pid1))
        assertTrue(result.contains(pid2))
        assertTrue(result.contains(pid3))
        
        // 原始集合不应该被修改
        assertEquals(1, pidSet1.count())
        assertEquals(2, pidSet2.count())
    }
    
    @Test
    fun `should remove PID from PIDSet using minus operator`() {
        val pidSet = PIDSet.of(pid1, pid2, pid3)
        
        val result = pidSet - pid2
        
        assertEquals(2, result.count())
        assertTrue(result.contains(pid1))
        assertFalse(result.contains(pid2))
        assertTrue(result.contains(pid3))
        
        // 原始集合不应该被修改
        assertEquals(3, pidSet.count())
    }
    
    @Test
    fun `should remove PIDSet from PIDSet using minus operator`() {
        val pidSet1 = PIDSet.of(pid1, pid2, pid3)
        val pidSet2 = PIDSet.of(pid2, pid3)
        
        val result = pidSet1 - pidSet2
        
        assertEquals(1, result.count())
        assertTrue(result.contains(pid1))
        assertFalse(result.contains(pid2))
        assertFalse(result.contains(pid3))
        
        // 原始集合不应该被修改
        assertEquals(3, pidSet1.count())
        assertEquals(2, pidSet2.count())
    }
    
    @Test
    fun `should check if PID is in PIDSet using contains operator`() {
        val pidSet = PIDSet.of(pid1, pid2)
        
        assertTrue(pid1 in pidSet)
        assertTrue(pid2 in pidSet)
        assertFalse(pid3 in pidSet)
    }
    
    @Test
    fun `should get size of PIDSet using size property`() {
        val pidSet = PIDSet.of(pid1, pid2, pid3)
        
        assertEquals(3, pidSet.size)
    }
    
    @Test
    fun `should iterate over PIDSet using iterator`() {
        val pidSet = PIDSet.of(pid1, pid2, pid3)
        
        val pids = mutableListOf<PID>()
        for (pid in pidSet) {
            pids.add(pid)
        }
        
        assertEquals(3, pids.size)
        assertTrue(pids.contains(pid1))
        assertTrue(pids.contains(pid2))
        assertTrue(pids.contains(pid3))
    }
}
