package actor.proto.metrics

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*

class MetricsTest {
    private lateinit var registry: MetricsRegistry
    
    @BeforeEach
    fun setup() {
        registry = MetricsRegistry()
    }
    
    @Test
    fun `counter should increment correctly`() {
        val counter = registry.counter("test_counter") as DefaultCounter
        
        counter.inc()
        assertEquals(1, counter.value())
        
        counter.inc(2)
        assertEquals(3, counter.value())
    }
    
    @Test
    fun `counter with tags should increment correctly`() {
        val counter = registry.counter("test_counter", mapOf("tag1" to "value1")) as DefaultCounter
        
        counter.inc()
        assertEquals(1, counter.value())
        
        counter.inc(2)
        assertEquals(3, counter.value())
    }
    
    @Test
    fun `gauge should set value correctly`() {
        val gauge = registry.gauge("test_gauge") as DefaultGauge
        
        gauge.set(42.0)
        assertEquals(42.0, gauge.value())
        
        gauge.set(123.0)
        assertEquals(123.0, gauge.value())
    }
    
    @Test
    fun `gauge with tags should set value correctly`() {
        val gauge = registry.gauge("test_gauge", mapOf("tag1" to "value1")) as DefaultGauge
        
        gauge.set(42.0)
        assertEquals(42.0, gauge.value())
        
        gauge.set(123.0)
        assertEquals(123.0, gauge.value())
    }
    
    @Test
    fun `histogram should record observations correctly`() {
        val histogram = registry.histogram("test_histogram") as DefaultHistogram
        
        histogram.observe(1.0)
        histogram.observe(2.0)
        histogram.observe(3.0)
        
        assertEquals(3, histogram.count())
        assertEquals(6.0, histogram.sum())
        
        val values = histogram.values()
        assertEquals(3, values.size)
        assertEquals(1, values[1.0])
        assertEquals(1, values[2.0])
        assertEquals(1, values[3.0])
    }
    
    @Test
    fun `histogram with tags should record observations correctly`() {
        val histogram = registry.histogram("test_histogram", mapOf("tag1" to "value1")) as DefaultHistogram
        
        histogram.observe(1.0)
        histogram.observe(2.0)
        histogram.observe(3.0)
        
        assertEquals(3, histogram.count())
        assertEquals(6.0, histogram.sum())
        
        val values = histogram.values()
        assertEquals(3, values.size)
        assertEquals(1, values[1.0])
        assertEquals(1, values[2.0])
        assertEquals(1, values[3.0])
    }
    
    @Test
    fun `registry should return same metric instance for same name and tags`() {
        val counter1 = registry.counter("test_counter", mapOf("tag1" to "value1"))
        val counter2 = registry.counter("test_counter", mapOf("tag1" to "value1"))
        
        assertSame(counter1, counter2)
        
        val gauge1 = registry.gauge("test_gauge", mapOf("tag1" to "value1"))
        val gauge2 = registry.gauge("test_gauge", mapOf("tag1" to "value1"))
        
        assertSame(gauge1, gauge2)
        
        val histogram1 = registry.histogram("test_histogram", mapOf("tag1" to "value1"))
        val histogram2 = registry.histogram("test_histogram", mapOf("tag1" to "value1"))
        
        assertSame(histogram1, histogram2)
    }
    
    @Test
    fun `registry should return different metric instances for different tags`() {
        val counter1 = registry.counter("test_counter", mapOf("tag1" to "value1"))
        val counter2 = registry.counter("test_counter", mapOf("tag1" to "value2"))
        
        assertNotSame(counter1, counter2)
        
        val gauge1 = registry.gauge("test_gauge", mapOf("tag1" to "value1"))
        val gauge2 = registry.gauge("test_gauge", mapOf("tag1" to "value2"))
        
        assertNotSame(gauge1, gauge2)
        
        val histogram1 = registry.histogram("test_histogram", mapOf("tag1" to "value1"))
        val histogram2 = registry.histogram("test_histogram", mapOf("tag1" to "value2"))
        
        assertNotSame(histogram1, histogram2)
    }
    
    @Test
    fun `registry should clear all metrics`() {
        registry.counter("test_counter")
        registry.gauge("test_gauge")
        registry.histogram("test_histogram")
        
        assertFalse(registry.getCounters().isEmpty())
        assertFalse(registry.getGauges().isEmpty())
        assertFalse(registry.getHistograms().isEmpty())
        
        registry.clear()
        
        assertTrue(registry.getCounters().isEmpty())
        assertTrue(registry.getGauges().isEmpty())
        assertTrue(registry.getHistograms().isEmpty())
    }
}
