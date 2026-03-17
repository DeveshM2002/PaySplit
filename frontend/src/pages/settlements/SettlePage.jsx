import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { settlementApi } from '../../api/settlements';
import { groupApi } from '../../api/groups';
import { userApi } from '../../api/users';
import useAuthStore from '../../store/authStore';
import LoadingSpinner from '../../components/ui/LoadingSpinner';

export default function SettlePage() {
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const [paidToId, setPaidToId] = useState('');
  const [amount, setAmount] = useState('');
  const [groupId, setGroupId] = useState('');
  const [date, setDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [notes, setNotes] = useState('');
  const [users, setUsers] = useState([]);
  const [groups, setGroups] = useState([]);
  const [loading, setLoading] = useState(false);
  const [loadingData, setLoadingData] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const load = async () => {
      try {
        setLoadingData(true);
        const [groupsRes, usersRes] = await Promise.all([
          groupApi.getAll().catch(() => ({ data: [] })),
          userApi.getAllUsers().catch(() => ({ data: [] })),
        ]);
        const g = groupsRes.data?.groups ?? groupsRes.data ?? [];
        setGroups(Array.isArray(g) ? g : []);
        const u = usersRes.data?.users ?? usersRes.data ?? [];
        setUsers(Array.isArray(u) ? u : []);
      } catch {
        setGroups([]);
        setUsers([]);
      } finally {
        setLoadingData(false);
      }
    };
    load();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    const amt = parseFloat(amount);
    if (!paidToId || !amt || amt <= 0) return;
    try {
      setLoading(true);
      setError(null);
      await settlementApi.create({
        paidToId,
        amount: amt,
        groupId: groupId || undefined,
        date,
        notes: notes.trim() || undefined,
        paidById: user?.id,
      });
      navigate(-1);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to record settlement');
    } finally {
      setLoading(false);
    }
  };

  if (loadingData) {
    return <LoadingSpinner />;
  }

  return (
    <div className="max-w-xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100 mb-6">
        Record Settlement
      </h1>

      <form onSubmit={handleSubmit} className="space-y-6">
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
            Paying to *
          </label>
          <select
            value={paidToId}
            onChange={(e) => setPaidToId(e.target.value)}
            className="input-field"
            required
          >
            <option value="">Select user</option>
            {users
              .filter((u) => u.id !== user?.id)
              .map((u) => (
                <option key={u.id} value={u.id}>
                  {u.name ?? u.email}
                </option>
              ))}
            {users.length === 0 && (
              <option value="" disabled>
                No users available
              </option>
            )}
          </select>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
            Amount *
          </label>
          <input
            type="number"
            step="0.01"
            min="0.01"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            className="input-field"
            placeholder="0.00"
            required
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
            Group (optional)
          </label>
          <select
            value={groupId}
            onChange={(e) => setGroupId(e.target.value)}
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
            Notes
          </label>
          <textarea
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            className="input-field min-h-[80px] resize-y"
            placeholder="Optional notes"
            rows={3}
          />
        </div>

        {error && (
          <div className="p-3 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-600 dark:text-red-400 text-sm">
            {error}
          </div>
        )}

        <button
          type="submit"
          disabled={loading || !paidToId || !amount || parseFloat(amount) <= 0}
          className="btn-primary w-full py-3"
        >
          {loading ? (
            <span className="flex items-center justify-center gap-2">
              <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
              Recording...
            </span>
          ) : (
            'Record Settlement'
          )}
        </button>
      </form>
    </div>
  );
}
