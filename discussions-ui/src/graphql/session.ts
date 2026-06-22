import { gql } from '@apollo/client';

/**
 * Session GraphQL operations for the anonymous flow.
 *
 * - `START_SESSION_MUTATION` is what the AuthGuard fires when no token is
 *   present. It creates a new anonymous user server-side and returns the
 *   engage-auth JWT plus a friendly display name.
 * - `ME_QUERY` resolves the current user from an existing JWT.
 * - `LOGOUT_MUTATION` is a best-effort signal — the client also clears local
 *   storage. "Logout" in this app really means "switch identity".
 */

export const ME_QUERY = gql`
  query Me {
    me {
      id
      displayName
      createdAt
    }
  }
`;

export const START_SESSION_MUTATION = gql`
  mutation StartSession {
    startSession {
      engageAuth
      user {
        id
        displayName
        createdAt
      }
    }
  }
`;

export const LOGOUT_MUTATION = gql`
  mutation Logout {
    logout {
      success
    }
  }
`;
