import React from "react";
import { Clock, Star, Trash } from "lucide-react";

export interface SqlHistoryItem {
  id: string;
  query: string;
  timestamp: string;
  executionTime?: number;
  status: "success" | "error";
  isFavorite?: boolean;
}

interface SqlHistoryProps {
  items: SqlHistoryItem[];
  onSelectQuery: (query: string) => void;
  onToggleFavorite: (id: string) => void;
  onDeleteQuery: (id: string) => void;
}

export default function SqlHistory({
  items,
  onSelectQuery,
  onToggleFavorite,
  onDeleteQuery,
}: SqlHistoryProps) {
  return (
    <div className="bg-card border border-border rounded-md p-4 h-full overflow-hidden flex flex-col">
      <div className="flex items-center justify-between mb-4">
        <h3 className="font-medium flex items-center">
          <Clock size={16} className="mr-2 text-muted-foreground" />
          SQL 历史记录
        </h3>
      </div>
      
      {items.length === 0 ? (
        <div className="flex-grow flex items-center justify-center text-sm text-muted-foreground">
          暂无查询历史记录
        </div>
      ) : (
        <div className="flex-grow overflow-auto">
          {items.map((item) => (
            <div 
              key={item.id}
              className="mb-3 last:mb-0 border border-border rounded-md overflow-hidden group"
            >
              <div 
                className={`px-3 py-2 text-xs font-medium flex items-center justify-between ${
                  item.status === "success" ? "bg-primary/5" : "bg-destructive/5"
                }`}
              >
                <div className="flex items-center">
                  <div 
                    className={`w-2 h-2 rounded-full mr-2 ${
                      item.status === "success" ? "bg-primary" : "bg-destructive"
                    }`}
                  />
                  <span>{new Date(item.timestamp).toLocaleString()}</span>
                </div>
                
                <div className="flex items-center space-x-1">
                  {item.executionTime !== undefined && (
                    <span className="text-muted-foreground">
                      {item.executionTime.toFixed(2)} ms
                    </span>
                  )}
                  
                  <button
                    onClick={() => onToggleFavorite(item.id)}
                    className={`p-1 rounded-sm opacity-70 group-hover:opacity-100 ${
                      item.isFavorite 
                        ? "text-yellow-400 hover:text-yellow-500" 
                        : "text-muted-foreground hover:text-yellow-400"
                    }`}
                    title={item.isFavorite ? "取消收藏" : "收藏查询"}
                  >
                    <Star size={14} fill={item.isFavorite ? "currentColor" : "none"} />
                  </button>
                  
                  <button
                    onClick={() => onDeleteQuery(item.id)}
                    className="p-1 rounded-sm text-muted-foreground hover:text-destructive opacity-70 group-hover:opacity-100"
                    title="删除此查询"
                  >
                    <Trash size={14} />
                  </button>
                </div>
              </div>
              
              <div 
                className="p-3 bg-secondary/20 cursor-pointer hover:bg-secondary/40 transition-colors"
                onClick={() => onSelectQuery(item.query)}
              >
                <pre className="text-xs font-mono overflow-hidden text-ellipsis line-clamp-2">
                  {item.query}
                </pre>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
} 