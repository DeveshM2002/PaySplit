import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { PencilIcon, TrashIcon } from '@heroicons/react/24/outline';
import { expenseApi } from '../../api/expenses';
import useAuthStore from '../../store/authStore';
import {
  formatCurrency,
  formatDate,
  getCategoryIcon,
} from '../../utils/helpers';
import LoadingSpinner from '../../components/ui/LoadingSpinner';

export default function ExpenseDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const [expense, setExpense] = useState(null);
  const [comments, setComments] = useState([]);
  const [commentText, setCommentText] = useState('');
  const [loading, setLoading] = useState(true);
  const [submittingComment, setSubmittingComment] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState(null);

  const isPayer = user?.id && expense?.paidById === user.id;

  useEffect(() => {
    const load = async () => {
      if (!id) return;
      try {
        setLoading(true);
        setError(null);
        const [expRes, comRes] = await Promise.all([
          expenseApi.getById(id),
          expenseApi.getComments(id).catch(() => ({ data: [] })),
        ]);
        setExpense(expRes.data?.expense ?? expRes.data);
        setComments(comRes.data?.comments ?? comRes.data ?? []);
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to load expense');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [id]);

  const handleAddComment = async (e) => {
    e.preventDefault();
    if (!commentText.trim()) return;
    try {
      setSubmittingComment(true);
      const response = await expenseApi.addComment(id, { text: commentText.trim() });
      const newComment = response.data?.comment ?? response.data;
      setComments((prev) => [...prev, newComment]);
      setCommentText('');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to add comment');
    } finally {
      setSubmittingComment(false);
    }
  };

  const handleDelete = async () => {
    if (!confirm('Are you sure you want to delete this expense?')) return;
    try {
      setDeleting(true);
      await expenseApi.delete(id);
      navigate(-1);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete expense');
    } finally {
      setDeleting(false);
    }
  };

  if (loading && !expense) {
    return <LoadingSpinner />;
  }

  if (error && !expense) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-8">
        <div className="p-4 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-600 dark:text-red-400">
          {error}
        </div>
      </div>
    );
  }

  if (!expense) {
    return null;
  }

  const splits = expense.splits ?? expense.splitsList ?? [];
  const paidBy = expense.paidBy ?? expense.paidByName;

  return (
    <div className="max-w-2xl mx-auto px-4 py-8">
      <div className="card mb-6">
        <div className="flex items-start justify-between gap-4">
          <div className="flex items-center gap-4">
            <span className="text-4xl">{getCategoryIcon(expense.category)}</span>
            <div>
              <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">
                {expense.description}
              </h1>
              <p className="text-lg font-semibold text-[var(--color-primary)] mt-1">
                {formatCurrency(expense.amount)}
              </p>
              <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
                Paid by {typeof paidBy === 'object' ? paidBy?.name : paidBy} • {formatDate(expense.date)}
              </p>
            </div>
          </div>
          {isPayer && (
            <div className="flex gap-2">
              <button
                onClick={() => navigate(`/expenses/${id}/edit`)}
                className="btn-secondary p-2"
                aria-label="Edit"
              >
                <PencilIcon className="w-5 h-5" />
              </button>
              <button
                onClick={handleDelete}
                disabled={deleting}
                className="btn-danger p-2"
                aria-label="Delete"
              >
                <TrashIcon className="w-5 h-5" />
              </button>
            </div>
          )}
        </div>
      </div>

      {error && (
        <div className="mb-6 p-4 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-600 dark:text-red-400">
          {error}
        </div>
      )}

      <div className="card mb-6">
        <h2 className="font-semibold text-gray-900 dark:text-gray-100 mb-4">
          Split details
        </h2>
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-gray-200 dark:border-gray-700">
                <th className="text-left py-2 text-sm font-medium text-gray-500 dark:text-gray-400">
                  Person
                </th>
                <th className="text-right py-2 text-sm font-medium text-gray-500 dark:text-gray-400">
                  Amount
                </th>
                {expense.splitType === 'PERCENTAGE' && (
                  <th className="text-right py-2 text-sm font-medium text-gray-500 dark:text-gray-400">
                    %
                  </th>
                )}
              </tr>
            </thead>
            <tbody>
              {splits.map((s, i) => (
                <tr
                  key={s.id ?? i}
                  className="border-b border-gray-100 dark:border-gray-700 last:border-0"
                >
                  <td className="py-3 text-gray-900 dark:text-gray-100">
                    {s.user?.name ?? s.userName ?? 'Unknown'}
                  </td>
                  <td className="py-3 text-right font-medium">
                    {formatCurrency(s.amount ?? 0)}
                  </td>
                  {expense.splitType === 'PERCENTAGE' && (
                    <td className="py-3 text-right text-gray-500">
                      {s.percentage ?? 0}%
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="card">
        <h2 className="font-semibold text-gray-900 dark:text-gray-100 mb-4">
          Comments
        </h2>
        <form onSubmit={handleAddComment} className="mb-6">
          <div className="flex gap-2">
            <input
              type="text"
              value={commentText}
              onChange={(e) => setCommentText(e.target.value)}
              className="input-field flex-1"
              placeholder="Add a comment..."
            />
            <button
              type="submit"
              disabled={submittingComment || !commentText.trim()}
              className="btn-primary shrink-0"
            >
              {submittingComment ? '...' : 'Post'}
            </button>
          </div>
        </form>
        <div className="space-y-4">
          {comments.length === 0 ? (
            <p className="text-sm text-gray-500 dark:text-gray-400">
              No comments yet.
            </p>
          ) : (
            comments.map((c) => (
              <div
                key={c.id}
                className="flex gap-3 p-3 rounded-lg bg-gray-50 dark:bg-gray-800/50"
              >
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                    {c.user?.name ?? c.author?.name ?? 'Unknown'}
                  </p>
                  <p className="text-gray-600 dark:text-gray-300 mt-0.5">
                    {c.text ?? c.content}
                  </p>
                  <p className="text-xs text-gray-500 mt-1">
                    {formatDate(c.createdAt ?? c.date)}
                  </p>
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
