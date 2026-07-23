'use client';
import React from 'react';
import {
  RadialBarChart,
  RadialBar,
  ResponsiveContainer,
  PolarAngleAxis,
} from 'recharts';

// BACKEND INTEGRATION: GET /api/dashboard/space-metrics → healthScore average
export default function StorageHealthChartInner({ healthScore }: { healthScore: number }) {
  const score = Math.round(healthScore);
  const data = [{ name: 'Health', value: score, fill: 'var(--primary)' }];
  return (
    <div className="relative flex items-center justify-center">
      <ResponsiveContainer width="100%" height={180}>
        <RadialBarChart
          cx="50%"
          cy="50%"
          innerRadius="65%"
          outerRadius="90%"
          barSize={14}
          data={data}
          startAngle={90}
          endAngle={-270}
        >
          <PolarAngleAxis
            type="number"
            domain={[0, 100]}
            angleAxisId={0}
            tick={false}
          />
          <RadialBar
            background={{ fill: 'var(--muted)' }}
            dataKey="value"
            cornerRadius={8}
            angleAxisId={0}
          />
        </RadialBarChart>
      </ResponsiveContainer>
      <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none">
        <span className="text-3xl font-bold text-foreground tabular-nums">{score}%</span>
        <span className="text-xs text-success font-medium mt-0.5">Optimal</span>
      </div>
    </div>
  );
}
