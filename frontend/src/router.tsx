import { lazy, Suspense } from 'react';
import { Spin } from 'antd';
import { createBrowserRouter } from 'react-router';
import { RequireAuth } from './auth/RequireAuth';
import { HomePage } from './features/home/HomePage';
import { SpaceHomePage } from './features/home/SpaceHomePage';
import { AppLayout } from './layout/AppLayout';

// Route-level splitting (design 10 S4.5): heavy pages load on demand;
// the editor (+Milkdown/CodeMirror) only loads on edit routes.
const PageView = lazy(() => import('./features/page/PageView').then(m => ({ default: m.PageView })));
const PageEditorPage = lazy(() => import('./features/edit/PageEditorPage').then(m => ({ default: m.PageEditorPage })));
const TrashPage = lazy(() => import('./features/home/TrashPage').then(m => ({ default: m.TrashPage })));
const SprintBoardPage = lazy(() => import('./features/query/SprintBoardPage').then(m => ({ default: m.SprintBoardPage })));
const SearchPage = lazy(() => import('./features/search/SearchPage').then(m => ({ default: m.SearchPage })));
const LegacyPage = lazy(() => import('./features/page/LegacyPage').then(m => ({ default: m.LegacyPage })));

function Suspended({ children }: { children: React.ReactNode }) {
  return <Suspense fallback={<Spin fullscreen description="Loading" />}>{children}</Suspense>;
}

const editElement = (mode: 'edit' | 'new') => (
  <Suspended>
    <PageEditorPage mode={mode} />
  </Suspended>
);

// Business routes share the authenticated application shell.
export const router = createBrowserRouter([
  {
    element: (
      <RequireAuth>
        <AppLayout />
      </RequireAuth>
    ),
    children: [
      { path: '/', element: <HomePage /> },
      { path: '/search', element: <Suspended><SearchPage /></Suspended> },
      { path: '/p/:legacySpace/*', element: <Suspended><LegacyPage /></Suspended> },
      { path: '/s/:slug', element: <SpaceHomePage /> },
      {
        path: '/s/:slug/p/:pageId',
        element: (
          <Suspended>
            <PageView />
          </Suspended>
        ),
      },
      { path: '/s/:slug/p/:pageId/edit', element: editElement('edit') },
      { path: '/s/:slug/new', element: editElement('new') },
      { path: '/s/:slug/board', element: <Suspended><SprintBoardPage /></Suspended> },
      {
        path: '/s/:slug/trash',
        element: (
          <Suspended>
            <TrashPage />
          </Suspended>
        ),
      },
    ],
  },
]);
