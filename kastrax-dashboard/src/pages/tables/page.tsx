import React, { useState, useEffect } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import SupabaseSidebar from "../../components/supabase-sidebar";
import TopNavigation from "../../components/top-navigation";
import DataTable from "../../components/data-table";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../../components/ui/tabs";
import {
  Search,
  RefreshCw,
  Menu,
  ArrowLeft,
  Columns,
  Eye,
  Layers
} from "lucide-react";
import { getTableList, getTableColumns, getTablePreview, Table, Column } from "../../lib/api/database";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";

// Column definition for tables
const tableColumns = [
  { id: "tableName", header: "表名", accessorKey: "tableName" },
  { id: "schema", header: "模式", accessorKey: "schema",
    cell: ({ row }: any) => row.original.schema || "public"
  },
  { id: "rowCount", header: "行数", accessorKey: "rowCount",
    cell: ({ row }: any) => row.original.rowCount.toLocaleString()
  },
  { id: "size", header: "大小", accessorKey: "size",
    cell: ({ row }: any) => {
      const sizeInBytes = row.original.size;
      if (sizeInBytes < 1024) {
        return `${sizeInBytes} B`;
      } else if (sizeInBytes < 1024 * 1024) {
        return `${(sizeInBytes / 1024).toFixed(2)} KB`;
      } else if (sizeInBytes < 1024 * 1024 * 1024) {
        return `${(sizeInBytes / (1024 * 1024)).toFixed(2)} MB`;
      } else {
        return `${(sizeInBytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;
      }
    }
  },
  { id: "updateTime", header: "更新时间", accessorKey: "updateTime" },
];

// Column definition for table schema
const schemaColumns = [
  { id: "columnName", header: "列名", accessorKey: "columnName" },
  { id: "dataType", header: "数据类型", accessorKey: "dataType" },
  { id: "isPrimaryKey", header: "主键", accessorKey: "isPrimaryKey",
    cell: ({ row }: any) => row.original.isPrimaryKey ? "✓" : ""
  },
  { id: "isNullable", header: "可空", accessorKey: "isNullable",
    cell: ({ row }: any) => row.original.isNullable ? "✓" : "✗"
  },
  { id: "isUnique", header: "唯一", accessorKey: "isUnique",
    cell: ({ row }: any) => row.original.isUnique ? "✓" : ""
  },
  { id: "defaultValue", header: "默认值", accessorKey: "defaultValue",
    cell: ({ row }: any) => row.original.defaultValue || ""
  },
  { id: "description", header: "描述", accessorKey: "description",
    cell: ({ row }: any) => row.original.description || ""
  },
];

export default function TablesPage() {
  const [searchParams] = useSearchParams();
  const dbId = searchParams.get("dbId");
  const navigate = useNavigate();
  
  const [tables, setTables] = useState<Table[]>([]);
  const [columns, setColumns] = useState<Column[]>([]);
  const [previewData, setPreviewData] = useState<any[]>([]);
  const [previewColumns, setPreviewColumns] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [loadingSchema, setLoadingSchema] = useState(false);
  const [loadingPreview, setLoadingPreview] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [selectedTable, setSelectedTable] = useState<Table | null>(null);
  const [activeTab, setActiveTab] = useState("tables");
  
  // Load tables on component mount
  useEffect(() => {
    if (dbId) {
      fetchTables(dbId);
    }
  }, [dbId]);
  
  // Function to fetch tables from API
  const fetchTables = async (dbId: string) => {
    setLoading(true);
    try {
      const response = await getTableList(dbId);
      setTables(response.data.data || []);
    } catch (error) {
      console.error("Failed to fetch tables:", error);
    } finally {
      setLoading(false);
    }
  };
  
  // Function to fetch table columns (schema)
  const fetchTableColumns = async (tableId: string) => {
    setLoadingSchema(true);
    try {
      const response = await getTableColumns(tableId);
      setColumns(response.data.data || []);
    } catch (error) {
      console.error("Failed to fetch table columns:", error);
    } finally {
      setLoadingSchema(false);
    }
  };
  
  // Function to fetch table preview data
  const fetchTablePreview = async (tableId: string) => {
    setLoadingPreview(true);
    try {
      const response = await getTablePreview(tableId, 100);
      if (response.data.data && response.data.data.length > 0) {
        setPreviewData(response.data.data);
        
        // Create columns for the preview data table
        const sample = response.data.data[0];
        const previewCols = Object.keys(sample).map(key => ({
          id: key,
          header: key,
          accessorKey: key
        }));
        
        setPreviewColumns(previewCols);
      } else {
        setPreviewData([]);
        setPreviewColumns([]);
      }
    } catch (error) {
      console.error("Failed to fetch table preview:", error);
    } finally {
      setLoadingPreview(false);
    }
  };
  
  // Handle table selection
  const handleSelectTable = (table: Table) => {
    setSelectedTable(table);
    fetchTableColumns(table.tableId);
    fetchTablePreview(table.tableId);
    setActiveTab("structure");
  };
  
  // Filter tables based on search query
  const filteredTables = tables.filter(table => 
    table.tableName.toLowerCase().includes(searchQuery.toLowerCase()) ||
    (table.description && table.description.toLowerCase().includes(searchQuery.toLowerCase()))
  );
  
  // Go back to database view
  const goBackToDatabases = () => {
    navigate("/database");
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
            <div className="flex items-center">
              <Button 
                variant="ghost" 
                size="sm" 
                className="mr-2"
                onClick={goBackToDatabases}
              >
                <ArrowLeft size={16} className="mr-2" />
                返回数据库
              </Button>
              
              <div>
                <h1 className="text-2xl font-medium">表管理</h1>
                <p className="text-muted-foreground">浏览和管理数据表</p>
              </div>
            </div>
            
            <div className="flex gap-2">
              <Button variant="outline" size="sm" onClick={() => dbId && fetchTables(dbId)}>
                <RefreshCw size={16} className="mr-2" />
                刷新
              </Button>
            </div>
          </div>
          
          {selectedTable ? (
            <div>
              <div className="flex items-center mb-6">
                <Button variant="ghost" size="sm" onClick={() => setSelectedTable(null)}>
                  <ArrowLeft size={16} className="mr-2" />
                  返回表列表
                </Button>
                <h2 className="text-xl font-medium ml-2">{selectedTable.tableName}</h2>
              </div>
              
              <Card className="mb-6">
                <CardHeader className="pb-3">
                  <CardTitle className="text-md font-medium">表信息</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                    <div>
                      <p className="text-sm text-muted-foreground">数据库</p>
                      <p className="font-medium">{selectedTable.dbName}</p>
                    </div>
                    <div>
                      <p className="text-sm text-muted-foreground">模式</p>
                      <p className="font-medium">{selectedTable.schema || "public"}</p>
                    </div>
                    <div>
                      <p className="text-sm text-muted-foreground">行数</p>
                      <p className="font-medium">{selectedTable.rowCount.toLocaleString()}</p>
                    </div>
                    <div>
                      <p className="text-sm text-muted-foreground">大小</p>
                      <p className="font-medium">
                        {selectedTable.size < 1024 * 1024
                          ? `${(selectedTable.size / 1024).toFixed(2)} KB`
                          : `${(selectedTable.size / (1024 * 1024)).toFixed(2)} MB`
                        }
                      </p>
                    </div>
                  </div>
                </CardContent>
              </Card>
              
              <Tabs value={activeTab} onValueChange={setActiveTab}>
                <TabsList className="mb-4">
                  <TabsTrigger value="structure">
                    <Columns className="h-4 w-4 mr-2" />
                    表结构
                  </TabsTrigger>
                  <TabsTrigger value="preview">
                    <Eye className="h-4 w-4 mr-2" />
                    数据预览
                  </TabsTrigger>
                </TabsList>
                
                <TabsContent value="structure" className="mt-0">
                  <DataTable
                    title="列信息"
                    description="表的列和数据类型"
                    columns={schemaColumns}
                    data={columns}
                    isLoading={loadingSchema}
                  />
                </TabsContent>
                
                <TabsContent value="preview" className="mt-0">
                  <DataTable
                    title="数据预览"
                    description="表中的前100行数据"
                    columns={previewColumns}
                    data={previewData}
                    isLoading={loadingPreview}
                  />
                </TabsContent>
              </Tabs>
            </div>
          ) : (
            <>
              <div className="mb-6">
                <div className="relative">
                  <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
                  <Input
                    type="search"
                    placeholder="搜索表名..."
                    className="pl-8"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                  />
                </div>
              </div>
              
              <DataTable 
                title="数据表"
                description="浏览和管理数据表"
                columns={tableColumns}
                data={filteredTables}
                isLoading={loading}
                actions={(row: any) => (
                  <div className="flex gap-2">
                    <Button 
                      variant="ghost" 
                      size="sm" 
                      className="h-8 w-8 p-0" 
                      title="查看表结构和数据"
                      onClick={() => handleSelectTable(row)}
                    >
                      <Layers className="h-4 w-4" />
                    </Button>
                  </div>
                )}
              />
            </>
          )}
        </div>
      </div>
    </div>
  );
} 