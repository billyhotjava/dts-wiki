import { Layout, Select } from 'antd';
import { Outlet, useNavigate, useParams } from 'react-router';
import { useAuth } from '../auth/AuthProvider';
import { useSpaces } from '../api/hooks';
import { PageTree } from '../features/tree/PageTree';

const { Header, Sider, Content } = Layout;

// Confluence-style layout (design 05 S3): space switcher, page-tree sider, content.
// Global search (W7), notifications (W8) and +New editor entry (W5) arrive later.
export function AppLayout() {
  const { account } = useAuth();
  const { slug } = useParams();
  const navigate = useNavigate();
  const { data: spaces } = useSpaces();
  const displayName = account ? `${account.firstName ?? ''}${account.lastName ?? ''}`.trim() || account.login : '';

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
        <span style={{ color: '#fff', fontSize: 18, cursor: 'pointer' }} onClick={() => navigate('/')}>
          DTS Wiki
        </span>
        <Select
          style={{ width: 220 }}
          placeholder="选择空间"
          value={slug}
          options={(spaces ?? []).map(s => ({ value: s.slug, label: s.name }))}
          onChange={value => navigate(`/s/${value}`)}
        />
        <span style={{ marginLeft: 'auto', color: '#fff' }}>{displayName}</span>
      </Header>
      <Layout>
        {slug !== undefined && (
          <Sider width={300} theme="light" collapsible style={{ overflow: 'auto', height: 'calc(100vh - 64px)' }}>
            <PageTree />
          </Sider>
        )}
        <Content style={{ padding: 24, maxWidth: 928 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
