import React from 'react';
import SpaceAuditLogContent from './components/SpaceAuditLogContent';

export default async function SpaceAuditLogPage({ params }: { params: Promise<{ spaceId: string }> }) {
  const { spaceId } = await params;
  return <SpaceAuditLogContent spaceId={spaceId} />;
}
