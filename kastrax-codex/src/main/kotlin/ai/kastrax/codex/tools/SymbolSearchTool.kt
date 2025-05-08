package ai.kastrax.codex.tools

import ai.kastrax.core.tools.Tool
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import kotlinx.serialization.json.*

/**
 * 符号查找工具，用于查找项目中的符号
 */
class SymbolSearchTool(private val project: Project) {
    
    private val logger = Logger.getInstance(SymbolSearchTool::class.java)
    
    /**
     * 创建符号查找工具
     */
    fun createTool(): Tool {
        return object : Tool {
            override val id: String = "symbolSearch"
            override val name: String = "符号查找"
            override val description: String = "查找项目中的类、方法、字段等符号"
            
            override val inputSchema: JsonElement = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("query") {
                        put("type", "string")
                        put("description", "要查找的符号名称")
                    }
                    putJsonObject("symbolType") {
                        put("type", "string")
                        put("description", "符号类型：class（类）、method（方法）、field（字段）、all（所有）")
                        putJsonArray("enum") {
                            add("class")
                            add("method")
                            add("field")
                            add("all")
                        }
                    }
                    putJsonObject("exactMatch") {
                        put("type", "boolean")
                        put("description", "是否精确匹配")
                    }
                    putJsonObject("limit") {
                        put("type", "integer")
                        put("description", "最大结果数量")
                    }
                }
                putJsonArray("required") {
                    add("query")
                }
            }
            
            override val outputSchema: JsonElement? = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("classes") {
                        put("type", "array")
                        put("description", "找到的类列表")
                    }
                    putJsonObject("methods") {
                        put("type", "array")
                        put("description", "找到的方法列表")
                    }
                    putJsonObject("fields") {
                        put("type", "array")
                        put("description", "找到的字段列表")
                    }
                }
            }
            
            override suspend fun execute(input: JsonElement): JsonElement {
                try {
                    // 解析输入参数
                    val inputObj = input.jsonObject
                    val query = inputObj["query"]?.jsonPrimitive?.content
                        ?: throw IllegalArgumentException("查询不能为空")
                    val symbolType = inputObj["symbolType"]?.jsonPrimitive?.content ?: "all"
                    val exactMatch = inputObj["exactMatch"]?.jsonPrimitive?.boolean ?: false
                    val limit = inputObj["limit"]?.jsonPrimitive?.int ?: 10
                    
                    // 执行查找
                    val scope = GlobalSearchScope.projectScope(project)
                    val shortNamesCache = PsiShortNamesCache.getInstance(project)
                    
                    // 构建结果
                    val resultObj = buildJsonObject {
                        put("success", true)
                        
                        // 查找类
                        if (symbolType == "class" || symbolType == "all") {
                            putJsonArray("classes") {
                                val classNames = if (exactMatch) {
                                    arrayOf(query)
                                } else {
                                    shortNamesCache.allClassNames.filter { it.contains(query, ignoreCase = true) }.toTypedArray()
                                }
                                
                                var count = 0
                                for (className in classNames) {
                                    if (count >= limit) break
                                    
                                    val classes = shortNamesCache.getClassesByName(className, scope)
                                    for (cls in classes) {
                                        if (count >= limit) break
                                        
                                        addJsonObject {
                                            put("name", cls.name)
                                            put("qualifiedName", cls.qualifiedName)
                                            put("file", cls.containingFile.name)
                                            put("path", cls.containingFile.virtualFile.path)
                                            put("isInterface", cls.isInterface)
                                            put("isEnum", cls.isEnum)
                                            put("isAbstract", cls.hasModifierProperty("abstract"))
                                        }
                                        count++
                                    }
                                }
                            }
                        }
                        
                        // 查找方法
                        if (symbolType == "method" || symbolType == "all") {
                            putJsonArray("methods") {
                                val methodNames = if (exactMatch) {
                                    arrayOf(query)
                                } else {
                                    shortNamesCache.allMethodNames.filter { it.contains(query, ignoreCase = true) }.toTypedArray()
                                }
                                
                                var count = 0
                                for (methodName in methodNames) {
                                    if (count >= limit) break
                                    
                                    val methods = shortNamesCache.getMethodsByName(methodName, scope)
                                    for (method in methods) {
                                        if (count >= limit) break
                                        
                                        val containingClass = method.containingClass
                                        addJsonObject {
                                            put("name", method.name)
                                            put("returnType", method.returnType?.presentableText ?: "void")
                                            put("parameters", method.parameterList.parameters.joinToString(", ") { "${it.type.presentableText} ${it.name}" })
                                            put("class", containingClass?.name ?: "")
                                            put("classQualifiedName", containingClass?.qualifiedName ?: "")
                                            put("file", method.containingFile.name)
                                            put("path", method.containingFile.virtualFile.path)
                                        }
                                        count++
                                    }
                                }
                            }
                        }
                        
                        // 查找字段
                        if (symbolType == "field" || symbolType == "all") {
                            putJsonArray("fields") {
                                val fieldNames = if (exactMatch) {
                                    arrayOf(query)
                                } else {
                                    shortNamesCache.allFieldNames.filter { it.contains(query, ignoreCase = true) }.toTypedArray()
                                }
                                
                                var count = 0
                                for (fieldName in fieldNames) {
                                    if (count >= limit) break
                                    
                                    val fields = shortNamesCache.getFieldsByName(fieldName, scope)
                                    for (field in fields) {
                                        if (count >= limit) break
                                        
                                        val containingClass = field.containingClass
                                        addJsonObject {
                                            put("name", field.name)
                                            put("type", field.type.presentableText)
                                            put("class", containingClass?.name ?: "")
                                            put("classQualifiedName", containingClass?.qualifiedName ?: "")
                                            put("file", field.containingFile.name)
                                            put("path", field.containingFile.virtualFile.path)
                                            put("isConstant", field.hasModifierProperty("final") && field.hasModifierProperty("static"))
                                        }
                                        count++
                                    }
                                }
                            }
                        }
                    }
                    
                    return resultObj
                } catch (e: Exception) {
                    logger.error("符号查找失败", e)
                    return buildJsonObject {
                        put("success", false)
                        put("error", e.message ?: "未知错误")
                    }
                }
            }
        }
    }
}
