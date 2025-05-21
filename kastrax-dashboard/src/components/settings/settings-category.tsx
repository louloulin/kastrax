import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { getSettingsByCategory, SystemSetting } from '@/lib/api/settings';
import { useToast } from '@/components/ui/use-toast';
import { Edit, Loader2 } from 'lucide-react';
import { Switch } from '@/components/ui/switch';
import SettingsDialog from './settings-dialog';

interface SettingsCategoryProps {
  category: 'general' | 'security' | 'backup' | 'notification' | 'display' | 'integration';
  title: string;
  description: string;
}

export default function SettingsCategory({ category, title, description }: SettingsCategoryProps) {
  const { toast } = useToast();
  const [settings, setSettings] = useState<SystemSetting[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [selectedSetting, setSelectedSetting] = useState<SystemSetting | null>(null);

  useEffect(() => {
    fetchSettings();
  }, [category]);

  const fetchSettings = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await getSettingsByCategory(category);
      setSettings(response.data.data || []);
    } catch (err: any) {
      const errorMessage = err.response?.data?.msg || '无法加载设置，请重试';
      setError(errorMessage);
      toast({
        variant: 'destructive',
        title: '加载失败',
        description: errorMessage,
      });
    } finally {
      setLoading(false);
    }
  };

  const handleEditSetting = (setting: SystemSetting) => {
    setSelectedSetting(setting);
    setDialogOpen(true);
  };

  const renderSettingItem = (setting: SystemSetting) => {
    // 对于布尔值设置，显示开关控件
    if (setting.value === 'true' || setting.value === 'false') {
      return (
        <div key={setting.settingId} className="flex items-center justify-between py-3 border-b last:border-0">
          <div>
            <div className="font-medium">{setting.key}</div>
            {setting.description && (
              <div className="text-sm text-muted-foreground">{setting.description}</div>
            )}
          </div>
          <div className="flex items-center gap-2">
            <Switch 
              checked={setting.value === 'true'} 
              onCheckedChange={() => handleEditSetting(setting)}
              aria-readonly
            />
            <Button 
              variant="ghost" 
              size="sm"
              className="h-8 w-8 p-0"
              onClick={() => handleEditSetting(setting)}
            >
              <Edit className="h-4 w-4" />
            </Button>
          </div>
        </div>
      );
    }

    // 对于其他类型的设置，显示值和编辑按钮
    return (
      <div key={setting.settingId} className="flex items-center justify-between py-3 border-b last:border-0">
        <div>
          <div className="font-medium">{setting.key}</div>
          {setting.description && (
            <div className="text-sm text-muted-foreground">{setting.description}</div>
          )}
          <div className="text-sm mt-1">{setting.value}</div>
        </div>
        <Button 
          variant="ghost" 
          size="sm"
          className="h-8 w-8 p-0"
          onClick={() => handleEditSetting(setting)}
        >
          <Edit className="h-4 w-4" />
        </Button>
      </div>
    );
  };

  return (
    <>
      <Card className="mb-6">
        <CardHeader>
          <CardTitle>{title}</CardTitle>
          <CardDescription>{description}</CardDescription>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="flex justify-center py-4">
              <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
            </div>
          ) : error ? (
            <div className="text-destructive py-4 text-center">{error}</div>
          ) : settings.length === 0 ? (
            <div className="text-muted-foreground py-4 text-center">暂无设置项</div>
          ) : (
            <div className="space-y-1 divide-y">
              {settings.map(renderSettingItem)}
            </div>
          )}
        </CardContent>
      </Card>

      <SettingsDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        setting={selectedSetting}
        onSuccess={fetchSettings}
      />
    </>
  );
} 