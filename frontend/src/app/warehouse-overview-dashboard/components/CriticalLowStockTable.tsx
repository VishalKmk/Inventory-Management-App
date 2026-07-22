'use client';
import React, { useState, useEffect } from 'react';
import { ShoppingCart, AlertTriangle } from 'lucide-react';

import { Skeleton } from '@/components/ui/LoadingSkeleton';
import Link from 'next/link';
import AppImage from '@/components/ui/AppImage';
import { apiFetch } from '@/lib/api';

interface LowStockItem {
  productId: string;
  productName: string;
  sku: string;
  category: string;
  spaceName: string;
  currentStock: number;
  minimumQuantity: number;
  severity: 'critical' | 'high' | 'medium';
  imageUrl: string;
  imageAlt: string;
}

const FALLBACK_IMAGE = '/assets/images/no_image.png';

interface LowStockAlertsResponse {
  alertsBySpace: Record<string, Array<{
    productId: string;
    productName: string;
    spaceName: string;
    currentStock: number;
    minimumQuantity: number;
    severity: LowStockItem['severity'];
  }>>;
}

const MOCK_LOW_STOCK: LowStockItem[] = [
{
  productId: 'prod-001',
  productName: 'Industrial Power Drill v2',
  sku: 'TL-DR-8821',
  category: 'Tools & Hardware',
  spaceName: 'WH-001',
  currentStock: 12,
  minimumQuantity: 50,
  severity: 'critical',
  imageUrl: "https://img.rocket.new/generatedImages/rocket_gen_img_1f219d492-1768358953473.png",
  imageAlt: 'Industrial power drill with black and orange body on white background'
},
{
  productId: 'prod-002',
  productName: 'Pro-Shield Safety Helmet',
  sku: 'SF-HM-0019',
  category: 'Safety Equipment',
  spaceName: 'WH-001',
  currentStock: 85,
  minimumQuantity: 100,
  severity: 'high',
  imageUrl: "https://images.unsplash.com/photo-1617118600471-ecc90bdb4db0",
  imageAlt: 'Yellow hard hat safety helmet on gray surface'
},
{
  productId: 'prod-003',
  productName: 'Precision Copper Spool 50m',
  sku: 'EL-CW-1200',
  category: 'Electrical',
  spaceName: 'WH-001',
  currentStock: 4,
  minimumQuantity: 20,
  severity: 'critical',
  imageUrl: "https://images.unsplash.com/photo-1599256871679-6a154745680b",
  imageAlt: 'Orange copper wire spool on wooden shelf'
},
{
  productId: 'prod-004',
  productName: 'Hydraulic Seal Kit 3/4"',
  sku: 'HY-SK-0440',
  category: 'Hydraulics',
  spaceName: 'WH-001',
  currentStock: 18,
  minimumQuantity: 40,
  severity: 'high',
  imageUrl: "https://img.rocket.new/generatedImages/rocket_gen_img_1f5ffdc67-1773744584376.png",
  imageAlt: 'Set of hydraulic seal rings and gaskets on gray background'
},
{
  productId: 'prod-005',
  productName: 'IoT Inventory Tracker Node',
  sku: 'IOT-NOD-A1',
  category: 'Electronics',
  spaceName: 'WH-001',
  currentStock: 3,
  minimumQuantity: 25,
  severity: 'critical',
  imageUrl: "https://img.rocket.new/generatedImages/rocket_gen_img_1c9338d72-1773209940839.png",
  imageAlt: 'Small IoT sensor device with antenna on circuit board'
}];


export default function CriticalLowStockTable() {
  const [items, setItems] = useState<LowStockItem[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    apiFetch<LowStockAlertsResponse>('/api/dashboard/low-stock-alerts')
      .then(({ alertsBySpace }) => Object.entries(alertsBySpace).flatMap(([spaceName, alerts]) =>
        alerts.map((alert) => ({ ...alert, spaceName: alert.spaceName || spaceName, sku: '—', category: 'Inventory item', imageUrl: FALLBACK_IMAGE, imageAlt: alert.productName }))
      ))
      .then((alerts) => { if (active) setItems(alerts); })
      .catch(() => { if (active) setItems([]); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  return (
    <div className="section-card overflow-hidden">
      <div className="flex items-center justify-between px-5 py-4 border-b border-border">
        <div className="flex items-center gap-2">
          <AlertTriangle size={16} className="text-danger" />
          <h2 className="text-base font-semibold text-foreground">Critical Low Stock</h2>
          {!loading &&
          <span className="badge-critical">{items.filter((i) => i.severity === 'critical').length} Critical</span>
          }
        </div>
        <Link href="/product-management" className="text-sm text-accent font-medium hover:underline">
          View All
        </Link>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full">
          <thead>
            <tr className="border-b border-border">
              {['PRODUCT NAME', 'SKU', 'ON HAND', 'THRESHOLD', 'ACTION'].map((col) =>
              <th
                key={`th-${col}`}
                className="text-left text-xs font-semibold text-muted-foreground tracking-wider px-5 py-3">

                  {col}
                </th>
              )}
            </tr>
          </thead>
          <tbody>
            {loading ?
            ['skel-r1', 'skel-r2', 'skel-r3'].map((k) =>
            <tr key={k} className="border-b border-border last:border-0">
                    {['c1', 'c2', 'c3', 'c4', 'c5'].map((c) =>
              <td key={`${k}-${c}`} className="px-5 py-3.5">
                        <Skeleton className="h-4 w-full" />
                      </td>
              )}
                  </tr>
            ) :
            items.map((item) =>
            <tr
              key={item.productId}
              className="border-b border-border last:border-0 hover:bg-secondary/50 transition-colors">

                    {/* Product Name */}
                    <td className="px-5 py-3.5">
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-lg overflow-hidden border border-border flex-shrink-0 bg-secondary">
                          <AppImage
                      src={item.imageUrl}
                      alt={item.imageAlt}
                      width={40}
                      height={40}
                      className="w-full h-full object-cover" />

                        </div>
                        <div>
                          <p className="text-sm font-semibold text-foreground leading-tight">
                            {item.productName}
                          </p>
                          <p className="text-xs text-muted-foreground">{item.category}</p>
                        </div>
                      </div>
                    </td>
                    {/* SKU */}
                    <td className="px-5 py-3.5">
                      <span className="badge-sku font-mono">{item.sku}</span>
                    </td>
                    {/* On Hand */}
                    <td className="px-5 py-3.5">
                      <span
                  className={`inline-flex items-center justify-center w-14 h-7 rounded-md text-sm font-bold tabular-nums ${
                  item.severity === 'critical' ? 'bg-danger/15 text-danger' :
                  item.severity === 'high' ? 'bg-warning/15 text-warning' : 'bg-yellow-100 text-yellow-700'}`
                  }>

                        {item.currentStock}
                        <span className="text-xs font-normal ml-0.5">u</span>
                      </span>
                    </td>
                    {/* Threshold */}
                    <td className="px-5 py-3.5">
                      <span className="text-sm text-muted-foreground tabular-nums">
                        {item.minimumQuantity} units
                      </span>
                    </td>
                    {/* Action */}
                    <td className="px-5 py-3.5">
                      <button
                  className="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-accent/10 text-accent transition-colors"
                  title={`Restock ${item.productName}`}
                  aria-label={`Restock ${item.productName}`}>

                        <ShoppingCart size={16} />
                      </button>
                    </td>
                  </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>);

}
