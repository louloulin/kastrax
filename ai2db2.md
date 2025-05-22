# KastraX AI2DB 实施指南

## 项目结构

```
kastrax-ai2db/
├── backend/                       # 后端服务
│   ├── build.gradle.kts           # Gradle构建文件
│   ├── settings.gradle.kts        # Gradle设置
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/com/kastrax/ai2db/
│   │   │   │   ├── api/           # API服务模块
│   │   │   │   │   ├── controller/  # REST控制器
│   │   │   │   │   ├── dto/         # 数据传输对象
│   │   │   │   │   ├── handler/     # WebSocket处理器
│   │   │   │   │   └── middleware/  # API中间件
│   │   │   │   │
│   │   │   │   ├── auth/          # 安全与认证模块
│   │   │   │   │   ├── config/      # 安全配置
│   │   │   │   │   ├── service/     # 认证服务
│   │   │   │   │   └── model/       # 安全相关模型
│   │   │   │   │
│   │   │   │   ├── connection/    # 连接管理模块
│   │   │   │   │   ├── connector/   # 数据库连接器
│   │   │   │   │   ├── manager/     # 连接管理器
│   │   │   │   │   ├── pool/        # 连接池实现
│   │   │   │   │   ├── model/       # 连接模型
│   │   │   │   │   └── repository/  # 连接存储库
│   │   │   │   │
│   │   │   │   ├── nl2sql/        # NL2SQL转换模块
│   │   │   │   │   ├── converter/   # 转换器实现
│   │   │   │   │   ├── parser/      # SQL解析器
│   │   │   │   │   ├── prompt/      # 提示模板
│   │   │   │   │   ├── llm/         # LLM适配器
│   │   │   │   │   └── model/       # 转换模型
│   │   │   │   │
│   │   │   │   ├── schema/        # 模式管理模块
│   │   │   │   │   ├── extractor/   # 模式提取器
│   │   │   │   │   ├── analyzer/    # 关系分析器
│   │   │   │   │   ├── cache/       # 模式缓存
│   │   │   │   │   ├── vector/      # 向量存储
│   │   │   │   │   └── model/       # 模式模型
│   │   │   │   │
│   │   │   │   ├── query/         # 查询执行模块
│   │   │   │   │   ├── executor/    # 查询执行器
│   │   │   │   │   ├── optimizer/   # 查询优化器
│   │   │   │   │   ├── validator/   # 查询验证器
│   │   │   │   │   └── model/       # 查询模型
│   │   │   │   │
│   │   │   │   ├── result/        # 结果处理模块
│   │   │   │   │   ├── processor/   # 结果处理器
│   │   │   │   │   ├── visualizer/  # 可视化生成器
│   │   │   │   │   ├── export/      # 导出工具
│   │   │   │   │   ├── insight/     # 洞察生成器
│   │   │   │   │   └── model/       # 结果模型
│   │   │   │   │
│   │   │   │   ├── common/        # 共享组件
│   │   │   │   │   ├── config/      # 通用配置
│   │   │   │   │   ├── exception/   # 异常处理
│   │   │   │   │   ├── util/        # 工具类
│   │   │   │   │   ├── logging/     # 日志工具
│   │   │   │   │   └── model/       # 通用模型
│   │   │   │   │
│   │   │   │   ├── persistence/   # 数据持久层
│   │   │   │   │   ├── entity/      # 实体类
│   │   │   │   │   ├── repository/  # 数据存储库
│   │   │   │   │   └── migration/   # 数据库迁移
│   │   │   │   │
│   │   │   │   └── Application.kt  # 应用入口
│   │   │   │
│   │   │   └── resources/
│   │   │       ├── application.yml     # 应用配置
│   │   │       ├── db/migration/       # Flyway迁移脚本
│   │   │       └── prompts/            # LLM提示模板
│   │   │
│   │   └── test/
│   │       └── kotlin/com/kastrax/ai2db/
│   │           ├── unit/              # 单元测试
│   │           │   ├── api/
│   │           │   ├── connection/
│   │           │   ├── nl2sql/
│   │           │   └── ...
│   │           │
│   │           └── integration/       # 集成测试
│   │               ├── database/
│   │               ├── nlconversion/
│   │               └── ...
│   │
│   └── docker/                   # 后端Docker配置
│       ├── Dockerfile
│       └── entrypoint.sh
│
├── frontend/                     # 前端应用
│   ├── package.json              # NPM包配置
│   ├── tsconfig.json             # TypeScript配置
│   ├── vite.config.ts            # Vite构建配置
│   ├── public/                   # 静态资源
│   │   ├── favicon.ico
│   │   └── assets/
│   │
│   ├── src/
│   │   ├── api/                 # API客户端
│   │   │   ├── auth.ts
│   │   │   ├── connections.ts
│   │   │   ├── queries.ts
│   │   │   └── schemas.ts
│   │   │
│   │   ├── components/          # UI组件
│   │   │   ├── common/           # 通用组件
│   │   │   ├── connection/       # 连接管理组件
│   │   │   ├── query/            # 查询组件
│   │   │   ├── schema/           # 模式浏览组件
│   │   │   ├── visualization/    # 可视化组件
│   │   │   └── layout/           # 布局组件
│   │   │
│   │   ├── contexts/            # React上下文
│   │   │   ├── AuthContext.tsx
│   │   │   ├── ConnectionContext.tsx
│   │   │   └── QueryContext.tsx
│   │   │
│   │   ├── hooks/               # 自定义钩子
│   │   │   ├── useQuery.ts
│   │   │   ├── useSchema.ts
│   │   │   └── useConnection.ts
│   │   │
│   │   ├── pages/               # 页面组件
│   │   │   ├── Dashboard.tsx
│   │   │   ├── Connections.tsx
│   │   │   ├── QueryBuilder.tsx
│   │   │   ├── SchemaExplorer.tsx
│   │   │   ├── Settings.tsx
│   │   │   └── Auth.tsx
│   │   │
│   │   ├── types/               # TypeScript类型
│   │   │   ├── connection.ts
│   │   │   ├── query.ts
│   │   │   ├── schema.ts
│   │   │   └── user.ts
│   │   │
│   │   ├── utils/               # 工具函数
│   │   │   ├── formatting.ts
│   │   │   ├── validation.ts
│   │   │   └── storage.ts
│   │   │
│   │   ├── theme/               # UI主题
│   │   │   ├── light.ts
│   │   │   ├── dark.ts
│   │   │   └── common.ts
│   │   │
│   │   ├── App.tsx              # 应用组件
│   │   ├── main.tsx             # 入口文件
│   │   └── routes.tsx           # 路由配置
│   │
│   └── docker/                  # 前端Docker配置
│       ├── Dockerfile
│       └── nginx.conf
│
├── common/                       # 共享代码
│   ├── dto/                     # 数据传输对象
│   ├── schemas/                 # JSON Schema定义
│   └── constants/               # 共享常量
│
├── docs/                         # 项目文档
│   ├── api/                     # API文档
│   ├── architecture/            # 架构文档
│   ├── deployment/              # 部署文档
│   └── development/             # 开发指南
│
├── scripts/                      # 工具脚本
│   ├── setup.sh                 # 环境设置脚本
│   ├── build.sh                 # 构建脚本
│   └── deploy.sh                # 部署脚本
│
├── e2e-tests/                    # 端到端测试
│   ├── package.json
│   ├── playwright.config.ts
│   └── tests/
│       ├── connection.spec.ts
│       ├── query.spec.ts
│       └── schema.spec.ts
│
├── config/                       # 配置文件
│   ├── dev/                     # 开发环境配置
│   ├── test/                    # 测试环境配置
│   └── prod/                    # 生产环境配置
│
├── docker-compose.yml            # Docker Compose配置
├── .github/                      # GitHub Actions工作流
│   └── workflows/
│       ├── ci.yml               # CI工作流
│       └── release.yml          # 发布工作流
│
├── README.md                     # 项目说明
├── LICENSE                       # 许可证
└── .gitignore                    # Git忽略配置
```

### 项目结构说明

1. **后端结构设计**
   - 采用模块化架构，每个核心模块有专门的包
   - 使用Kotlin语言和Spring Boot框架
   - 按照功能职责划分子模块和包
   - 包含单元测试和集成测试

2. **前端结构设计**
   - 使用React和TypeScript构建
   - 组件化设计，按功能区分组件
   - 使用React Context和Hooks管理状态
   - 模块化的API客户端

3. **共享资源**
   - 后端和前端共享的数据传输对象和常量
   - 保持API契约的一致性

4. **配置管理**
   - 环境特定配置分离
   - Docker和容器化支持
   - CI/CD工作流配置

5. **文档组织**
   - API文档
   - 架构和设计文档
   - 部署和操作指南
   - 开发者指南

6. **测试策略**
   - 后端单元测试和集成测试
   - 前端组件测试
   - 端到端测试套件

### 关键文件说明

1. **Application.kt**: 后端应用程序入口点，配置Spring Boot应用
2. **build.gradle.kts**: 后端构建配置，定义依赖和构建任务
3. **package.json**: 前端依赖和脚本定义
4. **docker-compose.yml**: 定义完整应用的容器化部署
5. **application.yml**: 后端应用配置，包括数据库连接、安全设置等
6. **routes.tsx**: 前端路由配置，定义应用页面结构
7. **ci.yml**: 持续集成工作流，运行测试和构建流程
8. **Dockerfile**: 容器化应用的构建指令


## 文档版本历史

| 版本 | 日期       | 作者       | 修改描述                     |
|------|------------|------------|------------------------------|
| 1.0  | 2023-07-20 | 项目组     | 初始版本                     |
| 1.1  | 2023-07-21 | 项目组     | 添加监控日志和错误处理       |
| 1.2  | 2023-07-22 | 项目组     | 补充测试策略和CI/CD流程      |
| 1.3  | 2023-07-25 | 项目组     | 补充Kastrax集成细节和代码示例 |

本文档为AI编程助手（如Augment）提供KastraX AI2DB项目的实施指南，按优先级排序各项任务，并提供足够的技术细节以确保顺利实现。

## KastraX-AI2DB 模块设计

### 核心模块

1. **连接管理模块 (Connection Manager)**
   - 负责数据库连接的创建、管理和维护
   - 处理连接池和连接健康检查
   - 支持多种数据库类型的连接器
   - 管理连接凭证和安全连接

2. **NL2SQL转换模块 (NL2SQL Engine)**
   - 自然语言查询解析和理解
   - 将自然语言转换为SQL查询
   - 基于Kastrax LLM技术进行意图识别和实体提取
   - 处理上下文相关查询和多轮对话

3. **模式管理模块 (Schema Manager)**
   - 提取和管理数据库模式信息
   - 创建表和列的向量表示
   - 缓存和同步模式数据
   - 分析表间关系和依赖

4. **查询执行模块 (Query Executor)**
   - 执行SQL查询并处理结果
   - 查询优化和执行计划分析
   - 处理查询超时和错误恢复
   - 支持分页和流式查询结果

5. **结果处理模块 (Result Processor)**
   - 处理查询结果数据
   - 生成可视化和图表
   - 提供数据导出功能
   - 分析查询结果并提供洞察

6. **用户界面模块 (User Interface)**
   - 提供Web界面和移动友好设计
   - 实现查询输入和结果展示
   - 支持模式浏览和连接管理
   - 提供可视化和交互式数据探索

7. **安全与认证模块 (Security & Authentication)**
   - 用户认证和会话管理
   - 基于角色的访问控制
   - 数据敏感性标记和保护
   - 审计日志和安全监控

8. **API服务模块 (API Service)**
   - 提供RESTful和WebSocket接口
   - 处理请求验证和响应格式化
   - 实现API版本控制
   - 提供开发者文档和SDK

### 模块依赖关系图

```
+---------------------------+       +---------------------------+
|                           |       |                           |
|  User Interface Module    |------>|  API Service Module       |
|                           |       |                           |
+---------------------------+       +---------------------------+
                                               |
                                               v
+---------------------------+       +---------------------------+
|                           |       |                           |
|  Security & Auth Module   |<----->|  Connection Manager       |
|                           |       |                           |
+---------------------------+       +---------------------------+
        |                                       |
        |                                       v
        |                   +---------------------------+
        |                   |                           |
        +------------------>|  Schema Manager           |
        |                   |                           |
        |                   +---------------------------+
        |                               |
        |                               v
        |                   +---------------------------+
        |                   |                           |
        +------------------>|  NL2SQL Engine            |
        |                   |                           |
        |                   +---------------------------+
        |                               |
        |                               v
        |                   +---------------------------+
        |                   |                           |
        +------------------>|  Query Executor           |
                            |                           |
                            +---------------------------+
                                        |
                                        v
                            +---------------------------+
                            |                           |
                            |  Result Processor         |
                            |                           |
                            +---------------------------+
```

### 模块交互流程

1. **查询执行流程**
   - UI模块接收用户自然语言查询
   - API服务模块验证请求并转发
   - 安全模块检查用户权限
   - NL2SQL模块将自然语言转换为SQL
   - 查询执行模块执行SQL查询
   - 结果处理模块处理和格式化结果
   - API服务模块返回结果给UI

2. **模式同步流程**
   - 连接管理模块连接到目标数据库
   - 模式管理模块提取数据库元数据
   - 向量表示被创建并存储
   - 表关系被分析和记录
   - 模式信息被缓存以供快速访问

3. **用户认证流程**
   - UI模块收集用户凭证
   - API服务模块处理认证请求
   - 安全模块验证凭证和生成令牌
   - 用户会话被创建和维护
   - 后续请求使用令牌进行认证

## 总体架构设计

### 系统架构图

```
+---------------------------+            +---------------------------+
|                           |            |                           |
|   Frontend Application    |<---------->|      API Gateway          |
|                           |            |                           |
+---------------------------+            +---------------------------+
                                                     |
                                                     |
                                                     v
+---------------------------+            +---------------------------+
|                           |            |                           |
|   Authentication &        |<---------->|   Backend Core Services   |
|   Authorization           |            |                           |
+---------------------------+            +---------------------------+
                                                     |
                                         +-----------+-----------+
                                         |                       |
                                         v                       v
+---------------------------+   +------------------+   +------------------+
|                           |   |                  |   |                  |
|   Schema Management       |   | NL2SQL Engine    |   | Query Execution  |
|                           |   |                  |   | & Optimization   |
+---------------------------+   +------------------+   +------------------+
           |                             |                       |
           |                             |                       |
           v                             v                       v
+---------------------------+   +------------------+   +------------------+
|                           |   |                  |   |                  |
|   Knowledge Base &        |   | Vector Storage   |   | Result Processing|
|   Context Management      |   |                  |   | & Visualization  |
+---------------------------+   +------------------+   +------------------+
                                                     |
                                                     |
                                                     v
+---------------------------+            +---------------------------+
|                           |            |                           |
|   Database Connector      |<---------->|   External Databases      |
|   Framework               |            |                           |
+---------------------------+            +---------------------------+
```

### 组件说明

1. **Frontend Application**
   - 基于React和TypeScript构建的Web界面
   - 支持连接管理、查询执行、结果展示和数据库模式浏览
   - 提供响应式设计，适配不同设备

2. **API Gateway**
   - 处理所有前端请求
   - 实现API版本控制和流量管理
   - 提供认证和基本授权校验

3. **Authentication & Authorization**
   - 实现用户身份验证和会话管理
   - 提供基于角色和资源的权限控制
   - 支持多因素认证和SSO集成

4. **Backend Core Services**
   - 协调各个子系统
   - 实现业务逻辑和工作流管理
   - 提供事件处理和异步任务支持

5. **Schema Management**
   - 管理数据库模式信息
   - 自动抽取和维护数据库元数据
   - 缓存模式信息以提高性能

6. **NL2SQL Engine**
   - 将自然语言转换为SQL查询
   - 利用Kastrax LLM能力理解用户意图
   - 处理复杂查询和上下文相关请求

7. **Query Execution & Optimization**
   - 执行生成的查询
   - 优化查询以提高性能
   - 处理查询结果分页和流式传输

8. **Knowledge Base & Context Management**
   - 维护领域知识和术语映射
   - 管理会话上下文和历史
   - 支持多轮对话和引用解析

9. **Vector Storage**
   - 存储模式元素的向量表示
   - 支持语义搜索和相似度匹配
   - 优化检索和排序

10. **Result Processing & Visualization**
    - 处理查询结果
    - 生成数据可视化
    - 提供结果分析和洞察

11. **Database Connector Framework**
    - 连接各种数据库系统
    - 提供统一的接口抽象
    - 处理连接池和安全连接

### 数据流设计

1. **查询处理流程**
   ```
   用户查询 -> 自然语言理解 -> 意图识别 -> 实体提取 -> SQL生成 -> 查询优化 -> 
   执行查询 -> 结果处理 -> 可视化生成 -> 返回结果
   ```

2. **模式同步流程**
   ```
   触发同步 -> 连接数据库 -> 提取元数据 -> 构建模式表示 -> 
   生成向量嵌入 -> 存储模式信息 -> 更新缓存
   ```

3. **用户认证流程**
   ```
   用户登录 -> 验证凭证 -> 生成访问令牌 -> 返回令牌 -> 
   后续请求携带令牌 -> 验证令牌 -> 授权访问
   ```

## Kastrax 集成详细设计

### Kastrax核心组件集成

1. **KastraxLLM集成**
   ```kotlin
   // 使用Kastrax的LLM适配器进行NL2SQL转换
   class KastraxNL2SQLConverter(
       private val llmAdapter: KastraxLLMAdapter,
       private val promptBuilder: SQLPromptBuilder
   ) : NL2SQLConverter {
       override fun convertToSQL(
           query: String, 
           databaseType: DatabaseType, 
           schema: DatabaseSchema,
           context: ConversationContext?
       ): SQLQuery {
           // 构建包含模式信息的提示
           val prompt = promptBuilder.buildSQLPrompt(query, databaseType, schema, context)
           
           // 使用Kastrax LLM处理提示
           val response = llmAdapter.complete(prompt, SQLResponseParser())
           
           // 解析SQL响应
           return SQLQueryParser.parse(response, databaseType)
       }
   }
   ```

2. **Kastrax向量存储集成**
   ```kotlin
   // 使用Kastrax向量存储系统进行模式元素的语义存储
   class SchemaVectorStore(
       private val kastraxVectorDb: KastraxVectorDB,
       private val embeddingModel: KastraxEmbeddingModel
   ) {
       // 索引集合名称
       private val tableCollection = "schema_tables"
       private val columnCollection = "schema_columns"
       
       // 为表生成向量并存储
       suspend fun indexTable(table: TableSchema, connectionId: String) {
           val embedding = embeddingModel.embed(
               "${table.name} ${table.description ?: ""}"
           )
           
           kastraxVectorDb.upsert(
               collection = tableCollection,
               id = "${connectionId}:${table.name}",
               vector = embedding,
               metadata = mapOf(
                   "connectionId" to connectionId,
                   "tableName" to table.name,
                   "description" to (table.description ?: ""),
                   "columnCount" to table.columns.size
               )
           )
       }
       
       // 基于自然语言查询找到相关表
       suspend fun findRelevantTables(
           query: String,
           connectionId: String,
           limit: Int = 5
       ): List<String> {
           val queryEmbedding = embeddingModel.embed(query)
           
           val results = kastraxVectorDb.search(
               collection = tableCollection,
               vector = queryEmbedding,
               filter = "connectionId == '$connectionId'",
               limit = limit
           )
           
           return results.map { it.metadata["tableName"] as String }
       }
   }
   ```

3. **Kastrax执行器集成**
   ```kotlin
   // 使用Kastrax的Agent执行器来执行和监控查询任务
   class QueryExecutionService(
       private val kastraxExecutor: KastraxExecutor,
       private val connectionManager: ConnectionManager
   ) {
       suspend fun executeQuery(query: SQLQuery, connectionId: String): QueryResult {
           // 创建查询任务
           val task = QueryTask(
               sql = query.sql,
               connectionId = connectionId,
               timeout = 30000L // 30秒超时
           )
           
           // 通过Kastrax执行器执行任务
           return kastraxExecutor.execute(task) { progress ->
               // 任务进度更新处理
               when (progress) {
                   is QueryProgress.Started -> logQueryStart(connectionId, query)
                   is QueryProgress.Completed -> processResults(progress.results)
                   is QueryProgress.Failed -> handleQueryFailure(progress.error)
               }
           }
       }
   }
   ```

### 数据库设计详细规范

#### 完整关系图

```
+---------------+     +---------------+     +---------------+
|    users      |     | connections   |     | query_history |
+---------------+     +---------------+     +---------------+
| PK id         |<---*| FK created_by |     | PK id         |
| username      |     | PK id         |     | FK user_id    |
| email         |     | name          |*--->| FK connection_id|
| password_hash |     | db_type       |     | natural_language_query|
| created_at    |     | host          |     | generated_query|
| updated_at    |     | ...           |     | status        |
+---------------+     +---------------+     | ...           |
      |                      |              +---------------+
      |                      |                     |
      v                      v                     |
+---------------+     +---------------+            |
| user_roles    |     | schemas       |            |
+---------------+     +---------------+            |
| PK,FK user_id |     | PK id         |            |
| PK,FK role_id |     | FK connection_id|           |
+---------------+     | last_synced   |            |
      |               | schema_data   |            |
      |               +---------------+            |
      |                      |                     |
      v                      |                     v
+---------------+            |              +---------------+
|    roles      |            |              | query_feedback|
+---------------+            |              +---------------+
| PK id         |            |              | PK id         |
| name          |            |              | FK query_history_id|
| description   |            |              | rating        |
+---------------+            |              | feedback_text |
      |                      |              | created_at    |
      |                      |              +---------------+
      v                      v
+---------------+     +---------------+
|role_permissions|     |    tables    |
+---------------+     +---------------+
| PK,FK role_id |     | PK id         |
| PK,FK perm_id |     | FK schema_id  |
+---------------+     | name          |
      |               | description   |
      |               | ...           |
      v               +---------------+
+---------------+            |
|  permissions  |            |
+---------------+            |
| PK id         |            |
| name          |            |
| description   |            |
+---------------+            |
                             v
                     +---------------+     +---------------+
                     |   columns     |     | relationships |
                     +---------------+     +---------------+
                     | PK id         |     | PK id         |
                     | FK table_id   |     | FK schema_id  |
                     | name          |     | FK source_table_id|
                     | data_type     |     | FK source_column_id|
                     | is_nullable   |     | FK target_table_id|
                     | ...           |     | FK target_column_id|
                     +---------------+     | relationship_type|
                                           +---------------+
```

#### 向量存储设计详细规范

```
vector_db.tables {
    id: String,              // 唯一标识符 "{connectionId}:{tableName}"
    vector: Float[],         // 表名和描述的向量表示
    metadata: {
        connectionId: String,
        tableName: String,
        description: String,
        columnCount: Integer,
        estimatedRowCount: Long,
        lastUpdated: Timestamp
    }
}

vector_db.columns {
    id: String,              // 唯一标识符 "{connectionId}:{tableName}:{columnName}"
    vector: Float[],         // 列名和描述的向量表示
    metadata: {
        connectionId: String,
        tableName: String,
        columnName: String,
        description: String,
        dataType: String,
        isPrimaryKey: Boolean,
        isForeignKey: Boolean,
        lastUpdated: Timestamp
    }
}

vector_db.queries {
    id: String,              // 唯一查询ID
    vector: Float[],         // 查询的向量表示
    metadata: {
        connectionId: String,
        naturalLanguageQuery: String,
        generatedSql: String,
        executionTime: Long,
        resultCount: Integer,
        timestamp: Timestamp,
        userId: String
    }
}

vector_db.terms {
    id: String,              // 术语ID
    vector: Float[],         // 术语的向量表示
    metadata: {
        term: String,
        definition: String,
        domain: String,
        relatedTerms: String[]
    }
}
```

### 核心接口详细设计

#### 1. NL2SQL转换引擎核心接口

```kotlin
// 核心转换器接口
interface NL2SQLConverter {
    /**
     * 将自然语言查询转换为SQL
     *
     * @param query 自然语言查询
     * @param databaseType 目标数据库类型
     * @param schema 数据库模式信息
     * @param context 可选的会话上下文
     * @return SQL查询对象
     */
    suspend fun convertToSQL(
        query: String,
        databaseType: DatabaseType,
        schema: DatabaseSchema,
        context: ConversationContext? = null
    ): SQLQuery
    
    /**
     * 解释自然语言转换过程
     */
    suspend fun explainConversion(
        query: String,
        databaseType: DatabaseType,
        schema: DatabaseSchema
    ): ConversionExplanation
}

// SQL查询结果
data class SQLQuery(
    val sql: String,                   // 生成的SQL查询
    val parameters: List<Any> = listOf(), // 参数化查询的参数
    val queryType: QueryType,          // 查询类型
    val tables: List<String>,          // 涉及的表
    val columns: List<String>,         // 涉及的列
    val conditions: List<String>,      // 查询条件
    val estimatedComplexity: QueryComplexity // 查询复杂度估计
)

// 转换解释
data class ConversionExplanation(
    val steps: List<ConversionStep>,   // 转换步骤
    val entityRecognition: Map<String, String>, // 识别的实体
    val alternatives: List<String>,    // 替代SQL选项
    val confidence: Float              // 转换置信度
)

// 转换步骤
data class ConversionStep(
    val step: String,                  // 步骤描述
    val reasoning: String,             // 推理过程
    val intermediateResult: String     // 中间结果
)
```

#### 2. 数据库连接器完整接口

```kotlin
// 数据库连接器接口
interface DatabaseConnector {
    /**
     * 连接到数据库
     */
    suspend fun connect(config: ConnectionConfig): Connection
    
    /**
     * 断开连接
     */
    suspend fun disconnect(connection: Connection): Boolean
    
    /**
     * 测试连接
     */
    suspend fun testConnection(config: ConnectionConfig): ConnectionStatus
    
    /**
     * 获取数据库元数据
     */
    suspend fun getMetadata(connection: Connection): DatabaseMetadata
    
    /**
     * 执行查询
     */
    suspend fun executeQuery(
        connection: Connection, 
        query: String,
        parameters: List<Any> = listOf(),
        timeout: Long = 30000L
    ): QueryResult
    
    /**
     * 执行更新操作
     */
    suspend fun executeUpdate(
        connection: Connection,
        query: String,
        parameters: List<Any> = listOf()
    ): UpdateResult
    
    /**
     * 开始事务
     */
    suspend fun beginTransaction(connection: Connection): Transaction
    
    /**
     * 提交事务
     */
    suspend fun commitTransaction(transaction: Transaction)
    
    /**
     * 回滚事务
     */
    suspend fun rollbackTransaction(transaction: Transaction)
}

// 连接配置
data class ConnectionConfig(
    val id: String,                    // 连接ID
    val name: String,                  // 连接名称
    val type: DatabaseType,            // 数据库类型
    val host: String,                  // 主机地址
    val port: Int,                     // 端口
    val database: String,              // 数据库名
    val username: String,              // 用户名
    val password: String,              // 密码
    val ssl: Boolean = false,          // 是否使用SSL
    val connectionTimeout: Long = 30000L, // 连接超时
    val idleTimeout: Long = 600000L,   // 空闲超时
    val maxPoolSize: Int = 10,         // 最大连接池大小
    val parameters: Map<String, String> = mapOf() // 其他参数
)

// 查询结果
data class QueryResult(
    val columns: List<Column>,         // 列信息
    val rows: List<List<Any?>>,        // 行数据
    val rowCount: Int,                 // 行数
    val executionTimeMs: Long,         // 执行时间
    val metadata: Map<String, Any> = mapOf() // 额外元数据
)

// 列信息
data class Column(
    val name: String,                  // 列名
    val label: String,                 // 列标签
    val type: String,                  // 列类型
    val typeName: String               // 类型名称
)
```

#### 3. 模式管理接口设计

```kotlin
// 模式管理器接口
interface SchemaManager {
    /**
     * 提取数据库模式
     */
    suspend fun extractSchema(connection: Connection): DatabaseSchema
    
    /**
     * 缓存模式信息
     */
    suspend fun cacheSchema(connectionId: String, schema: DatabaseSchema): Boolean
    
    /**
     * 获取缓存的模式信息
     */
    suspend fun getSchema(connectionId: String): DatabaseSchema?
    
    /**
     * 同步模式信息
     */
    suspend fun syncSchema(connectionId: String): DatabaseSchema
    
    /**
     * 获取表详情
     */
    suspend fun getTableDetails(
        connectionId: String, 
        tableName: String
    ): TableSchema?
    
    /**
     * 获取表间关系
     */
    suspend fun getRelationships(connectionId: String): List<Relationship>
    
    /**
     * 查找相关表
     */
    suspend fun findRelevantTables(
        query: String,
        connectionId: String,
        limit: Int = 5
    ): List<TableSchema>
}

// 数据库模式
data class DatabaseSchema(
    val tables: List<TableSchema>,     // 表信息
    val relationships: List<Relationship>, // 关系信息
    val version: String,               // 模式版本
    val lastSynced: Instant,           // 最后同步时间
    val metadata: Map<String, Any> = mapOf() // 额外元数据
)

// 表模式
data class TableSchema(
    val name: String,                  // 表名
    val columns: List<ColumnSchema>,   // 列信息
    val primaryKey: List<String>,      // 主键列名
    val indexes: List<IndexInfo>,      // 索引信息
    val description: String? = null,   // 表描述
    val estimatedRowCount: Long? = null, // 估计行数
    val schema: String? = null         // 所属模式名
)

// 列模式
data class ColumnSchema(
    val name: String,                  // 列名
    val dataType: String,              // 数据类型
    val typeName: String,              // 类型名称
    val size: Int?,                    // 大小
    val isNullable: Boolean,           // 是否可空
    val isPrimaryKey: Boolean,         // 是否主键
    val isForeignKey: Boolean,         // 是否外键
    val defaultValue: String?,         // 默认值
    val description: String? = null,   // 列描述
    val position: Int                  // 位置
)

// 关系信息
data class Relationship(
    val id: String,                    // 关系ID
    val sourceTable: String,           // 源表
    val sourceColumn: String,          // 源列
    val targetTable: String,           // 目标表
    val targetColumn: String,          // 目标列
    val relationshipType: RelationshipType, // 关系类型
    val name: String? = null           // 关系名称
)

// 关系类型
enum class RelationshipType {
    ONE_TO_ONE,
    ONE_TO_MANY,
    MANY_TO_ONE,
    MANY_TO_MANY
}
```

### 与Chat2DB等同类产品的比较分析

| 功能/特性 | KastraX AI2DB | Chat2DB | SQL Chat | DataChat |
|-----------|--------------|---------|----------|----------|
| **核心功能** |
| 支持数据库类型 | MySQL, PostgreSQL, MongoDB, Redis, Oracle, SQL Server, Elasticsearch等 | MySQL, PostgreSQL, Oracle, SQL Server, H2等 | MySQL, PostgreSQL, SQL Server | MySQL, PostgreSQL |
| 自然语言查询 | 高级语义理解，使用Kastrax LLM | 基于OpenAI，有限语义理解 | 基于自有模型 | 基于OpenAI |
| 模式浏览 | 完整的可视化模式浏览器 | 基础表列表视图 | 树形结构视图 | 简易表视图 |
| 多轮对话 | 完整上下文理解和状态维护 | 有限上下文保持 | 基本上下文保持 | 有限支持 |
| **技术特性** |
| 向量存储 | 全面集成，模式和查询向量化 | 不支持 | 有限支持 | 不支持 |
| 查询优化 | 智能查询分析和优化 | 基础优化 | 有限优化 | 无优化 |
| 可视化 | 智能图表推荐和丰富可视化 | 基础表格和图表 | 基础图表 | 简单表格 |
| API集成 | 完整REST和WebSocket API | 有限API | 基本API | 有限API |
| **部署与集成** |
| 部署方式 | 容器化，云原生，支持企业私有部署 | 桌面应用和服务器部署 | 云服务和有限本地部署 | 主要云服务 |
| 安全特性 | 企业级安全，细粒度权限控制 | 基础安全 | 标准安全 | 基础安全 |
| 与生态集成 | 与Kastrax生态完全集成 | 独立产品 | 有限集成 | 独立产品 |
| **扩展性** |
| 插件系统 | 完整插件架构 | 不支持 | 有限支持 | 不支持 |
| 自定义 | 高度可定制，支持领域适配 | 有限定制 | 中等定制 | 有限定制 |

### 代码示例：实现自定义查询模板

```kotlin
/**
 * 自定义查询模板实现
 * 允许用户保存和复用常用查询模式
 */
@Service
class QueryTemplateService(
    private val templateRepository: QueryTemplateRepository,
    private val nl2sqlConverter: NL2SQLConverter,
    private val vectorStore: KastraxVectorDB,
    private val embeddingModel: KastraxEmbeddingModel
) {
    // 创建新模板
    suspend fun createTemplate(template: QueryTemplate): QueryTemplate {
        // 生成模板向量表示
        val embedding = embeddingModel.embed(
            "${template.name} ${template.description} ${template.templateQuery}"
        )
        
        // 保存到向量数据库
        vectorStore.upsert(
            collection = "query_templates",
            id = template.id,
            vector = embedding,
            metadata = mapOf(
                "name" to template.name,
                "description" to template.description,
                "userId" to template.userId,
                "connectionId" to template.connectionId
            )
        )
        
        // 保存到关系数据库
        return templateRepository.save(template)
    }
    
    // 查找相似模板
    suspend fun findSimilarTemplates(
        query: String,
        connectionId: String,
        limit: Int = 3
    ): List<QueryTemplate> {
        val queryEmbedding = embeddingModel.embed(query)
        
        // 搜索相似向量
        val results = vectorStore.search(
            collection = "query_templates",
            vector = queryEmbedding,
            filter = "connectionId == '$connectionId'",
            limit = limit
        )
        
        // 获取完整模板
        return results.mapNotNull { result ->
            templateRepository.findById(result.id)
        }
    }
    
    // 应用模板
    suspend fun applyTemplate(
        templateId: String,
        parameters: Map<String, String>,
        connectionId: String,
        schema: DatabaseSchema
    ): SQLQuery {
        val template = templateRepository.findById(templateId) 
            ?: throw NotFoundException("Template not found")
        
        // 替换模板参数
        var processedQuery = template.templateQuery
        parameters.forEach { (key, value) ->
            processedQuery = processedQuery.replace("{$key}", value)
        }
        
        // 如果是NL模板，转换为SQL
        if (template.isNaturalLanguage) {
            return nl2sqlConverter.convertToSQL(
                query = processedQuery,
                databaseType = template.databaseType,
                schema = schema
            )
        }
        
        // 否则直接解析SQL模板
        return SQLQueryParser.parse(processedQuery, template.databaseType)
    }
}

// 查询模板数据类
data class QueryTemplate(
    val id: String,
    val name: String,
    val description: String,
    val templateQuery: String,
    val databaseType: DatabaseType,
    val isNaturalLanguage: Boolean,
    val parameters: List<TemplateParameter>,
    val userId: String,
    val connectionId: String,
    val createdAt: Instant,
    val updatedAt: Instant
)

// 模板参数
data class TemplateParameter(
    val name: String,
    val description: String,
    val defaultValue: String? = null,
    val required: Boolean = true
)
```

## 优先级1：核心基础设施

### 1.1 数据库连接器框架

**目标**：构建通用的数据库连接器框架，支持不同类型的数据库。

**关键组件**：
```kotlin
// 1. 核心连接器接口
interface DatabaseConnector {
    fun connect(config: ConnectionConfig): Connection
    fun disconnect(connection: Connection): Boolean
    fun testConnection(config: ConnectionConfig): ConnectionStatus
    fun getMetadata(connection: Connection): DatabaseMetadata
    fun executeQuery(connection: Connection, query: String): QueryResult
}

// 2. 连接配置数据类
data class ConnectionConfig(
    val id: String,
    val name: String,
    val type: DatabaseType,
    val host: String,
    val port: Int,
    val database: String,
    val username: String,
    val password: String,
    val parameters: Map<String, String> = mapOf()
)

// 3. 数据库类型枚举
enum class DatabaseType {
    MYSQL,
    POSTGRESQL,
    MONGODB,
    REDIS,
    SQL_SERVER,
    ORACLE,
    ELASTICSEARCH
    // 可扩展更多类型
}
```

**实现步骤**：
1. 创建核心接口和数据类
2. 实现MySQL连接器（优先）
3. 实现PostgreSQL连接器
4. 添加连接池支持
5. 实现连接安全管理

### 1.2 NL2SQL转换核心

**目标**：构建自然语言到SQL的转换核心引擎。

**关键组件**：
```kotlin
// 1. NL转换接口
interface NL2SQLConverter {
    fun convertToSQL(query: String, databaseType: DatabaseType, schema: DatabaseSchema, context: ConversationContext? = null): SQLQuery
    fun explainConversion(query: String, databaseType: DatabaseType, schema: DatabaseSchema): ConversionExplanation
}

// 2. SQL查询数据类
data class SQLQuery(
    val sql: String,
    val parameters: List<Any> = listOf(),
    val queryType: QueryType,
    val tables: List<String>,
    val estimatedComplexity: QueryComplexity
)

// 3. 查询类型枚举
enum class QueryType {
    SELECT,
    INSERT,
    UPDATE,
    DELETE,
    CREATE,
    ALTER,
    DROP,
    OTHER
}
```

**实现步骤**：
1. 建立基于Kastrax LLM的NL2SQL转换器
2. 实现基本的SELECT语句生成
3. 添加查询类型识别功能
4. 实现简单WHERE条件生成
5. 添加基础上下文管理

## 优先级2：用户界面与API

### 2.1 REST API基础

**目标**：实现基本的REST API接口，支持连接管理和查询执行。

**关键端点**：
```kotlin
// 使用Ktor框架示例
fun Application.configureRouting() {
    routing {
        // 连接管理API
        route("/api/connections") {
            get { /* 获取所有连接 */ }
            post { /* 创建新连接 */ }
            route("/{id}") {
                get { /* 获取特定连接 */ }
                put { /* 更新连接 */ }
                delete { /* 删除连接 */ }
                post("/test") { /* 测试连接 */ }
            }
        }
        
        // 查询API
        route("/api/query") {
            post { /* 执行自然语言查询 */ }
            post("/raw") { /* 执行原始SQL查询 */ }
            get("/history") { /* 获取查询历史 */ }
            get("/{id}") { /* 获取特定查询详情 */ }
            post("/explain") { /* 解释自然语言转换过程 */ }
        }
    }
}
```

**完整API规范**：

1. **认证API**
```
POST   /api/auth/login              # 用户登录
POST   /api/auth/refresh            # 刷新令牌
POST   /api/auth/logout             # 用户登出
```

2. **用户管理API**
```
GET    /api/users                   # 获取所有用户
POST   /api/users                   # 创建用户
GET    /api/users/{id}              # 获取用户详情
PUT    /api/users/{id}              # 更新用户
DELETE /api/users/{id}              # 删除用户
PUT    /api/users/{id}/password     # 修改密码
```

3. **连接管理API**
```
GET    /api/connections             # 获取所有连接
POST   /api/connections             # 创建连接
GET    /api/connections/{id}        # 获取连接详情
PUT    /api/connections/{id}        # 更新连接
DELETE /api/connections/{id}        # 删除连接
POST   /api/connections/{id}/test   # 测试连接
```

4. **模式管理API**
```
GET    /api/schemas/{connectionId}                 # 获取数据库模式
GET    /api/schemas/{connectionId}/tables          # 获取所有表
GET    /api/schemas/{connectionId}/tables/{name}   # 获取表详情
POST   /api/schemas/{connectionId}/sync            # 同步模式
```

5. **查询API**
```
POST   /api/query                   # 执行自然语言查询
POST   /api/query/raw               # 执行原始SQL查询
GET    /api/query/history           # 获取查询历史
GET    /api/query/{id}              # 获取查询详情
POST   /api/query/explain           # 解释查询转换过程
```

6. **会话API**
```
POST   /api/chat/sessions           # 创建会话
GET    /api/chat/sessions/{id}      # 获取会话
POST   /api/chat/sessions/{id}/messages # 发送消息
DELETE /api/chat/sessions/{id}      # 删除会话
```

7. **导出API**
```
POST   /api/export/csv              # 导出为CSV
POST   /api/export/json             # 导出为JSON
POST   /api/export/excel            # 导出为Excel
```

**请求/响应示例**：

1. **执行自然语言查询**

请求:
```json
POST /api/query
{
  "connectionId": "123e4567-e89b-12d3-a456-426614174000",
  "query": "查找销售额最高的前10个客户",
  "sessionId": "789e4567-e89b-12d3-a456-426614174001"
}
```

响应:
```json
{
  "id": "abce4567-e89b-12d3-a456-426614174002",
  "status": "SUCCESS",
  "generatedQuery": "SELECT c.customer_name, SUM(o.total_amount) as total_sales FROM customers c JOIN orders o ON c.customer_id = o.customer_id GROUP BY c.customer_name ORDER BY total_sales DESC LIMIT 10",
  "results": {
    "columns": ["customer_name", "total_sales"],
    "rows": [
      ["ABC Company", 125000.50],
      ["XYZ Corporation", 98750.25],
      // ...更多行
    ],
    "rowCount": 10,
    "executionTimeMs": 145
  },
  "visualization": {
    "recommendedType": "BAR_CHART",
    "config": {
      // 可视化配置
    }
  },
  "insights": [
    {
      "type": "DISTRIBUTION",
      "description": "前10名客户贡献了总销售额的45%"
    },
    // ...更多洞察
  ]
}
```

2. **创建数据库连接**

请求:
```json
POST /api/connections
{
  "name": "Production MySQL",
  "description": "生产环境主数据库",
  "dbType": "MYSQL",
  "host": "db.example.com",
  "port": 3306,
  "database": "production",
  "username": "db_user",
  "password": "db_password",
  "parameters": {
    "useSSL": "true",
    "connectionTimeout": "30000"
  }
}
```

响应:
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "name": "Production MySQL",
  "description": "生产环境主数据库",
  "dbType": "MYSQL",
  "host": "db.example.com",
  "port": 3306,
  "database": "production",
  "username": "db_user",
  "passwordSet": true,
  "parameters": {
    "useSSL": "true",
    "connectionTimeout": "30000"
  },
  "createdAt": "2023-07-15T10:30:00Z",
  "updatedAt": "2023-07-15T10:30:00Z"
}
```

**实现步骤**：
1. 设置Ktor或Spring Boot项目
2. 实现连接管理API
3. 实现基本查询API
4. 添加错误处理
5. 实现基本认证

### 2.2 前端UI设计与功能

**核心页面与功能**：

1. **登录页面**
   - 用户登录表单
   - 记住我功能
   - 忘记密码链接
   - 多因素认证支持

2. **仪表盘**
   - 最近查询概览
   - 已连接数据库状态
   - 快速操作卡片
   - 使用统计图表

3. **连接管理页面**
   - 连接列表视图
   - 创建/编辑连接表单
   - 连接测试功能
   - 连接状态指示器

4. **查询界面**
   - 查询输入区域（支持自然语言和SQL）
   - 数据库模式浏览树
   - 查询结果表格
   - 数据可视化面板
   - 查询历史边栏

5. **模式浏览器**
   - 数据库/表/列层级视图
   - 表关系可视化
   - 表详情和统计信息
   - 示例数据预览

6. **设置页面**
   - 用户资料设置
   - 界面主题配置
   - 查询默认设置
   - 导出格式偏好

**UI组件库与风格**：
- 使用Material-UI组件库
- 亮/暗主题支持
- 响应式设计适配移动设备
- 可访问性遵循WCAG 2.1标准

**前端技术架构**：
```
Frontend/
├── public/
├── src/
│   ├── api/                  # API通信模块
│   │   ├── auth.ts           # 认证相关
│   │   ├── connections.ts    # 连接管理
│   │   ├── queries.ts        # 查询执行
│   │   └── schemas.ts        # 模式管理
│   │
│   ├── components/           # 可复用组件
│   │   ├── common/           # 通用组件
│   │   ├── connection/       # 连接相关组件
│   │   ├── query/            # 查询相关组件
│   │   ├── schema/           # 模式相关组件
│   │   └── visualization/    # 可视化组件
│   │
│   ├── contexts/             # React上下文
│   │   ├── AuthContext.tsx   # 认证状态管理
│   │   └── AppContext.tsx    # 应用状态管理
│   │
│   ├── hooks/                # 自定义钩子
│   │   ├── useQuery.ts       # 查询相关钩子
│   │   ├── useSchema.ts      # 模式相关钩子
│   │   └── useConnection.ts  # 连接相关钩子
│   │
│   ├── pages/                # 页面组件
│   │   ├── Dashboard.tsx     # 仪表盘
│   │   ├── ConnectionsPage.tsx # 连接管理
│   │   ├── QueryPage.tsx     # 查询执行
│   │   ├── SchemaPage.tsx    # 模式浏览
│   │   └── SettingsPage.tsx  # 设置页面
│   │
│   ├── utils/                # 工具函数
│   │   ├── format.ts         # 格式化工具
│   │   ├── storage.ts        # 本地存储工具
│   │   └── validation.ts     # 表单验证
│   │
│   ├── App.tsx               # 应用入口
│   └── index.tsx             # 渲染入口
│
├── package.json              # 依赖配置
└── tsconfig.json             # TypeScript配置
```

**查询页面示例代码**：
```tsx
const QueryPage: React.FC = () => {
  const [queryInput, setQueryInput] = useState("");
  const [isNaturalLanguage, setIsNaturalLanguage] = useState(true);
  const [activeConnection, setActiveConnection] = useState<Connection | null>(null);
  const [queryResult, setQueryResult] = useState<QueryResult | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [activeTab, setActiveTab] = useState("results"); // results, visualization, explain, history
  
  const { connections } = useConnections();
  const { executeQuery, queryHistory } = useQuery();
  
  const handleExecuteQuery = async () => {
    if (!activeConnection || !queryInput.trim()) return;
    
    setIsLoading(true);
    try {
      const result = await executeQuery({
        connectionId: activeConnection.id,
        query: queryInput,
        isNaturalLanguage
      });
      setQueryResult(result);
      setActiveTab("results");
    } catch (error) {
      // 错误处理
    } finally {
      setIsLoading(false);
    }
  };
  
  return (
    <div className="query-page">
      <div className="query-sidebar">
        <ConnectionSelector 
          connections={connections}
          activeConnection={activeConnection}
          onConnectionChange={setActiveConnection}
        />
        <SchemaExplorer connectionId={activeConnection?.id} />
        <QueryHistory history={queryHistory} onSelectQuery={(q) => setQueryInput(q.query)} />
      </div>
      
      <div className="query-main">
        <div className="query-input-container">
          <div className="query-input-header">
            <ToggleButtonGroup
              value={isNaturalLanguage ? "nl" : "sql"}
              exclusive
              onChange={(e, value) => setIsNaturalLanguage(value === "nl")}
            >
              <ToggleButton value="nl">自然语言</ToggleButton>
              <ToggleButton value="sql">SQL</ToggleButton>
            </ToggleButtonGroup>
            <Button 
              variant="contained" 
              color="primary" 
              disabled={!activeConnection || !queryInput.trim() || isLoading}
              onClick={handleExecuteQuery}
            >
              {isLoading ? <CircularProgress size={24} /> : "执行查询"}
            </Button>
          </div>
          
          <TextField
            multiline
            rows={6}
            value={queryInput}
            onChange={(e) => setQueryInput(e.target.value)}
            placeholder={isNaturalLanguage ? "输入自然语言查询，例如：查找最近30天内销售额最高的产品" : "输入SQL查询"}
            variant="outlined"
            fullWidth
          />
        </div>
        
        {queryResult && (
          <div className="query-result-container">
            <Tabs value={activeTab} onChange={(e, newValue) => setActiveTab(newValue)}>
              <Tab label="结果" value="results" />
              <Tab label="可视化" value="visualization" disabled={!queryResult.visualization} />
              <Tab label="查询解释" value="explain" />
              <Tab label="执行统计" value="stats" />
            </Tabs>
            
            <div className="tab-content">
              {activeTab === "results" && (
                <ResultTable data={queryResult.results} />
              )}
              
              {activeTab === "visualization" && queryResult.visualization && (
                <Visualization data={queryResult.visualization} />
              )}
              
              {activeTab === "explain" && (
                <QueryExplanation 
                  naturalLanguage={isNaturalLanguage ? queryInput : ""}
                  generatedQuery={queryResult.generatedQuery}
                  explanation={queryResult.explanation}
                />
              )}
              
              {activeTab === "stats" && (
                <QueryStats 
                  executionTime={queryResult.results.executionTimeMs}
                  rowCount={queryResult.results.rowCount}
                  status={queryResult.status}
                />
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
```

## 优先级3：数据存储与模式管理

### 3.1 数据库模式

**目标**：实现内部数据库模式，存储连接、用户和查询历史信息。

**SQL脚本**：
```sql
-- 1. 创建用户表
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. 创建连接表
CREATE TABLE connections (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    db_type VARCHAR(50) NOT NULL,
    host VARCHAR(255) NOT NULL,
    port INTEGER,
    database_name VARCHAR(100),
    username VARCHAR(100),
    password_encrypted VARCHAR(255),
    parameters JSONB,
    created_by VARCHAR(36) REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. 创建查询历史表
CREATE TABLE query_history (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) REFERENCES users(id),
    connection_id VARCHAR(36) REFERENCES connections(id),
    natural_language_query TEXT,
    generated_query TEXT,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    execution_time_ms INTEGER,
    result_row_count INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

**实现步骤**：
1. 设置PostgreSQL数据库
2. 执行初始化脚本
3. 实现数据访问层
4. 添加数据迁移支持
5. 实现基本的CRUD操作

### 3.2 模式抽取与缓存

**目标**：实现从目标数据库抽取模式信息并缓存的功能。

**关键组件**：
```kotlin
// 1. 模式管理器接口
interface SchemaManager {
    fun extractSchema(connection: Connection): DatabaseSchema
    fun cacheSchema(connectionId: String, schema: DatabaseSchema): Boolean
    fun getSchema(connectionId: String): DatabaseSchema?
    fun syncSchema(connectionId: String): DatabaseSchema
}

// 2. 数据库模式数据类
data class DatabaseSchema(
    val tables: List<TableSchema>,
    val relationships: List<Relationship>,
    val lastSynced: Instant
)

// 3. 表模式数据类
data class TableSchema(
    val name: String,
    val columns: List<ColumnSchema>,
    val primaryKey: List<String>,
    val estimatedRowCount: Long?
)
```

**实现步骤**：
1. 实现MySQL模式抽取
2. 实现PostgreSQL模式抽取
3. 创建模式缓存机制
4. 实现表关系识别
5. 添加模式同步功能

## 优先级4：高级功能

### 4.1 高级NL2SQL功能

**目标**：增强NL2SQL转换器，支持复杂查询和多轮对话。

**关键组件**：
```kotlin
// 1. 会话管理器
interface ChatSessionManager {
    fun createSession(userId: String): ChatSession
    fun getSession(sessionId: String): ChatSession?
    fun updateSession(sessionId: String, message: ChatMessage): ChatSession
    fun clearSession(sessionId: String): Boolean
}

// 2. 会话数据类
data class ChatSession(
    val id: String,
    val userId: String,
    val messages: List<ChatMessage> = listOf(),
    val context: ConversationContext = ConversationContext(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

// 3. 上下文数据类
data class ConversationContext(
    val referencedTables: MutableSet<String> = mutableSetOf(),
    val referencedColumns: MutableMap<String, MutableSet<String>> = mutableMapOf(),
    val lastQueryType: QueryType? = null,
    val parameters: MutableMap<String, Any> = mutableMapOf()
)
```

**实现步骤**：
1. 实现会话管理
2. 添加上下文追踪
3. 支持多表连接查询
4. 实现聚合函数支持
5. 添加复杂WHERE条件支持

### 4.2 结果处理与可视化

**目标**：实现查询结果处理和基础可视化功能。

**关键组件**：
```kotlin
// 1. 结果处理器接口
interface ResultProcessor {
    fun processResult(result: QueryResult): ProcessedResult
    fun generateVisualization(result: QueryResult): Visualization
    fun generateInsights(result: QueryResult): List<Insight>
    fun formatForExport(result: QueryResult, format: ExportFormat): ByteArray
}

// 2. 可视化类型枚举
enum class VisualizationType {
    TABLE,
    BAR_CHART,
    LINE_CHART,
    PIE_CHART,
    SCATTER_PLOT,
    HEATMAP
}

// 3. 导出格式枚举
enum class ExportFormat {
    CSV,
    JSON,
    EXCEL,
    PDF
}
```

**实现步骤**：
1. 实现基本表格展示
2. 添加CSV和JSON导出
3. 实现基础图表生成
4. 添加自动可视化类型选择
5. 实现简单的结果分析

## 优先级5：安全与部署

### 5.1 安全实现

**目标**：实现基本的安全功能，包括认证、授权和数据保护。

**关键组件**：
```kotlin
// 1. 安全配置
fun Application.configureSecurity() {
    install(Authentication) {
        jwt {
            // JWT认证配置
        }
    }
    
    install(Authorization) {
        // 授权配置
    }
}

// 2. 密码加密服务
interface PasswordService {
    fun hashPassword(password: String): String
    fun verifyPassword(password: String, hash: String): Boolean
}

// 3. 权限检查
fun checkPermission(userId: String, connectionId: String, permissionType: PermissionType): Boolean {
    // 权限检查逻辑
}
```

**实现步骤**：
1. 实现JWT认证
2. 添加密码加密
3. 实现基本授权检查
4. 添加数据敏感信息保护
5. 实现审计日志

### 5.2 部署配置

**目标**：准备部署配置，支持容器化和基本的可伸缩性。

**Docker配置**：
```dockerfile
# 1. 后端Dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY build/libs/ai2db-backend.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

# 2. 前端Dockerfile
FROM node:16-alpine as build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/build /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

**Docker Compose配置**：
```yaml
version: '3'
services:
  db:
    image: postgres:14
    environment:
      POSTGRES_USER: ai2db
      POSTGRES_PASSWORD: ${DB_PASSWORD}
      POSTGRES_DB: ai2db
    volumes:
      - postgres_data:/var/lib/postgresql/data
    
  backend:
    build: ./backend
    depends_on:
      - db
    environment:
      DB_URL: jdbc:postgresql://db:5432/ai2db
      DB_USER: ai2db
      DB_PASSWORD: ${DB_PASSWORD}
      
  frontend:
    build: ./frontend
    ports:
      - "80:80"
    depends_on:
      - backend

volumes:
  postgres_data:
```

**实现步骤**：
1. 创建Docker配置
2. 设置环境变量配置
3. 准备部署脚本
4. 实现基本的健康检查
5. 添加监控支持

## 监控与日志系统

### 监控设计

1. **系统监控**
   - 服务器资源使用率（CPU、内存、磁盘、网络）
   - 服务健康状态和可用性
   - 关键进程监控

2. **业务监控**
   - 查询执行统计（成功率、响应时间、错误率）
   - 用户活跃度和连接使用情况
   - NL2SQL转换准确率

3. **监控技术栈**
```yaml
# Prometheus配置示例
scrape_configs:
  - job_name: 'ai2db-backend'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['backend:8080']
  
  - job_name: 'ai2db-db'
    static_configs:
      - targets: ['db:5432']
```

### 日志系统

1. **日志收集**
   - 结构化日志格式（JSON）
   - 关键操作审计日志
   - 错误和异常详细日志

2. **日志存储与分析**
   - ELK栈（Elasticsearch, Logstash, Kibana）
   - 日志保留策略（30天业务日志，1年审计日志）
   - 关键日志告警规则

3. **日志规范**
```kotlin
// 日志记录示例
logger.info("Query executed", 
    mapOf(
        "queryId" to queryId,
        "executionTimeMs" to executionTime,
        "userId" to userId,
        "connectionId" to connectionId
    )
)
```

## 错误处理机制

### 前端错误处理
1. **错误分类**
   - 用户输入错误
   - 网络通信错误
   - 业务逻辑错误

2. **错误展示**
   - 友好的错误提示
   - 错误代码和详细说明
   - 解决方案建议

### API错误规范
```json
{
  "error": {
    "code": "INVALID_QUERY",
    "message": "The provided query is invalid",
    "details": "Missing required parameter: connectionId",
    "timestamp": "2023-07-20T10:15:30Z"
  }
}
```

### 数据库错误恢复
1. **连接错误**
   - 自动重试机制
   - 连接池健康检查
   - 故障转移策略

2. **查询错误**
   - 事务回滚机制
   - 查询超时处理
   - 资源释放保证

## 测试策略与质量保证

### 测试金字塔实施

1. **单元测试**
   - 核心业务逻辑全覆盖
   - 最小化mock使用
   - 目标覆盖率：业务代码80%+
```kotlin
class NL2SQLConverterTest {
    @Test
    fun `should convert simple select query`() {
        val converter = NL2SQLConverter()
        val result = converter.convertToSQL("show me all users", DatabaseType.MYSQL, testSchema)
        assertEquals("SELECT * FROM users", result.sql)
    }
}
```

2. **集成测试**
   - 测试组件间交互
   - 包含数据库操作测试
   - 使用测试容器管理依赖服务

3. **端到端测试**
   - 关键用户旅程覆盖
   - API契约测试
   - UI自动化测试

### 质量门禁
1. **代码质量**
   - 静态代码分析（SonarQube）
   - 代码风格检查（ktlint/ESLint）
   - 禁止严重级别问题

2. **测试要求**
   - 单元测试通过率100%
   - 集成测试通过率95%+
   - 关键路径E2E测试100%通过

## CI/CD流程

### 持续集成
```yaml
# GitHub Actions示例
name: CI Pipeline
on: [push, pull_request]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          distribution: 'temurin'
          java-version: '17'
      
      - name: Build and Test
        run: ./gradlew build
      
      - name: Upload Test Results
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: test-results
          path: build/test-results
```

### 持续部署
1. **环境策略**
   - 开发环境：自动部署每个合并
   - 预发环境：手动触发部署
   - 生产环境：审批后部署

2. **发布流程**
   - 版本号遵循语义化版本
   - 变更日志自动生成
   - 蓝绿部署策略

## 性能优化建议

### 数据库层优化
1. **查询优化**
   - 添加索引建议功能
   - 慢查询监控
   - 执行计划分析

2. **连接管理**
   - 动态连接池调整
   - 空闲连接回收
   - 连接健康检查

### 应用层优化
1. **缓存策略**
   - 模式信息缓存
   - 查询结果缓存
   - 向量相似度缓存

2. **异步处理**
   - 长时间查询异步执行
   - 批量模式同步
   - 后台向量生成

## 详细实施计划

### 第1阶段：基础框架构建（1-4周）

| 周次 | 关键任务 | 预期成果 |
|------|----------|----------|
| 第1周 | - 项目初始化<br>- 搭建基本项目结构<br>- 实现核心接口定义 | - 项目骨架<br>- 基础数据模型<br>- 核心接口文档 |
| 第2周 | - 实现MySQL连接器<br>- 开发基础REST API<br>- 设计数据库模式 | - 可用的MySQL连接<br>- 基本API端点<br>- 数据库初始化脚本 |
| 第3周 | - 实现PostgreSQL连接器<br>- 开发简单NL2SQL转换器<br>- 创建前端项目结构 | - 多数据库支持<br>- 简单查询转换功能<br>- 前端基础框架 |
| 第4周 | - 实现连接管理界面<br>- 开发基本查询界面<br>- 集成测试 | - 可用的MVP版本<br>- 基本连接和查询功能<br>- 测试报告 |

### 第2阶段：核心功能实现（5-12周）

| 周次 | 关键任务 | 预期成果 |
|------|----------|----------|
| 第5-6周 | - 增强NL2SQL转换器<br>- 实现模式抽取功能<br>- 开发查询结果展示 | - 改进的查询转换<br>- 模式元数据管理<br>- 结果展示组件 |
| 第7-8周 | - 实现用户认证<br>- 开发查询历史记录<br>- 添加基本可视化功能 | - 登录和权限系统<br>- 历史记录功能<br>- 基础图表展示 |
| 第9-10周 | - 实现会话上下文管理<br>- 开发模式浏览器<br>- 增强查询优化功能 | - 多轮对话支持<br>- 交互式模式浏览<br>- 查询性能优化 |
| 第11-12周 | - 实现数据导出功能<br>- 开发查询解释功能<br>- 系统集成测试 | - 多格式导出<br>- 查询过程解释<br>- 测试报告与修复 |

### 第3阶段：高级功能与优化（13-20周）

| 周次 | 关键任务 | 预期成果 |
|------|----------|----------|
| 第13-14周 | - 实现高级可视化<br>- 开发NoSQL支持<br>- 添加洞察生成功能 | - 交互式图表<br>- MongoDB连接器<br>- 基础数据洞察 |
| 第15-16周 | - 实现安全增强<br>- 开发性能监控<br>- 添加主题支持 | - 细粒度权限控制<br>- 性能指标仪表盘<br>- 亮/暗主题切换 |
| 第17-18周 | - 实现数据敏感度标记<br>- 开发插件系统<br>- 添加批处理查询 | - 敏感数据保护<br>- 基础插件支持<br>- 大型查询处理 |
| 第19-20周 | - 实现Docker部署<br>- 开发系统管理功能<br>- 性能优化 | - 容器化部署<br>- 管理控制台<br>- 性能测试报告 |

### 第4阶段：产品化与扩展（21-24周）

| 周次 | 关键任务 | 预期成果 |
|------|----------|----------|
| 第21-22周 | - 文档完善<br>- 云数据库支持<br>- 用户体验优化 | - 完整文档<br>- 云服务连接器<br>- UX改进报告 |
| 第23-24周 | - 最终测试<br>- Bug修复<br>- 发布准备 | - 测试报告<br>- 稳定版本<br>- 发布包 |

## 集成与扩展计划

### 与Kastrax生态系统集成

1. **与Kastrax Core集成**
   - 利用Kastrax的LLM适配器进行自然语言处理
   - 整合Kastrax的向量存储功能
   - 复用Kastrax的认证和安全模块

2. **与其他Kastrax模块集成**
   - 与Kastrax Agent系统集成，允许Agent访问数据库
   - 与Kastrax Workflow集成，支持数据处理工作流
   - 与Kastrax UI组件库集成，保持UI一致性

### 未来扩展方向

1. **高级功能扩展**
   - 自动模式优化建议
   - 预测性查询缓存
   - 跨数据库查询联合

2. **行业特定模块**
   - 金融分析模板
   - 电商数据模型
   - 医疗数据合规工具

3. **集成与互操作性**
   - BI工具导出集成
   - ETL流程支持
   - 第三方数据源连接器

## 结论

KastraX AI2DB项目的成功实施需要专注于核心功能的稳定实现，然后逐步扩展更高级的功能。建议按照上述优先级顺序进行开发，确保每个阶段都有可用的功能。特别要关注NL2SQL转换的准确性和数据库连接的稳定性，这是系统的基础。

通过充分利用Kastrax平台的强大能力，特别是其LLM和向量存储功能，可以构建一个显著优于市场上其他产品的智能数据库接口，为用户提供更自然、更高效的数据交互体验，同时保持与Kastrax生态系统的无缝集成。 
