-- Kastrax教育科技AI解决方案数据库迁移脚本
-- 版本: V001
-- 描述: 创建教育系统核心数据表
-- 基于: ed2.md第3.2节数据实体设计

-- ============================================================================
-- 用户和身份管理表
-- ============================================================================

-- 学生表
CREATE TABLE IF NOT EXISTS students (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    grade_level VARCHAR(50) NOT NULL,
    enrollment_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 教师表
CREATE TABLE IF NOT EXISTS teachers (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    specialization TEXT[],
    certification_level VARCHAR(100),
    hire_date TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 班级表
CREATE TABLE IF NOT EXISTS classrooms (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    teacher_id VARCHAR(255) NOT NULL REFERENCES teachers(id),
    grade_level VARCHAR(50) NOT NULL,
    subject VARCHAR(100) NOT NULL,
    max_students INTEGER DEFAULT 30,
    academic_year VARCHAR(20) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 学生班级关联表
CREATE TABLE IF NOT EXISTS student_classroom_enrollments (
    id SERIAL PRIMARY KEY,
    student_id VARCHAR(255) NOT NULL REFERENCES students(id),
    classroom_id VARCHAR(255) NOT NULL REFERENCES classrooms(id),
    enrollment_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'ENROLLED',
    UNIQUE(student_id, classroom_id)
);

-- ============================================================================
-- 学习内容和课程管理表
-- ============================================================================

-- 课程表
CREATE TABLE IF NOT EXISTS courses (
    id VARCHAR(255) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    subject VARCHAR(100) NOT NULL,
    grade_level VARCHAR(50) NOT NULL,
    difficulty_level VARCHAR(50) NOT NULL,
    estimated_duration INTEGER, -- 分钟
    learning_objectives TEXT[],
    prerequisites TEXT[],
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_by VARCHAR(255) REFERENCES teachers(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 学习活动表
CREATE TABLE IF NOT EXISTS learning_activities (
    id VARCHAR(255) PRIMARY KEY,
    course_id VARCHAR(255) REFERENCES courses(id),
    title VARCHAR(255) NOT NULL,
    activity_type VARCHAR(100) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    content TEXT,
    difficulty_level VARCHAR(50) NOT NULL,
    estimated_duration INTEGER, -- 分钟
    skills_involved TEXT[],
    instructions TEXT,
    resources JSONB,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- 学习会话和进度跟踪表
-- ============================================================================

-- 学习会话表
CREATE TABLE IF NOT EXISTS learning_sessions (
    id VARCHAR(255) PRIMARY KEY,
    student_id VARCHAR(255) NOT NULL REFERENCES students(id),
    course_id VARCHAR(255) REFERENCES courses(id),
    classroom_id VARCHAR(255) REFERENCES classrooms(id),
    start_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    objectives TEXT[],
    initial_context JSONB,
    session_metrics JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 学习活动记录表
CREATE TABLE IF NOT EXISTS learning_activity_records (
    id SERIAL PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL REFERENCES learning_sessions(id),
    activity_id VARCHAR(255) NOT NULL REFERENCES learning_activities(id),
    start_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP,
    performance_score DECIMAL(5,2),
    completion_status VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS',
    feedback TEXT,
    time_spent INTEGER, -- 秒
    attempts INTEGER DEFAULT 1,
    hints_used INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- 学习档案和个性化数据表
-- ============================================================================

-- 学习档案表
CREATE TABLE IF NOT EXISTS learning_profiles (
    id SERIAL PRIMARY KEY,
    student_id VARCHAR(255) NOT NULL REFERENCES students(id),
    learning_style VARCHAR(100) NOT NULL DEFAULT 'BALANCED',
    cognitive_abilities JSONB,
    motivation_profile JSONB,
    learning_preferences JSONB,
    accessibility_needs JSONB,
    last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(student_id)
);

-- 个性化学习计划表
CREATE TABLE IF NOT EXISTS personalized_learning_plans (
    id VARCHAR(255) PRIMARY KEY,
    student_id VARCHAR(255) NOT NULL REFERENCES students(id),
    plan_type VARCHAR(100) NOT NULL DEFAULT 'ADAPTIVE',
    learning_goals JSONB,
    recommended_activities JSONB,
    next_recommendations JSONB,
    upcoming_milestones JSONB,
    adaptation_history JSONB,
    effectiveness_metrics JSONB,
    valid_from TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valid_until TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- 评估和反馈表
-- ============================================================================

-- 评估表
CREATE TABLE IF NOT EXISTS assessments (
    id VARCHAR(255) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    assessment_type VARCHAR(100) NOT NULL,
    subject VARCHAR(100) NOT NULL,
    difficulty_level VARCHAR(50) NOT NULL,
    questions JSONB,
    scoring_criteria JSONB,
    time_limit INTEGER, -- 分钟
    max_attempts INTEGER DEFAULT 1,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_by VARCHAR(255) REFERENCES teachers(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 评估结果表
CREATE TABLE IF NOT EXISTS assessment_results (
    id SERIAL PRIMARY KEY,
    assessment_id VARCHAR(255) NOT NULL REFERENCES assessments(id),
    student_id VARCHAR(255) NOT NULL REFERENCES students(id),
    session_id VARCHAR(255) REFERENCES learning_sessions(id),
    start_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP,
    score DECIMAL(5,2),
    max_score DECIMAL(5,2),
    percentage DECIMAL(5,2),
    answers JSONB,
    feedback JSONB,
    attempt_number INTEGER DEFAULT 1,
    status VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- 分析和报告表
-- ============================================================================

-- 学习分析数据表
CREATE TABLE IF NOT EXISTS learning_analytics (
    id SERIAL PRIMARY KEY,
    student_id VARCHAR(255) NOT NULL REFERENCES students(id),
    analysis_type VARCHAR(100) NOT NULL,
    analysis_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    time_period_start TIMESTAMP NOT NULL,
    time_period_end TIMESTAMP NOT NULL,
    metrics JSONB NOT NULL,
    insights JSONB,
    recommendations JSONB,
    confidence_score DECIMAL(3,2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 班级分析数据表
CREATE TABLE IF NOT EXISTS class_analytics (
    id SERIAL PRIMARY KEY,
    classroom_id VARCHAR(255) NOT NULL REFERENCES classrooms(id),
    analysis_type VARCHAR(100) NOT NULL,
    analysis_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    time_period_start TIMESTAMP NOT NULL,
    time_period_end TIMESTAMP NOT NULL,
    aggregate_metrics JSONB NOT NULL,
    performance_distribution JSONB,
    improvement_areas JSONB,
    recommendations JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- 索引创建
-- ============================================================================

-- 性能优化索引
CREATE INDEX IF NOT EXISTS idx_students_email ON students(email);
CREATE INDEX IF NOT EXISTS idx_students_grade_level ON students(grade_level);
CREATE INDEX IF NOT EXISTS idx_teachers_email ON teachers(email);
CREATE INDEX IF NOT EXISTS idx_classrooms_teacher_id ON classrooms(teacher_id);
CREATE INDEX IF NOT EXISTS idx_classrooms_subject ON classrooms(subject);
CREATE INDEX IF NOT EXISTS idx_student_enrollments_student_id ON student_classroom_enrollments(student_id);
CREATE INDEX IF NOT EXISTS idx_student_enrollments_classroom_id ON student_classroom_enrollments(classroom_id);
CREATE INDEX IF NOT EXISTS idx_courses_subject ON courses(subject);
CREATE INDEX IF NOT EXISTS idx_courses_grade_level ON courses(grade_level);
CREATE INDEX IF NOT EXISTS idx_learning_activities_course_id ON learning_activities(course_id);
CREATE INDEX IF NOT EXISTS idx_learning_activities_type ON learning_activities(activity_type);
CREATE INDEX IF NOT EXISTS idx_learning_sessions_student_id ON learning_sessions(student_id);
CREATE INDEX IF NOT EXISTS idx_learning_sessions_course_id ON learning_sessions(course_id);
CREATE INDEX IF NOT EXISTS idx_learning_sessions_start_time ON learning_sessions(start_time);
CREATE INDEX IF NOT EXISTS idx_activity_records_session_id ON learning_activity_records(session_id);
CREATE INDEX IF NOT EXISTS idx_activity_records_activity_id ON learning_activity_records(activity_id);
CREATE INDEX IF NOT EXISTS idx_learning_profiles_student_id ON learning_profiles(student_id);
CREATE INDEX IF NOT EXISTS idx_personalized_plans_student_id ON personalized_learning_plans(student_id);
CREATE INDEX IF NOT EXISTS idx_assessment_results_student_id ON assessment_results(student_id);
CREATE INDEX IF NOT EXISTS idx_assessment_results_assessment_id ON assessment_results(assessment_id);
CREATE INDEX IF NOT EXISTS idx_learning_analytics_student_id ON learning_analytics(student_id);
CREATE INDEX IF NOT EXISTS idx_learning_analytics_date ON learning_analytics(analysis_date);
CREATE INDEX IF NOT EXISTS idx_class_analytics_classroom_id ON class_analytics(classroom_id);
CREATE INDEX IF NOT EXISTS idx_class_analytics_date ON class_analytics(analysis_date);

-- JSONB字段索引（用于高效查询）
CREATE INDEX IF NOT EXISTS idx_learning_sessions_metrics ON learning_sessions USING GIN (session_metrics);
CREATE INDEX IF NOT EXISTS idx_learning_profiles_abilities ON learning_profiles USING GIN (cognitive_abilities);
CREATE INDEX IF NOT EXISTS idx_personalized_plans_goals ON personalized_learning_plans USING GIN (learning_goals);
CREATE INDEX IF NOT EXISTS idx_assessment_results_answers ON assessment_results USING GIN (answers);
CREATE INDEX IF NOT EXISTS idx_learning_analytics_metrics ON learning_analytics USING GIN (metrics);
CREATE INDEX IF NOT EXISTS idx_class_analytics_metrics ON class_analytics USING GIN (aggregate_metrics);
