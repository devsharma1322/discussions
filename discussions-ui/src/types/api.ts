/**
 * TypeScript types mirroring the GraphQL schema exposed by `discussions-graph`.
 *
 * Scalars and enums are re-exported from the codegen-generated module under
 * `src/graphql/__generated__/` so they stay in lock-step with the SDL. Entity
 * shapes are kept here because operations select different field sets and the
 * generated operation-types aren't entity-shaped; these interfaces describe
 * the canonical full-shape used by client-side caching, normalization, and
 * component props.
 *
 * To regenerate the underlying source-of-truth scalars/enums after a schema
 * change, run `npm run codegen`.
 */
import type {
  CircleScope as GqlCircleScope,
  CircleSort as GqlCircleSort,
  GenerateMode as GqlGenerateMode,
} from '../graphql/__generated__/graphql';

// ---------- Scalars ----------

export type UUID = string;
export type DateTime = string;

// ---------- Enums (re-exported from codegen output) ----------

export type CircleScope = GqlCircleScope;
export type CircleSort = GqlCircleSort;
export type GenerateMode = GqlGenerateMode;

// ---------- Object types ----------

/**
 * An anonymous user. The system identifies them by `id` (UUID) and shows
 * `displayName` (a generated pseudonym like `ShyOtter-42`) in their own UI.
 * Other users in a circle see a *different* per-circle handle, not this name.
 */
export interface User {
  id: UUID;
  displayName: string;
  createdAt: DateTime;
}

export interface AuthResult {
  engageAuth: string;
  user: User;
}

export interface Circle {
  id: UUID;
  topic: string;
  description: string;
  memberCount: number;
  adminUserId: UUID;
  isAdmin: boolean;
  isMember: boolean;
  createdAt: DateTime;
}

export interface Thread {
  id: UUID;
  circleId: UUID;
  title: string;
  createdBy: string;
  createdAt: DateTime;
}

export interface Message {
  id: UUID;
  threadId: UUID;
  body: string;
  author: string;
  createdAt: DateTime;
}

export interface Page<T> {
  data: T[];
  total: number;
  page: number;
  limit: number;
}

export type CirclePage = Page<Circle>;
export type ThreadPage = Page<Thread>;
export type MessagePage = Page<Message>;

export interface GenericResult {
  success: boolean;
}

// ---------- Unions ----------

export type GenerateDescriptionResult =
  | { __typename: 'DescriptionGenerated'; text: string }
  | { __typename: 'DescriptionUnavailable'; message: string };

// ---------- Mutation/query inputs (unchanged from auth flow) ----------

export interface CreateCircleInput {
  topic: string;
  description: string;
}

export interface UpdateCircleDescriptionInput {
  id: UUID;
  description: string;
}

export interface CreateThreadInput {
  circleId: UUID;
  title: string;
}

export interface PostMessageInput {
  threadId: UUID;
  body: string;
}

export interface GenerateDescriptionInput {
  topic: string;
  description?: string;
  mode: GenerateMode;
}

export interface CirclesQueryInput {
  scope: CircleScope;
  sort?: CircleSort;
  search?: string;
  page?: number;
  /** Hard-capped at 10 server-side; pass 1–10 from the UI. */
  limit?: number;
}

export const MAX_PAGE_LIMIT = 10;
