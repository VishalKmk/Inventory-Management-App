import React from 'react';
import SpaceProductsContent from './components/SpaceProductsContent';

export default async function SpaceProductsPage({ params }: { params: Promise<{ spaceId: string }> }) {
  const { spaceId } = await params;
  return <SpaceProductsContent spaceId={spaceId} />;
}
