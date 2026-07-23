import { redirect } from 'next/navigation';

export default async function SpacePage({ params }: { params: Promise<{ spaceId: string }> }) {
  const { spaceId } = await params;
  redirect(`/spaces/${spaceId}/dashboard`);
}
