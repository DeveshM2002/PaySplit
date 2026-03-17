/**
 * App.jsx — The root component that defines all routes.
 *
 * WHY React Router v6 (not v5)?
 * - v6 uses <Routes> instead of <Switch> (more intuitive)
 * - Nested routes with <Outlet> (cleaner layout patterns)
 * - Relative paths (no need to repeat parent path in child routes)
 * - Built-in route ranking (no more exact prop needed)
 *
 * ROUTE STRUCTURE:
 * /login, /signup → Public routes (no auth required)
 * Everything else → Wrapped in ProtectedRoute + Layout
 *   / → Dashboard
 *   /groups → Groups list
 *   /groups/create → Create group form
 *   /groups/:id → Group detail with expenses/balances
 *   /expenses/add → Add expense form
 *   /expenses/:id → Expense detail
 *   /settle → Record a settlement
 *   /activity → Activity feed
 */
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useEffect } from 'react';

import Layout from './components/layout/Layout';
import ProtectedRoute from './components/ui/ProtectedRoute';

import LoginPage from './pages/auth/LoginPage';
import SignupPage from './pages/auth/SignupPage';
import DashboardPage from './pages/dashboard/DashboardPage';
import GroupsListPage from './pages/groups/GroupsListPage';
import GroupDetailPage from './pages/groups/GroupDetailPage';
import CreateGroupPage from './pages/groups/CreateGroupPage';
import ExpensesListPage from './pages/expenses/ExpensesListPage';
import AddExpensePage from './pages/expenses/AddExpensePage';
import ExpenseDetailPage from './pages/expenses/ExpenseDetailPage';
import SettlePage from './pages/settlements/SettlePage';
import ActivityPage from './pages/activity/ActivityPage';

import useThemeStore from './store/themeStore';
import useAuthStore from './store/authStore';

function App() {
  const initTheme = useThemeStore((state) => state.initTheme);
  const initAuth = useAuthStore((state) => state.initAuth);
  const logout = useAuthStore((state) => state.logout);

  useEffect(() => {
    initTheme();
    initAuth();
  }, [initTheme, initAuth]);

  useEffect(() => {
    const handleAuthExpired = () => logout();
    window.addEventListener('auth-expired', handleAuthExpired);
    return () => window.removeEventListener('auth-expired', handleAuthExpired);
  }, [logout]);

  return (
    <BrowserRouter>
      <Routes>
        {/* Public routes — accessible without login */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />

        {/* Protected routes — require authentication */}
        <Route element={<ProtectedRoute />}>
          <Route element={<Layout />}>
            <Route path="/" element={<DashboardPage />} />
            <Route path="/groups" element={<GroupsListPage />} />
            <Route path="/groups/create" element={<CreateGroupPage />} />
            <Route path="/groups/:id" element={<GroupDetailPage />} />
            <Route path="/expenses" element={<ExpensesListPage />} />
            <Route path="/expenses/add" element={<AddExpensePage />} />
            <Route path="/expenses/:id" element={<ExpenseDetailPage />} />
            <Route path="/settle" element={<SettlePage />} />
            <Route path="/activity" element={<ActivityPage />} />
          </Route>
        </Route>

        {/* Catch-all: redirect unknown routes to dashboard */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
