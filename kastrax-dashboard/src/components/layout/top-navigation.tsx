import React from 'react';
import { MainNav } from '../../components/main-nav';
import { ModeToggle } from '../../components/mode-toggle';
import { UserNav } from '../../components/user-nav';

export default function TopNavigation() {
  return (
    <div className="border-b">
      <div className="flex h-16 items-center px-4">
        <div className="flex items-center space-x-4">
          <img 
            src="/logo.svg" 
            alt="Dataflare Logo" 
            className="h-8 w-8" 
          />
          <h2 className="text-xl font-bold tracking-tight">Dataflare</h2>
        </div>
        <div className="ml-8 flex-1">
          <MainNav />
        </div>
        <div className="flex items-center space-x-4">
          <ModeToggle />
          <UserNav />
        </div>
      </div>
    </div>
  );
} 