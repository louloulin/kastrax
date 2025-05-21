import React, { useState } from "react";
import { Plus, Filter, MoreHorizontal, Edit, Trash, ExternalLink, Loader2 } from "lucide-react";

export interface Column {
  id: string;
  header: string;
  accessorKey: string;
  cell?: (props: { row: any }) => React.ReactNode;
}

export interface DataTableProps {
  title?: string;
  description?: string;
  columns: Column[];
  data: any[];
  isLoading?: boolean;
  actions?: (row: any) => React.ReactNode;
}

export default function DataTable({
  title,
  description,
  columns,
  data,
  isLoading = false,
  actions
}: DataTableProps) {
  const [selectedRows, setSelectedRows] = useState<Record<string, boolean>>({});

  const toggleSelectAll = () => {
    if (Object.keys(selectedRows).length === data.length) {
      setSelectedRows({});
    } else {
      const newSelected: Record<string, boolean> = {};
      data.forEach((row) => {
        newSelected[row.id] = true;
      });
      setSelectedRows(newSelected);
    }
  };

  const toggleSelectRow = (id: string) => {
    setSelectedRows((prev) => ({
      ...prev,
      [id]: !prev[id],
    }));
  };

  const isAllSelected = data.length > 0 && Object.keys(selectedRows).length === data.length;

  return (
    <div className="bg-card border border-border rounded-lg shadow-sm">
      {(title || description) && (
        <div className="p-6 border-b border-border">
          {title && <h2 className="text-lg font-medium">{title}</h2>}
          {description && <p className="text-sm text-muted-foreground mt-1">{description}</p>}
        </div>
      )}

      <div className="p-4 flex items-center justify-between border-b border-border">
        <div className="flex space-x-2">
          <button className="bg-primary text-primary-foreground hover:bg-primary/90 rounded-md px-3 py-1.5 text-sm font-medium flex items-center">
            <Plus size={14} className="mr-1.5" />
            添加
          </button>
          <button className="border border-input bg-background hover:bg-secondary rounded-md px-3 py-1.5 text-sm font-medium flex items-center">
            <Filter size={14} className="mr-1.5" />
            筛选
          </button>
        </div>
      </div>

      <div className="overflow-x-auto">
        {isLoading ? (
          <div className="p-8 flex items-center justify-center">
            <Loader2 className="h-8 w-8 animate-spin text-primary" />
            <span className="ml-2 text-sm text-muted-foreground">加载中...</span>
          </div>
        ) : (
          <table className="w-full">
            <thead>
              <tr className="bg-secondary/50 border-b border-border">
                <th className="w-10 px-4 py-3 text-left">
                  <input
                    type="checkbox"
                    checked={isAllSelected}
                    onChange={toggleSelectAll}
                    className="rounded text-primary focus:ring-primary h-4 w-4"
                  />
                </th>
                {columns.map((column) => (
                  <th
                    key={column.id}
                    className="px-4 py-3 text-left text-sm font-medium"
                  >
                    {column.header}
                  </th>
                ))}
                <th className="w-10 px-4 py-3 text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              {data.length === 0 ? (
                <tr>
                  <td colSpan={columns.length + 2} className="px-4 py-8 text-center text-sm text-muted-foreground">
                    没有找到数据
                  </td>
                </tr>
              ) : (
                data.map((row) => (
                  <tr
                    key={row.id}
                    className="border-b border-border hover:bg-secondary/20"
                  >
                    <td className="px-4 py-3">
                      <input
                        type="checkbox"
                        checked={!!selectedRows[row.id]}
                        onChange={() => toggleSelectRow(row.id)}
                        className="rounded text-primary focus:ring-primary h-4 w-4"
                      />
                    </td>
                    {columns.map((column) => (
                      <td key={column.id} className="px-4 py-3 text-sm">
                        {column.cell ? column.cell({ row: { original: row } }) : (row[column.accessorKey] ?? '-')}
                      </td>
                    ))}
                    <td className="px-4 py-3 text-right">
                      {actions ? actions(row) : (
                        <div className="flex items-center justify-end space-x-2">
                          <button className="p-1 text-muted-foreground hover:text-foreground rounded-full">
                            <Edit size={14} />
                          </button>
                          <button className="p-1 text-muted-foreground hover:text-destructive rounded-full">
                            <Trash size={14} />
                          </button>
                          <button className="p-1 text-muted-foreground hover:text-foreground rounded-full">
                            <MoreHorizontal size={14} />
                          </button>
                        </div>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        )}
      </div>

      {!isLoading && data.length > 0 && (
        <div className="p-4 flex items-center justify-between border-t border-border">
          <div className="text-sm text-muted-foreground">
            显示 {data.length} 条中的 1-{data.length} 条
          </div>
          <div className="flex items-center space-x-2">
            <button className="px-2.5 py-1 border border-input rounded text-sm disabled:opacity-50" disabled>
              上一页
            </button>
            <span className="text-sm">第 1 页</span>
            <button className="px-2.5 py-1 border border-input rounded text-sm disabled:opacity-50" disabled>
              下一页
            </button>
          </div>
        </div>
      )}
    </div>
  );
}