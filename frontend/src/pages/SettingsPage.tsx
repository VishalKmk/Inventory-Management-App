import React, { useState } from 'react';
import { Save, Bell, Shield, Palette, Database } from 'lucide-react';
import Layout from '../components/layout/Layout';
import Card from '../components/ui/Card';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';

const SettingsPage: React.FC = () => {
  const [settings, setSettings] = useState({
    notifications: {
      lowStockAlerts: true,
      emailNotifications: true,
      pushNotifications: false,
      weeklyReports: true,
    },
    inventory: {
      defaultMinStock: '10',
      defaultMaxStock: '100',
      autoReorderThreshold: '5',
    },
    system: {
      autoBackup: true,
      dataRetention: '365',
    }
  });

  const [saving, setSaving] = useState(false);

  const handleSave = async () => {
    setSaving(true);
    // Simulate API call
    await new Promise(resolve => setTimeout(resolve, 1000));
    setSaving(false);
  };

  const SettingSection: React.FC<{
    title: string;
    description: string;
    icon: React.ReactNode;
    children: React.ReactNode;
  }> = ({ title, description, icon, children }) => (
    <Card>
      <div className="flex items-start gap-4 mb-6">
        <div className="p-3 bg-indigo-100 dark:bg-indigo-900/30 rounded-xl">
          {icon}
        </div>
        <div>
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white">{title}</h3>
          <p className="text-gray-600 dark:text-slate-400 text-sm">{description}</p>
        </div>
      </div>
      {children}
    </Card>
  );

  const ToggleSetting: React.FC<{
    label: string;
    description: string;
    checked: boolean;
    onChange: (checked: boolean) => void;
  }> = ({ label, description, checked, onChange }) => (
    <div className="flex items-center justify-between py-3">
      <div>
        <p className="font-medium text-gray-900 dark:text-white">{label}</p>
        <p className="text-sm text-gray-600 dark:text-slate-400">{description}</p>
      </div>
      <label className="relative inline-flex items-center cursor-pointer">
        <input
          type="checkbox"
          checked={checked}
          onChange={(e) => onChange(e.target.checked)}
          className="sr-only peer"
        />
        <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-indigo-300 dark:peer-focus:ring-indigo-800 rounded-full peer dark:bg-gray-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-gray-600 peer-checked:bg-indigo-600"></div>
      </label>
    </div>
  );

  return (
    <Layout 
      title="Settings" 
      subtitle="Configure your inventory management preferences"
      action={
        <Button 
          leftIcon={<Save size={18} />}
          loading={saving}
          onClick={handleSave}
        >
          Save Changes
        </Button>
      }
    >
      <div className="space-y-8">
        {/* Notifications */}
        <SettingSection
          title="Notifications"
          description="Control how and when you receive notifications"
          icon={<Bell size={24} className="text-indigo-600 dark:text-indigo-400" />}
        >
          <div className="space-y-1 divide-y divide-gray-200 dark:divide-slate-700">
            <ToggleSetting
              label="Low Stock Alerts"
              description="Get notified when products fall below minimum stock levels"
              checked={settings.notifications.lowStockAlerts}
              onChange={(checked) => setSettings(prev => ({
                ...prev,
                notifications: { ...prev.notifications, lowStockAlerts: checked }
              }))}
            />
            <ToggleSetting
              label="Email Notifications"
              description="Receive notifications via email"
              checked={settings.notifications.emailNotifications}
              onChange={(checked) => setSettings(prev => ({
                ...prev,
                notifications: { ...prev.notifications, emailNotifications: checked }
              }))}
            />
            <ToggleSetting
              label="Push Notifications"
              description="Receive browser push notifications"
              checked={settings.notifications.pushNotifications}
              onChange={(checked) => setSettings(prev => ({
                ...prev,
                notifications: { ...prev.notifications, pushNotifications: checked }
              }))}
            />
            <ToggleSetting
              label="Weekly Reports"
              description="Get weekly inventory summary reports"
              checked={settings.notifications.weeklyReports}
              onChange={(checked) => setSettings(prev => ({
                ...prev,
                notifications: { ...prev.notifications, weeklyReports: checked }
              }))}
            />
          </div>
        </SettingSection>

        {/* Inventory Defaults */}
        <SettingSection
          title="Inventory Defaults"
          description="Set default values for new products and stock management"
          icon={<Palette size={24} className="text-indigo-600 dark:text-indigo-400" />}
        >
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            <Input
              label="Default Minimum Stock"
              type="number"
              value={settings.inventory.defaultMinStock}
              onChange={(e) => setSettings(prev => ({
                ...prev,
                inventory: { ...prev.inventory, defaultMinStock: e.target.value }
              }))}
            />
            <Input
              label="Default Maximum Stock"
              type="number"
              value={settings.inventory.defaultMaxStock}
              onChange={(e) => setSettings(prev => ({
                ...prev,
                inventory: { ...prev.inventory, defaultMaxStock: e.target.value }
              }))}
            />
            <Input
              label="Auto Reorder Threshold"
              type="number"
              value={settings.inventory.autoReorderThreshold}
              onChange={(e) => setSettings(prev => ({
                ...prev,
                inventory: { ...prev.inventory, autoReorderThreshold: e.target.value }
              }))}
            />
          </div>
        </SettingSection>

        {/* System Settings */}
        <SettingSection
          title="System & Data"
          description="Manage system behavior and data retention policies"
          icon={<Database size={24} className="text-indigo-600 dark:text-indigo-400" />}
        >
          <div className="space-y-6">
            <div className="space-y-1 divide-y divide-gray-200 dark:divide-slate-700">
              <ToggleSetting
                label="Automatic Backups"
                description="Automatically backup your data daily"
                checked={settings.system.autoBackup}
                onChange={(checked) => setSettings(prev => ({
                  ...prev,
                  system: { ...prev.system, autoBackup: checked }
                }))}
              />
            </div>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <Input
                label="Data Retention (days)"
                type="number"
                value={settings.system.dataRetention}
                onChange={(e) => setSettings(prev => ({
                  ...prev,
                  system: { ...prev.system, dataRetention: e.target.value }
                }))}
              />
            </div>
          </div>
        </SettingSection>

        {/* Security */}
        <SettingSection
          title="Security"
          description="Manage your account security preferences"
          icon={<Shield size={24} className="text-indigo-600 dark:text-indigo-400" />}
        >
          <div className="space-y-4">
            <div className="bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800 rounded-lg p-4">
              <p className="text-yellow-800 dark:text-yellow-400 text-sm">
                Security settings like password changes and two-factor authentication 
                should be handled through your profile settings.
              </p>
            </div>
            
            <div className="flex gap-3">
              <Button variant="outline">
                Change Password
              </Button>
              <Button variant="outline">
                Enable 2FA
              </Button>
            </div>
          </div>
        </SettingSection>
      </div>
    </Layout>
  );
};

export default SettingsPage;