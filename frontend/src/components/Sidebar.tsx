'use client';
import React, { useState } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import AppLogo from '@/components/ui/AppLogo';
import {
  LayoutDashboard,
  Package,
  Users,
  ClipboardList,
  ScanLine,
  ChevronLeft,
  ChevronRight,
  ArrowLeft,
  Boxes,
} from 'lucide-react';

interface NavItem {
  key: string;
  label: string;
  href: string;
  icon: React.ReactNode;
  badge?: number;
}

interface SidebarProps {
  collapsed: boolean;
  onToggle: () => void;
  spaceId?: string;
  spaceName?: string;
  spaceCode?: string;
}

export default function Sidebar({ collapsed, onToggle, spaceId, spaceName = 'My Space', spaceCode }: SidebarProps) {
  const pathname = usePathname();

  const navItems: NavItem[] = spaceId ? [
    {
      key: 'nav-dashboard',
      label: 'Dashboard',
      href: `/spaces/${spaceId}/dashboard`,
      icon: <LayoutDashboard size={18} />,
    },
    {
      key: 'nav-products',
      label: 'Products',
      href: `/spaces/${spaceId}/products`,
      icon: <Package size={18} />,
      badge: 14,
    },
    {
      key: 'nav-members',
      label: 'Members',
      href: `/spaces/${spaceId}/members`,
      icon: <Users size={18} />,
    },
    {
      key: 'nav-audit',
      label: 'Audit Log',
      href: `/spaces/${spaceId}/audit-log`,
      icon: <ClipboardList size={18} />,
    },
  ] : [
    {
      key: 'nav-spaces',
      label: 'My Spaces',
      href: '/spaces',
      icon: <Boxes size={18} />,
    },
  ];

  const isActive = (href: string) => {
    return pathname === href || pathname.startsWith(href + '/');
  };

  return (
    <aside
      className="flex flex-col h-screen bg-primary text-primary-foreground fixed left-0 top-0 z-30 transition-all duration-300 ease-in-out"
      style={{ width: collapsed ? 64 : 240 }}
    >
      {/* Brand */}
      <div className="flex items-center gap-3 px-4 py-5 border-b border-white/10 min-h-[68px]">
        <div className="flex-shrink-0">
          <AppLogo size={32} />
        </div>
        {!collapsed && (
          <div className="fade-in overflow-hidden">
            <div className="font-semibold text-white text-sm leading-tight truncate max-w-[140px]">
              {spaceId ? spaceName : 'StockRoom'}
            </div>
            <div className="text-white/50 text-xs font-medium tracking-wide">
              {spaceId ? (spaceCode || spaceId.slice(0, 8).toUpperCase()) : 'Inventory Manager'}
            </div>
          </div>
        )}
      </div>

      {/* Back to Spaces (when inside a space) */}
      {spaceId && (
        <div className="px-2 pt-3 pb-1">
          <Link
            href="/spaces"
            className="flex items-center gap-2 px-3 py-2 rounded-lg text-white/50 hover:text-white/90 hover:bg-white/08 transition-colors text-xs font-medium"
            title={collapsed ? 'All Spaces' : undefined}
          >
            <ArrowLeft size={14} className="flex-shrink-0" />
            {!collapsed && <span className="fade-in">All Spaces</span>}
          </Link>
        </div>
      )}

      {/* Navigation */}
      <nav className="flex-1 px-2 py-4 space-y-1 overflow-hidden">
        {!collapsed && (
          <p className="text-white/30 text-xs font-semibold uppercase tracking-widest px-3 pb-2">
            {spaceId ? 'Space Menu' : 'Menu'}
          </p>
        )}
        {navItems.map((item) => (
          <Link
            key={item.key}
            href={item.href}
            className={`sidebar-nav-item ${isActive(item.href) ? 'active' : ''} relative`}
            title={collapsed ? item.label : undefined}
          >
            <span className="flex-shrink-0">{item.icon}</span>
            {!collapsed && (
              <span className="flex-1 fade-in">{item.label}</span>
            )}
            {!collapsed && item.badge && item.badge > 0 && (
              <span className="bg-danger text-white text-xs font-bold rounded-full px-1.5 py-0.5 min-w-[20px] text-center leading-none fade-in">
                {item.badge}
              </span>
            )}
            {collapsed && item.badge && item.badge > 0 && (
              <span className="absolute top-1 right-1 bg-danger rounded-full w-2 h-2" />
            )}
          </Link>
        ))}
      </nav>

      {/* Quick Scan CTA */}
      <div className="px-2 py-4 border-t border-white/10">
        <button
          className="w-full flex items-center justify-center gap-2 bg-white/10 hover:bg-white/20 text-white font-semibold text-sm py-2.5 rounded-lg transition-all duration-150 active:scale-95"
          title={collapsed ? 'Quick Scan' : undefined}
        >
          <ScanLine size={16} />
          {!collapsed && <span className="fade-in">Quick Scan</span>}
        </button>
      </div>

      {/* Collapse toggle */}
      <button
        onClick={onToggle}
        className="absolute -right-3 top-[72px] bg-card border border-border rounded-full w-6 h-6 flex items-center justify-center shadow-card hover:shadow-elevated transition-shadow z-40"
        aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
      >
        {collapsed ? (
          <ChevronRight size={12} className="text-muted-foreground" />
        ) : (
          <ChevronLeft size={12} className="text-muted-foreground" />
        )}
      </button>
    </aside>
  );
}