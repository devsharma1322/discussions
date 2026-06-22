import type { CodegenConfig } from '@graphql-codegen/cli';

/**
 * Reads the SDL directly from `discussions-graph` so codegen runs without a
 * live server (CI-friendly). Set `VITE_GRAPHQL_URL` to switch to introspection
 * over HTTP if needed.
 */
const config: CodegenConfig = {
  schema:
    process.env.VITE_GRAPHQL_URL ??
    '../discussions-graph/src/main/resources/graphql/schema.graphqls',
  documents: ['src/**/*.{ts,tsx}', '!src/graphql/__generated__/**'],
  generates: {
    './src/graphql/__generated__/': {
      preset: 'client',
      config: {
        useTypeImports: true,
        scalars: {
          UUID: 'string',
          DateTime: 'string',
        },
      },
    },
  },
  ignoreNoDocuments: true,
};

export default config;
