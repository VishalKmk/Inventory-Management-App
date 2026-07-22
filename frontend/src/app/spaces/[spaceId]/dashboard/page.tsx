import React from 'react';
import SpaceDashboardContent from './components/SpaceDashboardContent';

export default async function SpaceDashboardPage({ params }: { params: Promise<{ spaceId: string }> }) {
  const { spaceId } = await params;
  return <SpaceDashboardContent spaceId={spaceId} />;
}
