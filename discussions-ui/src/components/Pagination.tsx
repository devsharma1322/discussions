interface PaginationProps {
  page: number;
  limit: number;
  total: number;
  onChange: (page: number) => void;
}

export function Pagination({ page, limit, total, onChange }: PaginationProps) {
  const totalPages = Math.max(1, Math.ceil(total / limit));
  const isFirst = page <= 1;
  const isLast = page >= totalPages;

  return (
    <nav
      aria-label="Pagination"
      className="mt-8 flex items-center justify-between border-t border-purple-100 pt-4 text-sm text-purple-900"
    >
      <button
        type="button"
        onClick={() => onChange(page - 1)}
        disabled={isFirst}
        className="rounded-full border border-purple-200 bg-white px-4 py-1 font-medium transition hover:bg-purple-50 disabled:opacity-40"
      >
        ← Prev
      </button>
      <span>
        Page <span className="font-semibold">{page}</span> of {totalPages}
        <span className="ml-2 text-purple-400">({total} total)</span>
      </span>
      <button
        type="button"
        onClick={() => onChange(page + 1)}
        disabled={isLast}
        className="rounded-full border border-purple-200 bg-white px-4 py-1 font-medium transition hover:bg-purple-50 disabled:opacity-40"
      >
        Next →
      </button>
    </nav>
  );
}
