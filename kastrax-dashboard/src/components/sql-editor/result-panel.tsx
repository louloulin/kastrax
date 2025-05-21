import React, { useState } from "react";
import { Download, Copy, Table, List, Grid, X } from "lucide-react";

interface ResultPanelProps {
  data: any[] | null;
  isLoading: boolean;
  error: string | null;
  executionTime?: number;
  affectedRows?: number;
}

export default function ResultPanel({ 
  data, 
  isLoading, 
  error, 
  executionTime = 0,
  affectedRows
}: ResultPanelProps) {
  const [viewMode, setViewMode] = useState<"table" | "json" | "grid">("table");
  
  const handleExportCSV = () => {
    if (!data || data.length === 0) return;
    
    const headers = Object.keys(data[0]);
    const csvContent = [
      headers.join(","),
      ...data.map(row => headers.map(header => JSON.stringify(row[header] || "")).join(","))
    ].join("\n");
    
    const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.setAttribute("href", url);
    link.setAttribute("download", `query_result_${new Date().toISOString()}.csv`);
    link.style.visibility = "hidden";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };
  
  const handleCopyToClipboard = () => {
    if (!data) return;
    
    const jsonString = JSON.stringify(data, null, 2);
    navigator.clipboard.writeText(jsonString)
      .then(() => {
        alert("已复制查询结果到剪贴板");
      })
      .catch(err => {
        console.error("无法复制到剪贴板:", err);
      });
  };
  
  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-40 bg-card border border-border rounded-md">
        <div className="flex flex-col items-center">
          <div className="w-6 h-6 border-2 border-primary border-t-transparent rounded-full animate-spin mb-2"></div>
          <p className="text-sm text-muted-foreground">执行查询中...</p>
        </div>
      </div>
    );
  }
  
  if (error) {
    return (
      <div className="bg-destructive/10 border border-destructive/30 text-destructive rounded-md p-4 h-40 overflow-auto">
        <div className="flex items-start">
          <X className="mr-2 h-5 w-5 flex-shrink-0" />
          <div>
            <h4 className="font-medium">查询错误</h4>
            <p className="text-sm mt-1 font-mono overflow-auto">{error}</p>
          </div>
        </div>
      </div>
    );
  }
  
  if (!data || data.length === 0) {
    // 这里处理没有结果但执行成功的情况（例如INSERT, UPDATE等）
    if (affectedRows !== undefined) {
      return (
        <div className="bg-card border border-border rounded-md p-4 text-sm">
          <p className="text-primary font-medium">
            查询执行成功 ({executionTime.toFixed(2)} ms)
          </p>
          <p className="mt-1">
            影响了 {affectedRows} 行数据
          </p>
        </div>
      );
    }
    
    return (
      <div className="flex items-center justify-center h-40 bg-card border border-border rounded-md">
        <p className="text-sm text-muted-foreground">没有返回数据</p>
      </div>
    );
  }
  
  const headers = Object.keys(data[0]);
  
  return (
    <div className="bg-card border border-border rounded-md">
      <div className="flex items-center justify-between border-b border-border p-2">
        <div className="flex items-center space-x-1">
          <button 
            onClick={() => setViewMode("table")}
            className={`p-1 rounded-sm ${viewMode === "table" ? "bg-primary/10 text-primary" : "text-muted-foreground hover:text-foreground hover:bg-secondary"}`}
          >
            <Table size={16} />
          </button>
          <button 
            onClick={() => setViewMode("json")}
            className={`p-1 rounded-sm ${viewMode === "json" ? "bg-primary/10 text-primary" : "text-muted-foreground hover:text-foreground hover:bg-secondary"}`}
          >
            <List size={16} />
          </button>
          <button 
            onClick={() => setViewMode("grid")}
            className={`p-1 rounded-sm ${viewMode === "grid" ? "bg-primary/10 text-primary" : "text-muted-foreground hover:text-foreground hover:bg-secondary"}`}
          >
            <Grid size={16} />
          </button>
        </div>
        
        <div className="flex items-center text-xs text-muted-foreground">
          <span>{data.length} 行结果</span>
          <span className="mx-2">|</span>
          <span>{executionTime.toFixed(2)} ms</span>
        </div>
        
        <div className="flex items-center space-x-1">
          <button 
            onClick={handleCopyToClipboard}
            className="p-1 rounded-sm text-muted-foreground hover:text-foreground hover:bg-secondary"
            title="复制到剪贴板"
          >
            <Copy size={16} />
          </button>
          <button 
            onClick={handleExportCSV}
            className="p-1 rounded-sm text-muted-foreground hover:text-foreground hover:bg-secondary"
            title="导出为CSV"
          >
            <Download size={16} />
          </button>
        </div>
      </div>
      
      <div className="overflow-auto max-h-80">
        {viewMode === "table" && (
          <table className="w-full border-collapse">
            <thead>
              <tr className="bg-secondary/50">
                {headers.map((header, i) => (
                  <th key={i} className="px-3 py-2 text-left text-xs font-medium border-b border-border">
                    {header}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {data.map((row, rowIndex) => (
                <tr 
                  key={rowIndex}
                  className="border-b border-border hover:bg-secondary/20"
                >
                  {headers.map((header, colIndex) => (
                    <td key={colIndex} className="px-3 py-2 text-sm">
                      {row[header] !== null && row[header] !== undefined 
                        ? String(row[header]) 
                        : <span className="text-muted-foreground italic">NULL</span>}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        )}
        
        {viewMode === "json" && (
          <pre className="p-4 text-xs overflow-auto font-mono">
            {JSON.stringify(data, null, 2)}
          </pre>
        )}
        
        {viewMode === "grid" && (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 p-4">
            {data.map((row, rowIndex) => (
              <div key={rowIndex} className="border border-border rounded-md p-3 bg-secondary/20">
                {headers.map((header, headIndex) => (
                  <div key={headIndex} className="mb-1 last:mb-0">
                    <span className="text-xs font-medium text-muted-foreground">{header}: </span>
                    <span className="text-sm">
                      {row[header] !== null && row[header] !== undefined 
                        ? String(row[header]) 
                        : <span className="text-muted-foreground italic">NULL</span>}
                    </span>
                  </div>
                ))}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
} 