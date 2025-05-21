import React, { useState } from 'react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Loader2 } from 'lucide-react';
import { useToast } from '@/components/ui/use-toast';
import { Script, deleteScript } from '@/lib/api/scripts';

interface ScriptDeleteDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  script: Script | null;
  onSuccess?: () => void;
}

export default function ScriptDeleteDialog({
  open,
  onOpenChange,
  script,
  onSuccess,
}: ScriptDeleteDialogProps) {
  const { toast } = useToast();
  const [isDeleting, setIsDeleting] = useState(false);

  // 处理删除操作
  const handleDelete = async () => {
    if (!script) return;
    
    setIsDeleting(true);
    try {
      await deleteScript(script.scriptId);
      
      toast({
        title: "删除成功",
        description: `脚本 "${script.scriptName}" 已成功删除`,
      });
      
      // 关闭对话框
      onOpenChange(false);
      
      // 执行成功回调，通常是刷新列表
      if (onSuccess) {
        onSuccess();
      }
    } catch (error) {
      console.error('删除脚本失败:', error);
      toast({
        variant: "destructive",
        title: "删除失败",
        description: "无法删除脚本，请稍后重试",
      });
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>删除脚本</DialogTitle>
          <DialogDescription>
            确定要删除脚本 <span className="font-semibold">{script?.scriptName}</span> 吗？此操作不可撤销。
          </DialogDescription>
        </DialogHeader>
        
        <DialogFooter className="mt-4">
          <Button
            variant="outline"
            onClick={() => onOpenChange(false)}
            disabled={isDeleting}
          >
            取消
          </Button>
          <Button 
            variant="destructive" 
            onClick={handleDelete}
            disabled={isDeleting}
          >
            {isDeleting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            删除
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
} 