'use client';
import React, { useState, useEffect } from 'react';
import { Package, DollarSign, AlertTriangle, TrendingUp } from 'lucide-react';
import { Skeleton } from '@/components/ui/LoadingSkeleton';
import { apiFetch } from '@/lib/api';

interface KpiData {
  totalProducts: number;
  totalValue: number;
  lowStockCount: number;
  productsChange: number;
  valueChange: number;
  lowStockChange: number;
}

const MOCK_KPI: KpiData = {
  totalProducts: 12842,
  totalValue: 1420000,
  lowStockCount: 24,
  productsChange: 2.4,
  valueChange: 1.8,
  lowStockChange: 4,
};

function formatValue(v: number): string {
  if (v >= 1_000_000) return `$${(v / 1_000_000).toFixed(2)}M`;
  if (v >= 1_000) return `$${(v / 1_000).toFixed(0)}K`;
  return `$${v}`;
}

export default function DashboardKpiCards() {
  const [data, setData] = useState<KpiData | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    apiFetch<Pick<KpiData, 'totalProducts' | 'totalValue' | 'lowStockCount'>>('/api/dashboard/overview')
      .then((overview) => {
        if (active) setData({ ...overview, productsChange: 0, valueChange: 0, lowStockChange: 0 });
      })
      .catch(() => {
        if (active) setData(null);
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => { active = false; };
  }, []);

  if (loading) {
    return (
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {['kpi-skel-1', 'kpi-skel-2', 'kpi-skel-3'].map((k) => (
          <div key={k} className="kpi-card">
            <Skeleton className="h-3 w-28 mb-4" />
            <Skeleton className="h-9 w-28 mb-3" />
            <Skeleton className="h-3 w-36" />
          </div>
        ))}
      </div>
    );
  }

  if (!data) return null;

  const cards = [
    {
      key: 'kpi-total-products',
      label: 'TOTAL PRODUCTS',
      value: data.totalProducts.toLocaleString(),
      change: data.productsChange ? `+${data.productsChange}% from last week` : 'Current inventory total',
      changePositive: true,
      icon: <Package size={20} className="text-accent" />,
      iconBg: 'bg-accent/10',
      alert: false,
    },
    {
      key: 'kpi-total-valuation',
      label: 'TOTAL VALUATION',
      value: formatValue(data.totalValue),
      change: 'Weighted average cost',
      changePositive: true,
      icon: <DollarSign size={20} className="text-success" />,
      iconBg: 'bg-success/10',
      alert: false,
    },
    {
      key: 'kpi-low-stock',
      label: 'LOW STOCK ALERTS',
      value: String(data.lowStockCount),
      change: '! Requires immediate action',
      changePositive: false,
      icon: <AlertTriangle size={20} className="text-danger" />,
      iconBg: 'bg-danger/10',
      alert: true,
    },
  ];

  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
      {cards.map((card) => (
        <div
          key={card.key}
          className={`kpi-card ${card.alert ? 'border-danger/30 bg-danger/5' : ''}`}
        >
          <div className="flex items-start justify-between mb-3">
            <p className="text-xs font-semibold text-muted-foreground tracking-widest uppercase">
              {card.label}
            </p>
            <div className={`w-9 h-9 rounded-lg ${card.iconBg} flex items-center justify-center`}>
              {card.icon}
            </div>
          </div>
          <p
            className={`text-4xl font-bold tabular-nums mb-2 ${
              card.alert ? 'text-danger' : 'text-foreground'
            }`}
          >
            {card.value}
          </p>
          <p
            className={`text-xs font-medium flex items-center gap-1 ${
              card.alert
                ? 'text-danger'
                : card.changePositive
                ? 'text-success' :'text-muted-foreground'
            }`}
          >
            {card.changePositive && !card.alert && <TrendingUp size={12} />}
            {card.alert && <AlertTriangle size={12} />}
            {card.change}
          </p>
        </div>
      ))}
    </div>
  );
}
