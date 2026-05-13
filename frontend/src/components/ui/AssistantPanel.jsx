import { useState } from 'react';
import { SparklesIcon, ChevronDownIcon, ChevronUpIcon, ExclamationTriangleIcon } from '@heroicons/react/24/outline';

export default function AssistantPanel({
  title = 'AI Insights',
  loading = false,
  error = null,
  narrative = null,
  citations = [],
  children,
  defaultOpen = false,
  onCitationClick,
  className = '',
}) {
  const [open, setOpen] = useState(defaultOpen);

  return (
    <div className={`rounded-xl border border-purple-200 dark:border-purple-800 bg-gradient-to-br from-purple-50 to-indigo-50 dark:from-purple-950/30 dark:to-indigo-950/30 overflow-hidden ${className}`}>
      <button
        onClick={() => setOpen(!open)}
        className="w-full flex items-center justify-between px-4 py-3 hover:bg-purple-100/50 dark:hover:bg-purple-900/20 transition-colors"
      >
        <div className="flex items-center gap-2">
          <SparklesIcon className="w-5 h-5 text-purple-600 dark:text-purple-400" />
          <span className="font-medium text-purple-900 dark:text-purple-100 text-sm">
            {title}
          </span>
        </div>
        {open ? (
          <ChevronUpIcon className="w-4 h-4 text-purple-600 dark:text-purple-400" />
        ) : (
          <ChevronDownIcon className="w-4 h-4 text-purple-600 dark:text-purple-400" />
        )}
      </button>

      {open && (
        <div className="px-4 pb-4">
          {loading && (
            <div className="flex items-center gap-3 py-4">
              <div className="w-5 h-5 border-2 border-purple-400 border-t-transparent rounded-full animate-spin" />
              <span className="text-sm text-purple-600 dark:text-purple-400">Analyzing...</span>
            </div>
          )}

          {error && !loading && (
            <div className="flex items-start gap-2 py-3 px-3 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800">
              <ExclamationTriangleIcon className="w-5 h-5 text-red-500 shrink-0 mt-0.5" />
              <p className="text-sm text-red-600 dark:text-red-400">{error}</p>
            </div>
          )}

          {!loading && !error && narrative && (
            <div className="prose prose-sm dark:prose-invert max-w-none text-gray-700 dark:text-gray-300">
              {narrative.split('\n').map((line, i) => (
                <p key={i} className="mb-1.5 last:mb-0 leading-relaxed">
                  {line}
                </p>
              ))}
            </div>
          )}

          {!loading && !error && children}

          {!loading && citations.length > 0 && (
            <div className="mt-3 pt-3 border-t border-purple-200 dark:border-purple-800">
              <p className="text-xs font-medium text-purple-600 dark:text-purple-400 mb-1.5">Sources</p>
              <div className="flex flex-wrap gap-1.5">
                {citations.map((c, i) => (
                  <button
                    key={i}
                    onClick={() => onCitationClick?.(c)}
                    className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-purple-100 dark:bg-purple-900/40 text-purple-700 dark:text-purple-300 text-xs hover:bg-purple-200 dark:hover:bg-purple-800/40 transition-colors"
                  >
                    <span className="capitalize">{c.type?.replace('_', ' ')}</span>
                    {c.label && <span className="truncate max-w-[120px]">: {c.label}</span>}
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
