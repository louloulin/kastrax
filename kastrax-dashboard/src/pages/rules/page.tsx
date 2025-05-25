import React, { useEffect, useState } from 'react';
import SupabaseSidebar from "@/components/supabase-sidebar";
import TopNavigation from "@/components/top-navigation";
import { Button } from '@/components/ui/button';
import { PlusIcon, PlayIcon, Pencil1Icon, TrashIcon, ReloadIcon } from '@radix-ui/react-icons';
import { Menu } from 'lucide-react';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Label } from '@/components/ui/label';
import { useToast } from '@/components/ui/use-toast';
import { Badge } from '@/components/ui/badge';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { 
  getRuleList, 
  deleteRule,
  Rule, 
  RuleType, 
  RuleStatus
} from '@/lib/api/rules';
import { useNavigate } from 'react-router-dom';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { formatDate } from '@/lib/utils';
import RuleRunDialog from '@/components/rules/rule-run-dialog';

const RulesPage: React.FC = () => {
  const [rules, setRules] = useState<Rule[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchName, setSearchName] = useState('');
  const [selectedType, setSelectedType] = useState<string>('');
  const [selectedStatus, setSelectedStatus] = useState<string>('');
  const [runDialogOpen, setRunDialogOpen] = useState(false);
  const [selectedRule, setSelectedRule] = useState<Rule | null>(null);
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const { toast } = useToast();
  const navigate = useNavigate();

  const fetchRules = async () => {
    setLoading(true);
    try {
      const response = await getRuleList({
        ruleName: searchName || undefined,
        type: selectedType ? selectedType as RuleType : undefined,
        status: selectedStatus ? selectedStatus as RuleStatus : undefined,
      });
      
      const responseData = response.data.data as Rule[] | { list: Rule[], total: number };
      if (Array.isArray(responseData)) {
        setRules(responseData);
      } else if (responseData && 'list' in responseData) {
        setRules(responseData.list);
      } else {
        setRules([]);
      }
    } catch (error) {
      console.error('Failed to fetch rules:', error);
      toast({
        title: '获取规则列表失败',
        description: '请稍后重试',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRules();
  }, []);

  const handleSearch = () => {
    fetchRules();
  };

  const handleCreateRule = () => {
    navigate('/rules/editor/new');
  };

  const handleEditRule = (rule: Rule) => {
    navigate(`/rules/editor/${rule.ruleId}`);
  };

  const handleRunRule = (rule: Rule) => {
    setSelectedRule(rule);
    setRunDialogOpen(true);
  };

  const handleDeleteRule = async (rule: Rule) => {
    try {
      await deleteRule(rule.ruleId);
      toast({
        title: '删除成功',
        description: `规则 "${rule.ruleName}" 已删除`,
      });
      fetchRules();
    } catch (error) {
      console.error('Failed to delete rule:', error);
      toast({
        title: '删除失败',
        description: '请稍后重试',
        variant: 'destructive',
      });
    }
  };

  const getStatusBadge = (status: RuleStatus) => {
    const statusConfig = {
      [RuleStatus.DRAFT]: { label: '草稿', variant: 'secondary' },
      [RuleStatus.ACTIVE]: { label: '启用', variant: 'success' },
      [RuleStatus.INACTIVE]: { label: '禁用', variant: 'default' },
      [RuleStatus.DEPRECATED]: { label: '已弃用', variant: 'destructive' },
    };

    const config = statusConfig[status] || { label: status, variant: 'default' };
    
    return (
      <Badge variant={config.variant as any}>
        {config.label}
      </Badge>
    );
  };

  const getTypeLabel = (type: RuleType) => {
    const typeConfig = {
      [RuleType.CONDITIONAL]: '条件规则',
      [RuleType.SEQUENCE]: '顺序规则',
      [RuleType.PARALLEL]: '并行规则',
      [RuleType.TRANSFORM]: '转换规则',
    };

    return typeConfig[type] || type;
  };

  return (
    <div className="flex min-h-screen bg-background">
      <SupabaseSidebar collapsed={sidebarCollapsed} setCollapsed={setSidebarCollapsed} />
      
      <div className="flex-1 flex flex-col">
        <TopNavigation>
          <Button 
            variant="ghost" 
            size="sm" 
            onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
            className="md:hidden"
          >
            <Menu size={16} />
          </Button>
        </TopNavigation>
        
        <div className="flex-1 p-6 overflow-auto">
          <div className="flex justify-between items-center mb-6">
            <h1 className="text-2xl font-bold">规则管理</h1>
            <Button onClick={handleCreateRule}>
              <PlusIcon className="mr-2 h-4 w-4" />
              创建规则
            </Button>
          </div>

          <div className="mb-6 bg-card p-4 rounded-lg">
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              <div className="flex-1 min-w-[200px]">
                <Label htmlFor="search-name" className="mb-2">
                  规则名称
                </Label>
                <Input
                  id="search-name"
                  placeholder="输入规则名称"
                  value={searchName}
                  onChange={(e) => setSearchName(e.target.value)}
                />
              </div>
              <div className="w-[200px]">
                <Label htmlFor="rule-type" className="mb-2">
                  规则类型
                </Label>
                <Select
                  value={selectedType}
                  onValueChange={setSelectedType}
                >
                  <SelectTrigger id="rule-type">
                    <SelectValue placeholder="全部类型" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="">全部类型</SelectItem>
                    <SelectItem value={RuleType.CONDITIONAL}>条件规则</SelectItem>
                    <SelectItem value={RuleType.SEQUENCE}>顺序规则</SelectItem>
                    <SelectItem value={RuleType.PARALLEL}>并行规则</SelectItem>
                    <SelectItem value={RuleType.TRANSFORM}>转换规则</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="w-[200px]">
                <Label htmlFor="rule-status" className="mb-2">
                  规则状态
                </Label>
                <Select
                  value={selectedStatus}
                  onValueChange={setSelectedStatus}
                >
                  <SelectTrigger id="rule-status">
                    <SelectValue placeholder="全部状态" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="">全部状态</SelectItem>
                    <SelectItem value={RuleStatus.DRAFT}>草稿</SelectItem>
                    <SelectItem value={RuleStatus.ACTIVE}>启用</SelectItem>
                    <SelectItem value={RuleStatus.INACTIVE}>禁用</SelectItem>
                    <SelectItem value={RuleStatus.DEPRECATED}>已弃用</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="flex items-end">
                <Button variant="default" onClick={handleSearch}>
                  搜索
                </Button>
              </div>
            </div>
          </div>

          <div className="bg-card rounded-lg shadow-sm overflow-hidden">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>规则名称</TableHead>
                  <TableHead>规则类型</TableHead>
                  <TableHead>状态</TableHead>
                  <TableHead>版本</TableHead>
                  <TableHead>创建时间</TableHead>
                  <TableHead>更新时间</TableHead>
                  <TableHead className="text-right">操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {loading ? (
                  <TableRow>
                    <TableCell colSpan={7} className="text-center py-10">
                      <div className="flex justify-center items-center">
                        <ReloadIcon className="mr-2 h-4 w-4 animate-spin" />
                        加载中...
                      </div>
                    </TableCell>
                  </TableRow>
                ) : rules.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={7} className="text-center py-10">
                      没有找到规则
                    </TableCell>
                  </TableRow>
                ) : (
                  rules.map((rule) => (
                    <TableRow key={rule.ruleId}>
                      <TableCell className="font-medium">{rule.ruleName}</TableCell>
                      <TableCell>{getTypeLabel(rule.type)}</TableCell>
                      <TableCell>{getStatusBadge(rule.status)}</TableCell>
                      <TableCell>v{rule.version}</TableCell>
                      <TableCell>{formatDate(rule.createTime)}</TableCell>
                      <TableCell>{formatDate(rule.updateTime)}</TableCell>
                      <TableCell className="text-right">
                        <div className="flex justify-end gap-2">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleRunRule(rule)}
                          >
                            <PlayIcon className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleEditRule(rule)}
                          >
                            <Pencil1Icon className="h-4 w-4" />
                          </Button>
                          <ConfirmDialog
                            title="确认删除"
                            description={`确定要删除规则"${rule.ruleName}"吗？此操作不可撤销。`}
                            onConfirm={() => handleDeleteRule(rule)}
                          >
                            <Button variant="outline" size="sm">
                              <TrashIcon className="h-4 w-4" />
                            </Button>
                          </ConfirmDialog>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </div>

          {selectedRule && (
            <RuleRunDialog
              rule={selectedRule}
              open={runDialogOpen}
              onClose={() => setRunDialogOpen(false)}
            />
          )}
        </div>
      </div>
    </div>
  );
};

export default RulesPage; 