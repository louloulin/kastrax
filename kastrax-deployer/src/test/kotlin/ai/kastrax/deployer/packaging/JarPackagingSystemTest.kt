package ai.kastrax.deployer.packaging

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JarPackagingSystemTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    @Test
    fun `test validate package with valid JAR`() {
        // 创建一个简单的 JAR 文件
        val jarFile = createSimpleJar(tempDir)
        
        val packagingSystem = JarPackagingSystem()
        val isValid = packagingSystem.validatePackage(jarFile.toFile())
        
        assertTrue(isValid, "Valid JAR file should be validated successfully")
    }
    
    @Test
    fun `test validate package with invalid file`() {
        // 创建一个普通文件，而不是 JAR
        val invalidFile = tempDir.resolve("invalid.txt")
        Files.write(invalidFile, "Not a JAR file".toByteArray())
        
        val packagingSystem = JarPackagingSystem()
        val isValid = packagingSystem.validatePackage(invalidFile.toFile())
        
        assertFalse(isValid, "Invalid file should not be validated as JAR")
    }
    
    @Test
    fun `test package application with non-existent project path`() {
        val nonExistentPath = tempDir.resolve("non-existent")
        val config = PackagingConfig(name = "test-app", version = "1.0.0")
        
        val packagingSystem = JarPackagingSystem()
        val result = packagingSystem.packageApplication(nonExistentPath, config)
        
        assertFalse(result.success, "Packaging should fail with non-existent project path")
        assertEquals("Project path does not exist: $nonExistentPath", result.message)
    }
    
    @Test
    fun `test package application with non-gradle non-maven project`() {
        val projectPath = tempDir.resolve("non-gradle-maven")
        Files.createDirectories(projectPath)
        
        val config = PackagingConfig(name = "test-app", version = "1.0.0")
        
        val packagingSystem = JarPackagingSystem()
        val result = packagingSystem.packageApplication(projectPath, config)
        
        assertFalse(result.success, "Packaging should fail with non-gradle non-maven project")
        assertEquals("Not a Gradle or Maven project", result.message)
    }
    
    /**
     * 创建一个简单的 JAR 文件。
     *
     * @param dir 目录
     * @return JAR 文件路径
     */
    private fun createSimpleJar(dir: Path): Path {
        val jarPath = dir.resolve("simple.jar")
        
        // 创建一个临时目录来存放 JAR 内容
        val contentDir = dir.resolve("content")
        Files.createDirectories(contentDir)
        
        // 创建 META-INF 目录和 MANIFEST.MF 文件
        val metaInfDir = contentDir.resolve("META-INF")
        Files.createDirectories(metaInfDir)
        
        val manifestFile = metaInfDir.resolve("MANIFEST.MF")
        Files.write(manifestFile, "Manifest-Version: 1.0\nCreated-By: JarPackagingSystemTest\n".toByteArray())
        
        // 创建一个简单的类文件
        val classDir = contentDir.resolve("com/example")
        Files.createDirectories(classDir)
        
        val classFile = classDir.resolve("Example.class")
        Files.write(classFile, "Dummy class file content".toByteArray())
        
        // 使用 jar 命令创建 JAR 文件
        val processBuilder = ProcessBuilder()
            .command("jar", "cf", jarPath.toString(), "-C", contentDir.toString(), ".")
            .directory(dir.toFile())
        
        val process = processBuilder.start()
        val exitCode = process.waitFor()
        
        if (exitCode != 0) {
            throw RuntimeException("Failed to create JAR file, exit code: $exitCode")
        }
        
        return jarPath
    }
}
