import React, { ReactNode } from "react";
import { MoreHorizontal } from "lucide-react";

interface DashboardCardProps {
  title?: string;
  children: ReactNode;
  action?: ReactNode;
  className?: string;
  fullHeight?: boolean;
}

export function DashboardCard({
  title,
  children,
  action,
  className = "",
  fullHeight = false,
}: DashboardCardProps) {
  return (
    <div
      className={`bg-card border border-border rounded-lg shadow-sm ${
        fullHeight ? "h-full" : ""
      } ${className}`}
    >
      {title && (
        <div className="flex items-center justify-between border-b border-border p-4">
          <h3 className="font-medium">{title}</h3>
          {action || (
            <button className="text-muted-foreground hover:text-foreground p-1 rounded-full">
              <MoreHorizontal size={16} />
            </button>
          )}
        </div>
      )}
      <div className="p-4">{children}</div>
    </div>
  );
}

interface StatCardProps {
  title: string;
  value: string | number;
  change?: string;
  icon?: ReactNode;
  decreasing?: boolean;
}

export function StatCard({ title, value, change, icon, decreasing = false }: StatCardProps) {
  return (
    <DashboardCard>
      <div className="flex justify-between">
        <div>
          <p className="text-sm text-muted-foreground">{title}</p>
          <p className="text-2xl font-semibold mt-1">{value}</p>
          {change && (
            <div className="flex items-center mt-1">
              <span
                className={`text-xs font-medium ${
                  decreasing ? "text-destructive" : "text-primary"
                }`}
              >
                {change}
              </span>
            </div>
          )}
        </div>
        {icon && <div>{icon}</div>}
      </div>
    </DashboardCard>
  );
} 