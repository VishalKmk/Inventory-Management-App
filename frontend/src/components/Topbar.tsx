'use client';
import React, { useState } from 'react';
import { Search, Bell, Settings } from 'lucide-react';
import AppImage from '@/components/ui/AppImage';

interface TopbarProps {
  searchPlaceholder?: string;
  userName?: string;
  userRole?: string;
  userAvatar?: string;
  notificationCount?: number;
}

export default function Topbar({
  searchPlaceholder = 'Search inventory, batches, or SKU...',
  userName = 'Alex Rivera',
  userRole = 'Inventory Manager',
  userAvatar = 'https://i.pravatar.cc/40?img=12',
  notificationCount = 3,
}: TopbarProps) {
  const [searchValue, setSearchValue] = useState('');

  return (
    <header className="h-[68px] bg-card border-b border-border flex items-center gap-4 px-6 sticky top-0 z-20">
      {/* Search */}
      <div className="flex-1 max-w-md relative">
        <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
        <input
          type="text"
          value={searchValue}
          onChange={(e) => setSearchValue(e.target.value)}
          placeholder={searchPlaceholder}
          className="input-field pl-9 py-2 text-sm"
        />
      </div>

      <div className="flex items-center gap-2 ml-auto">
        {/* Notifications */}
        <button className="relative w-9 h-9 flex items-center justify-center rounded-lg hover:bg-secondary transition-colors">
          <Bell size={18} className="text-muted-foreground" />
          {notificationCount > 0 && (
            <span className="absolute top-1.5 right-1.5 bg-danger rounded-full w-2 h-2" />
          )}
        </button>

        {/* Settings */}
        <button className="w-9 h-9 flex items-center justify-center rounded-lg hover:bg-secondary transition-colors">
          <Settings size={18} className="text-muted-foreground" />
        </button>

        {/* Divider */}
        <div className="w-px h-6 bg-border mx-1" />

        {/* User */}
        <button className="flex items-center gap-2.5 hover:bg-secondary rounded-lg px-2 py-1.5 transition-colors">
          <div className="text-right">
            <div className="text-sm font-semibold text-foreground leading-tight">{userName}</div>
            <div className="text-xs text-muted-foreground leading-tight">{userRole}</div>
          </div>
          <div className="w-8 h-8 rounded-full overflow-hidden border-2 border-border">
            <AppImage
              src={userAvatar}
              alt={`${userName} profile photo`}
              width={32}
              height={32}
              className="w-full h-full object-cover"
            />
          </div>
        </button>
      </div>
    </header>
  );
}