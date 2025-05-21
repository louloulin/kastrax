import React, { useState, useEffect } from "react";
import { Button } from "../../components/ui/button";
import { Checkbox } from "../../components/ui/checkbox";
import { Loader2 } from "lucide-react";
import { getUserPermissions, updateUserPermissions, User } from "../../lib/api/auth";

// Available permissions in the system
const availablePermissions = [
  { value: "dashboard", label: "仪表盘" },
  { value: "rule", label: "规则管理" },
  { value: "resource", label: "资源管理" },
  { value: "script", label: "脚本管理" },
  { value: "variable", label: "变量管理" },
  { value: "backup", label: "备份管理" },
  { value: "data", label: "数据管理" },
  { value: "admin", label: "系统管理" },
  { value: "all", label: "全部权限" }
];

interface PermissionFormProps {
  user: User;
  onSuccess: () => void;
  onCancel: () => void;
}

export default function PermissionForm({ user, onSuccess, onCancel }: PermissionFormProps) {
  const [loading, setLoading] = useState(false);
  const [loadingPermissions, setLoadingPermissions] = useState(false);
  const [selectedPermissions, setSelectedPermissions] = useState<string[]>([]);

  // Load user permissions
  useEffect(() => {
    const fetchPermissions = async () => {
      if (!user.username) return;
      
      setLoadingPermissions(true);
      try {
        const response = await getUserPermissions(user.username);
        if (response.data && response.data.success) {
          setSelectedPermissions(response.data.data || []);
        }
      } catch (error) {
        console.error("Failed to fetch user permissions:", error);
      } finally {
        setLoadingPermissions(false);
      }
    };

    fetchPermissions();
  }, [user.username]);

  // Handle permission toggle
  const handlePermissionToggle = (permission: string) => {
    setSelectedPermissions(prev => {
      // If selecting "all", clear other permissions
      if (permission === "all") {
        return prev.includes("all") ? [] : ["all"];
      }
      
      // If another permission is selected while "all" is active, remove "all"
      let newPermissions = [...prev];
      if (prev.includes("all") && permission !== "all") {
        newPermissions = newPermissions.filter(p => p !== "all");
      }
      
      // Toggle the selected permission
      if (newPermissions.includes(permission)) {
        return newPermissions.filter(p => p !== permission);
      } else {
        return [...newPermissions, permission];
      }
    });
  };

  // Handle form submission
  const handleSubmit = async () => {
    setLoading(true);
    
    try {
      await updateUserPermissions(user.username, selectedPermissions);
      onSuccess();
    } catch (error) {
      console.error("Failed to update permissions:", error);
      alert("更新权限失败");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-4">
      {loadingPermissions ? (
        <div className="flex justify-center py-4">
          <Loader2 className="h-6 w-6 animate-spin" />
        </div>
      ) : (
        <>
          <div className="space-y-4">
            <div className="text-sm text-muted-foreground mb-2">
              选择用户 <span className="font-medium">{user.username}</span> 的权限:
            </div>
            
            {availablePermissions.map(permission => (
              <div key={permission.value} className="flex items-center space-x-2">
                <Checkbox
                  id={`permission-${permission.value}`}
                  checked={selectedPermissions.includes(permission.value)}
                  onCheckedChange={() => handlePermissionToggle(permission.value)}
                  disabled={user.system && permission.value === "all"} // System users always have "all" permission
                />
                <label
                  htmlFor={`permission-${permission.value}`}
                  className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70"
                >
                  {permission.label}
                </label>
              </div>
            ))}
          </div>

          <div className="flex justify-end space-x-2 pt-4">
            <Button 
              type="button" 
              variant="outline" 
              onClick={onCancel}
            >
              取消
            </Button>
            <Button 
              onClick={handleSubmit}
              disabled={loading || (user.system && selectedPermissions.includes("all"))}
            >
              {loading ? (
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              ) : null}
              保存权限
            </Button>
          </div>
        </>
      )}
    </div>
  );
}
