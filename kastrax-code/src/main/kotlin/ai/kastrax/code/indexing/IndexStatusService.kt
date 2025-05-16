package ai.kastrax.code.indexing

import ai.kastrax.code.common.KastraXCodeBase
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 索引状态服务
 *
 * 提供索引状态和进度信息
 */
@Service(Service.Level.PROJECT)
class IndexStatusService(private val project: Project) : KastraXCodeBase(component = "INDEX_STATUS_SERVICE") {
    
    override val logger = KotlinLogging.logger {}
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // 索引状态流
    private val _indexStatus = MutableStateFlow<IndexStatusInfo>(IndexStatusInfo.Idle)
    val indexStatus: StateFlow<IndexStatusInfo> = _indexStatus.asStateFlow()
    
    // 索引管理器
    private val indexManager by lazy { CodeIndexManager.getInstance(project) }
    
    init {
        // 监听索引事件
        scope.launch {
            indexManager.indexEvents.collect { event ->
                handleIndexEvent(event)
            }
        }
    }
    
    /**
     * 处理索引事件
     *
     * @param event 索引事件
     */
    private fun handleIndexEvent(event: IndexEvent) {
        when (event) {
            is IndexEvent.StatusChanged -> {
                when (event.status) {
                    IndexStatus.IDLE -> _indexStatus.value = IndexStatusInfo.Idle
                    IndexStatus.INDEXING -> _indexStatus.value = IndexStatusInfo.Indexing(0f)
                    IndexStatus.READY -> _indexStatus.value = IndexStatusInfo.Ready
                    IndexStatus.ERROR -> _indexStatus.value = IndexStatusInfo.Error("索引出错")
                }
            }
            is IndexEvent.ProgressUpdated -> {
                val currentStatus = _indexStatus.value
                if (currentStatus is IndexStatusInfo.Indexing) {
                    _indexStatus.value = IndexStatusInfo.Indexing(event.progress)
                }
            }
            is IndexEvent.Error -> {
                _indexStatus.value = IndexStatusInfo.Error(event.message)
            }
        }
    }
    
    /**
     * 启动索引
     */
    fun startIndexing() {
        indexManager.start()
    }
    
    /**
     * 停止索引
     */
    fun stopIndexing() {
        indexManager.stop()
    }
    
    /**
     * 重新索引
     */
    fun reindex() {
        indexManager.requestReindex()
    }
    
    companion object {
        /**
         * 获取项目的索引状态服务实例
         *
         * @param project 项目
         * @return 索引状态服务实例
         */
        fun getInstance(project: Project): IndexStatusService {
            return project.service<IndexStatusService>()
        }
    }
}

/**
 * 索引状态信息
 */
sealed class IndexStatusInfo {
    /**
     * 空闲状态
     */
    object Idle : IndexStatusInfo()
    
    /**
     * 正在索引
     *
     * @property progress 进度（0-1）
     */
    data class Indexing(val progress: Float) : IndexStatusInfo()
    
    /**
     * 索引就绪
     */
    object Ready : IndexStatusInfo()
    
    /**
     * 索引错误
     *
     * @property message 错误消息
     */
    data class Error(val message: String) : IndexStatusInfo()
}
