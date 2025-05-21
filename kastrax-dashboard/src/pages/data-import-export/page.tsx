import React, { useState } from "react";
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
import { Progress } from "../../components/ui/progress";
import { Menu, Upload, Download, Database, FileText, RefreshCw, AlertTriangle } from "lucide-react";
import { Alert, AlertDescription } from "../../components/ui/alert";

// Mock API functions for data import/export
const importData = async (file: File, format: string, databaseName: string, tableName: string): Promise<void> => {
  return new Promise((resolve, reject) => {
    // Simulate API call
    setTimeout(() => {
      if (Math.random() > 0.2) {
        resolve();
      } else {
        reject(new Error("导入失败，请检查文件格式或连接状态"));
      }
    }, 2000);
  });
};

const exportData = async (format: string, databaseName: string, tableName: string): Promise<string> => {
  return new Promise((resolve, reject) => {
    // Simulate API call
    setTimeout(() => {
      if (Math.random() > 0.2) {
        resolve("data:text/csv;charset=utf-8,id,name,value\n1,Item 1,100\n2,Item 2,200");
      } else {
        reject(new Error("导出失败，请检查数据库连接状态"));
      }
    }, 2000);
  });
};

export default function DataImportExportPage() {
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [activeTab, setActiveTab] = useState("import");
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [importFormat, setImportFormat] = useState("csv");
  const [exportFormat, setExportFormat] = useState("csv");
  const [databaseName, setDatabaseName] = useState("");
  const [tableName, setTableName] = useState("");
  const [loading, setLoading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      setSelectedFile(e.target.files[0]);
      setError(null);
    }
  };

  const handleImport = async () => {
    if (!selectedFile) {
      setError("请选择要导入的文件");
      return;
    }

    if (!databaseName) {
      setError("请选择目标数据库");
      return;
    }

    if (!tableName) {
      setError("请输入目标表名");
      return;
    }

    setLoading(true);
    setProgress(0);
    setError(null);
    setSuccess(null);

    // Simulate progress
    const interval = setInterval(() => {
      setProgress(prev => {
        if (prev >= 90) {
          clearInterval(interval);
          return prev;
        }
        return prev + 10;
      });
    }, 300);

    try {
      await importData(selectedFile, importFormat, databaseName, tableName);
      clearInterval(interval);
      setProgress(100);
      setSuccess(`成功导入数据到 ${databaseName}.${tableName}`);
      // Reset form after success
      setSelectedFile(null);
      if (document.getElementById("file-input") instanceof HTMLInputElement) {
        (document.getElementById("file-input") as HTMLInputElement).value = "";
      }
    } catch (err) {
      clearInterval(interval);
      setProgress(0);
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  };

  const handleExport = async () => {
    if (!databaseName) {
      setError("请选择源数据库");
      return;
    }

    if (!tableName) {
      setError("请输入源表名");
      return;
    }

    setLoading(true);
    setProgress(0);
    setError(null);
    setSuccess(null);

    // Simulate progress
    const interval = setInterval(() => {
      setProgress(prev => {
        if (prev >= 90) {
          clearInterval(interval);
          return prev;
        }
        return prev + 10;
      });
    }, 300);

    try {
      const dataUrl = await exportData(exportFormat, databaseName, tableName);
      clearInterval(interval);
      setProgress(100);
      
      // Create download link
      const link = document.createElement("a");
      link.href = dataUrl;
      link.download = `${tableName}.${exportFormat}`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      
      setSuccess(`成功导出 ${databaseName}.${tableName} 数据`);
    } catch (err) {
      clearInterval(interval);
      setProgress(0);
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
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
              <h1 className="text-2xl font-medium">数据导入导出</h1>
              <p className="text-muted-foreground">导入和导出数据库数据</p>
            </div>
          </div>
          
          <Tabs defaultValue="import" value={activeTab} onValueChange={setActiveTab}>
            <TabsList className="mb-4">
              <TabsTrigger value="import" className="flex items-center">
                <Upload size={16} className="mr-2" />
                导入数据
              </TabsTrigger>
              <TabsTrigger value="export" className="flex items-center">
                <Download size={16} className="mr-2" />
                导出数据
              </TabsTrigger>
            </TabsList>
            
            <TabsContent value="import">
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center">
                    <Upload size={20} className="mr-2" />
                    导入数据
                  </CardTitle>
                  <CardDescription>
                    将CSV或其他格式的数据导入到数据库表中
                  </CardDescription>
                </CardHeader>
                
                <CardContent>
                  {error && (
                    <Alert className="mb-4" variant="destructive">
                      <AlertTriangle className="h-4 w-4" />
                      <AlertDescription>{error}</AlertDescription>
                    </Alert>
                  )}
                  
                  {success && (
                    <Alert className="mb-4" variant="success">
                      <AlertDescription>{success}</AlertDescription>
                    </Alert>
                  )}
                  
                  <div className="space-y-4">
                    <div className="space-y-2">
                      <Label htmlFor="file-input">选择文件</Label>
                      <Input
                        id="file-input"
                        type="file"
                        accept=".csv,.json,.sql,.xlsx"
                        onChange={handleFileChange}
                        disabled={loading}
                      />
                      {selectedFile && (
                        <p className="text-sm text-muted-foreground">
                          文件名: {selectedFile.name} ({Math.round(selectedFile.size / 1024)} KB)
                        </p>
                      )}
                    </div>
                    
                    <div className="space-y-2">
                      <Label htmlFor="import-format">导入格式</Label>
                      <Select
                        value={importFormat}
                        onValueChange={setImportFormat}
                        disabled={loading}
                      >
                        <SelectTrigger id="import-format" className="w-full">
                          <SelectValue placeholder="选择格式" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="csv">CSV</SelectItem>
                          <SelectItem value="json">JSON</SelectItem>
                          <SelectItem value="sql">SQL</SelectItem>
                          <SelectItem value="xlsx">Excel (XLSX)</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                    
                    <div className="space-y-2">
                      <Label htmlFor="database-name">目标数据库</Label>
                      <Select
                        value={databaseName}
                        onValueChange={setDatabaseName}
                        disabled={loading}
                      >
                        <SelectTrigger id="database-name" className="w-full">
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
                      <Label htmlFor="table-name">目标表名</Label>
                      <Input
                        id="table-name"
                        value={tableName}
                        onChange={(e) => setTableName(e.target.value)}
                        placeholder="输入表名"
                        disabled={loading}
                      />
                    </div>
                    
                    {loading && (
                      <div className="space-y-2">
                        <div className="flex justify-between text-sm">
                          <span>导入进度</span>
                          <span>{progress}%</span>
                        </div>
                        <Progress value={progress} className="h-2" />
                      </div>
                    )}
                    
                    <div className="pt-4">
                      <Button
                        onClick={handleImport}
                        disabled={loading || !selectedFile}
                        className="w-full"
                      >
                        {loading ? (
                          <>
                            <RefreshCw className="mr-2 h-4 w-4 animate-spin" />
                            导入中...
                          </>
                        ) : (
                          <>
                            <Upload className="mr-2 h-4 w-4" />
                            开始导入
                          </>
                        )}
                      </Button>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </TabsContent>
            
            <TabsContent value="export">
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center">
                    <Download size={20} className="mr-2" />
                    导出数据
                  </CardTitle>
                  <CardDescription>
                    将数据库表导出为CSV或其他格式
                  </CardDescription>
                </CardHeader>
                
                <CardContent>
                  {error && (
                    <Alert className="mb-4" variant="destructive">
                      <AlertTriangle className="h-4 w-4" />
                      <AlertDescription>{error}</AlertDescription>
                    </Alert>
                  )}
                  
                  {success && (
                    <Alert className="mb-4" variant="success">
                      <AlertDescription>{success}</AlertDescription>
                    </Alert>
                  )}
                  
                  <div className="space-y-4">
                    <div className="space-y-2">
                      <Label htmlFor="export-format">导出格式</Label>
                      <Select
                        value={exportFormat}
                        onValueChange={setExportFormat}
                        disabled={loading}
                      >
                        <SelectTrigger id="export-format" className="w-full">
                          <SelectValue placeholder="选择格式" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="csv">CSV</SelectItem>
                          <SelectItem value="json">JSON</SelectItem>
                          <SelectItem value="sql">SQL</SelectItem>
                          <SelectItem value="xlsx">Excel (XLSX)</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                    
                    <div className="space-y-2">
                      <Label htmlFor="database-name-export">源数据库</Label>
                      <Select
                        value={databaseName}
                        onValueChange={setDatabaseName}
                        disabled={loading}
                      >
                        <SelectTrigger id="database-name-export" className="w-full">
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
                      <Label htmlFor="table-name-export">源表名</Label>
                      <Input
                        id="table-name-export"
                        value={tableName}
                        onChange={(e) => setTableName(e.target.value)}
                        placeholder="输入表名"
                        disabled={loading}
                      />
                    </div>
                    
                    {loading && (
                      <div className="space-y-2">
                        <div className="flex justify-between text-sm">
                          <span>导出进度</span>
                          <span>{progress}%</span>
                        </div>
                        <Progress value={progress} className="h-2" />
                      </div>
                    )}
                    
                    <div className="pt-4">
                      <Button
                        onClick={handleExport}
                        disabled={loading || !databaseName || !tableName}
                        className="w-full"
                      >
                        {loading ? (
                          <>
                            <RefreshCw className="mr-2 h-4 w-4 animate-spin" />
                            导出中...
                          </>
                        ) : (
                          <>
                            <Download className="mr-2 h-4 w-4" />
                            开始导出
                          </>
                        )}
                      </Button>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </TabsContent>
          </Tabs>
        </div>
      </div>
    </div>
  );
} 