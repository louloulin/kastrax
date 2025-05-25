import { useFormContext } from "react-hook-form";
import { BaseProperties, AddressProperties, AuthProperties, TimeProperties } from "./base-properties";
import { Input } from "@/components/ui/input";
import { FormItem, FormLabel, FormControl, FormField, FormMessage, FormDescription } from "@/components/ui/form";
import { Textarea } from "@/components/ui/textarea";

/**
 * Modbus TCP属性表单组件
 * 用于Modbus TCP协议资源配置
 */
export default function ModbustcpProperties() {
  const form = useFormContext();

  return (
    <div className="space-y-6">
      {/* 基础属性 */}
      <BaseProperties />
      
      {/* 地址属性 */}
      <AddressProperties />
      
      {/* Modbus TCP特有属性 */}
      <div className="space-y-4 mt-4">
        <div className="text-sm font-medium mb-2">Modbus TCP设置</div>
        
        {/* 单元标识符 */}
        <FormField
          control={form.control}
          name="properties.unitId"
          render={({ field }) => (
            <FormItem>
              <FormLabel>单元标识符</FormLabel>
              <FormControl>
                <Input
                  placeholder="输入单元标识符，例如：1"
                  type="number"
                  {...field}
                  onChange={(e) => field.onChange(e.target.valueAsNumber || 1)}
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
                  placeholder="输入点位列表，每行一个，例如：HR,100,2;IR,200,1"
                  className="min-h-[100px]"
                  {...field}
                />
              </FormControl>
              <FormDescription>
                每行一个点位，格式为：功能码类型,地址,数量
                功能码类型：DO(线圈),DI(离散输入),HR(保持寄存器),IR(输入寄存器)
              </FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />
        
        {/* 字节序 */}
        <FormField
          control={form.control}
          name="properties.byteOrder"
          render={({ field }) => (
            <FormItem>
              <FormLabel>字节序</FormLabel>
              <FormControl>
                <select
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                  {...field}
                >
                  <option value="ABCD">ABCD (大端序)</option>
                  <option value="CDAB">CDAB (字节对换)</option>
                  <option value="BADC">BADC (字内字节对换)</option>
                  <option value="DCBA">DCBA (小端序)</option>
                </select>
              </FormControl>
              <FormDescription>
                多字节数据的字节顺序
              </FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />
        
        {/* 启用写操作 */}
        <FormField
          control={form.control}
          name="properties.address"
          render={({ field }) => (
            <FormItem>
              <FormLabel>数据地址</FormLabel>
              <FormControl>
                <Input
                  placeholder="输入写入数据的地址，例如：HR,100"
                  {...field}
                />
              </FormControl>
              <FormDescription>
                用于写操作的寄存器地址，格式为：功能码类型,地址
              </FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />
        
        <FormField
          control={form.control}
          name="properties.dataValue"
          render={({ field }) => (
            <FormItem>
              <FormLabel>数据值</FormLabel>
              <FormControl>
                <Input
                  placeholder="输入写入的数据值"
                  {...field}
                />
              </FormControl>
              <FormDescription>
                要写入的值，可以是单个值或多个值(用逗号分隔)
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