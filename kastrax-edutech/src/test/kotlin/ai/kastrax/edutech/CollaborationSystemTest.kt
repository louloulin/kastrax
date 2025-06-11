package ai.kastrax.edutech

import ai.kastrax.edutech.auth.AuthService
import ai.kastrax.edutech.collaboration.*
import ai.kastrax.edutech.models.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

/**
 * Week 17-18 实时协作学习系统测试
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CollaborationSystemTest {
    
    private lateinit var authService: AuthService
    private lateinit var collaborationService: RealTimeCollaborationService
    
    @BeforeAll
    fun setup() {
        println("🚀 初始化协作学习系统测试环境...")
        authService = AuthService()
        collaborationService = RealTimeCollaborationService(authService)
        println("✅ 协作学习系统测试环境初始化完成")
    }
    
    @Test
    @DisplayName("CS-001: 协作会话创建和管理测试")
    fun testCollaborationSessionCreationAndManagement() = runBlocking {
        println("\n🤝 测试协作会话创建和管理...")
        
        // 1. 创建协作学习会话
        val creatorId = "teacher001"
        val courseId = CourseId.generate()
        val sessionResult = collaborationService.createCollaborationSession(
            creatorId = creatorId,
            courseId = courseId,
            title = "数学协作学习",
            description = "线性代数协作学习会话",
            maxParticipants = 5
        )
        
        assertTrue(sessionResult is CollaborationSessionResult.Success, "协作会话创建应该成功")
        val sessionId = (sessionResult as CollaborationSessionResult.Success).sessionId
        println("✅ 协作会话创建成功: $sessionId")
        
        // 2. 验证会话信息
        val session = collaborationService.getCollaborationSession(sessionId)
        assertNotNull(session, "应该能够获取协作会话信息")
        assertEquals(creatorId, session!!.creatorId, "创建者ID应该匹配")
        assertEquals(courseId, session.courseId, "课程ID应该匹配")
        assertEquals("数学协作学习", session.title, "标题应该匹配")
        assertEquals(5, session.maxParticipants, "最大参与者数应该匹配")
        assertEquals(CollaborationStatus.ACTIVE, session.status, "会话状态应该是活跃的")
        assertTrue(session.participants.contains(creatorId), "创建者应该在参与者列表中")
        
        println("✅ 协作会话信息验证通过")
    }
    
    @Test
    @DisplayName("CS-002: 用户加入和离开协作会话测试")
    fun testUserJoinAndLeaveCollaborationSession() = runBlocking {
        println("\n👥 测试用户加入和离开协作会话...")
        
        // 1. 创建协作会话
        val creatorId = "teacher002"
        val courseId = CourseId.generate()
        val sessionResult = collaborationService.createCollaborationSession(
            creatorId = creatorId,
            courseId = courseId,
            title = "物理协作学习",
            description = "力学协作学习会话",
            maxParticipants = 3
        )
        
        assertTrue(sessionResult is CollaborationSessionResult.Success)
        val sessionId = (sessionResult as CollaborationSessionResult.Success).sessionId
        
        // 2. 学生加入协作会话
        val student1Id = "student001"
        val student2Id = "student002"
        
        val joinResult1 = collaborationService.joinCollaborationSession(student1Id, sessionId)
        assertTrue(joinResult1 is CollaborationJoinResult.Success, "学生1加入应该成功")
        println("✅ 学生1成功加入协作会话")
        
        val joinResult2 = collaborationService.joinCollaborationSession(student2Id, sessionId)
        assertTrue(joinResult2 is CollaborationJoinResult.Success, "学生2加入应该成功")
        println("✅ 学生2成功加入协作会话")
        
        // 3. 验证参与者列表
        val session = collaborationService.getCollaborationSession(sessionId)
        assertNotNull(session)
        assertEquals(3, session!!.participants.size, "应该有3个参与者")
        assertTrue(session.participants.contains(creatorId), "应该包含创建者")
        assertTrue(session.participants.contains(student1Id), "应该包含学生1")
        assertTrue(session.participants.contains(student2Id), "应该包含学生2")
        
        // 4. 测试会话已满的情况
        val student3Id = "student003"
        val joinResult3 = collaborationService.joinCollaborationSession(student3Id, sessionId)
        assertTrue(joinResult3 is CollaborationJoinResult.Failure, "会话已满时加入应该失败")
        
        println("✅ 用户加入和离开协作会话测试通过")
    }
    
    @Test
    @DisplayName("CS-003: 协作消息发送和接收测试")
    fun testCollaborativeMessaging() = runBlocking {
        println("\n💬 测试协作消息发送和接收...")
        
        // 1. 创建协作会话并添加参与者
        val creatorId = "teacher003"
        val studentId = "student003"
        val courseId = CourseId.generate()
        
        val sessionResult = collaborationService.createCollaborationSession(
            creatorId = creatorId,
            courseId = courseId,
            title = "化学协作学习",
            description = "有机化学协作学习会话"
        )
        
        assertTrue(sessionResult is CollaborationSessionResult.Success)
        val sessionId = (sessionResult as CollaborationSessionResult.Success).sessionId
        
        val joinResult = collaborationService.joinCollaborationSession(studentId, sessionId)
        assertTrue(joinResult is CollaborationJoinResult.Success)
        
        // 2. 发送不同类型的消息
        val chatMessageResult = collaborationService.sendMessage(
            sessionId = sessionId,
            senderId = studentId,
            messageType = MessageType.CHAT,
            content = "大家好，我对有机化学有些疑问"
        )
        
        assertTrue(chatMessageResult is MessageSendResult.Success, "聊天消息发送应该成功")
        println("✅ 聊天消息发送成功")
        
        val discussionMessageResult = collaborationService.sendMessage(
            sessionId = sessionId,
            senderId = creatorId,
            messageType = MessageType.DISCUSSION,
            content = "我们来讨论一下苯环的结构",
            metadata = mapOf("threadId" to "thread_001")
        )
        
        assertTrue(discussionMessageResult is MessageSendResult.Success, "讨论消息发送应该成功")
        println("✅ 讨论消息发送成功")
        
        val resourceShareResult = collaborationService.sendMessage(
            sessionId = sessionId,
            senderId = creatorId,
            messageType = MessageType.RESOURCE_SHARE,
            content = "分享有机化学参考资料",
            metadata = mapOf(
                "resourceName" to "有机化学教程",
                "resourceType" to "pdf",
                "resourceUrl" to "https://example.com/organic-chemistry.pdf"
            )
        )
        
        assertTrue(resourceShareResult is MessageSendResult.Success, "资源分享消息发送应该成功")
        println("✅ 资源分享消息发送成功")
        
        // 3. 验证会话中的共享资源
        val session = collaborationService.getCollaborationSession(sessionId)
        assertNotNull(session)
        assertEquals(1, session!!.sharedResources.size, "应该有1个共享资源")
        
        val sharedResource = session.sharedResources.first()
        assertEquals("有机化学教程", sharedResource.name, "资源名称应该匹配")
        assertEquals("pdf", sharedResource.type, "资源类型应该匹配")
        assertEquals(creatorId, sharedResource.sharedBy, "分享者应该匹配")
        
        println("✅ 协作消息发送和接收测试通过")
    }
    
    @Test
    @DisplayName("CS-004: 协作笔记创建和编辑测试")
    fun testCollaborativeNotes() = runBlocking {
        println("\n📝 测试协作笔记创建和编辑...")
        
        // 1. 创建协作会话
        val creatorId = "teacher004"
        val studentId = "student004"
        val courseId = CourseId.generate()
        
        val sessionResult = collaborationService.createCollaborationSession(
            creatorId = creatorId,
            courseId = courseId,
            title = "生物协作学习",
            description = "细胞生物学协作学习会话"
        )
        
        assertTrue(sessionResult is CollaborationSessionResult.Success)
        val sessionId = (sessionResult as CollaborationSessionResult.Success).sessionId
        
        val joinResult = collaborationService.joinCollaborationSession(studentId, sessionId)
        assertTrue(joinResult is CollaborationJoinResult.Success)
        
        // 2. 创建协作笔记
        val noteResult = collaborationService.createCollaborativeNote(
            sessionId = sessionId,
            creatorId = studentId,
            title = "细胞结构笔记",
            content = "细胞是生物体的基本单位，包含细胞膜、细胞质和细胞核..."
        )
        
        assertTrue(noteResult is CollaborativeNoteResult.Success, "协作笔记创建应该成功")
        val note = (noteResult as CollaborativeNoteResult.Success).note
        
        assertEquals("细胞结构笔记", note.title, "笔记标题应该匹配")
        assertEquals(studentId, note.creatorId, "笔记创建者应该匹配")
        assertTrue(note.contributors.contains(studentId), "创建者应该在贡献者列表中")
        assertEquals(1, note.version, "初始版本应该是1")
        
        println("✅ 协作笔记创建成功: ${note.title}")
        
        // 3. 更新协作笔记
        val updateResult = collaborationService.updateCollaborativeNote(
            sessionId = sessionId,
            noteId = note.id,
            userId = creatorId,
            newContent = note.content + "\n\n补充：线粒体是细胞的能量工厂。"
        )
        
        assertTrue(updateResult is CollaborativeNoteResult.Success, "笔记更新应该成功")
        val updatedNote = (updateResult as CollaborativeNoteResult.Success).note
        
        assertEquals(2, updatedNote.version, "更新后版本应该是2")
        assertTrue(updatedNote.contributors.contains(creatorId), "更新者应该在贡献者列表中")
        assertTrue(updatedNote.content.contains("线粒体"), "更新的内容应该包含新添加的文本")
        
        println("✅ 协作笔记更新成功，版本: ${updatedNote.version}")
        
        // 4. 添加笔记标注
        val annotation = NoteAnnotation(
            content = "这里需要更详细的解释",
            type = AnnotationType.COMMENT,
            position = AnnotationPosition(0, 10, "细胞是生物体")
        )
        
        val annotationResult = collaborationService.addNoteAnnotation(
            sessionId = sessionId,
            noteId = note.id,
            userId = creatorId,
            annotation = annotation
        )
        
        assertTrue(annotationResult is AnnotationResult.Success, "标注添加应该成功")
        val addedAnnotation = (annotationResult as AnnotationResult.Success).annotation
        
        assertEquals(creatorId, addedAnnotation.authorId, "标注作者应该匹配")
        assertEquals(AnnotationType.COMMENT, addedAnnotation.type, "标注类型应该匹配")
        assertFalse(addedAnnotation.isResolved, "新标注应该是未解决状态")
        
        println("✅ 笔记标注添加成功")
        
        // 5. 验证会话中的协作笔记
        val session = collaborationService.getCollaborationSession(sessionId)
        assertNotNull(session)
        assertEquals(1, session!!.collaborativeNotes.size, "应该有1个协作笔记")
        
        val sessionNote = session.collaborativeNotes[note.id]
        assertNotNull(sessionNote, "会话中应该包含创建的笔记")
        assertEquals(1, sessionNote!!.annotations.size, "笔记应该有1个标注")
        
        println("✅ 协作笔记创建和编辑测试通过")
    }
    
    @Test
    @DisplayName("CS-005: 协作会话结束测试")
    fun testCollaborationSessionEnd() = runBlocking {
        println("\n🔚 测试协作会话结束...")
        
        // 1. 创建协作会话
        val creatorId = "teacher005"
        val courseId = CourseId.generate()
        
        val sessionResult = collaborationService.createCollaborationSession(
            creatorId = creatorId,
            courseId = courseId,
            title = "历史协作学习",
            description = "中国古代史协作学习会话"
        )
        
        assertTrue(sessionResult is CollaborationSessionResult.Success)
        val sessionId = (sessionResult as CollaborationSessionResult.Success).sessionId
        
        // 2. 添加一些参与者
        val studentId = "student005"
        val joinResult = collaborationService.joinCollaborationSession(studentId, sessionId)
        assertTrue(joinResult is CollaborationJoinResult.Success)
        
        // 3. 结束协作会话
        val endResult = collaborationService.endCollaborationSession(sessionId, creatorId)
        assertTrue(endResult is CollaborationEndResult.Success, "协作会话结束应该成功")
        
        // 4. 验证会话状态
        val session = collaborationService.getCollaborationSession(sessionId)
        assertNotNull(session)
        assertEquals(CollaborationStatus.ENDED, session!!.status, "会话状态应该是已结束")
        assertNotNull(session.endedAt, "结束时间应该被设置")
        
        println("✅ 协作会话结束成功")
        
        // 5. 测试非创建者结束会话（应该失败）
        val sessionResult2 = collaborationService.createCollaborationSession(
            creatorId = creatorId,
            courseId = courseId,
            title = "测试会话2",
            description = "测试非创建者结束会话"
        )
        
        assertTrue(sessionResult2 is CollaborationSessionResult.Success)
        val sessionId2 = (sessionResult2 as CollaborationSessionResult.Success).sessionId
        
        val endResult2 = collaborationService.endCollaborationSession(sessionId2, studentId)
        assertTrue(endResult2 is CollaborationEndResult.Failure, "非创建者结束会话应该失败")
        
        println("✅ 协作会话结束测试通过")
    }
    
    @AfterAll
    fun cleanup() {
        println("\n🧹 清理协作学习系统测试环境...")
        println("✅ 协作学习系统测试完成")
    }
}
