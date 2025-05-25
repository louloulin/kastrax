import React, { useState } from "react";
import { 
  LineChart, 
  Line, 
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip, 
  Legend, 
  ResponsiveContainer 
} from "recharts";
import { DashboardCard } from "./dashboard-card";

// 模拟数据
const data = [
  { name: "1月", users: 100, queries: 230 },
  { name: "2月", users: 140, queries: 290 },
  { name: "3月", users: 180, queries: 370 },
  { name: "4月", users: 240, queries: 450 },
  { name: "5月", users: 280, queries: 520 },
  { name: "6月", users: 320, queries: 590 },
  { name: "7月", users: 390, queries: 620 },
];

interface ChartToggleProps {
  activeRange: string;
  onChange: (range: string) => void;
}

const ChartToggle: React.FC<ChartToggleProps> = ({ activeRange, onChange }) => {
  const ranges = ["今天", "最近7天", "最近30天", "今年"];
  
  return (
    <div className="flex space-x-2 text-xs mb-4">
      {ranges.map((range) => (
        <button
          key={range}
          className={`px-2.5 py-1 rounded-md font-medium transition-colors ${
            activeRange === range 
              ? "bg-primary/10 text-primary"
              : "text-muted-foreground hover:text-foreground hover:bg-secondary"
          }`}
          onClick={() => onChange(range)}
        >
          {range}
        </button>
      ))}
    </div>
  );
};

export default function UserActivityChart() {
  const [range, setRange] = useState("今年");
  
  return (
    <DashboardCard title="用户活动" fullHeight>
      <ChartToggle activeRange={range} onChange={setRange} />
      
      <div className="h-72">
        <ResponsiveContainer width="100%" height="100%">
          <LineChart
            data={data}
            margin={{ top: 5, right: 5, left: 0, bottom: 5 }}
          >
            <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.1)" />
            <XAxis 
              dataKey="name" 
              tick={{ fontSize: 12 }} 
              stroke="rgba(255,255,255,0.3)" 
            />
            <YAxis 
              tick={{ fontSize: 12 }} 
              stroke="rgba(255,255,255,0.3)"
            />
            <Tooltip 
              contentStyle={{ 
                backgroundColor: "rgba(22, 22, 26, 0.9)", 
                border: "1px solid rgba(255,255,255,0.1)",
                borderRadius: "4px",
                fontSize: "12px"
              }}
            />
            <Legend iconSize={8} wrapperStyle={{ fontSize: "12px" }} />
            <Line
              type="monotone"
              dataKey="users"
              stroke="#3ECF8E"
              strokeWidth={2}
              activeDot={{ r: 6 }}
              dot={{ r: 3 }}
              name="用户"
            />
            <Line 
              type="monotone" 
              dataKey="queries" 
              stroke="#6A7BFF" 
              strokeWidth={2}
              activeDot={{ r: 6 }}
              dot={{ r: 3 }}
              name="查询"
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </DashboardCard>
  );
} 