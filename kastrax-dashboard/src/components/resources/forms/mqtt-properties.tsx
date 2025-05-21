import { useFormContext } from "react-hook-form";
import { BaseProperties, UrlProperties, AuthProperties } from "./base-properties";
import { Input } from "@/components/ui/input";
import { FormItem, FormLabel, FormControl, FormField, FormMessage } from "@/components/ui/form";
import { Checkbox } from "@/components/ui/checkbox";

/**
 * MQTT属性表单组件
 * 用于MQTT消息通道资源配置
 */
export default function MqttProperties() {
  const form = useFormContext();

  return (
    <div className="space-y-6">
      {/* 基础属性 */}
      <BaseProperties />
      
      {/* URL属性 */}
      <UrlProperties />
      
      {/* MQTT特有属性 */}
      <div className="space-y-4 mt-4">
        <div className="text-sm font-medium mb-2">MQTT设置</div>
        
        {/* 客户端ID */}
        <FormField
          control={form.control}
          name="properties.clientId"
          render={({ field }) => (
            <FormItem>
              <FormLabel>客户端ID</FormLabel>
              <FormControl>
                <Input
                  placeholder="输入客户端ID，为空则自动生成"
                  {...field}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        
        {/* Topic */}
        <FormField
          control={form.control}
          name="properties.topic"
          render={({ field }) => (
            <FormItem>
              <FormLabel className="required">Topic</FormLabel>
              <FormControl>
                <Input
                  placeholder="输入Topic，例如：test/topic"
                  {...field}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        
        {/* QoS */}
        <FormField
          control={form.control}
          name="properties.qos"
          render={({ field }) => (
            <FormItem>
              <FormLabel>服务质量(QoS)</FormLabel>
              <FormControl>
                <select
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                  {...field}
                >
                  <option value="0">0 - 至多一次</option>
                  <option value="1">1 - 至少一次</option>
                  <option value="2">2 - 只有一次</option>
                </select>
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        
        {/* 保留消息 */}
        <FormField
          control={form.control}
          name="properties.retained"
          render={({ field }) => (
            <FormItem className="flex flex-row items-start space-x-3 space-y-0 rounded-md border p-4">
              <FormControl>
                <Checkbox
                  checked={field.value}
                  onCheckedChange={field.onChange}
                />
              </FormControl>
              <div className="space-y-1 leading-none">
                <FormLabel>保留消息</FormLabel>
                <p className="text-sm text-muted-foreground">
                  启用后，消息将被保留在MQTT代理上
                </p>
              </div>
            </FormItem>
          )}
        />
      </div>
      
      {/* 认证属性 */}
      <AuthProperties />
    </div>
  );
} 