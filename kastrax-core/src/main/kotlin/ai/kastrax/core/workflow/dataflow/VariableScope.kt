package ai.kastrax.core.workflow.dataflow

import ai.kastrax.core.workflow.WorkflowContext
import kotlinx.serialization.Serializable

/**
 * 变量作用域枚举。
 */
enum class ScopeType {
    /**
     * 全局作用域，变量在整个工作流中可见。
     */
    GLOBAL,

    /**
     * 工作流作用域，变量在当前工作流中可见。
     */
    WORKFLOW,

    /**
     * 步骤作用域，变量仅在当前步骤中可见。
     */
    STEP
}

/**
 * 变量作用域接口。
 */
interface VariableScope {
    /**
     * 作用域类型。
     */
    val type: ScopeType

    /**
     * 获取变量值。
     *
     * @param name 变量名
     * @return 变量值，如果不存在则返回null
     */
    fun get(name: String): Any?

    /**
     * 设置变量值。
     *
     * @param name 变量名
     * @param value 变量值
     */
    fun set(name: String, value: Any?)

    /**
     * 检查变量是否存在。
     *
     * @param name 变量名
     * @return 是否存在
     */
    fun contains(name: String): Boolean

    /**
     * 获取所有变量。
     *
     * @return 所有变量的映射
     */
    fun getAll(): Map<String, Any?>
}

/**
 * 全局变量作用域。
 *
 * @property variables 变量映射
 */
class GlobalScope(
    private val variables: MutableMap<String, Any?> = mutableMapOf()
) : VariableScope {
    override val type = ScopeType.GLOBAL

    override fun get(name: String): Any? = variables[name]

    override fun set(name: String, value: Any?) {
        variables[name] = value
    }

    override fun contains(name: String): Boolean = variables.containsKey(name)

    override fun getAll(): Map<String, Any?> = variables.toMap()
}

/**
 * 工作流变量作用域。
 *
 * @property workflowId 工作流ID
 * @property variables 变量映射
 * @property parent 父作用域
 */
class WorkflowScope(
    val workflowId: String,
    private val variables: MutableMap<String, Any?> = mutableMapOf(),
    private val parent: VariableScope? = null
) : VariableScope {
    override val type = ScopeType.WORKFLOW

    override fun get(name: String): Any? {
        return variables[name] ?: parent?.get(name)
    }

    override fun set(name: String, value: Any?) {
        variables[name] = value
    }

    override fun contains(name: String): Boolean {
        return variables.containsKey(name) || (parent?.contains(name) ?: false)
    }

    override fun getAll(): Map<String, Any?> {
        val result = parent?.getAll()?.toMutableMap() ?: mutableMapOf()
        result.putAll(variables)
        return result
    }
}

/**
 * 步骤变量作用域。
 *
 * @property stepId 步骤ID
 * @property variables 变量映射
 * @property parent 父作用域
 */
class StepScope(
    val stepId: String,
    private val variables: MutableMap<String, Any?> = mutableMapOf(),
    private val parent: VariableScope? = null
) : VariableScope {
    override val type = ScopeType.STEP

    override fun get(name: String): Any? {
        return variables[name] ?: parent?.get(name)
    }

    override fun set(name: String, value: Any?) {
        variables[name] = value
    }

    override fun contains(name: String): Boolean {
        return variables.containsKey(name) || (parent?.contains(name) ?: false)
    }

    override fun getAll(): Map<String, Any?> {
        val result = parent?.getAll()?.toMutableMap() ?: mutableMapOf()
        result.putAll(variables)
        return result
    }
}

/**
 * 变量作用域管理器。
 */
class VariableScopeManager {
    private val globalScope = GlobalScope()
    private val workflowScopes = mutableMapOf<String, WorkflowScope>()
    private val stepScopes = mutableMapOf<String, StepScope>()

    /**
     * 获取全局作用域。
     *
     * @return 全局作用域
     */
    fun getGlobalScope(): VariableScope = globalScope

    /**
     * 获取工作流作用域。
     *
     * @param workflowId 工作流ID
     * @return 工作流作用域
     */
    fun getWorkflowScope(workflowId: String): VariableScope {
        return workflowScopes.getOrPut(workflowId) {
            WorkflowScope(workflowId, parent = globalScope)
        }
    }

    /**
     * 获取步骤作用域。
     *
     * @param workflowId 工作流ID
     * @param stepId 步骤ID
     * @return 步骤作用域
     */
    fun getStepScope(workflowId: String, stepId: String): VariableScope {
        val workflowScope = getWorkflowScope(workflowId)
        return stepScopes.getOrPut("$workflowId:$stepId") {
            StepScope(stepId, parent = workflowScope)
        }
    }

    /**
     * 获取变量值。
     *
     * @param name 变量名
     * @param scope 作用域
     * @return 变量值
     */
    fun getVariable(name: String, scope: VariableScope): Any? {
        return scope.get(name)
    }

    /**
     * 设置变量值。
     *
     * @param name 变量名
     * @param value 变量值
     * @param scope 作用域
     */
    fun setVariable(name: String, value: Any?, scope: VariableScope) {
        scope.set(name, value)
    }

    /**
     * 清除所有作用域。
     */
    fun clear() {
        workflowScopes.clear()
        stepScopes.clear()
    }
}
