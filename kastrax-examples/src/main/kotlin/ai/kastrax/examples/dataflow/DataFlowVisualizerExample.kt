package ai.kastrax.examples.dataflow

import ai.kastrax.core.workflow.dataflow.visualization.DataFlowVisualizer
import java.io.File

/**
 * 数据流可视化示例，展示如何使用数据流可视化工具。
 */
class DataFlowVisualizerExample {

    /**
     * 演示数据流可视化。
     */
    fun demonstrateDataFlowVisualization() {
        val visualizer = DataFlowVisualizer()
        
        // 生成简单的Mermaid图表
        val mermaidDiagram = """
            graph TD
                A[开始] --> B[处理数据]
                B --> C{判断条件}
                C -->|条件为真| D[处理1]
                C -->|条件为假| E[处理2]
                D --> F[结束]
                E --> F
        """.trimIndent()
        
        // 保存到文件
        val outputDir = File("examples")
        outputDir.mkdirs()
        
        val outputFile = File(outputDir, "simple_diagram.mmd")
        outputFile.writeText(mermaidDiagram)
        
        println("Mermaid图表已保存到: ${outputFile.absolutePath}")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val example = DataFlowVisualizerExample()
            example.demonstrateDataFlowVisualization()
        }
    }
}
