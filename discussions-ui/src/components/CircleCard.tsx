import { useMutation } from '@apollo/client/react';
import { Link } from 'react-router-dom';
import type { Circle } from '../types/api';
import {
  JOIN_CIRCLE_MUTATION,
  LEAVE_CIRCLE_MUTATION,
} from '../graphql/circles';

interface JoinData { joinCircle: Circle; }
interface LeaveData { leaveCircle: Circle; }

/**
 * Warm yellow card with an amber border. Host pill is rose; Join button is
 * indigo; Leave button is white-on-border. Hover lifts the card slightly.
 */
export function CircleCard({ circle }: { circle: Circle }) {
  const [joinCircle, { loading: joining }] = useMutation<JoinData>(JOIN_CIRCLE_MUTATION);
  const [leaveCircle, { loading: leaving }] = useMutation<LeaveData>(LEAVE_CIRCLE_MUTATION);

  function handleJoin() {
    joinCircle({
      variables: { id: circle.id },
      optimisticResponse: {
        joinCircle: { ...circle, isMember: true, memberCount: circle.memberCount + 1 },
      },
    }).catch(() => {});
  }

  function handleLeave() {
    leaveCircle({
      variables: { id: circle.id },
      optimisticResponse: {
        leaveCircle: {
          ...circle,
          isMember: false,
          memberCount: Math.max(0, circle.memberCount - 1),
        },
      },
    }).catch(() => {});
  }

  return (
    <article className="flex flex-col rounded-2xl border-2 border-amber-200 bg-gradient-to-br from-yellow-50 to-amber-100 p-5 shadow-sm transition hover:-translate-y-0.5 hover:border-amber-300 hover:shadow-md">
      <header className="flex items-start justify-between gap-3">
        <Link
          to={`/circles/${circle.id}`}
          className="text-lg font-bold tracking-tight text-amber-950 hover:text-purple-700"
        >
          {circle.topic}
        </Link>
        {circle.isAdmin && (
          <span className="rounded-full bg-rose-500 px-2.5 py-0.5 text-xs font-semibold uppercase tracking-wide text-white shadow-sm">
            Host
          </span>
        )}
      </header>
      <p className="mt-2 line-clamp-3 text-sm text-amber-900/80">
        {circle.description}
      </p>
      <footer className="mt-4 flex items-center justify-between text-sm">
        <span className="rounded-full bg-amber-200/70 px-2.5 py-0.5 text-xs font-medium text-amber-900">
          �� {circle.memberCount} {circle.memberCount === 1 ? 'member' : 'members'}
        </span>
        {circle.isAdmin ? (
          <span className="text-xs italic text-amber-700">You host this circle</span>
        ) : circle.isMember ? (
          <button
            type="button"
            onClick={handleLeave}
            disabled={leaving}
            className="rounded-full border border-amber-400 bg-white px-3 py-1 font-medium text-amber-900 transition hover:bg-amber-50 disabled:opacity-60"
          >
            {leaving ? 'Leaving…' : 'Leave'}
          </button>
        ) : (
          <button
            type="button"
            onClick={handleJoin}
            disabled={joining}
            className="rounded-full bg-indigo-600 px-3 py-1 font-semibold text-white shadow-sm transition hover:bg-indigo-700 disabled:opacity-60"
          >
            {joining ? 'Joining…' : 'Join'}
          </button>
        )}
      </footer>
    </article>
  );
}
