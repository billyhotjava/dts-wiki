import { useQuery } from '@tanstack/react-query';
import { createContext, useContext, type ReactNode } from 'react';
import { fetchAccount, type Account } from '../api/client';

interface AuthState {
  account?: Account;
  isLoading: boolean;
  isError: boolean;
}

const AuthContext = createContext<AuthState>({ isLoading: true, isError: false });

export function AuthProvider({ children }: { children: ReactNode }) {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['account'],
    queryFn: fetchAccount,
    retry: false,
    staleTime: 60_000,
  });
  return <AuthContext.Provider value={{ account: data, isLoading, isError }}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  return useContext(AuthContext);
}
