import React, { useState, useEffect, useRef } from 'react';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import SupabaseSidebar from '@/components/supabase-sidebar';
import TopNavigation from '@/components/top-navigation';
import SettingsCategory from '@/components/settings/settings-category';
import { Button } from '@/components/ui/button';
import { useToast } from '@/components/ui/use-toast';
import { 
  Settings, Shield, Database, Bell, MonitorSmartphone, Plug2, 
  RefreshCw, Download, Upload, Menu
} from 'lucide-react';
import { getSystemSettings, SystemSetting, batchUpdateSettings } from '@/lib/api/settings';

export default function SettingsPage() {
  const { toast } = useToast();
  const [activeTab, setActiveTab] = useState('general');
  const [allSettings, setAllSettings] = useState<SystemSetting[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    fetchAllSettings();
  }, []);

  const fetchAllSettings = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await getSystemSettings();
      setAllSettings(response.data.data || []);
    } catch (error) {
      console.error('Failed to fetch settings:', error);
      setError(`加载设置失败：${error instanceof Error ? error.message : '未知错误'}`);
      toast({
        variant: 'destructive',
        title: '加载失败',
        description: '无法加载系统设置，请刷新重试',
      });
    } finally {
      setLoading(false);
    }
  };

  const handleExportSettings = () => {
    try {
      const settingsJson = JSON.stringify(allSettings, null, 2);
      const blob = new Blob([settingsJson], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `dataflare-settings-${new Date().toISOString().split('T')[0]}.json`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
      
      toast({
        title: '导出成功',
        description: '系统设置已成功导出',
      });
    } catch (error) {
      console.error('Failed to export settings:', error);
      toast({
        variant: 'destructive',
        title: '导出失败',
        description: '导出系统设置时发生错误',
      });
    }
  };

  const handleImportSettings = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = async (e) => {
      try {
        const content = e.target?.result as string;
        const importedSettings = JSON.parse(content) as SystemSetting[];
        
        // 验证导入的设置格式
        if (!Array.isArray(importedSettings) || !importedSettings.every(s => 
          s.settingId && s.key && s.value !== undefined && s.category)) {
          throw new Error('Invalid settings format');
        }

        // 更新设置
        const updatePayload = importedSettings.map(s => ({
          settingId: s.settingId,
          value: s.value,
          description: s.description
        }));

        await batchUpdateSettings(updatePayload);
        await fetchAllSettings();
        
        toast({
          title: '导入成功',
          description: `成功导入${importedSettings.length}个设置项`,
        });
      } catch (error) {
        console.error('Failed to import settings:', error);
        toast({
          variant: 'destructive',
          title: '导入失败',
          description: '导入系统设置时发生错误，请检查文件格式',
        });
      }
    };
    reader.readAsText(file);
    
    // 重置输入，以便同一文件可以重复选择
    event.target.value = '';
  };

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
              <h1 className="text-2xl font-medium">系统设置</h1>
              <p className="text-muted-foreground">配置系统偏好和参数</p>
            </div>
            
            <div className="flex gap-2">
              <Button variant="outline" size="sm" onClick={fetchAllSettings} disabled={loading}>
                <RefreshCw size={16} className="mr-2" />
                刷新
              </Button>
              <Button variant="outline" size="sm" onClick={handleExportSettings}>
                <Download size={16} className="mr-2" />
                导出设置
              </Button>
              <Button variant="outline" size="sm" asChild>
                <label className="cursor-pointer">
                  <Upload size={16} className="mr-2" />
                  导入设置
                  <input 
                    type="file" 
                    className="hidden" 
                    accept=".json" 
                    onChange={handleImportSettings} 
                    ref={fileInputRef}
                  />
                </label>
              </Button>
            </div>
          </div>
          
          <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-4">
            <TabsList className="grid grid-cols-3 md:grid-cols-6 mb-4">
              <TabsTrigger value="general" className="flex items-center gap-2">
                <Settings className="h-4 w-4" />
                <span className="hidden md:inline">通用</span>
              </TabsTrigger>
              <TabsTrigger value="security" className="flex items-center gap-2">
                <Shield className="h-4 w-4" />
                <span className="hidden md:inline">安全</span>
              </TabsTrigger>
              <TabsTrigger value="backup" className="flex items-center gap-2">
                <Database className="h-4 w-4" />
                <span className="hidden md:inline">备份</span>
              </TabsTrigger>
              <TabsTrigger value="notification" className="flex items-center gap-2">
                <Bell className="h-4 w-4" />
                <span className="hidden md:inline">通知</span>
              </TabsTrigger>
              <TabsTrigger value="display" className="flex items-center gap-2">
                <MonitorSmartphone className="h-4 w-4" />
                <span className="hidden md:inline">显示</span>
              </TabsTrigger>
              <TabsTrigger value="integration" className="flex items-center gap-2">
                <Plug2 className="h-4 w-4" />
                <span className="hidden md:inline">集成</span>
              </TabsTrigger>
            </TabsList>
            
            <TabsContent value="general">
              <SettingsCategory
                category="general"
                title="通用设置"
                description="基本系统配置和设置项"
              />
            </TabsContent>
            
            <TabsContent value="security">
              <SettingsCategory
                category="security"
                title="安全设置"
                description="用户认证和系统安全相关设置"
              />
            </TabsContent>
            
            <TabsContent value="backup">
              <SettingsCategory
                category="backup"
                title="备份设置"
                description="数据备份和恢复相关配置"
              />
            </TabsContent>
            
            <TabsContent value="notification">
              <SettingsCategory
                category="notification"
                title="通知设置"
                description="系统通知和提醒配置"
              />
            </TabsContent>
            
            <TabsContent value="display">
              <SettingsCategory
                category="display"
                title="显示设置"
                description="用户界面和主题相关配置"
              />
            </TabsContent>
            
            <TabsContent value="integration">
              <SettingsCategory
                category="integration"
                title="集成设置"
                description="第三方系统集成配置"
              />
            </TabsContent>
          </Tabs>
        </div>
      </div>
    </div>
  );
}