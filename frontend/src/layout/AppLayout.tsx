import { Button, Layout, Select, Spin } from 'antd';
import { Suspense, lazy } from 'react';
import { Outlet, useNavigate, useParams } from 'react-router';
import { useAuth } from '../auth/AuthProvider';
import { useTranslation } from 'react-i18next';

const { Header, Sider, Content } = Layout;

// design 10 S4.5: the tree (rc-tree + dropdown + modal) is NOT in the home bundle.
const PageTree = lazy(() => import('../features/tree/PageTree').then(m => ({ default: m.PageTree })));
const SearchBox = lazy(() => import('../features/search/SearchBox').then(m => ({ default: m.SearchBox })));

// Confluence-style layout (design 05 S3): space switcher, page-tree sider, content.
// Global search (W7), notifications (W8) and +New editor entry (W5) arrive later.
export function AppLayout() {
  const { account, spaces } = useAuth();
  const { slug } = useParams();
  const navigate = useNavigate();
  const { t } = useTranslation();
  const displayName = account ? `${account.firstName ?? ''}${account.lastName ?? ''}`.trim() || account.login : '';

  return (
    <Layout style={{ minHeight: '100vh' }}>
      {/* design 10 S4.6: light header (white + 1px divider), light sider */}
      <Header style={{ display: 'flex', alignItems: 'center', gap: 16, background: '#fff', borderBottom: '1px solid #f0f0f0', padding: '0 24px' }}>
        <span style={{ fontSize: 18, fontWeight: 600, cursor: 'pointer' }} onClick={() => navigate('/')}>
          DTS Wiki
        </span>
        <Select
          style={{ width: 220 }}
          placeholder="选择空间"
          value={slug}
          options={(spaces ?? []).map(s => ({ value: s.slug, label: s.name }))}
          onChange={value => navigate(`/s/${value}`)}
        />
        {slug && <Button onClick={() => navigate(`/s/${slug}/board`)}>{t('board.title')}</Button>}
        <Suspense><SearchBox /></Suspense>
        <span style={{ marginLeft: 'auto' }}>{displayName}</span>
      </Header>
      <Layout>
        {slug !== undefined && (
          <Sider width={300} theme="light" collapsible style={{ overflow: 'auto', height: 'calc(100vh - 64px)', background: '#fff' }}>
            <Suspense fallback={<Spin style={{ margin: 24 }} />}>
              <PageTree />
            </Suspense>
          </Sider>
        )}
        <Content style={{ padding: 24, background: '#f5f5f5', minHeight: 'calc(100vh - 64px)' }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
