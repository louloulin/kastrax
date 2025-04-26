// Simple MCP Server in JavaScript
const readline = require('readline');

// Create readline interface
const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
});

// Server resources
const resources = [
  {
    id: "greeting",
    name: "greeting",
    description: "问候消息",
    content: "Hello, MCP!"
  },
  {
    id: "documentation",
    name: "documentation",
    description: "KastraX MCP 文档",
    content: "# KastraX MCP\n\nKastraX MCP 是 Model Context Protocol (MCP) 在 KastraX 框架中的实现。\n它允许 KastraX 代理与支持 MCP 的应用程序和服务进行无缝集成。"
  }
];

// Server tools
const tools = [
  {
    id: "echo",
    name: "echo",
    description: "回显输入的文本",
    parameters: [
      {
        name: "text",
        type: "string",
        description: "要回显的文本",
        required: true
      }
    ]
  },
  {
    id: "reverse",
    name: "reverse",
    description: "反转输入的文本",
    parameters: [
      {
        name: "text",
        type: "string",
        description: "要反转的文本",
        required: true
      }
    ]
  }
];

// Server prompts
const prompts = [
  {
    id: "greeting",
    name: "greeting",
    description: "问候提示",
    content: "你好，{{name}}！",
    parameters: [
      {
        name: "name",
        type: "string",
        description: "用户名称",
        required: true
      }
    ]
  }
];

// Handle MCP messages
rl.on('line', (line) => {
  try {
    const message = JSON.parse(line);
    
    // Handle different message types
    switch (message.type) {
      case "resources":
        sendResponse(message.id, resources);
        break;
      
      case "resource":
        const resource = resources.find(r => r.id === message.resourceId);
        if (resource) {
          sendResponse(message.id, resource.content);
        } else {
          sendError(message.id, "Resource not found");
        }
        break;
      
      case "tools":
        sendResponse(message.id, tools);
        break;
      
      case "tool":
        if (message.toolId === "echo") {
          const text = message.parameters.text;
          sendResponse(message.id, `Echo: ${text}`);
        } else if (message.toolId === "reverse") {
          const text = message.parameters.text;
          sendResponse(message.id, `Reversed: ${text.split('').reverse().join('')}`);
        } else {
          sendError(message.id, "Tool not found");
        }
        break;
      
      case "prompts":
        sendResponse(message.id, prompts);
        break;
      
      case "prompt":
        const prompt = prompts.find(p => p.id === message.promptId);
        if (prompt) {
          sendResponse(message.id, prompt.content);
        } else {
          sendError(message.id, "Prompt not found");
        }
        break;
      
      default:
        sendError(message.id, "Unknown message type");
    }
  } catch (error) {
    console.error("Error processing message:", error);
  }
});

// Send response
function sendResponse(id, result) {
  const response = {
    id: id,
    result: result
  };
  console.log(JSON.stringify(response));
}

// Send error
function sendError(id, error) {
  const response = {
    id: id,
    error: error
  };
  console.log(JSON.stringify(response));
}

// Log server start
console.error("Simple MCP Server started");
