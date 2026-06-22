interface SearchBarProps {
  value: string;
  onChange: (value: string) => void;
  helper?: string;
  placeholder?: string;
}

export function SearchBar({
  value,
  onChange,
  helper,
  placeholder = 'Search by topic or description',
}: SearchBarProps) {
  return (
    <div className="w-full">
      <div className="relative">
        <span aria-hidden className="absolute inset-y-0 left-3 flex items-center text-purple-400">
          🔍
        </span>
        <input
          type="search"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          maxLength={80}
          placeholder={placeholder}
          className="w-full rounded-full border border-purple-200 bg-white py-2 pl-9 pr-9 text-sm shadow-sm placeholder:text-purple-300 focus:border-purple-500 focus:outline-none focus:ring-2 focus:ring-purple-200"
        />
        {value && (
          <button
            type="button"
            aria-label="Clear search"
            onClick={() => onChange('')}
            className="absolute inset-y-0 right-3 flex items-center text-purple-400 hover:text-purple-700"
          >
            ✕
          </button>
        )}
      </div>
      {helper && <p className="mt-1 text-xs italic text-teal-700">{helper}</p>}
    </div>
  );
}
