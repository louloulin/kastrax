package ai.kastrax.graal;

/**
 * 一个简单的 Java Hello World 程序
 */
public class JavaHello {
    public static void main(String[] args) {
        System.out.println("Hello, Native World from Java!");
        
        // 显示命令行参数
        if (args.length > 0) {
            System.out.println("Arguments:");
            for (int i = 0; i < args.length; i++) {
                System.out.println("  " + i + ": " + args[i]);
            }
        }
        
        // 显示系统信息
        System.out.println("System information:");
        System.out.println("  OS: " + System.getProperty("os.name"));
        System.out.println("  Java version: " + System.getProperty("java.version"));
        System.out.println("  User: " + System.getProperty("user.name"));
    }
}
