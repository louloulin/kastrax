import React, { useState } from "react";
import SupabaseSidebar from "../components/supabase-sidebar";
import TopNavigation from "../components/top-navigation";
import SqlEditor from "../components/sql-editor";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../components/ui/tabs";
import { Button } from "../components/ui/button";
import { 
  Save, 
  Download, 
  Star, 
  Clock, 
  FileCode, 
  Maximize2, 
  Minimize2, 
  Menu, 
  PlusCircle 
} from "lucide-react";

export default function SqlPage() {
  const [activeSavedQuery, setActiveSavedQuery] = useState<string | null>(null);
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [isFullScreen, setIsFullScreen] = useState(false);
  
  // 模拟保存的查询
  const savedQueries = [
    { id: "1", name: "获取活跃用户", query: "SELECT * FROM users WHERE last_active > now() - interval '30 days'" },
    { id: "2", name: "最近订单", query: "SELECT * FROM orders ORDER BY created_at DESC LIMIT 100" },
    { id: "3", name: "热门产品", query: "SELECT p.name, COUNT(o.id) as order_count FROM products p JOIN order_items oi ON p.id = oi.product_id JOIN orders o ON oi.order_id = o.id GROUP BY p.id, p.name ORDER BY order_count DESC LIMIT 10" },
  ];
  
  const toggleFullScreen = () => {
    setIsFullScreen(!isFullScreen);
  };
  
  if (isFullScreen) {
    return (
      <div className="fixed inset-0 bg-background z-50 flex flex-col">
        <div className="h-14 border-b border-border flex items-center px-4 justify-between shadow-sm">
          <div className="flex items-center">
            <Button 
              variant="ghost" 
              size="sm" 
              onClick={toggleFullScreen}
              className="text-muted-foreground hover:text-foreground"
            >
              <Minimize2 size={16} className="mr-2" />
              退出全屏
            </Button>
          </div>
          
          <div className="flex gap-2">
            <Button variant="outline" size="sm">
              <Download size={16} className="mr-2" />
              导出结果
            </Button>
            <Button variant="outline" size="sm">
              <Save size={16} className="mr-2" />
              保存查询
            </Button>
            <Button size="sm">
              <PlusCircle size={16} className="mr-2" />
              新建查询
            </Button>
          </div>
        </div>
        
        <div className="flex-1 overflow-hidden">
          <SqlEditor />
        </div>
      </div>
    );
  }
  
  return (
    <div className="flex min-h-screen bg-background">
      <SupabaseSidebar collapsed={sidebarCollapsed} setCollapsed={setSidebarCollapsed} />
      
      <div className="flex-1 flex flex-col">
        <TopNavigation>
          <Button variant="ghost" size="sm" onClick={() => setSidebarCollapsed(!sidebarCollapsed)}>
            <Menu size={16} />
          </Button>
        </TopNavigation>
        
        <div className="flex-1 p-6 overflow-hidden flex flex-col">
          <div className="flex justify-between items-center mb-6">
            <div>
              <h1 className="text-2xl font-medium">SQL 编辑器</h1>
              <p className="text-muted-foreground">使用 SQL 查询和管理您的数据库。</p>
            </div>
            
            <div className="flex gap-2">
              <Button 
                variant="ghost" 
                size="sm" 
                onClick={toggleFullScreen}
                className="text-muted-foreground hover:text-foreground"
              >
                <Maximize2 size={16} className="mr-2" />
                全屏模式
              </Button>
              <Button variant="outline" size="sm">
                <Download size={16} className="mr-2" />
                导出结果
              </Button>
              <Button variant="outline" size="sm">
                <Save size={16} className="mr-2" />
                保存查询
              </Button>
              <Button size="sm">
                <PlusCircle size={16} className="mr-2" />
                新建查询
              </Button>
            </div>
          </div>
          
          <div className="grid grid-cols-5 gap-6 flex-1 overflow-hidden">
            <div className={`${sidebarCollapsed ? 'col-span-1' : 'col-span-1'} bg-card border border-border rounded-lg p-4 overflow-hidden flex flex-col`}>
              <Tabs defaultValue="saved">
                <TabsList className="mb-4">
                  <TabsTrigger value="saved" className="flex items-center">
                    <Star size={14} className="mr-2" />
                    {!sidebarCollapsed && "保存的查询"}
                  </TabsTrigger>
                  <TabsTrigger value="history" className="flex items-center">
                    <Clock size={14} className="mr-2" />
                    {!sidebarCollapsed && "历史记录"}
                  </TabsTrigger>
                </TabsList>
                
                <TabsContent value="saved" className="overflow-auto flex-1">
                  <div className="space-y-2">
                    {savedQueries.map(q => (
                      <div
                        key={q.id}
                        className={`p-3 text-sm rounded-md cursor-pointer ${
                          activeSavedQuery === q.id
                            ? "bg-primary/10 border border-primary/30"
                            : "hover:bg-secondary"
                        }`}
                        onClick={() => setActiveSavedQuery(q.id)}
                      >
                        <div className="font-medium mb-1 flex items-center">
                          <FileCode size={14} className="mr-2 text-primary" />
                          {!sidebarCollapsed && q.name}
                        </div>
                        {!sidebarCollapsed && (
                          <div className="text-xs text-muted-foreground font-mono truncate">
                            {q.query}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                </TabsContent>
                
                <TabsContent value="history" className="overflow-auto flex-1">
                  {!sidebarCollapsed && (
                    <div className="text-sm text-muted-foreground">
                      查询历史记录会在您执行查询时自动保存
                    </div>
                  )}
                </TabsContent>
              </Tabs>
            </div>
            
            <div className={`${sidebarCollapsed ? 'col-span-4' : 'col-span-4'} rounded-lg border border-border bg-card flex-1 overflow-hidden`}>
              <SqlEditor />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
} 