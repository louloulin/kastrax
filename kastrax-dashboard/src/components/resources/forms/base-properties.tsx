import { useEffect } from "react";
import { useFormContext } from "react-hook-form";
import { Input } from "@/components/ui/input";
import { FormItem, FormLabel, FormControl, FormField, FormMessage } from "@/components/ui/form";

/**
 * 基础属性表单组件
 * 所有资源类型都需要的通用表单字段
 */
export function BaseProperties() {
  const form = useFormContext();

  return (
    <div className="space-y-4">
      {/* 资源名称 */}
      <FormField
        control={form.control}
        name="resourceName"
        render={({ field }) => (
          <FormItem>
            <FormLabel className="required">资源名称</FormLabel>
            <FormControl>
              <Input
                placeholder="输入资源名称"
                {...field}
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
    </div>
  );
}

/**
 * 时间属性表单组件
 * 用于配置启动延迟和定时任务
 */
export function TimeProperties() {
  const form = useFormContext();

  return (
    <div className="space-y-4 mt-4">
      <div className="text-sm font-medium mb-2">时间设置</div>
      
      {/* 初始延迟 */}
      <div className="grid grid-cols-2 gap-4">
        <FormField
          control={form.control}
          name="properties.initialDelay"
          render={({ field }) => (
            <FormItem>
              <FormLabel>启动延迟</FormLabel>
              <FormControl>
                <Input
                  type="number"
                  placeholder="延迟时间"
                  {...field}
                  onChange={(e) => field.onChange(e.target.valueAsNumber || 0)}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        
        <FormField
          control={form.control}
          name="properties.initialDelayUnit"
          render={({ field }) => (
            <FormItem>
              <FormLabel>时间单位</FormLabel>
              <FormControl>
                <select
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                  {...field}
                >
                  <option value="MILLISECONDS">毫秒</option>
                  <option value="SECONDS">秒</option>
                  <option value="MINUTES">分钟</option>
                  <option value="HOURS">小时</option>
                  <option value="DAYS">天</option>
                </select>
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
      </div>
      
      {/* Cron表达式 */}
      <FormField
        control={form.control}
        name="properties.cronExpression"
        render={({ field }) => (
          <FormItem>
            <FormLabel>Cron表达式</FormLabel>
            <FormControl>
              <Input
                placeholder="例如: 0 0/5 * * * ?"
                {...field}
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
    </div>
  );
}

/**
 * 地址属性表单组件
 * 用于配置IP地址和端口
 */
export function AddressProperties() {
  const form = useFormContext();

  return (
    <div className="space-y-4 mt-4">
      <div className="text-sm font-medium mb-2">连接设置</div>
      
      {/* IP地址 */}
      <FormField
        control={form.control}
        name="properties.ip"
        render={({ field }) => (
          <FormItem>
            <FormLabel className="required">IP地址</FormLabel>
            <FormControl>
              <Input
                placeholder="例如: 192.168.1.1"
                {...field}
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
      
      {/* 端口 */}
      <FormField
        control={form.control}
        name="properties.port"
        render={({ field }) => (
          <FormItem>
            <FormLabel className="required">端口</FormLabel>
            <FormControl>
              <Input
                type="number"
                placeholder="例如: 3306"
                {...field}
                onChange={(e) => field.onChange(e.target.valueAsNumber || 0)}
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
    </div>
  );
}

/**
 * URL属性表单组件
 * 用于配置URL地址
 */
export function UrlProperties() {
  const form = useFormContext();

  return (
    <div className="space-y-4 mt-4">
      <div className="text-sm font-medium mb-2">连接设置</div>
      
      {/* URL */}
      <FormField
        control={form.control}
        name="properties.url"
        render={({ field }) => (
          <FormItem>
            <FormLabel className="required">URL地址</FormLabel>
            <FormControl>
              <Input
                placeholder="例如: http://example.com:8080"
                {...field}
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
    </div>
  );
}

/**
 * 认证属性表单组件
 * 用于配置用户名和密码
 */
export function AuthProperties() {
  const form = useFormContext();

  return (
    <div className="space-y-4 mt-4">
      <div className="text-sm font-medium mb-2">认证设置</div>
      
      {/* 用户名 */}
      <FormField
        control={form.control}
        name="properties.username"
        render={({ field }) => (
          <FormItem>
            <FormLabel>用户名</FormLabel>
            <FormControl>
              <Input
                placeholder="输入用户名"
                {...field}
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
      
      {/* 密码 */}
      <FormField
        control={form.control}
        name="properties.password"
        render={({ field }) => (
          <FormItem>
            <FormLabel>密码</FormLabel>
            <FormControl>
              <Input
                type="password"
                placeholder="输入密码"
                {...field}
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
    </div>
  );
} 