package ai.kastrax.graal;

import java.time.LocalDateTime;

/**
 * 一个纯 Java 的主程序，不使用 Kotlin 反射和序列化
 */
public class JavaMain {
    public static void main(String[] args) {
        System.out.println("KastraX Native Image (Java)");
        System.out.println("===========================");
        
        if (args.length == 0) {
            printHelp();
            return;
        }
        
        String command = args[0];
        switch (command) {
            case "help":
                printHelp();
                break;
            case "version":
                printVersion();
                break;
            case "config":
                printConfig();
                break;
            default:
                System.out.println("未知命令: " + command);
                printHelp();
                break;
        }
    }
    
    private static void printHelp() {
        System.out.println("可用命令:");
        System.out.println("  help    - 显示帮助信息");
        System.out.println("  version - 显示版本信息");
        System.out.println("  config  - 显示配置信息");
    }
    
    private static void printVersion() {
        System.out.println("KastraX 版本: 0.1.0");
        System.out.println("构建时间: " + LocalDateTime.now());
        System.out.println("Java 版本: " + System.getProperty("java.version"));
        System.out.println("OS: " + System.getProperty("os.name"));
    }
    
    private static void printConfig() {
        System.out.println("配置信息:");
        System.out.println("  工作目录: " + System.getProperty("user.dir"));
        System.out.println("  用户主目录: " + System.getProperty("user.home"));
        System.out.println("  临时目录: " + System.getProperty("java.io.tmpdir"));
        System.out.println("  文件分隔符: " + System.getProperty("file.separator"));
        System.out.println("  路径分隔符: " + System.getProperty("path.separator"));
        System.out.println("  行分隔符: " + System.getProperty("line.separator")
                .replace("\n", "\\n")
                .replace("\r", "\\r"));
    }
}
