package ai.kastrax.integrations.qwen

/**
 * Qwen 模型枚举，包含所有可用的 Qwen 模型。
 */
enum class QwenModel(val id: String) {
    // Qwen2.5 系列模型
    QWEN2_5_72B_INSTRUCT("qwen2.5-72b-instruct"),
    QWEN2_5_32B_INSTRUCT("qwen2.5-32b-instruct"),
    QWEN2_5_14B_INSTRUCT("qwen2.5-14b-instruct"),
    QWEN2_5_7B_INSTRUCT("qwen2.5-7b-instruct"),
    QWEN2_5_3B_INSTRUCT("qwen2.5-3b-instruct"),
    QWEN2_5_1_5B_INSTRUCT("qwen2.5-1.5b-instruct"),
    QWEN2_5_0_5B_INSTRUCT("qwen2.5-0.5b-instruct"),
    
    // Qwen2.5 Coder 系列模型
    QWEN2_5_CODER_32B_INSTRUCT("qwen2.5-coder-32b-instruct"),
    QWEN2_5_CODER_14B_INSTRUCT("qwen2.5-coder-14b-instruct"),
    QWEN2_5_CODER_7B_INSTRUCT("qwen2.5-coder-7b-instruct"),
    QWEN2_5_CODER_1_5B_INSTRUCT("qwen2.5-coder-1.5b-instruct"),
    
    // Qwen2.5 Math 系列模型
    QWEN2_5_MATH_72B_INSTRUCT("qwen2.5-math-72b-instruct"),
    QWEN2_5_MATH_7B_INSTRUCT("qwen2.5-math-7b-instruct"),
    QWEN2_5_MATH_1_5B_INSTRUCT("qwen2.5-math-1.5b-instruct"),
    
    // QwQ 系列模型（推理模型）
    QWQ_32B_PREVIEW("qwq-32b-preview"),
    
    // Qwen-VL 系列模型（多模态）
    QWEN_VL_MAX("qwen-vl-max"),
    QWEN_VL_PLUS("qwen-vl-plus");
    
    companion object {
        /**
         * 根据模型 ID 获取模型枚举。
         * 如果找不到匹配的预定义模型，则返回 QWEN2_5_72B_INSTRUCT。
         */
        fun fromId(id: String): QwenModel {
            return entries.find { it.id == id } ?: QWEN2_5_72B_INSTRUCT
        }
    }
}