import { gql } from '@apollo/client';

/**
 * `generateDescription` mutation. The union return forces UI callers to
 * `__typename`-discriminate on success vs unavailable — compile-time safety
 * for the failure path.
 *
 * <p>Prefer {@link GENERATE_DESCRIPTION_STREAM_SUBSCRIPTION} in the UI: it
 * streams tokens over WebSocket as they arrive, so the textarea fills
 * progressively instead of staying frozen for the full latency. This mutation
 * is kept as a non-streaming fallback (and for callers without WS).
 */
export const GENERATE_DESCRIPTION_MUTATION = gql`
  mutation GenerateDescription(
    $topic: String!
    $description: String
    $mode: GenerateMode!
  ) {
    generateDescription(topic: $topic, description: $description, mode: $mode) {
      __typename
      ... on DescriptionGenerated {
        text
      }
      ... on DescriptionUnavailable {
        message
      }
    }
  }
`;

/**
 * Streaming subscription companion to {@link GENERATE_DESCRIPTION_MUTATION}.
 * Each event is a chunk to append to whatever the UI has rendered so far.
 * Stream completes when generation finishes; a GraphQL error event signals
 * UNAVAILABLE (same UX as the union's `DescriptionUnavailable` branch).
 */
export const GENERATE_DESCRIPTION_STREAM_SUBSCRIPTION = gql`
  subscription GenerateDescriptionStream(
    $topic: String!
    $description: String
    $mode: GenerateMode!
  ) {
    generateDescriptionStream(topic: $topic, description: $description, mode: $mode)
  }
`;
