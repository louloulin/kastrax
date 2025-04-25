package ai.kastrax.examples.workflow

import ai.kastrax.core.agent.agent
import ai.kastrax.core.workflow.WorkflowContext
import ai.kastrax.core.workflow.WorkflowStep
import ai.kastrax.core.workflow.WorkflowStepResult
import ai.kastrax.core.workflow.builder.workflow
import ai.kastrax.core.workflow.engine.WorkflowEngine
import ai.kastrax.core.workflow.state.InMemoryWorkflowStateStorage
import ai.kastrax.core.workflow.suspend.AbstractSuspendableStep
import ai.kastrax.core.workflow.suspend.SuspendController
import ai.kastrax.integrations.openai.openAi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * 高级工作流示例，展示工作流引擎的新功能。
 */
fun main() = runBlocking {
    println("高级工作流示例")
    println("------------")
    
    // 创建内容生成步骤
    val generateContentStep = object : WorkflowStep {
        override val id: String = "generateContent"
        override val name: String = "生成内容"
        override val description: String = "生成初始内容"
        override val after: List<String> = emptyList()
        
        override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
            println("执行内容生成步骤...")
            
            // 模拟内容生成
            val content = "这是自动生成的内容，需要进行审核和改进。"
            val qualityScore = 0.7 // 模拟质量评分
            
            return WorkflowStepResult.success(
                stepId = id,
                output = mapOf(
                    "content" to JsonPrimitive(content),
                    "qualityScore" to JsonPrimitive(qualityScore)
                )
            )
        }
    }
    
    // 创建内容审核步骤（可暂停）
    val reviewContentStep = object : AbstractSuspendableStep(
        id = "reviewContent",
        name = "内容审核",
        description = "审核生成的内容",
        after = listOf("generateContent")
    ) {
        override suspend fun execute(
            context: WorkflowContext,
            suspendController: SuspendController
        ): WorkflowStepResult {
            // 获取上一步生成的内容
            val content = context.getStepOutput("generateContent")
                ?.get("content")?.toString()?.removeSurrounding("\"")
                ?: "无内容"
            
            val qualityScore = context.getStepOutput("generateContent")
                ?.get("qualityScore")?.toString()?.toDoubleOrNull()
                ?: 0.0
            
            println("执行内容审核步骤...")
            println("内容: $content")
            println("质量评分: $qualityScore")
            
            // 检查是否是恢复执行
            if (suspendController.isResumed()) {
                println("恢复执行内容审核步骤...")
                
                // 获取恢复数据
                val resumeData = suspendController.getResumeData()
                val approved = resumeData?.get("approved")?.toString()?.toBoolean() ?: false
                val feedback = resumeData?.get("feedback")?.toString()?.removeSurrounding("\"") ?: ""
                
                println("审核结果: ${if (approved) "通过" else "不通过"}")
                println("反馈: $feedback")
                
                return WorkflowStepResult.success(
                    stepId = id,
                    output = mapOf(
                        "content" to JsonPrimitive(content),
                        "approved" to JsonPrimitive(approved),
                        "feedback" to JsonPrimitive(feedback)
                    )
                )
            }
            
            // 如果质量评分低于阈值，暂停工作流等待人工审核
            if (qualityScore < 0.8) {
                println("内容质量低于阈值，暂停工作流等待人工审核...")
                
                // 暂停工作流
                suspendController.suspend(buildJsonObject {
                    put("content", JsonPrimitive(content))
                    put("qualityScore", JsonPrimitive(qualityScore))
                    put("message", JsonPrimitive("请审核此内容并提供反馈"))
                })
                
                // 这里的代码不会执行，因为suspend会抛出异常
                return WorkflowStepResult.success(id, emptyMap())
            }
            
            // 如果质量评分高于阈值，自动通过
            println("内容质量高于阈值，自动通过审核")
            
            return WorkflowStepResult.success(
                stepId = id,
                output = mapOf(
                    "content" to JsonPrimitive(content),
                    "approved" to JsonPrimitive(true),
                    "feedback" to JsonPrimitive("自动通过")
                )
            )
        }
    }
    
    // 创建内容改进步骤
    val improveContentStep = object : WorkflowStep {
        override val id: String = "improveContent"
        override val name: String = "内容改进"
        override val description: String = "根据审核结果改进内容"
        override val after: List<String> = listOf("reviewContent")
        
        override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
            // 获取审核结果
            val content = context.getStepOutput("reviewContent")
                ?.get("content")?.toString()?.removeSurrounding("\"")
                ?: "无内容"
            
            val approved = context.getStepOutput("reviewContent")
                ?.get("approved")?.toString()?.toBoolean()
                ?: false
            
            val feedback = context.getStepOutput("reviewContent")
                ?.get("feedback")?.toString()?.removeSurrounding("\"")
                ?: ""
            
            println("执行内容改进步骤...")
            println("原始内容: $content")
            println("审核通过: $approved")
            println("反馈: $feedback")
            
            // 根据审核结果改进内容
            val improvedContent = if (approved) {
                content
            } else {
                "$content\n\n改进：根据反馈「$feedback」进行了修改。"
            }
            
            println("改进后的内容: $improvedContent")
            
            return WorkflowStepResult.success(
                stepId = id,
                output = mapOf(
                    "content" to JsonPrimitive(improvedContent),
                    "improved" to JsonPrimitive(true)
                )
            )
        }
    }
    
    // 创建并行处理步骤
    val formatContentStep = object : WorkflowStep {
        override val id: String = "formatContent"
        override val name: String = "格式化内容"
        override val description: String = "格式化内容"
        override val after: List<String> = emptyList()
        
        override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
            val content = context.getStepOutput("improveContent")
                ?.get("content")?.toString()?.removeSurrounding("\"")
                ?: "无内容"
            
            println("执行格式化内容步骤...")
            
            // 模拟格式化
            val formattedContent = content.replace("\n\n", "\n")
            
            return WorkflowStepResult.success(
                stepId = id,
                output = mapOf(
                    "formattedContent" to JsonPrimitive(formattedContent)
                )
            )
        }
    }
    
    val analyzeContentStep = object : WorkflowStep {
        override val id: String = "analyzeContent"
        override val name: String = "分析内容"
        override val description: String = "分析内容"
        override val after: List<String> = emptyList()
        
        override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
            val content = context.getStepOutput("improveContent")
                ?.get("content")?.toString()?.removeSurrounding("\"")
                ?: "无内容"
            
            println("执行内容分析步骤...")
            
            // 模拟分析
            val wordCount = content.split("\\s+".toRegex()).size
            val sentiment = "积极"
            
            return WorkflowStepResult.success(
                stepId = id,
                output = mapOf(
                    "wordCount" to JsonPrimitive(wordCount),
                    "sentiment" to JsonPrimitive(sentiment)
                )
            )
        }
    }
    
    // 创建最终步骤
    val finalizeContentStep = object : WorkflowStep {
        override val id: String = "finalizeContent"
        override val name: String = "最终处理"
        override val description: String = "最终处理内容"
        override val after: List<String> = listOf("parallelProcessing")
        
        override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
            // 获取并行步骤的结果
            val parallelResults = context.getStepOutput("parallelProcessing")
                ?.get("results") as? JsonElement
            
            val formattedContent = parallelResults
                ?.get("formatContent")
                ?.get("formattedContent")?.toString()?.removeSurrounding("\"")
                ?: "无内容"
            
            val wordCount = parallelResults
                ?.get("analyzeContent")
                ?.get("wordCount")?.toString()?.toIntOrNull()
                ?: 0
            
            val sentiment = parallelResults
                ?.get("analyzeContent")
                ?.get("sentiment")?.toString()?.removeSurrounding("\"")
                ?: "未知"
            
            println("执行最终处理步骤...")
            println("格式化内容: $formattedContent")
            println("字数: $wordCount")
            println("情感: $sentiment")
            
            // 创建最终输出
            val finalContent = """
                |最终内容:
                |$formattedContent
                |
                |统计信息:
                |字数: $wordCount
                |情感: $sentiment
            """.trimMargin()
            
            println("\n最终结果:")
            println(finalContent)
            
            return WorkflowStepResult.success(
                stepId = id,
                output = mapOf(
                    "finalContent" to JsonPrimitive(finalContent)
                )
            )
        }
    }
    
    // 创建工作流
    val contentWorkflow = workflow("ContentWorkflow", "内容处理工作流") {
        // 添加内容生成步骤
        step(generateContentStep)
        
        // 添加内容审核步骤
        suspendableStep(reviewContentStep)
        
        // 添加内容改进步骤
        step(improveContentStep)
        
        // 添加并行处理步骤
        parallel(formatContentStep, analyzeContentStep)
        
        // 添加最终处理步骤
        step(finalizeContentStep)
    }
    
    // 创建工作流引擎
    val workflowEngine = WorkflowEngine(
        workflows = mapOf("ContentWorkflow" to contentWorkflow),
        stateStorage = InMemoryWorkflowStateStorage()
    )
    
    // 执行工作流
    println("\n开始执行工作流...")
    val result = workflowEngine.executeWorkflow(
        workflowId = "ContentWorkflow",
        input = emptyMap()
    )
    
    // 检查工作流是否暂停
    val suspendedStepId = result.steps.entries.find { (_, stepResult) -> 
        stepResult.status == ai.kastrax.core.workflow.StepStatus.SUSPENDED 
    }?.key
    
    if (suspendedStepId != null) {
        println("\n工作流已暂停，等待人工审核...")
        println("暂停的步骤: $suspendedStepId")
        
        // 获取暂停的步骤
        val suspendedStep = result.steps[suspendedStepId]
        val suspendPayload = suspendedStep?.suspendPayload
        
        println("暂停数据:")
        println("内容: ${suspendPayload?.get("content")}")
        println("质量评分: ${suspendPayload?.get("qualityScore")}")
        println("消息: ${suspendPayload?.get("message")}")
        
        // 模拟人工审核
        println("\n模拟人工审核...")
        val humanReviewData = buildJsonObject {
            put("approved", JsonPrimitive(true))
            put("feedback", JsonPrimitive("内容已审核，质量良好"))
        }
        
        // 恢复工作流
        println("\n恢复工作流执行...")
        val resumeResult = workflowEngine.resumeWorkflow(
            executionId = result.executionId,
            stepId = suspendedStepId,
            data = humanReviewData
        )
        
        // 打印最终结果
        println("\n工作流执行完成!")
        println("成功: ${resumeResult.success}")
        println("执行时间: ${resumeResult.executionTime}ms")
        
        // 打印每个步骤的结果
        println("\n步骤执行结果:")
        resumeResult.steps.forEach { (stepId, stepResult) ->
            println("$stepId: ${if (stepResult.success) "成功" else "失败"}")
        }
    } else {
        // 打印最终结果
        println("\n工作流执行完成!")
        println("成功: ${result.success}")
        println("执行时间: ${result.executionTime}ms")
        
        // 打印每个步骤的结果
        println("\n步骤执行结果:")
        result.steps.forEach { (stepId, stepResult) ->
            println("$stepId: ${if (stepResult.success) "成功" else "失败"}")
        }
    }
}
