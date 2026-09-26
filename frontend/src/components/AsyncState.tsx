import { Empty, Result, Skeleton, Button } from 'antd';
import type { ReactNode } from 'react';

// Four states for every data block (design 05 S3).
export function AsyncState({
  loading,
  error,
  empty,
  onRetry,
  children,
}: {
  loading: boolean;
  error: unknown;
  empty: boolean;
  onRetry: () => void;
  children: ReactNode;
}) {
  if (loading) {
    return <Skeleton active paragraph={{ rows: 6 }} />;
  }
  if (error) {
    return (
      <Result
        status="error"
        title="加载失败"
        subTitle={error instanceof Error ? error.message : String(error)}
        extra={<Button onClick={onRetry}>重试</Button>}
      />
    );
  }
  if (empty) {
    return <Empty description="暂无内容" />;
  }
  return <>{children}</>;
}
