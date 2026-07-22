'use client';
import React from 'react';
import { Package, AlertTriangle } from 'lucide-react';
import type { Product } from './ProductManagementContent';

interface ProductSummaryCardsProps {
  products: Product[];
}

export default function ProductSummaryCards({ products }: ProductSummaryCardsProps) {
  const totalValue = products.reduce((sum, p) => sum + p.price * p.currentStock, 0);
  const lowStockCount = products.filter((p) => p.currentStock <= p.minimumQuantity).length;

  const formatCurrency = (v: number) => {
    if (v >= 1_000_000) return `$${(v / 1_000_000).toFixed(2)}M`;
    if (v >= 1_000) return `$${Math.round(v / 1000)}K`;
    return `$${v.toFixed(2)}`;
  };

  const summaryCards = [
    {
      key: 'summary-total-value',
      icon: <Package size={20} className="text-muted-foreground" />,
      label: 'Total Value',
      value: formatCurrency(totalValue),
      iconBg: 'bg-secondary',
    },
    {
      key: 'summary-low-stock',
      icon: <AlertTriangle size={20} className="text-warning" />,
      label: 'Low Stock Items',
      value: `${lowStockCount} Alerts`,
      iconBg: 'bg-warning/10',
    },
  ];

  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
      {summaryCards.map((card) => (
        <div key={card.key} className="kpi-card flex items-center gap-4">
          <div className={`w-11 h-11 rounded-xl flex items-center justify-center flex-shrink-0 ${card.iconBg}`}>
            {card.icon}
          </div>
          <div>
            <p className="text-xs text-muted-foreground font-medium mb-0.5">{card.label}</p>
            <p className="text-xl font-bold text-foreground tabular-nums">{card.value}</p>
          </div>
        </div>
      ))}
    </div>
  );
}
