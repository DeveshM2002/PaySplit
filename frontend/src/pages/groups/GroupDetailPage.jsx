import { useState, useEffect, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  UserPlusIcon,
  PlusIcon,
} from '@heroicons/react/24/outline';
import {
  BanknotesIcon,
  ArrowPathIcon,
  ChatBubbleLeftRightIcon,
  BoltIcon,
} from '@heroicons/react/24/outline';
import { groupApi } from '../../api/groups';
import { expenseApi } from '../../api/expenses';
import { settlementApi } from '../../api/settlements';
import { dashboardApi } from '../../api/dashboard';
import { userApi } from '../../api/users';
import useAuthStore from '../../store/authStore';
import {
  formatCurrency,
  formatDate,
  getCategoryIcon,
  formatRelativeTime,
} from '../../utils/helpers';
import Avatar from '../../components/ui/Avatar';
import Modal from '../../components/ui/Modal';
import LoadingSpinner from '../../components/ui/LoadingSpinner';

const TABS = [
  { id: 'expenses', label: 'Expenses', icon: BanknotesIcon },
  { id: 'balances', label: 'Balances', icon: ArrowPathIcon },
  { id: 'settlements', label: 'Settlements', icon: ChatBubbleLeftRightIcon },
  { id: 'activity', label: 'Activity', icon: BoltIcon },
];

export default function GroupDetailPage() {
  const { id } = useParams();
  const { user: currentUser } = useAuthStore();
  const [group, setGroup] = useState(null);
  const [expenses, setExpenses] = useState([]);
  const [balances, setBalances] = useState([]);
  const [memberBalances, setMemberBalances] = useState([]);
  const [useSimplified, setUseSimplified] = useState(false);
  const [settlements, setSettlements] = useState([]);
  const [activity, setActivity] = useState([]);
  const [activeTab, setActiveTab] = useState('expenses');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [addMemberOpen, setAddMemberOpen] = useState(false);
  const [memberFilter, setMemberFilter] = useState('');
  const [allUsers, setAllUsers] = useState([]);
  const [addingMember, setAddingMember] = useState(false);

  const [settlementOpen, setSettlementOpen] = useState(false);
  const [settlementPaidBy, setSettlementPaidBy] = useState('');
  const [settlementPaidTo, setSettlementPaidTo] = useState('');
  const [settlementAmount, setSettlementAmount] = useState('');
  const [settlementNotes, setSettlementNotes] = useState('');
  const [settlementSubmitting, setSettlementSubmitting] = useState(false);

  const fetchGroup = async () => {
    if (!id) return;
    try {
      const response = await groupApi.getById(id);
      setGroup(response.data?.group ?? response.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load group');
    }
  };

  const fetchAllData = useCallback(async () => {
    if (!id) return;
    try {
      const [expRes, balRes, mbRes, setRes, actRes] = await Promise.all([
        expenseApi.getGroupExpenses(id).catch(() => ({ data: [] })),
        groupApi.getBalances(id).catch(() => ({ data: [] })),
        groupApi.getMemberBalances(id).catch(() => ({ data: [] })),
        settlementApi.getGroupSettlements(id).catch(() => ({ data: [] })),
        dashboardApi.getGroupActivity(id).catch(() => ({ data: [] })),
      ]);
      setExpenses(expRes.data?.expenses ?? expRes.data ?? []);
      const balData = balRes.data;
      setBalances(Array.isArray(balData) ? balData : balData?.balances ?? []);
      const mbData = mbRes.data;
      setMemberBalances(Array.isArray(mbData) ? mbData : []);
      setSettlements(setRes.data?.settlements ?? setRes.data ?? []);
      const actData = actRes.data;
      setActivity(Array.isArray(actData) ? actData : actData?.activity ?? []);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load data');
    }
  }, [id]);

  useEffect(() => {
    const load = async () => {
      if (!id) return;
      try {
        setLoading(true);
        setError(null);
        await fetchGroup();
        await fetchAllData();
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to load data');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [id, fetchAllData]);

  useEffect(() => {
    if (!addMemberOpen) return;
    const fetchAllUsers = async () => {
      try {
        const response = await userApi.getAllUsers();
        const users = response.data?.users ?? response.data ?? [];
        setAllUsers(Array.isArray(users) ? users : []);
      } catch {
        setAllUsers([]);
      }
    };
    fetchAllUsers();
  }, [addMemberOpen]);

  const handleAddMember = async (userId) => {
    try {
      setAddingMember(true);
      await groupApi.addMember(id, userId);
      await fetchGroup();
      setAddMemberOpen(false);
      setMemberFilter('');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to add member');
    } finally {
      setAddingMember(false);
    }
  };

  const handleAddMemberByName = async (name) => {
    const trimmed = name.trim();
    if (!trimmed) return;
    try {
      setAddingMember(true);
      await groupApi.addMemberByName(id, trimmed);
      await fetchGroup();
      setAddMemberOpen(false);
      setMemberFilter('');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to add member');
    } finally {
      setAddingMember(false);
    }
  };

  const handleRecordSettlement = async (e) => {
    e.preventDefault();
    if (!settlementPaidBy || !settlementPaidTo || !settlementAmount) return;
    try {
      setSettlementSubmitting(true);
      setError(null);
      await settlementApi.create({
        paidByUserId: Number(settlementPaidBy),
        paidToUserId: Number(settlementPaidTo),
        amount: parseFloat(settlementAmount),
        groupId: Number(id),
        date: new Date().toISOString().slice(0, 10),
        notes: settlementNotes.trim() || undefined,
      });
      setSettlementOpen(false);
      setSettlementPaidBy('');
      setSettlementPaidTo('');
      setSettlementAmount('');
      setSettlementNotes('');
      await fetchAllData();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to record settlement');
    } finally {
      setSettlementSubmitting(false);
    }
  };

  const members = group?.members ?? [];

  if (loading && !group) {
    return <LoadingSpinner />;
  }

  if (error && !group) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-8">
        <div className="p-4 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-600 dark:text-red-400">
          {error}
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <div className="card mb-6">
        <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">
              {group?.name}
            </h1>
            {group?.description && (
              <p className="text-gray-500 dark:text-gray-400 mt-1">
                {group.description}
              </p>
            )}
            <div className="flex items-center gap-2 mt-3">
              <div className="flex -space-x-2">
                {members.slice(0, 5).map((m) => (
                  <Avatar
                    key={m.id ?? m.userId}
                    name={m.name ?? m.user?.name}
                    avatarUrl={m.avatarUrl ?? m.user?.avatarUrl}
                    size="sm"
                  />
                ))}
              </div>
              <span className="text-sm text-gray-500 dark:text-gray-400">
                {members.length} members
              </span>
            </div>
          </div>
          <div className="flex flex-wrap gap-2 shrink-0">
            <button
              onClick={() => setAddMemberOpen(true)}
              className="btn-secondary inline-flex items-center gap-2"
            >
              <UserPlusIcon className="w-5 h-5" />
              Add Member
            </button>
            <Link
              to={`/expenses/add?groupId=${id}`}
              className="btn-primary inline-flex items-center gap-2"
            >
              <PlusIcon className="w-5 h-5" />
              Add Expense
            </Link>
          </div>
        </div>
      </div>

      {error && (
        <div className="mb-6 p-4 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-600 dark:text-red-400">
          {error}
        </div>
      )}

      <div className="border-b border-gray-200 dark:border-gray-700 mb-6">
        <nav className="flex gap-6">
          {TABS.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`flex items-center gap-2 py-3 px-1 border-b-2 font-medium text-sm transition-colors ${
                activeTab === tab.id
                  ? 'border-[var(--color-primary)] text-[var(--color-primary)]'
                  : 'border-transparent text-gray-500 hover:text-gray-700 dark:hover:text-gray-400'
              }`}
            >
              <tab.icon className="w-4 h-4" />
              {tab.label}
            </button>
          ))}
        </nav>
      </div>

      {activeTab === 'expenses' && (
        <div className="space-y-3">
          {expenses.length === 0 ? (
            <div className="card text-center py-12 text-gray-500 dark:text-gray-400">
              No expenses yet.{' '}
              <Link to={`/expenses/add?groupId=${id}`} className="text-[var(--color-primary)] hover:underline">
                Add one
              </Link>
            </div>
          ) : (
            expenses.map((exp) => (
              <Link
                key={exp.id}
                to={`/expenses/${exp.id}`}
                className="card flex items-center gap-4 hover:shadow-md transition-shadow"
              >
                <span className="text-2xl">{getCategoryIcon(exp.category)}</span>
                <div className="flex-1 min-w-0">
                  <p className="font-medium text-gray-900 dark:text-gray-100 truncate">
                    {exp.description}
                  </p>
                  <p className="text-sm text-gray-500 dark:text-gray-400">
                    {exp.paidBy?.name ?? exp.paidByName ?? 'Unknown'} • {formatDate(exp.date)}
                  </p>
                </div>
                <span className="font-semibold text-gray-900 dark:text-gray-100 shrink-0">
                  {formatCurrency(exp.amount)}
                </span>
              </Link>
            ))
          )}
        </div>
      )}

      {activeTab === 'balances' && (
        <div className="space-y-4">
          <label className="flex items-center gap-2 cursor-pointer">
            <input
              type="checkbox"
              checked={useSimplified}
              onChange={(e) => setUseSimplified(e.target.checked)}
              className="rounded border-gray-300"
            />
            <span className="text-sm text-gray-600 dark:text-gray-400">
              Show per-person summary
            </span>
          </label>

          {useSimplified ? (
            memberBalances.length === 0 ? (
              <div className="card text-center py-12 text-gray-500 dark:text-gray-400">
                No balances to show. Everyone is settled up!
              </div>
            ) : (
              <div className="space-y-3">
                {memberBalances.map((mb, i) => {
                  const name = mb.user?.name ?? 'Unknown';
                  const amt = Number(mb.amount ?? 0);
                  const isPositive = amt > 0;
                  const isNegative = amt < 0;
                  return (
                    <div key={mb.user?.id ?? i} className="card flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <Avatar name={name} size="sm" />
                        <span className="font-medium text-gray-800 dark:text-gray-200">
                          {name}
                        </span>
                      </div>
                      <span
                        className={`font-semibold ${
                          isPositive ? 'text-emerald-600 dark:text-emerald-400'
                          : isNegative ? 'text-red-600 dark:text-red-400'
                          : 'text-gray-500'
                        }`}
                      >
                        {isPositive ? 'is owed ' : isNegative ? 'owes ' : ''}
                        {formatCurrency(Math.abs(amt))}
                      </span>
                    </div>
                  );
                })}
              </div>
            )
          ) : (
            balances.length === 0 ? (
              <div className="card text-center py-12 text-gray-500 dark:text-gray-400">
                No balances to show. Everyone is settled up!
              </div>
            ) : (
              <div className="space-y-3">
                {balances.map((b, i) => {
                  const fromName = b.from?.name ?? 'Unknown';
                  const toName = b.to?.name ?? 'Unknown';
                  return (
                    <div key={i} className="card flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <Avatar name={fromName} size="sm" />
                        <span className="font-medium text-gray-800 dark:text-gray-200 text-sm">
                          {fromName}
                        </span>
                        <span className="text-gray-500 text-sm">owes</span>
                        <Avatar name={toName} size="sm" />
                        <span className="font-medium text-gray-800 dark:text-gray-200 text-sm">
                          {toName}
                        </span>
                      </div>
                      <span className="font-semibold text-red-600 dark:text-red-400 shrink-0">
                        {formatCurrency(Math.abs(b.amount ?? 0))}
                      </span>
                    </div>
                  );
                })}
              </div>
            )
          )}
        </div>
      )}

      {activeTab === 'settlements' && (
        <div className="space-y-4">
          <div className="flex justify-end">
            <button
              onClick={() => setSettlementOpen(true)}
              className="btn-primary inline-flex items-center gap-2"
            >
              <PlusIcon className="w-5 h-5" />
              Record Settlement
            </button>
          </div>
          {settlements.length === 0 ? (
            <div className="card text-center py-12 text-gray-500 dark:text-gray-400">
              No settlements recorded yet.
            </div>
          ) : (
            <div className="space-y-3">
              {settlements.map((s) => (
                <div key={s.id} className="card flex items-center gap-3">
                  <Avatar name={s.paidBy?.name ?? 'Unknown'} size="sm" />
                  <span className="font-medium text-gray-800 dark:text-gray-200 text-sm">
                    {s.paidBy?.name ?? 'Unknown'}
                  </span>
                  <span className="text-gray-500 text-sm">paid</span>
                  <Avatar name={s.paidTo?.name ?? 'Unknown'} size="sm" />
                  <span className="font-medium text-gray-800 dark:text-gray-200 text-sm">
                    {s.paidTo?.name ?? 'Unknown'}
                  </span>
                  <span className="font-semibold text-emerald-600 dark:text-emerald-400 ml-auto">
                    {formatCurrency(s.amount)}
                  </span>
                  <span className="text-sm text-gray-500">
                    {formatDate(s.date)}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {activeTab === 'activity' && (
        <div className="space-y-3">
          {activity.length === 0 ? (
            <div className="card text-center py-12 text-gray-500 dark:text-gray-400">
              No recent activity.
            </div>
          ) : (
            activity.map((a, i) => {
              const actType = (a.type || '').toString().toLowerCase();
              const icon = actType.includes('expense') ? '💰'
                : actType.includes('settlement') ? '✅'
                : actType.includes('group') ? '👥'
                : '📋';
              return (
                <div key={a.id ?? i} className="card flex items-center gap-4">
                  <span className="text-2xl">{icon}</span>
                  <div className="flex-1">
                    <p className="text-gray-900 dark:text-gray-100">{a.description ?? a.message}</p>
                    <p className="text-sm text-gray-500 dark:text-gray-400">
                      {a.performedBy?.name ?? 'Unknown'} • {formatRelativeTime(a.createdAt ?? a.timestamp)}
                    </p>
                  </div>
                </div>
              );
            })
          )}
        </div>
      )}

      <Modal
        isOpen={addMemberOpen}
        onClose={() => {
          setAddMemberOpen(false);
          setMemberFilter('');
        }}
        title="Add Member"
      >
        <div className="space-y-4">
          <div className="relative">
            <input
              type="text"
              value={memberFilter}
              onChange={(e) => setMemberFilter(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  e.preventDefault();
                  const trimmed = memberFilter.trim();
                  if (!trimmed) return;
                  const memberIds = members.map((m) => m.id ?? m.userId);
                  const exactMatch = allUsers.find(
                    (u) =>
                      !memberIds.includes(u.id) &&
                      (u.name || '').toLowerCase() === trimmed.toLowerCase()
                  );
                  if (exactMatch) {
                    handleAddMember(exactMatch.id);
                  } else {
                    handleAddMemberByName(trimmed);
                  }
                }
              }}
              className="input-field"
              placeholder="Type a name and press Enter to add..."
              disabled={addingMember}
            />
          </div>
          {(() => {
            if (!memberFilter.trim()) return null;
            const memberIds = members.map((m) => m.id ?? m.userId);
            const filtered = allUsers.filter(
              (u) =>
                !memberIds.includes(u.id) &&
                ((u.name || '').toLowerCase().includes(memberFilter.toLowerCase()) ||
                  (u.email || '').toLowerCase().includes(memberFilter.toLowerCase()))
            );
            if (filtered.length > 0) {
              return (
                <ul className="max-h-60 overflow-y-auto border border-gray-200 dark:border-gray-600 rounded-lg">
                  {filtered.map((u) => (
                    <li key={u.id}>
                      <button
                        type="button"
                        onClick={() => handleAddMember(u.id)}
                        disabled={addingMember}
                        className="w-full px-4 py-2.5 text-left hover:bg-gray-50 dark:hover:bg-gray-700 flex items-center gap-3 transition-colors"
                      >
                        <Avatar name={u.name ?? u.email} size="sm" />
                        <div className="min-w-0">
                          <span className="font-medium text-gray-900 dark:text-gray-100 block truncate">{u.name ?? u.email}</span>
                          {u.email && u.name && (
                            <span className="text-sm text-gray-500 dark:text-gray-400 block truncate">{u.email}</span>
                          )}
                        </div>
                      </button>
                    </li>
                  ))}
                </ul>
              );
            }
            return null;
          })()}
          <p className="text-xs text-gray-500 dark:text-gray-400">
            Type any name and press Enter. Registered users appear as suggestions you can click.
          </p>
        </div>
      </Modal>

      <Modal
        isOpen={settlementOpen}
        onClose={() => {
          setSettlementOpen(false);
          setSettlementPaidBy('');
          setSettlementPaidTo('');
          setSettlementAmount('');
          setSettlementNotes('');
        }}
        title="Record Settlement"
      >
        <form onSubmit={handleRecordSettlement} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
              Who paid (settled by)
            </label>
            <select
              value={settlementPaidBy}
              onChange={(e) => {
                setSettlementPaidBy(e.target.value);
                if (e.target.value === settlementPaidTo) setSettlementPaidTo('');
              }}
              className="input-field"
              required
            >
              <option value="">Select who paid...</option>
              {members.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.name || m.email}{m.id === currentUser?.id ? ' (You)' : ''}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
              Paid to
            </label>
            <select
              value={settlementPaidTo}
              onChange={(e) => setSettlementPaidTo(e.target.value)}
              className="input-field"
              required
            >
              <option value="">Select who received...</option>
              {members
                .filter((m) => String(m.id) !== String(settlementPaidBy))
                .map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.name || m.email}{m.id === currentUser?.id ? ' (You)' : ''}
                  </option>
                ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
              Amount
            </label>
            <input
              type="number"
              step="0.01"
              min="0.01"
              value={settlementAmount}
              onChange={(e) => setSettlementAmount(e.target.value)}
              className="input-field"
              placeholder="0.00"
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
              Notes (optional)
            </label>
            <input
              type="text"
              value={settlementNotes}
              onChange={(e) => setSettlementNotes(e.target.value)}
              className="input-field"
              placeholder="e.g. UPI payment, cash, etc."
            />
          </div>
          {error && (
            <p className="text-sm text-red-600 dark:text-red-400">{error}</p>
          )}
          <button
            type="submit"
            disabled={settlementSubmitting || !settlementPaidBy || !settlementPaidTo || !settlementAmount}
            className="btn-primary w-full py-2.5"
          >
            {settlementSubmitting ? 'Recording...' : 'Record Settlement'}
          </button>
        </form>
      </Modal>
    </div>
  );
}
