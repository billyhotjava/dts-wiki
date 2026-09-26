import { useEffect } from 'react';
import { useNavigate, useParams } from 'react-router';
import { useSpace } from '../../api/hooks';
import { AsyncState } from '../../components/AsyncState';

// Space landing: jump to the space root page (or stay empty when the space has no pages yet).
export function SpaceHomePage() {
  const { slug = '' } = useParams();
  const navigate = useNavigate();
  const { data, isLoading, isError, refetch } = useSpace(slug);

  useEffect(() => {
    if (data?.rootPageId !== undefined && data?.rootPageId !== null) {
      navigate(`/s/${slug}/p/${data.rootPageId}`, { replace: true });
    }
  }, [data, navigate, slug]);

  return (
    <AsyncState loading={isLoading} error={isError ? new Error('load failed') : null} empty={data?.rootPageId == null} onRetry={() => void refetch()}>
      <div />
    </AsyncState>
  );
}
