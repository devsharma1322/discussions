import { Link, useLocation } from 'react-router-dom';

/**
 * Three color-coded tabs:
 *   All       → indigo
 *   Discover  → teal
 *   My        → emerald
 *
 * Active tab uses its accent color as a thick underline + matching text.
 * Inactive tabs are subdued grey.
 */
const TABS: Array<{ to: string; label: string; accent: string; active: string }> = [
  { to: '/',         label: 'All',            accent: 'indigo',  active: 'border-indigo-500 text-indigo-700' },
  { to: '/discover', label: 'Discover',       accent: 'teal',    active: 'border-teal-500 text-teal-700' },
  { to: '/my',       label: 'My Discussions', accent: 'emerald', active: 'border-emerald-500 text-emerald-700' },
];

export function FeedTabs() {
  const { pathname } = useLocation();
  return (
    <div className="border-b border-purple-100/80 bg-white/70 backdrop-blur">
      <nav aria-label="Feeds" className="mx-auto flex max-w-5xl gap-1 px-4">
        {TABS.map((tab) => {
          const isActive = pathname === tab.to;
          return (
            <Link
              key={tab.to}
              to={tab.to}
              className={
                'border-b-[3px] px-4 py-3 text-sm font-semibold transition ' +
                (isActive
                  ? tab.active
                  : 'border-transparent text-gray-500 hover:text-gray-800')
              }
            >
              {tab.label}
            </Link>
          );
        })}
      </nav>
    </div>
  );
}
