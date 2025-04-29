package actor.proto.throttle

import actor.proto.logging.Logger
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration

/**
 * 阀门状态，表示节流器的当前状态
 */
enum class Valve {
    /**
     * 开放状态，允许消息通过
     */
    OPEN,
    
    /**
     * 关闭中状态，表示即将关闭
     */
    CLOSING,
    
    /**
     * 关闭状态，不允许消息通过
     */
    CLOSED
}

/**
 * 节流器函数类型，调用时返回当前阀门状态
 */
typealias ShouldThrottle = () -> Valve

/**
 * 创建一个新的节流器
 * 
 * 注意：此实现不保证节流器在周期结束后立即打开，因为它是异步重置的
 * 优先考虑了吞吐量而不是精确的重新打开时间
 * 
 * @param maxEventsInPeriod 在一个周期内允许的最大事件数
 * @param period 重置周期
 * @param throttledCallback 当周期结束时，如果有被节流的事件，将调用此回调函数，参数为被节流的事件数
 * @return 节流器函数，调用时返回当前阀门状态
 */
fun newThrottle(
    maxEventsInPeriod: Int,
    period: Duration,
    throttledCallback: (Int) -> Unit
): ShouldThrottle {
    val currentEvents = AtomicInteger(0)
    
    fun startTimer(duration: Duration) {
        CoroutineScope(Dispatchers.Default).launch {
            delay(duration.toMillis())
            
            val timesCalled = currentEvents.getAndSet(0)
            if (timesCalled > maxEventsInPeriod) {
                throttledCallback(timesCalled - maxEventsInPeriod)
            }
        }
    }
    
    return {
        val tries = currentEvents.incrementAndGet()
        if (tries == 1) {
            startTimer(period)
        }
        
        when {
            tries == maxEventsInPeriod -> Valve.CLOSING
            tries > maxEventsInPeriod -> Valve.CLOSED
            else -> Valve.OPEN
        }
    }
}

/**
 * 创建一个带有日志记录的节流器
 * 
 * @param logger 用于记录日志的Logger实例
 * @param maxEventsInPeriod 在一个周期内允许的最大事件数
 * @param period 重置周期
 * @param throttledCallback 当周期结束时，如果有被节流的事件，将调用此回调函数，参数为Logger和被节流的事件数
 * @return 节流器函数，调用时返回当前阀门状态
 */
fun newThrottleWithLogger(
    logger: Logger,
    maxEventsInPeriod: Int,
    period: Duration,
    throttledCallback: (Logger, Int) -> Unit
): ShouldThrottle {
    val currentEvents = AtomicInteger(0)
    
    fun startTimer(duration: Duration) {
        CoroutineScope(Dispatchers.Default).launch {
            delay(duration.toMillis())
            
            val timesCalled = currentEvents.getAndSet(0)
            if (timesCalled > maxEventsInPeriod) {
                throttledCallback(logger, timesCalled - maxEventsInPeriod)
            }
        }
    }
    
    return {
        val tries = currentEvents.incrementAndGet()
        if (tries == 1) {
            startTimer(period)
        }
        
        when {
            tries == maxEventsInPeriod -> Valve.CLOSING
            tries > maxEventsInPeriod -> Valve.CLOSED
            else -> Valve.OPEN
        }
    }
}
