package ai.kastrax.cli

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertTrue

class KastraXCliTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    @Test
    fun `test cli initialization`() {
        val cli = KastraXCli()
        // 简单测试 CLI 初始化不会抛出异常
        assertTrue(true, "CLI initialized successfully")
    }
}
