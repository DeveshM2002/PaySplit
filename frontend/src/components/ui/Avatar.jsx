import { getInitials } from '../../utils/helpers';

const AVATAR_SIZES = {
  sm: 'w-8 h-8 text-xs',
  md: 'w-10 h-10 text-sm',
  lg: 'w-12 h-12 text-base',
};

const AVATAR_COLORS = [
  'bg-emerald-500',
  'bg-teal-500',
  'bg-cyan-500',
  'bg-blue-500',
  'bg-indigo-500',
  'bg-violet-500',
  'bg-purple-500',
  'bg-pink-500',
];

function hashNameToColor(name) {
  if (!name) return AVATAR_COLORS[0];
  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash);
  }
  const index = Math.abs(hash) % AVATAR_COLORS.length;
  return AVATAR_COLORS[index];
}

export default function Avatar({ name, avatarUrl, size = 'md' }) {
  const sizeClasses = AVATAR_SIZES[size] || AVATAR_SIZES.md;
  const initials = getInitials(name);

  if (avatarUrl) {
    return (
      <img
        src={avatarUrl}
        alt={name || 'Avatar'}
        className={`${sizeClasses} rounded-full object-cover ring-2 ring-white dark:ring-gray-800`}
      />
    );
  }

  return (
    <div
      className={`${sizeClasses} ${hashNameToColor(name)} rounded-full flex items-center justify-center font-medium text-white ring-2 ring-white dark:ring-gray-800`}
    >
      {initials}
    </div>
  );
}
