'use client';
import React, { useState, useEffect } from 'react';
import {
  PackagePlus,
  PackageMinus,
  Settings2,
  UserPlus,
  PlusSquare,
  ChevronRight,
} from 'lucide-react';
import { ActivityItemSkeleton } from '@/components/ui/LoadingSkeleton';
import Link from 'next/link';
import { apiFetch } from '@/lib/api';

interface ActivityItem {
  id: string;
  type: 'STOCK_ADD' | 'STOCK_REMOVE' | 'SPACE_META' | 'USER_ACCESS' | 'CREATE';
  title: string;
  meta: string;
  metaLabel: string;
  timestamp: string;
  iconColor: string;
  iconBg: string;
}

const MOCK_ACTIVITY: ActivityItem[] = [
  {
    id: 'act-001',
    type: 'STOCK_ADD',
    title: 'Stock In: 500x Industrial Gaskets',
    meta: 'Bin B-04',
    metaLabel: 'Logged by Sarah J.',
    timestamp: '14:22 PM',
    iconColor: 'text-success',
    iconBg: 'bg-success/10',
  },
  {
    id: 'act-002',
    type: 'SPACE_META',
    title: 'Audit Check: Aisle 4 Comprehensive',
    meta: 'No discrepancies',
    metaLabel: 'Completed by Alex Rivera',
    timestamp: '11:05 AM',
    iconColor: 'text-warning',
    iconBg: 'bg-warning/10',
  },
  {
    id: 'act-003',
    type: 'STOCK_REMOVE',
    title: 'Stock Out: 12x Safety Helmets',
    meta: 'Work Order #WO-9912',
    metaLabel: 'Site B',
    timestamp: '09:45 AM',
    iconColor: 'text-danger',
    iconBg: 'bg-danger/10',
  },
  {
    id: 'act-004',
    type: 'USER_ACCESS',
    title: 'New Member: James Wilson',
    meta: 'Warehouse Associate',
    metaLabel: 'Added as',
    timestamp: 'Yesterday',
    iconColor: 'text-accent',
    iconBg: 'bg-accent/10',
  },
  {
    id: 'act-005',
    type: 'CREATE',
    title: "Registered new Product 'Monitor Arm'",
    meta: 'SKU-4409',
    metaLabel: 'CREATE PRODUCT',
    timestamp: 'Yesterday',
    iconColor: 'text-primary',
    iconBg: 'bg-primary/10',
  },
];

const iconMap: Record<ActivityItem['type'], React.ReactNode> = {
  STOCK_ADD: <PackagePlus size={16} />,
  STOCK_REMOVE: <PackageMinus size={16} />,
  SPACE_META: <Settings2 size={16} />,
  USER_ACCESS: <UserPlus size={16} />,
  CREATE: <PlusSquare size={16} />,
};

export default function RecentActivityFeed() {
  const [items, setItems] = useState<ActivityItem[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    apiFetch<{ activities: Array<{ id: string; type: string; description: string; timestamp: string }> }>('/api/dashboard/recent-activity')
      .then(({ activities }) => activities.map((activity): ActivityItem => {
        const type = activity.type.toUpperCase();
        const displayType: ActivityItem['type'] = type === 'STOCK_ADD' || type === 'STOCK_REMOVE' || type === 'CREATE'
          ? type : type.includes('USER') || type.includes('MEMBER') ? 'USER_ACCESS' : 'SPACE_META';
        const styles = {
          STOCK_ADD: ['text-success', 'bg-success/10'], STOCK_REMOVE: ['text-danger', 'bg-danger/10'],
          CREATE: ['text-primary', 'bg-primary/10'], USER_ACCESS: ['text-accent', 'bg-accent/10'], SPACE_META: ['text-warning', 'bg-warning/10'],
        } as const;
        return { id: activity.id, type: displayType, title: activity.description, meta: '', metaLabel: displayType.replace('_', ' '), timestamp: new Date(activity.timestamp).toISOString().slice(11, 16), iconColor: styles[displayType][0], iconBg: styles[displayType][1] };
      }))
      .then((activities) => { if (active) setItems(activities); })
      .catch(() => { if (active) setItems([]); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  return (
    <div className="section-card flex flex-col h-full">
      <div className="flex items-center justify-between px-5 py-4 border-b border-border">
        <h2 className="text-base font-semibold text-foreground">Recent Activity</h2>
      </div>

      <div className="flex-1 divide-y divide-border overflow-y-auto">
        {loading
          ? ['al-1', 'al-2', 'al-3', 'al-4'].map((k) => (
              <div key={k} className="px-5">
                <ActivityItemSkeleton />
              </div>
            ))
          : items.map((item) => (
              <div key={item.id} className="px-5 py-3.5 hover:bg-secondary/40 transition-colors">
                <div className="flex items-start gap-3">
                  <div
                    className={`w-9 h-9 rounded-xl flex items-center justify-center flex-shrink-0 ${item.iconBg} ${item.iconColor}`}
                  >
                    {iconMap[item.type]}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-foreground leading-snug truncate">
                      {item.title}
                    </p>
                    <p className="text-xs text-muted-foreground mt-0.5">
                      {item.metaLabel}
                      {item.meta && (
                        <>
                          {' '}
                          <span className="font-medium text-foreground">• {item.meta}</span>
                        </>
                      )}
                    </p>
                  </div>
                  <span className="text-xs text-muted-foreground flex-shrink-0 tabular-nums">
                    {item.timestamp}
                  </span>
                </div>
              </div>
            ))}
      </div>

      <div className="px-5 py-3.5 border-t border-border">
        <Link
          href="/audit-log"
          className="flex items-center justify-center gap-1.5 text-sm text-muted-foreground hover:text-foreground font-medium transition-colors w-full"
        >
          View Full Audit Log
          <ChevronRight size={14} />
        </Link>
      </div>
    </div>
  );
}
