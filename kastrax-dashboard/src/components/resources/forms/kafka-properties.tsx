import { useFormContext } from "react-hook-form";
import { BaseProperties, UrlProperties, AuthProperties } from "./base-properties";
import { Input } from "@/components/ui/input";
import { FormItem, FormLabel, FormControl, FormField, FormMessage } from "@/components/ui/form";

/**
 * Kafka属性表单组件
 * 用于Kafka消息通道资源配置
 */
export default function KafkaProperties() {
  const form = useFormContext();

  return (
    <div className="space-y-6">
      {/* 基础属性 */}
      <BaseProperties />
      
      {/* URL属性 */}
      <UrlProperties />
      
      {/* Kafka特有属性 */}
      <div className="space-y-4 mt-4">
        <div className="text-sm font-medium mb-2">Kafka设置</div>
        
        {/* Topic */}
        <FormField
          control={form.control}
          name="properties.topic"
          render={({ field }) => (
            <FormItem>
              <FormLabel className="required">Topic</FormLabel>
              <FormControl>
                <Input
                  placeholder="输入Topic，例如：my-topic"
                  {...field}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        
        {/* 消费者组ID */}
        <FormField
          control={form.control}
          name="properties.groupId"
          render={({ field }) => (
            <FormItem>
              <FormLabel>消费者组ID</FormLabel>
              <FormControl>
                <Input
                  placeholder="输入消费者组ID，例如：my-group"
                  {...field}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        
        {/* 自动提交 */}
        <FormField
          control={form.control}
          name="properties.autoOffset"
          render={({ field }) => (
            <FormItem>
              <FormLabel>偏移量策略</FormLabel>
              <FormControl>
                <select
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                  {...field}
                >
                  <option value="earliest">最早偏移量</option>
                  <option value="latest">最新偏移量</option>
                  <option value="none">不自动重置</option>
                </select>
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
      </div>
      
      {/* 认证属性 */}
      <AuthProperties />
    </div>
  );
} 