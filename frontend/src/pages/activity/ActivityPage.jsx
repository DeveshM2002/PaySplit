import { useState, useEffect } from 'react';
import { dashboardApi } from '../../api/dashboard';
import { formatRelativeTime } from '../../utils/helpers';
import LoadingSpinner from '../../components/ui/LoadingSpinner';

const ACTIVITY_ICONS = {
  expense: '💰',
  settlement: '✅',
  group: '👥',
  comment: '💬',
  default: '📋',
};

function getActivityIcon(type) {
  if (!type) return ACTIVITY_ICONS.default;
  const lower = type.toLowerCase();
  if (lower.includes('expense')) return ACTIVITY_ICONS.expense;
  if (lower.includes('settlement')) return ACTIVITY_ICONS.settlement;
  if (lower.includes('group')) return ACTIVITY_ICONS.group;
  if (lower.includes('comment')) return ACTIVITY_ICONS.comment;
  return ACTIVITY_ICONS.default;
}

export default function ActivityPage() {
  const [activity, setActivity] = useState([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState(null);

  const pageSize = 20;

  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true);
        setError(null);
        const response = await dashboardApi.getActivity(0, pageSize);
        const raw = response.data;
        const data = raw?.content ?? raw?.activity ?? (Array.isArray(raw) ? raw : []);
        setActivity(Array.isArray(data) ? data : []);
        setHasMore((Array.isArray(data) ? data : []).length >= pageSize);
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to load activity');
        setActivity([]);
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const loadMore = async () => {
    if (loadingMore || !hasMore) return;
    try {
      setLoadingMore(true);
      const nextPage = page + 1;
      const response = await dashboardApi.getActivity(nextPage, pageSize);
      const raw = response.data;
      const data = raw?.content ?? raw?.activity ?? (Array.isArray(raw) ? raw : []);
      const list = Array.isArray(data) ? data : [];
      setActivity((prev) => [...prev, ...list]);
      setHasMore(list.length >= pageSize);
      setPage(nextPage);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load more');
    } finally {
      setLoadingMore(false);
    }
  };

  if (loading) {
    return <LoadingSpinner />;
  }

  return (
    <div className="max-w-2xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100 mb-6">
        Activity
      </h1>

      {error && (
        <div className="mb-6 p-4 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-600 dark:text-red-400">
          {error}
        </div>
      )}

      {activity.length === 0 && !error ? (
        <div className="card text-center py-16 text-gray-500 dark:text-gray-400">
          No activity yet.
        </div>
      ) : (
        <div className="space-y-3">
          {activity.map((a, i) => (
            <div
              key={a.id ?? i}
              className="card flex items-start gap-4 hover:shadow-md transition-shadow"
            >
              <span className="text-2xl shrink-0">
                {getActivityIcon(a.type)}
              </span>
              <div className="flex-1 min-w-0">
                <p className="text-gray-900 dark:text-gray-100">
                  {a.description ?? a.message ?? 'Activity'}
                </p>
                <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
                  {a.performedBy?.name ?? a.performer?.name ?? a.user?.name ?? 'Someone'} •{' '}
                  {formatRelativeTime(a.createdAt ?? a.timestamp ?? a.date)}
                </p>
              </div>
            </div>
          ))}
        </div>
      )}

      {hasMore && activity.length > 0 && (
        <div className="mt-8 flex justify-center">
          <button
            onClick={loadMore}
            disabled={loadingMore}
            className="btn-secondary px-8 py-3"
          >
            {loadingMore ? (
              <span className="flex items-center gap-2">
                <div className="w-4 h-4 border-2 border-gray-400 border-t-transparent rounded-full animate-spin" />
                Loading...
              </span>
            ) : (
              'Load more'
            )}
          </button>
        </div>
      )}
    </div>
  );
}
