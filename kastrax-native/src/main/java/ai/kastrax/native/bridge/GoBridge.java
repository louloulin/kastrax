package ai.kastrax.native.bridge;

/**
 * JNI Bridge for Go SDK integration.
 * This class provides native methods that will be implemented in Go.
 */
public class GoBridge {
    
    static {
        try {
            System.loadLibrary("kastrax_go");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Native code library failed to load: " + e.getMessage());
        }
    }
    
    /**
     * Initialize the Go SDK.
     * 
     * @param configJson Configuration in JSON format
     * @return Status code (0 for success)
     */
    public static native int initialize(String configJson);
    
    /**
     * Create an agent using the Go SDK.
     * 
     * @param agentJson Agent configuration in JSON format
     * @return Agent ID or negative value on error
     */
    public static native long createAgent(String agentJson);
    
    /**
     * Run an agent with the given input.
     * 
     * @param agentId Agent ID returned by createAgent
     * @param input User input text
     * @return Response from the agent
     */
    public static native String runAgent(long agentId, String input);
    
    /**
     * Add a tool to an agent.
     * 
     * @param agentId Agent ID
     * @param toolJson Tool configuration in JSON format
     * @return Status code (0 for success)
     */
    public static native int addTool(long agentId, String toolJson);
    
    /**
     * Release resources associated with an agent.
     * 
     * @param agentId Agent ID to release
     */
    public static native void releaseAgent(long agentId);
    
    /**
     * Shutdown the Go SDK and release all resources.
     */
    public static native void shutdown();
}
