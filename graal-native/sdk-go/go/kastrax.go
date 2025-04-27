package main

// #include <jni.h>
// #include <stdlib.h>
import "C"
import (
	"encoding/json"
	"fmt"
	"sync"
	"unsafe"
)

// Global state
var (
	agents      = make(map[int64]*Agent)
	nextAgentID int64 = 1
	agentsMutex sync.Mutex
	config      *Config
)

// Error type
type Error struct {
	Message string
}

func (e *Error) Error() string {
	return e.Message
}

// Config represents the SDK configuration
type Config struct {
	APIKey   string `json:"api_key"`
	LogLevel string `json:"log_level"`
}

// AgentConfig represents the configuration for creating an agent
type AgentConfig struct {
	Name    string       `json:"name"`
	Model   string       `json:"model"`
	Options AgentOptions `json:"options,omitempty"`
}

// AgentOptions represents additional options for an agent
type AgentOptions struct {
	Temperature float64 `json:"temperature,omitempty"`
	MaxTokens   int     `json:"max_tokens,omitempty"`
}

// AgentState represents the current state of an agent
type AgentState struct {
	ID        int64  `json:"id"`
	Name      string `json:"name"`
	Model     string `json:"model"`
	ToolCount int    `json:"tool_count"`
}

// Agent represents an AI agent
type Agent struct {
	ID          int64
	Name        string
	Model       string
	Temperature float64
	MaxTokens   int
	Tools       []Tool
}

// Tool represents a tool that can be used by an agent
type Tool struct {
	Name        string                      `json:"name"`
	Description string                      `json:"description,omitempty"`
	Function    func(string) (string, error) `json:"-"`
}

// ToolJSON is used for JSON serialization of Tool
type ToolJSON struct {
	Name        string `json:"name"`
	Description string `json:"description,omitempty"`
}

// Memory represents agent memory
type Memory struct {
	Type    string        `json:"type"`
	Options MemoryOptions `json:"options,omitempty"`
}

// MemoryOptions represents additional options for memory
type MemoryOptions struct {
	MaxMessages int `json:"max_messages,omitempty"`
}

// NewAgent creates a new agent with the given configuration
func NewAgent(config AgentConfig) (*Agent, error) {
	if config.Name == "" {
		return nil, &Error{Message: "Agent name is required"}
	}
	if config.Model == "" {
		return nil, &Error{Message: "Model name is required"}
	}

	agentsMutex.Lock()
	defer agentsMutex.Unlock()

	id := nextAgentID
	nextAgentID++

	temperature := 0.7
	if config.Options.Temperature != 0 {
		temperature = config.Options.Temperature
	}

	maxTokens := 1000
	if config.Options.MaxTokens != 0 {
		maxTokens = config.Options.MaxTokens
	}

	agent := &Agent{
		ID:          id,
		Name:        config.Name,
		Model:       config.Model,
		Temperature: temperature,
		MaxTokens:   maxTokens,
		Tools:       []Tool{},
	}

	agents[id] = agent
	return agent, nil
}

// Run executes the agent with the given input
func (a *Agent) Run(input string) (string, error) {
	// In a real implementation, this would call the JNI method to run the agent
	// For now, we just return a mock response
	toolNames := ""
	for i, tool := range a.Tools {
		if i > 0 {
			toolNames += ", "
		}
		toolNames += tool.Name
	}

	return fmt.Sprintf(
		"Agent '%s' (ID: %d) using model '%s' with tools [%s] response to: %s",
		a.Name, a.ID, a.Model, toolNames, input,
	), nil
}

// AddTool adds a tool to the agent
func (a *Agent) AddTool(tool Tool) error {
	a.Tools = append(a.Tools, tool)
	return nil
}

// GetState returns the current state of the agent
func (a *Agent) GetState() AgentState {
	return AgentState{
		ID:        a.ID,
		Name:      a.Name,
		Model:     a.Model,
		ToolCount: len(a.Tools),
	}
}

// Init initializes the SDK with the given configuration
func Init(cfg Config) error {
	config = &cfg
	return nil
}

// Shutdown releases all resources used by the SDK
func Shutdown() {
	agentsMutex.Lock()
	defer agentsMutex.Unlock()

	agents = make(map[int64]*Agent)
	config = nil
}

// NewMemory creates a new memory with the given configuration
func NewMemory(memType string, maxMessages int) *Memory {
	return &Memory{
		Type: memType,
		Options: MemoryOptions{
			MaxMessages: maxMessages,
		},
	}
}

//export Java_ai_kastrax_graal_bridge_GoBridge_initialize
func Java_ai_kastrax_graal_bridge_GoBridge_initialize(env *C.JNIEnv, class C.jclass, configJSON C.jstring) C.jint {
	configStr := jstringToString(env, configJSON)
	var cfg Config
	if err := json.Unmarshal([]byte(configStr), &cfg); err != nil {
		return C.jint(-1)
	}

	if err := Init(cfg); err != nil {
		return C.jint(-1)
	}

	return C.jint(0)
}

//export Java_ai_kastrax_graal_bridge_GoBridge_createAgent
func Java_ai_kastrax_graal_bridge_GoBridge_createAgent(env *C.JNIEnv, class C.jclass, agentJSON C.jstring) C.jlong {
	agentStr := jstringToString(env, agentJSON)
	var agentCfg AgentConfig
	if err := json.Unmarshal([]byte(agentStr), &agentCfg); err != nil {
		return C.jlong(-1)
	}

	agent, err := NewAgent(agentCfg)
	if err != nil {
		return C.jlong(-1)
	}

	return C.jlong(agent.ID)
}

//export Java_ai_kastrax_graal_bridge_GoBridge_runAgent
func Java_ai_kastrax_graal_bridge_GoBridge_runAgent(env *C.JNIEnv, class C.jclass, agentID C.jlong, input C.jstring) C.jstring {
	inputStr := jstringToString(env, input)

	agentsMutex.Lock()
	agent, exists := agents[int64(agentID)]
	agentsMutex.Unlock()

	if !exists {
		errMsg := fmt.Sprintf("Agent with ID %d not found", agentID)
		return stringToJstring(env, errMsg)
	}

	response, err := agent.Run(inputStr)
	if err != nil {
		errMsg := fmt.Sprintf("Error: %s", err.Error())
		return stringToJstring(env, errMsg)
	}

	return stringToJstring(env, response)
}

//export Java_ai_kastrax_graal_bridge_GoBridge_addTool
func Java_ai_kastrax_graal_bridge_GoBridge_addTool(env *C.JNIEnv, class C.jclass, agentID C.jlong, toolJSON C.jstring) C.jint {
	toolStr := jstringToString(env, toolJSON)
	var toolData map[string]interface{}
	if err := json.Unmarshal([]byte(toolStr), &toolData); err != nil {
		return C.jint(-1)
	}

	name, ok := toolData["name"].(string)
	if !ok {
		return C.jint(-1)
	}

	description := ""
	if desc, ok := toolData["description"].(string); ok {
		description = desc
	}

	agentsMutex.Lock()
	agent, exists := agents[int64(agentID)]
	agentsMutex.Unlock()

	if !exists {
		return C.jint(-1)
	}

	tool := Tool{
		Name:        name,
		Description: description,
		Function: func(input string) (string, error) {
			return fmt.Sprintf("Tool '%s' processed: %s", name, input), nil
		},
	}

	if err := agent.AddTool(tool); err != nil {
		return C.jint(-1)
	}

	return C.jint(0)
}

//export Java_ai_kastrax_graal_bridge_GoBridge_releaseAgent
func Java_ai_kastrax_graal_bridge_GoBridge_releaseAgent(env *C.JNIEnv, class C.jclass, agentID C.jlong) {
	agentsMutex.Lock()
	defer agentsMutex.Unlock()

	delete(agents, int64(agentID))
}

//export Java_ai_kastrax_graal_bridge_GoBridge_shutdown
func Java_ai_kastrax_graal_bridge_GoBridge_shutdown(env *C.JNIEnv, class C.jclass) {
	Shutdown()
}

// Helper functions for JNI

func jstringToString(env *C.JNIEnv, jstr C.jstring) string {
	cstr := C.(*C.char)(unsafe.Pointer(C.GetStringUTFChars(env, jstr, nil)))
	gostr := C.GoString(cstr)
	C.ReleaseStringUTFChars(env, jstr, cstr)
	return gostr
}

func stringToJstring(env *C.JNIEnv, gostr string) C.jstring {
	cstr := C.CString(gostr)
	defer C.free(unsafe.Pointer(cstr))
	return C.jstring(unsafe.Pointer(C.NewStringUTF(env, cstr)))
}

func main() {
	// This is required for building a shared library
}
