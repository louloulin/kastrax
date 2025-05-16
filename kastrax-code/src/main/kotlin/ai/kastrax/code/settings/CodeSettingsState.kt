package ai.kastrax.code.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/**
 * 代码设置状态
 * 
 * 存储 KastraX Code 的设置
 */
@Service(Service.Level.APP)
@State(
    name = "ai.kastrax.code.settings.CodeSettingsState",
    storages = [Storage("kastraxCodeSettings.xml")]
)
class CodeSettingsState : PersistentStateComponent<CodeSettingsState.State> {
    
    /**
     * 设置状态
     */
    data class State(
        var model: String = "deepseek-coder",
        var apiKey: String = "",
        var maxTokens: Int = 2000,
        var temperature: Double = 0.3,
        var enableCompletion: Boolean = true,
        var enableContext: Boolean = true
    )
    
    private var myState = State()
    
    /**
     * 获取状态
     */
    override fun getState(): State {
        return myState
    }
    
    /**
     * 加载状态
     */
    override fun loadState(state: State) {
        myState = state
    }
    
    /**
     * 模型
     */
    var model: String
        get() = myState.model
        set(value) {
            myState.model = value
        }
    
    /**
     * API 密钥
     */
    var apiKey: String
        get() = myState.apiKey
        set(value) {
            myState.apiKey = value
        }
    
    /**
     * 最大令牌数
     */
    var maxTokens: Int
        get() = myState.maxTokens
        set(value) {
            myState.maxTokens = value
        }
    
    /**
     * 温度
     */
    var temperature: Double
        get() = myState.temperature
        set(value) {
            myState.temperature = value
        }
    
    /**
     * 是否启用代码补全
     */
    var enableCompletion: Boolean
        get() = myState.enableCompletion
        set(value) {
            myState.enableCompletion = value
        }
    
    /**
     * 是否启用上下文感知
     */
    var enableContext: Boolean
        get() = myState.enableContext
        set(value) {
            myState.enableContext = value
        }
    
    companion object {
        /**
         * 获取实例
         */
        fun getInstance(): CodeSettingsState {
            return ApplicationManager.getApplication().getService(CodeSettingsState::class.java)
        }
    }
}
