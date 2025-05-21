import React, { useState, useEffect } from "react";
import SupabaseSidebar from "../../components/supabase-sidebar";
import TopNavigation from "../../components/top-navigation";
import DataTable from "../../components/data-table";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import {
  Plus,
  Search,
  Trash2,
  Edit,
  RefreshCw,
  Menu,
  ExternalLink,
  PlayCircle,
  PauseCircle,
} from "lucide-react";
import { getDatabaseList, Database, testConnection } from "../../lib/api/database";
import { useNavigate } from "react-router-dom";

// Column definition for databases
const databaseColumns = [
  { id: "dbName", header: "数据库名称", accessorKey: "dbName" },
  { id: "dbType", header: "数据库类型", accessorKey: "dbType",
    cell: ({ row }: any) => {
      const dbTypeIcons: Record<string, React.ReactNode> = {
        mysql: <span className="text-blue-500 font-semibold">MySQL</span>,
        postgresql: <span className="text-blue-400 font-semibold">PostgreSQL</span>,
        sqlserver: <span className="text-red-500 font-semibold">SQL Server</span>,
        oracle: <span className="text-red-600 font-semibold">Oracle</span>,
        sqlite: <span className="text-gray-500 font-semibold">SQLite</span>,
        mongodb: <span className="text-green-600 font-semibold">MongoDB</span>,
      };
      
      return dbTypeIcons[row.original.dbType.toLowerCase()] || row.original.dbType;
    }
  },
  { id: "host", header: "主机地址", accessorKey: "host",
    cell: ({ row }: any) => (
      <div className="flex items-center">
        <span>{row.original.host}</span>
        {row.original.port && <span className="text-muted-foreground ml-1">:{row.original.port}</span>}
      </div>
    )
  },
  { id: "status", header: "状态", accessorKey: "status", 
    cell: ({ row }: any) => (
      <div className="flex items-center">
        <div className={`h-2 w-2 rounded-full mr-2 ${row.original.status === 1 ? 'bg-green-500' : 'bg-gray-500'}`}></div>
        <span>{row.original.status === 1 ? '已连接' : '未连接'}</span>
      </div>
    )
  },
  { id: "createTime", header: "创建时间", accessorKey: "createTime" },
];

export default function DatabasePage() {
  const [databases, setDatabases] = useState<Database[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [testingConnection, setTestingConnection] = useState<string | null>(null);
  const navigate = useNavigate();
  
  // Load databases on component mount
  useEffect(() => {
    fetchDatabases();
  }, []);
  
  // Function to fetch databases from API
  const fetchDatabases = async () => {
    setLoading(true);
    try {
      const response = await getDatabaseList();
      setDatabases(response.data.data || []);
    } catch (error) {
      console.error("Failed to fetch databases:", error);
    } finally {
      setLoading(false);
    }
  };
  
  // Function to test a database connection
  const handleTestConnection = async (dbId: string) => {
    setTestingConnection(dbId);
    try {
      const db = databases.find(db => db.dbId === dbId);
      if (db) {
        const response = await testConnection({
          dbId: db.dbId,
          dbType: db.dbType,
          host: db.host,
          port: db.port,
          username: db.username
        });
        
        if (response.data.success) {
          alert("连接测试成功！");
        } else {
          alert(`连接测试失败: ${response.data.message}`);
        }
      }
    } catch (error) {
      console.error("Failed to test connection:", error);
      alert("连接测试失败，请检查网络或服务器状态。");
    } finally {
      setTestingConnection(null);
    }
  };
  
  // Filter databases based on search query
  const filteredDatabases = databases.filter(db => 
    db.dbName.toLowerCase().includes(searchQuery.toLowerCase()) ||
    db.dbType.toLowerCase().includes(searchQuery.toLowerCase()) ||
    db.host.toLowerCase().includes(searchQuery.toLowerCase())
  );
  
  // Navigate to tables view for this database
  const viewTables = (dbId: string) => {
    navigate(`/tables?dbId=${dbId}`);
  };
  
  return (
    <div className="flex min-h-screen bg-background">
      <SupabaseSidebar collapsed={sidebarCollapsed} setCollapsed={setSidebarCollapsed} />
      
      <div className="flex-1 flex flex-col">
        <TopNavigation>
          <Button variant="ghost" size="sm" onClick={() => setSidebarCollapsed(!sidebarCollapsed)}>
            <Menu size={16} />
          </Button>
        </TopNavigation>
        
        <div className="flex-1 p-6 overflow-auto">
          <div className="flex justify-between items-center mb-6">
            <div>
              <h1 className="text-2xl font-medium">数据库管理</h1>
              <p className="text-muted-foreground">管理数据库连接和表</p>
            </div>
            
            <div className="flex gap-2">
              <Button variant="outline" size="sm" onClick={fetchDatabases}>
                <RefreshCw size={16} className="mr-2" />
                刷新
              </Button>
              <Button size="sm">
                <Plus size={16} className="mr-2" />
                添加数据库
              </Button>
            </div>
          </div>
          
          <div className="mb-6">
            <div className="relative">
              <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
              <Input
                type="search"
                placeholder="搜索数据库名称、类型或主机..."
                className="pl-8"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>
          </div>
          
          <DataTable 
            title="数据库连接"
            description="管理数据源连接"
            columns={databaseColumns}
            data={filteredDatabases}
            isLoading={loading}
            actions={(row: any) => (
              <div className="flex gap-2">
                <Button 
                  variant="ghost" 
                  size="sm" 
                  className="h-8 w-8 p-0" 
                  title="查看表"
                  onClick={() => viewTables(row.dbId)}
                >
                  <ExternalLink className="h-4 w-4" />
                </Button>
                <Button 
                  variant="ghost" 
                  size="sm" 
                  className="h-8 w-8 p-0" 
                  title="测试连接"
                  onClick={() => handleTestConnection(row.dbId)}
                  disabled={testingConnection === row.dbId}
                >
                  {testingConnection === row.dbId ? (
                    <RefreshCw className="h-4 w-4 animate-spin" />
                  ) : (
                    row.status === 1 ? <PauseCircle className="h-4 w-4" /> : <PlayCircle className="h-4 w-4" />
                  )}
                </Button>
                <Button variant="ghost" size="sm" className="h-8 w-8 p-0" title="编辑">
                  <Edit className="h-4 w-4" />
                </Button>
                <Button variant="ghost" size="sm" className="h-8 w-8 p-0" title="删除">
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>
            )}
          />
        </div>
      </div>
    </div>
  );
} 