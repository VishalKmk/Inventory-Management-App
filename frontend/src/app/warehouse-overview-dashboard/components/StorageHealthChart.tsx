'use client';
import React, { useState, useEffect } from 'react';
import dynamic from 'next/dynamic';
import { Skeleton } from '@/components/ui/LoadingSkeleton';
import { apiFetch } from '@/lib/api';

const StorageHealthChartInner = dynamic(
  () => import('./StorageHealthChartInner'),
  { ssr: false, loading: () => <Skeleton className="h-[200px] w-full" /> }
);

export default function StorageHealthChart() {
  const [healthScore, setHealthScore] = useState<number | null>(null);

  useEffect(() => {
    let active = true;
    apiFetch<{ spaceMetrics: Array<{ healthScore: number }> }>('/api/dashboard/space-metrics')
      .then(({ spaceMetrics }) => {
        const average = spaceMetrics.length ? spaceMetrics.reduce((sum, space) => sum + space.healthScore, 0) / spaceMetrics.length : 0;
        if (active) setHealthScore(average);
      })
      .catch(() => { if (active) setHealthScore(0); });
    return () => { active = false; };
  }, []);

  return (
    <div className="section-card p-5">
      <p className="text-xs font-semibold text-muted-foreground tracking-widest uppercase mb-4">
        Storage Health Score
      </p>
      {healthScore === null ? <Skeleton className="h-[200px] w-full" /> : <StorageHealthChartInner healthScore={healthScore} />}
      <p className="text-xs text-muted-foreground text-center mt-3 leading-relaxed">
        Space utilization is optimal. Capacity: 840/1000 bins.
      </p>
    </div>
  );
}
