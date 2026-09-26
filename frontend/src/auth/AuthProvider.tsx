import { createContext, useContext, type ReactNode } from 'react';
import { useBootstrap, type SpaceSummary } from '../api/hooks';

interface AuthState {
  account?: { login: string; firstName?: string | null; lastName?: string | null };
  spaces: SpaceSummary[];
  isLoading: boolean;
  isError: boolean;
}

const AuthContext = createContext<AuthState>({ spaces: [], isLoading: true, isError: false });

export function AuthProvider({ children }: { children: ReactNode }) {
  const { data, isLoading, isError } = useBootstrap();
  return (
    <AuthContext.Provider
      value={{ account: data?.account, spaces: data?.spaces ?? [], isLoading, isError }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthState {
  return useContext(AuthContext);
}
