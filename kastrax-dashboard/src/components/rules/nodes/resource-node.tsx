import { useCallback, useEffect, useState, memo } from 'react';
import { Handle, Position } from 'reactflow';
import { Database, PencilLine } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { Label } from '@/components/ui/label';
import { Loader2 } from 'lucide-react';
import { getResourceById, getResourceList, Resource } from '@/lib/api/resources';

interface ResourceNodeProps {
  id: string;
  data: {
    label?: string;
    config?: {
      resourceId?: string;
      queryType?: 'sql' | 'api';
      query?: string;
    };
    onConfigure?: (id: string) => void;
  };
  selected: boolean;
}

const ResourceNode = memo(({ id, data, selected }: ResourceNodeProps) => {
  const [open, setOpen] = useState(false);
  const [resourceId, setResourceId] = useState(data.config?.resourceId || '');
  const [queryType, setQueryType] = useState<'sql' | 'api'>(data.config?.queryType || 'sql');
  const [query, setQuery] = useState(data.config?.query || '');
  const [resources, setResources] = useState<Resource[]>([]);
  const [loading, setLoading] = useState(false);
  const [resource, setResource] = useState<Resource | null>(null);

  // Fetch resources when dialog is opened
  useEffect(() => {
    if (open) {
      setLoading(true);
      getResourceList({})
        .then(response => {
          if (response.data && response.data.data) {
            setResources(response.data.data);
          }
        })
        .catch(error => {
          console.error('Error fetching resources:', error);
        })
        .finally(() => {
          setLoading(false);
        });
    }
  }, [open]);

  // Fetch resource details when resourceId changes
  useEffect(() => {
    if (resourceId) {
      setLoading(true);
      getResourceById(resourceId)
        .then(response => {
          if (response.data && response.data.data) {
            const resourceData = response.data.data;
            setResource(resourceData);
            // Default query type based on resource type
            if (resourceData.resourceType.toLowerCase().includes('database')) {
              setQueryType('sql');
            } else {
              setQueryType('api');
            }
          }
        })
        .catch(error => {
          console.error('Error fetching resource details:', error);
        })
        .finally(() => {
          setLoading(false);
        });
    }
  }, [resourceId]);

  const handleSaveConfig = useCallback(() => {
    // Here we would typically save the configuration to the node data
    if (!data.config) {
      data.config = {};
    }
    data.config.resourceId = resourceId;
    data.config.queryType = queryType;
    data.config.query = query;
    setOpen(false);
  }, [data, resourceId, queryType, query]);

  const handleOpenConfig = useCallback(() => {
    setOpen(true);
  }, []);

  const handleNodeClick = useCallback(() => {
    if (data.onConfigure) {
      data.onConfigure(id);
    }
  }, [data, id]);

  const handleEditButtonClick = useCallback((e: React.MouseEvent) => {
    e.stopPropagation();
    handleOpenConfig();
  }, [handleOpenConfig]);

  const isDatabaseResource = useCallback(() => {
    return resource?.resourceType.toLowerCase().includes('database');
  }, [resource]);

  return (
    <>
      <div
        className={`rounded-md border-2 ${selected ? 'border-primary' : 'border-border'} bg-background p-3 shadow-md cursor-pointer`}
        onClick={handleNodeClick}
      >
        <div className="flex items-center justify-between mb-2">
          <div className="flex items-center gap-2">
            <Database className="h-4 w-4 text-cyan-500" />
            <span className="font-medium">{data.label || '连接资源'}</span>
          </div>
          <Button
            size="icon"
            variant="ghost"
            className="h-6 w-6"
            onClick={handleEditButtonClick}
          >
            <PencilLine className="h-4 w-4" />
          </Button>
        </div>
        
        <div className="text-xs text-muted-foreground">
          {data.config?.resourceId ? (
            <div className="space-y-1">
              <div><span className="font-medium">资源:</span> {resource?.resourceName || data.config.resourceId}</div>
              {data.config.queryType === 'sql' ? (
                <div><span className="font-medium">SQL 查询:</span> {data.config.query ? `${data.config.query.substring(0, 30)}${data.config.query.length > 30 ? '...' : ''}` : '未配置'}</div>
              ) : (
                <div><span className="font-medium">API 路径:</span> {data.config.query || '未配置'}</div>
              )}
            </div>
          ) : (
            <span>点击配置资源节点</span>
          )}
        </div>

        <Handle
          type="target"
          position={Position.Left}
          id="in"
          className="w-3 h-3 rounded-full border-2 border-primary bg-background"
        />
        <Handle
          type="source"
          position={Position.Right}
          id="out"
          className="w-3 h-3 rounded-full border-2 border-primary bg-background"
        />
      </div>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="sm:max-w-[425px]">
          <DialogHeader>
            <DialogTitle>配置资源节点</DialogTitle>
            <DialogDescription>
              选择要连接的资源并配置查询或API路径
            </DialogDescription>
          </DialogHeader>

          <div className="grid gap-4 py-4">
            <div className="grid grid-cols-4 items-center gap-4">
              <Label htmlFor="resource" className="text-right">
                资源
              </Label>
              <Select
                value={resourceId}
                onValueChange={(value) => setResourceId(value)}
                disabled={loading}
              >
                <SelectTrigger id="resource" className="col-span-3">
                  <SelectValue placeholder="选择资源" />
                </SelectTrigger>
                <SelectContent>
                  {resources.map((resource) => (
                    <SelectItem key={resource.resourceId} value={resource.resourceId}>
                      {resource.resourceName} ({resource.resourceType})
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {resource && (
              <>
                <div className="grid grid-cols-4 items-center gap-4">
                  <Label htmlFor="queryType" className="text-right">
                    查询类型
                  </Label>
                  <Select
                    value={queryType}
                    onValueChange={(value: 'sql' | 'api') => setQueryType(value)}
                    disabled={loading || !isDatabaseResource()}
                  >
                    <SelectTrigger id="queryType" className="col-span-3">
                      <SelectValue placeholder="选择查询类型" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="sql" disabled={!isDatabaseResource()}>
                        SQL 查询
                      </SelectItem>
                      <SelectItem value="api">
                        API 路径
                      </SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                <div className="grid grid-cols-4 items-center gap-4">
                  <Label htmlFor="query" className="text-right">
                    {queryType === 'sql' ? 'SQL 查询' : 'API 路径'}
                  </Label>
                  <Textarea
                    id="query"
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    className="col-span-3"
                    placeholder={queryType === 'sql' ? 'SELECT * FROM table' : '/api/endpoint'}
                    rows={4}
                  />
                </div>
              </>
            )}

            {loading && (
              <div className="flex justify-center py-2">
                <Loader2 className="h-6 w-6 animate-spin text-primary" />
              </div>
            )}
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setOpen(false)}>
              取消
            </Button>
            <Button onClick={handleSaveConfig} disabled={loading || !resourceId}>
              保存
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
});

export default ResourceNode; 