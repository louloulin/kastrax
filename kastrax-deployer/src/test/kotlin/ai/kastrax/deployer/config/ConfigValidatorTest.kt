package ai.kastrax.deployer.config

import ai.kastrax.deployer.DeploymentConfig
import ai.kastrax.deployer.ResourceConfig
import ai.kastrax.deployer.docker.DockerConfig
import ai.kastrax.deployer.kubernetes.KubernetesConfig
import ai.kastrax.deployer.serverless.LambdaConfig
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfigValidatorTest {
    
    @Test
    fun `test validate deployment config with valid config`() {
        val config = DeploymentConfig(
            name = "test-app",
            version = "1.0.0",
            environment = mapOf("KEY" to "value"),
            resources = ResourceConfig(
                memory = 512,
                cpu = 2,
                timeout = 30,
                concurrency = 10
            )
        )
        
        val result = ConfigValidator.validateDeploymentConfig(config)
        
        assertTrue(result.valid, "Valid deployment config should be validated successfully")
        assertEquals(0, result.errors.size, "There should be no errors")
    }
    
    @Test
    fun `test validate deployment config with invalid name`() {
        val config = DeploymentConfig(
            name = "",
            version = "1.0.0"
        )
        
        val result = ConfigValidator.validateDeploymentConfig(config)
        
        assertFalse(result.valid, "Invalid deployment config should not be validated")
        assertTrue(result.errors.any { it.contains("name cannot be empty") }, "Error should mention empty name")
    }
    
    @Test
    fun `test validate deployment config with invalid version`() {
        val config = DeploymentConfig(
            name = "test-app",
            version = "invalid"
        )
        
        val result = ConfigValidator.validateDeploymentConfig(config)
        
        assertFalse(result.valid, "Invalid deployment config should not be validated")
        assertTrue(result.errors.any { it.contains("Invalid version format") }, "Error should mention invalid version format")
    }
    
    @Test
    fun `test validate resource config with valid config`() {
        val config = ResourceConfig(
            memory = 512,
            cpu = 2,
            timeout = 30,
            concurrency = 10
        )
        
        val result = ConfigValidator.validateResourceConfig(config)
        
        assertTrue(result.valid, "Valid resource config should be validated successfully")
        assertEquals(0, result.errors.size, "There should be no errors")
    }
    
    @Test
    fun `test validate resource config with invalid memory`() {
        val config = ResourceConfig(
            memory = 0,
            cpu = 2,
            timeout = 30,
            concurrency = 10
        )
        
        val result = ConfigValidator.validateResourceConfig(config)
        
        assertFalse(result.valid, "Invalid resource config should not be validated")
        assertTrue(result.errors.any { it.contains("Memory must be greater than 0") }, "Error should mention invalid memory")
    }
    
    @Test
    fun `test validate docker config with valid config`() {
        val config = DockerConfig(
            baseImage = "openjdk:17-slim",
            port = 8080,
            hostPort = 8080
        )
        
        val result = ConfigValidator.validateDockerConfig(config)
        
        assertTrue(result.valid, "Valid docker config should be validated successfully")
        assertEquals(0, result.errors.size, "There should be no errors")
    }
    
    @Test
    fun `test validate docker config with invalid port`() {
        val config = DockerConfig(
            baseImage = "openjdk:17-slim",
            port = 0,
            hostPort = 8080
        )
        
        val result = ConfigValidator.validateDockerConfig(config)
        
        assertFalse(result.valid, "Invalid docker config should not be validated")
        assertTrue(result.errors.any { it.contains("Port must be between 1 and 65535") }, "Error should mention invalid port")
    }
    
    @Test
    fun `test validate kubernetes config with valid config`() {
        val config = KubernetesConfig(
            namespace = "default",
            replicas = 2,
            serviceType = "ClusterIP",
            dockerConfig = DockerConfig(
                baseImage = "openjdk:17-slim",
                port = 8080,
                hostPort = 8080
            )
        )
        
        val result = ConfigValidator.validateKubernetesConfig(config)
        
        assertTrue(result.valid, "Valid kubernetes config should be validated successfully")
        assertEquals(0, result.errors.size, "There should be no errors")
    }
    
    @Test
    fun `test validate kubernetes config with invalid service type`() {
        val config = KubernetesConfig(
            namespace = "default",
            replicas = 2,
            serviceType = "InvalidType",
            dockerConfig = DockerConfig(
                baseImage = "openjdk:17-slim",
                port = 8080,
                hostPort = 8080
            )
        )
        
        val result = ConfigValidator.validateKubernetesConfig(config)
        
        assertFalse(result.valid, "Invalid kubernetes config should not be validated")
        assertTrue(result.errors.any { it.contains("Invalid service type") }, "Error should mention invalid service type")
    }
    
    @Test
    fun `test validate lambda config with valid config`() {
        val config = LambdaConfig(
            region = "us-east-1",
            runtime = "java11",
            handler = "ai.kastrax.Handler::handleRequest",
            role = "arn:aws:iam::123456789012:role/lambda-role",
            bucketName = "kastrax-deployments"
        )
        
        val result = ConfigValidator.validateLambdaConfig(config)
        
        assertTrue(result.valid, "Valid lambda config should be validated successfully")
        assertEquals(0, result.errors.size, "There should be no errors")
    }
    
    @Test
    fun `test validate lambda config with invalid handler`() {
        val config = LambdaConfig(
            region = "us-east-1",
            runtime = "java11",
            handler = "invalid-handler",
            role = "arn:aws:iam::123456789012:role/lambda-role",
            bucketName = "kastrax-deployments"
        )
        
        val result = ConfigValidator.validateLambdaConfig(config)
        
        assertFalse(result.valid, "Invalid lambda config should not be validated")
        assertTrue(result.errors.any { it.contains("Invalid handler format") }, "Error should mention invalid handler format")
    }
}
