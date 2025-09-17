import React from 'react';
import { useTheme } from '../../contexts/ThemeContext';
import { Sun, Moon } from 'lucide-react';
import Sidebar from './Sidebar';

interface LayoutProps {
  children: React.ReactNode;
  title: string;
  subtitle?: string;
  action?: React.ReactNode;
}

const Layout: React.FC<LayoutProps> = ({ children, title, subtitle, action }) => {
  const { isDarkMode, toggleTheme } = useTheme();

  return (
    <div className="flex h-screen bg-gray-50 dark:bg-slate-900">
      {/* Sidebar */}
      <div className="w-64 flex-shrink-0">
        <Sidebar />
      </div>

      {/* Main Content */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Header */}
        <header className="bg-white dark:bg-slate-800 border-b border-gray-200 dark:border-slate-700 px-6 py-4">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold text-gray-900 dark:text-white">{title}</h1>
              {subtitle && (
                <p className="text-gray-600 dark:text-slate-400 mt-1">{subtitle}</p>
              )}
            </div>
            
            <div className="flex items-center gap-4">
              <button
                onClick={toggleTheme}
                className="p-2 rounded-lg bg-gray-100 hover:bg-gray-200 dark:bg-slate-700 dark:hover:bg-slate-600 transition-colors"
                aria-label="Toggle theme"
              >
                {isDarkMode ? (
                  <Sun size={20} className="text-yellow-500" />
                ) : (
                  <Moon size={20} className="text-slate-600" />
                )}
              </button>
              {action}
            </div>
          </div>
        </header>

        {/* Page Content */}
        <main className="flex-1 overflow-auto p-6">
          {children}
        </main>
      </div>
    </div>
  );
};

export default Layout;