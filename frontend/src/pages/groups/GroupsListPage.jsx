import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { UserGroupIcon, PlusIcon } from '@heroicons/react/24/outline';
import { groupApi } from '../../api/groups';
import { formatCurrency } from '../../utils/helpers';
import EmptyState from '../../components/ui/EmptyState';
import LoadingSpinner from '../../components/ui/LoadingSpinner';

export default function GroupsListPage() {
  const navigate = useNavigate();
  const [groups, setGroups] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchGroups = async () => {
      try {
        setLoading(true);
        setError(null);
        const response = await groupApi.getAll();
        const data = response.data?.groups ?? response.data ?? [];
        setGroups(Array.isArray(data) ? data : []);
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to load groups');
        setGroups([]);
      } finally {
        setLoading(false);
      }
    };
    fetchGroups();
  }, []);

  if (loading) {
    return <LoadingSpinner />;
  }

  return (
    <div className="max-w-6xl mx-auto px-4 py-8">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-8">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">
          Your Groups
        </h1>
        <Link
          to="/groups/create"
          className="btn-primary inline-flex items-center gap-2 shrink-0"
        >
          <PlusIcon className="w-5 h-5" />
          Create Group
        </Link>
      </div>

      {error && (
        <div className="mb-6 p-4 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-600 dark:text-red-400">
          {error}
        </div>
      )}

      {groups.length === 0 && !error ? (
        <div className="card">
          <EmptyState
            icon={UserGroupIcon}
            title="No groups yet"
            description="Create a group to start splitting expenses with friends and family."
            actionLabel="Create Group"
            onAction={() => navigate('/groups/create')}
          />
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {groups.map((group) => (
            <Link
              key={group.id}
              to={`/groups/${group.id}`}
              className="card hover:shadow-md hover:border-[var(--color-primary)]/30 transition-all duration-200 group"
            >
              <div className="flex items-start justify-between gap-3">
                <div className="flex-1 min-w-0">
                  <h3 className="font-semibold text-gray-900 dark:text-gray-100 truncate group-hover:text-[var(--color-primary)] transition-colors">
                    {group.name}
                  </h3>
                  <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
                    {group.memberCount ?? group.members?.length ?? 0} members
                  </p>
                  <p className="text-sm font-medium mt-2 text-gray-700 dark:text-gray-300">
                    Your balance:{' '}
                    <span
                      className={
                        (group.myBalance ?? group.balance ?? 0) >= 0
                          ? 'text-green-600 dark:text-green-400'
                          : 'text-red-600 dark:text-red-400'
                      }
                    >
                      {formatCurrency(group.myBalance ?? group.balance ?? 0)}
                    </span>
                  </p>
                </div>
                <UserGroupIcon className="w-10 h-10 text-gray-300 dark:text-gray-600 shrink-0" />
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
