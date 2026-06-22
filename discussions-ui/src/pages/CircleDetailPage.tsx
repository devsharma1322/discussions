import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery } from '@apollo/client/react';
import {
  CIRCLES_QUERY,
  CIRCLE_QUERY,
  DELETE_CIRCLE_MUTATION,
  JOIN_CIRCLE_MUTATION,
  LEAVE_CIRCLE_MUTATION,
} from '../graphql/circles';
import { CREATE_THREAD_MUTATION, THREADS_QUERY } from '../graphql/threads';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { EditCircleModal } from '../components/EditCircleModal';
import { Pagination } from '../components/Pagination';
import { useToast } from '../components/Toast';
import type { Circle, Thread, ThreadPage } from '../types/api';

interface CircleData { circle: Circle | null; }
interface ThreadsData { threads: ThreadPage; }
interface CreateThreadData { createThread: Thread; }

export function CircleDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { toast } = useToast();
  const [page, setPage] = useState(1);
  const [showEdit, setShowEdit] = useState(false);
  const [showDelete, setShowDelete] = useState(false);

  const { data: circleData, loading: circleLoading } = useQuery<CircleData>(CIRCLE_QUERY, {
    variables: { id },
    skip: !id,
    fetchPolicy: 'cache-and-network',
  });

  const { data: threadsData, loading: threadsLoading } = useQuery<ThreadsData>(THREADS_QUERY, {
    variables: { circleId: id, page, limit: 10 },
    skip: !id,
    fetchPolicy: 'cache-and-network',
  });

  const [deleteCircle, { loading: deleting }] = useMutation(DELETE_CIRCLE_MUTATION, {
    refetchQueries: [
      { query: CIRCLES_QUERY, variables: { scope: 'ALL', sort: 'POPULAR', search: null, page: 1, limit: 10 } },
      { query: CIRCLES_QUERY, variables: { scope: 'MINE', sort: 'POPULAR', search: null, page: 1, limit: 10 } },
    ],
  });

  // Join/leave keep the cached Circle in sync via the mutation's own response
  // (the BFF returns the updated circle, including the new isMember flag),
  // so no refetchQueries needed on this page — the CIRCLE_QUERY entry in the
  // cache is updated automatically by normalized id. We DO refetch the
  // listing queries so navigating back to a feed shows the new membership.
  const listingRefetch = [
    { query: CIRCLES_QUERY, variables: { scope: 'ALL', sort: 'POPULAR', search: null, page: 1, limit: 10 } },
    { query: CIRCLES_QUERY, variables: { scope: 'DISCOVER', sort: 'POPULAR', search: null, page: 1, limit: 10 } },
    { query: CIRCLES_QUERY, variables: { scope: 'MINE', sort: 'POPULAR', search: null, page: 1, limit: 10 } },
  ];
  const [joinCircle, { loading: joining }] = useMutation(JOIN_CIRCLE_MUTATION, {
    refetchQueries: listingRefetch,
  });
  const [leaveCircle, { loading: leaving }] = useMutation(LEAVE_CIRCLE_MUTATION, {
    refetchQueries: listingRefetch,
  });

  if (!id) return null;
  if (circleLoading && !circleData) return <PageSkeleton />;

  const circle = circleData?.circle;
  if (!circle) {
    return (
      <div className="p-6">
        <p className="text-sm text-purple-700">Circle not found.</p>
        <Link to="/" className="text-fuchsia-600 hover:underline">
          ← Back to All Discussions
        </Link>
      </div>
    );
  }

  async function handleDelete() {
    try {
      await deleteCircle({ variables: { id: circle!.id } });
      toast('Circle deleted.', 'success');
      navigate('/my', { replace: true });
    } catch {
      toast('Couldn’t delete the circle.', 'error');
    } finally {
      setShowDelete(false);
    }
  }

  async function handleJoin() {
    try {
      await joinCircle({ variables: { id: circle!.id } });
      toast(`Joined ${circle!.topic}.`, 'success');
    } catch {
      toast('Couldn’t join the circle.', 'error');
    }
  }

  async function handleLeave() {
    try {
      await leaveCircle({ variables: { id: circle!.id } });
      toast(`Left ${circle!.topic}.`, 'success');
    } catch {
      toast('Couldn’t leave the circle.', 'error');
    }
  }

  return (
    <section className="p-6">
      <Link to="/" className="text-sm font-medium text-fuchsia-600 hover:underline">
        ← All Discussions
      </Link>

      <header className="mt-3 rounded-2xl border-2 border-purple-200 bg-gradient-to-br from-purple-50 via-white to-fuchsia-50 p-6 shadow-md">
        <div className="flex items-start justify-between gap-3">
          <h1 className="text-3xl font-bold tracking-tight text-purple-900">{circle.topic}</h1>
          <div className="flex items-center gap-2">
            {circle.isAdmin && (
              <span className="rounded-full bg-rose-500 px-3 py-1 text-xs font-semibold uppercase tracking-wide text-white shadow-sm">
                Host
              </span>
            )}
            {!circle.isAdmin && circle.isMember && (
              <span className="rounded-full bg-emerald-500 px-3 py-1 text-xs font-semibold uppercase tracking-wide text-white shadow-sm">
                Member
              </span>
            )}
            {circle.isAdmin && (
              <>
                <button
                  type="button"
                  onClick={() => setShowEdit(true)}
                  className="rounded-full border border-indigo-300 bg-white px-3 py-1 text-xs font-medium text-indigo-700 hover:bg-indigo-50"
                >
                  Edit
                </button>
                <button
                  type="button"
                  onClick={() => setShowDelete(true)}
                  className="rounded-full border border-rose-300 bg-white px-3 py-1 text-xs font-medium text-rose-700 hover:bg-rose-50"
                >
                  Delete
                </button>
              </>
            )}
            {!circle.isAdmin && !circle.isMember && (
              <button
                type="button"
                onClick={handleJoin}
                disabled={joining}
                className="rounded-full bg-gradient-to-r from-emerald-500 to-teal-500 px-4 py-1.5 text-xs font-semibold uppercase tracking-wide text-white shadow-sm transition hover:from-emerald-600 hover:to-teal-600 disabled:opacity-60"
              >
                {joining ? 'Joining…' : 'Join'}
              </button>
            )}
            {!circle.isAdmin && circle.isMember && (
              <button
                type="button"
                onClick={handleLeave}
                disabled={leaving}
                className="rounded-full border border-rose-300 bg-white px-3 py-1 text-xs font-medium text-rose-700 hover:bg-rose-50 disabled:opacity-60"
              >
                {leaving ? 'Leaving…' : 'Leave'}
              </button>
            )}
          </div>
        </div>
        <p className="mt-3 whitespace-pre-wrap text-base text-purple-800/90">{circle.description}</p>
        <p className="mt-4">
          <span className="rounded-full bg-amber-200/70 px-3 py-1 text-xs font-medium text-amber-900">
            👥 {circle.memberCount} {circle.memberCount === 1 ? 'member' : 'members'}
          </span>
        </p>
      </header>

      {(circle.isMember || circle.isAdmin) && <NewThreadForm circleId={circle.id} />}
      {!circle.isMember && !circle.isAdmin && (
        <div className="mt-6 rounded-2xl bg-amber-50 px-4 py-3 text-sm text-amber-900 ring-1 ring-amber-200">
          You can read threads here. Join the circle to start new ones or reply.
        </div>
      )}

      <h2 className="mt-8 mb-3 text-base font-semibold uppercase tracking-wide text-purple-800">
        Threads
      </h2>
      {threadsLoading && !threadsData ? (
        <ThreadListSkeleton />
      ) : !threadsData || threadsData.threads.data.length === 0 ? (
        <div className="rounded-2xl border-2 border-dashed border-purple-200 bg-white/60 p-6 text-center text-sm text-purple-600">
          No threads yet. Be the first to start one.
        </div>
      ) : (
        <>
          <ul className="space-y-2">
            {threadsData.threads.data.map((t) => (
              <li key={t.id}>
                <Link
                  to={`/threads/${t.id}`}
                  className="flex items-center justify-between rounded-xl border border-teal-200 bg-gradient-to-r from-teal-50 to-cyan-50 px-4 py-3 transition hover:border-teal-400 hover:shadow-sm"
                >
                  <span className="font-semibold text-teal-900">{t.title}</span>
                  <span className="text-xs text-teal-700">
                    by {t.createdBy} · {formatRelative(t.createdAt)}
                  </span>
                </Link>
              </li>
            ))}
          </ul>
          <Pagination
            page={threadsData.threads.page}
            limit={threadsData.threads.limit}
            total={threadsData.threads.total}
            onChange={setPage}
          />
        </>
      )}

      {showEdit && <EditCircleModal circle={circle} onClose={() => setShowEdit(false)} />}
      {showDelete && (
        <ConfirmDialog
          title="Delete this circle?"
          body={
            <>
              This permanently removes <span className="font-medium">{circle.topic}</span>{' '}
              and every thread and message inside it. This cannot be undone.
            </>
          }
          destructive
          confirmLabel="Delete"
          loading={deleting}
          onConfirm={handleDelete}
          onCancel={() => setShowDelete(false)}
        />
      )}
    </section>
  );
}

function NewThreadForm({ circleId }: { circleId: string }) {
  const [title, setTitle] = useState('');
  const [error, setError] = useState<string | null>(null);

  const [createThread, { loading }] = useMutation<CreateThreadData>(CREATE_THREAD_MUTATION, {
    refetchQueries: [{ query: THREADS_QUERY, variables: { circleId, page: 1, limit: 10 } }],
  });

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError(null);
    try {
      await createThread({ variables: { circleId, title: title.trim() } });
      setTitle('');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Couldn’t create thread.');
    }
  }

  return (
    <form onSubmit={handleSubmit} className="mt-6 flex gap-2" noValidate>
      <input
        type="text"
        value={title}
        onChange={(e) => setTitle(e.target.value)}
        minLength={3}
        maxLength={120}
        required
        placeholder="Start a new thread…"
        className="flex-1 rounded-full border border-teal-200 bg-white px-4 py-2 text-sm shadow-sm placeholder:text-teal-400 focus:border-teal-500 focus:outline-none focus:ring-2 focus:ring-teal-200"
      />
      <button
        type="submit"
        disabled={loading || title.trim().length < 3}
        className="rounded-full bg-teal-600 px-5 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-teal-700 disabled:opacity-60"
      >
        {loading ? 'Posting…' : 'Post'}
      </button>
      {error && <span className="ml-3 self-center text-xs text-rose-600">{error}</span>}
    </form>
  );
}

function ThreadListSkeleton() {
  return (
    <ul className="space-y-2">
      {Array.from({ length: 3 }).map((_, i) => (
        <li key={i} className="h-12 animate-pulse rounded-xl border border-teal-100 bg-white/60" />
      ))}
    </ul>
  );
}

function PageSkeleton() {
  return (
    <div className="p-6">
      <div className="h-6 w-24 animate-pulse rounded bg-purple-200/60" />
      <div className="mt-4 h-40 animate-pulse rounded-2xl bg-purple-100/40" />
    </div>
  );
}

function formatRelative(iso: string): string {
  const diffMs = Date.now() - new Date(iso).getTime();
  const sec = Math.round(diffMs / 1000);
  if (sec < 60) return `${sec}s ago`;
  const min = Math.round(sec / 60);
  if (min < 60) return `${min}m ago`;
  const hr = Math.round(min / 60);
  if (hr < 24) return `${hr}h ago`;
  return new Date(iso).toLocaleDateString();
}
