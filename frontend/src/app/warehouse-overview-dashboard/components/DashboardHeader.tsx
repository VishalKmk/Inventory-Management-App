'use client';
import React from 'react';
import { Calendar } from 'lucide-react';
import Link from 'next/link';

export default function DashboardHeader() {
  return (
    <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4 mb-5">
      <div>
        <h1 className="text-2xl font-semibold text-foreground">Warehouse Overview</h1>
        <p className="text-sm text-muted-foreground mt-0.5">
          Inventory status across all spaces you can access
        </p>
      </div>
      <div className="flex items-center gap-3">
        <button className="btn-secondary text-sm">
          <Calendar size={15} />
          Last 30 Days
        </button>
        <Link href="/product-management" className="btn-primary text-sm">
          + Stock Adjustment
        </Link>
      </div>
    </div>
  );
}
