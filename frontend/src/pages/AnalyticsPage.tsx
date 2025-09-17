import React, { useState, useEffect } from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, LineChart, Line } from 'recharts';
import Layout from '../components/layout/Layout';
import Card from '../components/ui/Card';
import { useApi } from '../hooks/useApi';
import { TrendingUp, Package, DollarSign, AlertTriangle } from 'lucide-react';

interface AnalyticsData {
  valueBySpace: Record<string, number>;
  productCountBySpace: Record<string, number>;
  priceAnalysis: {
    minimum: number;
    maximum: number;
    average: number;
  };
  stockAnalysis: {
    minimum: number;
    maximum: number;
    average: number;
    total: number;
  };
}

interface LowStockAlert {
  totalAlerts: number;
  severityBreakdown: {
    high: number;
    medium: number;
    critical: number;
  };
}

const AnalyticsPage: React.FC = () => {
  const { get } = useApi();
  const [analyticsData, setAnalyticsData] = useState<AnalyticsData | null>(null);
  const [lowStockData, setLowStockData] = useState<LowStockAlert | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchAnalyticsData();
  }, []);

  const fetchAnalyticsData = async () => {
    try {
      const [insightsResponse, lowStockResponse] = await Promise.all([
        get('/dashboard/insights'),
        get('/dashboard/low-stock-alerts')
      ]);

      if (insightsResponse.success) {
        setAnalyticsData(insightsResponse.data);
      }

      if (lowStockResponse.success) {
        setLowStockData(lowStockResponse.data);
      }
    } catch (error) {
      console.error('Error fetching analytics data:', error);
    } finally {
      setLoading(false);
    }
  };

  const COLORS = ['#6366f1', '#06b6d4', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'];

  // Prepare chart data
  const valueBySpaceData = analyticsData?.valueBySpace ? 
    Object.entries(analyticsData.valueBySpace).map(([name, value]) => ({ name, value })) : [];

  const productCountData = analyticsData?.productCountBySpace ? 
    Object.entries(analyticsData.productCountBySpace).map(([name, count]) => ({ name, count })) : [];

  const stockDistributionData = lowStockData?.severityBreakdown ? [
    { name: 'Critical', value: lowStockData.severityBreakdown.critical, color: '#ef4444' },
    { name: 'High', value: lowStockData.severityBreakdown.high, color: '#f59e0b' },
    { name: 'Medium', value: lowStockData.severityBreakdown.medium, color: '#06b6d4' },
  ].filter(item => item.value > 0) : [];

  const StatCard: React.FC<{
    title: string;
    value: string | number;
    subtitle: string;
    icon: React.ReactNode;
    color: string;
  }> = ({ title, value, subtitle, icon, color }) => (
    <Card className="flex items-center gap-4">
      <div className={`p-3 rounded-xl ${color}`}>
        {icon}
      </div>
      <div>
        <p className="text-sm font-medium text-gray-600 dark:text-slate-400">{title}</p>
        <p className="text-2xl font-bold text-gray-900 dark:text-white">{value}</p>
        <p className="text-xs text-gray-500 dark:text-slate-500">{subtitle}</p>
      </div>
    </Card>
  );

  return (
    <Layout 
      title="Analytics" 
      subtitle="Insights into your inventory performance and trends"
    >
      <div className="space-y-8">
        {/* Key Metrics */}
        {analyticsData && (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            <StatCard
              title="Average Price"
              value={`$${analyticsData.priceAnalysis?.average?.toFixed(2) || '0.00'}`}
              subtitle="Per product"
              icon={<DollarSign size={24} className="text-green-600 dark:text-green-400" />}
              color="bg-green-100 dark:bg-green-900/30"
            />
            <StatCard
              title="Total Stock"
              value={analyticsData.stockAnalysis?.total?.toLocaleString() || '0'}
              subtitle="All products"
              icon={<Package size={24} className="text-blue-600 dark:text-blue-400" />}
              color="bg-blue-100 dark:bg-blue-900/30"
            />
            <StatCard
              title="Average Stock"
              value={Math.round(analyticsData.stockAnalysis?.average || 0)}
              subtitle="Per product"
              icon={<TrendingUp size={24} className="text-purple-600 dark:text-purple-400" />}
              color="bg-purple-100 dark:bg-purple-900/30"
            />
            <StatCard
              title="Low Stock Alerts"
              value={lowStockData?.totalAlerts || 0}
              subtitle="Need attention"
              icon={<AlertTriangle size={24} className="text-orange-600 dark:text-orange-400" />}
              color="bg-orange-100 dark:bg-orange-900/30"
            />
          </div>
        )}

        {/* Charts Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          {/* Inventory Value by Space */}
          <Card>
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-6">
              Inventory Value by Space
            </h3>
            {valueBySpaceData.length > 0 ? (
              <div className="h-80">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={valueBySpaceData}>
                    <CartesianGrid strokeDasharray="3 3" className="opacity-30" />
                    <XAxis 
                      dataKey="name" 
                      className="text-xs"
                      tick={{ fill: 'currentColor' }}
                    />
                    <YAxis 
                      className="text-xs"
                      tick={{ fill: 'currentColor' }}
                      tickFormatter={(value) => `$${value.toLocaleString()}`}
                    />
                    <Tooltip 
                      formatter={(value: number) => [`$${value.toLocaleString()}`, 'Value']}
                      contentStyle={{ 
                        backgroundColor: 'var(--tooltip-bg)', 
                        border: '1px solid var(--tooltip-border)' 
                      }}
                    />
                    <Bar dataKey="value" fill="#6366f1" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            ) : (
              <div className="h-80 flex items-center justify-center text-gray-500 dark:text-slate-400">
                No data available
              </div>
            )}
          </Card>

          {/* Product Count by Space */}
          <Card>
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-6">
              Product Distribution
            </h3>
            {productCountData.length > 0 ? (
              <div className="h-80">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={productCountData}
                      cx="50%"
                      cy="50%"
                      labelLine={false}
                      label={({ name, count }) => `${name}: ${count}`}
                      outerRadius={100}
                      fill="#8884d8"
                      dataKey="count"
                    >
                      {productCountData.map((_, index) => (
                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip />
                  </PieChart>
                </ResponsiveContainer>
              </div>
            ) : (
              <div className="h-80 flex items-center justify-center text-gray-500 dark:text-slate-400">
                No data available
              </div>
            )}
          </Card>

          {/* Stock Status Distribution */}
          <Card>
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-6">
              Low Stock Alerts by Severity
            </h3>
            {stockDistributionData.length > 0 ? (
              <div className="h-80">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={stockDistributionData}
                      cx="50%"
                      cy="50%"
                      labelLine={false}
                      label={({ name, value }) => `${name}: ${value}`}
                      outerRadius={100}
                      fill="#8884d8"
                      dataKey="value"
                    >
                      {stockDistributionData.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={entry.color} />
                      ))}
                    </Pie>
                    <Tooltip />
                  </PieChart>
                </ResponsiveContainer>
              </div>
            ) : (
              <div className="h-80 flex items-center justify-center text-gray-500 dark:text-slate-400">
                <div className="text-center">
                  <AlertTriangle size={48} className="mx-auto text-green-500 mb-3" />
                  <p className="text-green-600 dark:text-green-400 font-medium">All stock levels are healthy!</p>
                  <p className="text-sm text-gray-500 dark:text-slate-400">No low stock alerts</p>
                </div>
              </div>
            )}
          </Card>

          {/* Price Range Analysis */}
          <Card>
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-6">
              Price Analysis
            </h3>
            {analyticsData?.priceAnalysis ? (
              <div className="space-y-6">
                <div className="flex justify-between items-center">
                  <span className="text-gray-600 dark:text-slate-400">Minimum Price</span>
                  <span className="font-bold text-gray-900 dark:text-white">
                    ${analyticsData.priceAnalysis.minimum.toFixed(2)}
                  </span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-gray-600 dark:text-slate-400">Average Price</span>
                  <span className="font-bold text-indigo-600 dark:text-indigo-400">
                    ${analyticsData.priceAnalysis.average.toFixed(2)}
                  </span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-gray-600 dark:text-slate-400">Maximum Price</span>
                  <span className="font-bold text-gray-900 dark:text-white">
                    ${analyticsData.priceAnalysis.maximum.toFixed(2)}
                  </span>
                </div>
                
                <div className="mt-8">
                  <h4 className="font-medium text-gray-900 dark:text-white mb-4">Stock Overview</h4>
                  <div className="space-y-3">
                    <div className="flex justify-between items-center">
                      <span className="text-sm text-gray-600 dark:text-slate-400">Total Items</span>
                      <span className="font-medium">{analyticsData.stockAnalysis?.total?.toLocaleString() || 0}</span>
                    </div>
                    <div className="flex justify-between items-center">
                      <span className="text-sm text-gray-600 dark:text-slate-400">Average per Product</span>
                      <span className="font-medium">{Math.round(analyticsData.stockAnalysis?.average || 0)}</span>
                    </div>
                  </div>
                </div>
              </div>
            ) : (
              <div className="h-80 flex items-center justify-center text-gray-500 dark:text-slate-400">
                No pricing data available
              </div>
            )}
          </Card>
        </div>
      </div>
    </Layout>
  );
};

export default AnalyticsPage;