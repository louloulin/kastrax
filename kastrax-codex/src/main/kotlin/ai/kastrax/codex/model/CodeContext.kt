package ai.kastrax.codex.model

/**
 * 代码上下文模型，用于将IDE上下文转换为智能体输入
 *
 * @property fileName 文件名
 * @property language 编程语言
 * @property code 完整代码
 * @property selection 选中的代码（可选）
 * @property task 任务描述（可选）
 * @property projectStructure 项目结构信息（可选）
 * @property imports 导入语句（可选）
 * @property dependencies 项目依赖（可选）
 * @property metadata 元数据（可选）
 */
data class CodeContext(
    val fileName: String,
    val language: String,
    val code: String,
    val selection: String = "",
    val task: String = "",
    val projectStructure: ProjectStructure? = null,
    val imports: List<String> = emptyList(),
    val dependencies: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)

/**
 * 项目结构信息
 *
 * @property rootPath 项目根路径
 * @property modules 项目模块
 * @property sourceDirectories 源代码目录
 * @property testDirectories 测试目录
 * @property resourceDirectories 资源目录
 */
data class ProjectStructure(
    val rootPath: String,
    val modules: List<Module> = emptyList(),
    val sourceDirectories: List<String> = emptyList(),
    val testDirectories: List<String> = emptyList(),
    val resourceDirectories: List<String> = emptyList()
)

/**
 * 项目模块
 *
 * @property name 模块名称
 * @property path 模块路径
 * @property dependencies 模块依赖
 */
data class Module(
    val name: String,
    val path: String,
    val dependencies: List<String> = emptyList()
)

/**
 * 代码上下文构建器
 */
class CodeContextBuilder {
    private var fileName: String = ""
    private var language: String = ""
    private var code: String = ""
    private var selection: String = ""
    private var task: String = ""
    private var projectStructure: ProjectStructure? = null
    private var imports: MutableList<String> = mutableListOf()
    private var dependencies: MutableList<String> = mutableListOf()
    private var metadata: MutableMap<String, String> = mutableMapOf()
    
    /**
     * 设置文件名
     */
    fun fileName(fileName: String) = apply { this.fileName = fileName }
    
    /**
     * 设置编程语言
     */
    fun language(language: String) = apply { this.language = language }
    
    /**
     * 设置完整代码
     */
    fun code(code: String) = apply { this.code = code }
    
    /**
     * 设置选中的代码
     */
    fun selection(selection: String) = apply { this.selection = selection }
    
    /**
     * 设置任务描述
     */
    fun task(task: String) = apply { this.task = task }
    
    /**
     * 设置项目结构信息
     */
    fun projectStructure(projectStructure: ProjectStructure) = apply { this.projectStructure = projectStructure }
    
    /**
     * 添加导入语句
     */
    fun addImport(import: String) = apply { this.imports.add(import) }
    
    /**
     * 设置导入语句列表
     */
    fun imports(imports: List<String>) = apply { this.imports = imports.toMutableList() }
    
    /**
     * 添加项目依赖
     */
    fun addDependency(dependency: String) = apply { this.dependencies.add(dependency) }
    
    /**
     * 设置项目依赖列表
     */
    fun dependencies(dependencies: List<String>) = apply { this.dependencies = dependencies.toMutableList() }
    
    /**
     * 添加元数据
     */
    fun addMetadata(key: String, value: String) = apply { this.metadata[key] = value }
    
    /**
     * 设置元数据
     */
    fun metadata(metadata: Map<String, String>) = apply { this.metadata = metadata.toMutableMap() }
    
    /**
     * 构建代码上下文
     */
    fun build(): CodeContext {
        require(fileName.isNotEmpty()) { "文件名不能为空" }
        require(language.isNotEmpty()) { "编程语言不能为空" }
        require(code.isNotEmpty()) { "代码不能为空" }
        
        return CodeContext(
            fileName = fileName,
            language = language,
            code = code,
            selection = selection,
            task = task,
            projectStructure = projectStructure,
            imports = imports,
            dependencies = dependencies,
            metadata = metadata
        )
    }
}
