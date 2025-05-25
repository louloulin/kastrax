import { useFormContext } from "react-hook-form";
import { BaseProperties, AddressProperties, AuthProperties, TimeProperties } from "./base-properties";
import { Input } from "@/components/ui/input";
import { FormItem, FormLabel, FormControl, FormField, FormMessage, FormDescription } from "@/components/ui/form";
import { Textarea } from "@/components/ui/textarea";

/**
 * Redis属性表单组件
 * 用于Redis数据库资源配置
 */
export default function RedisProperties() {
  const form = useFormContext();

  return (
    <div className="space-y-6">
      {/* 基础属性 */}
      <BaseProperties />
      
      {/* 模式选择 */}
      <div className="space-y-4 mt-4">
        <div className="text-sm font-medium mb-2">Redis模式</div>
        
        <FormField
          control={form.control}
          name="properties.mode"
          render={({ field }) => (
            <FormItem>
              <FormLabel>部署模式</FormLabel>
              <FormControl>
                <select
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                  {...field}
                  defaultValue="STANDALONE"
                >
                  <option value="STANDALONE">单机模式</option>
                  <option value="SENTINEL">哨兵模式</option>
                  <option value="CLUSTER">集群模式</option>
                </select>
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
      </div>
      
      {/* 根据模式显示不同的表单 */}
      {form.watch('properties.mode') === 'STANDALONE' ? (
        <AddressProperties />
      ) : (
        <div className="space-y-4 mt-4">
          <div className="text-sm font-medium mb-2">集群节点</div>
          
          <FormField
            control={form.control}
            name="properties.nodes"
            render={({ field }) => (
              <FormItem>
                <FormLabel className="required">节点列表</FormLabel>
                <FormControl>
                  <Textarea
                    placeholder="输入节点列表，每行一个，例如：127.0.0.1:6379"
                    className="min-h-[100px]"
                    {...field}
                  />
                </FormControl>
                <FormDescription>
                  集群模式下需要填写节点列表，每行一个节点，格式为：ip:port
                </FormDescription>
                <FormMessage />
              </FormItem>
            )}
          />
          
          {form.watch('properties.mode') === 'SENTINEL' && (
            <FormField
              control={form.control}
              name="properties.masterName"
              render={({ field }) => (
                <FormItem>
                  <FormLabel className="required">主节点名称</FormLabel>
                  <FormControl>
                    <Input
                      placeholder="输入主节点名称，例如：mymaster"
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          )}
        </div>
      )}
      
      {/* 数据库索引 */}
      <div className="space-y-4 mt-4">
        <div className="text-sm font-medium mb-2">数据库设置</div>
        
        <FormField
          control={form.control}
          name="properties.database"
          render={({ field }) => (
            <FormItem>
              <FormLabel>数据库索引</FormLabel>
              <FormControl>
                <Input
                  type="number"
                  placeholder="0"
                  {...field}
                  onChange={(e) => field.onChange(e.target.valueAsNumber || 0)}
                />
              </FormControl>
              <FormDescription>
                Redis数据库索引，默认为0
              </FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />
        
        {/* 执行命令 */}
        <FormField
          control={form.control}
          name="properties.command"
          render={({ field }) => (
            <FormItem>
              <FormLabel>执行命令</FormLabel>
              <FormControl>
                <Textarea
                  placeholder="输入Redis命令，例如：GET key"
                  className="min-h-[100px]"
                  {...field}
                />
              </FormControl>
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