'use client';

import React, { useEffect, useMemo, useState } from 'react';
import { Calendar, ChevronLeft, ChevronRight, Download, Package, Settings, UserPlus } from 'lucide-react';
import AppLayout from '@/components/AppLayout';
import { Skeleton } from '@/components/ui/LoadingSkeleton';
import { apiFetch } from '@/lib/api';

type AuditLog = {
  id: string;
  entityType: string;
  operation: string;
  details: string | null;
  timestamp: string;
};

type AuditLogResponse = {
  data: AuditLog[];
  pagination: { page: number; totalPages: number; totalElements: number; hasNext: boolean; hasPrevious: boolean };
};

const ENTITY_ICONS: Record<string, React.ReactNode> = {
  PRODUCT: <Package size={16} />,
  SPACE: <Settings size={16} />,
  USER: <UserPlus size={16} />,
};

function descriptionFor(log: AuditLog) {
  try {
    const details = log.details ? JSON.parse(log.details) as { action?: string; productName?: string; spaceName?: string } : {};
    if (details.action) return details.action;
    if (details.productName) return `${log.operation} ${details.productName}`;
    if (details.spaceName) return `${log.operation} ${details.spaceName}`;
  } catch { /* Details from older audit events may not be JSON. */ }
  return `${log.operation.replaceAll('_', ' ')} ${log.entityType}`;
}

export default function SpaceAuditLogContent({ spaceId }: { spaceId: string }) {
  const [entityType, setEntityType] = useState('');
  const [operation, setOperation] = useState('');
  const [page, setPage] = useState(0);
  const [response, setResponse] = useState<AuditLogResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const query = useMemo(() => {
    const params = new URLSearchParams({ page: String(page), size: '15' });
    if (entityType) params.set('entityType', entityType);
    if (operation) params.set('operation', operation);
    return params.toString();
  }, [entityType, operation, page]);

  useEffect(() => {
    let active = true;
    setResponse(null);
    apiFetch<AuditLogResponse>(`/api/spaces/${spaceId}/audit-logs?${query}`)
      .then((data) => { if (active) { setResponse(data); setError(null); } })
      .catch((reason) => { if (active) setError(reason instanceof Error ? reason.message : 'Unable to load audit logs'); });
    return () => { active = false; };
  }, [spaceId, query]);

  const logs = response?.data ?? [];
  const pagination = response?.pagination;
  const exportCsv = () => {
    const rows = [['Entity type', 'Operation', 'Description', 'Timestamp'], ...logs.map((log) => [log.entityType, log.operation, descriptionFor(log), log.timestamp])];
    const csv = rows.map((row) => row.map((value) => `"${String(value).replaceAll('"', '""')}"`).join(',')).join('\n');
    const link = document.createElement('a');
    link.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
    link.download = `space-${spaceId}-audit-log.csv`;
    link.click();
    URL.revokeObjectURL(link.href);
  };

  return (
    <AppLayout spaceId={spaceId} spaceCode={spaceId.slice(0, 8).toUpperCase()} userRole="MEMBER">
      <div className="px-6 py-6 max-w-screen-2xl mx-auto">
        <div className="flex items-start justify-between mb-6">
          <div><h1 className="text-2xl font-bold text-foreground">Audit Log</h1><p className="text-sm text-muted-foreground mt-0.5">A record of activity in this space.</p></div>
          <button onClick={exportCsv} disabled={!logs.length} className="btn-secondary text-sm"><Download size={14} /> Export CSV</button>
        </div>
        <div className="bg-card border border-border rounded-xl p-4 mb-6 grid grid-cols-1 md:grid-cols-2 gap-3">
          <label className="text-xs font-medium text-muted-foreground">Entity Type
            <select value={entityType} onChange={(event) => { setEntityType(event.target.value); setPage(0); }} className="input-field text-sm py-2 mt-1.5"><option value="">All entities</option><option value="PRODUCT">Product</option><option value="SPACE">Space</option><option value="USER">User</option></select>
          </label>
          <label className="text-xs font-medium text-muted-foreground">Operation
            <select value={operation} onChange={(event) => { setOperation(event.target.value); setPage(0); }} className="input-field text-sm py-2 mt-1.5"><option value="">All operations</option><option value="CREATE">Create</option><option value="UPDATE">Update</option><option value="DELETE">Delete</option><option value="STOCK_ADD">Stock add</option><option value="STOCK_REMOVE">Stock remove</option><option value="STOCK_UPDATE">Stock update</option></select>
          </label>
        </div>
        <div className="section-card">
          <div className="flex items-center justify-between px-5 py-4 border-b border-border"><h2 className="font-semibold text-foreground">Recent Activity</h2>{pagination && <span className="text-xs text-muted-foreground">{pagination.totalElements} entries</span>}</div>
          {error ? <p className="p-5 text-danger">{error}</p> : response === null ? <Skeleton className="h-64 w-full" /> : <div className="divide-y divide-border">{logs.length ? logs.map((log) => <div key={log.id} className="flex items-center gap-4 px-5 py-4"><div className="w-9 h-9 rounded-full bg-secondary flex items-center justify-center text-muted-foreground">{ENTITY_ICONS[log.entityType] ?? <Calendar size={16} />}</div><div className="flex-1"><p className="text-sm font-medium text-foreground">{descriptionFor(log)}</p><p className="text-xs text-muted-foreground mt-1">{log.entityType} · {log.operation}</p></div><time className="text-xs text-muted-foreground">{new Date(log.timestamp).toLocaleString()}</time></div>) : <p className="p-5 text-sm text-muted-foreground">No activity matches these filters.</p>}</div>}
          <div className="flex items-center justify-between px-5 py-3 border-t border-border"><p className="text-xs text-muted-foreground">{pagination ? `Page ${pagination.page + 1} of ${Math.max(1, pagination.totalPages)}` : ''}</p><div className="flex gap-1"><button onClick={() => setPage((current) => current - 1)} disabled={!pagination?.hasPrevious} className="w-8 h-8 flex items-center justify-center rounded border border-border disabled:opacity-40"><ChevronLeft size={14} /></button><button onClick={() => setPage((current) => current + 1)} disabled={!pagination?.hasNext} className="w-8 h-8 flex items-center justify-center rounded border border-border disabled:opacity-40"><ChevronRight size={14} /></button></div></div>
        </div>
      </div>
    </AppLayout>
  );
}
