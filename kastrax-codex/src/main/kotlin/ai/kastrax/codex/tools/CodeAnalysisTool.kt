package ai.kastrax.codex.tools

import ai.kastrax.core.tools.Tool
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import kotlinx.serialization.json.*

/**
 * 代码分析工具，用于分析代码结构和语义
 */
class CodeAnalysisTool(private val project: Project) {
    
    private val logger = Logger.getInstance(CodeAnalysisTool::class.java)
    
    /**
     * 创建代码分析工具
     */
    fun createTool(): Tool {
        return object : Tool {
            override val id: String = "codeAnalysis"
            override val name: String = "代码分析"
            override val description: String = "分析代码结构和语义，包括类、方法、字段等"
            
            override val inputSchema: JsonElement = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("fileName") {
                        put("type", "string")
                        put("description", "要分析的文件名")
                    }
                    putJsonObject("className") {
                        put("type", "string")
                        put("description", "要分析的类名（可选）")
                    }
                    putJsonObject("methodName") {
                        put("type", "string")
                        put("description", "要分析的方法名（可选）")
                    }
                    putJsonObject("analysisType") {
                        put("type", "string")
                        put("description", "分析类型：structure（结构）、dependencies（依赖）、complexity（复杂度）")
                        putJsonArray("enum") {
                            add("structure")
                            add("dependencies")
                            add("complexity")
                        }
                    }
                }
                putJsonArray("required") {
                    add("fileName")
                    add("analysisType")
                }
            }
            
            override val outputSchema: JsonElement? = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("analysis") {
                        put("type", "string")
                        put("description", "分析结果")
                    }
                    putJsonObject("structure") {
                        put("type", "object")
                        put("description", "代码结构信息")
                    }
                }
            }
            
            override suspend fun execute(input: JsonElement): JsonElement {
                try {
                    // 解析输入参数
                    val inputObj = input.jsonObject
                    val fileName = inputObj["fileName"]?.jsonPrimitive?.content
                        ?: throw IllegalArgumentException("文件名不能为空")
                    val className = inputObj["className"]?.jsonPrimitive?.content
                    val methodName = inputObj["methodName"]?.jsonPrimitive?.content
                    val analysisType = inputObj["analysisType"]?.jsonPrimitive?.content
                        ?: "structure"
                    
                    // 查找文件
                    val psiFiles = FilenameIndex.getFilesByName(
                        project,
                        fileName,
                        GlobalSearchScope.projectScope(project)
                    )
                    
                    if (psiFiles.isEmpty()) {
                        return buildJsonObject {
                            put("success", false)
                            put("error", "找不到文件: $fileName")
                        }
                    }
                    
                    // 分析文件
                    val result = when (analysisType) {
                        "structure" -> analyzeStructure(psiFiles[0], className, methodName)
                        "dependencies" -> analyzeDependencies(psiFiles[0], className)
                        "complexity" -> analyzeComplexity(psiFiles[0], className, methodName)
                        else -> "不支持的分析类型: $analysisType"
                    }
                    
                    return buildJsonObject {
                        put("success", true)
                        put("result", result)
                    }
                } catch (e: Exception) {
                    logger.error("代码分析失败", e)
                    return buildJsonObject {
                        put("success", false)
                        put("error", e.message ?: "未知错误")
                    }
                }
            }
        }
    }
    
    /**
     * 分析代码结构
     */
    private fun analyzeStructure(psiFile: PsiFile, className: String?, methodName: String?): String {
        val result = StringBuilder()
        
        // 如果指定了类名，查找特定类
        if (className != null) {
            val psiClass = findClass(psiFile, className)
            if (psiClass != null) {
                result.append("类: ${psiClass.name}\n")
                result.append("包: ${psiClass.qualifiedName?.substringBeforeLast('.') ?: "默认包"}\n")
                result.append("修饰符: ${psiClass.modifierList?.text ?: ""}\n\n")
                
                // 如果指定了方法名，查找特定方法
                if (methodName != null) {
                    val method = psiClass.findMethodsByName(methodName, false).firstOrNull()
                    if (method != null) {
                        result.append("方法: ${method.name}\n")
                        result.append("返回类型: ${method.returnType?.presentableText ?: "void"}\n")
                        result.append("参数: ${method.parameterList.parameters.joinToString(", ") { "${it.type.presentableText} ${it.name}" }}\n")
                        result.append("修饰符: ${method.modifierList.text}\n")
                    } else {
                        result.append("找不到方法: $methodName\n")
                    }
                } else {
                    // 列出所有方法
                    result.append("方法列表:\n")
                    psiClass.methods.forEach { method ->
                        result.append("- ${method.name}(${method.parameterList.parameters.joinToString(", ") { "${it.type.presentableText} ${it.name}" }}): ${method.returnType?.presentableText ?: "void"}\n")
                    }
                    
                    // 列出所有字段
                    result.append("\n字段列表:\n")
                    psiClass.fields.forEach { field ->
                        result.append("- ${field.type.presentableText} ${field.name} ${if (field.hasModifierProperty("final")) "(常量)" else ""}\n")
                    }
                }
            } else {
                result.append("找不到类: $className\n")
            }
        } else {
            // 列出文件中的所有类
            val classes = PsiTreeUtil.findChildrenOfType(psiFile, PsiClass::class.java)
            result.append("文件: ${psiFile.name}\n")
            result.append("类数量: ${classes.size}\n\n")
            
            classes.forEach { psiClass ->
                result.append("类: ${psiClass.name}\n")
                result.append("方法数量: ${psiClass.methods.size}\n")
                result.append("字段数量: ${psiClass.fields.size}\n\n")
            }
        }
        
        return result.toString()
    }
    
    /**
     * 分析代码依赖
     */
    private fun analyzeDependencies(psiFile: PsiFile, className: String?): String {
        val result = StringBuilder()
        
        // 获取导入语句
        val importList = psiFile.children.find { it.toString().contains("PsiImportList") }
        if (importList != null) {
            result.append("导入语句:\n")
            importList.children.forEach { importStatement ->
                if (importStatement.toString().contains("PsiImportStatement")) {
                    result.append("- ${importStatement.text.trim()}\n")
                }
            }
            result.append("\n")
        }
        
        // 如果指定了类名，分析特定类的依赖
        if (className != null) {
            val psiClass = findClass(psiFile, className)
            if (psiClass != null) {
                result.append("类 $className 的依赖:\n")
                
                // 分析字段依赖
                result.append("字段依赖:\n")
                psiClass.fields.forEach { field ->
                    result.append("- ${field.name}: ${field.type.presentableText}\n")
                }
                
                // 分析方法参数和返回类型依赖
                result.append("\n方法依赖:\n")
                psiClass.methods.forEach { method ->
                    result.append("- ${method.name}:\n")
                    result.append("  返回类型: ${method.returnType?.presentableText ?: "void"}\n")
                    method.parameterList.parameters.forEach { param ->
                        result.append("  参数: ${param.name}: ${param.type.presentableText}\n")
                    }
                }
            } else {
                result.append("找不到类: $className\n")
            }
        } else {
            result.append("未指定类名，请提供类名以分析依赖\n")
        }
        
        return result.toString()
    }
    
    /**
     * 分析代码复杂度
     */
    private fun analyzeComplexity(psiFile: PsiFile, className: String?, methodName: String?): String {
        val result = StringBuilder()
        
        // 如果指定了类名和方法名，分析特定方法的复杂度
        if (className != null && methodName != null) {
            val psiClass = findClass(psiFile, className)
            if (psiClass != null) {
                val method = psiClass.findMethodsByName(methodName, false).firstOrNull()
                if (method != null) {
                    result.append("方法 $methodName 的复杂度分析:\n")
                    
                    // 计算圈复杂度（简化版）
                    val complexity = calculateComplexity(method)
                    result.append("圈复杂度: $complexity\n")
                    
                    // 方法长度
                    val lines = method.text.lines().size
                    result.append("方法行数: $lines\n")
                    
                    // 参数数量
                    val paramCount = method.parameterList.parametersCount
                    result.append("参数数量: $paramCount\n")
                    
                    // 复杂度评估
                    result.append("\n复杂度评估:\n")
                    if (complexity > 10) {
                        result.append("- 圈复杂度过高，建议重构\n")
                    }
                    if (lines > 30) {
                        result.append("- 方法过长，建议拆分\n")
                    }
                    if (paramCount > 5) {
                        result.append("- 参数过多，建议使用对象封装参数\n")
                    }
                } else {
                    result.append("找不到方法: $methodName\n")
                }
            } else {
                result.append("找不到类: $className\n")
            }
        } else {
            result.append("未指定类名或方法名，请提供类名和方法名以分析复杂度\n")
        }
        
        return result.toString()
    }
    
    /**
     * 查找类
     */
    private fun findClass(psiFile: PsiFile, className: String): PsiClass? {
        return PsiTreeUtil.findChildrenOfType(psiFile, PsiClass::class.java)
            .find { it.name == className }
    }
    
    /**
     * 计算方法的圈复杂度（简化版）
     */
    private fun calculateComplexity(method: PsiMethod): Int {
        // 基础复杂度为1
        var complexity = 1
        
        // 统计条件语句和循环语句
        val methodText = method.text.lowercase()
        
        // 计算if语句
        complexity += "\\bif\\b".toRegex().findAll(methodText).count()
        
        // 计算for循环
        complexity += "\\bfor\\b".toRegex().findAll(methodText).count()
        
        // 计算while循环
        complexity += "\\bwhile\\b".toRegex().findAll(methodText).count()
        
        // 计算case语句
        complexity += "\\bcase\\b".toRegex().findAll(methodText).count()
        
        // 计算catch块
        complexity += "\\bcatch\\b".toRegex().findAll(methodText).count()
        
        // 计算三元运算符
        complexity += "\\?.*:".toRegex().findAll(methodText).count()
        
        return complexity
    }
}
