import { Spin } from 'antd';
import type { ReactNode } from 'react';
import { useAuth } from './AuthProvider';

// Unauthenticated users are bounced to Keycloak by the axios 401 interceptor;
// this guard only covers the loading state (design 05 S1).
export function RequireAuth({ children }: { children: ReactNode }) {
  const { isLoading, isError } = useAuth();
  if (isLoading) {
    return <Spin fullscreen description="Loading" />;
  }
  if (isError) {
    window.location.href = '/oauth2/authorization/oidc';
    return null;
  }
  return <>{children}</>;
}
