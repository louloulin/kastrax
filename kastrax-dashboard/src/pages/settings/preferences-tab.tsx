import React from "react";
import { useTheme } from "../../components/theme-provider";
import { Label } from "../../components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../../components/ui/select";
import { Switch } from "../../components/ui/switch";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "../../components/ui/card";
import { Laptop, Moon, Sun } from "lucide-react";

interface PreferencesTabProps {
  loading?: boolean;
}

export default function PreferencesTab({ loading = false }: PreferencesTabProps) {
  const { theme, setTheme } = useTheme();

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-xl">用户偏好设置</CardTitle>
        <CardDescription>
          自定义应用程序的外观和行为
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        <div className="space-y-2">
          <h3 className="text-lg font-medium">主题设置</h3>
          <p className="text-sm text-muted-foreground">
            选择应用程序的主题模式
          </p>
          <div className="grid gap-4 pt-2">
            <div className="grid grid-cols-3 gap-4">
              <div
                className={`flex flex-col items-center justify-between rounded-md border-2 p-4 cursor-pointer ${
                  theme === "light" ? "border-primary" : "border-border"
                }`}
                onClick={() => setTheme("light")}
              >
                <Sun className="h-6 w-6 mb-3" />
                <div className="space-y-1 text-center">
                  <h4 className="font-medium">浅色模式</h4>
                  <p className="text-xs text-muted-foreground">
                    明亮的配色方案
                  </p>
                </div>
              </div>
              <div
                className={`flex flex-col items-center justify-between rounded-md border-2 p-4 cursor-pointer ${
                  theme === "dark" ? "border-primary" : "border-border"
                }`}
                onClick={() => setTheme("dark")}
              >
                <Moon className="h-6 w-6 mb-3" />
                <div className="space-y-1 text-center">
                  <h4 className="font-medium">深色模式</h4>
                  <p className="text-xs text-muted-foreground">
                    暗黑的配色方案
                  </p>
                </div>
              </div>
              <div
                className={`flex flex-col items-center justify-between rounded-md border-2 p-4 cursor-pointer ${
                  theme === "system" ? "border-primary" : "border-border"
                }`}
                onClick={() => setTheme("system")}
              >
                <Laptop className="h-6 w-6 mb-3" />
                <div className="space-y-1 text-center">
                  <h4 className="font-medium">跟随系统</h4>
                  <p className="text-xs text-muted-foreground">
                    采用系统设置
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="space-y-2">
          <h3 className="text-lg font-medium">布局设置</h3>
          <p className="text-sm text-muted-foreground">
            自定义应用程序的布局
          </p>
          <div className="flex flex-col gap-2 pt-2">
            <div className="flex items-center justify-between">
              <div>
                <Label htmlFor="compact-mode">紧凑模式</Label>
                <p className="text-xs text-muted-foreground">
                  减小边距和内边距，显示更多内容
                </p>
              </div>
              <Switch
                id="compact-mode"
                disabled={loading}
              />
            </div>
            <div className="flex items-center justify-between">
              <div>
                <Label htmlFor="sticky-header">固定标题栏</Label>
                <p className="text-xs text-muted-foreground">
                  滚动时保持标题栏可见
                </p>
              </div>
              <Switch
                id="sticky-header"
                disabled={loading}
                defaultChecked
              />
            </div>
          </div>
        </div>

        <div className="space-y-2">
          <h3 className="text-lg font-medium">表格设置</h3>
          <p className="text-sm text-muted-foreground">
            自定义表格显示方式
          </p>
          <div className="flex items-center justify-between pt-2">
            <div>
              <Label htmlFor="page-size">默认分页大小</Label>
              <p className="text-xs text-muted-foreground">
                每页显示的行数
              </p>
            </div>
            <Select defaultValue="10" disabled={loading}>
              <SelectTrigger className="w-24">
                <SelectValue placeholder="选择" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="5">5 行</SelectItem>
                <SelectItem value="10">10 行</SelectItem>
                <SelectItem value="20">20 行</SelectItem>
                <SelectItem value="50">50 行</SelectItem>
                <SelectItem value="100">100 行</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>
      </CardContent>
    </Card>
  );
} 