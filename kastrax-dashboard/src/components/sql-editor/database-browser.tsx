import React, { useState } from "react";
import { 
  Database, 
  TableProperties, 
  Key, 
  FileText, 
  Workflow, 
  ChevronRight, 
  ChevronDown, 
  Search, 
  Layers,
  RefreshCw 
} from "lucide-react";

// 模拟数据库结构
const MOCK_DATABASE = {
  name: "example_db",
  tables: [
    {
      name: "users",
      columns: [
        { name: "id", type: "INTEGER", isPrimary: true },
        { name: "name", type: "VARCHAR(255)", isPrimary: false },
        { name: "email", type: "VARCHAR(255)", isPrimary: false },
        { name: "created_at", type: "TIMESTAMP", isPrimary: false },
      ],
      indexes: [
        { name: "idx_users_email", columns: ["email"] },
      ]
    },
    {
      name: "orders",
      columns: [
        { name: "id", type: "INTEGER", isPrimary: true },
        { name: "user_id", type: "INTEGER", isPrimary: false },
        { name: "amount", type: "DECIMAL(10,2)", isPrimary: false },
        { name: "status", type: "VARCHAR(50)", isPrimary: false },
        { name: "created_at", type: "TIMESTAMP", isPrimary: false },
      ],
      indexes: [
        { name: "idx_orders_user_id", columns: ["user_id"] },
        { name: "idx_orders_status", columns: ["status"] },
      ]
    },
    {
      name: "products",
      columns: [
        { name: "id", type: "INTEGER", isPrimary: true },
        { name: "name", type: "VARCHAR(255)", isPrimary: false },
        { name: "price", type: "DECIMAL(10,2)", isPrimary: false },
        { name: "stock", type: "INTEGER", isPrimary: false },
        { name: "category", type: "VARCHAR(100)", isPrimary: false },
      ],
      indexes: [
        { name: "idx_products_category", columns: ["category"] },
      ]
    },
  ],
  views: [
    { name: "active_users", definition: "SELECT * FROM users WHERE last_login > NOW() - INTERVAL 30 DAY" },
    { name: "product_sales", definition: "SELECT p.name, SUM(o.amount) FROM products p JOIN order_items oi ON p.id = oi.product_id JOIN orders o ON oi.order_id = o.id GROUP BY p.id" },
  ],
  functions: [
    { name: "calculate_discount", returnType: "DECIMAL", argTypes: ["DECIMAL", "VARCHAR"] },
    { name: "get_user_orders", returnType: "TABLE", argTypes: ["INTEGER"] },
  ]
};

interface DatabaseBrowserProps {
  onSelectTable: (tableName: string) => void;
  onSelectQuery: (query: string) => void;
}

export default function DatabaseBrowser({ onSelectTable, onSelectQuery }: DatabaseBrowserProps) {
  const [expandedItems, setExpandedItems] = useState<Record<string, boolean>>({
    tables: true,
    views: false,
    functions: false,
  });
  
  const [expandedTables, setExpandedTables] = useState<Record<string, boolean>>({});
  const [searchQuery, setSearchQuery] = useState("");
  
  const toggleExpand = (item: string) => {
    setExpandedItems(prev => ({
      ...prev,
      [item]: !prev[item]
    }));
  };
  
  const toggleExpandTable = (tableName: string) => {
    setExpandedTables(prev => ({
      ...prev,
      [tableName]: !prev[tableName]
    }));
  };
  
  const generateSelectQuery = (tableName: string) => {
    const table = MOCK_DATABASE.tables.find(t => t.name === tableName);
    if (!table) return "";
    
    return `SELECT * FROM ${tableName} LIMIT 100;`;
  };
  
  const handleTableClick = (tableName: string) => {
    onSelectTable(tableName);
    onSelectQuery(generateSelectQuery(tableName));
  };
  
  const filteredTables = searchQuery
    ? MOCK_DATABASE.tables.filter(t => t.name.toLowerCase().includes(searchQuery.toLowerCase()))
    : MOCK_DATABASE.tables;
    
  const filteredViews = searchQuery
    ? MOCK_DATABASE.views.filter(v => v.name.toLowerCase().includes(searchQuery.toLowerCase()))
    : MOCK_DATABASE.views;
    
  const filteredFunctions = searchQuery
    ? MOCK_DATABASE.functions.filter(f => f.name.toLowerCase().includes(searchQuery.toLowerCase()))
    : MOCK_DATABASE.functions;
  
  return (
    <div className="bg-card border border-border rounded-md h-full flex flex-col overflow-hidden">
      <div className="p-3 border-b border-border">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center">
            <Database size={16} className="mr-2 text-primary" />
            <h3 className="font-medium">{MOCK_DATABASE.name}</h3>
          </div>
          <button className="p-1 rounded-sm text-muted-foreground hover:text-foreground hover:bg-secondary">
            <RefreshCw size={14} />
          </button>
        </div>
        
        <div className="relative">
          <Search size={14} className="absolute left-2 top-1/2 transform -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            placeholder="搜索..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-8 py-1 pr-2 text-sm bg-secondary/30 border border-border rounded-md"
          />
        </div>
      </div>
      
      <div className="flex-grow overflow-auto p-1">
        <div className="mb-1">
          <div 
            className="flex items-center py-1 px-2 rounded-sm hover:bg-secondary cursor-pointer"
            onClick={() => toggleExpand("tables")}
          >
            {expandedItems.tables ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
            <TableProperties size={14} className="ml-1 mr-2 text-muted-foreground" />
            <span className="text-sm">表 ({filteredTables.length})</span>
          </div>
          
          {expandedItems.tables && filteredTables.map(table => (
            <div key={table.name} className="ml-2">
              <div 
                className="flex items-center py-1 px-2 rounded-sm hover:bg-secondary cursor-pointer"
                onClick={() => toggleExpandTable(table.name)}
              >
                {expandedTables[table.name] ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
                <Layers size={14} className="ml-1 mr-2 text-muted-foreground" />
                <span 
                  className="text-sm hover:text-primary"
                  onClick={(e) => {
                    e.stopPropagation();
                    handleTableClick(table.name);
                  }}
                >
                  {table.name}
                </span>
              </div>
              
              {expandedTables[table.name] && (
                <div className="ml-6">
                  <div className="flex items-center py-1">
                    <span className="text-xs text-muted-foreground">列</span>
                  </div>
                  
                  {table.columns.map(column => (
                    <div 
                      key={column.name}
                      className="flex items-center py-0.5 px-2 text-xs"
                    >
                      {column.isPrimary && <Key size={10} className="mr-1 text-yellow-500" />}
                      <span className="mr-2">{column.name}</span>
                      <span className="text-muted-foreground">{column.type}</span>
                    </div>
                  ))}
                  
                  {table.indexes.length > 0 && (
                    <>
                      <div className="flex items-center py-1 mt-1">
                        <span className="text-xs text-muted-foreground">索引</span>
                      </div>
                      
                      {table.indexes.map(index => (
                        <div 
                          key={index.name}
                          className="flex items-center py-0.5 px-2 text-xs"
                        >
                          <span>{index.name}</span>
                          <span className="text-muted-foreground ml-2">
                            ({index.columns.join(", ")})
                          </span>
                        </div>
                      ))}
                    </>
                  )}
                </div>
              )}
            </div>
          ))}
        </div>
        
        <div className="mb-1">
          <div 
            className="flex items-center py-1 px-2 rounded-sm hover:bg-secondary cursor-pointer"
            onClick={() => toggleExpand("views")}
          >
            {expandedItems.views ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
            <FileText size={14} className="ml-1 mr-2 text-muted-foreground" />
            <span className="text-sm">视图 ({filteredViews.length})</span>
          </div>
          
          {expandedItems.views && filteredViews.map(view => (
            <div 
              key={view.name}
              className="flex items-center py-1 px-2 ml-6 text-sm hover:bg-secondary cursor-pointer"
              onClick={() => onSelectQuery(`SELECT * FROM ${view.name} LIMIT 100;`)}
            >
              <span>{view.name}</span>
            </div>
          ))}
        </div>
        
        <div>
          <div 
            className="flex items-center py-1 px-2 rounded-sm hover:bg-secondary cursor-pointer"
            onClick={() => toggleExpand("functions")}
          >
            {expandedItems.functions ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
            <Workflow size={14} className="ml-1 mr-2 text-muted-foreground" />
            <span className="text-sm">函数 ({filteredFunctions.length})</span>
          </div>
          
          {expandedItems.functions && filteredFunctions.map(func => (
            <div 
              key={func.name}
              className="flex items-center py-1 px-2 ml-6 text-sm"
            >
              <span>{func.name}({func.argTypes.join(", ")})</span>
              <span className="text-muted-foreground ml-1">→ {func.returnType}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
} 