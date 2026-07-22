import React from 'react';
import SpaceMembersContent from './components/SpaceMembersContent';

export default async function SpaceMembersPage({ params }: { params: Promise<{ spaceId: string }> }) {
  const { spaceId } = await params;
  return <SpaceMembersContent spaceId={spaceId} />;
}
