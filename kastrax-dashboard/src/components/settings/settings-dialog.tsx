import React, { useState } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { SystemSetting, updateSetting, SettingUpdatePayload } from '@/lib/api/settings';
import SettingsForm from './settings-form';
import { useToast } from '@/components/ui/use-toast';

interface SettingsDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  setting: SystemSetting | null;
  onSuccess: () => void;
}

export default function SettingsDialog({
  open,
  onOpenChange,
  setting,
  onSuccess,
}: SettingsDialogProps) {
  const { toast } = useToast();
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (values: SettingUpdatePayload) => {
    setError(null);
    try {
      await updateSetting(values);
      toast({
        title: '设置已更新',
        description: `设置项已成功更新。`,
      });
      onSuccess();
      onOpenChange(false);
    } catch (err: any) {
      const errorMessage = err.response?.data?.msg || '操作失败，请重试';
      setError(errorMessage);
      console.error('Setting update failed:', err);
      throw err; // 让表单组件知道提交失败
    }
  };

  // 如果没有设置项，不显示对话框
  if (!setting) return null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>编辑设置: {setting.key}</DialogTitle>
        </DialogHeader>

        {error && (
          <Alert variant="destructive" className="mb-4">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}

        <SettingsForm
          setting={setting}
          onSubmit={handleSubmit}
          onCancel={() => onOpenChange(false)}
        />
      </DialogContent>
    </Dialog>
  );
} 