package ai.kastrax.edutech.generation

import ai.kastrax.edutech.models.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

/**
 * 内存内容模板仓库实现
 * 
 * 用于开发和测试，生产环境应替换为数据库实现
 */
class InMemoryContentTemplateRepository : ContentTemplateRepository {
    private val templates = mutableMapOf<String, ContentTemplate>()
    private val mutex = Mutex()
    
    init {
        // 初始化一些默认模板
        initializeDefaultTemplates()
    }
    
    override suspend fun findBestTemplate(
        contentType: ContentType,
        subject: Subject,
        difficulty: DifficultyLevel
    ): ContentTemplate? {
        return mutex.withLock {
            // 首先查找完全匹配的模板
            val exactMatch = templates.values.find { template ->
                template.contentType == contentType &&
                template.subject == subject &&
                template.difficulty == difficulty
            }
            
            if (exactMatch != null) return exactMatch
            
            // 查找内容类型和学科匹配的模板
            val typeSubjectMatch = templates.values.find { template ->
                template.contentType == contentType &&
                template.subject == subject
            }
            
            if (typeSubjectMatch != null) return typeSubjectMatch
            
            // 查找内容类型匹配的模板
            val typeMatch = templates.values.find { template ->
                template.contentType == contentType
            }
            
            return typeMatch
        }
    }
    
    override suspend fun saveTemplate(template: ContentTemplate) {
        mutex.withLock {
            templates[template.id] = template
        }
    }
    
    override suspend fun getAllTemplates(): List<ContentTemplate> {
        return mutex.withLock {
            templates.values.toList()
        }
    }
    
    override suspend fun getTemplateById(id: String): ContentTemplate? {
        return mutex.withLock {
            templates[id]
        }
    }
    
    override suspend fun deleteTemplate(id: String) {
        mutex.withLock {
            templates.remove(id)
        }
    }
    
    private fun initializeDefaultTemplates() {
        val defaultTemplates = listOf(
            // 数学文本模板
            ContentTemplate(
                id = "math_text_beginner",
                name = "数学基础文本模板",
                contentType = ContentType.TEXT,
                subject = Subject.MATHEMATICS,
                difficulty = DifficultyLevel.BEGINNER,
                promptTemplate = """
                    请为数学初学者创建关于"{topic}"的教学内容：
                    
                    学习目标：{objectives}
                    难度级别：{difficulty}
                    目标受众：{audience}
                    
                    内容要求：
                    1. 使用简单易懂的语言
                    2. 从基础概念开始讲解
                    3. 提供具体的数字例子
                    4. 包含步骤分解
                    5. 添加练习提示
                    
                    请按以下结构组织内容：
                    ## 概念介绍
                    ## 基础知识
                    ## 实例演示
                    ## 练习指导
                    ## 总结要点
                """.trimIndent(),
                createdAt = Clock.System.now()
            ),
            
            // 物理视频模板
            ContentTemplate(
                id = "physics_video_intermediate",
                name = "物理中级视频模板",
                contentType = ContentType.VIDEO,
                subject = Subject.PHYSICS,
                difficulty = DifficultyLevel.INTERMEDIATE,
                promptTemplate = """
                    请为"{topic}"创建一个物理教学视频脚本：
                    
                    学习目标：{objectives}
                    难度级别：{difficulty}
                    目标受众：{audience}
                    
                    视频脚本要求：
                    1. 总时长8-10分钟
                    2. 包含实验演示说明
                    3. 使用生动的比喻
                    4. 包含视觉提示
                    5. 互动问题穿插
                    
                    脚本结构：
                    [00:00-00:30] 开场和目标介绍
                    [00:30-02:00] 理论基础讲解
                    [02:00-05:00] 实验演示和分析
                    [05:00-07:30] 实际应用举例
                    [07:30-08:30] 总结和思考问题
                    [08:30-09:00] 预告下节内容
                    
                    请为每个时间段提供详细的讲解内容和视觉提示。
                """.trimIndent(),
                createdAt = Clock.System.now()
            ),
            
            // 计算机科学交互式模板
            ContentTemplate(
                id = "cs_interactive_advanced",
                name = "计算机科学高级交互模板",
                contentType = ContentType.INTERACTIVE,
                subject = Subject.COMPUTER_SCIENCE,
                difficulty = DifficultyLevel.ADVANCED,
                promptTemplate = """
                    请为"{topic}"设计一个高级计算机科学交互式学习活动：
                    
                    学习目标：{objectives}
                    难度级别：{difficulty}
                    目标受众：{audience}
                    
                    交互活动要求：
                    1. 包含代码实践环节
                    2. 设计算法挑战
                    3. 提供即时反馈
                    4. 包含调试练习
                    5. 设置进阶任务
                    
                    活动结构：
                    ## 知识回顾 (5分钟)
                    - 快速测验
                    - 概念检查
                    
                    ## 编程实践 (15分钟)
                    - 基础代码练习
                    - 逐步指导
                    
                    ## 算法挑战 (20分钟)
                    - 问题分析
                    - 解决方案设计
                    - 代码实现
                    
                    ## 调试训练 (10分钟)
                    - 错误识别
                    - 修复练习
                    
                    ## 扩展探索 (10分钟)
                    - 优化思考
                    - 变体问题
                    
                    请为每个环节提供具体的活动内容和评估标准。
                """.trimIndent(),
                createdAt = Clock.System.now()
            ),
            
            // 语言艺术论文模板
            ContentTemplate(
                id = "language_essay_intermediate",
                name = "语言艺术中级论文模板",
                contentType = ContentType.TEXT,
                subject = Subject.LANGUAGE_ARTS,
                difficulty = DifficultyLevel.INTERMEDIATE,
                promptTemplate = """
                    请为"{topic}"创建一篇语言艺术教学文章：
                    
                    学习目标：{objectives}
                    难度级别：{difficulty}
                    目标受众：{audience}
                    
                    文章要求：
                    1. 文学性与教育性并重
                    2. 包含文本分析示例
                    3. 提供写作技巧指导
                    4. 激发创造性思维
                    5. 连接现实生活
                    
                    文章结构：
                    ## 引言：文学的魅力
                    ## 核心概念解析
                    ## 经典作品赏析
                    ## 写作技巧分享
                    ## 创作实践指导
                    ## 思考与讨论
                    ## 延伸阅读推荐
                    
                    请确保内容既有深度又有趣味性，适合中级学习者。
                """.trimIndent(),
                createdAt = Clock.System.now()
            ),
            
            // 历史多媒体模板
            ContentTemplate(
                id = "history_multimedia_beginner",
                name = "历史基础多媒体模板",
                contentType = ContentType.VIDEO,
                subject = Subject.HISTORY,
                difficulty = DifficultyLevel.BEGINNER,
                promptTemplate = """
                    请为"{topic}"创建一个历史教学多媒体内容：
                    
                    学习目标：{objectives}
                    难度级别：{difficulty}
                    目标受众：{audience}
                    
                    多媒体内容要求：
                    1. 故事化叙述方式
                    2. 时间线清晰展示
                    3. 人物形象生动
                    4. 历史背景详实
                    5. 现代意义阐释
                    
                    内容框架：
                    ## 历史背景设定
                    - 时代特征
                    - 社会环境
                    
                    ## 关键事件叙述
                    - 事件起因
                    - 发展过程
                    - 重要转折
                    
                    ## 人物故事讲述
                    - 主要人物介绍
                    - 人物关系网络
                    - 个人选择影响
                    
                    ## 历史意义分析
                    - 当时影响
                    - 长远意义
                    - 现代启示
                    
                    ## 互动思考环节
                    - 假设性问题
                    - 价值观讨论
                    
                    请用生动的语言和丰富的细节来呈现历史的魅力。
                """.trimIndent(),
                createdAt = Clock.System.now()
            )
        )
        
        defaultTemplates.forEach { template ->
            templates[template.id] = template
        }
    }
}
