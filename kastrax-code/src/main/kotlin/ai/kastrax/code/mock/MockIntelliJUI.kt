package ai.kastrax.code.mock

import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.event.ActionListener
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.border.Border

/**
 * 模拟IntelliJ UI组件
 */
object JBUI {
    /**
     * 创建边框
     */
    object Borders {
        /**
         * 创建空边框
         */
        fun empty(size: Int): Border {
            return empty(size, size, size, size)
        }
        
        /**
         * 创建空边框
         */
        fun empty(top: Int, left: Int, bottom: Int, right: Int): Border {
            return object : Border {
                override fun paintBorder(c: Component, g: java.awt.Graphics, x: Int, y: Int, width: Int, height: Int) {
                    // 不绘制边框
                }
                
                override fun getBorderInsets(c: Component): java.awt.Insets {
                    return java.awt.Insets(top, left, bottom, right)
                }
                
                override fun isBorderOpaque(): Boolean {
                    return false
                }
            }
        }
        
        /**
         * 创建自定义边框
         */
        fun customLine(color: Color): Border {
            return object : Border {
                override fun paintBorder(c: Component, g: java.awt.Graphics, x: Int, y: Int, width: Int, height: Int) {
                    g.color = color
                    g.drawRect(x, y, width - 1, height - 1)
                }
                
                override fun getBorderInsets(c: Component): java.awt.Insets {
                    return java.awt.Insets(1, 1, 1, 1)
                }
                
                override fun isBorderOpaque(): Boolean {
                    return false
                }
            }
        }
        
        /**
         * 创建复合边框
         */
        fun compound(outer: Border, inner: Border): Border {
            return object : Border {
                override fun paintBorder(c: Component, g: java.awt.Graphics, x: Int, y: Int, width: Int, height: Int) {
                    outer.paintBorder(c, g, x, y, width, height)
                    val insets = outer.getBorderInsets(c)
                    inner.paintBorder(c, g, x + insets.left, y + insets.top, 
                        width - insets.left - insets.right, 
                        height - insets.top - insets.bottom)
                }
                
                override fun getBorderInsets(c: Component): java.awt.Insets {
                    val outerInsets = outer.getBorderInsets(c)
                    val innerInsets = inner.getBorderInsets(c)
                    return java.awt.Insets(
                        outerInsets.top + innerInsets.top,
                        outerInsets.left + innerInsets.left,
                        outerInsets.bottom + innerInsets.bottom,
                        outerInsets.right + innerInsets.right
                    )
                }
                
                override fun isBorderOpaque(): Boolean {
                    return outer.isBorderOpaque() || inner.isBorderOpaque()
                }
            }
        }
    }
}

/**
 * 模拟JBColor
 */
object JBColor {
    val RED = Color.RED
    val GREEN = Color.GREEN
    val BLUE = Color.BLUE
    val YELLOW = Color.YELLOW
    val ORANGE = Color.ORANGE
    val BLACK = Color.BLACK
    val WHITE = Color.WHITE
    val GRAY = Color.GRAY
    val LIGHT_GRAY = Color.LIGHT_GRAY
    val DARK_GRAY = Color.DARK_GRAY
    
    /**
     * 获取边框颜色
     */
    fun border(): Color {
        return GRAY
    }
    
    /**
     * 创建颜色
     */
    fun namedColor(name: String, light: Color, dark: Color): Color {
        return light // 简化实现，始终返回light颜色
    }
}

/**
 * 模拟JBPanel
 */
open class JBPanel<T : JBPanel<T>> : JPanel {
    constructor() : super()
    constructor(layout: java.awt.LayoutManager) : super(layout)
}

/**
 * 模拟JBScrollPane
 */
class JBScrollPane : JScrollPane {
    constructor() : super()
    constructor(view: Component) : super(view)
    constructor(view: Component, vsbPolicy: Int, hsbPolicy: Int) : super(view, vsbPolicy, hsbPolicy)
}

/**
 * 模拟JBLabel
 */
class JBLabel : javax.swing.JLabel {
    constructor() : super()
    constructor(text: String) : super(text)
    constructor(icon: Icon) : super(icon)
    constructor(text: String, icon: Icon, horizontalAlignment: Int) : super(text, icon, horizontalAlignment)
}

/**
 * 模拟JBSplitter
 */
class JBSplitter : JPanel {
    constructor() : super()
    constructor(vertical: Boolean) : super()
    
    /**
     * 设置比例
     */
    fun setProportion(proportion: Float) {
        // 模拟实现
    }
    
    /**
     * 设置第一个组件
     */
    fun setFirstComponent(component: JComponent) {
        add(component)
    }
    
    /**
     * 设置第二个组件
     */
    fun setSecondComponent(component: JComponent) {
        add(component)
    }
}

/**
 * 模拟SimpleToolWindowPanel
 */
open class SimpleToolWindowPanel(vertical: Boolean) : JPanel() {
    /**
     * 设置内容
     */
    fun setContent(content: JComponent) {
        removeAll()
        add(content)
    }
    
    /**
     * 设置工具栏
     */
    fun setToolbar(toolbar: JComponent) {
        add(toolbar, "North")
    }
}

/**
 * 模拟ActionManager
 */
object ActionManager {
    /**
     * 获取实例
     */
    fun getInstance(): ActionManager {
        return this
    }
    
    /**
     * 创建操作工具栏
     */
    fun createActionToolbar(place: String, actionGroup: DefaultActionGroup, horizontal: Boolean): ActionToolbar {
        return ActionToolbar()
    }
}

/**
 * 模拟ActionToolbar
 */
class ActionToolbar : JPanel() {
    /**
     * 获取组件
     */
    fun getComponent(): JComponent {
        return this
    }
    
    /**
     * 设置目标组件
     */
    fun setTargetComponent(component: JComponent) {
        // 模拟实现
    }
}

/**
 * 模拟DefaultActionGroup
 */
class DefaultActionGroup : AnAction() {
    /**
     * 添加操作
     */
    fun add(action: AnAction) {
        // 模拟实现
    }
    
    /**
     * 添加分隔符
     */
    fun addSeparator() {
        // 模拟实现
    }
    
    /**
     * 添加分隔符
     */
    fun addSeparator(text: String) {
        // 模拟实现
    }
}

/**
 * 模拟AnAction
 */
open class AnAction {
    constructor()
    constructor(text: String, description: String, icon: Icon?) {
        // 模拟实现
    }
    
    /**
     * 执行操作
     */
    open fun actionPerformed(e: AnActionEvent) {
        // 模拟实现
    }
}

/**
 * 模拟AnActionEvent
 */
class AnActionEvent {
    /**
     * 获取项目
     */
    fun getProject(): Project? {
        return null
    }
    
    /**
     * 获取数据上下文
     */
    fun getDataContext(): DataContext {
        return DataContext()
    }
}

/**
 * 模拟DataContext
 */
class DataContext {
    /**
     * 获取数据
     */
    fun getData(dataId: String): Any? {
        return null
    }
}

/**
 * 模拟DataManager
 */
object DataManager {
    /**
     * 获取实例
     */
    fun getInstance(): DataManager {
        return this
    }
    
    /**
     * 获取数据上下文变更事件
     */
    fun getDataContextChangeEvent(): Any {
        return Any()
    }
}

/**
 * 模拟Project
 */
class Project {
    /**
     * 获取名称
     */
    fun getName(): String {
        return "MockProject"
    }
    
    /**
     * 获取基础路径
     */
    fun getBasePath(): String {
        return "/mock/project/path"
    }
}

/**
 * 模拟EditorFactory
 */
object EditorFactory {
    /**
     * 获取实例
     */
    fun getInstance(): EditorFactory {
        return this
    }
    
    /**
     * 创建文档
     */
    fun createDocument(text: String): Document {
        return Document(text)
    }
    
    /**
     * 创建编辑器
     */
    fun createEditor(document: Document): Editor {
        return Editor(document)
    }
    
    /**
     * 释放编辑器
     */
    fun releaseEditor(editor: Editor) {
        // 模拟实现
    }
}

/**
 * 模拟Document
 */
class Document(private var text: String) {
    /**
     * 获取文本
     */
    fun getText(): String {
        return text
    }
    
    /**
     * 设置文本
     */
    fun setText(text: String) {
        this.text = text
    }
    
    /**
     * 获取行数
     */
    fun getLineCount(): Int {
        return text.split("\n").size
    }
}

/**
 * 模拟Editor
 */
open class Editor(val document: Document) {
    /**
     * 获取组件
     */
    fun getComponent(): JComponent {
        return JPanel()
    }
    
    /**
     * 获取内容组件
     */
    fun getContentComponent(): JComponent {
        return JPanel()
    }
}

/**
 * 模拟EditorEx
 */
class EditorEx(document: Document) : Editor(document) {
    /**
     * 获取设置
     */
    fun getSettings(): EditorSettings {
        return EditorSettings()
    }
}

/**
 * 模拟EditorSettings
 */
class EditorSettings {
    /**
     * 设置是否显示行号
     */
    fun setLineNumbersShown(value: Boolean) {
        // 模拟实现
    }
    
    /**
     * 设置是否显示行标记区域
     */
    fun setLineMarkerAreaShown(value: Boolean) {
        // 模拟实现
    }
    
    /**
     * 设置是否显示折叠大纲
     */
    fun setFoldingOutlineShown(value: Boolean) {
        // 模拟实现
    }
    
    /**
     * 设置是否显示右边距
     */
    fun setRightMarginShown(value: Boolean) {
        // 模拟实现
    }
    
    /**
     * 设置额外的行数
     */
    fun setAdditionalLinesCount(value: Int) {
        // 模拟实现
    }
    
    /**
     * 设置额外的列数
     */
    fun setAdditionalColumnsCount(value: Int) {
        // 模拟实现
    }
}

/**
 * 模拟FileTypeManager
 */
object FileTypeManager {
    /**
     * 获取实例
     */
    fun getInstance(): FileTypeManager {
        return this
    }
    
    /**
     * 获取文件类型
     */
    fun getFileTypeByExtension(extension: String): FileType {
        return when (extension) {
            "kt", "kts" -> FileType("Kotlin")
            "java" -> FileType("Java")
            "py" -> FileType("Python")
            "js" -> FileType("JavaScript")
            "ts" -> FileType("TypeScript")
            "html" -> FileType("HTML")
            "css" -> FileType("CSS")
            "json" -> FileType("JSON")
            "xml" -> FileType("XML")
            else -> FileType("Plain Text")
        }
    }
}

/**
 * 模拟FileType
 */
class FileType(val name: String) {
    /**
     * 获取名称
     */
    fun getName(): String {
        return name
    }
}

/**
 * 模拟Icons
 */
object AllIcons {
    /**
     * 模拟General图标
     */
    object General {
        val Add = EmptyIcon()
        val Remove = EmptyIcon()
        val Settings = EmptyIcon()
        val ContextHelp = EmptyIcon()
    }
    
    /**
     * 模拟Actions图标
     */
    object Actions {
        val GC = EmptyIcon()
        val MenuSaveall = EmptyIcon()
    }
    
    /**
     * 模拟Vcs图标
     */
    object Vcs {
        val Snapshot = EmptyIcon()
    }
}

/**
 * 模拟空图标
 */
class EmptyIcon : Icon {
    override fun paintIcon(c: Component, g: java.awt.Graphics, x: Int, y: Int) {
        // 不绘制图标
    }
    
    override fun getIconWidth(): Int {
        return 16
    }
    
    override fun getIconHeight(): Int {
        return 16
    }
}

/**
 * 模拟WindowManager
 */
object WindowManager {
    /**
     * 获取实例
     */
    fun getInstance(): WindowManager {
        return this
    }
    
    /**
     * 获取窗口
     */
    fun getFrame(project: Project?): java.awt.Window? {
        return null
    }
}

/**
 * 模拟ApplicationManager
 */
object ApplicationManager {
    /**
     * 获取应用
     */
    fun getApplication(): Application {
        return Application()
    }
}

/**
 * 模拟Application
 */
class Application {
    /**
     * 在事件调度线程中执行
     */
    fun invokeLater(runnable: Runnable) {
        javax.swing.SwingUtilities.invokeLater(runnable)
    }
}
