package ai.kastrax.codex.tools

import ai.kastrax.core.tools.Tool
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import git4idea.GitUtil
import git4idea.changes.GitChangeUtils
import git4idea.repo.GitRepository
import kotlinx.serialization.json.*

/**
 * Git操作工具，用于执行Git操作
 */
class GitOperationTool(private val project: Project) {
    
    private val logger = Logger.getInstance(GitOperationTool::class.java)
    
    /**
     * 创建Git操作工具
     */
    fun createTool(): Tool {
        return object : Tool {
            override val id: String = "gitOperation"
            override val name: String = "Git操作"
            override val description: String = "执行Git操作，如查看变更、生成提交信息等"
            
            override val inputSchema: JsonElement = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("operation") {
                        put("type", "string")
                        put("description", "Git操作类型：status（状态）、changes（变更）、history（历史）、generateCommitMessage（生成提交信息）")
                        putJsonArray("enum") {
                            add("status")
                            add("changes")
                            add("history")
                            add("generateCommitMessage")
                        }
                    }
                    putJsonObject("filePath") {
                        put("type", "string")
                        put("description", "文件路径（可选）")
                    }
                    putJsonObject("limit") {
                        put("type", "integer")
                        put("description", "最大结果数量")
                    }
                }
                putJsonArray("required") {
                    add("operation")
                }
            }
            
            override val outputSchema: JsonElement? = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("result") {
                        put("type", "string")
                        put("description", "操作结果")
                    }
                }
            }
            
            override suspend fun execute(input: JsonElement): JsonElement {
                try {
                    // 解析输入参数
                    val inputObj = input.jsonObject
                    val operation = inputObj["operation"]?.jsonPrimitive?.content
                        ?: throw IllegalArgumentException("操作类型不能为空")
                    val filePath = inputObj["filePath"]?.jsonPrimitive?.content
                    val limit = inputObj["limit"]?.jsonPrimitive?.int ?: 10
                    
                    // 获取Git仓库
                    val repositories = GitUtil.getRepositories(project)
                    if (repositories.isEmpty()) {
                        return buildJsonObject {
                            put("success", false)
                            put("error", "项目不是Git仓库")
                        }
                    }
                    
                    val repository = repositories[0]
                    
                    // 执行操作
                    val result = when (operation) {
                        "status" -> getGitStatus(repository)
                        "changes" -> getGitChanges(repository, filePath, limit)
                        "history" -> getGitHistory(repository, filePath, limit)
                        "generateCommitMessage" -> generateCommitMessage(repository)
                        else -> "不支持的操作类型: $operation"
                    }
                    
                    return buildJsonObject {
                        put("success", true)
                        put("result", result)
                    }
                } catch (e: Exception) {
                    logger.error("Git操作失败", e)
                    return buildJsonObject {
                        put("success", false)
                        put("error", e.message ?: "未知错误")
                    }
                }
            }
        }
    }
    
    /**
     * 获取Git状态
     */
    private fun getGitStatus(repository: GitRepository): String {
        val result = StringBuilder()
        
        // 更新Git状态
        repository.update()
        
        // 获取当前分支
        val currentBranch = repository.currentBranch
        result.append("当前分支: ${currentBranch?.name ?: "未知"}\n")
        
        // 获取远程分支
        val trackingBranch = currentBranch?.findTrackedBranch(repository)
        result.append("跟踪分支: ${trackingBranch?.name ?: "无"}\n\n")
        
        // 获取状态
        val status = repository.status
        result.append("状态:\n")
        
        // 未跟踪文件
        if (status.untracked.isNotEmpty()) {
            result.append("未跟踪文件: ${status.untracked.size}个\n")
            status.untracked.take(5).forEach { file ->
                result.append("- $file\n")
            }
            if (status.untracked.size > 5) {
                result.append("- ... 等${status.untracked.size - 5}个文件\n")
            }
            result.append("\n")
        }
        
        // 修改的文件
        if (status.changed.isNotEmpty()) {
            result.append("修改的文件: ${status.changed.size}个\n")
            status.changed.take(5).forEach { file ->
                result.append("- $file\n")
            }
            if (status.changed.size > 5) {
                result.append("- ... 等${status.changed.size - 5}个文件\n")
            }
            result.append("\n")
        }
        
        // 暂存的文件
        if (status.added.isNotEmpty()) {
            result.append("暂存的文件: ${status.added.size}个\n")
            status.added.take(5).forEach { file ->
                result.append("- $file\n")
            }
            if (status.added.size > 5) {
                result.append("- ... 等${status.added.size - 5}个文件\n")
            }
            result.append("\n")
        }
        
        return result.toString()
    }
    
    /**
     * 获取Git变更
     */
    private fun getGitChanges(repository: GitRepository, filePath: String?, limit: Int): String {
        val result = StringBuilder()
        
        try {
            // 获取变更
            val changes = if (filePath != null) {
                // 获取特定文件的变更
                val file = repository.root.findFileByRelativePath(filePath)
                if (file != null) {
                    GitChangeUtils.getDiff(project, repository.root, filePath, repository.currentRevision, null)
                } else {
                    return "找不到文件: $filePath"
                }
            } else {
                // 获取所有变更
                repository.changeLists
            }
            
            if (changes.isEmpty()) {
                return "没有变更"
            }
            
            result.append("变更列表:\n")
            changes.take(limit).forEach { change ->
                result.append("- ${change.afterRevision?.file?.name ?: change.beforeRevision?.file?.name ?: "未知文件"}: ${change.type}\n")
            }
            
            if (changes.size > limit) {
                result.append("- ... 等${changes.size - limit}个变更\n")
            }
        } catch (e: Exception) {
            logger.error("获取Git变更失败", e)
            result.append("获取变更失败: ${e.message}")
        }
        
        return result.toString()
    }
    
    /**
     * 获取Git历史
     */
    private fun getGitHistory(repository: GitRepository, filePath: String?, limit: Int): String {
        val result = StringBuilder()
        
        try {
            // 获取历史
            val commits = if (filePath != null) {
                // 获取特定文件的历史
                val file = repository.root.findFileByRelativePath(filePath)
                if (file != null) {
                    GitChangeUtils.history(project, repository.root, filePath)
                } else {
                    return "找不到文件: $filePath"
                }
            } else {
                // 获取所有历史
                GitChangeUtils.history(project, repository.root)
            }
            
            if (commits.isEmpty()) {
                return "没有历史记录"
            }
            
            result.append("提交历史:\n")
            commits.take(limit).forEach { commit ->
                result.append("- ${commit.id.toShortString()} (${commit.authorTime}): ${commit.subject}\n")
            }
            
            if (commits.size > limit) {
                result.append("- ... 等${commits.size - limit}个提交\n")
            }
        } catch (e: Exception) {
            logger.error("获取Git历史失败", e)
            result.append("获取历史失败: ${e.message}")
        }
        
        return result.toString()
    }
    
    /**
     * 生成提交信息
     */
    private fun generateCommitMessage(repository: GitRepository): String {
        val result = StringBuilder()
        
        try {
            // 获取变更
            val changes = repository.changeLists
            
            if (changes.isEmpty()) {
                return "没有变更，无法生成提交信息"
            }
            
            // 分析变更类型
            val addedFiles = mutableListOf<String>()
            val modifiedFiles = mutableListOf<String>()
            val deletedFiles = mutableListOf<String>()
            
            changes.forEach { change ->
                val fileName = change.afterRevision?.file?.name ?: change.beforeRevision?.file?.name ?: "未知文件"
                when (change.type) {
                    "ADDED" -> addedFiles.add(fileName)
                    "MODIFIED" -> modifiedFiles.add(fileName)
                    "DELETED" -> deletedFiles.add(fileName)
                }
            }
            
            // 生成提交信息
            val commitType = when {
                addedFiles.isNotEmpty() && modifiedFiles.isEmpty() && deletedFiles.isEmpty() -> "feat"
                modifiedFiles.isNotEmpty() && addedFiles.isEmpty() && deletedFiles.isEmpty() -> "fix"
                deletedFiles.isNotEmpty() -> "refactor"
                else -> "chore"
            }
            
            val scope = determineScope(changes)
            
            result.append("$commitType($scope): ")
            
            when (commitType) {
                "feat" -> {
                    result.append("添加")
                    if (addedFiles.size == 1) {
                        result.append("${addedFiles[0]}文件")
                    } else {
                        result.append("${addedFiles.size}个新文件")
                    }
                }
                "fix" -> {
                    result.append("修复")
                    if (modifiedFiles.size == 1) {
                        result.append("${modifiedFiles[0]}中的问题")
                    } else {
                        result.append("${modifiedFiles.size}个文件中的问题")
                    }
                }
                "refactor" -> {
                    if (deletedFiles.isNotEmpty()) {
                        result.append("删除")
                        if (deletedFiles.size == 1) {
                            result.append("${deletedFiles[0]}文件")
                        } else {
                            result.append("${deletedFiles.size}个文件")
                        }
                    } else {
                        result.append("重构代码")
                    }
                }
                else -> {
                    result.append("更新项目文件")
                }
            }
            
            // 添加详细信息
            result.append("\n\n详细变更:\n")
            
            if (addedFiles.isNotEmpty()) {
                result.append("- 添加: ${addedFiles.joinToString(", ")}\n")
            }
            
            if (modifiedFiles.isNotEmpty()) {
                result.append("- 修改: ${modifiedFiles.joinToString(", ")}\n")
            }
            
            if (deletedFiles.isNotEmpty()) {
                result.append("- 删除: ${deletedFiles.joinToString(", ")}\n")
            }
        } catch (e: Exception) {
            logger.error("生成提交信息失败", e)
            result.append("生成提交信息失败: ${e.message}")
        }
        
        return result.toString()
    }
    
    /**
     * 确定提交范围
     */
    private fun determineScope(changes: List<Any>): String {
        // 简单实现，根据文件扩展名确定范围
        val extensions = changes.mapNotNull {
            val fileName = it.toString()
            fileName.substringAfterLast('.', "")
        }.filter { it.isNotEmpty() }
        
        return when {
            extensions.contains("kt") || extensions.contains("java") -> "core"
            extensions.contains("xml") || extensions.contains("json") -> "config"
            extensions.contains("md") || extensions.contains("txt") -> "docs"
            extensions.contains("gradle") -> "build"
            else -> "misc"
        }
    }
}
