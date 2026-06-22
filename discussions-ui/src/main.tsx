import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { ApolloProvider } from '@apollo/client/react';
import { RouterProvider } from 'react-router-dom';
import { router } from './App';
import { ToastHost } from './components/Toast';
import { WakingSplash } from './components/WakingSplash';
import { apolloClient } from './lib/apolloClient';
import './index.css';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <WakingSplash>
      <ApolloProvider client={apolloClient}>
        <ToastHost>
          <RouterProvider router={router} />
        </ToastHost>
      </ApolloProvider>
    </WakingSplash>
  </StrictMode>,
);
