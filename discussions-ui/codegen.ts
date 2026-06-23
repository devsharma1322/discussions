import type { CodegenConfig } from '@graphql-codegen/cli';

/**
 * Reads the SDL directly from `discussions-graph` so codegen runs without a
 * live server. Works in both local dev and Vercel builds where the monorepo
 * is fully checked out and the `../discussions-graph/...` path resolves.
 *
 * Set CODEGEN_SCHEMA_URL to override (e.g. for introspection over HTTP).
 */
const config: CodegenConfig = {
  schema:
    process.env.CODEGEN_SCHEMA_URL ??
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
