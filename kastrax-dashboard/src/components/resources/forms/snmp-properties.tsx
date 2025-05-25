import { useFormContext } from "react-hook-form";
import { BaseProperties, AddressProperties, AuthProperties, TimeProperties } from "./base-properties";
import { Input } from "@/components/ui/input";
import { FormItem, FormLabel, FormControl, FormField, FormMessage, FormDescription } from "@/components/ui/form";
import { Textarea } from "@/components/ui/textarea";

/**
 * SNMP属性表单组件
 * 用于SNMP协议资源配置
 */
export default function SnmpProperties() {
  const form = useFormContext();

  return (
    <div className="space-y-6">
      {/* 基础属性 */}
      <BaseProperties />
      
      {/* 地址属性 */}
      <AddressProperties />
      
      {/* SNMP特有属性 */}
      <div className="space-y-4 mt-4">
        <div className="text-sm font-medium mb-2">SNMP设置</div>
        
        {/* SNMP版本 */}
        <FormField
          control={form.control}
          name="properties.version"
          render={({ field }) => (
            <FormItem>
              <FormLabel>SNMP版本</FormLabel>
              <FormControl>
                <select
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                  {...field}
                >
                  <option value="v1">SNMP v1</option>
                  <option value="v2c">SNMP v2c</option>
                  <option value="v3">SNMP v3</option>
                </select>
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        
        {/* 团体名 */}
        {(form.watch('properties.version') === 'v1' || form.watch('properties.version') === 'v2c') && (
          <FormField
            control={form.control}
            name="properties.community"
            render={({ field }) => (
              <FormItem>
                <FormLabel>团体名</FormLabel>
                <FormControl>
                  <Input
                    placeholder="输入团体名，例如：public"
                    {...field}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        )}
        
        {/* SNMPv3 安全级别 */}
        {form.watch('properties.version') === 'v3' && (
          <>
            <FormField
              control={form.control}
              name="properties.securityLevel"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>安全级别</FormLabel>
                  <FormControl>
                    <select
                      className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                      {...field}
                    >
                      <option value="noAuthNoPriv">无认证无加密</option>
                      <option value="authNoPriv">认证无加密</option>
                      <option value="authPriv">认证加密</option>
                    </select>
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            
            <FormField
              control={form.control}
              name="properties.securityName"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>安全名称</FormLabel>
                  <FormControl>
                    <Input
                      placeholder="输入安全名称"
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            
            {form.watch('properties.securityLevel') !== 'noAuthNoPriv' && (
              <>
                <FormField
                  control={form.control}
                  name="properties.authProtocol"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>认证协议</FormLabel>
                      <FormControl>
                        <select
                          className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                          {...field}
                        >
                          <option value="MD5">MD5</option>
                          <option value="SHA">SHA</option>
                        </select>
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                
                <FormField
                  control={form.control}
                  name="properties.authPassword"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>认证密码</FormLabel>
                      <FormControl>
                        <Input
                          type="password"
                          placeholder="输入认证密码"
                          {...field}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </>
            )}
            
            {form.watch('properties.securityLevel') === 'authPriv' && (
              <>
                <FormField
                  control={form.control}
                  name="properties.privProtocol"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>加密协议</FormLabel>
                      <FormControl>
                        <select
                          className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                          {...field}
                        >
                          <option value="DES">DES</option>
                          <option value="AES">AES</option>
                        </select>
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                
                <FormField
                  control={form.control}
                  name="properties.privPassword"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>加密密码</FormLabel>
                      <FormControl>
                        <Input
                          type="password"
                          placeholder="输入加密密码"
                          {...field}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </>
            )}
          </>
        )}
        
        {/* 点位列表 */}
        <FormField
          control={form.control}
          name="properties.points"
          render={({ field }) => (
            <FormItem>
              <FormLabel>OID列表</FormLabel>
              <FormControl>
                <Textarea
                  placeholder="输入OID列表，每行一个，例如：1.3.6.1.2.1.1.1.0"
                  className="min-h-[100px]"
                  {...field}
                />
              </FormControl>
              <FormDescription>
                每行一个OID，格式为：1.3.6.1.2.1.1.1.0
              </FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />
      </div>
      
      {/* 时间属性 */}
      <TimeProperties />
    </div>
  );
} 