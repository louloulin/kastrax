// Weather MCP Server in JavaScript
const readline = require('readline');

// Create readline interface
const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
});

// Server resources
const resources = [
  {
    id: "weather-info",
    name: "weather-info",
    description: "天气服务信息",
    content: "# 天气服务\n\n这是一个模拟的天气服务，提供全球主要城市的天气信息。"
  }
];

// Server tools
const tools = [
  {
    id: "getWeather",
    name: "getWeather",
    description: "获取城市天气信息",
    parameters: [
      {
        name: "city",
        type: "string",
        description: "城市名称",
        required: true
      }
    ]
  },
  {
    id: "getWeatherForecast",
    name: "getWeatherForecast",
    description: "获取城市天气预报",
    parameters: [
      {
        name: "city",
        type: "string",
        description: "城市名称",
        required: true
      },
      {
        name: "days",
        type: "integer",
        description: "预报天数",
        required: false
      }
    ]
  }
];

// Weather data (mock)
const weatherData = {
  "纽约": {
    temperature: 22,
    condition: "晴朗",
    humidity: 65,
    wind: "5 km/h"
  },
  "北京": {
    temperature: 28,
    condition: "多云",
    humidity: 70,
    wind: "10 km/h"
  },
  "伦敦": {
    temperature: 18,
    condition: "小雨",
    humidity: 80,
    wind: "15 km/h"
  },
  "东京": {
    temperature: 25,
    condition: "晴朗",
    humidity: 60,
    wind: "8 km/h"
  },
  "悉尼": {
    temperature: 20,
    condition: "多云",
    humidity: 75,
    wind: "12 km/h"
  }
};

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
        if (message.toolId === "getWeather") {
          const city = message.parameters.city;
          if (weatherData[city]) {
            sendResponse(message.id, `${city}的当前天气：${weatherData[city].temperature}°C，${weatherData[city].condition}，湿度${weatherData[city].humidity}%，风速${weatherData[city].wind}`);
          } else {
            sendResponse(message.id, `抱歉，没有找到${city}的天气信息。`);
          }
        } else if (message.toolId === "getWeatherForecast") {
          const city = message.parameters.city;
          const days = message.parameters.days || 3;
          
          if (weatherData[city]) {
            let forecast = `${city}未来${days}天天气预报：\n`;
            for (let i = 1; i <= days; i++) {
              // Generate random variations for the forecast
              const tempVariation = Math.floor(Math.random() * 5) - 2;
              const conditions = ["晴朗", "多云", "小雨", "大雨", "阴天"];
              const randomCondition = conditions[Math.floor(Math.random() * conditions.length)];
              
              forecast += `第${i}天：${weatherData[city].temperature + tempVariation}°C，${randomCondition}\n`;
            }
            sendResponse(message.id, forecast);
          } else {
            sendResponse(message.id, `抱歉，没有找到${city}的天气信息。`);
          }
        } else {
          sendError(message.id, "Tool not found");
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
console.error("Weather MCP Server started");
