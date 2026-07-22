'use client';
import React, { useState } from 'react';
import Sidebar from './Sidebar';
import Topbar from './Topbar';

interface AppLayoutProps {
  children: React.ReactNode;
  searchPlaceholder?: string;
  userName?: string;
  userRole?: string;
  spaceId?: string;
  spaceName?: string;
  spaceCode?: string;
}

export default function AppLayout({
  children,
  searchPlaceholder,
  userName,
  userRole,
  spaceId,
  spaceName,
  spaceCode,
}: AppLayoutProps) {
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

  return (
    <div className="flex h-screen overflow-hidden bg-background">
      <Sidebar
        collapsed={sidebarCollapsed}
        onToggle={() => setSidebarCollapsed((p) => !p)}
        spaceId={spaceId}
        spaceName={spaceName}
        spaceCode={spaceCode}
      />
      <div
        className="flex flex-col flex-1 overflow-hidden transition-all duration-300"
        style={{ marginLeft: sidebarCollapsed ? 64 : 240 }}
      >
        <Topbar
          searchPlaceholder={searchPlaceholder}
          userName={userName}
          userRole={userRole}
        />
        <main className="flex-1 overflow-y-auto">
          {children}
        </main>
      </div>
    </div>
  );
}