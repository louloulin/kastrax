import React, { useState } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { createVariable, updateVariable, Variable } from '@/lib/api/variables';
import VariableForm, { VariableFormValues } from './variable-form';
import { useToast } from '@/components/ui/use-toast';

interface VariableDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  variable?: Variable;
  onSuccess: () => void;
}

export default function VariableDialog({
  open,
  onOpenChange,
  variable,
  onSuccess,
}: VariableDialogProps) {
  const { toast } = useToast();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isEditMode = !!variable;
  const title = isEditMode ? '编辑变量' : '创建新变量';

  const handleSubmit = async (values: VariableFormValues) => {
    setSubmitting(true);
    setError(null);

    try {
      if (isEditMode && variable) {
        await updateVariable({
          variableId: variable.variableId,
          variableName: values.variableName,
          variableValue: values.variableValue,
          description: values.description || undefined,
        });
        toast({
          title: '变量已更新',
          description: `变量 "${values.variableName}" 已成功更新。`,
        });
      } else {
        await createVariable({
          variableName: values.variableName,
          variableValue: values.variableValue,
          description: values.description || undefined,
        });
        toast({
          title: '变量已创建',
          description: `变量 "${values.variableName}" 已成功创建。`,
        });
      }
      onSuccess();
      onOpenChange(false);
    } catch (err: any) {
      const errorMessage = err.response?.data?.msg || '操作失败，请重试';
      setError(errorMessage);
      console.error('Variable operation failed:', err);
    } finally {
      setSubmitting(false);
    }
  };

  const handleCancel = () => {
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
        </DialogHeader>

        {error && (
          <Alert variant="destructive" className="mb-4">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}

        <VariableForm
          defaultValues={
            variable
              ? {
                  variableName: variable.variableName,
                  variableValue: variable.variableValue,
                  description: variable.description,
                }
              : undefined
          }
          onSubmit={handleSubmit}
          onCancel={handleCancel}
          isSubmitting={submitting}
        />
      </DialogContent>
    </Dialog>
  );
} 