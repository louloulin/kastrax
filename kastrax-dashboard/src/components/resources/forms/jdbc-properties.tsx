import { useFormContext } from "react-hook-form";
import { BaseProperties, AddressProperties, AuthProperties, TimeProperties } from "./base-properties";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { FormItem, FormLabel, FormControl, FormField, FormMessage, FormDescription } from "@/components/ui/form";

/**
 * JDBC属性表单组件
 * 用于数据库资源配置（MySQL, PostgreSQL, SQL Server等）
 */
export default function JdbcProperties() {
  const form = useFormContext();

  return (
    <div className="space-y-6">
      {/* 基础属性 */}
      <BaseProperties />
      
      {/* 地址属性 */}
      <AddressProperties />
      
      {/* 数据库特有属性 */}
      <div className="space-y-4 mt-4">
        <div className="text-sm font-medium mb-2">数据库设置</div>
        
        {/* 数据库名称 */}
        <FormField
          control={form.control}
          name="properties.databaseName"
          render={({ field }) => (
            <FormItem>
              <FormLabel className="required">数据库名称</FormLabel>
              <FormControl>
                <Input
                  placeholder="输入数据库名称，例如：mydb"
                  {...field}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        
        {/* SQL语句 */}
        <FormField
          control={form.control}
          name="properties.sql"
          render={({ field }) => (
            <FormItem>
              <FormLabel>SQL语句模板</FormLabel>
              <FormControl>
                <Textarea
                  placeholder="输入SQL查询语句，例如：SELECT * FROM users WHERE created_at > ?"
                  className="min-h-[100px]"
                  {...field}
                />
              </FormControl>
              <FormDescription>
                可以使用参数占位符（?），这些占位符将在执行时被替换
              </FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />
      </div>
      
      {/* 认证属性 */}
      <AuthProperties />
      
      {/* 时间属性 */}
      <TimeProperties />
    </div>
  );
} 