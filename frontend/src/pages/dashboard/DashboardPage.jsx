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
import { SparklesIcon, ExclamationTriangleIcon } from '@heroicons/react/24/outline';
import { dashboardApi } from '../../api/dashboard';
import { assistantApi } from '../../api/assistant';
import useAuthStore from '../../store/authStore';
import LoadingSpinner from '../../components/ui/LoadingSpinner';
import AssistantPanel from '../../components/ui/AssistantPanel';
import AiChatInput from '../../components/ui/AiChatInput';
import {
  formatCurrency,
  formatRelativeTime,
} from '../../utils/helpers';

export default function DashboardPage() {
  const { user } = useAuthStore();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [aiBalanceExplanation, setAiBalanceExplanation] = useState(null);
  const [aiBalanceLoading, setAiBalanceLoading] = useState(false);
  const [aiBalanceError, setAiBalanceError] = useState(null);
  const [aiChartCaption, setAiChartCaption] = useState(null);
  const [aiChartLoading, setAiChartLoading] = useState(false);
  const [aiQnAResult, setAiQnAResult] = useState(null);
  const [aiQnALoading, setAiQnALoading] = useState(false);
  const [aiQnAError, setAiQnAError] = useState(null);
  const [coherenceFlags, setCoherenceFlags] = useState([]);
  const [coherenceLoading, setCoherenceLoading] = useState(false);
  const [whatIfResult, setWhatIfResult] = useState(null);
  const [whatIfLoading, setWhatIfLoading] = useState(false);
  const [whatIfCategories, setWhatIfCategories] = useState([]);

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

  const handleExplainBalance = async () => {
    try {
      setAiBalanceLoading(true);
      setAiBalanceError(null);
      const res = await assistantApi.explainBalance();
      setAiBalanceExplanation(res.data);
    } catch (err) {
      setAiBalanceError(
        err.response?.data?.message || err.response?.data?.error || 'Failed to get AI explanation'
      );
    } finally {
      setAiBalanceLoading(false);
    }
  };

  const handleChartCaption = async (chartKind, series) => {
    try {
      setAiChartLoading(true);
      const res = await assistantApi.chartCaption(chartKind, series);
      setAiChartCaption(res.data);
    } catch {
      // silent fail for caption
    } finally {
      setAiChartLoading(false);
    }
  };

  const handleExpenseQnA = async (question) => {
    try {
      setAiQnALoading(true);
      setAiQnAError(null);
      const chartSummary = {
        spendingByCategory,
        monthlySpending,
      };
      const res = await assistantApi.expenseQnA(question, undefined, undefined, chartSummary);
      setAiQnAResult(res.data);
    } catch (err) {
      setAiQnAError(err.response?.data?.message || err.response?.data?.error || 'Failed to get answer');
    } finally {
      setAiQnALoading(false);
    }
  };

  const handleCoherenceScan = async () => {
    try {
      setCoherenceLoading(true);
      const res = await assistantApi.coherenceScan();
      setCoherenceFlags(res.data || []);
    } catch {
      setCoherenceFlags([]);
    } finally {
      setCoherenceLoading(false);
    }
  };

  const handleWhatIf = async () => {
    if (whatIfCategories.length === 0) return;
    try {
      setWhatIfLoading(true);
      const res = await assistantApi.whatIf(whatIfCategories);
      setWhatIfResult(res.data);
    } catch {
      setWhatIfResult(null);
    } finally {
      setWhatIfLoading(false);
    }
  };

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

        {/* AI Balance Explanation */}
        <div className="mb-8">
          <AssistantPanel
            title="AI Balance Explanation"
            loading={aiBalanceLoading}
            error={aiBalanceError}
            narrative={aiBalanceExplanation?.narrative}
            citations={aiBalanceExplanation?.citations || []}
          />
          {!aiBalanceExplanation && !aiBalanceLoading && (
            <button
              onClick={handleExplainBalance}
              className="mt-2 text-sm text-purple-600 dark:text-purple-400 hover:text-purple-700 dark:hover:text-purple-300 flex items-center gap-1.5"
            >
              <SparklesIcon className="w-4 h-4" />
              Explain my balance with AI
            </button>
          )}
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

        {/* AI Expense Q&A */}
        <div className="mb-8 mt-8">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-3">
            Ask about your expenses
          </h2>
          <AiChatInput
            placeholder="e.g., How much did I spend on food this month?"
            onSubmit={handleExpenseQnA}
            loading={aiQnALoading}
          />
          {aiQnAResult && (
            <AssistantPanel
              title="Answer"
              narrative={aiQnAResult.narrative}
              citations={aiQnAResult.citations || []}
              defaultOpen={true}
              className="mt-3"
            />
          )}
          {aiQnAError && (
            <p className="mt-2 text-sm text-red-500">{aiQnAError}</p>
          )}
        </div>

        {/* Coherence Scan */}
        <div className="mb-8">
          <div className="flex items-center justify-between mb-3">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
              Spending Health Check
            </h2>
            <button
              onClick={handleCoherenceScan}
              disabled={coherenceLoading}
              className="text-sm text-purple-600 dark:text-purple-400 hover:text-purple-700 flex items-center gap-1.5"
            >
              {coherenceLoading ? (
                <div className="w-4 h-4 border-2 border-purple-400 border-t-transparent rounded-full animate-spin" />
              ) : (
                <SparklesIcon className="w-4 h-4" />
              )}
              Scan
            </button>
          </div>
          {coherenceFlags.length > 0 && (
            <div className="space-y-2">
              {coherenceFlags.map((flag, i) => (
                <div key={i} className="p-3 rounded-lg border border-amber-200 dark:border-amber-800 bg-amber-50 dark:bg-amber-950/30">
                  <div className="flex items-start gap-2">
                    <ExclamationTriangleIcon className="w-5 h-5 text-amber-500 shrink-0 mt-0.5" />
                    <div>
                      <p className="text-sm font-medium text-amber-800 dark:text-amber-200">
                        {flag.flagType === 'POSSIBLE_DUPLICATE' ? 'Possible Duplicate' : 'Category Mismatch'}
                      </p>
                      <p className="text-sm text-amber-700 dark:text-amber-300 mt-0.5">{flag.reason}</p>
                      <p className="text-xs text-amber-600 dark:text-amber-400 mt-1">
                        {flag.expense1Description}
                        {flag.expense2Description && ` / ${flag.expense2Description}`}
                      </p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
          {coherenceFlags.length === 0 && !coherenceLoading && (
            <p className="text-sm text-gray-500 dark:text-gray-400">Click Scan to check for issues.</p>
          )}
        </div>

        {/* What-If Explorer */}
        <div className="mb-8">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-3">
            What-If Explorer
          </h2>
          <p className="text-sm text-gray-500 dark:text-gray-400 mb-3">
            Select categories to exclude and see how your spending changes.
          </p>
          <div className="flex flex-wrap gap-2 mb-3">
            {['FOOD', 'TRANSPORT', 'RENT', 'UTILITIES', 'ENTERTAINMENT', 'SHOPPING', 'HEALTHCARE', 'EDUCATION', 'TRAVEL', 'GROCERIES', 'SUBSCRIPTIONS'].map((cat) => (
              <button
                key={cat}
                type="button"
                onClick={() =>
                  setWhatIfCategories((prev) =>
                    prev.includes(cat) ? prev.filter((c) => c !== cat) : [...prev, cat]
                  )
                }
                className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                  whatIfCategories.includes(cat)
                    ? 'bg-purple-600 text-white'
                    : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
                }`}
              >
                {cat}
              </button>
            ))}
          </div>
          <button
            onClick={handleWhatIf}
            disabled={whatIfCategories.length === 0 || whatIfLoading}
            className="btn-primary text-sm px-4 py-2 disabled:opacity-50"
          >
            {whatIfLoading ? 'Calculating...' : 'Calculate'}
          </button>
          {whatIfResult && (
            <AssistantPanel
              title="What-If Results"
              narrative={whatIfResult.narrative}
              defaultOpen={true}
              className="mt-3"
            >
              <div className="grid grid-cols-2 gap-3 mt-2 text-sm">
                <div className="p-2 rounded-lg bg-white/60 dark:bg-gray-800/60">
                  <p className="text-gray-500 dark:text-gray-400 text-xs">Original Total</p>
                  <p className="font-semibold text-gray-900 dark:text-gray-100">{formatCurrency(whatIfResult.originalTotal)}</p>
                </div>
                <div className="p-2 rounded-lg bg-white/60 dark:bg-gray-800/60">
                  <p className="text-gray-500 dark:text-gray-400 text-xs">After Excluding</p>
                  <p className="font-semibold text-gray-900 dark:text-gray-100">{formatCurrency(whatIfResult.adjustedTotal)}</p>
                </div>
              </div>
              <p className="text-sm text-emerald-600 dark:text-emerald-400 mt-2 font-medium">
                You would save {formatCurrency(whatIfResult.difference)}
              </p>
            </AssistantPanel>
          )}
        </div>
      </div>
    </div>
  );
}
