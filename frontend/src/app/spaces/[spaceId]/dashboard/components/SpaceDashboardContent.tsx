'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { AlertTriangle, DollarSign, Package, Users } from 'lucide-react';
import AppLayout from '@/components/AppLayout';
import { Skeleton } from '@/components/ui/LoadingSkeleton';
import { apiFetch } from '@/lib/api';

interface Product {
  productId: string;
  name: string;
  price: number;
  currentStock: number;
  totalValue: number;
}

interface Activity {
  id: string;
  operation: string;
  entityType: string;
  timestamp: string;
}

interface SpaceDashboard {
  spaceId: string;
  spaceName: string;
  currentUserRole: string;
  memberCount: number;
  overview: { totalProducts: number; totalValue: number; lowStockCount: number; stockStatus: Record<string, number> };
  lowStockProducts: Product[];
  recentActivity: Activity[];
}

export default function SpaceDashboardContent({ spaceId }: { spaceId: string }) {
  const [data, setData] = useState<SpaceDashboard | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    apiFetch<SpaceDashboard>(`/api/spaces/${spaceId}/dashboard`)
      .then((dashboard) => { if (active) setData(dashboard); })
      .catch((reason) => { if (active) setError(reason instanceof Error ? reason.message : 'Unable to load dashboard'); });
    return () => { active = false; };
  }, [spaceId]);

  if (!data && !error) return <div className="p-6"><Skeleton className="h-80 w-full" /></div>;
  if (error) return <div className="p-6 text-danger">{error}</div>;
  if (!data) return null;

  const cards = [
    { label: 'Total products', value: data.overview.totalProducts, icon: <Package size={20} /> },
    { label: 'Inventory value', value: `$${data.overview.totalValue.toLocaleString()}`, icon: <DollarSign size={20} /> },
    { label: 'Low-stock alerts', value: data.overview.lowStockCount, icon: <AlertTriangle size={20} /> },
    { label: 'Members', value: data.memberCount, icon: <Users size={20} /> },
  ];

  return (
    <AppLayout spaceId={spaceId} spaceName={data.spaceName} spaceCode={spaceId.slice(0, 8).toUpperCase()} userRole={data.currentUserRole}>
      <div className="px-6 py-6 max-w-screen-2xl mx-auto space-y-6">
        <div className="flex items-center justify-between">
          <div><h1 className="text-2xl font-bold text-foreground">{data.spaceName}</h1><p className="text-sm text-muted-foreground">Space inventory overview</p></div>
          <Link href={`/spaces/${spaceId}/products`} className="btn-primary">Manage products</Link>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
          {cards.map((card) => <div key={card.label} className="kpi-card"><div className="flex justify-between text-muted-foreground">{card.label}{card.icon}</div><p className="text-3xl font-bold mt-3">{card.value}</p></div>)}
        </div>
        <div className="grid grid-cols-1 xl:grid-cols-2 gap-5">
          <section className="section-card"><div className="p-5 border-b border-border flex justify-between"><h2 className="font-semibold">Low stock</h2><Link href={`/spaces/${spaceId}/products`} className="text-accent text-sm">View products</Link></div>
            <div className="divide-y divide-border">{data.lowStockProducts.length ? data.lowStockProducts.map((product) => <div key={product.productId} className="p-4 flex justify-between"><span>{product.name}</span><span className="text-danger font-semibold">{product.currentStock} in stock</span></div>) : <p className="p-5 text-sm text-muted-foreground">No low-stock products.</p>}</div>
          </section>
          <section className="section-card"><div className="p-5 border-b border-border flex justify-between"><h2 className="font-semibold">Recent activity</h2><Link href={`/spaces/${spaceId}/audit-log`} className="text-accent text-sm">Audit log</Link></div>
            <div className="divide-y divide-border">{data.recentActivity.length ? data.recentActivity.map((activity) => <div key={activity.id} className="p-4"><p className="font-medium">{activity.operation.replace('_', ' ')} {activity.entityType}</p><p className="text-xs text-muted-foreground">{new Date(activity.timestamp).toLocaleString()}</p></div>) : <p className="p-5 text-sm text-muted-foreground">No recent activity.</p>}</div>
          </section>
        </div>
      </div>
    </AppLayout>
  );
}
