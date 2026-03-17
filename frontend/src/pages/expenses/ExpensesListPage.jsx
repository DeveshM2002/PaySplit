import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { PlusIcon } from '@heroicons/react/24/outline';
import { expenseApi } from '../../api/expenses';
import { formatCurrency, formatDate, getCategoryIcon } from '../../utils/helpers';
import LoadingSpinner from '../../components/ui/LoadingSpinner';

export default function ExpensesListPage() {
  const [expenses, setExpenses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);

  const PAGE_SIZE = 20;

  const fetchExpenses = async (pageNum) => {
    try {
      if (pageNum === 0) setLoading(true);
      setError(null);
      const response = await expenseApi.getMyExpenses(pageNum, PAGE_SIZE);
      const data = response.data;

      const list = data?.content ?? data?.expenses ?? (Array.isArray(data) ? data : []);

      if (pageNum === 0) {
        setExpenses(list);
      } else {
        setExpenses((prev) => [...prev, ...list]);
      }

      const totalPages = data?.totalPages ?? 1;
      setHasMore(pageNum + 1 < totalPages);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load expenses');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchExpenses(0);
  }, []);

  const loadMore = () => {
    const nextPage = page + 1;
    setPage(nextPage);
    fetchExpenses(nextPage);
  };

  if (loading && expenses.length === 0) {
    return <LoadingSpinner />;
  }

  return (
    <div className="max-w-3xl mx-auto px-4 py-8">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">
          My Expenses
        </h1>
        <Link
          to="/expenses/add"
          className="btn-primary inline-flex items-center gap-2"
        >
          <PlusIcon className="w-5 h-5" />
          Add Expense
        </Link>
      </div>

      {error && (
        <div className="mb-6 p-4 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-600 dark:text-red-400">
          {error}
        </div>
      )}

      {expenses.length === 0 ? (
        <div className="card text-center py-16">
          <p className="text-gray-500 dark:text-gray-400 text-lg mb-4">
            No expenses yet
          </p>
          <Link
            to="/expenses/add"
            className="text-[var(--color-primary)] hover:underline font-medium"
          >
            Add your first expense
          </Link>
        </div>
      ) : (
        <>
          <div className="space-y-3">
            {expenses.map((exp) => (
              <Link
                key={exp.id}
                to={`/expenses/${exp.id}`}
                className="card flex items-center gap-4 hover:shadow-md transition-shadow"
              >
                <span className="text-2xl shrink-0">
                  {getCategoryIcon(exp.category)}
                </span>
                <div className="flex-1 min-w-0">
                  <p className="font-medium text-gray-900 dark:text-gray-100 truncate">
                    {exp.description}
                  </p>
                  <div className="flex flex-wrap gap-x-3 text-sm text-gray-500 dark:text-gray-400">
                    <span>
                      Paid by {exp.paidBy?.name ?? exp.paidByName ?? 'You'}
                    </span>
                    <span>{formatDate(exp.date)}</span>
                    {exp.groupName && (
                      <span className="px-2 py-0.5 rounded-full bg-gray-100 dark:bg-gray-700 text-xs">
                        {exp.groupName}
                      </span>
                    )}
                  </div>
                </div>
                <span className="font-semibold text-gray-900 dark:text-gray-100 shrink-0 text-lg">
                  {formatCurrency(exp.amount)}
                </span>
              </Link>
            ))}
          </div>

          {hasMore && (
            <div className="mt-6 text-center">
              <button onClick={loadMore} className="btn-secondary">
                Load more
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
