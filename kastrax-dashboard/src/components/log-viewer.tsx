import React, { useState, useEffect } from 'react';
import { LogEntry, LogLevel, LogCategory, getLogHistory } from '../lib/utils/logger';
import { Button } from './ui/button';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from './ui/tabs';
import { ScrollArea } from './ui/scroll-area';
import { Badge } from './ui/badge';
import { X, RefreshCw, Download } from 'lucide-react';

interface LogViewerProps {
  onClose?: () => void;
  maxHeight?: string;
  showHeader?: boolean;
  autoRefresh?: boolean;
  refreshInterval?: number;
}

export default function LogViewer({
  onClose,
  maxHeight = '400px',
  showHeader = true,
  autoRefresh = true,
  refreshInterval = 5000
}: LogViewerProps) {
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [filter, setFilter] = useState<LogLevel | 'all'>('all');
  const [categoryFilter, setCategoryFilter] = useState<LogCategory | 'all'>('all');

  // Function to refresh logs
  const refreshLogs = () => {
    const history = getLogHistory();
    setLogs(history);
  };

  // Initial load and auto-refresh
  useEffect(() => {
    refreshLogs();
    
    if (autoRefresh) {
      const interval = setInterval(refreshLogs, refreshInterval);
      return () => clearInterval(interval);
    }
  }, [autoRefresh, refreshInterval]);

  // Filter logs based on selected level and category
  const filteredLogs = logs.filter(log => {
    const levelMatch = filter === 'all' || log.level === filter;
    const categoryMatch = categoryFilter === 'all' || log.category === categoryFilter;
    return levelMatch && categoryMatch;
  });

  // Download logs as JSON
  const downloadLogs = () => {
    const data = JSON.stringify(logs, null, 2);
    const blob = new Blob([data], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `logs-${new Date().toISOString()}.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  // Get badge color based on log level
  const getLevelBadgeColor = (level: LogLevel) => {
    switch (level) {
      case LogLevel.DEBUG:
        return 'bg-gray-500';
      case LogLevel.INFO:
        return 'bg-blue-500';
      case LogLevel.WARN:
        return 'bg-yellow-500';
      case LogLevel.ERROR:
        return 'bg-red-500';
      default:
        return 'bg-gray-500';
    }
  };

  // Get badge color based on log category
  const getCategoryBadgeColor = (category: LogCategory) => {
    switch (category) {
      case LogCategory.API:
        return 'bg-purple-500';
      case LogCategory.AUTH:
        return 'bg-green-500';
      case LogCategory.UI:
        return 'bg-blue-500';
      case LogCategory.STORE:
        return 'bg-orange-500';
      case LogCategory.SYSTEM:
        return 'bg-gray-500';
      default:
        return 'bg-gray-500';
    }
  };

  return (
    <Card className="w-full">
      {showHeader && (
        <CardHeader className="flex flex-row items-center justify-between pb-2">
          <CardTitle className="text-xl">日志查看器</CardTitle>
          <div className="flex space-x-2">
            <Button variant="outline" size="sm" onClick={refreshLogs}>
              <RefreshCw className="h-4 w-4 mr-1" />
              刷新
            </Button>
            <Button variant="outline" size="sm" onClick={downloadLogs}>
              <Download className="h-4 w-4 mr-1" />
              下载
            </Button>
            {onClose && (
              <Button variant="ghost" size="sm" onClick={onClose}>
                <X className="h-4 w-4" />
              </Button>
            )}
          </div>
        </CardHeader>
      )}
      <CardContent>
        <Tabs defaultValue="all">
          <div className="flex justify-between items-center mb-4">
            <TabsList>
              <TabsTrigger value="all" onClick={() => setFilter('all')}>
                全部
              </TabsTrigger>
              <TabsTrigger value="debug" onClick={() => setFilter(LogLevel.DEBUG)}>
                调试
              </TabsTrigger>
              <TabsTrigger value="info" onClick={() => setFilter(LogLevel.INFO)}>
                信息
              </TabsTrigger>
              <TabsTrigger value="warn" onClick={() => setFilter(LogLevel.WARN)}>
                警告
              </TabsTrigger>
              <TabsTrigger value="error" onClick={() => setFilter(LogLevel.ERROR)}>
                错误
              </TabsTrigger>
            </TabsList>
            
            <div className="flex space-x-2">
              <select 
                className="px-2 py-1 rounded border text-sm"
                value={categoryFilter}
                onChange={(e) => setCategoryFilter(e.target.value as LogCategory | 'all')}
              >
                <option value="all">全部分类</option>
                <option value={LogCategory.API}>API</option>
                <option value={LogCategory.AUTH}>认证</option>
                <option value={LogCategory.UI}>界面</option>
                <option value={LogCategory.STORE}>存储</option>
                <option value={LogCategory.SYSTEM}>系统</option>
              </select>
              <span className="text-sm text-muted-foreground">
                {filteredLogs.length} 条日志
              </span>
            </div>
          </div>
          
          <TabsContent value="all" className="mt-0">
            <ScrollArea className={`border rounded-md p-4 ${maxHeight ? `max-h-[${maxHeight}]` : ''}`} style={{ maxHeight }}>
              {filteredLogs.length === 0 ? (
                <div className="text-center py-8 text-muted-foreground">
                  没有日志记录
                </div>
              ) : (
                <div className="space-y-2">
                  {filteredLogs.map((log, index) => (
                    <div key={index} className="border-b pb-2 last:border-0">
                      <div className="flex items-center space-x-2 mb-1">
                        <Badge className={getLevelBadgeColor(log.level)}>
                          {log.level.toUpperCase()}
                        </Badge>
                        <Badge className={getCategoryBadgeColor(log.category)}>
                          {log.category}
                        </Badge>
                        <span className="text-xs text-muted-foreground">
                          {new Date(log.timestamp).toLocaleString()}
                        </span>
                      </div>
                      <div className="text-sm">{log.message}</div>
                      {log.data && (
                        <pre className="text-xs bg-muted p-2 rounded mt-1 overflow-x-auto">
                          {typeof log.data === 'object' 
                            ? JSON.stringify(log.data, null, 2) 
                            : String(log.data)}
                        </pre>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </ScrollArea>
          </TabsContent>
          
          <TabsContent value="debug" className="mt-0">
            {/* Same content as "all" but filtered by debug level */}
          </TabsContent>
          
          <TabsContent value="info" className="mt-0">
            {/* Same content as "all" but filtered by info level */}
          </TabsContent>
          
          <TabsContent value="warn" className="mt-0">
            {/* Same content as "all" but filtered by warn level */}
          </TabsContent>
          
          <TabsContent value="error" className="mt-0">
            {/* Same content as "all" but filtered by error level */}
          </TabsContent>
        </Tabs>
      </CardContent>
    </Card>
  );
}
