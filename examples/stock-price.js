// Stock Price MCP Server in JavaScript
const readline = require('readline');

// Create readline interface
const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
});

// Server resources
const resources = [
  {
    id: "stock-info",
    name: "stock-info",
    description: "股票服务信息",
    content: "# 股票价格服务\n\n这是一个模拟的股票价格服务，提供主要公司的股票价格信息。"
  }
];

// Server tools
const tools = [
  {
    id: "getStockPrice",
    name: "getStockPrice",
    description: "获取公司股票价格",
    parameters: [
      {
        name: "company",
        type: "string",
        description: "公司名称",
        required: true
      }
    ]
  },
  {
    id: "getStockHistory",
    name: "getStockHistory",
    description: "获取公司股票历史价格",
    parameters: [
      {
        name: "company",
        type: "string",
        description: "公司名称",
        required: true
      },
      {
        name: "days",
        type: "integer",
        description: "历史天数",
        required: false
      }
    ]
  }
];

// Stock data (mock)
const stockData = {
  "苹果": {
    symbol: "AAPL",
    price: 175.42,
    change: 2.35,
    volume: "45.2M"
  },
  "谷歌": {
    symbol: "GOOGL",
    price: 142.65,
    change: -0.87,
    volume: "22.1M"
  },
  "微软": {
    symbol: "MSFT",
    price: 338.11,
    change: 1.23,
    volume: "18.7M"
  },
  "亚马逊": {
    symbol: "AMZN",
    price: 178.75,
    change: 3.45,
    volume: "32.5M"
  },
  "特斯拉": {
    symbol: "TSLA",
    price: 245.30,
    change: -5.67,
    volume: "55.8M"
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
        if (message.toolId === "getStockPrice") {
          const company = message.parameters.company;
          if (stockData[company]) {
            const stock = stockData[company];
            const changeDirection = stock.change >= 0 ? "上涨" : "下跌";
            const changeAbs = Math.abs(stock.change);
            sendResponse(message.id, `${company}(${stock.symbol})的当前股价为 $${stock.price}，${changeDirection} $${changeAbs.toFixed(2)}，成交量 ${stock.volume}`);
          } else {
            sendResponse(message.id, `抱歉，没有找到${company}的股票信息。`);
          }
        } else if (message.toolId === "getStockHistory") {
          const company = message.parameters.company;
          const days = message.parameters.days || 5;
          
          if (stockData[company]) {
            let history = `${company}(${stockData[company].symbol})过去${days}天的股价历史：\n`;
            const currentPrice = stockData[company].price;
            
            for (let i = days; i > 0; i--) {
              // Generate random variations for the history
              const priceVariation = (Math.random() * 10 - 5).toFixed(2);
              const historicalPrice = (currentPrice - priceVariation).toFixed(2);
              
              history += `${i}天前：$${historicalPrice}\n`;
            }
            
            history += `今天：$${currentPrice.toFixed(2)}`;
            sendResponse(message.id, history);
          } else {
            sendResponse(message.id, `抱歉，没有找到${company}的股票信息。`);
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
console.error("Stock Price MCP Server started");
