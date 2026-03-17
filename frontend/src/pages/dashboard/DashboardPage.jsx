import { useEffect, useState } from 'react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  LineChart,
  Line,
} from 'recharts';
import { dashboardApi } from '../../api/dashboard';
import useAuthStore from '../../store/authStore';
import LoadingSpinner from '../../components/ui/LoadingSpinner';
import {
  formatCurrency,
  formatRelativeTime,
} from '../../utils/helpers';

export default function DashboardPage() {
  const { user } = useAuthStore();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        const response = await dashboardApi.getDashboard();
        setData(response.data);
      } catch {
        setData({
          balances: { youOwed: 0, youAreOwed: 0, net: 0 },
          friendBalances: [],
          groupBalances: [],
          recentActivity: [],
          spendingByCategory: [],
          monthlySpending: [],
        });
      } finally {
        setLoading(false);
      }
    };
    fetchDashboard();
  }, []);

  if (loading) {
    return <LoadingSpinner />;
  }

  const totalOwed = data?.totalOwed ?? data?.balances?.youAreOwed ?? 0;
  const totalOwing = data?.totalOwing ?? data?.balances?.youOwed ?? 0;
  const netBalance = data?.netBalance ?? data?.balances?.net ?? 0;
  const friendBalances = data?.friendBalances || [];
  const groupBalances = data?.groupBalances || [];
  const recentActivity = (data?.recentActivity || []).slice(0, 10);

  const rawCategorySpending = data?.categorySpending ?? data?.spendingByCategory ?? {};
  const spendingByCategory = Array.isArray(rawCategorySpending)
    ? rawCategorySpending
    : Object.entries(rawCategorySpending).map(([category, amount]) => ({ category, amount }));

  const rawMonthlySpending = data?.monthlySpending ?? {};
  const monthlySpending = Array.isArray(rawMonthlySpending)
    ? rawMonthlySpending
    : Object.entries(rawMonthlySpending).map(([month, amount]) => ({ month, amount }));

  const userName = user?.name?.split(' ')[0] || user?.email?.split('@')[0] || 'there';

  return (
    <div className="min-h-screen">
      <div className="max-w-6xl mx-auto px-4 py-8">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100 mb-8">
          Welcome back, {userName}!
        </h1>

        {/* Summary Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
          <div className="card border-l-4 border-emerald-500">
            <p className="text-sm font-medium text-gray-500 dark:text-gray-400 mb-1">You are owed</p>
            <p className="text-xl font-bold text-emerald-600 dark:text-emerald-400">
              {formatCurrency(totalOwed)}
            </p>
          </div>
          <div className="card border-l-4 border-red-500">
            <p className="text-sm font-medium text-gray-500 dark:text-gray-400 mb-1">You owe</p>
            <p className="text-xl font-bold text-red-600 dark:text-red-400">
              {formatCurrency(totalOwing)}
            </p>
          </div>
          <div className="card border-l-4 border-slate-500">
            <p className="text-sm font-medium text-gray-500 dark:text-gray-400 mb-1">Net balance</p>
            <p className={`text-xl font-bold ${netBalance >= 0 ? 'text-emerald-600 dark:text-emerald-400' : 'text-red-600 dark:text-red-400'}`}>
              {formatCurrency(netBalance)}
            </p>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          {/* Friend Balances */}
          <div className="card">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">Friend Balances</h2>
            {friendBalances.length === 0 ? (
              <p className="text-gray-500 dark:text-gray-400 text-sm">No friend balances yet</p>
            ) : (
              <ul className="space-y-3">
                {friendBalances.map((friend, i) => {
                  const name = friend.friendName || friend.user?.name || friend.name || 'Unknown';
                  const amt = friend.amount ?? 0;
                  return (
                    <li
                      key={friend.friendId || friend.user?.id || friend.id || i}
                      className="flex items-center justify-between py-2 border-b border-gray-100 dark:border-gray-700 last:border-0"
                    >
                      <span className="font-medium text-gray-800 dark:text-gray-200">
                        {name}
                      </span>
                      <span
                        className={`font-semibold ${
                          amt >= 0 ? 'text-emerald-600 dark:text-emerald-400' : 'text-red-600 dark:text-red-400'
                        }`}
                      >
                        {amt >= 0 ? 'You are owed ' : 'You owe '}
                        {formatCurrency(Math.abs(amt))}
                      </span>
                    </li>
                  );
                })}
              </ul>
            )}
          </div>

          {/* Group Balances */}
          <div className="card">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">Group Balances</h2>
            {groupBalances.length === 0 ? (
              <p className="text-gray-500 dark:text-gray-400 text-sm">No groups yet</p>
            ) : (
              <div className="space-y-3">
                {groupBalances.map((group, i) => (
                  <div
                    key={group.groupId || group.id || i}
                    className="p-3 rounded-lg bg-gray-50 dark:bg-gray-700/50 border border-gray-100 dark:border-gray-600"
                  >
                    <p className="font-medium text-gray-800 dark:text-gray-200">
                      {group.groupName || group.name || 'Group'}
                    </p>
                    <p
                      className={`text-sm font-semibold mt-1 ${
                        (group.balance ?? 0) >= 0 ? 'text-emerald-600 dark:text-emerald-400' : 'text-red-600 dark:text-red-400'
                      }`}
                    >
                      {(group.balance ?? 0) >= 0 ? '+' : ''}{formatCurrency(group.balance ?? 0)}
                    </p>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Recent Activity */}
        <div className="card mt-8">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">Recent Activity</h2>
          {recentActivity.length === 0 ? (
            <p className="text-gray-500 dark:text-gray-400 text-sm">No recent activity</p>
          ) : (
            <ul className="space-y-3">
              {recentActivity.map((activity, i) => {
                const activityType = (activity.type || '').toString().toLowerCase();
                const icon = activityType.includes('expense') ? '💰'
                  : activityType.includes('settlement') ? '✅'
                  : activityType.includes('group') ? '👥'
                  : '📋';
                return (
                  <li
                    key={activity.id || i}
                    className="flex items-center gap-3 py-2 border-b border-gray-100 dark:border-gray-700 last:border-0"
                  >
                    <span className="text-xl">{icon}</span>
                    <div className="flex-1 min-w-0">
                      <p className="font-medium text-gray-800 dark:text-gray-200 truncate">
                        {activity.description || activity.title || 'Activity'}
                      </p>
                      <p className="text-xs text-gray-500 dark:text-gray-400">
                        {activity.performedBy?.name && (
                          <span className="font-medium">{activity.performedBy.name} · </span>
                        )}
                        {formatRelativeTime(activity.createdAt || activity.date)}
                      </p>
                    </div>
                  </li>
                );
              })}
            </ul>
          )}
        </div>

        {/* Spending by Category */}
        {spendingByCategory.length > 0 && (
          <div className="card mt-8">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">Spending by Category</h2>
            <div className="h-64">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart
                  data={spendingByCategory.map((c) => ({
                    name: c.category || c.name || 'Other',
                    amount: Number(c.amount ?? c.total ?? 0),
                  }))}
                  layout="vertical"
                  margin={{ top: 5, right: 30, left: 80, bottom: 5 }}
                >
                  <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                  <XAxis type="number" tickFormatter={(v) => `₹${v}`} />
                  <YAxis type="category" dataKey="name" width={70} />
                  <Tooltip formatter={(v) => [formatCurrency(v), 'Amount']} />
                  <Bar dataKey="amount" fill="var(--color-primary)" radius={[0, 4, 4, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        )}

        {/* Monthly Spending */}
        {monthlySpending.length > 0 && (
          <div className="card mt-8">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">Monthly Spending</h2>
            <div className="h-64">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart
                  data={monthlySpending.map((m) => ({
                    month: m.month || m.label || m.date,
                    amount: Number(m.amount ?? m.total ?? 0),
                  }))}
                  margin={{ top: 5, right: 30, left: 0, bottom: 5 }}
                >
                  <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                  <XAxis dataKey="month" />
                  <YAxis tickFormatter={(v) => `₹${v}`} />
                  <Tooltip formatter={(v) => [formatCurrency(v), 'Spent']} />
                  <Line
                    type="monotone"
                    dataKey="amount"
                    stroke="var(--color-primary)"
                    strokeWidth={2}
                    dot={{ fill: 'var(--color-primary)' }}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
