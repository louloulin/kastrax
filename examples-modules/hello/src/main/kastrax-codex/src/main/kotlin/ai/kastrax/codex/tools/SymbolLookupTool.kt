package ai.kastrax.codex.tools

import ai.kastrax.core.tools.Tool
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 符号查找工具，用于查找项目中的类、方法、字段等符号
 */
class SymbolLookupTool(private val project: Project) : Tool {
    private val logger = Logger.getInstance(SymbolLookupTool::class.java)
    
    override val id: String = "symbol_lookup"
    override val name: String = "Symbol Lookup"
    override val description: String = "查找项目中的类、方法、字段等符号"
    
    override val inputSchema: JsonElement = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            put("symbol_type", buildJsonObject {
                put("type", "string")
                put("description", "符号类型")
                put("enum", buildJsonObject {
                    put("class", "class")
                    put("method", "method")
                    put("field", "field")
                })
            })
            put("name", buildJsonObject {
                put("type", "string")
                put("description", "符号名称")
            })
            put("fully_qualified", buildJsonObject {
                put("type", "boolean")
                put("description", "是否使用完全限定名")
            })
        })
        put("required", buildJsonObject {
            put("symbol_type", true)
            put("name", true)
        })
    }
    
    override suspend fun execute(params: JsonElement): String {
        try {
            val paramsObj = params.jsonObject
            val symbolType = paramsObj["symbol_type"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("Missing 'symbol_type' parameter")
            val name = paramsObj["name"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("Missing 'name' parameter")
            val fullyQualified = paramsObj["fully_qualified"]?.jsonPrimitive?.content?.toBoolean() ?: false
            
            // 查找符号
            return when (symbolType) {
                "class" -> findClasses(name, fullyQualified)
                "method" -> findMethods(name, fullyQualified)
                "field" -> findFields(name, fullyQualified)
                else -> throw IllegalArgumentException("Unknown symbol type: $symbolType")
            }
        } catch (e: Exception) {
            logger.error("Error executing symbol lookup tool", e)
            return buildJsonObject {
                put("error", JsonPrimitive(e.message ?: "Unknown error"))
            }.toString()
        }
    }
    
    /**
     * 查找类
     */
    private fun findClasses(name: String, fullyQualified: Boolean): String {
        val cache = PsiShortNamesCache.getInstance(project)
        val scope = GlobalSearchScope.allScope(project)
        val classes = cache.getClassesByName(name, scope)
        
        val classInfos = classes.map { psiClass ->
            buildJsonObject {
                put("name", JsonPrimitive(psiClass.name ?: ""))
                put("qualified_name", JsonPrimitive(psiClass.qualifiedName ?: ""))
                put("is_interface", JsonPrimitive(psiClass.isInterface))
                put("is_enum", JsonPrimitive(psiClass.isEnum))
                put("super_class", JsonPrimitive(psiClass.superClass?.qualifiedName ?: ""))
                put("interfaces", buildJsonObject {
                    psiClass.interfaces.forEachIndexed { index, interfaceClass ->
                        put(index.toString(), JsonPrimitive(interfaceClass.qualifiedName ?: ""))
                    }
                })
                put("methods", buildJsonObject {
                    psiClass.methods.forEachIndexed { index, method ->
                        put(index.toString(), JsonPrimitive(method.name))
                    }
                })
                put("fields", buildJsonObject {
                    psiClass.fields.forEachIndexed { index, field ->
                        put(index.toString(), JsonPrimitive(field.name))
                    }
                })
            }
        }
        
        return buildJsonObject {
            put("classes", buildJsonObject {
                classInfos.forEachIndexed { index, classInfo ->
                    put(index.toString(), classInfo)
                }
            })
            put("count", JsonPrimitive(classes.size))
        }.toString()
    }
    
    /**
     * 查找方法
     */
    private fun findMethods(name: String, fullyQualified: Boolean): String {
        val cache = PsiShortNamesCache.getInstance(project)
        val scope = GlobalSearchScope.allScope(project)
        val methods = cache.getMethodsByName(name, scope)
        
        val methodInfos = methods.map { method ->
            buildJsonObject {
                put("name", JsonPrimitive(method.name))
                put("containing_class", JsonPrimitive(method.containingClass?.qualifiedName ?: ""))
                put("return_type", JsonPrimitive(method.returnType?.presentableText ?: ""))
                put("parameters", buildJsonObject {
                    method.parameterList.parameters.forEachIndexed { index, parameter ->
                        put(index.toString(), buildJsonObject {
                            put("name", JsonPrimitive(parameter.name ?: ""))
                            put("type", JsonPrimitive(parameter.type.presentableText))
                        })
                    }
                })
                put("is_constructor", JsonPrimitive(method.isConstructor))
                put("is_static", JsonPrimitive(method.hasModifierProperty("static")))
                put("visibility", JsonPrimitive(getVisibility(method)))
            }
        }
        
        return buildJsonObject {
            put("methods", buildJsonObject {
                methodInfos.forEachIndexed { index, methodInfo ->
                    put(index.toString(), methodInfo)
                }
            })
            put("count", JsonPrimitive(methods.size))
        }.toString()
    }
    
    /**
     * 查找字段
     */
    private fun findFields(name: String, fullyQualified: Boolean): String {
        val cache = PsiShortNamesCache.getInstance(project)
        val scope = GlobalSearchScope.allScope(project)
        val fields = cache.getFieldsByName(name, scope)
        
        val fieldInfos = fields.map { field ->
            buildJsonObject {
                put("name", JsonPrimitive(field.name))
                put("containing_class", JsonPrimitive(field.containingClass?.qualifiedName ?: ""))
                put("type", JsonPrimitive(field.type.presentableText))
                put("is_static", JsonPrimitive(field.hasModifierProperty("static")))
                put("is_final", JsonPrimitive(field.hasModifierProperty("final")))
                put("visibility", JsonPrimitive(getVisibility(field)))
            }
        }
        
        return buildJsonObject {
            put("fields", buildJsonObject {
                fieldInfos.forEachIndexed { index, fieldInfo ->
                    put(index.toString(), fieldInfo)
                }
            })
            put("count", JsonPrimitive(fields.size))
        }.toString()
    }
    
    /**
     * 获取可见性
     */
    private fun getVisibility(element: PsiMethod): String {
        return when {
            element.hasModifierProperty("public") -> "public"
            element.hasModifierProperty("protected") -> "protected"
            element.hasModifierProperty("private") -> "private"
            else -> "package-private"
        }
    }
    
    /**
     * 获取可见性
     */
    private fun getVisibility(element: PsiField): String {
        return when {
            element.hasModifierProperty("public") -> "public"
            element.hasModifierProperty("protected") -> "protected"
            element.hasModifierProperty("private") -> "private"
            else -> "package-private"
        }
    }
}
