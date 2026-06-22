import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation } from '@apollo/client/react';
import { UPDATE_CIRCLE_DESCRIPTION_MUTATION } from '../graphql/circles';
import { GENERATE_DESCRIPTION_MUTATION } from '../graphql/generate';
import { useToast } from './Toast';
import type { Circle, GenerateDescriptionResult, GenerateMode } from '../types/api';

interface UpdateData { updateCircleDescription: Circle; }
interface GenerateData { generateDescription: GenerateDescriptionResult; }

export function EditCircleModal({ circle, onClose }: { circle: Circle; onClose: () => void }) {
  const { toast } = useToast();
  const [description, setDescription] = useState(circle.description);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [aiUnavailable, setAiUnavailable] = useState<string | null>(null);

  const [updateCircle, { loading }] = useMutation<UpdateData>(UPDATE_CIRCLE_DESCRIPTION_MUTATION);
  const [generateDescription, { loading: generating }] = useMutation<GenerateData>(GENERATE_DESCRIPTION_MUTATION);

  if (!circle.isAdmin) {
    if (import.meta.env.DEV) {
      console.warn('[EditCircleModal] rendered for a non-admin viewer; bailing out.');
    }
    return null;
  }

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setSubmitError(null);
    try {
      await updateCircle({ variables: { id: circle.id, description: description.trim() } });
      toast('Description updated.', 'success');
      onClose();
    } catch (err) {
      setSubmitError(extractMessage(err));
    }
  }

  async function handleGenerate() {
    setAiUnavailable(null);
    const mode: GenerateMode = description.trim()
      ? 'FROM_TOPIC_AND_DESCRIPTION'
      : 'FROM_TOPIC';
    try {
      const result = await generateDescription({
        variables: { topic: circle.topic, description: description.trim() || null, mode },
      });
      const payload = result.data?.generateDescription;
      if (!payload) return;
      if (payload.__typename === 'DescriptionGenerated') {
        setDescription(payload.text);
      } else {
        setAiUnavailable(payload.message);
      }
    } catch {
      setAiUnavailable('AI is unavailable right now.');
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
        className="w-full max-w-md overflow-hidden rounded-2xl bg-white shadow-2xl ring-2 ring-indigo-300"
      >
        <div className="bg-gradient-to-r from-indigo-600 to-purple-600 px-6 py-4 text-white">
          <h2 className="text-lg font-bold">✏️ Edit circle</h2>
          <p className="mt-1 text-sm text-indigo-100">Only the description can change.</p>
        </div>

        <form className="space-y-4 p-6" onSubmit={handleSubmit} noValidate>
          <div>
            <span className="block text-sm font-semibold text-indigo-900">Topic</span>
            <div className="mt-1 flex items-center gap-2 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-900">
              <span aria-hidden>🔒</span>
              <span className="font-medium">{circle.topic}</span>
            </div>
            <span className="mt-1 block text-xs text-indigo-400">
              Topic is fixed once a circle is created.
            </span>
          </div>

          <label className="block">
            <div className="flex items-center justify-between">
              <span className="block text-sm font-semibold text-indigo-900">Description</span>
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
              maxLength={500}
              required
              rows={5}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="mt-1 block w-full rounded-lg border border-indigo-200 bg-white px-3 py-2 text-sm shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            />
            <div className="mt-1 flex items-center justify-between">
              <span className="text-xs text-indigo-400">{description.length} / 500</span>
              {aiUnavailable && <span className="text-xs text-rose-600">{aiUnavailable}</span>}
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
              className="rounded-full border border-indigo-200 px-4 py-1.5 text-sm font-medium text-indigo-700 hover:bg-indigo-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading || description.trim().length === 0}
              className="rounded-full bg-gradient-to-r from-indigo-600 to-purple-600 px-5 py-1.5 text-sm font-semibold text-white shadow-sm transition hover:from-indigo-700 hover:to-purple-700 disabled:opacity-60"
            >
              {loading ? 'Saving…' : 'Save'}
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
