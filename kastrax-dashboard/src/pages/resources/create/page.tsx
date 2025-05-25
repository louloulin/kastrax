import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Card, 
  CardContent, 
  CardDescription, 
  CardHeader, 
  CardTitle 
} from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import { ArrowLeft, Save } from 'lucide-react';
import { createResource } from '@/lib/api/resources';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { AlertCircle } from 'lucide-react';
import DynamicResourceForm from '@/components/resources/dynamic-resource-form';
import TopNavigation from '@/components/top-navigation';
import SupabaseSidebar from '@/components/supabase-sidebar';

export default function CreateResourcePage() {
  const navigate = useNavigate();
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [formData, setFormData] = useState<any>({
    resourceName: '',
    resourceType: '',
    properties: {}
  });
  
  const handleFormChange = (data: any) => {
    setFormData(data);
  };
  
  const handleCreate = async () => {
    if (!formData.resourceName || !formData.resourceType) {
      setError('请填写资源名称和选择资源类型');
      return;
    }
    
    setSaving(true);
    setError(null);
    
    try {
      const response = await createResource({
        resourceName: formData.resourceName,
        resourceType: formData.resourceType,
        properties: formData.properties
      });
      
      // 创建成功后导航到资源详情页
      const { resourceId } = response.data.data;
      navigate(`/resources/${resourceId}`);
    } catch (error: any) {
      setError(error.response?.data?.message || '创建资源失败');
      console.error('Failed to create resource:', error);
      setSaving(false);
    }
  };
  
  return (
    <div className="flex h-screen bg-background">
      <SupabaseSidebar />
      
      <div className="flex-1 flex flex-col">
        <TopNavigation />
        
        <div className="flex-1 p-6 pt-0 overflow-auto">
          <div className="flex justify-between items-center py-4">
            <div className="flex items-center gap-2">
              <Button 
                variant="ghost" 
                size="sm" 
                onClick={() => navigate('/resources')}
              >
                <ArrowLeft className="h-4 w-4 mr-2" />
                返回
              </Button>
              <h1 className="text-2xl font-bold">创建新资源</h1>
            </div>
            
            <Button 
              onClick={handleCreate} 
              disabled={saving}
            >
              {saving ? '创建中...' : '创建资源'}
              {!saving && <Save className="ml-2 h-4 w-4" />}
            </Button>
          </div>
          
          <Separator className="my-4" />
          
          {error && (
            <Alert variant="destructive" className="mb-6">
              <AlertCircle className="h-4 w-4" />
              <AlertTitle>错误</AlertTitle>
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}
          
          <DynamicResourceForm 
            onChange={handleFormChange}
            onSubmit={handleCreate}
          />
        </div>
      </div>
    </div>
  );
} 