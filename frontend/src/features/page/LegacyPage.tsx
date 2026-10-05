import { useQuery } from '@tanstack/react-query';
import { Navigate, useParams } from 'react-router';
import { api } from '../../api/client';
import { AsyncState } from '../../components/AsyncState';

/** A legacy URL becomes a current page URL only after an authorized server lookup. */
export function LegacyPage() {
  const { legacySpace = '', '*': path = '' } = useParams();
  const destination = useQuery({
    queryKey: ['legacy-link', legacySpace, path], retry: false,
    queryFn: async () => (await api.get<{ spaceSlug: string; pageId: number }>('/api/wiki/legacy/resolve', {
      params: { space: legacySpace, path: path.replace(/\/$/, '') },
    })).data,
  });
  return <AsyncState loading={destination.isLoading} error={destination.isError ? new Error('legacy link unavailable') : null}
    empty={!destination.data} onRetry={() => void destination.refetch()}>
    {destination.data && <Navigate replace to={`/s/${destination.data.spaceSlug}/p/${destination.data.pageId}`} />}
  </AsyncState>;
}
