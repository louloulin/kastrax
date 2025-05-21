import React, { useState } from 'react';
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle } from '@/components/ui/alert-dialog';
import { deleteVariable, Variable } from '@/lib/api/variables';
import { useToast } from '@/components/ui/use-toast';
import { Loader2 } from 'lucide-react';

interface VariableDeleteDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  variable: Variable | null;
  onSuccess: () => void;
}

export default function VariableDeleteDialog({
  open,
  onOpenChange,
  variable,
  onSuccess,
}: VariableDeleteDialogProps) {
  const { toast } = useToast();
  const [deleting, setDeleting] = useState(false);

  const handleDelete = async () => {
    if (!variable) return;
    
    setDeleting(true);
    try {
      await deleteVariable(variable.variableId);
      toast({
        title: '变量已删除',
        description: `变量 "${variable.variableName}" 已成功删除。`,
      });
      onSuccess();
      onOpenChange(false);
    } catch (err: any) {
      toast({
        variant: 'destructive',
        title: '删除失败',
        description: err.response?.data?.msg || '删除变量失败，请重试',
      });
      console.error('Failed to delete variable:', err);
    } finally {
      setDeleting(false);
    }
  };

  if (!variable) return null;

  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>确认删除变量</AlertDialogTitle>
          <AlertDialogDescription>
            您确定要删除变量 <strong>{variable.variableName}</strong> 吗？此操作无法撤销，可能会影响依赖此变量的规则和脚本。
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel disabled={deleting}>取消</AlertDialogCancel>
          <AlertDialogAction 
            onClick={handleDelete} 
            className="bg-destructive hover:bg-destructive/90"
            disabled={deleting}
          >
            {deleting ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                删除中...
              </>
            ) : (
              '删除'
            )}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
} 