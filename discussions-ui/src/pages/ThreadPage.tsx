import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery } from '@apollo/client/react';
import { MESSAGES_QUERY, POST_MESSAGE_MUTATION } from '../graphql/threads';
import { Pagination } from '../components/Pagination';
import type { Message, MessagePage } from '../types/api';

interface MessagesData { messages: MessagePage; }
interface PostMessageData { postMessage: Message; }

export function ThreadPage() {
  const { id } = useParams<{ id: string }>();
  const [page, setPage] = useState(1);
  const [body, setBody] = useState('');
  const [submitError, setSubmitError] = useState<string | null>(null);
  const listEndRef = useRef<HTMLDivElement>(null);

  const { data, loading, error, refetch } = useQuery<MessagesData>(MESSAGES_QUERY, {
    variables: { threadId: id, page, limit: 10 },
    skip: !id,
    fetchPolicy: 'cache-and-network',
  });

  const [postMessage, { loading: posting }] = useMutation<PostMessageData>(POST_MESSAGE_MUTATION);

  useEffect(() => {
    listEndRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' });
  }, [data?.messages.data.length]);

  if (!id) return null;

  if (error) {
    const code = (error as { graphQLErrors?: { extensions?: { code?: string } }[] })
      .graphQLErrors?.[0]?.extensions?.code;
    if (code === 'FORBIDDEN') {
      return (
        <div className="p-6">
          <p className="text-sm text-purple-700">
            You need to join the circle before you can read this thread.
          </p>
          <Link to="/discover" className="mt-2 inline-block text-teal-700 hover:underline">
            Find a circle to join →
          </Link>
        </div>
      );
    }
  }

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setSubmitError(null);
    const trimmed = body.trim();
    if (!trimmed) return;
    try {
      await postMessage({ variables: { threadId: id, body: trimmed } });
      setBody('');
      const total = (data?.messages.total ?? 0) + 1;
      const lastPage = Math.max(1, Math.ceil(total / 10));
      if (lastPage !== page) {
        setPage(lastPage);
      } else {
        await refetch();
      }
    } catch (err) {
      setSubmitError(err instanceof Error ? err.message : 'Couldn’t post.');
    }
  }

  return (
    <section className="p-6">
      <Link to="/" className="text-sm font-medium text-fuchsia-600 hover:underline">
        ← Back to feed
      </Link>

      <h1 className="mt-3 text-2xl font-bold tracking-tight text-purple-900">Thread</h1>

      {loading && !data ? (
        <MessageListSkeleton />
      ) : data && data.messages.data.length > 0 ? (
        <>
          <ol className="mt-4 space-y-3">
            {data.messages.data.map((m) => (
              <li
                key={m.id}
                className="rounded-2xl border border-purple-100 bg-white p-3 shadow-sm"
              >
                <div className="flex items-center justify-between text-xs">
                  <span className="rounded-full bg-fuchsia-100 px-2 py-0.5 font-semibold text-fuchsia-800">
                    {m.author}
                  </span>
                  <span className="text-purple-400">{formatRelative(m.createdAt)}</span>
                </div>
                <p className="mt-2 whitespace-pre-wrap text-sm text-gray-800">{m.body}</p>
              </li>
            ))}
          </ol>
          <div ref={listEndRef} />
          <Pagination
            page={data.messages.page}
            limit={data.messages.limit}
            total={data.messages.total}
            onChange={setPage}
          />
        </>
      ) : (
        <div className="mt-4 rounded-2xl border-2 border-dashed border-purple-200 bg-white/60 p-6 text-center text-sm text-purple-500">
          No messages yet — say something.
        </div>
      )}

      <form onSubmit={handleSubmit} className="mt-6 flex gap-2" noValidate>
        <textarea
          value={body}
          onChange={(e) => setBody(e.target.value)}
          maxLength={2000}
          rows={2}
          placeholder="Write a message…"
          className="flex-1 rounded-2xl border border-purple-200 bg-white px-4 py-2 text-sm shadow-sm placeholder:text-purple-300 focus:border-purple-500 focus:outline-none focus:ring-2 focus:ring-purple-200"
        />
        <button
          type="submit"
          disabled={posting || body.trim().length === 0}
          className="self-end rounded-full bg-gradient-to-r from-fuchsia-600 to-purple-600 px-5 py-2 text-sm font-semibold text-white shadow-sm transition hover:from-fuchsia-700 hover:to-purple-700 disabled:opacity-60"
        >
          {posting ? 'Sending…' : 'Send'}
        </button>
      </form>
      {submitError && <p className="mt-2 text-xs text-rose-600">{submitError}</p>}
      <p className="mt-2 text-xs text-purple-400">{body.length} / 2000</p>
    </section>
  );
}

function MessageListSkeleton() {
  return (
    <ol className="mt-4 space-y-3">
      {Array.from({ length: 3 }).map((_, i) => (
        <li key={i} className="h-16 animate-pulse rounded-2xl border border-purple-100 bg-white/70" />
      ))}
    </ol>
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
