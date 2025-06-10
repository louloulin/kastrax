-- Kastrax教育科技AI解决方案示例数据插入脚本
-- 版本: V002
-- 描述: 插入示例教育数据用于开发和测试
-- 基于: ed2.md第3.3节用户体验流程

-- ============================================================================
-- 示例教师数据
-- ============================================================================

INSERT INTO teachers (id, name, email, specialization, certification_level, hire_date, status) VALUES
('teacher_001', '李明', 'liming@school.edu.cn', ARRAY['数学', '物理'], '高级教师', '2020-09-01', 'ACTIVE'),
('teacher_002', '王芳', 'wangfang@school.edu.cn', ARRAY['语文', '历史'], '一级教师', '2019-08-15', 'ACTIVE'),
('teacher_003', '张伟', 'zhangwei@school.edu.cn', ARRAY['英语'], '二级教师', '2021-03-10', 'ACTIVE'),
('teacher_004', '刘红', 'liuhong@school.edu.cn', ARRAY['化学', '生物'], '高级教师', '2018-07-20', 'ACTIVE'),
('teacher_005', '陈强', 'chenqiang@school.edu.cn', ARRAY['计算机科学'], '一级教师', '2022-01-15', 'ACTIVE');

-- ============================================================================
-- 示例班级数据
-- ============================================================================

INSERT INTO classrooms (id, name, teacher_id, grade_level, subject, max_students, academic_year, status) VALUES
('classroom_001', '八年级1班数学', 'teacher_001', 'GRADE_8', '数学', 35, '2024-2025', 'ACTIVE'),
('classroom_002', '八年级2班数学', 'teacher_001', 'GRADE_8', '数学', 35, '2024-2025', 'ACTIVE'),
('classroom_003', '九年级1班语文', 'teacher_002', 'GRADE_9', '语文', 32, '2024-2025', 'ACTIVE'),
('classroom_004', '七年级1班英语', 'teacher_003', 'GRADE_7', '英语', 30, '2024-2025', 'ACTIVE'),
('classroom_005', '九年级2班化学', 'teacher_004', 'GRADE_9', '化学', 28, '2024-2025', 'ACTIVE'),
('classroom_006', '高一1班计算机', 'teacher_005', 'GRADE_10', '计算机科学', 25, '2024-2025', 'ACTIVE');

-- ============================================================================
-- 示例学生数据
-- ============================================================================

INSERT INTO students (id, name, email, grade_level, enrollment_date, status) VALUES
('student_001', '张三', 'zhangsan@student.edu.cn', 'GRADE_8', '2024-09-01', 'ACTIVE'),
('student_002', '李四', 'lisi@student.edu.cn', 'GRADE_8', '2024-09-01', 'ACTIVE'),
('student_003', '王五', 'wangwu@student.edu.cn', 'GRADE_8', '2024-09-01', 'ACTIVE'),
('student_004', '赵六', 'zhaoliu@student.edu.cn', 'GRADE_9', '2024-09-01', 'ACTIVE'),
('student_005', '钱七', 'qianqi@student.edu.cn', 'GRADE_9', '2024-09-01', 'ACTIVE'),
('student_006', '孙八', 'sunba@student.edu.cn', 'GRADE_7', '2024-09-01', 'ACTIVE'),
('student_007', '周九', 'zhoujiu@student.edu.cn', 'GRADE_7', '2024-09-01', 'ACTIVE'),
('student_008', '吴十', 'wushi@student.edu.cn', 'GRADE_10', '2024-09-01', 'ACTIVE'),
('student_009', '郑一', 'zhengyi@student.edu.cn', 'GRADE_10', '2024-09-01', 'ACTIVE'),
('student_010', '王二', 'wanger@student.edu.cn', 'GRADE_8', '2024-09-01', 'ACTIVE');

-- ============================================================================
-- 学生班级注册关系
-- ============================================================================

INSERT INTO student_classroom_enrollments (student_id, classroom_id, enrollment_date, status) VALUES
('student_001', 'classroom_001', '2024-09-01', 'ENROLLED'),
('student_002', 'classroom_001', '2024-09-01', 'ENROLLED'),
('student_003', 'classroom_002', '2024-09-01', 'ENROLLED'),
('student_010', 'classroom_002', '2024-09-01', 'ENROLLED'),
('student_004', 'classroom_003', '2024-09-01', 'ENROLLED'),
('student_005', 'classroom_005', '2024-09-01', 'ENROLLED'),
('student_006', 'classroom_004', '2024-09-01', 'ENROLLED'),
('student_007', 'classroom_004', '2024-09-01', 'ENROLLED'),
('student_008', 'classroom_006', '2024-09-01', 'ENROLLED'),
('student_009', 'classroom_006', '2024-09-01', 'ENROLLED');

-- ============================================================================
-- 示例课程数据
-- ============================================================================

INSERT INTO courses (id, title, description, subject, grade_level, difficulty_level, estimated_duration, learning_objectives, prerequisites, status, created_by) VALUES
('course_001', '基础代数', '学习代数基础概念和运算方法', '数学', 'GRADE_8', 'BEGINNER', 1200, ARRAY['理解代数表达式', '掌握一元一次方程求解', '应用代数解决实际问题'], ARRAY['基础算术'], 'ACTIVE', 'teacher_001'),
('course_002', '几何入门', '平面几何基础知识和证明方法', '数学', 'GRADE_8', 'BEGINNER', 1500, ARRAY['理解几何图形性质', '掌握几何证明方法', '计算面积和周长'], ARRAY['基础算术'], 'ACTIVE', 'teacher_001'),
('course_003', '现代文阅读', '提高现代文阅读理解和分析能力', '语文', 'GRADE_9', 'INTERMEDIATE', 1800, ARRAY['理解文章主旨', '分析写作手法', '提高阅读速度'], ARRAY['基础语文'], 'ACTIVE', 'teacher_002'),
('course_004', '英语语法基础', '掌握英语基础语法规则', '英语', 'GRADE_7', 'BEGINNER', 1000, ARRAY['理解时态概念', '掌握句型结构', '提高语法准确性'], ARRAY['基础英语词汇'], 'ACTIVE', 'teacher_003'),
('course_005', '化学反应原理', '学习化学反应的基本原理和规律', '化学', 'GRADE_9', 'INTERMEDIATE', 2000, ARRAY['理解化学反应机理', '掌握反应方程式', '应用化学知识'], ARRAY['基础化学'], 'ACTIVE', 'teacher_004'),
('course_006', 'Python编程入门', '学习Python编程基础语法和应用', '计算机科学', 'GRADE_10', 'BEGINNER', 2400, ARRAY['掌握Python语法', '理解编程思维', '开发简单程序'], ARRAY['计算机基础'], 'ACTIVE', 'teacher_005');

-- ============================================================================
-- 示例学习活动数据
-- ============================================================================

INSERT INTO learning_activities (id, course_id, title, activity_type, topic, content, difficulty_level, estimated_duration, skills_involved, instructions, resources, status) VALUES
('activity_001', 'course_001', '代数表达式练习', 'EXERCISE', '代数表达式', '练习代数表达式的化简和计算', 'BEGINNER', 30, ARRAY['逻辑推理', '数学计算'], '完成10道代数表达式化简题目', '{"materials": ["练习册", "计算器"], "tools": ["在线计算器"]}', 'ACTIVE'),
('activity_002', 'course_001', '一元一次方程求解', 'PRACTICE', '方程求解', '学习和练习一元一次方程的求解方法', 'BEGINNER', 45, ARRAY['逻辑推理', '问题解决'], '学习求解步骤，完成练习题', '{"materials": ["教材", "练习册"], "videos": ["求解方法视频"]}', 'ACTIVE'),
('activity_003', 'course_002', '三角形性质探索', 'INTERACTIVE', '三角形', '通过互动探索三角形的基本性质', 'BEGINNER', 40, ARRAY['空间想象', '逻辑推理'], '使用几何软件探索三角形性质', '{"software": ["几何画板"], "materials": ["测量工具"]}', 'ACTIVE'),
('activity_004', 'course_003', '散文阅读理解', 'READING', '散文阅读', '阅读经典散文并分析写作技巧', 'INTERMEDIATE', 50, ARRAY['阅读理解', '文学分析'], '仔细阅读文章，回答理解问题', '{"texts": ["朱自清散文集"], "questions": ["理解题目"]}', 'ACTIVE'),
('activity_005', 'course_004', '现在时态练习', 'QUIZ', '英语时态', '测试对现在时态的理解和应用', 'BEGINNER', 25, ARRAY['语言理解', '语法应用'], '完成时态选择题和填空题', '{"materials": ["语法练习册"], "audio": ["听力材料"]}', 'ACTIVE'),
('activity_006', 'course_006', 'Python变量和数据类型', 'CODING', 'Python基础', '学习Python中的变量定义和数据类型', 'BEGINNER', 60, ARRAY['逻辑思维', '编程能力'], '编写代码练习变量操作', '{"ide": ["Python IDLE"], "materials": ["编程教材"]}', 'ACTIVE');

-- ============================================================================
-- 示例学习档案数据
-- ============================================================================

INSERT INTO learning_profiles (student_id, learning_style, cognitive_abilities, motivation_profile, learning_preferences, accessibility_needs) VALUES
('student_001', 'VISUAL', 
 '{"workingMemoryCapacity": 7, "processingSpeed": 6, "attentionSpan": 8, "logicalReasoning": 7, "spatialAbility": 8}',
 '{"intrinsicMotivation": 8, "extrinsicMotivation": 6, "goalOrientation": "MASTERY", "competitiveness": 5, "persistenceLevel": 9}',
 '{"preferredDifficulty": "INTERMEDIATE", "preferredContentTypes": ["VIDEO", "INTERACTIVE"], "preferredSessionDuration": 45, "preferredTimeOfDay": "MORNING", "feedbackFrequency": "IMMEDIATE"}',
 '{"visualImpairment": false, "hearingImpairment": false, "motorImpairment": false, "cognitiveSupport": false, "languageSupport": false}'
),
('student_002', 'AUDITORY',
 '{"workingMemoryCapacity": 6, "processingSpeed": 7, "attentionSpan": 6, "logicalReasoning": 8, "spatialAbility": 5}',
 '{"intrinsicMotivation": 7, "extrinsicMotivation": 7, "goalOrientation": "PERFORMANCE", "competitiveness": 8, "persistenceLevel": 6}',
 '{"preferredDifficulty": "BEGINNER", "preferredContentTypes": ["AUDIO", "TEXT"], "preferredSessionDuration": 30, "preferredTimeOfDay": "AFTERNOON", "feedbackFrequency": "PERIODIC"}',
 '{"visualImpairment": false, "hearingImpairment": false, "motorImpairment": false, "cognitiveSupport": false, "languageSupport": false}'
),
('student_003', 'KINESTHETIC',
 '{"workingMemoryCapacity": 8, "processingSpeed": 8, "attentionSpan": 7, "logicalReasoning": 6, "spatialAbility": 9}',
 '{"intrinsicMotivation": 9, "extrinsicMotivation": 5, "goalOrientation": "MASTERY", "competitiveness": 4, "persistenceLevel": 8}',
 '{"preferredDifficulty": "INTERMEDIATE", "preferredContentTypes": ["INTERACTIVE", "HANDS_ON"], "preferredSessionDuration": 60, "preferredTimeOfDay": "MORNING", "feedbackFrequency": "IMMEDIATE"}',
 '{"visualImpairment": false, "hearingImpairment": false, "motorImpairment": false, "cognitiveSupport": false, "languageSupport": false}'
);

-- ============================================================================
-- 示例评估数据
-- ============================================================================

INSERT INTO assessments (id, title, assessment_type, subject, difficulty_level, questions, scoring_criteria, time_limit, max_attempts, status, created_by) VALUES
('assessment_001', '代数基础测试', 'QUIZ', '数学', 'BEGINNER',
 '{"questions": [{"id": 1, "type": "multiple_choice", "question": "化简表达式 2x + 3x", "options": ["5x", "6x", "5x²", "6x²"], "correct": "5x"}, {"id": 2, "type": "short_answer", "question": "求解方程 2x + 5 = 11", "correct": "x = 3"}]}',
 '{"total_points": 100, "question_weights": [50, 50], "passing_score": 70}',
 30, 2, 'ACTIVE', 'teacher_001'),
('assessment_002', '几何图形识别', 'INTERACTIVE', '数学', 'BEGINNER',
 '{"questions": [{"id": 1, "type": "drag_drop", "question": "将图形拖拽到正确的分类中", "shapes": ["triangle", "square", "circle"], "categories": ["三角形", "四边形", "圆形"]}]}',
 '{"total_points": 100, "question_weights": [100], "passing_score": 80}',
 20, 3, 'ACTIVE', 'teacher_001'),
('assessment_003', '现代文理解测试', 'READING_COMPREHENSION', '语文', 'INTERMEDIATE',
 '{"passage": "春天来了，万物复苏...", "questions": [{"id": 1, "type": "multiple_choice", "question": "文章的主要情感色彩是？", "options": ["喜悦", "忧伤", "平静", "激动"], "correct": "喜悦"}]}',
 '{"total_points": 100, "question_weights": [100], "passing_score": 75}',
 45, 1, 'ACTIVE', 'teacher_002');

-- ============================================================================
-- 示例学习会话数据
-- ============================================================================

INSERT INTO learning_sessions (id, student_id, course_id, classroom_id, start_time, end_time, status, objectives, initial_context, session_metrics) VALUES
('session_001', 'student_001', 'course_001', 'classroom_001', '2024-12-19 09:00:00', '2024-12-19 09:45:00', 'COMPLETED',
 ARRAY['掌握代数表达式化简', '理解变量概念'],
 '{"previous_knowledge": "基础算术", "learning_style": "VISUAL", "difficulty_preference": "INTERMEDIATE"}',
 '{"activities_completed": 2, "total_time_spent": 2700, "average_performance": 85.5, "engagement_score": 8.2, "help_requests": 1}'
),
('session_002', 'student_002', 'course_001', 'classroom_001', '2024-12-19 10:00:00', '2024-12-19 10:30:00', 'COMPLETED',
 ARRAY['练习一元一次方程求解'],
 '{"previous_knowledge": "基础算术", "learning_style": "AUDITORY", "difficulty_preference": "BEGINNER"}',
 '{"activities_completed": 1, "total_time_spent": 1800, "average_performance": 78.0, "engagement_score": 7.5, "help_requests": 2}'
),
('session_003', 'student_003', 'course_002', 'classroom_002', '2024-12-19 14:00:00', NULL, 'ACTIVE',
 ARRAY['探索三角形性质', '理解几何证明'],
 '{"previous_knowledge": "基础几何", "learning_style": "KINESTHETIC", "difficulty_preference": "INTERMEDIATE"}',
 '{"activities_completed": 0, "total_time_spent": 0, "average_performance": 0, "engagement_score": 0, "help_requests": 0}'
);

-- ============================================================================
-- 示例学习活动记录
-- ============================================================================

INSERT INTO learning_activity_records (session_id, activity_id, start_time, end_time, performance_score, completion_status, feedback, time_spent, attempts, hints_used) VALUES
('session_001', 'activity_001', '2024-12-19 09:00:00', '2024-12-19 09:25:00', 88.0, 'COMPLETED', '表现优秀！对代数表达式的理解很好，继续保持。', 1500, 1, 0),
('session_001', 'activity_002', '2024-12-19 09:25:00', '2024-12-19 09:45:00', 83.0, 'COMPLETED', '方程求解方法掌握良好，建议多练习复杂方程。', 1200, 1, 1),
('session_002', 'activity_002', '2024-12-19 10:00:00', '2024-12-19 10:30:00', 78.0, 'COMPLETED', '基础掌握不错，需要加强练习提高熟练度。', 1800, 2, 2);

-- ============================================================================
-- 示例个性化学习计划
-- ============================================================================

INSERT INTO personalized_learning_plans (id, student_id, plan_type, learning_goals, recommended_activities, next_recommendations, upcoming_milestones, adaptation_history, effectiveness_metrics, valid_from, valid_until, status) VALUES
('plan_001', 'student_001', 'ADAPTIVE',
 '{"short_term": [{"goal": "掌握代数基础", "target_date": "2024-12-31", "progress": 65}], "long_term": [{"goal": "完成八年级数学课程", "target_date": "2025-06-30", "progress": 25}]}',
 '{"current": ["activity_002", "activity_003"], "upcoming": ["activity_004", "activity_005"]}',
 '{"immediate": [{"activity": "几何证明练习", "reason": "基于当前进度推荐", "priority": "HIGH"}], "suggested": [{"activity": "代数应用题", "reason": "巩固已学知识", "priority": "MEDIUM"}]}',
 '{"next_week": [{"milestone": "完成代数单元测试", "date": "2024-12-26"}], "next_month": [{"milestone": "开始几何学习", "date": "2025-01-15"}]}',
 '{"adaptations": [{"date": "2024-12-19", "reason": "学习风格偏好调整", "changes": ["增加视觉化内容"]}]}',
 '{"engagement_improvement": 15, "performance_improvement": 12, "time_efficiency": 8}',
 '2024-12-19 00:00:00', '2025-01-19 00:00:00', 'ACTIVE'
);

-- ============================================================================
-- 示例分析数据
-- ============================================================================

INSERT INTO learning_analytics (student_id, analysis_type, analysis_date, time_period_start, time_period_end, metrics, insights, recommendations, confidence_score) VALUES
('student_001', 'PERFORMANCE_ANALYSIS', '2024-12-19 18:00:00', '2024-12-12 00:00:00', '2024-12-19 23:59:59',
 '{"average_score": 85.5, "completion_rate": 100, "time_spent": 3600, "activities_completed": 3, "improvement_rate": 12}',
 '{"strengths": ["视觉学习能力强", "逻辑推理能力好"], "weaknesses": ["需要更多练习时间"], "learning_patterns": ["上午学习效率最高", "偏好互动式内容"]}',
 '{"immediate": ["增加几何可视化练习"], "short_term": ["保持当前学习节奏"], "long_term": ["考虑提高难度等级"]}',
 0.87
),
('student_002', 'ENGAGEMENT_ANALYSIS', '2024-12-19 18:00:00', '2024-12-12 00:00:00', '2024-12-19 23:59:59',
 '{"engagement_score": 7.5, "session_duration": 1800, "help_requests": 2, "completion_rate": 100, "return_frequency": 5}',
 '{"engagement_factors": ["听觉学习偏好", "需要更多指导"], "attention_patterns": ["注意力持续时间较短"], "motivation_indicators": ["外在激励敏感"]}',
 '{"immediate": ["提供更多音频内容"], "short_term": ["增加即时反馈"], "long_term": ["建立奖励机制"]}',
 0.82
);
