package ai.kastrax.examples;

/**
 * 一个简单的Java示例，用于测试编译和运行
 */
public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello, Kastrax!");
        System.out.println("这是一个简单的Java示例，用于测试编译和运行。");
        
        if (args.length > 0) {
            System.out.println("你提供的参数是: " + String.join(", ", args));
        }
    }
}
