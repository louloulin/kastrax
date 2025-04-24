package ai.kastrax.core.workflow.visualization

/**
 * Represents the format for workflow visualization.
 */
enum class VisualizationFormat {
    /**
     * DOT format for use with Graphviz.
     */
    DOT,
    
    /**
     * Mermaid format for use in Markdown documents.
     */
    MERMAID,
    
    /**
     * JSON format for use with custom visualization tools.
     */
    JSON,
    
    /**
     * Plain text format for simple visualization.
     */
    TEXT
}
