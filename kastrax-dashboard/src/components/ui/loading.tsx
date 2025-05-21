import React from 'react';
import { ReloadIcon } from '@radix-ui/react-icons';

const Loading: React.FC = () => {
  return (
    <div className="flex items-center justify-center min-h-screen">
      <div className="flex flex-col items-center gap-2">
        <ReloadIcon className="h-8 w-8 animate-spin text-primary" />
        <span className="text-sm text-muted-foreground">加载中...</span>
      </div>
    </div>
  );
};

export default Loading;
