package ai.kastrax.server.quarkus.repository

import ai.kastrax.server.common.model.Workflow
import jakarta.enterprise.context.ApplicationScoped
import java.util.concurrent.ConcurrentHashMap

/**
 * 工作流存储库接口
 */
interface IWorkflowRepository {
    /**
     * 保存工作流
     */
    fun save(workflow: Workflow): Workflow

    /**
     * 根据ID查找工作流
     */
    fun findById(id: String): Workflow?

    /**
     * 删除工作流
     */
    fun deleteById(id: String): Boolean

    /**
     * 查找所有工作流
     */
    fun findAll(page: Int, size: Int, filter: Map<String, String>): List<Workflow>
}

/**
 * 工作流存储库实现
 */
@ApplicationScoped
class WorkflowRepository : IWorkflowRepository {
    private val workflows = ConcurrentHashMap<String, Workflow>()

    /**
     * 保存工作流
     */
    override fun save(workflow: Workflow): Workflow {
        workflows[workflow.id] = workflow
        return workflow
    }

    /**
     * 根据ID查找工作流
     */
    override fun findById(id: String): Workflow? {
        return workflows[id]
    }

    /**
     * 删除工作流
     */
    override fun deleteById(id: String): Boolean {
        return workflows.remove(id) != null
    }

    /**
     * 查找所有工作流
     */
    override fun findAll(page: Int, size: Int, filter: Map<String, String>): List<Workflow> {
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
