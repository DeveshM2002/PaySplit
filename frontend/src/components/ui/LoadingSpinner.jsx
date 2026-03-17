export default function LoadingSpinner() {
  return (
    <div className="flex items-center justify-center min-h-[200px]">
      <div
        className="w-10 h-10 border-4 border-gray-200 dark:border-gray-700 border-t-[var(--color-primary)] rounded-full animate-spin"
        role="status"
        aria-label="Loading"
      />
    </div>
  );
}
