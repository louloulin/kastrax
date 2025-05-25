import React, { useState, useEffect } from "react";
import { v4 as uuidv4 } from "uuid";
import { ResizablePanel, ResizablePanelGroup } from "../ui/resizable";
import { format } from "sql-formatter";
import Toolbar from "./toolbar";
import EditorPanel from "./editor-panel";
import ResultPanel from "./result-panel";
import DatabaseBrowser from "./database-browser";
import SqlHistory, { SqlHistoryItem } from "./sql-history";

// 模拟执行 SQL 查询
const mockExecuteSql = (sql: string): Promise<any> => {
  return new Promise((resolve, reject) => {
    // 模拟网络延迟
    setTimeout(() => {
      try {
        // 处理 SELECT 查询
        if (sql.trim().toUpperCase().startsWith("SELECT")) {
          const results = [
            { id: 1, name: "张三", email: "zhangsan@example.com", role: "admin", created_at: "2023-01-15" },
            { id: 2, name: "李四", email: "lisi@example.com", role: "user", created_at: "2023-02-20" },
            { id: 3, name: "王五", email: "wangwu@example.com", role: "user", created_at: "2023-03-10" },
            { id: 4, name: "赵六", email: "zhaoliu@example.com", role: "editor", created_at: "2023-04-05" },
            { id: 5, name: "孙七", email: "sunqi@example.com", role: "user", created_at: "2023-05-12" },
          ];
          
          resolve({
            data: results,
            affectedRows: null,
            executionTime: 120 + Math.random() * 100,
          });
        } 
        // 处理 INSERT, UPDATE, DELETE 查询
        else if (
          sql.trim().toUpperCase().startsWith("INSERT") || 
          sql.trim().toUpperCase().startsWith("UPDATE") || 
          sql.trim().toUpperCase().startsWith("DELETE")
        ) {
          resolve({
            data: [],
            affectedRows: Math.floor(Math.random() * 10) + 1, // 随机影响的行数
            executionTime: 80 + Math.random() * 50,
          });
        }
        // 处理 CREATE, ALTER, DROP 查询
        else if (
          sql.trim().toUpperCase().startsWith("CREATE") || 
          sql.trim().toUpperCase().startsWith("ALTER") || 
          sql.trim().toUpperCase().startsWith("DROP")
        ) {
          resolve({
            data: [],
            affectedRows: null,
            executionTime: 150 + Math.random() * 100,
          });
        }
        // 其他查询或无效查询
        else {
          reject(new Error("无效或不支持的 SQL 查询"));
        }
      } catch (error) {
        reject(error);
      }
    }, 800); // 模拟执行时间
  });
};

export default function SqlEditor() {
  const [query, setQuery] = useState<string>("-- 在此输入您的 SQL 查询\nSELECT * FROM users LIMIT 10;");
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [results, setResults] = useState<any[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [executionTime, setExecutionTime] = useState<number>(0);
  const [affectedRows, setAffectedRows] = useState<number | undefined>(undefined);
  const [history, setHistory] = useState<SqlHistoryItem[]>([]);
  const [showHistory, setShowHistory] = useState<boolean>(false);
  const [activeConnection] = useState<string>("本地数据库");
  const [activeDatabase] = useState<string>("example_db");
  
  const executeQuery = async () => {
    setIsLoading(true);
    setError(null);
    
    try {
      const result = await mockExecuteSql(query);
      setResults(result.data);
      setAffectedRows(result.affectedRows);
      setExecutionTime(result.executionTime);
      
      // 添加到历史记录
      const historyItem: SqlHistoryItem = {
        id: uuidv4(),
        query,
        timestamp: new Date().toISOString(),
        executionTime: result.executionTime,
        status: "success",
        isFavorite: false,
      };
      
      setHistory(prev => [historyItem, ...prev]);
    } catch (err) {
      setError((err as Error).message);
      setResults(null);
      
      // 添加错误到历史记录
      const historyItem: SqlHistoryItem = {
        id: uuidv4(),
        query,
        timestamp: new Date().toISOString(),
        status: "error",
        isFavorite: false,
      };
      
      setHistory(prev => [historyItem, ...prev]);
    } finally {
      setIsLoading(false);
    }
  };
  
  const executeSelected = () => {
    // 这里我们会假设用户选中了查询进行执行
    // 在真实应用中，可以从 Monaco Editor 获取选中的文本
    alert("请先在编辑器中选择要执行的 SQL 语句");
  };
  
  const formatQuery = () => {
    try {
      const formattedQuery = format(query, {
        language: "mysql",
        tabWidth: 2,
        keywordCase: "upper",
        linesBetweenQueries: 2,
      });
      
      setQuery(formattedQuery);
    } catch (error) {
      console.error("Failed to format SQL:", error);
    }
  };
  
  const saveQuery = () => {
    alert("查询已保存");
  };
  
  const toggleHistory = () => {
    setShowHistory(!showHistory);
  };
  
  const handleToggleFavorite = (id: string) => {
    setHistory(prev => 
      prev.map(item => 
        item.id === id ? { ...item, isFavorite: !item.isFavorite } : item
      )
    );
  };
  
  const handleDeleteQuery = (id: string) => {
    setHistory(prev => prev.filter(item => item.id !== id));
  };
  
  const handleSelectQuery = (selectedQuery: string) => {
    setQuery(selectedQuery);
  };
  
  const handleNewQuery = () => {
    setQuery("-- 新建查询\n");
    setResults(null);
    setError(null);
  };
  
  return (
    <div className="flex flex-col h-full">
      <Toolbar 
        onExecute={executeQuery}
        onExecuteSelected={executeSelected}
        onFormat={formatQuery}
        onSave={saveQuery}
        onToggleHistory={toggleHistory}
        onNewQuery={handleNewQuery}
        isLoading={isLoading}
        activeConnection={activeConnection}
        activeDatabase={activeDatabase}
        showHistory={showHistory}
      />
      
      <ResizablePanelGroup direction="horizontal" className="flex-grow">
        <ResizablePanel defaultSize={20} minSize={15} maxSize={30} className="h-full">
          <DatabaseBrowser 
            onSelectTable={(tableName) => console.log("选择表:", tableName)}
            onSelectQuery={handleSelectQuery}
          />
        </ResizablePanel>
        
        <ResizablePanel defaultSize={55} className="h-full">
          <ResizablePanelGroup direction="vertical" className="h-full">
            <ResizablePanel defaultSize={50} className="h-full overflow-hidden">
              <EditorPanel 
                value={query}
                onChange={setQuery}
                onExecute={executeQuery}
              />
            </ResizablePanel>
            
            <ResizablePanel defaultSize={50} className="h-full overflow-hidden p-1">
              <ResultPanel 
                data={results}
                isLoading={isLoading}
                error={error}
                executionTime={executionTime}
                affectedRows={affectedRows}
              />
            </ResizablePanel>
          </ResizablePanelGroup>
        </ResizablePanel>
        
        {showHistory && (
          <ResizablePanel defaultSize={25} minSize={20} maxSize={30} className="h-full">
            <SqlHistory 
              items={history}
              onSelectQuery={handleSelectQuery}
              onToggleFavorite={handleToggleFavorite}
              onDeleteQuery={handleDeleteQuery}
            />
          </ResizablePanel>
        )}
      </ResizablePanelGroup>
    </div>
  );
} 