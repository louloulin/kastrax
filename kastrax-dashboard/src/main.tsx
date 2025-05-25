import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import App from "./App";
import "./styles/globals.css";

// 导入MSW模拟服务器设置
async function initializeMockServiceWorker() {
  if (
    import.meta.env.DEV &&
    import.meta.env.VITE_ENABLE_MSW === "true"
  ) {
    try {
      console.log("[MSW] 初始化模拟API服务");
      const { setupMockServiceWorker } = await import("./mocks/browser");
      await setupMockServiceWorker();
      console.log("[MSW] 模拟API服务初始化成功");
    } catch (error) {
      console.error("[MSW] 初始化失败:", error);
      console.log("[MSW] 将使用真实后端 API");
    }
  }
}

// 初始化应用
async function initializeApp() {
  // 先初始化MSW（如果启用）
  await initializeMockServiceWorker();

  // 然后渲染应用
  ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
    <React.StrictMode>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </React.StrictMode>
  );
}

initializeApp();
