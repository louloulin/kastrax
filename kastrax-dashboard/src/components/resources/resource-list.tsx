import React, { useState, useEffect } from 'react';
import {
  Table,
  TableBody,
  TableCaption,
  TableCell,
  TableHead,
  TableHeader,
  TableRow
} from '@/components/ui/table';
import { Button } from '@/components/ui/button';
import { 
  Plus, 
  Edit, 
  Trash2, 
  Copy, 
  Eye, 
  MoreHorizontal,
  Database,
  MessageSquare,
  Plug,
  HardDrive,
  Server
} from 'lucide-react';
import { 
  Card, 
  CardContent, 
  CardDescription, 
  CardHeader, 
  CardTitle 
} from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger
} from '@/components/ui/dropdown-menu';
import { getResourceTypeIcon, ResourceGroupType } from '@/lib/config/resource.config';

// 资源数据接口
interface ResourceItem {
  id: string;
  name: string;
  resourceType: string;
  group: ResourceGroupType;
  createdAt: string;
  status: 'active' | 'inactive' | 'error';
}

// 资源状态标签
const StatusBadge = ({ status }: { status: string }) => {
  switch (status) {
    case 'active':
      return <Badge variant="outline" className="bg-green-50 text-green-700 hover:bg-green-50">活动</Badge>;
    case 'inactive':
      return <Badge variant="outline" className="bg-gray-50 text-gray-500 hover:bg-gray-50">未激活</Badge>;
    case 'error':
      return <Badge variant="outline" className="bg-red-50 text-red-700 hover:bg-red-50">错误</Badge>;
    default:
      return <Badge variant="outline">未知</Badge>;
  }
};

// 资源图标
const ResourceTypeIcon = ({ type }: { type: string }) => {
  const iconName = getResourceTypeIcon(type);
  
  switch (iconName) {
    case 'database':
      return <Database className="h-5 w-5" />;
    case 'message-square':
      return <MessageSquare className="h-5 w-5" />;
    case 'plug':
      return <Plug className="h-5 w-5" />;
    case 'hard-drive':
      return <HardDrive className="h-5 w-5" />;
    case 'server':
      return <Server className="h-5 w-5" />;
    default:
      return <Database className="h-5 w-5" />;
  }
};

export default function ResourceList() {
  const [resources, setResources] = useState<ResourceItem[]>([]);
  const [loading, setLoading] = useState(true);

  // 加载资源列表
  useEffect(() => {
    // 这里应该是从API获取资源列表
    // 为了演示，使用模拟数据
    setTimeout(() => {
      setResources([
        {
          id: '1',
          name: 'MySQL生产数据库',
          resourceType: 'MYSQL',
          group: ResourceGroupType.DATABASE,
          createdAt: '2023-06-01',
          status: 'active'
        },
        {
          id: '2',
          name: 'MQTT消息服务',
          resourceType: 'MQTT',
          group: ResourceGroupType.CHANNEL,
          createdAt: '2023-06-02',
          status: 'active'
        },
        {
          id: '3',
          name: 'OPC UA设备连接',
          resourceType: 'OPCUA',
          group: ResourceGroupType.PROTOCOL,
          createdAt: '2023-06-03',
          status: 'error'
        },
        {
          id: '4',
          name: 'Redis缓存服务',
          resourceType: 'REDIS',
          group: ResourceGroupType.DATABASE,
          createdAt: '2023-06-04',
          status: 'inactive'
        }
      ]);
      setLoading(false);
    }, 1000);
  }, []);

  // 查看资源详情
  const handleViewResource = (id: string) => {
    console.log('查看资源:', id);
    // 跳转到资源详情页面或打开详情抽屉
  };

  // 编辑资源
  const handleEditResource = (id: string) => {
    console.log('编辑资源:', id);
    // 跳转到资源编辑页面或打开编辑抽屉
  };

  // 删除资源
  const handleDeleteResource = (id: string) => {
    if (window.confirm('确定要删除此资源吗？')) {
      console.log('删除资源:', id);
      // 调用API删除资源，然后刷新列表
      setResources(resources.filter(resource => resource.id !== id));
    }
  };

  // 复制资源
  const handleDuplicateResource = (id: string) => {
    console.log('复制资源:', id);
    // 复制资源逻辑
  };

  // 创建新资源
  const handleCreateResource = () => {
    console.log('创建新资源');
    // 跳转到资源创建页面或打开创建抽屉
  };

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between">
        <div>
          <CardTitle>资源列表</CardTitle>
          <CardDescription>管理系统的所有数据资源连接</CardDescription>
        </div>
        <Button onClick={handleCreateResource}>
          <Plus className="mr-2 h-4 w-4" />
          新建资源
        </Button>
      </CardHeader>
      <CardContent>
        <Table>
          <TableCaption>当前共有 {resources.length} 个资源</TableCaption>
          <TableHeader>
            <TableRow>
              <TableHead>资源名称</TableHead>
              <TableHead>资源类型</TableHead>
              <TableHead>创建日期</TableHead>
              <TableHead>状态</TableHead>
              <TableHead className="text-right">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={5} className="text-center py-10">
                  <div className="flex items-center justify-center">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
                    <span className="ml-2">加载中...</span>
                  </div>
                </TableCell>
              </TableRow>
            ) : resources.length === 0 ? (
              <TableRow>
                <TableCell colSpan={5} className="text-center py-10 text-muted-foreground">
                  暂无资源数据，点击"新建资源"创建
                </TableCell>
              </TableRow>
            ) : (
              resources.map((resource) => (
                <TableRow key={resource.id}>
                  <TableCell className="font-medium">
                    <div className="flex items-center">
                      <div className="mr-2">
                        <ResourceTypeIcon type={resource.resourceType} />
                      </div>
                      <div>{resource.name}</div>
                    </div>
                  </TableCell>
                  <TableCell>{resource.resourceType}</TableCell>
                  <TableCell>{resource.createdAt}</TableCell>
                  <TableCell>
                    <StatusBadge status={resource.status} />
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex items-center justify-end space-x-1">
                      <Button variant="ghost" size="icon" onClick={() => handleViewResource(resource.id)}>
                        <Eye className="h-4 w-4" />
                      </Button>
                      <Button variant="ghost" size="icon" onClick={() => handleEditResource(resource.id)}>
                        <Edit className="h-4 w-4" />
                      </Button>
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" size="icon">
                            <MoreHorizontal className="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem onClick={() => handleDuplicateResource(resource.id)}>
                            <Copy className="mr-2 h-4 w-4" />
                            <span>复制</span>
                          </DropdownMenuItem>
                          <DropdownMenuItem 
                            onClick={() => handleDeleteResource(resource.id)}
                            className="text-red-600 focus:text-red-600"
                          >
                            <Trash2 className="mr-2 h-4 w-4" />
                            <span>删除</span>
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </div>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  );
} 