import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { MagnifyingGlassIcon, XMarkIcon } from '@heroicons/react/24/outline';
import { groupApi } from '../../api/groups';
import { userApi } from '../../api/users';
import { expenseApi } from '../../api/expenses';
import useAuthStore from '../../store/authStore';
import {
  EXPENSE_CATEGORIES,
  SPLIT_TYPES,
  getCategoryIcon,
  formatCurrency,
} from '../../utils/helpers';
import LoadingSpinner from '../../components/ui/LoadingSpinner';

export default function AddExpensePage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const preselectedGroupId = searchParams.get('groupId');
  const { user } = useAuthStore();

  const [description, setDescription] = useState('');
  const [amount, setAmount] = useState('');
  const [category, setCategory] = useState('OTHER');
  const [date, setDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [groupId, setGroupId] = useState(preselectedGroupId || '');
  const [splitType, setSplitType] = useState('EQUAL');
  const [paidById, setPaidById] = useState(user?.id || '');
  const [participants, setParticipants] = useState([]);
  const [exactAmounts, setExactAmounts] = useState({});
  const [percentages, setPercentages] = useState({});

  const [groups, setGroups] = useState([]);
  const [members, setMembers] = useState([]);
  const [userSearch, setUserSearch] = useState('');
  const [userSearchResults, setUserSearchResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [loadingGroups, setLoadingGroups] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchGroups = async () => {
      try {
        setLoadingGroups(true);
        const response = await groupApi.getAll();
        const data = response.data?.groups ?? response.data ?? [];
        setGroups(Array.isArray(data) ? data : []);
      } catch {
        setGroups([]);
      } finally {
        setLoadingGroups(false);
      }
    };
    fetchGroups();
  }, []);

  useEffect(() => {
    if (groupId) {
      const loadGroup = async () => {
        try {
          const response = await groupApi.getById(groupId);
          const g = response.data?.group ?? response.data;
          const m = g?.members ?? [];
          setMembers(m.map((x) => ({ id: x.id ?? x.userId, name: x.name ?? x.user?.name ?? x.email })));
        } catch {
          setMembers([]);
        }
      };
      loadGroup();
    } else {
      setMembers([]);
    }
  }, [groupId]);

  useEffect(() => {
    if (!userSearch.trim()) {
      setUserSearchResults([]);
      return;
    }
    const timer = setTimeout(async () => {
      try {
        const response = await userApi.searchUsers(userSearch);
        const users = response.data?.users ?? response.data ?? [];
        const list = Array.isArray(users) ? users : [];
        setUserSearchResults(list);
      } catch {
        setUserSearchResults([]);
      }
    }, 300);
    return () => clearTimeout(timer);
  }, [userSearch]);

  const addParticipant = (u) => {
    const id = u.id;
    if (!participants.some((p) => p.id === id)) {
      setParticipants([...participants, { id, name: u.name ?? u.email }]);
      setExactAmounts((prev) => ({ ...prev, [id]: '' }));
      setPercentages((prev) => ({ ...prev, [id]: '' }));
      setUserSearch('');
      setUserSearchResults([]);
    }
  };

  const removeParticipant = (id) => {
    setParticipants(participants.filter((p) => p.id !== id));
    setExactAmounts((prev) => {
      const next = { ...prev };
      delete next[id];
      return next;
    });
    setPercentages((prev) => {
      const next = { ...prev };
      delete next[id];
      return next;
    });
  };

  const allParticipants = groupId
    ? [...members.filter((m) => !participants.some((p) => p.id === m.id)), ...participants.filter((p) => !members.some((m) => m.id === p.id))]
    : participants;

  const availableToAdd = groupId
    ? members.filter((m) => !participants.some((p) => p.id === m.id))
    : [];

  const totalExact = Object.values(exactAmounts).reduce(
    (sum, v) => sum + (parseFloat(v) || 0),
    0
  );
  const totalPct = Object.values(percentages).reduce(
    (sum, v) => sum + (parseFloat(v) || 0),
    0
  );
  const amountNum = parseFloat(amount) || 0;
  const pctValid = splitType !== 'PERCENTAGE' || Math.abs(totalPct - 100) < 0.01;
  const exactValid = splitType !== 'EXACT' || Math.abs(totalExact - amountNum) < 0.01;

  const buildSplits = () => {
    const amt = amountNum;
    const parts = participants.length || 1;
    if (splitType === 'EQUAL') {
      const share = amt / parts;
      return participants.map((p) => ({ userId: p.id, amount: share, percentage: 100 / parts }));
    }
    if (splitType === 'EXACT') {
      return participants.map((p) => ({
        userId: p.id,
        amount: parseFloat(exactAmounts[p.id]) || 0,
      }));
    }
    return participants.map((p) => ({
      userId: p.id,
      amount: (amt * (parseFloat(percentages[p.id]) || 0)) / 100,
      percentage: parseFloat(percentages[p.id]) || 0,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!description.trim() || amountNum <= 0) return;
    if (splitType === 'PERCENTAGE' && !pctValid) return;
    if (splitType === 'EXACT' && !exactValid) return;
    if (participants.length === 0) {
      setError('Select at least one participant');
      return;
    }
    try {
      setLoading(true);
      setError(null);
      const splits = buildSplits();
      await expenseApi.create({
        description: description.trim(),
        amount: amountNum,
        category,
        date,
        groupId: groupId || undefined,
        splitType,
        paidById: paidById || user?.id,
        splits,
      });
      navigate(-1);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to add expense');
    } finally {
      setLoading(false);
    }
  };

  if (loadingGroups) {
    return <LoadingSpinner />;
  }

  return (
    <div className="max-w-xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100 mb-6">
        Add Expense
      </h1>

      <form onSubmit={handleSubmit} className="space-y-6">
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
            Description *
          </label>
          <input
            type="text"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className="input-field"
            placeholder="What was it for?"
            required
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
            Amount *
          </label>
          <input
            type="number"
            step="0.01"
            min="0"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            className="input-field"
            placeholder="0.00"
            required
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
            Category
          </label>
          <select
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            className="input-field"
          >
            {EXPENSE_CATEGORIES.map((c) => (
              <option key={c.value} value={c.value}>
                {getCategoryIcon(c.value)} {c.label}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
            Date
          </label>
          <input
            type="date"
            value={date}
            onChange={(e) => setDate(e.target.value)}
            className="input-field"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
            Group (optional)
          </label>
          <select
            value={groupId}
            onChange={(e) => {
              setGroupId(e.target.value);
              setParticipants([]);
              setExactAmounts({});
              setPercentages({});
              setPaidById(user?.id || '');
            }}
            className="input-field"
          >
            <option value="">No group</option>
            {groups.map((g) => (
              <option key={g.id} value={g.id}>
                {g.name}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
            Split type
          </label>
          <select
            value={splitType}
            onChange={(e) => setSplitType(e.target.value)}
            className="input-field"
          >
            {SPLIT_TYPES.map((s) => (
              <option key={s.value} value={s.value}>
                {s.label}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
            Paid by *
          </label>
          <select
            value={paidById}
            onChange={(e) => setPaidById(Number(e.target.value))}
            className="input-field"
          >
            <option value={user?.id}>
              You ({user?.name || user?.email})
            </option>
            {(groupId ? members : participants)
              .filter((m) => m.id !== user?.id)
              .map((m) => (
                <option key={m.id} value={m.id}>
                  {m.name}
                </option>
              ))}
          </select>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
            Participants *
          </label>
          {groupId ? (
            <div className="space-y-2">
              <p className="text-sm text-gray-500">Select from group members:</p>
              <div className="flex flex-wrap gap-2">
                {availableToAdd.map((m) => (
                  <button
                    key={m.id}
                    type="button"
                    onClick={() => addParticipant(m)}
                    className="px-3 py-1.5 rounded-lg border border-gray-300 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-700 text-sm"
                  >
                    + {m.name}
                  </button>
                ))}
              </div>
              <div className="relative mt-2">
                <MagnifyingGlassIcon className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                <input
                  type="text"
                  value={userSearch}
                  onChange={(e) => setUserSearch(e.target.value)}
                  className="input-field pl-10"
                  placeholder="Or search other users..."
                />
              </div>
              {userSearchResults.length > 0 && (
                <ul className="mt-2 border border-gray-200 dark:border-gray-600 rounded-lg overflow-hidden">
                  {userSearchResults
                    .filter((u) => !participants.some((p) => p.id === u.id))
                    .map((u) => (
                      <li key={u.id}>
                        <button
                          type="button"
                          onClick={() => addParticipant(u)}
                          className="w-full px-4 py-2 text-left hover:bg-gray-50 dark:hover:bg-gray-700"
                        >
                          {u.name ?? u.email}
                        </button>
                      </li>
                    ))}
                </ul>
              )}
            </div>
          ) : (
            <div className="relative">
              <MagnifyingGlassIcon className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <input
                type="text"
                value={userSearch}
                onChange={(e) => setUserSearch(e.target.value)}
                className="input-field pl-10"
                placeholder="Search users to add..."
              />
              {userSearchResults.length > 0 && (
                <ul className="mt-2 border border-gray-200 dark:border-gray-600 rounded-lg overflow-hidden">
                  {userSearchResults
                    .filter((u) => !participants.some((p) => p.id === u.id))
                    .map((u) => (
                      <li key={u.id}>
                        <button
                          type="button"
                          onClick={() => addParticipant(u)}
                          className="w-full px-4 py-2 text-left hover:bg-gray-50 dark:hover:bg-gray-700"
                        >
                          {u.name ?? u.email}
                        </button>
                      </li>
                    ))}
                </ul>
              )}
            </div>
          )}
          {participants.length > 0 && (
            <div className="flex flex-wrap gap-2 mt-3">
              {participants.map((p) => (
                <span
                  key={p.id}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-[var(--color-primary)]/10 text-[var(--color-primary)] text-sm"
                >
                  {p.name}
                  <button
                    type="button"
                    onClick={() => removeParticipant(p.id)}
                    className="hover:bg-[var(--color-primary)]/20 rounded-full p-0.5"
                    aria-label={`Remove ${p.name}`}
                  >
                    <XMarkIcon className="w-4 h-4" />
                  </button>
                </span>
              ))}
            </div>
          )}
        </div>

        {participants.length > 0 && (
          <div className="space-y-3">
            <h3 className="font-medium text-gray-900 dark:text-gray-100">
              Split details
            </h3>
            {splitType === 'EQUAL' && (
              <p className="text-sm text-gray-500">
                Each person owes {formatCurrency(amountNum / participants.length)}
              </p>
            )}
            {splitType === 'EXACT' && (
              <div className="space-y-2">
                {participants.map((p) => (
                  <div key={p.id} className="flex items-center gap-3">
                    <span className="w-32 truncate">{p.name}</span>
                    <input
                      type="number"
                      step="0.01"
                      min="0"
                      value={exactAmounts[p.id] ?? ''}
                      onChange={(e) =>
                        setExactAmounts((prev) => ({
                          ...prev,
                          [p.id]: e.target.value,
                        }))
                      }
                      className="input-field flex-1"
                      placeholder="0.00"
                    />
                  </div>
                ))}
                {!exactValid && (
                  <p className="text-sm text-red-600">
                    Total must equal {amountNum.toFixed(2)}
                  </p>
                )}
              </div>
            )}
            {splitType === 'PERCENTAGE' && (
              <div className="space-y-2">
                {participants.map((p) => (
                  <div key={p.id} className="flex items-center gap-3">
                    <span className="w-32 truncate">{p.name}</span>
                    <input
                      type="number"
                      step="0.01"
                      min="0"
                      max="100"
                      value={percentages[p.id] ?? ''}
                      onChange={(e) =>
                        setPercentages((prev) => ({
                          ...prev,
                          [p.id]: e.target.value,
                        }))
                      }
                      className="input-field flex-1"
                      placeholder="%"
                    />
                    <span>%</span>
                  </div>
                ))}
                {!pctValid && (
                  <p className="text-sm text-red-600">
                    Percentages must sum to 100% (current: {totalPct.toFixed(1)}%)
                  </p>
                )}
              </div>
            )}
          </div>
        )}

        {error && (
          <div className="p-3 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-600 dark:text-red-400 text-sm">
            {error}
          </div>
        )}

        <button
          type="submit"
          disabled={
            loading ||
            !description.trim() ||
            amountNum <= 0 ||
            participants.length === 0 ||
            (splitType === 'PERCENTAGE' && !pctValid) ||
            (splitType === 'EXACT' && !exactValid)
          }
          className="btn-primary w-full py-3"
        >
          {loading ? (
            <span className="flex items-center justify-center gap-2">
              <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
              Adding...
            </span>
          ) : (
            'Add Expense'
          )}
        </button>
      </form>
    </div>
  );
}
