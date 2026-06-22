import { ApolloClient, InMemoryCache, HttpLink, from, split } from '@apollo/client';
import { setContext } from '@apollo/client/link/context';
import { onError } from '@apollo/client/link/error';
import { GraphQLWsLink } from '@apollo/client/link/subscriptions';
import { getMainDefinition } from '@apollo/client/utilities';
import { createClient } from 'graphql-ws';
import { getToken } from './auth';

const GRAPHQL_URL = import.meta.env.VITE_GRAPHQL_URL ?? 'http://localhost:4003/graphql';

const WS_URL = GRAPHQL_URL.replace(/^http/, 'ws');

// (1) authLink — reads engage-auth from storage and sets the Bearer header.
const authLink = setContext((_op, { headers }) => {
  const token = getToken();
  return {
    headers: {
      ...headers,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  };
});

// (2) errorLink — anonymous flow: there is no /login page, so UNAUTHENTICATED
// is handled by `useAuth` bootstrapping a fresh session. We just log here.
const errorLink = onError(({ error }) => {
  if (import.meta.env.DEV) {
    // eslint-disable-next-line no-console
    console.debug('[apollo] link error:', error);
  }
});

// (3) httpLink — points at the BFF for queries/mutations.
const httpLink = new HttpLink({ uri: GRAPHQL_URL });

// (4) wsLink — graphql-ws subprotocol for Subscription.generateDescriptionStream.
// Browsers can't set custom WS upgrade headers, so the engage-auth JWT is sent
// in the connection_init payload (parsed server-side by WebSocketAuthInterceptor).
// connectionParams is called per-connection, so a token issued *after* the
// client was constructed (e.g. by useAuth's startSession) is still picked up.
const wsLink = new GraphQLWsLink(
  createClient({
    url: WS_URL,
    connectionParams: () => {
      const token = getToken();
      return token ? { Authorization: `Bearer ${token}` } : {};
    },
    lazy: true,
    retryAttempts: 3,
  }),
);

// (5) Route subscriptions through the WS link, everything else through HTTP.
const splitLink = split(
  ({ query }) => {
    const def = getMainDefinition(query);
    return def.kind === 'OperationDefinition' && def.operation === 'subscription';
  },
  wsLink,
  from([authLink, errorLink, httpLink]),
);

export const apolloClient = new ApolloClient({
  link: splitLink,
  cache: new InMemoryCache(),
});

