import React from 'react';
import { useNavigate } from 'react-router-dom';
import { ShieldAlertIcon } from 'lucide-react';
import { Button } from '../../components/ui/button';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '../../components/ui/card';

/**
 * 未授权页面
 * 当用户尝试访问没有权限的页面时显示
 */
export function UnauthorizedPage() {
  const navigate = useNavigate();
  
  return (
    <div className="flex items-center justify-center min-h-screen bg-background p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center">
          <div className="flex justify-center mb-4">
            <ShieldAlertIcon className="h-16 w-16 text-destructive" />
          </div>
          <CardTitle className="text-2xl">访问被拒绝</CardTitle>
          <CardDescription>
            您没有权限访问此页面。
          </CardDescription>
        </CardHeader>
        <CardContent className="text-center">
          <p className="text-muted-foreground">
            如果您认为这是一个错误，请联系系统管理员获取必要的权限。
          </p>
        </CardContent>
        <CardFooter className="flex justify-center space-x-4">
          <Button variant="outline" onClick={() => navigate(-1)}>
            返回上一页
          </Button>
          <Button onClick={() => navigate('/')}>
            返回首页
          </Button>
        </CardFooter>
      </Card>
    </div>
  );
}

export default UnauthorizedPage;
