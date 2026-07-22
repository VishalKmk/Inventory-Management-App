import React from 'react';
import AppLayout from '@/components/AppLayout';
import DashboardKpiCards from './components/DashboardKpiCards';
import CriticalLowStockTable from './components/CriticalLowStockTable';
import StorageHealthChart from './components/StorageHealthChart';
import InventoryTrafficChart from './components/InventoryTrafficChart';
import RecentActivityFeed from './components/RecentActivityFeed';
import DashboardHeader from './components/DashboardHeader';

export default function WarehouseOverviewDashboardPage() {
  return (
    <AppLayout
      searchPlaceholder="Search inventory, batches, or SKU..."
      userName="Alex Rivera"
      userRole="Inventory Manager"
    >
      <div className="px-6 py-6 max-w-screen-2xl mx-auto">
        <DashboardHeader />
        <DashboardKpiCards />
        {/* Main grid */}
        <div className="grid grid-cols-1 xl:grid-cols-3 gap-5 mt-5">
          {/* Left + Center — 2/3 width */}
          <div className="xl:col-span-2 space-y-5">
            <CriticalLowStockTable />
            <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
              <StorageHealthChart />
              <InventoryTrafficChart />
            </div>
          </div>
          {/* Right — Recent Activity */}
          <div className="xl:col-span-1">
            <RecentActivityFeed />
          </div>
        </div>
      </div>
    </AppLayout>
  );
}