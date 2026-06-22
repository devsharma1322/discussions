import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { useApolloClient, useMutation } from '@apollo/client/react';
import type { Subscription } from 'rxjs';
import { CIRCLES_QUERY, CREATE_CIRCLE_MUTATION } from '../graphql/circles';
import {
  GENERATE_DESCRIPTION_MUTATION,
  GENERATE_DESCRIPTION_STREAM_SUBSCRIPTION,
} from '../graphql/generate';
import { useToast } from './Toast';
import type { Circle, GenerateDescriptionResult, GenerateMode } from '../types/api';

interface CreateData { createCircle: Circle; }
interface GenerateData { generateDescription: GenerateDescriptionResult; }
interface GenerateStreamData { generateDescriptionStream: string; }

export function CreateCircleModal({ onClose }: { onClose: () => void }) {
  const { toast } = useToast();
  const client = useApolloClient();
  const [topic, setTopic] = useState('');
  const [description, setDescription] = useState('');
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [aiUnavailable, setAiUnavailable] = useState<string | null>(null);
  const [generating, setGenerating] = useState(false);
  const streamSubRef = useRef<Subscription | null>(null);

  // Tear down any in-flight stream if the modal unmounts mid-generation.
  useEffect(() => () => streamSubRef.current?.unsubscribe(), []);

  const [createCircle, { loading }] = useMutation<CreateData>(CREATE_CIRCLE_MUTATION, {
    refetchQueries: [
      { query: CIRCLES_QUERY, variables: { scope: 'ALL', sort: 'POPULAR', search: null, page: 1, limit: 10 } },
      { query: CIRCLES_QUERY, variables: { scope: 'MINE', sort: 'POPULAR', search: null, page: 1, limit: 10 } },
    ],
  });

  // Non-streaming fallback for environments where the WS subprotocol is blocked.
  const [generateDescriptionFallback] = useMutation<GenerateData>(GENERATE_DESCRIPTION_MUTATION);

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setSubmitError(null);
    try {
      await createCircle({
        variables: { topic: topic.trim(), description: description.trim() },
      });
      toast('Circle created.', 'success');
      onClose();
    } catch (err) {
      setSubmitError(extractMessage(err));
    }
  }

  function handleGenerate() {
    if (topic.trim().length < 3) {
      setSubmitError('Add a topic first (at least 3 characters) so the AI has context.');
      return;
    }
    setSubmitError(null);
    setAiUnavailable(null);

    const mode: GenerateMode = description.trim()
      ? 'FROM_TOPIC_AND_DESCRIPTION'
      : 'FROM_TOPIC';
    const variables = {
      topic: topic.trim(),
      description: description.trim() || null,
      mode,
    };

    setGenerating(true);
    setDescription('');

    let assembled = '';
    streamSubRef.current?.unsubscribe();
    streamSubRef.current = client
      .subscribe<GenerateStreamData>({
        query: GENERATE_DESCRIPTION_STREAM_SUBSCRIPTION,
        variables,
      })
      .subscribe({
        next(payload) {
          const chunk = payload.data?.generateDescriptionStream;
          if (chunk) {
            assembled += chunk;
            setDescription(assembled);
          }
        },
        error(err) {
          const isRateLimited =
            (err as { graphQLErrors?: { extensions?: { code?: string } }[] })
              .graphQLErrors?.[0]?.extensions?.code === 'RATE_LIMITED';
          if (isRateLimited) {
            setAiUnavailable("You've used today's AI quota. Try again later.");
            setGenerating(false);
            return;
          }
          // WS may be unavailable on this network (corporate proxies frequently
          // block the upgrade). Fall back to the non-streaming mutation so the
          // user still gets a description, just without progressive fill.
          fallbackGenerate(variables);
        },
        complete() {
          setGenerating(false);
        },
      });
  }

  async function fallbackGenerate(variables: {
    topic: string;
    description: string | null;
    mode: GenerateMode;
  }) {
    try {
      const result = await generateDescriptionFallback({ variables });
      const payload = result.data?.generateDescription;
      if (payload?.__typename === 'DescriptionGenerated') {
        setDescription(payload.text);
      } else if (payload?.__typename === 'DescriptionUnavailable') {
        setAiUnavailable(payload.message);
      } else {
        setAiUnavailable('AI is unavailable right now.');
      }
    } catch {
      setAiUnavailable('AI is unavailable right now.');
    } finally {
      setGenerating(false);
    }
  }

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center bg-purple-950/60 p-4 backdrop-blur-sm"
      onClick={onClose}
    >
      <div
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-md overflow-hidden rounded-2xl bg-white shadow-2xl ring-2 ring-purple-300"
      >
        <div className="bg-gradient-to-r from-purple-600 to-fuchsia-600 px-6 py-4 text-white">
          <h2 className="text-lg font-bold">✨ Create a circle</h2>
          <p className="mt-1 text-sm text-purple-100">You'll be the host. Topic is fixed after creation.</p>
        </div>

        <form className="space-y-4 p-6" onSubmit={handleSubmit} noValidate>
          <label className="block">
            <span className="block text-sm font-semibold text-purple-900">Topic</span>
            <input
              type="text"
              minLength={3}
              maxLength={80}
              required
              value={topic}
              onChange={(e) => setTopic(e.target.value)}
              className="mt-1 block w-full rounded-lg border border-purple-200 bg-amber-50/40 px-3 py-2 text-sm shadow-sm focus:border-purple-500 focus:outline-none focus:ring-2 focus:ring-purple-200"
            />
            <span className="mt-1 block text-xs text-purple-400">3–80 characters.</span>
          </label>
          <label className="block">
            <div className="flex items-center justify-between">
              <span className="block text-sm font-semibold text-purple-900">Description</span>
              <button
                type="button"
                onClick={handleGenerate}
                disabled={generating}
                className="rounded-full bg-amber-300 px-3 py-0.5 text-xs font-semibold text-amber-900 hover:bg-amber-200 disabled:opacity-60"
              >
                {generating ? '✨ Generating…' : '✨ Generate'}
              </button>
            </div>
            <textarea
              maxLength={300}
              required
              rows={4}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="mt-1 block w-full rounded-lg border border-purple-200 bg-amber-50/40 px-3 py-2 text-sm shadow-sm focus:border-purple-500 focus:outline-none focus:ring-2 focus:ring-purple-200"
            />
            <div className="mt-1 flex items-center justify-between">
              <span className="text-xs text-purple-400">{description.length} / 300</span>
              {aiUnavailable && (
                <span className="text-xs text-rose-600">{aiUnavailable}</span>
              )}
            </div>
          </label>
          {submitError && (
            <div className="rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-700 ring-1 ring-rose-200">
              {submitError}
            </div>
          )}
          <div className="flex justify-end gap-2 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="rounded-full border border-purple-200 px-4 py-1.5 text-sm font-medium text-purple-700 hover:bg-purple-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="rounded-full bg-gradient-to-r from-purple-600 to-fuchsia-600 px-5 py-1.5 text-sm font-semibold text-white shadow-sm transition hover:from-purple-700 hover:to-fuchsia-700 disabled:opacity-60"
            >
              {loading ? 'Creating…' : 'Create'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function extractMessage(err: unknown): string {
  if (err && typeof err === 'object' && 'message' in err && typeof err.message === 'string') {
    return err.message;
  }
  return 'Something went wrong.';
}
