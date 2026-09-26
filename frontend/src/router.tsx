import { createBrowserRouter } from 'react-router';
import { RequireAuth } from './auth/RequireAuth';
import { PageEditorPage } from './features/edit/PageEditorPage';
import { HomePage } from './features/home/HomePage';
import { SpaceHomePage } from './features/home/SpaceHomePage';
import { TrashPage } from './features/home/TrashPage';
import { PageView } from './features/page/PageView';
import { AppLayout } from './layout/AppLayout';
import { RoundtripRunner } from './roundtrip-tmp/Runner'; // TEMPORARY W5 acceptance, deleted before merge

// W5 routes (design 05 S2 subset): + edit/new. Conflict/history/search/admin arrive later.
export const router = createBrowserRouter([
  { path: '/__roundtrip', element: <RoundtripRunner /> }, // TEMPORARY W5 acceptance, deleted before merge
  {
    element: (
      <RequireAuth>
        <AppLayout />
      </RequireAuth>
    ),
    children: [
      { path: '/', element: <HomePage /> },
      { path: '/s/:slug', element: <SpaceHomePage /> },
      { path: '/s/:slug/p/:pageId', element: <PageView /> },
      { path: '/s/:slug/p/:pageId/edit', element: <PageEditorPage mode="edit" /> },
      { path: '/s/:slug/new', element: <PageEditorPage mode="new" /> },
      { path: '/s/:slug/trash', element: <TrashPage /> },
    ],
  },
]);
