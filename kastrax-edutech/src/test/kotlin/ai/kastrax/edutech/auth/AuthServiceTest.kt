package ai.kastrax.edutech.auth

import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * 认证服务测试
 * 
 * 验证ed2.md第一阶段Week 3-4用户认证和授权系统
 */
class AuthServiceTest {
    
    private lateinit var authService: AuthService
    
    @BeforeTest
    fun setup() {
        authService = AuthService()
    }
    
    @Test
    fun `should generate valid JWT token`() = runTest {
        // Given
        val userId = "user123"
        val roles = listOf(Role.STUDENT)
        val tenantId = "tenant456"
        
        // When
        val token = authService.generateToken(userId, roles, tenantId)
        
        // Then
        assertNotNull(token.accessToken)
        assertNotNull(token.refreshToken)
        assertEquals("Bearer", token.tokenType)
        assertTrue(token.expiresIn > 0)
    }
    
    @Test
    fun `should validate token correctly`() = runTest {
        // Given
        val userId = "user123"
        val roles = listOf(Role.TEACHER)
        val token = authService.generateToken(userId, roles)
        
        // When
        val claims = authService.validateToken(token.accessToken)
        
        // Then
        assertNotNull(claims)
        assertEquals(userId, claims.subject)
    }
    
    @Test
    fun `should extract user ID from token`() = runTest {
        // Given
        val userId = "user123"
        val roles = listOf(Role.ADMIN)
        val token = authService.generateToken(userId, roles)
        
        // When
        val extractedUserId = authService.getUserIdFromToken(token.accessToken)
        
        // Then
        assertEquals(userId, extractedUserId)
    }
    
    @Test
    fun `should extract roles from token`() = runTest {
        // Given
        val userId = "user123"
        val roles = listOf(Role.TEACHER, Role.ADMIN)
        val token = authService.generateToken(userId, roles)
        
        // When
        val extractedRoles = authService.getRolesFromToken(token.accessToken)
        
        // Then
        assertEquals(roles.size, extractedRoles.size)
        assertTrue(extractedRoles.containsAll(roles))
    }
    
    @Test
    fun `should extract tenant ID from token`() = runTest {
        // Given
        val userId = "user123"
        val roles = listOf(Role.STUDENT)
        val tenantId = "tenant456"
        val token = authService.generateToken(userId, roles, tenantId)
        
        // When
        val extractedTenantId = authService.getTenantIdFromToken(token.accessToken)
        
        // Then
        assertEquals(tenantId, extractedTenantId)
    }
    
    @Test
    fun `should refresh token successfully`() = runTest {
        // Given
        val userId = "user123"
        val roles = listOf(Role.STUDENT)
        val originalToken = authService.generateToken(userId, roles)
        
        // When
        val refreshedToken = authService.refreshToken(originalToken.refreshToken)
        
        // Then
        assertNotNull(refreshedToken)
        assertNotEquals(originalToken.accessToken, refreshedToken!!.accessToken)
        assertEquals(userId, authService.getUserIdFromToken(refreshedToken.accessToken))
    }
    
    @Test
    fun `should check permissions correctly`() = runTest {
        // Given
        val studentRoles = listOf(Role.STUDENT)
        val teacherRoles = listOf(Role.TEACHER)
        
        // When & Then
        assertTrue(authService.hasPermission(studentRoles, Permission.VIEW_COURSE))
        assertFalse(authService.hasPermission(studentRoles, Permission.CREATE_COURSE))
        
        assertTrue(authService.hasPermission(teacherRoles, Permission.VIEW_COURSE))
        assertTrue(authService.hasPermission(teacherRoles, Permission.CREATE_COURSE))
        assertFalse(authService.hasPermission(teacherRoles, Permission.MANAGE_USERS))
    }
    
    @Test
    fun `should validate tenant access correctly`() = runTest {
        // Given
        val userTenantId = "tenant123"
        val resourceTenantId = "tenant123"
        val differentTenantId = "tenant456"
        val studentRoles = listOf(Role.STUDENT)
        val systemAdminRoles = listOf(Role.SYSTEM_ADMIN)
        
        // When & Then
        // 同租户访问
        assertTrue(authService.hasTenantAccess(userTenantId, resourceTenantId, studentRoles))
        
        // 不同租户访问
        assertFalse(authService.hasTenantAccess(userTenantId, differentTenantId, studentRoles))
        
        // 系统管理员可以访问所有租户
        assertTrue(authService.hasTenantAccess(userTenantId, differentTenantId, systemAdminRoles))
        
        // 资源没有租户ID时允许访问
        assertTrue(authService.hasTenantAccess(userTenantId, null, studentRoles))
    }
    
    @Test
    fun `should handle invalid token gracefully`() = runTest {
        // Given
        val invalidToken = "invalid.token.here"
        
        // When
        val claims = authService.validateToken(invalidToken)
        val userId = authService.getUserIdFromToken(invalidToken)
        val roles = authService.getRolesFromToken(invalidToken)
        val tenantId = authService.getTenantIdFromToken(invalidToken)
        
        // Then
        assertNull(claims)
        assertNull(userId)
        assertTrue(roles.isEmpty())
        assertNull(tenantId)
    }
    
    @Test
    fun `should handle refresh token validation`() = runTest {
        // Given
        val userId = "user123"
        val roles = listOf(Role.STUDENT)
        val token = authService.generateToken(userId, roles)
        
        // When - 尝试用访问令牌作为刷新令牌
        val refreshResult = authService.refreshToken(token.accessToken)
        
        // Then
        assertNull(refreshResult)
    }
    
    @Test
    fun `should validate role permissions`() = runTest {
        // Given & When & Then
        // 学生权限
        val studentPermissions = Role.STUDENT.permissions
        assertTrue(Permission.VIEW_COURSE in studentPermissions)
        assertTrue(Permission.SUBMIT_ASSIGNMENT in studentPermissions)
        assertFalse(Permission.CREATE_COURSE in studentPermissions)
        
        // 教师权限
        val teacherPermissions = Role.TEACHER.permissions
        assertTrue(Permission.VIEW_COURSE in teacherPermissions)
        assertTrue(Permission.CREATE_COURSE in teacherPermissions)
        assertTrue(Permission.GRADE_ASSIGNMENT in teacherPermissions)
        assertFalse(Permission.MANAGE_USERS in teacherPermissions)
        
        // 管理员权限
        val adminPermissions = Role.ADMIN.permissions
        assertTrue(Permission.MANAGE_USERS in adminPermissions)
        assertTrue(Permission.VIEW_ANALYTICS in adminPermissions)
        
        // 系统管理员权限
        val systemAdminPermissions = Role.SYSTEM_ADMIN.permissions
        assertEquals(Permission.values().toSet(), systemAdminPermissions)
    }
    
    @Test
    fun `should generate tokens with additional claims`() = runTest {
        // Given
        val userId = "user123"
        val roles = listOf(Role.TEACHER)
        val additionalClaims = mapOf(
            "department" to "Mathematics",
            "level" to "Senior"
        )
        
        // When
        val token = authService.generateToken(userId, roles, additionalClaims = additionalClaims)
        val claims = authService.validateToken(token.accessToken)
        
        // Then
        assertNotNull(claims)
        assertEquals("Mathematics", claims["department"])
        assertEquals("Senior", claims["level"])
    }
}
