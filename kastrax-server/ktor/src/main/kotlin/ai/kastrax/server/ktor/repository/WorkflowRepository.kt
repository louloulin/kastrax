package ai.kastrax.server.ktor.repository

import ai.kastrax.server.common.model.Workflow
import java.util.concurrent.ConcurrentHashMap

/**
 * 工作流存储库
 */
class WorkflowRepository {
    private val workflows = ConcurrentHashMap<String, Workflow>()
    
    /**
     * 保存工作流
     */
    fun save(workflow: Workflow): Workflow {
        workflows[workflow.id] = workflow
        return workflow
    }
    
    /**
     * 根据ID查找工作流
     */
    fun findById(id: String): Workflow? {
        return workflows[id]
    }
    
    /**
     * 删除工作流
     */
    fun deleteById(id: String): Boolean {
        return workflows.remove(id) != null
    }
    
    /**
     * 查找所有工作流
     */
    fun findAll(page: Int, size: Int, filter: Map<String, String>): List<Workflow> {
        return workflows.values
            .filter { workflow ->
                filter.all { (key, value) ->
                    when (key) {
                        "name" -> workflow.name.contains(value, ignoreCase = true)
                        "description" -> workflow.description.contains(value, ignoreCase = true)
                        "version" -> workflow.version == value
                        else -> true
                    }
                }
            }
            .sortedByDescending { it.updatedAt }
            .drop(page * size)
            .take(size)
            .toList()
    }
}
