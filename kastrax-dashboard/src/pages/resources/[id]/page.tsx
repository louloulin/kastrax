import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  Card, 
  CardContent, 
  CardDescription, 
  CardHeader, 
  CardTitle 
} from '@/components/ui/card';
import { 
  Tabs, 
  TabsContent, 
  TabsList, 
  TabsTrigger 
} from '@/components/ui/tabs';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import { 
  ArrowLeft, 
  Save, 
  Trash2, 
  Play, 
  AlertCircle,
  CheckCircle2
} from 'lucide-react';
import { getResourceById, updateResource, deleteResource, testResourceConnection } from '@/lib/api/resources';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Skeleton } from '@/components/ui/skeleton';
import DynamicResourceForm from '@/components/resources/dynamic-resource-form';
import TopNavigation from '@/components/top-navigation';
import SupabaseSidebar from '@/components/supabase-sidebar';

export default function ResourceDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [resource, setResource] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [testing, setTesting] = useState(false);
  const [testResult, setTestResult] = useState<{ success: boolean; message: string } | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [formData, setFormData] = useState<any>(null);
  const [activeTab, setActiveTab] = useState('details');
  
  useEffect(() => {
    if (id) {
      fetchResource();
    }
  }, [id]);
  
  const fetchResource = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await getResourceById(id as string);
      const resourceData = response.data.data;
      setResource(resourceData);
      setFormData({
        resourceName: resourceData.resourceName,
        resourceType: resourceData.resourceType,
        properties: resourceData.properties
      });
    } catch (error: any) {
      setError(error.response?.data?.message || '获取资源详情失败');
      console.error('Failed to fetch resource:', error);
    } finally {
      setLoading(false);
    }
  };
  
  const handleFormChange = (data: any) => {
    setFormData(data);
  };
  
  const handleSave = async () => {
    setSaving(true);
    setError(null);
    try {
      await updateResource({
        resourceId: id as string,
        resourceName: formData.resourceName,
        properties: formData.properties
      });
      // Refresh resource data
      await fetchResource();
    } catch (error: any) {
      setError(error.response?.data?.message || '保存资源失败');
      console.error('Failed to save resource:', error);
    } finally {
      setSaving(false);
    }
  };
  
  const handleDelete = async () => {
    if (window.confirm('确定要删除此资源吗？')) {
      try {
        await deleteResource(id as string);
        navigate('/resources');
      } catch (error: any) {
        setError(error.response?.data?.message || '删除资源失败');
        console.error('Failed to delete resource:', error);
      }
    }
  };
  
  const handleTestConnection = async () => {
    setTesting(true);
    setTestResult(null);
    try {
      const response = await testResourceConnection(id as string);
      const result = response.data.data;
      
      setTestResult({
        success: result.success,
        message: result.message
      });
    } catch (error: any) {
      setTestResult({
        success: false,
        message: error.response?.data?.message || '连接测试失败'
      });
    } finally {
      setTesting(false);
    }
  };
  
  const renderContent = () => {
    if (loading) {
      return (
        <div className="space-y-4">
          <Skeleton className="h-8 w-3/4" />
          <Skeleton className="h-32 w-full" />
          <Skeleton className="h-32 w-full" />
        </div>
      );
    }
    
    if (error) {
      return (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertTitle>错误</AlertTitle>
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      );
    }
    
    return (
      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList className="grid w-full grid-cols-2">
          <TabsTrigger value="details">详细信息</TabsTrigger>
          <TabsTrigger value="monitoring">监控</TabsTrigger>
        </TabsList>
        
        <TabsContent value="details" className="mt-4">
          {testResult && (
            <Alert variant={testResult.success ? "default" : "destructive"} className="mb-4">
              {testResult.success 
                ? <CheckCircle2 className="h-4 w-4 text-green-500" /> 
                : <AlertCircle className="h-4 w-4" />
              }
              <AlertTitle>{testResult.success ? '成功' : '失败'}</AlertTitle>
              <AlertDescription>{testResult.message}</AlertDescription>
            </Alert>
          )}
          
          <DynamicResourceForm 
            initialData={formData}
            onChange={handleFormChange}
            readOnly={false}
          />
        </TabsContent>
        
        <TabsContent value="monitoring" className="mt-4">
          <Card>
            <CardHeader>
              <CardTitle>资源监控</CardTitle>
              <CardDescription>查看资源的使用情况和性能指标</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="text-center p-12 text-muted-foreground">
                监控功能即将推出
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    );
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
              <h1 className="text-2xl font-bold truncate">
                {loading ? '加载中...' : resource?.resourceName}
              </h1>
            </div>
            
            <div className="flex gap-2">
              <Button 
                variant="outline" 
                onClick={handleTestConnection} 
                disabled={loading || testing}
              >
                {testing ? '测试中...' : '测试连接'}
                {!testing && <Play className="ml-2 h-4 w-4" />}
              </Button>
              
              <Button 
                variant="destructive" 
                onClick={handleDelete} 
                disabled={loading}
              >
                删除
                <Trash2 className="ml-2 h-4 w-4" />
              </Button>
              
              <Button 
                onClick={handleSave} 
                disabled={loading || saving}
              >
                {saving ? '保存中...' : '保存'}
                {!saving && <Save className="ml-2 h-4 w-4" />}
              </Button>
            </div>
          </div>
          
          <Separator className="my-4" />
          
          {renderContent()}
        </div>
      </div>
    </div>
  );
} 