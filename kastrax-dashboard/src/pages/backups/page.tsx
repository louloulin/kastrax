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
  RefreshCw,
  Menu,
  Download,
  RotateCcw,
  Calendar,
  HardDrive,
  Loader2
} from "lucide-react";
import { getBackupList, createBackup, restoreBackup, deleteBackup, downloadBackup, Backup } from "../../lib/api/backups";
import { 
  Dialog, 
  DialogContent, 
  DialogHeader, 
  DialogTitle,
  DialogFooter,
  DialogDescription
} from "../../components/ui/dialog";
import { Alert, AlertDescription, AlertTitle } from "../../components/ui/alert";
import { useToast } from "../../components/ui/use-toast";

// Column definition for backups
const backupColumns = [
  { id: "backupName", header: "备份名称", accessorKey: "backupName" },
  { id: "backupSize", header: "备份大小", accessorKey: "backupSize",
    cell: ({ row }: any) => {
      // Format size in MB/GB
      const sizeInBytes = row.original.backupSize;
      if (sizeInBytes < 1024 * 1024) {
        return `${(sizeInBytes / 1024).toFixed(2)} KB`;
      } else if (sizeInBytes < 1024 * 1024 * 1024) {
        return `${(sizeInBytes / (1024 * 1024)).toFixed(2)} MB`;
      } else {
        return `${(sizeInBytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;
      }
    }
  },
  { id: "backupType", header: "备份类型", accessorKey: "backupType" },
  { id: "createTime", header: "创建时间", accessorKey: "createTime" },
  { id: "status", header: "状态", accessorKey: "status",
    cell: ({ row }: any) => {
      const status = row.original.status;
      let statusText = "未知";
      let statusColor = "bg-gray-500";
      
      if (status === 1) {
        statusText = "完成";
        statusColor = "bg-green-500";
      } else if (status === 0) {
        statusText = "进行中";
        statusColor = "bg-blue-500";
      } else if (status === -1) {
        statusText = "失败";
        statusColor = "bg-red-500";
      }
      
      return (
        <div className="flex items-center">
          <div className={`h-2 w-2 rounded-full mr-2 ${statusColor}`}></div>
          <span>{statusText}</span>
        </div>
      );
    }
  },
];

export default function BackupsPage() {
  const { toast } = useToast();
  const [backups, setBackups] = useState<Backup[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [isRestoreDialogOpen, setIsRestoreDialogOpen] = useState(false);
  const [selectedBackup, setSelectedBackup] = useState<Backup | null>(null);
  const [operationInProgress, setOperationInProgress] = useState(false);
  
  // Load backups on component mount
  useEffect(() => {
    fetchBackups();
  }, []);
  
  // Function to fetch backups from API
  const fetchBackups = async () => {
    setLoading(true);
    try {
      const response = await getBackupList();
      // 确保每行数据都有唯一的ID
      const dataWithIds = (response.data.data || []).map((backup: Backup) => ({
        ...backup,
        id: backup.backupId // 使用backupId作为行的唯一标识
      }));
      setBackups(dataWithIds);
    } catch (error) {
      console.error("Failed to fetch backups:", error);
      toast({
        variant: "destructive",
        title: "加载失败",
        description: "无法加载备份列表，请刷新重试"
      });
    } finally {
      setLoading(false);
    }
  };

  // Function to create a new backup
  const handleCreateBackup = async () => {
    setOperationInProgress(true);
    try {
      await createBackup({
        backupName: `Backup_${new Date().toISOString().slice(0, 19).replace(/[-:T]/g, '')}`,
        backupType: 'full'
      });
      toast({
        title: "创建成功",
        description: "系统备份已开始创建，请稍后刷新查看"
      });
      fetchBackups(); // Refresh list after action
    } catch (error) {
      console.error("Failed to create backup:", error);
      toast({
        variant: "destructive",
        title: "创建失败",
        description: "无法创建系统备份，请稍后重试"
      });
    } finally {
      setOperationInProgress(false);
    }
  };

  // Function to download a backup
  const handleDownloadBackup = async (backupId: string) => {
    setOperationInProgress(true);
    try {
      const response = await downloadBackup(backupId);
      // 创建Blob对象
      const blob = new Blob([response.data.data], { type: 'application/octet-stream' });
      // 创建下载链接
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `backup_${backupId}.zip`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      
      toast({
        title: "下载成功",
        description: "备份文件已开始下载"
      });
    } catch (error) {
      console.error("Failed to download backup:", error);
      toast({
        variant: "destructive",
        title: "下载失败",
        description: "无法下载备份文件，请稍后重试"
      });
    } finally {
      setOperationInProgress(false);
    }
  };

  // Function to delete a backup
  const handleDeleteBackup = async (backupId: string) => {
    if (window.confirm("确定要删除这个备份吗？此操作不可恢复。")) {
      setOperationInProgress(true);
      try {
        await deleteBackup(backupId);
        toast({
          title: "删除成功",
          description: "备份已成功删除"
        });
        fetchBackups(); // Refresh list after action
      } catch (error) {
        console.error("Failed to delete backup:", error);
        toast({
          variant: "destructive",
          title: "删除失败",
          description: "无法删除备份，请稍后重试"
        });
      } finally {
        setOperationInProgress(false);
      }
    }
  };

  // Function to restore from a backup
  const handleRestoreBackup = async () => {
    if (selectedBackup) {
      setOperationInProgress(true);
      try {
        await restoreBackup(selectedBackup.backupId);
        setIsRestoreDialogOpen(false);
        toast({
          title: "恢复成功",
          description: "系统已成功从备份恢复"
        });
        fetchBackups(); // Refresh list after action
      } catch (error) {
        console.error("Failed to restore backup:", error);
        toast({
          variant: "destructive",
          title: "恢复失败",
          description: "无法从备份恢复系统，请稍后重试"
        });
      } finally {
        setOperationInProgress(false);
      }
    }
  };
  
  // Open restore confirmation dialog
  const openRestoreDialog = (backup: Backup) => {
    setSelectedBackup(backup);
    setIsRestoreDialogOpen(true);
  };

  // Filter backups based on search query
  const filteredBackups = backups.filter(backup => 
    backup.backupName.toLowerCase().includes(searchQuery.toLowerCase()) ||
    backup.backupType.toLowerCase().includes(searchQuery.toLowerCase())
  );
  
  return (
    <div className="flex min-h-screen bg-background">
      <SupabaseSidebar collapsed={sidebarCollapsed} setCollapsed={setSidebarCollapsed} />
      
      <div className="flex-1 flex flex-col">
        <TopNavigation>
          <Button 
            variant="ghost" 
            size="sm" 
            onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
            className="md:hidden"
          >
            <Menu size={16} />
          </Button>
        </TopNavigation>
        
        <div className="flex-1 p-6 overflow-auto">
          <div className="flex justify-between items-center mb-6">
            <div>
              <h1 className="text-2xl font-medium">备份管理</h1>
              <p className="text-muted-foreground">管理系统备份和恢复</p>
            </div>
            
            <div className="flex gap-2">
              <Button variant="outline" size="sm" onClick={fetchBackups} disabled={operationInProgress}>
                {loading ? (
                  <Loader2 size={16} className="mr-2 animate-spin" />
                ) : (
                  <RefreshCw size={16} className="mr-2" />
                )}
                刷新
              </Button>
              <Button 
                size="sm" 
                onClick={handleCreateBackup} 
                disabled={operationInProgress}
              >
                <Plus size={16} className="mr-2" />
                创建备份
              </Button>
            </div>
          </div>
          
          <div className="mb-6">
            <Alert className="mb-4">
              <Calendar className="h-4 w-4" />
              <AlertTitle>自动备份</AlertTitle>
              <AlertDescription>
                系统每日凌晨会自动创建备份，并保留最近 7 天的备份记录。
              </AlertDescription>
            </Alert>
            
            <div className="relative">
              <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
              <Input
                type="search"
                placeholder="搜索备份名称..."
                className="pl-8"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>
          </div>
          
          <DataTable 
            title="备份列表"
            description="查看和管理系统备份"
            columns={backupColumns}
            data={filteredBackups}
            isLoading={loading}
            actions={(row: Backup & { id?: string }) => (
              <div className="flex gap-2">
                <Button 
                  variant="ghost" 
                  size="sm" 
                  className="h-8 w-8 p-0" 
                  title="下载备份"
                  onClick={() => handleDownloadBackup(row.backupId)}
                  disabled={operationInProgress || row.status !== 1}
                >
                  <Download className="h-4 w-4" />
                </Button>
                <Button 
                  variant="ghost" 
                  size="sm" 
                  className="h-8 w-8 p-0" 
                  title="从此备份恢复"
                  onClick={() => openRestoreDialog(row)}
                  disabled={operationInProgress || row.status !== 1}
                >
                  <RotateCcw className="h-4 w-4" />
                </Button>
                <Button 
                  variant="ghost" 
                  size="sm" 
                  className="h-8 w-8 p-0" 
                  title="删除备份"
                  onClick={() => handleDeleteBackup(row.backupId)}
                  disabled={operationInProgress}
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>
            )}
          />
        </div>
      </div>
      
      {/* 恢复确认对话框 */}
      <Dialog open={isRestoreDialogOpen} onOpenChange={setIsRestoreDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>确认恢复</DialogTitle>
            <DialogDescription>
              您确定要从备份 <strong>{selectedBackup?.backupName}</strong> 恢复系统吗？
              此操作将覆盖当前系统数据，无法撤销。
            </DialogDescription>
          </DialogHeader>
          <div className="bg-amber-50 dark:bg-amber-950 border border-amber-200 dark:border-amber-900 p-3 rounded-md mt-2">
            <div className="flex items-start">
              <HardDrive className="h-5 w-5 text-amber-600 dark:text-amber-400 mr-2 mt-0.5" />
              <div className="text-sm text-amber-800 dark:text-amber-300">
                <p className="font-medium">恢复过程中系统将暂时不可用</p>
                <p className="mt-1">恢复完成后，系统将自动重新启动，这可能需要几分钟时间。</p>
              </div>
            </div>
          </div>
          <DialogFooter className="gap-2 sm:gap-0">
            <Button 
              variant="outline" 
              onClick={() => setIsRestoreDialogOpen(false)}
              disabled={operationInProgress}
            >
              取消
            </Button>
            <Button 
              variant="destructive" 
              onClick={handleRestoreBackup}
              disabled={operationInProgress}
            >
              {operationInProgress ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  恢复中...
                </>
              ) : (
                <>确认恢复</>
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
} 