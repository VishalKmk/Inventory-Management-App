import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { 
  LayoutDashboard, 
  Warehouse, 
  BarChart3, 
  Settings, 
  User, 
  LogOut,
  Box
} from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';

const Sidebar: React.FC = () => {
  const location = useLocation();
  const { logout, user } = useAuth();

  const navigation = [
    { name: 'Dashboard', href: '/dashboard', icon: LayoutDashboard },
    { name: 'Spaces', href: '/spaces', icon: Warehouse },
    { name: 'Analytics', href: '/analytics', icon: BarChart3 },
    { name: 'Settings', href: '/settings', icon: Settings },
    { name: 'Profile', href: '/profile', icon: User },
  ];

  return (
    <div className="flex flex-col h-full bg-slate-900 dark:bg-slate-950 text-white">
      {/* Logo */}
      <div className="flex items-center gap-3 p-6 border-b border-slate-700">
        <div className="p-2 bg-indigo-600 rounded-lg">
          <Box size={24} />
        </div>
        <div>
          <h1 className="font-bold text-xl">IMS</h1>
          <p className="text-slate-400 text-sm">Inventory System</p>
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 p-4 space-y-2">
        {navigation.map((item) => {
          const isActive = location.pathname === item.href;
          return (
            <Link
              key={item.name}
              to={item.href}
              className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-200 ${
                isActive
                  ? 'bg-indigo-600 text-white shadow-lg'
                  : 'text-slate-300 hover:text-white hover:bg-slate-700'
              }`}
            >
              <item.icon size={20} />
              <span className="font-medium">{item.name}</span>
            </Link>
          );
        })}
      </nav>

      {/* User Info & Logout */}
      <div className="p-4 border-t border-slate-700">
        <div className="flex items-center gap-3 mb-3">
          <div className="w-8 h-8 bg-indigo-500 rounded-full flex items-center justify-center">
            <span className="text-sm font-medium">
              {user?.name?.charAt(0).toUpperCase() || 'U'}
            </span>
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium text-white truncate">{user?.name}</p>
            <p className="text-xs text-slate-400 truncate">{user?.email}</p>
          </div>
        </div>
        
        <button
          onClick={logout}
          className="flex items-center gap-3 w-full px-4 py-2 text-slate-300 hover:text-white hover:bg-slate-700 rounded-lg transition-colors"
        >
          <LogOut size={18} />
          <span>Logout</span>
        </button>
      </div>
    </div>
  );
};

export default Sidebar;