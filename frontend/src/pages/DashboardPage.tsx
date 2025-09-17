import React, { useState, useEffect } from 'react';
import { Plus, Warehouse, Package, DollarSign, AlertTriangle, ArrowRight } from 'lucide-react';
import Layout from '../components/layout/Layout';
import Card from '../components/ui/Card';
import Button from '../components/ui/Button';
import { useApi } from '../hooks/useApi';
import { Space, Product } from '../types/api';


interface DashboardData {
  totalSpaces: number;
  totalProducts: number;
  totalValue: number;
  lowStockCount: number;
  stockStatus: {
    inStock: number;
    lowStock: number;
    outOfStock: number;
  };
}

interface RecentActivity {
  type: string;
  name: string;
  spaceName?: string;
  date: string;
  id: string;
}


const DashboardPage: React.FC = () => {
  const { get } = useApi();
  const [dashboardData, setDashboardData] = useState<DashboardData | null>(null);
  const [recentActivity, setRecentActivity] = useState<RecentActivity[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchDashboardData();
  }, []);

const fetchDashboardData = async () => {
  try {
    const [overviewResponse, activityResponse, spacesResponse] = await Promise.all([
      get('/dashboard/overview'),
      get('/dashboard/recent-activity'),
      get('/spaces')
    ]);

    if (overviewResponse.success) {
      setDashboardData(overviewResponse.data);
    }

    let activities: RecentActivity[] = [];

if (activityResponse.success && activityResponse.data.activities) {
  const normalizedActivities: RecentActivity[] = activityResponse.data.activities
    .map((a: any) => {
      if (a.entityType === "product" && a.type === "create") {
        return {
          type: "product_created",
          name: a.details.productName,
          spaceName: a.details.spaceName,
          date: a.timestamp,
          id: a.entityId
        };
      }

      if (a.entityType === "space" && a.type === "create") {
        return {
          type: "space_created",
          name: a.details.spaceName,
          date: a.timestamp,
          id: a.entityId
        };
      }

      // ignore updates, deletes, stock_add, stock_remove etc.
      return null;
    })
    .filter(Boolean) as RecentActivity[];

  setRecentActivity(normalizedActivities);
}


    if (spacesResponse.success && spacesResponse.data?.length > 0) {
      const spaces: Space[] = spacesResponse.data;

      // only add fallback if no spaces in recent activities
      const hasRecentSpaces = activities.some(item => item.type === 'space_created');

      if (!hasRecentSpaces) {
        const fallbackSpaces: RecentActivity[] = spaces
          .sort(
            (a, b) =>
              new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
          )
          .slice(0, 5)
          .map(space => ({
            type: 'space_created',
            name: space.name,
            date: space.createdAt,
            id: space.id
          }));

        activities = [...activities, ...fallbackSpaces];
      }
    }

    setRecentActivity(activities);
  } catch (error) {
    console.error('Error fetching dashboard data:', error);
  } finally {
    setLoading(false);
  }
};



  const MetricCard: React.FC<{
    title: string;
    value: string | number;
    subtitle: string;
    icon: React.ReactNode;
    color: string;
  }> = ({ title, value, subtitle, icon, color }) => (
    <Card className={`${color} border-none text-white`}>
      <div className="flex items-center justify-between">
        <div>
          <p className="text-white/80 text-sm font-medium">{title}</p>
          <p className="text-3xl font-bold mt-1">{value}</p>
          <p className="text-white/70 text-xs mt-1">{subtitle}</p>
        </div>
        <div className="p-3 bg-white/20 rounded-xl">
          {icon}
        </div>
      </div>
    </Card>
  );

  return (
    <Layout 
      title="Dashboard" 
      subtitle="Overview of your inventory management system"
      action={
        <Button leftIcon={<Plus size={18} />}>
          Add Space
        </Button>
      }
    >
      <div className="space-y-8">
        {/* Metrics Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <MetricCard
            title="Total Spaces"
            value={dashboardData?.totalSpaces || 0}
            subtitle="Active locations"
            icon={<Warehouse size={24} />}
            color="bg-gradient-to-br from-blue-500 to-blue-600"
          />
          <MetricCard
            title="Total Products"
            value={dashboardData?.totalProducts || 0}
            subtitle="Items tracked"
            icon={<Package size={24} />}
            color="bg-gradient-to-br from-green-500 to-emerald-600"
          />
          <MetricCard
            title="Inventory Value"
            value={`$${dashboardData?.totalValue?.toLocaleString() || '0'}`}
            subtitle="Total value"
            icon={<DollarSign size={24} />}
            color="bg-gradient-to-br from-purple-500 to-purple-600"
          />
          <MetricCard
            title="Low Stock Items"
            value={dashboardData?.lowStockCount || 0}
            subtitle="Need attention"
            icon={<AlertTriangle size={24} />}
            color="bg-gradient-to-br from-orange-500 to-red-500"
          />
        </div>

        {/* Recent Activity */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Recent Spaces */}
          <Card>
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white">Recent Spaces</h3>
              <button className="text-indigo-600 dark:text-indigo-400 hover:underline text-sm font-medium flex items-center gap-1">
                View all <ArrowRight size={14} />
              </button>
            </div>
            
            <div className="space-y-4">
              {recentActivity
                .filter(item => item.type === 'space_created')
                .slice(0, 5)
                .map((space, index) => (
                  <div key={index} className="flex items-center gap-3">
                    <div className="p-2 bg-indigo-100 dark:bg-indigo-900/30 rounded-lg">
                      <Warehouse size={16} className="text-indigo-600 dark:text-indigo-400" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="font-medium text-gray-900 dark:text-white truncate">
                        {space.name}
                      </p>
                      <p className="text-sm text-gray-500 dark:text-slate-400">
                        {new Date(space.date).toLocaleDateString()}
                      </p>
                    </div>
                  </div>
                ))}
              
              {recentActivity.filter(item => item.type === 'space_created').length === 0 && (
                <div className="text-center py-8">
                  <Warehouse size={48} className="mx-auto text-gray-300 dark:text-slate-600 mb-3" />
                  <p className="text-gray-500 dark:text-slate-400 mb-4">No spaces created yet</p>
                  <Button size="sm" leftIcon={<Plus size={16} />}>
                    Create your first space
                  </Button>
                </div>
              )}
            </div>
          </Card>

          {/* Recent Products */}
          <Card>
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white">Recent Products</h3>
            </div>
            
            <div className="space-y-4">
              {recentActivity
                .filter(item => item.type === 'product_created')
                .slice(0, 5)
                .map((product, index) => (
                  <div key={index} className="flex items-center gap-3">
                    <div className="p-2 bg-green-100 dark:bg-green-900/30 rounded-lg">
                      <Package size={16} className="text-green-600 dark:text-green-400" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="font-medium text-gray-900 dark:text-white truncate">
                        {product.name}
                      </p>
                      <p className="text-sm text-gray-500 dark:text-slate-400">
                        {product.spaceName} • {new Date(product.date).toLocaleDateString()}
                      </p>
                    </div>
                  </div>
                ))}
              
              {recentActivity.filter(item => item.type === 'product_created').length === 0 && (
                <div className="text-center py-8">
                  <Package size={48} className="mx-auto text-gray-300 dark:text-slate-600 mb-3" />
                  <p className="text-gray-500 dark:text-slate-400 mb-4">No products added yet</p>
                  <Button size="sm" leftIcon={<Plus size={16} />}>
                    Add your first product
                  </Button>
                </div>
              )}
            </div>
          </Card>
        </div>
      </div>
    </Layout>
  );
};

export default DashboardPage;