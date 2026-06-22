import { useEffect, useState } from 'react';
import { useQuery } from '@apollo/client/react';
import { CIRCLES_QUERY } from '../graphql/circles';
import { useDebounced } from '../hooks/useDebounced';
import { CircleCard } from './CircleCard';
import { Pagination } from './Pagination';
import { SearchBar } from './SearchBar';
import type { CirclePage, CircleScope, CircleSort } from '../types/api';

interface CirclesData { circles: CirclePage; }

interface CircleFeedProps {
  scope: CircleScope;
  emptyTitle: string;
  emptyBody: string;
}

export function CircleFeed({ scope, emptyTitle, emptyBody }: CircleFeedProps) {
  const [search, setSearch] = useState('');
  const [sort, setSort] = useState<CircleSort>('POPULAR');
  const [page, setPage] = useState(1);

  const debouncedSearch = useDebounced(search.trim(), 300);

  useEffect(() => {
    setPage(1);
  }, [debouncedSearch, sort, scope]);

  const { data, loading, error } = useQuery<CirclesData>(CIRCLES_QUERY, {
    variables: {
      scope,
      sort,
      search: debouncedSearch || null,
      page,
      limit: 10,
    },
    fetchPolicy: 'cache-and-network',
  });

  const discoverHelper =
    scope === 'DISCOVER' && debouncedSearch
      ? 'Showing matches across all circles, including ones you have joined.'
      : undefined;

  return (
    <section className="p-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div className="flex-1">
          <SearchBar value={search} onChange={setSearch} helper={discoverHelper} />
        </div>
        <label className="text-sm font-medium text-purple-900">
          <span className="mr-2">Sort by</span>
          <select
            value={sort}
            onChange={(e) => setSort(e.target.value as CircleSort)}
            className="rounded-md border border-purple-200 bg-white px-2 py-1.5 text-sm shadow-sm focus:border-purple-500 focus:outline-none focus:ring-1 focus:ring-purple-500"
          >
            <option value="POPULAR">Popular</option>
            <option value="NEWEST">Newest</option>
          </select>
        </label>
      </div>

      {error && (
        <div className="mt-4 rounded-md bg-rose-50 px-3 py-2 text-sm text-rose-700 ring-1 ring-rose-200">
          Couldn't load circles right now.
        </div>
      )}

      {loading && !data ? (
        <SkeletonGrid />
      ) : data && data.circles.data.length === 0 ? (
        <EmptyState title={emptyTitle} body={emptyBody} />
      ) : data ? (
        <>
          <div className="mt-6 grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
            {data.circles.data.map((c) => (
              <CircleCard key={c.id} circle={c} />
            ))}
          </div>
          <Pagination
            page={data.circles.page}
            limit={data.circles.limit}
            total={data.circles.total}
            onChange={setPage}
          />
        </>
      ) : null}
    </section>
  );
}

function SkeletonGrid() {
  return (
    <div className="mt-6 grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
      {Array.from({ length: 6 }).map((_, i) => (
        <div
          key={i}
          className="h-40 animate-pulse rounded-2xl border-2 border-amber-100 bg-gradient-to-br from-yellow-50 to-amber-50 p-5"
        >
          <div className="h-5 w-1/3 rounded bg-amber-200/60" />
          <div className="mt-3 h-3 w-full rounded bg-amber-200/50" />
          <div className="mt-2 h-3 w-2/3 rounded bg-amber-200/50" />
        </div>
      ))}
    </div>
  );
}

function EmptyState({ title, body }: { title: string; body: string }) {
  return (
    <div className="mt-10 rounded-2xl border-2 border-dashed border-purple-200 bg-white/60 p-10 text-center shadow-sm">
      <h2 className="text-base font-semibold text-purple-900">{title}</h2>
      <p className="mt-1 text-sm text-purple-600/80">{body}</p>
    </div>
  );
}
