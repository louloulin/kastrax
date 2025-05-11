package ai.kastrax.codebase.semantic.parser

import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.model.Location
import ai.kastrax.codebase.semantic.model.Modifier
import ai.kastrax.codebase.semantic.model.Visibility
import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Modifier as JavaParserModifier
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.EnumDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.Parameter
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.comments.Comment
import com.github.javaparser.ast.expr.AnnotationExpr
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * Java 代码解析器
 *
 * 使用 JavaParser 库解析 Java 代码文件
 */
class JavaCodeParser : AbstractCodeParser() {
    private val javaParser: JavaParser
    
    init {
        val config = ParserConfiguration()
        config.setAttributeComments(true)
        javaParser = JavaParser(config)
    }
    
    /**
     * 解析 Java 代码文件
     *
     * @param filePath 文件路径
     * @param content 文件内容
     * @return 代码元素（文件级别）
     */
    override fun parseFile(filePath: Path, content: String): CodeElement {
        try {
            // 创建文件元素
            val fileElement = createFileElement(filePath, content)
            
            // 解析 Java 代码
            val result = javaParser.parse(content)
            if (result.isSuccessful) {
                val compilationUnit = result.result.get()
                
                // 解析包声明
                compilationUnit.packageDeclaration.ifPresent { packageDecl ->
                    val packageName = packageDecl.nameAsString
                    val packageLocation = createLocation(filePath, packageDecl)
                    
                    val packageElement = CodeElement(
                        id = UUID.randomUUID().toString(),
                        name = packageName,
                        qualifiedName = packageName,
                        type = CodeElementType.PACKAGE,
                        location = packageLocation,
                        visibility = Visibility.PUBLIC,
                        parent = fileElement,
                        language = "java"
                    )
                    
                    fileElement.addChild(packageElement)
                    fileElement.metadata["package"] = packageName
                }
                
                // 解析导入声明
                compilationUnit.imports.forEach { importDecl ->
                    val importName = importDecl.nameAsString
                    val isStatic = importDecl.isStatic
                    val isAsterisk = importDecl.isAsterisk
                    val importLocation = createLocation(filePath, importDecl)
                    
                    val importElement = CodeElement(
                        id = UUID.randomUUID().toString(),
                        name = importName,
                        qualifiedName = importName,
                        type = CodeElementType.IMPORT,
                        location = importLocation,
                        parent = fileElement,
                        language = "java"
                    )
                    
                    importElement.metadata["isStatic"] = isStatic
                    importElement.metadata["isAsterisk"] = isAsterisk
                    
                    fileElement.addChild(importElement)
                }
                
                // 解析类型声明
                compilationUnit.types.forEach { typeDecl ->
                    when (typeDecl) {
                        is ClassOrInterfaceDeclaration -> {
                            val classElement = parseClassOrInterface(filePath, typeDecl, fileElement)
                            fileElement.addChild(classElement)
                        }
                        is EnumDeclaration -> {
                            val enumElement = parseEnum(filePath, typeDecl, fileElement)
                            fileElement.addChild(enumElement)
                        }
                        else -> {
                            logger.warn { "不支持的类型声明: ${typeDecl.javaClass.simpleName}" }
                        }
                    }
                }
                
                // 解析注释
                compilationUnit.allComments.forEach { comment ->
                    val commentElement = parseComment(filePath, comment, fileElement)
                    fileElement.addChild(commentElement)
                }
            } else {
                logger.error { "解析 Java 文件失败: $filePath, 错误: ${result.problems}" }
            }
            
            return fileElement
        } catch (e: Exception) {
            logger.error(e) { "解析 Java 文件时出错: $filePath" }
            return createFileElement(filePath, content)
        }
    }
    
    /**
     * 解析类或接口声明
     *
     * @param filePath 文件路径
     * @param declaration 类或接口声明
     * @param parent 父元素
     * @return 代码元素
     */
    private fun parseClassOrInterface(
        filePath: Path,
        declaration: ClassOrInterfaceDeclaration,
        parent: CodeElement
    ): CodeElement {
        val name = declaration.nameAsString
        val packageName = parent.metadata["package"] as? String ?: ""
        val qualifiedName = if (packageName.isNotEmpty()) "$packageName.$name" else name
        val location = createLocation(filePath, declaration)
        
        val type = if (declaration.isInterface) CodeElementType.INTERFACE else CodeElementType.CLASS
        val visibility = parseVisibility(declaration)
        val modifiers = parseModifiers(declaration)
        
        val classElement = CodeElement(
            id = UUID.randomUUID().toString(),
            name = name,
            qualifiedName = qualifiedName,
            type = type,
            location = location,
            visibility = visibility,
            modifiers = modifiers,
            parent = parent,
            language = "java"
        )
        
        // 解析文档注释
        declaration.comment.ifPresent { comment ->
            if (comment.isJavadocComment) {
                classElement.documentation = comment.content
            }
        }
        
        // 解析字段
        declaration.fields.forEach { field ->
            field.variables.forEach { variable ->
                val fieldElement = parseField(filePath, field, variable, classElement)
                classElement.addChild(fieldElement)
            }
        }
        
        // 解析构造函数
        declaration.constructors.forEach { constructor ->
            val constructorElement = parseConstructor(filePath, constructor, classElement)
            classElement.addChild(constructorElement)
        }
        
        // 解析方法
        declaration.methods.forEach { method ->
            val methodElement = parseMethod(filePath, method, classElement)
            classElement.addChild(methodElement)
        }
        
        // 解析内部类
        declaration.members.forEach { member ->
            if (member is ClassOrInterfaceDeclaration) {
                val innerClassElement = parseClassOrInterface(filePath, member, classElement)
                classElement.addChild(innerClassElement)
            }
        }
        
        // 添加元数据
        classElement.metadata["isInterface"] = declaration.isInterface
        
        // 解析继承关系
        if (declaration.extendedTypes.isNonEmpty) {
            val extendedTypes = declaration.extendedTypes.map { it.nameAsString }
            classElement.metadata["extends"] = extendedTypes
        }
        
        // 解析实现关系
        if (declaration.implementedTypes.isNonEmpty) {
            val implementedTypes = declaration.implementedTypes.map { it.nameAsString }
            classElement.metadata["implements"] = implementedTypes
        }
        
        return classElement
    }
    
    /**
     * 解析枚举声明
     *
     * @param filePath 文件路径
     * @param declaration 枚举声明
     * @param parent 父元素
     * @return 代码元素
     */
    private fun parseEnum(
        filePath: Path,
        declaration: EnumDeclaration,
        parent: CodeElement
    ): CodeElement {
        val name = declaration.nameAsString
        val packageName = parent.metadata["package"] as? String ?: ""
        val qualifiedName = if (packageName.isNotEmpty()) "$packageName.$name" else name
        val location = createLocation(filePath, declaration)
        
        val visibility = parseVisibility(declaration)
        val modifiers = parseModifiers(declaration)
        
        val enumElement = CodeElement(
            id = UUID.randomUUID().toString(),
            name = name,
            qualifiedName = qualifiedName,
            type = CodeElementType.ENUM,
            location = location,
            visibility = visibility,
            modifiers = modifiers,
            parent = parent,
            language = "java"
        )
        
        // 解析文档注释
        declaration.comment.ifPresent { comment ->
            if (comment.isJavadocComment) {
                enumElement.documentation = comment.content
            }
        }
        
        // 解析枚举常量
        declaration.entries.forEach { entry ->
            val entryName = entry.nameAsString
            val entryLocation = createLocation(filePath, entry)
            
            val entryElement = CodeElement(
                id = UUID.randomUUID().toString(),
                name = entryName,
                qualifiedName = "$qualifiedName.$entryName",
                type = CodeElementType.FIELD,
                location = entryLocation,
                visibility = Visibility.PUBLIC,
                modifiers = setOf(Modifier.STATIC, Modifier.FINAL),
                parent = enumElement,
                language = "java"
            )
            
            // 解析文档注释
            entry.comment.ifPresent { comment ->
                if (comment.isJavadocComment) {
                    entryElement.documentation = comment.content
                }
            }
            
            enumElement.addChild(entryElement)
        }
        
        // 解析方法
        declaration.methods.forEach { method ->
            val methodElement = parseMethod(filePath, method, enumElement)
            enumElement.addChild(methodElement)
        }
        
        // 解析实现关系
        if (declaration.implementedTypes.isNonEmpty) {
            val implementedTypes = declaration.implementedTypes.map { it.nameAsString }
            enumElement.metadata["implements"] = implementedTypes
        }
        
        return enumElement
    }
    
    /**
     * 解析字段
     *
     * @param filePath 文件路径
     * @param field 字段声明
     * @param variable 变量声明
     * @param parent 父元素
     * @return 代码元素
     */
    private fun parseField(
        filePath: Path,
        field: FieldDeclaration,
        variable: VariableDeclarator,
        parent: CodeElement
    ): CodeElement {
        val name = variable.nameAsString
        val qualifiedName = "${parent.qualifiedName}.$name"
        val location = createLocation(filePath, variable)
        
        val visibility = parseVisibility(field)
        val modifiers = parseModifiers(field)
        
        val fieldElement = CodeElement(
            id = UUID.randomUUID().toString(),
            name = name,
            qualifiedName = qualifiedName,
            type = CodeElementType.FIELD,
            location = location,
            visibility = visibility,
            modifiers = modifiers,
            parent = parent,
            language = "java"
        )
        
        // 解析文档注释
        field.comment.ifPresent { comment ->
            if (comment.isJavadocComment) {
                fieldElement.documentation = comment.content
            }
        }
        
        // 添加元数据
        fieldElement.metadata["type"] = field.elementType.asString()
        
        // 解析初始化表达式
        variable.initializer.ifPresent { initializer ->
            fieldElement.metadata["initializer"] = initializer.toString()
        }
        
        return fieldElement
    }
    
    /**
     * 解析构造函数
     *
     * @param filePath 文件路径
     * @param constructor 构造函数声明
     * @param parent 父元素
     * @return 代码元素
     */
    private fun parseConstructor(
        filePath: Path,
        constructor: ConstructorDeclaration,
        parent: CodeElement
    ): CodeElement {
        val name = constructor.nameAsString
        val qualifiedName = "${parent.qualifiedName}.$name"
        val location = createLocation(filePath, constructor)
        
        val visibility = parseVisibility(constructor)
        val modifiers = parseModifiers(constructor)
        
        val constructorElement = CodeElement(
            id = UUID.randomUUID().toString(),
            name = name,
            qualifiedName = qualifiedName,
            type = CodeElementType.CONSTRUCTOR,
            location = location,
            visibility = visibility,
            modifiers = modifiers,
            parent = parent,
            language = "java"
        )
        
        // 解析文档注释
        constructor.comment.ifPresent { comment ->
            if (comment.isJavadocComment) {
                constructorElement.documentation = comment.content
            }
        }
        
        // 解析参数
        constructor.parameters.forEach { parameter ->
            val parameterElement = parseParameter(filePath, parameter, constructorElement)
            constructorElement.addChild(parameterElement)
        }
        
        // 添加元数据
        constructorElement.metadata["parameters"] = constructor.parameters.map { it.nameAsString }
        
        // 解析异常
        if (constructor.thrownExceptions.isNonEmpty) {
            val exceptions = constructor.thrownExceptions.map { it.nameAsString }
            constructorElement.metadata["throws"] = exceptions
        }
        
        return constructorElement
    }
    
    /**
     * 解析方法
     *
     * @param filePath 文件路径
     * @param method 方法声明
     * @param parent 父元素
     * @return 代码元素
     */
    private fun parseMethod(
        filePath: Path,
        method: MethodDeclaration,
        parent: CodeElement
    ): CodeElement {
        val name = method.nameAsString
        val qualifiedName = "${parent.qualifiedName}.$name"
        val location = createLocation(filePath, method)
        
        val visibility = parseVisibility(method)
        val modifiers = parseModifiers(method)
        
        val methodElement = CodeElement(
            id = UUID.randomUUID().toString(),
            name = name,
            qualifiedName = qualifiedName,
            type = CodeElementType.METHOD,
            location = location,
            visibility = visibility,
            modifiers = modifiers,
            parent = parent,
            language = "java"
        )
        
        // 解析文档注释
        method.comment.ifPresent { comment ->
            if (comment.isJavadocComment) {
                methodElement.documentation = comment.content
            }
        }
        
        // 解析参数
        method.parameters.forEach { parameter ->
            val parameterElement = parseParameter(filePath, parameter, methodElement)
            methodElement.addChild(parameterElement)
        }
        
        // 添加元数据
        methodElement.metadata["returnType"] = method.type.asString()
        methodElement.metadata["parameters"] = method.parameters.map { it.nameAsString }
        
        // 解析异常
        if (method.thrownExceptions.isNonEmpty) {
            val exceptions = method.thrownExceptions.map { it.nameAsString }
            methodElement.metadata["throws"] = exceptions
        }
        
        return methodElement
    }
    
    /**
     * 解析参数
     *
     * @param filePath 文件路径
     * @param parameter 参数声明
     * @param parent 父元素
     * @return 代码元素
     */
    private fun parseParameter(
        filePath: Path,
        parameter: Parameter,
        parent: CodeElement
    ): CodeElement {
        val name = parameter.nameAsString
        val qualifiedName = "${parent.qualifiedName}.$name"
        val location = createLocation(filePath, parameter)
        
        val modifiers = parseModifiers(parameter)
        
        val parameterElement = CodeElement(
            id = UUID.randomUUID().toString(),
            name = name,
            qualifiedName = qualifiedName,
            type = CodeElementType.PARAMETER,
            location = location,
            visibility = Visibility.UNKNOWN,
            modifiers = modifiers,
            parent = parent,
            language = "java"
        )
        
        // 添加元数据
        parameterElement.metadata["type"] = parameter.type.asString()
        parameterElement.metadata["isVarArgs"] = parameter.isVarArgs
        
        return parameterElement
    }
    
    /**
     * 解析注释
     *
     * @param filePath 文件路径
     * @param comment 注释
     * @param parent 父元素
     * @return 代码元素
     */
    private fun parseComment(
        filePath: Path,
        comment: Comment,
        parent: CodeElement
    ): CodeElement {
        val content = comment.content
        val location = createLocation(filePath, comment)
        
        val commentElement = CodeElement(
            id = UUID.randomUUID().toString(),
            name = "Comment",
            qualifiedName = "${parent.qualifiedName}.Comment",
            type = CodeElementType.COMMENT,
            location = location,
            visibility = Visibility.UNKNOWN,
            parent = parent,
            language = "java"
        )
        
        // 添加元数据
        commentElement.metadata["content"] = content
        commentElement.metadata["isJavadoc"] = comment.isJavadocComment
        commentElement.metadata["isBlockComment"] = comment.isBlockComment
        commentElement.metadata["isLineComment"] = comment.isLineComment
        
        return commentElement
    }
    
    /**
     * 解析可见性
     *
     * @param node 带修饰符的节点
     * @return 可见性
     */
    private fun parseVisibility(node: NodeWithModifiers<*>): Visibility {
        return when {
            node.isPublic -> Visibility.PUBLIC
            node.isProtected -> Visibility.PROTECTED
            node.isPrivate -> Visibility.PRIVATE
            else -> Visibility.PACKAGE_PRIVATE
        }
    }
    
    /**
     * 解析修饰符
     *
     * @param node 带修饰符的节点
     * @return 修饰符集合
     */
    private fun parseModifiers(node: NodeWithModifiers<*>): Set<Modifier> {
        val modifiers = mutableSetOf<Modifier>()
        
        if (node.isStatic) modifiers.add(Modifier.STATIC)
        if (node.isFinal) modifiers.add(Modifier.FINAL)
        if (node.isAbstract) modifiers.add(Modifier.ABSTRACT)
        
        // 解析其他修饰符
        node.modifiers.forEach { modifier ->
            when (modifier.keyword) {
                JavaParserModifier.Keyword.SYNCHRONIZED -> modifiers.add(Modifier.SYNCHRONIZED)
                JavaParserModifier.Keyword.VOLATILE -> modifiers.add(Modifier.VOLATILE)
                JavaParserModifier.Keyword.TRANSIENT -> modifiers.add(Modifier.TRANSIENT)
                JavaParserModifier.Keyword.NATIVE -> modifiers.add(Modifier.NATIVE)
                JavaParserModifier.Keyword.STRICTFP -> modifiers.add(Modifier.STRICTFP)
                JavaParserModifier.Keyword.DEFAULT -> modifiers.add(Modifier.DEFAULT)
                else -> {}
            }
        }
        
        return modifiers
    }
    
    /**
     * 创建位置
     *
     * @param filePath 文件路径
     * @param node 节点
     * @return 位置
     */
    private fun createLocation(filePath: Path, node: com.github.javaparser.ast.Node): Location {
        val begin = node.begin.orElse(null)
        val end = node.end.orElse(null)
        
        val startLine = begin?.line ?: 1
        val startColumn = begin?.column ?: 1
        val endLine = end?.line ?: startLine
        val endColumn = end?.column ?: startColumn
        
        return Location(
            filePath = filePath,
            startLine = startLine,
            startColumn = startColumn,
            endLine = endLine,
            endColumn = endColumn
        )
    }
    
    /**
     * 获取支持的文件扩展名
     *
     * @return 支持的文件扩展名集合
     */
    override fun getSupportedExtensions(): Set<String> {
        return setOf("java")
    }
    
    /**
     * 获取语言名称
     *
     * @return 语言名称
     */
    override fun getLanguageName(): String {
        return "java"
    }
}
