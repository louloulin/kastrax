import { useFormContext } from "react-hook-form";
import { BaseProperties, AddressProperties, AuthProperties, TimeProperties } from "./base-properties";
import { Input } from "@/components/ui/input";
import { FormItem, FormLabel, FormControl, FormField, FormMessage, FormDescription } from "@/components/ui/form";
import { Textarea } from "@/components/ui/textarea";

/**
 * OPC UA属性表单组件
 * 用于OPC UA协议资源配置
 */
export default function OpcuaProperties() {
  const form = useFormContext();

  return (
    <div className="space-y-6">
      {/* 基础属性 */}
      <BaseProperties />
      
      {/* 地址属性 */}
      <AddressProperties />
      
      {/* OPC UA特有属性 */}
      <div className="space-y-4 mt-4">
        <div className="text-sm font-medium mb-2">OPC UA设置</div>
        
        {/* 命名空间 */}
        <FormField
          control={form.control}
          name="properties.namespace"
          render={({ field }) => (
            <FormItem>
              <FormLabel>命名空间</FormLabel>
              <FormControl>
                <Input
                  placeholder="输入命名空间索引，例如：2"
                  type="number"
                  {...field}
                  onChange={(e) => field.onChange(e.target.valueAsNumber || 0)}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        
        {/* 点位地址 */}
        <FormField
          control={form.control}
          name="properties.address"
          render={({ field }) => (
            <FormItem>
              <FormLabel>点位地址</FormLabel>
              <FormControl>
                <Input
                  placeholder="输入点位地址，例如：Channel1.Device1.Tag1"
                  {...field}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        
        {/* 点位列表 */}
        <FormField
          control={form.control}
          name="properties.points"
          render={({ field }) => (
            <FormItem>
              <FormLabel>点位列表</FormLabel>
              <FormControl>
                <Textarea
                  placeholder="输入点位列表，每行一个，例如：ns=2;s=Channel1.Device1.Tag1"
                  className="min-h-[100px]"
                  {...field}
                />
              </FormControl>
              <FormDescription>
                每行一个点位，格式为：ns=命名空间索引;s=点位标识符
              </FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />
        
        {/* 安全策略 */}
        <FormField
          control={form.control}
          name="properties.securityPolicy"
          render={({ field }) => (
            <FormItem>
              <FormLabel>安全策略</FormLabel>
              <FormControl>
                <select
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                  {...field}
                >
                  <option value="None">None</option>
                  <option value="Basic128Rsa15">Basic128Rsa15</option>
                  <option value="Basic256">Basic256</option>
                  <option value="Basic256Sha256">Basic256Sha256</option>
                </select>
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        
        {/* 安全模式 */}
        <FormField
          control={form.control}
          name="properties.securityMode"
          render={({ field }) => (
            <FormItem>
              <FormLabel>安全模式</FormLabel>
              <FormControl>
                <select
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                  {...field}
                >
                  <option value="None">None</option>
                  <option value="Sign">Sign</option>
                  <option value="SignAndEncrypt">SignAndEncrypt</option>
                </select>
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