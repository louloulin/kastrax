import React from 'react';
import { Link } from 'react-router-dom';
import { Button } from '../../components/ui/button';

export default function NotFoundPage() {
  return (
    <div className="flex flex-col items-center justify-center min-h-screen p-4 text-center">
      <h1 className="text-6xl font-bold mb-4">404</h1>
      <h2 className="text-2xl font-semibold mb-6">页面不存在</h2>
      <p className="text-muted-foreground mb-8 max-w-md">
        您访问的页面不存在或已被移除。请检查 URL 是否正确，或返回首页。
      </p>
      <div className="flex gap-4">
        <Button asChild variant="default">
          <Link to="/">返回首页</Link>
        </Button>
        <Button asChild variant="outline">
          <Link to="/">前往仪表盘</Link>
        </Button>
      </div>
    </div>
  );
}
