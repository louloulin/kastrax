package ai.kastrax.code.ui.components

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.JToolBar

/**
 * 代码展示面板
 * 
 * 显示代码并提供操作按钮
 */
class CodeDisplayPanel(
    private val project: Project
) : JBPanel<CodeDisplayPanel>(BorderLayout()) {
    
    private val editorFactory = EditorFactory.getInstance()
    private var editor: EditorEx? = null
    private val languageComboBox = JComboBox(arrayOf("kotlin", "java", "python", "javascript", "typescript", "html", "css", "json", "xml"))
    
    init {
        border = JBUI.Borders.empty(8)
        
        // 创建工具栏
        val toolbar = createToolbar()
        add(toolbar, BorderLayout.NORTH)
        
        // 创建编辑器
        createEditor("kotlin", "// 代码将显示在这里")
        
        // 语言选择事件
        languageComboBox.addActionListener {
            val language = languageComboBox.selectedItem as String
            val currentText = editor?.document?.text ?: ""
            createEditor(language, currentText)
        }
    }
    
    /**
     * 创建工具栏
     */
    private fun createToolbar(): JPanel {
        val toolbar = JToolBar()
        toolbar.isFloatable = false
        toolbar.border = JBUI.Borders.empty(0, 0, 8, 0)
        
        // 语言选择
        toolbar.add(languageComboBox)
        
        // 复制按钮
        val copyButton = JButton("复制")
        copyButton.addActionListener {
            val text = editor?.document?.text
            if (!text.isNullOrEmpty()) {
                val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
                val selection = java.awt.datatransfer.StringSelection(text)
                clipboard.setContents(selection, null)
            }
        }
        toolbar.add(copyButton)
        
        // 插入到编辑器按钮
        val insertButton = JButton("插入到编辑器")
        insertButton.addActionListener {
            // 这里需要实现插入到当前编辑器的逻辑
            // 暂时留空，后续实现
        }
        toolbar.add(insertButton)
        
        // 清空按钮
        val clearButton = JButton("清空")
        clearButton.addActionListener {
            setCode("")
        }
        toolbar.add(clearButton)
        
        val panel = JPanel(BorderLayout())
        panel.add(toolbar, BorderLayout.CENTER)
        return panel
    }
    
    /**
     * 创建编辑器
     */
    private fun createEditor(language: String, initialText: String) {
        // 释放旧编辑器
        disposeEditor()
        
        // 获取文件类型
        val fileType = when (language.lowercase()) {
            "kotlin" -> FileTypeManager.getInstance().findFileTypeByName("Kotlin")
            "java" -> FileTypeManager.getInstance().findFileTypeByName("JAVA")
            "python" -> FileTypeManager.getInstance().findFileTypeByName("Python")
            "javascript", "js" -> FileTypeManager.getInstance().findFileTypeByName("JavaScript")
            "typescript", "ts" -> FileTypeManager.getInstance().findFileTypeByName("TypeScript")
            "html" -> FileTypeManager.getInstance().findFileTypeByName("HTML")
            "css" -> FileTypeManager.getInstance().findFileTypeByName("CSS")
            "json" -> FileTypeManager.getInstance().findFileTypeByName("JSON")
            "xml" -> FileTypeManager.getInstance().findFileTypeByName("XML")
            else -> FileTypeManager.getInstance().findFileTypeByName("PLAIN_TEXT")
        } ?: FileTypeManager.getInstance().findFileTypeByName("PLAIN_TEXT")
        
        // 创建文档
        val document = editorFactory.createDocument(initialText)
        
        // 创建编辑器
        editor = editorFactory.createEditor(document, project, fileType, false) as EditorEx
        editor?.let {
            // 设置编辑器选项
            it.settings.isLineNumbersShown = true
            it.settings.isLineMarkerAreaShown = true
            it.settings.isFoldingOutlineShown = true
            it.settings.isRightMarginShown = true
            it.settings.isWhitespacesShown = true
            
            // 添加编辑器到面板
            removeAll()
            add(createToolbar(), BorderLayout.NORTH)
            add(JBScrollPane(it.component), BorderLayout.CENTER)
            revalidate()
            repaint()
        }
    }
    
    /**
     * 设置代码
     */
    fun setCode(code: String) {
        editor?.document?.setText(code)
    }
    
    /**
     * 设置代码和语言
     */
    fun setCode(code: String, language: String) {
        languageComboBox.selectedItem = language
        createEditor(language, code)
    }
    
    /**
     * 获取代码
     */
    fun getCode(): String {
        return editor?.document?.text ?: ""
    }
    
    /**
     * 获取语言
     */
    fun getLanguage(): String {
        return languageComboBox.selectedItem as String
    }
    
    /**
     * 释放编辑器
     */
    private fun disposeEditor() {
        editor?.let {
            editorFactory.releaseEditor(it)
        }
        editor = null
    }
    
    /**
     * 释放资源
     */
    fun dispose() {
        disposeEditor()
    }
}
