'use client';
import React, { useCallback, useEffect, useState } from 'react';
import AppLayout from '@/components/AppLayout';
import { UserPlus, Trash2, Crown, UserCheck, Users, Eye, Clock, X, Mail,  } from 'lucide-react';
import { toast } from 'sonner';
import { apiFetch } from '@/lib/api';

const SPACE_NAMES: Record<string, string> = {
  'space-wh001': 'Main Warehouse',
  'space-off002': 'Office Supplies',
  'space-lab003': 'Lab Equipment',
  'space-shared001': 'East Wing Storage',
  'space-shared002': 'Cold Storage Unit B',
};
const SPACE_CODES: Record<string, string> = {
  'space-wh001': 'WH-001',
  'space-off002': 'OFF-002',
  'space-lab003': 'LAB-003',
  'space-shared001': 'EWS-001',
  'space-shared002': 'CSB-002',
};

type Role = 'OWNER' | 'ADMIN' | 'MEMBER' | 'VIEWER' | 'PENDING';

interface Member {
  id: string;
  userId: string;
  userName: string;
  email: string;
  role: Role;
  joinedAt: string;
}

const INITIAL_MEMBERS: Member[] = [
  { id: 'm1', userId: 'u1', userName: 'Alex Rivera', email: 'alex.rivera@nexacorp.io', role: 'OWNER', joinedAt: '2024-01-15T10:00:00Z' },
  { id: 'm2', userId: 'u2', userName: 'Sarah Chen', email: 'sarah.c@company.com', role: 'ADMIN', joinedAt: '2024-02-01T09:00:00Z' },
  { id: 'm3', userId: 'u3', userName: 'James Wilson', email: 'james.w@company.com', role: 'MEMBER', joinedAt: '2024-03-10T11:00:00Z' },
  { id: 'm4', userId: 'u4', userName: 'Diana Park', email: 'diana.p@company.com', role: 'VIEWER', joinedAt: '2024-04-05T14:00:00Z' },
  { id: 'm5', userId: 'u5', userName: 'Tom Bradley', email: 'tom.b@company.com', role: 'PENDING', joinedAt: '2024-07-18T10:00:00Z' },
];

const ROLE_CONFIG: Record<Role, { label: string; color: string; icon: React.ReactNode }> = {
  OWNER: { label: 'Owner', color: 'bg-amber-50 text-amber-700 border border-amber-200', icon: <Crown size={11} /> },
  ADMIN: { label: 'Admin', color: 'bg-blue-50 text-blue-700 border border-blue-200', icon: <UserCheck size={11} /> },
  MEMBER: { label: 'Member', color: 'bg-green-50 text-green-700 border border-green-200', icon: <Users size={11} /> },
  VIEWER: { label: 'Viewer', color: 'bg-gray-100 text-gray-600 border border-gray-200', icon: <Eye size={11} /> },
  PENDING: { label: 'Pending', color: 'bg-orange-50 text-orange-600 border border-orange-200', icon: <Clock size={11} /> },
};

function getInitials(name: string) {
  return name.split(' ').map((n) => n[0]).join('').toUpperCase().slice(0, 2);
}

function getAvatarColor(name: string) {
  const colors = ['bg-blue-500', 'bg-green-500', 'bg-purple-500', 'bg-pink-500', 'bg-indigo-500', 'bg-teal-500'];
  return colors[name.charCodeAt(0) % colors.length];
}

function InviteModal({ onClose, onInvite }: { onClose: () => void; onInvite: (email: string, role: Role) => void }) {
  const [email, setEmail] = useState('');
  const [role, setRole] = useState<Role>('MEMBER');
  return (
    <div className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4">
      <div className="bg-card rounded-xl border border-border shadow-xl w-full max-w-md p-6">
        <div className="flex items-center justify-between mb-5">
          <h2 className="text-lg font-semibold text-foreground">Invite Member</h2>
          <button onClick={onClose} className="text-muted-foreground hover:text-foreground"><X size={18} /></button>
        </div>
        <div className="space-y-4 mb-5">
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5">Email Address</label>
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="name@company.com" className="input-field" autoFocus />
          </div>
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5">Role</label>
            <select value={role} onChange={(e) => setRole(e.target.value as Role)} className="input-field">
              <option value="ADMIN">Admin</option>
              <option value="MEMBER">Member</option>
              <option value="VIEWER">Viewer</option>
            </select>
            <p className="text-xs text-muted-foreground mt-1.5">Only Owners can invite Admins or Owners.</p>
          </div>
        </div>
        <div className="flex gap-3 justify-end">
          <button onClick={onClose} className="btn-secondary">Cancel</button>
          <button disabled={!email.trim()} onClick={() => email.trim() && onInvite(email.trim(), role)} className="btn-primary">
            <Mail size={14} /> Send Invite
          </button>
        </div>
      </div>
    </div>
  );
}

export default function SpaceMembersContent({ spaceId }: { spaceId: string }) {
  const spaceName = SPACE_NAMES[spaceId] || 'My Space';
  const spaceCode = SPACE_CODES[spaceId] || spaceId.slice(0, 8).toUpperCase();
  const [members, setMembers] = useState<Member[]>([]);
  const [inviteOpen, setInviteOpen] = useState(false);
  const [page, setPage] = useState(1);
  const PAGE_SIZE = 10;

  const loadMembers = useCallback(async () => {
    try {
      const response = await apiFetch<{ content: Member[] }>(`/api/spaces/${spaceId}/members?page=0&size=100`);
      setMembers(response.content);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Unable to load members');
    }
  }, [spaceId]);

  useEffect(() => { void loadMembers(); }, [loadMembers]);

  const totalPages = Math.ceil(members.length / PAGE_SIZE);
  const paginated = members.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  const handleInvite = async (email: string, role: Role) => {
    try {
      await apiFetch(`/api/spaces/${spaceId}/members/invite`, { method: 'POST', body: JSON.stringify({ email, role }) });
      setInviteOpen(false);
      toast.success('Invitation sent');
      await loadMembers();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Unable to invite member');
    }
  };

  const handleRemove = async (userId: string) => {
    try {
      await apiFetch(`/api/spaces/${spaceId}/members/${userId}`, { method: 'DELETE' });
      await loadMembers();
      toast.success('Member removed');
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Unable to remove member');
    }
  };

  return (
    <AppLayout
      searchPlaceholder="Search members..."
      userName="Alex Rivera"
      userRole="ADMIN"
      spaceId={spaceId}
      spaceName={spaceName}
      spaceCode={spaceCode}
    >
      <div className="px-6 py-6 max-w-screen-2xl mx-auto">
        {/* Header */}
        <div className="flex items-start justify-between mb-6">
          <div>
            <h1 className="text-2xl font-bold text-foreground">Members</h1>
            <p className="text-sm text-muted-foreground mt-0.5">{spaceCode} · {members.length} members</p>
          </div>
          <button onClick={() => setInviteOpen(true)} className="btn-primary">
            <UserPlus size={15} /> Invite Member
          </button>
        </div>

        {/* Role Summary */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-6">
          {(['OWNER', 'ADMIN', 'MEMBER', 'VIEWER'] as Role[]).map((role) => {
            const count = members.filter((m) => m.role === role).length;
            const cfg = ROLE_CONFIG[role];
            return (
              <div key={role} className="bg-card border border-border rounded-xl p-4 flex items-center gap-3">
                <div className={`w-8 h-8 rounded-lg flex items-center justify-center ${cfg.color}`}>{cfg.icon}</div>
                <div>
                  <p className="text-xs text-muted-foreground">{cfg.label}s</p>
                  <p className="text-lg font-bold text-foreground">{count}</p>
                </div>
              </div>
            );
          })}
        </div>

        {/* Members Table */}
        <div className="section-card">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-border">
                  <th className="text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider px-5 py-3">Member</th>
                  <th className="text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider px-4 py-3">Email</th>
                  <th className="text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider px-4 py-3">Role</th>
                  <th className="text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider px-4 py-3">Joined</th>
                  <th className="text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider px-4 py-3">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {paginated.map((m) => {
                  const cfg = ROLE_CONFIG[m.role];
                  const date = new Date(m.joinedAt);
                  const dateStr = `${date.toLocaleString('default', { month: 'short' })} ${date.getDate()}, ${date.getFullYear()}`;
                  return (
                    <tr key={m.id} className="hover:bg-secondary/40 transition-colors">
                      <td className="px-5 py-3.5">
                        <div className="flex items-center gap-3">
                          <div className={`w-8 h-8 rounded-full ${getAvatarColor(m.userName)} flex items-center justify-center text-white text-xs font-bold flex-shrink-0`}>
                            {getInitials(m.userName)}
                          </div>
                          <span className="text-sm font-semibold text-foreground">{m.userName}</span>
                        </div>
                      </td>
                      <td className="px-4 py-3.5 text-sm text-muted-foreground">{m.email}</td>
                      <td className="px-4 py-3.5">
                        <span className={`inline-flex items-center gap-1 text-xs font-semibold px-2 py-0.5 rounded-full ${cfg.color}`}>
                          {cfg.icon} {cfg.label}
                        </span>
                      </td>
                      <td className="px-4 py-3.5 text-sm text-muted-foreground">{dateStr}</td>
                      <td className="px-4 py-3.5">
                        {m.role !== 'OWNER' && (
                          <button
                            onClick={() => handleRemove(m.userId)}
                            className="w-7 h-7 flex items-center justify-center rounded border border-border hover:bg-red-50 hover:text-danger transition-colors"
                          >
                            <Trash2 size={13} />
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
          <div className="flex items-center justify-between px-5 py-3 border-t border-border">
            <p className="text-xs text-muted-foreground">Showing {Math.min((page - 1) * PAGE_SIZE + 1, members.length)}–{Math.min(page * PAGE_SIZE, members.length)} of {members.length} members</p>
            <div className="flex gap-2">
              <button onClick={() => setPage((p) => Math.max(1, p - 1))} disabled={page === 1} className="btn-secondary text-xs py-1.5 px-3 disabled:opacity-40">Previous</button>
              <button onClick={() => setPage((p) => Math.min(totalPages, p + 1))} disabled={page === totalPages} className="btn-secondary text-xs py-1.5 px-3 disabled:opacity-40">Next</button>
            </div>
          </div>
        </div>
      </div>
      {inviteOpen && <InviteModal onClose={() => setInviteOpen(false)} onInvite={handleInvite} />}
    </AppLayout>
  );
}
