import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { UserGroupIcon, UserIcon } from '@heroicons/react/24/outline';
import { groupApi } from '../../api/groups';
import { dashboardApi } from '../../api/dashboard';
import { formatCurrency } from '../../utils/helpers';
import Avatar from '../ui/Avatar';
import LoadingSpinner from '../ui/LoadingSpinner';

export default function Sidebar() {
  const [groups, setGroups] = useState([]);
  const [balances, setBalances] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchData() {
      setLoading(true);
      try {
        const [groupsRes, balancesRes] = await Promise.all([
          groupApi.getAll(),
          dashboardApi.getBalances(),
        ]);
        setGroups(groupsRes.data?.groups || groupsRes.data?.data || groupsRes.data || []);
        setBalances(balancesRes.data?.data || balancesRes.data?.balances || []);
      } catch {
        setGroups([]);
        setBalances([]);
      } finally {
        setLoading(false);
      }
    }
    fetchData();
  }, []);

  if (loading) {
    return (
      <aside className="hidden lg:block w-[250px] shrink-0 border-r border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 p-4">
        <LoadingSpinner />
      </aside>
    );
  }

  // Build friends list from balances (amount > 0 means they owe you, amount < 0 means you owe them)
  const friendBalances = Array.isArray(balances)
    ? balances
    : Object.entries(balances).flatMap(([key, val]) =>
        Array.isArray(val) ? val : [{ ...val, _key: key }]
      );

  return (
    <aside className="hidden lg:block w-[250px] shrink-0 border-r border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 overflow-y-auto">
      <nav className="p-4 space-y-6">
        <Link
          to="/"
          className="flex items-center gap-2 px-3 py-2 rounded-lg text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800 font-medium transition-colors"
        >
          <UserIcon className="w-5 h-5" />
          Dashboard
        </Link>

        {/* Groups */}
        <div>
          <h3 className="px-3 mb-2 text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">
            Groups
          </h3>
          <div className="space-y-0.5">
            {groups.length === 0 ? (
              <p className="px-3 py-2 text-sm text-gray-500 dark:text-gray-400">
                No groups yet
              </p>
            ) : (
              groups.map((group) => {
                const balance = group.balance ?? group.userBalance ?? 0;
                const isOwed = balance > 0;
                const isOwing = balance < 0;
                return (
                  <Link
                    key={group.id}
                    to={`/groups/${group.id}`}
                    className="flex items-center justify-between gap-2 px-3 py-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors group"
                  >
                    <div className="flex items-center gap-2 min-w-0">
                      <div className="w-8 h-8 rounded-full bg-gray-200 dark:bg-gray-700 flex items-center justify-center shrink-0">
                        <UserGroupIcon className="w-4 h-4 text-gray-500 dark:text-gray-400" />
                      </div>
                      <span className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">
                        {group.name}
                      </span>
                    </div>
                    {balance !== undefined && balance !== null && (
                      <span
                        className={`text-xs font-medium shrink-0 ${
                          isOwed
                            ? 'text-green-600 dark:text-green-400'
                            : isOwing
                            ? 'text-red-600 dark:text-red-400'
                            : 'text-gray-500 dark:text-gray-400'
                        }`}
                      >
                        {isOwed ? '+' : ''}
                        {formatCurrency(Math.abs(balance))}
                      </span>
                    )}
                  </Link>
                );
              })
            )}
          </div>
        </div>

        {/* Friends */}
        <div>
          <h3 className="px-3 mb-2 text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">
            Friends
          </h3>
          <div className="space-y-0.5">
            {friendBalances.length === 0 ? (
              <p className="px-3 py-2 text-sm text-gray-500 dark:text-gray-400">
                No balances
              </p>
            ) : (
              friendBalances.map((item, idx) => {
                const name =
                  item.friendName ??
                  item.userName ??
                  item.creditorName ??
                  item.debtorName ??
                  'Friend';
                const amount = item.amount ?? item.balance ?? 0;
                const isOwed = amount > 0;
                const isOwing = amount < 0;
                const friendId = item.friendId ?? item.userId ?? item.creditorId ?? item.debtorId ?? idx;
                return (
                  <Link
                    key={friendId}
                    to={`/friends/${friendId}`}
                    className="flex items-center justify-between gap-2 px-3 py-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
                  >
                    <div className="flex items-center gap-2 min-w-0">
                      <Avatar name={name} size="sm" />
                      <span className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">
                        {name}
                      </span>
                    </div>
                    <span
                      className={`text-xs font-medium shrink-0 ${
                        isOwed
                          ? 'text-green-600 dark:text-green-400'
                          : isOwing
                          ? 'text-red-600 dark:text-red-400'
                          : 'text-gray-500 dark:text-gray-400'
                      }`}
                    >
                      {isOwed ? '+' : ''}
                      {formatCurrency(Math.abs(amount))}
                    </span>
                  </Link>
                );
              })
            )}
          </div>
        </div>
      </nav>
    </aside>
  );
}
