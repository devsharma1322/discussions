import { gql } from '@apollo/client';

/**
 * GraphQL operations for threads and messages.
 *
 * Page size is locked at 10 server-side; the UI just passes it through.
 */

export const THREAD_FIELDS = gql`
  fragment ThreadFields on Thread {
    id
    circleId
    title
    createdBy
    createdAt
  }
`;

export const MESSAGE_FIELDS = gql`
  fragment MessageFields on Message {
    id
    threadId
    body
    author
    createdAt
  }
`;

export const THREADS_QUERY = gql`
  query Threads($circleId: UUID!, $page: Int, $limit: Int) {
    threads(circleId: $circleId, page: $page, limit: $limit) {
      total
      page
      limit
      data {
        ...ThreadFields
      }
    }
  }
  ${THREAD_FIELDS}
`;

export const MESSAGES_QUERY = gql`
  query Messages($threadId: UUID!, $page: Int, $limit: Int) {
    messages(threadId: $threadId, page: $page, limit: $limit) {
      total
      page
      limit
      data {
        ...MessageFields
      }
    }
  }
  ${MESSAGE_FIELDS}
`;

export const CREATE_THREAD_MUTATION = gql`
  mutation CreateThread($circleId: UUID!, $title: String!) {
    createThread(circleId: $circleId, title: $title) {
      ...ThreadFields
    }
  }
  ${THREAD_FIELDS}
`;

export const POST_MESSAGE_MUTATION = gql`
  mutation PostMessage($threadId: UUID!, $body: String!) {
    postMessage(threadId: $threadId, body: $body) {
      ...MessageFields
    }
  }
  ${MESSAGE_FIELDS}
`;
