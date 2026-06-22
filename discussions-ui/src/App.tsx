import { createBrowserRouter, Outlet } from 'react-router-dom';
import { AuthGuard } from './components/AuthGuard';
import { FeedTabs } from './components/FeedTabs';
import { IdentityBar } from './components/IdentityBar';
import { AllDiscussionsPage } from './pages/AllDiscussionsPage';
import { CircleDetailPage } from './pages/CircleDetailPage';
import { DiscoverPage } from './pages/DiscoverPage';
import { MyDiscussionsPage } from './pages/MyDiscussionsPage';
import { ThreadPage } from './pages/ThreadPage';

export const router = createBrowserRouter([
  {
    element: (
      <AuthGuard>
        <div className="min-h-full">
          <IdentityBar />
          <FeedTabs />
          <main className="mx-auto max-w-5xl">
            <Outlet />
          </main>
        </div>
      </AuthGuard>
    ),
    children: [
      { path: '/',            element: <AllDiscussionsPage /> },
      { path: '/discover',    element: <DiscoverPage /> },
      { path: '/my',          element: <MyDiscussionsPage /> },
      { path: '/circles/:id', element: <CircleDetailPage /> },
      { path: '/threads/:id', element: <ThreadPage /> },
    ],
  },
]);
