import React, { useEffect } from "react";
import { Routes, Route, Navigate, useNavigate, useLocation } from "react-router-dom";
import { ThemeProvider } from "./components/theme-provider";
import { AuthProvider } from "./lib/context/auth-context";
import { usePermissionStore } from "./lib/store/permission-store";
import { PermissionGuard } from "./components/permission-guard";
import NotFoundPage from "./pages/not-found/page";
import DashboardPage from "./dashboard/page";
import SqlPage from "./pages/sql-page";
import ResourcesPage from "./pages/resources/page";
import ResourceDetailPage from "./pages/resources/[id]/page";
import CreateResourcePage from "./pages/resources/create/page";
import DatabasePage from "./pages/database/page";
import TablesPage from "./pages/tables/page";
import UsersPage from "./pages/users/page";
import SettingsPage from "./pages/settings/page";
import ProfilePage from "./pages/profile/page";
import LoginPage from "./pages/login/page";
import DataImportExportPage from "./pages/data-import-export/page";
import DataVisualizationPage from "./pages/data-visualization/page";
import VariablesPage from "./pages/variables/page";
import ScriptsPage from "./pages/scripts/page";
import BackupsPage from "./pages/backups/page";
import RulesPage from "./pages/rules/page";
import RuleEditor from "./pages/rules/editor/[id]";
import LogsPage from "./pages/logs/page";
import JwtTestPage from "./pages/jwt-test";
import JwtTestFixedPage from "./pages/jwt-test-fixed";
import Loading from "./components/ui/loading";
import './index.css';

// 权限路由组件
function PermissionRoutes() {
  const { routes } = usePermissionStore();
  const navigate = useNavigate();
  const location = useLocation();

  // 当路由变化时检查权限
  useEffect(() => {
    console.log('[App] Current path:', location.pathname);
    console.log('[App] Available routes:', routes);
  }, [location.pathname, routes]);

  return (
    <Routes>
      {/* 登录页面 */}
      <Route path="/login" element={<LoginPage />} />

      {/* 仪表盘页面 - 需要 dashboard 权限 */}
      <Route path="/" element={
        <PermissionGuard permission="dashboard">
          <DashboardPage />
        </PermissionGuard>
      } />

      {/* 仪表盘路径重定向到根路径 */}
      <Route path="/dashboard" element={
        <Navigate to="/" replace />
      } />

      {/* 规则页面 - 需要 rule 权限 */}
      <Route path="/rules" element={
        <PermissionGuard permission="rule">
          <RulesPage />
        </PermissionGuard>
      } />
      <Route path="/rules/editor/:id" element={
        <PermissionGuard permission="rule">
          <React.Suspense fallback={<Loading />}><RuleEditor /></React.Suspense>
        </PermissionGuard>
      } />

      {/* 资源页面 - 需要 resource 权限 */}
      <Route path="/resources" element={
        <PermissionGuard permission="resource">
          <ResourcesPage />
        </PermissionGuard>
      } />
      <Route path="/resources/create" element={
        <PermissionGuard permission="resource">
          <CreateResourcePage />
        </PermissionGuard>
      } />
      <Route path="/resources/:id" element={
        <PermissionGuard permission="resource">
          <ResourceDetailPage />
        </PermissionGuard>
      } />

      {/* 脚本页面 - 需要 script 权限 */}
      <Route path="/scripts" element={
        <PermissionGuard permission="script">
          <ScriptsPage />
        </PermissionGuard>
      } />

      {/* 变量页面 - 需要 variable 权限 */}
      <Route path="/variables" element={
        <PermissionGuard permission="variable">
          <VariablesPage />
        </PermissionGuard>
      } />

      {/* 备份页面 - 需要 backup 权限 */}
      <Route path="/backups" element={
        <PermissionGuard permission="backup">
          <BackupsPage />
        </PermissionGuard>
      } />

      {/* 数据页面 - 需要 data 权限 */}
      <Route path="/data-import-export" element={
        <PermissionGuard permission="data">
          <DataImportExportPage />
        </PermissionGuard>
      } />
      <Route path="/data-visualization" element={
        <PermissionGuard permission="data">
          <DataVisualizationPage />
        </PermissionGuard>
      } />
      <Route path="/database" element={
        <PermissionGuard permission="data">
          <DatabasePage />
        </PermissionGuard>
      } />
      <Route path="/tables" element={
        <PermissionGuard permission="data">
          <TablesPage />
        </PermissionGuard>
      } />
      <Route path="/sql" element={
        <PermissionGuard permission="data">
          <SqlPage />
        </PermissionGuard>
      } />

      {/* 管理页面 - 需要 admin 权限 */}
      <Route path="/users" element={
        <PermissionGuard permission="admin">
          <UsersPage />
        </PermissionGuard>
      } />
      <Route path="/settings" element={
        <PermissionGuard permission="admin">
          <SettingsPage />
        </PermissionGuard>
      } />
      <Route path="/logs" element={
        <PermissionGuard permission="admin">
          <LogsPage />
        </PermissionGuard>
      } />

      {/* 个人资料页面 - 所有登录用户可访问 */}
      <Route path="/profile" element={<ProfilePage />} />

      {/* JWT 测试页面 */}
      <Route path="/jwt-test" element={<JwtTestPage />} />
      <Route path="/jwt-test-fixed" element={<JwtTestFixedPage />} />

      {/* 404 页面 */}
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}

export default function App() {
  return (
    <ThemeProvider defaultTheme="dark" attribute="class">
      <AuthProvider>
        <PermissionRoutes />
        <TailwindIndicator />
      </AuthProvider>
    </ThemeProvider>
  );
}

function TailwindIndicator() {
  if (import.meta.env.PROD) return null;

  return (
    <div className="fixed bottom-1 right-1 z-50 flex h-6 w-6 items-center justify-center rounded-full bg-gray-800 p-3 font-mono text-xs text-white">
      <div className="block sm:hidden">xs</div>
      <div className="hidden sm:block md:hidden">sm</div>
      <div className="hidden md:block lg:hidden">md</div>
      <div className="hidden lg:block xl:hidden">lg</div>
      <div className="hidden xl:block 2xl:hidden">xl</div>
      <div className="hidden 2xl:block">2xl</div>
    </div>
  );
}
