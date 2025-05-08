package ai.kastrax.codex.tools

import ai.kastrax.core.tools.Tool
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListManager
import git4idea.GitUtil
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Git 工具，用于执行 Git 相关操作
 */
class GitTool(private val project: Project) : Tool {
    private val logger = Logger.getInstance(GitTool::class.java)
    
    override val id: String = "git"
    override val name: String = "Git Operations"
    override val description: String = "执行 Git 相关操作，如生成提交消息"
    
    override val inputSchema: JsonElement = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            put("operation", buildJsonObject {
                put("type", "string")
                put("description", "Git 操作类型")
                put("enum", buildJsonObject {
                    put("generate_commit_message", "generate_commit_message")
                    put("analyze_diff", "analyze_diff")
                    put("get_changes", "get_changes")
                })
            })
            put("diff", buildJsonObject {
                put("type", "string")
                put("description", "代码差异（用于生成提交消息）")
            })
            put("style", buildJsonObject {
                put("type", "string")
                put("description", "提交消息风格 (conventional, descriptive, detailed)")
                put("enum", buildJsonObject {
                    put("conventional", "conventional")
                    put("descriptive", "descriptive")
                    put("detailed", "detailed")
                })
            })
        })
        put("required", buildJsonObject {
            put("operation", true)
        })
    }
    
    override suspend fun execute(params: JsonElement): String {
        try {
            val paramsObj = params.jsonObject
            val operation = paramsObj["operation"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("Missing 'operation' parameter")
            
            return when (operation) {
                "generate_commit_message" -> generateCommitMessage(paramsObj)
                "analyze_diff" -> analyzeDiff(paramsObj)
                "get_changes" -> getChanges()
                else -> throw IllegalArgumentException("Unknown operation: $operation")
            }
        } catch (e: Exception) {
            logger.error("Error executing git tool", e)
            return buildJsonObject {
                put("error", JsonPrimitive(e.message ?: "Unknown error"))
            }.toString()
        }
    }
    
    /**
     * 生成提交消息
     */
    private fun generateCommitMessage(params: JsonObject): String {
        val diff = params["diff"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Missing 'diff' parameter")
        val style = params["style"]?.jsonPrimitive?.content ?: "conventional"
        
        // 分析差异并生成提交消息
        val commitMessage = when (style) {
            "conventional" -> generateConventionalCommitMessage(diff)
            "descriptive" -> generateDescriptiveCommitMessage(diff)
            "detailed" -> generateDetailedCommitMessage(diff)
            else -> throw IllegalArgumentException("Unknown style: $style")
        }
        
        return buildJsonObject {
            put("message", JsonPrimitive(commitMessage.message))
            put("type", JsonPrimitive(commitMessage.type))
            put("scope", JsonPrimitive(commitMessage.scope))
            put("description", JsonPrimitive(commitMessage.description))
            put("breaking_changes", JsonPrimitive(commitMessage.breakingChanges))
        }.toString()
    }
    
    /**
     * 分析差异
     */
    private fun analyzeDiff(params: JsonObject): String {
        val diff = params["diff"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Missing 'diff' parameter")
        
        // 分析差异
        val analysis = analyzeDiffContent(diff)
        
        return buildJsonObject {
            put("files_changed", JsonPrimitive(analysis.filesChanged))
            put("insertions", JsonPrimitive(analysis.insertions))
            put("deletions", JsonPrimitive(analysis.deletions))
            put("summary", JsonPrimitive(analysis.summary))
        }.toString()
    }
    
    /**
     * 获取当前更改
     */
    private fun getChanges(): String {
        val changeListManager = ChangeListManager.getInstance(project)
        val changes = changeListManager.allChanges
        
        val changedFiles = changes.map { change ->
            val filePath = change.virtualFile?.path ?: "unknown"
            val changeType = getChangeType(change)
            
            buildJsonObject {
                put("file", JsonPrimitive(filePath))
                put("type", JsonPrimitive(changeType))
            }
        }
        
        return buildJsonObject {
            put("changes", buildJsonObject {
                put("files", buildJsonObject {
                    changedFiles.forEachIndexed { index, jsonObject ->
                        put(index.toString(), jsonObject)
                    }
                })
                put("count", JsonPrimitive(changes.size))
            })
        }.toString()
    }
    
    /**
     * 获取更改类型
     */
    private fun getChangeType(change: Change): String {
        return when {
            change.type == Change.Type.NEW -> "added"
            change.type == Change.Type.DELETED -> "deleted"
            change.type == Change.Type.MOVED -> "moved"
            else -> "modified"
        }
    }
    
    /**
     * 生成约定式提交消息
     */
    private fun generateConventionalCommitMessage(diff: String): CommitMessage {
        // 这里是简化实现，实际应该使用 LLM 或其他技术分析差异并生成提交消息
        val type = if (diff.contains("test")) "test" else if (diff.contains("fix")) "fix" else "feat"
        val scope = if (diff.contains("ui")) "ui" else if (diff.contains("api")) "api" else ""
        val description = "Generated conventional commit message"
        val breakingChanges = if (diff.contains("BREAKING CHANGE")) "Some breaking changes" else ""
        
        val message = if (scope.isNotEmpty()) {
            "$type($scope): $description"
        } else {
            "$type: $description"
        }
        
        return CommitMessage(message, type, scope, description, breakingChanges)
    }
    
    /**
     * 生成描述性提交消息
     */
    private fun generateDescriptiveCommitMessage(diff: String): CommitMessage {
        // 这里是简化实现
        val description = "Generated descriptive commit message"
        return CommitMessage(description, "", "", description, "")
    }
    
    /**
     * 生成详细提交消息
     */
    private fun generateDetailedCommitMessage(diff: String): CommitMessage {
        // 这里是简化实现
        val type = if (diff.contains("test")) "test" else if (diff.contains("fix")) "fix" else "feat"
        val scope = if (diff.contains("ui")) "ui" else if (diff.contains("api")) "api" else ""
        val description = "Generated detailed commit message"
        val breakingChanges = if (diff.contains("BREAKING CHANGE")) "Some breaking changes" else ""
        
        val message = if (scope.isNotEmpty()) {
            "$type($scope): $description\n\nDetailed explanation would go here.\n\n${if (breakingChanges.isNotEmpty()) "BREAKING CHANGE: $breakingChanges" else ""}"
        } else {
            "$type: $description\n\nDetailed explanation would go here.\n\n${if (breakingChanges.isNotEmpty()) "BREAKING CHANGE: $breakingChanges" else ""}"
        }
        
        return CommitMessage(message, type, scope, description, breakingChanges)
    }
    
    /**
     * 分析差异内容
     */
    private fun analyzeDiffContent(diff: String): DiffAnalysis {
        // 这里是简化实现，实际应该使用 Git API 或其他工具分析差异
        val lines = diff.lines()
        val filesChanged = lines.count { it.startsWith("+++") || it.startsWith("---") } / 2
        val insertions = lines.count { it.startsWith("+") && !it.startsWith("+++") }
        val deletions = lines.count { it.startsWith("-") && !it.startsWith("---") }
        val summary = "$filesChanged files changed, $insertions insertions(+), $deletions deletions(-)"
        
        return DiffAnalysis(filesChanged, insertions, deletions, summary)
    }
    
    /**
     * 提交消息
     */
    data class CommitMessage(
        val message: String,
        val type: String,
        val scope: String,
        val description: String,
        val breakingChanges: String
    )
    
    /**
     * 差异分析
     */
    data class DiffAnalysis(
        val filesChanged: Int,
        val insertions: Int,
        val deletions: Int,
        val summary: String
    )
}
