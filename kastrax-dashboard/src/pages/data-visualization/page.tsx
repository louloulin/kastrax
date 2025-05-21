import React, { useState, useEffect } from "react";
import SupabaseSidebar from "../../components/supabase-sidebar";
import TopNavigation from "../../components/top-navigation";
import { Button } from "../../components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "../../components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../../components/ui/tabs";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../../components/ui/select";
import { Input } from "../../components/ui/input";
import { Label } from "../../components/ui/label";
import { Menu, BarChart, PieChart, LineChart, RefreshCw, AlertTriangle, Database } from "lucide-react";
import { Alert, AlertDescription } from "../../components/ui/alert";
import {
  BarChart as RechartsBarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  LineChart as RechartsLineChart,
  Line,
  PieChart as RechartsPieChart,
  Pie,
  Cell,
} from "recharts";

// Mock API function to fetch data
const fetchChartData = async (chartType: string, databaseName: string, tableName: string, dataFields: string[]): Promise<any[]> => {
  return new Promise((resolve, reject) => {
    // Simulate API call
    setTimeout(() => {
      if (Math.random() > 0.2) {
        if (chartType === "bar" || chartType === "line") {
          resolve([
            { name: "Jan", value: 4000, category: "A" },
            { name: "Feb", value: 3000, category: "A" },
            { name: "Mar", value: 2000, category: "A" },
            { name: "Apr", value: 2780, category: "A" },
            { name: "May", value: 1890, category: "A" },
            { name: "Jun", value: 2390, category: "A" },
            { name: "Jul", value: 3490, category: "A" },
          ]);
        } else if (chartType === "pie") {
          resolve([
            { name: "Group A", value: 400 },
            { name: "Group B", value: 300 },
            { name: "Group C", value: 300 },
            { name: "Group D", value: 200 },
          ]);
        } else {
          resolve([]);
        }
      } else {
        reject(new Error("获取数据失败，请检查数据库连接"));
      }
    }, 1500);
  });
};

// COLORS for pie chart
const COLORS = ["#0088FE", "#00C49F", "#FFBB28", "#FF8042", "#8884D8", "#82CA9D"];

export default function DataVisualizationPage() {
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [activeTab, setActiveTab] = useState("bar");
  const [databaseName, setDatabaseName] = useState("");
  const [tableName, setTableName] = useState("");
  const [xField, setXField] = useState("");
  const [yField, setYField] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [chartData, setChartData] = useState<any[]>([]);

  const handleGenerateChart = async () => {
    if (!databaseName) {
      setError("请选择数据库");
      return;
    }

    if (!tableName) {
      setError("请输入表名");
      return;
    }

    if (!xField || !yField) {
      setError("请指定要显示的字段");
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const data = await fetchChartData(activeTab, databaseName, tableName, [xField, yField]);
      setChartData(data);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  };

  // Automatically fetch data when tab changes if other fields are filled
  useEffect(() => {
    if (databaseName && tableName && xField && yField) {
      handleGenerateChart();
    }
  }, [activeTab]);

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
              <h1 className="text-2xl font-medium">数据可视化</h1>
              <p className="text-muted-foreground">直观展示数据趋势和分布</p>
            </div>
          </div>
          
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="md:col-span-1">
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center">
                    <Database size={18} className="mr-2" />
                    数据源设置
                  </CardTitle>
                  <CardDescription>
                    选择要可视化的数据源和图表类型
                  </CardDescription>
                </CardHeader>
                
                <CardContent>
                  {error && (
                    <Alert className="mb-4" variant="destructive">
                      <AlertTriangle className="h-4 w-4" />
                      <AlertDescription>{error}</AlertDescription>
                    </Alert>
                  )}
                  
                  <div className="space-y-4">
                    <div className="space-y-2">
                      <Label htmlFor="chart-type">图表类型</Label>
                      <Tabs defaultValue="bar" value={activeTab} onValueChange={setActiveTab}>
                        <TabsList className="w-full">
                          <TabsTrigger value="bar" className="flex-1">
                            <BarChart size={16} className="mr-2" />
                            柱状图
                          </TabsTrigger>
                          <TabsTrigger value="line" className="flex-1">
                            <LineChart size={16} className="mr-2" />
                            折线图
                          </TabsTrigger>
                          <TabsTrigger value="pie" className="flex-1">
                            <PieChart size={16} className="mr-2" />
                            饼图
                          </TabsTrigger>
                        </TabsList>
                      </Tabs>
                    </div>
                    
                    <div className="space-y-2">
                      <Label htmlFor="vis-database-name">数据库</Label>
                      <Select
                        value={databaseName}
                        onValueChange={setDatabaseName}
                        disabled={loading}
                      >
                        <SelectTrigger id="vis-database-name" className="w-full">
                          <SelectValue placeholder="选择数据库" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="main">主数据库</SelectItem>
                          <SelectItem value="analytics">分析数据库</SelectItem>
                          <SelectItem value="archive">存档数据库</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                    
                    <div className="space-y-2">
                      <Label htmlFor="vis-table-name">表名</Label>
                      <Input
                        id="vis-table-name"
                        value={tableName}
                        onChange={(e) => setTableName(e.target.value)}
                        placeholder="输入表名"
                        disabled={loading}
                      />
                    </div>
                    
                    <div className="space-y-2">
                      <Label htmlFor="x-field">X轴字段 (分类)</Label>
                      <Input
                        id="x-field"
                        value={xField}
                        onChange={(e) => setXField(e.target.value)}
                        placeholder="例如: date, category"
                        disabled={loading}
                      />
                    </div>
                    
                    <div className="space-y-2">
                      <Label htmlFor="y-field">Y轴字段 (数值)</Label>
                      <Input
                        id="y-field"
                        value={yField}
                        onChange={(e) => setYField(e.target.value)}
                        placeholder="例如: count, amount"
                        disabled={loading}
                      />
                    </div>
                    
                    <div className="pt-4">
                      <Button
                        onClick={handleGenerateChart}
                        disabled={loading || !databaseName || !tableName || !xField || !yField}
                        className="w-full"
                      >
                        {loading ? (
                          <>
                            <RefreshCw className="mr-2 h-4 w-4 animate-spin" />
                            生成中...
                          </>
                        ) : (
                          <>
                            生成图表
                          </>
                        )}
                      </Button>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </div>
            
            <div className="md:col-span-2">
              <Card className="h-full">
                <CardHeader>
                  <CardTitle className="flex items-center">
                    {activeTab === "bar" && <BarChart size={18} className="mr-2" />}
                    {activeTab === "line" && <LineChart size={18} className="mr-2" />}
                    {activeTab === "pie" && <PieChart size={18} className="mr-2" />}
                    {activeTab === "bar" && "数据柱状图"}
                    {activeTab === "line" && "数据折线图"}
                    {activeTab === "pie" && "数据饼图"}
                  </CardTitle>
                  <CardDescription>
                    {databaseName && tableName ? `${databaseName}.${tableName} 的数据可视化` : "选择数据源生成图表"}
                  </CardDescription>
                </CardHeader>
                
                <CardContent className="h-[500px] flex items-center justify-center">
                  {loading ? (
                    <div className="flex flex-col items-center">
                      <RefreshCw size={40} className="animate-spin text-muted-foreground mb-4" />
                      <p className="text-muted-foreground">数据加载中...</p>
                    </div>
                  ) : chartData.length === 0 ? (
                    <div className="text-center text-muted-foreground">
                      <p>暂无数据</p>
                      <p className="text-sm">请选择数据源并生成图表</p>
                    </div>
                  ) : (
                    <ResponsiveContainer width="100%" height="100%">
                      {activeTab === "bar" && (
                        <RechartsBarChart
                          data={chartData}
                          margin={{ top: 20, right: 30, left: 20, bottom: 10 }}
                        >
                          <CartesianGrid strokeDasharray="3 3" />
                          <XAxis dataKey="name" />
                          <YAxis />
                          <Tooltip />
                          <Legend />
                          <Bar dataKey="value" name="数值" fill="#8884d8" />
                          {chartData[0]?.category && (
                            <Bar dataKey="category" name="类别" fill="#82ca9d" />
                          )}
                        </RechartsBarChart>
                      )}
                      
                      {activeTab === "line" && (
                        <RechartsLineChart
                          data={chartData}
                          margin={{ top: 20, right: 30, left: 20, bottom: 10 }}
                        >
                          <CartesianGrid strokeDasharray="3 3" />
                          <XAxis dataKey="name" />
                          <YAxis />
                          <Tooltip />
                          <Legend />
                          <Line
                            type="monotone"
                            dataKey="value"
                            name="数值"
                            stroke="#8884d8"
                            activeDot={{ r: 8 }}
                          />
                        </RechartsLineChart>
                      )}
                      
                      {activeTab === "pie" && (
                        <RechartsPieChart>
                          <Pie
                            data={chartData}
                            cx="50%"
                            cy="50%"
                            labelLine={true}
                            label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                            outerRadius={150}
                            fill="#8884d8"
                            dataKey="value"
                          >
                            {chartData.map((entry, index) => (
                              <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                            ))}
                          </Pie>
                          <Tooltip />
                          <Legend />
                        </RechartsPieChart>
                      )}
                    </ResponsiveContainer>
                  )}
                </CardContent>
              </Card>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
} 