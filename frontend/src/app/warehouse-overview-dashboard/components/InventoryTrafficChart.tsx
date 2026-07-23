'use client';
import React, { useEffect, useState } from 'react';
import dynamic from 'next/dynamic';
import { Skeleton } from '@/components/ui/LoadingSkeleton';
import { apiFetch } from '@/lib/api';

const InventoryTrafficChartInner = dynamic(
  () => import('./InventoryTrafficChartInner'),
  { ssr: false, loading: () => <Skeleton className="h-[200px] w-full" /> }
);

export default function InventoryTrafficChart() {
  const [data, setData] = useState<Array<{ day: string; ops: number }> | null>(null);

  useEffect(() => {
    let active = true;
    apiFetch<{ dailyActivity: Record<string, number> }>('/api/dashboard/trends?days=7')
      .then(({ dailyActivity }) => Object.entries(dailyActivity).map(([date, ops]) => ({
        day: new Date(`${date}T00:00:00Z`).toLocaleDateString('en-US', { weekday: 'short' }).toUpperCase(), ops,
      })))
      .then((traffic) => { if (active) setData(traffic); })
      .catch(() => { if (active) setData([]); });
    return () => { active = false; };
  }, []);

  return (
    <div className="section-card p-5">
      <p className="text-xs font-semibold text-muted-foreground tracking-widest uppercase mb-4">
        Activity Traffic
      </p>
      {data === null ? <Skeleton className="h-[200px] w-full" /> : <InventoryTrafficChartInner data={data} />}
    </div>
  );
}
