import { clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs) {
  return twMerge(clsx(inputs));
}

export function formatCurrency(amount, currency = 'INR') {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
  }).format(amount);
}

export function formatDate(date) {
  if (!date) return '';
  return new Date(date).toLocaleDateString('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
}

export function formatRelativeTime(date) {
  if (!date) return '';
  const now = new Date();
  const then = new Date(date);
  const diffMs = now - then;
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);

  if (diffMins < 1) return 'just now';
  if (diffMins < 60) return `${diffMins}m ago`;
  if (diffHours < 24) return `${diffHours}h ago`;
  if (diffDays < 7) return `${diffDays}d ago`;
  return formatDate(date);
}

export function getInitials(name) {
  if (!name) return '?';
  return name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
}

export function getCategoryIcon(category) {
  const icons = {
    FOOD: '🍕',
    TRANSPORT: '🚗',
    RENT: '🏠',
    UTILITIES: '💡',
    ENTERTAINMENT: '🎬',
    SHOPPING: '🛍️',
    HEALTHCARE: '🏥',
    EDUCATION: '📚',
    TRAVEL: '✈️',
    GROCERIES: '🛒',
    SUBSCRIPTIONS: '📱',
    OTHER: '📋',
  };
  return icons[category] || '📋';
}

export const EXPENSE_CATEGORIES = [
  { value: 'FOOD', label: 'Food & Dining' },
  { value: 'TRANSPORT', label: 'Transport' },
  { value: 'RENT', label: 'Rent' },
  { value: 'UTILITIES', label: 'Utilities' },
  { value: 'ENTERTAINMENT', label: 'Entertainment' },
  { value: 'SHOPPING', label: 'Shopping' },
  { value: 'HEALTHCARE', label: 'Healthcare' },
  { value: 'EDUCATION', label: 'Education' },
  { value: 'TRAVEL', label: 'Travel' },
  { value: 'GROCERIES', label: 'Groceries' },
  { value: 'SUBSCRIPTIONS', label: 'Subscriptions' },
  { value: 'OTHER', label: 'Other' },
];

export const SPLIT_TYPES = [
  { value: 'EQUAL', label: 'Split Equally' },
  { value: 'EXACT', label: 'Exact Amounts' },
  { value: 'PERCENTAGE', label: 'By Percentage' },
];
