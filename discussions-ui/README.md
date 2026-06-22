# discussions-ui

React 18 + Vite + TypeScript + Apollo Client + Tailwind CSS for AnonCircles.

## Run locally

> **First time on this machine?** Follow **[../SETUP.md](../SETUP.md)** — installs Node 22, configures the BFF URL, and brings the backend up first (the UI needs the GraphQL endpoint reachable to bootstrap an anonymous session).

```bash
npm install
npm run dev
```

Dev server on `http://localhost:5173`. Point it at the BFF via `VITE_GRAPHQL_URL` in `.env` (default `http://localhost:4003/graphql`).

## Scripts

```bash
npm run dev       # vite dev
npm run build     # tsc + vite build
npm run preview   # preview the built bundle
npm run codegen   # regenerate typed GraphQL hooks under src/graphql/__generated__/
npm run lint
```

## Layout

```
src/
  App.tsx               React Router route map
  main.tsx              ApolloProvider + RouterProvider bootstrap
  index.css             Tailwind v4 entrypoint (@import "tailwindcss")
  lib/
    apolloClient.ts     authLink + errorLink + httpLink chain
    auth.ts             token storage helpers (localStorage today; cookie tomorrow)
  components/
    AuthGuard.tsx       Route-level auth gate
  pages/                Page components (one per route, added in later tickets)
  hooks/                Custom React hooks
  types/                Hand-written types
  graphql/              GraphQL operations + codegen output (__generated__/)
```
