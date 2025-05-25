import React from "react";
import { Users, Database, ArrowRightLeft, Clock, Code, ExternalLink } from "lucide-react";
import { StatCard, DashboardCard } from "../components/dashboard-card";
import UserActivityChart from "../components/user-activity-chart";
import SupabaseSidebar from "../components/supabase-sidebar";
import TopNavigation from "../components/top-navigation";
import DataTable from "../components/data-table";
import { Link } from "react-router-dom";

// 模拟最近活动数据
const recentActivities = [
  { 
    id: "1", 
    user: "zhangsan@example.com", 
    action: "创建了表 users", 
    timestamp: "2023-08-15 10:23:45" 
  },
  { 
    id: "2", 
    user: "lisi@example.com", 
    action: "更新了数据 posts/123", 
    timestamp: "2023-08-15 09:45:30" 
  },
  { 
    id: "3", 
    user: "wangwu@example.com", 
    action: "删除了记录 comments/456", 
    timestamp: "2023-08-14 16:20:12" 
  },
  { 
    id: "4", 
    user: "admin@example.com", 
    action: "执行了 SQL 查询", 
    timestamp: "2023-08-14 14:05:37" 
  },
  { 
    id: "5", 
    user: "system", 
    action: "添加了新用户 zhaoliu", 
    timestamp: "2023-08-13 18:30:22" 
  },
];

// 定义活动列
const activityColumns = [
  { id: "user", header: "用户", accessorKey: "user" },
  { id: "action", header: "操作", accessorKey: "action" },
  { id: "timestamp", header: "时间", accessorKey: "timestamp" },
];

export default function DashboardPage() {
  return (
    <div className="flex min-h-screen bg-background">
      <SupabaseSidebar />
      
      <div className="flex-1 flex flex-col">
        <TopNavigation />
        
        <div className="flex-1 px-6 py-6 overflow-auto">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
            <StatCard 
              title="总用户" 
              value="1,234" 
              change="+12.3%" 
              icon={<Users size={18} className="text-primary" />} 
            />
            <StatCard 
              title="数据库大小" 
              value="2.45 GB" 
              change="+5.8%" 
              icon={<Database size={18} className="text-primary" />} 
            />
            <StatCard 
              title="API 请求" 
              value="45,678" 
              change="+24.5%" 
              icon={<ArrowRightLeft size={18} className="text-primary" />} 
            />
            <StatCard 
              title="平均响应时间" 
              value="123 ms" 
              change="-8.7%" 
              decreasing
              icon={<Clock size={18} className="text-primary" />} 
            />
          </div>
          
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
            <UserActivityChart />
            <DashboardCard title="SQL 编辑器" className="flex flex-col">
              <div className="mb-3 bg-secondary/20 rounded-md p-3 text-sm font-mono overflow-hidden text-muted-foreground">
                <div>SELECT * FROM users</div>
                <div>WHERE created_at {'>'}  now() - interval '7 days'</div>
                <div>LIMIT 10;</div>
              </div>
              <p className="text-sm text-muted-foreground mb-4">
                使用高级 SQL 编辑器查询和管理数据库，支持语法高亮、自动完成、SQL 格式化和查询历史记录。
              </p>
              <Link 
                to="/sql" 
                className="bg-primary text-primary-foreground hover:bg-primary/90 rounded-md px-4 py-2 text-sm font-medium mt-auto self-start flex items-center"
              >
                <Code size={16} className="mr-2" />
                打开完整 SQL 编辑器
                <ExternalLink size={14} className="ml-1" />
              </Link>
            </DashboardCard>
          </div>
          
          <div className="mb-6">
            <DataTable 
              title="最近活动" 
              description="系统中的最近操作记录"
              columns={activityColumns}
              data={recentActivities}
            />
          </div>
        </div>
      </div>
    </div>
  );
}
