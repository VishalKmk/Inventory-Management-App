import React, { useState } from 'react';
import { Save, User, Mail, Calendar, Shield } from 'lucide-react';
import Layout from '../components/layout/Layout';
import Card from '../components/ui/Card';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import { useAuth } from '../contexts/AuthContext';

const ProfilePage: React.FC = () => {
  const { user } = useAuth();
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [formData, setFormData] = useState({
    name: user?.name || '',
    email: user?.email || '',
  });

  const handleSave = async () => {
    setSaving(true);
    // Simulate API call
    await new Promise(resolve => setTimeout(resolve, 1000));
    setEditing(false);
    setSaving(false);
  };

  const handleCancel = () => {
    setFormData({
      name: user?.name || '',
      email: user?.email || '',
    });
    setEditing(false);
  };

  return (
    <Layout 
      title="Profile" 
      subtitle="Manage your account information and preferences"
    >
      <div className="max-w-4xl space-y-8">
        {/* Profile Information */}
        <Card>
          <div className="flex items-start justify-between mb-6">
            <div className="flex items-center gap-4">
              <div className="w-16 h-16 bg-gradient-to-br from-indigo-500 to-purple-600 rounded-2xl flex items-center justify-center">
                <span className="text-2xl font-bold text-white">
                  {user?.name?.charAt(0).toUpperCase() || 'U'}
                </span>
              </div>
              <div>
                <h3 className="text-xl font-semibold text-gray-900 dark:text-white">{user?.name}</h3>
                <p className="text-gray-600 dark:text-slate-400">{user?.email}</p>
              </div>
            </div>
            
            {!editing && (
              <Button onClick={() => setEditing(true)}>
                Edit Profile
              </Button>
            )}
          </div>

          {editing ? (
            <div className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <Input
                  label="Full Name"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  leftIcon={<User size={18} />}
                />
                <Input
                  label="Email Address"
                  type="email"
                  value={formData.email}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                  leftIcon={<Mail size={18} />}
                />
              </div>
              
              <div className="flex gap-3 pt-4">
                <Button
                  variant="outline"
                  onClick={handleCancel}
                >
                  Cancel
                </Button>
                <Button
                  leftIcon={<Save size={18} />}
                  loading={saving}
                  onClick={handleSave}
                >
                  Save Changes
                </Button>
              </div>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
              <div className="space-y-6">
                <div className="flex items-center gap-3">
                  <User size={20} className="text-gray-400" />
                  <div>
                    <p className="text-sm text-gray-600 dark:text-slate-400">Full Name</p>
                    <p className="font-medium text-gray-900 dark:text-white">{user?.name}</p>
                  </div>
                </div>
                
                <div className="flex items-center gap-3">
                  <Mail size={20} className="text-gray-400" />
                  <div>
                    <p className="text-sm text-gray-600 dark:text-slate-400">Email Address</p>
                    <p className="font-medium text-gray-900 dark:text-white">{user?.email}</p>
                  </div>
                </div>
              </div>

              <div className="space-y-6">
                <div className="flex items-center gap-3">
                  <Calendar size={20} className="text-gray-400" />
                  <div>
                    <p className="text-sm text-gray-600 dark:text-slate-400">Member Since</p>
                    <p className="font-medium text-gray-900 dark:text-white">
                      {user?.createdAt ? new Date(user.createdAt).toLocaleDateString() : 'N/A'}
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  <Shield size={20} className="text-green-500" />
                  <div>
                    <p className="text-sm text-gray-600 dark:text-slate-400">Account Status</p>
                    <p className="font-medium text-green-600 dark:text-green-400">
                      {user?.verified ? 'Verified' : 'Unverified'}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          )}
        </Card>

        {/* Account Statistics */}
        <Card>
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-6">Account Activity</h3>
          
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="text-center p-6 bg-gradient-to-br from-blue-50 to-blue-100 dark:from-blue-900/20 dark:to-blue-800/20 rounded-xl">
              <p className="text-2xl font-bold text-blue-600 dark:text-blue-400">0</p>
              <p className="text-sm text-gray-600 dark:text-slate-400 mt-1">Total Spaces</p>
            </div>
            
            <div className="text-center p-6 bg-gradient-to-br from-green-50 to-green-100 dark:from-green-900/20 dark:to-green-800/20 rounded-xl">
              <p className="text-2xl font-bold text-green-600 dark:text-green-400">0</p>
              <p className="text-sm text-gray-600 dark:text-slate-400 mt-1">Total Products</p>
            </div>
            
            <div className="text-center p-6 bg-gradient-to-br from-purple-50 to-purple-100 dark:from-purple-900/20 dark:to-purple-800/20 rounded-xl">
              <p className="text-2xl font-bold text-purple-600 dark:text-purple-400">$0</p>
              <p className="text-sm text-gray-600 dark:text-slate-400 mt-1">Total Value</p>
            </div>
          </div>
        </Card>

        {/* Security Settings */}
        <Card>
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-6">Security</h3>
          
          <div className="space-y-4">
            <div className="flex items-center justify-between py-3 border-b border-gray-200 dark:border-slate-700">
              <div>
                <p className="font-medium text-gray-900 dark:text-white">Password</p>
                <p className="text-sm text-gray-600 dark:text-slate-400">Last changed 30 days ago</p>
              </div>
              <Button variant="outline" size="sm">
                Change Password
              </Button>
            </div>
            
            <div className="flex items-center justify-between py-3 border-b border-gray-200 dark:border-slate-700">
              <div>
                <p className="font-medium text-gray-900 dark:text-white">Two-Factor Authentication</p>
                <p className="text-sm text-gray-600 dark:text-slate-400">Add an extra layer of security</p>
              </div>
              <Button variant="outline" size="sm">
                Enable 2FA
              </Button>
            </div>
            
            <div className="flex items-center justify-between py-3">
              <div>
                <p className="font-medium text-gray-900 dark:text-white">Active Sessions</p>
                <p className="text-sm text-gray-600 dark:text-slate-400">Manage your active login sessions</p>
              </div>
              <Button variant="outline" size="sm">
                View Sessions
              </Button>
            </div>
          </div>
        </Card>
      </div>
    </Layout>
  );
};

export default ProfilePage;