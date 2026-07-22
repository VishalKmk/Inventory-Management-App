'use client';
import React, { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import AppLogo from '@/components/ui/AppLogo';
import {
  Plus,
  Boxes,
  Package,
  Users,
  MoreVertical,
  Pencil,
  Trash2,
  Bell,
  Settings,
  X,
  Check,
  ChevronRight,
  Crown,
  UserCheck,
  AlertTriangle,
} from 'lucide-react';
import AppImage from '@/components/ui/AppImage';
import { toast } from 'sonner';
import { apiFetch } from '@/lib/api';

interface Space {
  id: string;
  name: string;
  ownerId: string;
  ownerName: string;
  productCount: number;
  currentUserRole: 'OWNER' | 'ADMIN' | 'MEMBER' | 'VIEWER';
  createdAt: string;
}

interface PendingInvite {
  spaceId: string;
  spaceName: string;
  spaceOwnerName: string;
  role: string;
  invitedAt: string;
}

const MOCK_OWNED_SPACES: Space[] = [
  {
    id: 'space-wh001',
    name: 'Main Warehouse',
    ownerId: 'user-001',
    ownerName: 'Alex Rivera',
    productCount: 124,
    currentUserRole: 'OWNER',
    createdAt: '2024-01-15T10:00:00Z',
  },
  {
    id: 'space-off002',
    name: 'Office Supplies',
    ownerId: 'user-001',
    ownerName: 'Alex Rivera',
    productCount: 38,
    currentUserRole: 'OWNER',
    createdAt: '2024-03-20T09:00:00Z',
  },
  {
    id: 'space-lab003',
    name: 'Lab Equipment',
    ownerId: 'user-001',
    ownerName: 'Alex Rivera',
    productCount: 57,
    currentUserRole: 'OWNER',
    createdAt: '2024-05-10T14:00:00Z',
  },
];

const MOCK_SHARED_SPACES: Space[] = [
  {
    id: 'space-shared001',
    name: 'East Wing Storage',
    ownerId: 'user-002',
    ownerName: 'Sarah Chen',
    productCount: 89,
    currentUserRole: 'ADMIN',
    createdAt: '2024-02-01T08:00:00Z',
  },
  {
    id: 'space-shared002',
    name: 'Cold Storage Unit B',
    ownerId: 'user-003',
    ownerName: 'James Wilson',
    productCount: 22,
    currentUserRole: 'MEMBER',
    createdAt: '2024-04-12T11:00:00Z',
  },
];

const MOCK_INVITES: PendingInvite[] = [
  {
    spaceId: 'space-inv001',
    spaceName: "Marketing Supplies",
    spaceOwnerName: 'Diana Park',
    role: 'MEMBER',
    invitedAt: '2024-07-18T10:00:00Z',
  },
];

const ROLE_COLORS: Record<string, string> = {
  OWNER: 'bg-amber-50 text-amber-700 border border-amber-200',
  ADMIN: 'bg-blue-50 text-blue-700 border border-blue-200',
  MEMBER: 'bg-green-50 text-green-700 border border-green-200',
  VIEWER: 'bg-gray-100 text-gray-600 border border-gray-200',
};

const ROLE_ICONS: Record<string, React.ReactNode> = {
  OWNER: <Crown size={10} />,
  ADMIN: <UserCheck size={10} />,
  MEMBER: <Users size={10} />,
  VIEWER: <Users size={10} />,
};

function SpaceCard({
  space,
  onEdit,
  onDelete,
}: {
  space: Space;
  onEdit: (s: Space) => void;
  onDelete: (id: string) => void;
}) {
  const [menuOpen, setMenuOpen] = useState(false);

  return (
    <div className="bg-card border border-border rounded-xl p-5 hover:shadow-md transition-shadow group relative">
      <div className="flex items-start justify-between mb-4">
        <div className="w-10 h-10 bg-primary/8 rounded-lg flex items-center justify-center">
          <Boxes size={20} className="text-primary" />
        </div>
        <div className="flex items-center gap-2">
          <span className={`inline-flex items-center gap-1 text-xs font-semibold px-2 py-0.5 rounded-full ${ROLE_COLORS[space.currentUserRole]}`}>
            {ROLE_ICONS[space.currentUserRole]}
            {space.currentUserRole}
          </span>
          {space.currentUserRole === 'OWNER' && (
            <div className="relative">
              <button
                onClick={() => setMenuOpen((p) => !p)}
                className="w-7 h-7 flex items-center justify-center rounded-lg hover:bg-secondary transition-colors text-muted-foreground"
              >
                <MoreVertical size={15} />
              </button>
              {menuOpen && (
                <div className="absolute right-0 top-8 bg-card border border-border rounded-lg shadow-lg z-10 min-w-[140px] py-1">
                  <button
                    onClick={() => { onEdit(space); setMenuOpen(false); }}
                    className="w-full flex items-center gap-2 px-3 py-2 text-sm text-foreground hover:bg-secondary transition-colors"
                  >
                    <Pencil size={13} /> Rename
                  </button>
                  <button
                    onClick={() => { onDelete(space.id); setMenuOpen(false); }}
                    className="w-full flex items-center gap-2 px-3 py-2 text-sm text-danger hover:bg-red-50 transition-colors"
                  >
                    <Trash2 size={13} /> Delete
                  </button>
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      <h3 className="font-semibold text-foreground text-base mb-1 truncate">{space.name}</h3>
      {space.currentUserRole !== 'OWNER' && (
        <p className="text-xs text-muted-foreground mb-3">by {space.ownerName}</p>
      )}

      <div className="flex items-center gap-4 mt-3 mb-4">
        <div className="flex items-center gap-1.5 text-muted-foreground">
          <Package size={13} />
          <span className="text-xs font-medium">{space.productCount} products</span>
        </div>
      </div>

      <Link
        href={`/spaces/${space.id}/dashboard`}
        className="w-full flex items-center justify-center gap-2 bg-primary text-white text-sm font-semibold py-2 rounded-lg hover:opacity-90 transition-opacity"
      >
        Open Space <ChevronRight size={14} />
      </Link>
    </div>
  );
}

function CreateSpaceModal({
  onClose,
  onSubmit,
}: {
  onClose: () => void;
  onSubmit: (name: string) => void;
}) {
  const [name, setName] = useState('');
  return (
    <div className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4">
      <div className="bg-card rounded-xl border border-border shadow-xl w-full max-w-md p-6">
        <div className="flex items-center justify-between mb-5">
          <h2 className="text-lg font-semibold text-foreground">Create New Space</h2>
          <button onClick={onClose} className="text-muted-foreground hover:text-foreground">
            <X size={18} />
          </button>
        </div>
        <div className="mb-5">
          <label className="block text-sm font-medium text-foreground mb-1.5">Space Name</label>
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="e.g. Main Warehouse, Office Supplies..."
            className="input-field"
            autoFocus
          />
          <p className="text-xs text-muted-foreground mt-1.5">You can create up to 10 spaces.</p>
        </div>
        <div className="flex gap-3 justify-end">
          <button onClick={onClose} className="btn-secondary">Cancel</button>
          <button
            onClick={() => name.trim() && onSubmit(name.trim())}
            disabled={!name.trim()}
            className="btn-primary"
          >
            Create Space
          </button>
        </div>
      </div>
    </div>
  );
}

function EditSpaceModal({
  space,
  onClose,
  onSubmit,
}: {
  space: Space;
  onClose: () => void;
  onSubmit: (id: string, name: string) => void;
}) {
  const [name, setName] = useState(space.name);
  return (
    <div className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4">
      <div className="bg-card rounded-xl border border-border shadow-xl w-full max-w-md p-6">
        <div className="flex items-center justify-between mb-5">
          <h2 className="text-lg font-semibold text-foreground">Rename Space</h2>
          <button onClick={onClose} className="text-muted-foreground hover:text-foreground">
            <X size={18} />
          </button>
        </div>
        <div className="mb-5">
          <label className="block text-sm font-medium text-foreground mb-1.5">Space Name</label>
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="input-field"
            autoFocus
          />
        </div>
        <div className="flex gap-3 justify-end">
          <button onClick={onClose} className="btn-secondary">Cancel</button>
          <button
            onClick={() => name.trim() && onSubmit(space.id, name.trim())}
            disabled={!name.trim()}
            className="btn-primary"
          >
            Save Changes
          </button>
        </div>
      </div>
    </div>
  );
}

export default function SpacesPageContent() {
  const [ownedSpaces, setOwnedSpaces] = useState<Space[]>([]);
  const [sharedSpaces, setSharedSpaces] = useState<Space[]>([]);
  const [invites, setInvites] = useState<PendingInvite[]>([]);
  const [maxSpaces, setMaxSpaces] = useState(10);
  const [createOpen, setCreateOpen] = useState(false);
  const [editSpace, setEditSpace] = useState<Space | null>(null);
  const [deleteConfirm, setDeleteConfirm] = useState<string | null>(null);

  const totalSpaces = ownedSpaces.length + sharedSpaces.length;

  const toSpace = (space: Omit<Space, 'createdAt'>): Space => ({
    ...space,
    currentUserRole: space.currentUserRole || 'OWNER',
    createdAt: '',
  });

  const loadSpaces = useCallback(async () => {
    try {
      const [owned, shared, status, pendingInvites] = await Promise.all([
        apiFetch<Omit<Space, 'createdAt'>[]>('/api/spaces/owned'),
        apiFetch<Omit<Space, 'createdAt'>[]>('/api/spaces/shared'),
        apiFetch<{ maxSpaces: number }>('/api/spaces/creation-status'),
        apiFetch<PendingInvite[]>('/api/spaces/invites'),
      ]);
      setOwnedSpaces(owned.map(toSpace));
      setSharedSpaces(shared.map(toSpace));
      setMaxSpaces(status.maxSpaces);
      setInvites(pendingInvites);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Unable to load spaces');
    }
  }, []);

  useEffect(() => { void loadSpaces(); }, [loadSpaces]);

  const handleCreate = async (name: string) => {
    try {
      await apiFetch('/api/spaces', { method: 'POST', body: JSON.stringify({ name }) });
      setCreateOpen(false);
      await loadSpaces();
      toast.success('Space created');
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Unable to create space');
    }
  };

  const handleEdit = async (id: string, name: string) => {
    try {
      await apiFetch(`/api/spaces/${id}`, { method: 'PUT', body: JSON.stringify({ name }) });
      setEditSpace(null);
      await loadSpaces();
      toast.success('Space renamed');
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Unable to rename space');
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await apiFetch(`/api/spaces/${id}`, { method: 'DELETE' });
      setDeleteConfirm(null);
      await loadSpaces();
      toast.success('Space deleted');
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Unable to delete space');
    }
  };

  const handleAcceptInvite = async (spaceId: string) => {
    await apiFetch(`/api/spaces/${spaceId}/members/accept`, { method: 'POST' });
    setInvites((prev) => prev.filter((i) => i.spaceId !== spaceId));
    await loadSpaces();
  };

  const handleDeclineInvite = async (spaceId: string) => {
    await apiFetch(`/api/spaces/${spaceId}/members/decline`, { method: 'POST' });
    setInvites((prev) => prev.filter((i) => i.spaceId !== spaceId));
  };

  return (
    <div className="min-h-screen bg-background">
      {/* Top Nav */}
      <header className="h-[68px] bg-card border-b border-border flex items-center justify-between px-6 sticky top-0 z-20">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 bg-primary rounded-lg flex items-center justify-center">
            <AppLogo size={20} />
          </div>
          <span className="font-bold text-foreground text-lg">StockRoom</span>
        </div>
        <div className="flex items-center gap-2">
          <button className="relative w-9 h-9 flex items-center justify-center rounded-lg hover:bg-secondary transition-colors">
            <Bell size={18} className="text-muted-foreground" />
            {invites.length > 0 && (
              <span className="absolute top-1.5 right-1.5 bg-danger rounded-full w-2 h-2" />
            )}
          </button>
          <button className="w-9 h-9 flex items-center justify-center rounded-lg hover:bg-secondary transition-colors">
            <Settings size={18} className="text-muted-foreground" />
          </button>
          <div className="w-px h-6 bg-border mx-1" />
          <button className="flex items-center gap-2.5 hover:bg-secondary rounded-lg px-2 py-1.5 transition-colors">
            <div className="text-right">
              <div className="text-sm font-semibold text-foreground leading-tight">Alex Rivera</div>
              <div className="text-xs text-muted-foreground leading-tight">ADMIN</div>
            </div>
            <div className="w-8 h-8 rounded-full overflow-hidden border-2 border-border">
              <AppImage
                src="https://i.pravatar.cc/40?img=12"
                alt="Alex Rivera profile photo"
                width={32}
                height={32}
                className="w-full h-full object-cover"
              />
            </div>
          </button>
        </div>
      </header>

      <div className="max-w-6xl mx-auto px-6 py-8">
        {/* Page Header */}
        <div className="flex items-start justify-between mb-8">
          <div>
            <h1 className="text-2xl font-bold text-foreground">My Spaces</h1>
            <p className="text-sm text-muted-foreground mt-1">
              {totalSpaces} of {maxSpaces} spaces used · Manage your inventory collections
            </p>
          </div>
          <button
            onClick={() => setCreateOpen(true)}
            disabled={ownedSpaces.length >= maxSpaces}
            className="btn-primary"
          >
            <Plus size={16} /> New Space
          </button>
        </div>

        {/* Space usage bar */}
        <div className="bg-card border border-border rounded-xl p-4 mb-8 flex items-center gap-4">
          <div className="flex-1">
            <div className="flex items-center justify-between mb-1.5">
              <span className="text-xs font-medium text-muted-foreground">Space Usage</span>
              <span className="text-xs font-semibold text-foreground">{totalSpaces}/{maxSpaces}</span>
            </div>
            <div className="h-2 bg-secondary rounded-full overflow-hidden">
              <div
                className="h-full bg-primary rounded-full transition-all"
                style={{ width: `${(totalSpaces / maxSpaces) * 100}%` }}
              />
            </div>
          </div>
          <div className="text-right">
            <div className="text-xs text-muted-foreground">{maxSpaces - totalSpaces} slots remaining</div>
          </div>
        </div>

        {/* Pending Invites */}
        {invites.length > 0 && (
          <div className="mb-8">
            <h2 className="text-sm font-semibold text-foreground mb-3 flex items-center gap-2">
              <AlertTriangle size={14} className="text-warning" />
              Pending Invitations ({invites.length})
            </h2>
            <div className="space-y-2">
              {invites.map((invite) => (
                <div
                  key={invite.spaceId}
                  className="bg-amber-50 border border-amber-200 rounded-xl p-4 flex items-center justify-between"
                >
                  <div>
                    <p className="text-sm font-semibold text-foreground">{invite.spaceName}</p>
                    <p className="text-xs text-muted-foreground mt-0.5">
                      Invited by {invite.spaceOwnerName} · Role: <span className="font-medium">{invite.role}</span>
                    </p>
                  </div>
                  <div className="flex gap-2">
                    <button
                      onClick={() => handleDeclineInvite(invite.spaceId)}
                      className="btn-secondary text-xs py-1.5 px-3"
                    >
                      Decline
                    </button>
                    <button
                      onClick={() => handleAcceptInvite(invite.spaceId)}
                      className="btn-primary text-xs py-1.5 px-3"
                    >
                      <Check size={12} /> Accept
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Owned Spaces */}
        <div className="mb-8">
          <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-4">
            Owned by You ({ownedSpaces.length})
          </h2>
          {ownedSpaces.length === 0 ? (
            <div className="bg-card border border-dashed border-border rounded-xl p-10 text-center">
              <Boxes size={32} className="text-muted-foreground mx-auto mb-3" />
              <p className="text-sm font-medium text-foreground">No spaces yet</p>
              <p className="text-xs text-muted-foreground mt-1">Create your first inventory space to get started.</p>
              <button onClick={() => setCreateOpen(true)} className="btn-primary mt-4 text-sm">
                <Plus size={14} /> Create Space
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {ownedSpaces.map((space) => (
                <SpaceCard
                  key={space.id}
                  space={space}
                  onEdit={(s) => setEditSpace(s)}
                  onDelete={(id) => setDeleteConfirm(id)}
                />
              ))}
            </div>
          )}
        </div>

        {/* Shared Spaces */}
        {sharedSpaces.length > 0 && (
          <div>
            <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-4">
              Shared with You ({sharedSpaces.length})
            </h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {sharedSpaces.map((space) => (
                <SpaceCard
                  key={space.id}
                  space={space}
                  onEdit={(s) => setEditSpace(s)}
                  onDelete={(id) => setDeleteConfirm(id)}
                />
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Modals */}
      {createOpen && (
        <CreateSpaceModal onClose={() => setCreateOpen(false)} onSubmit={handleCreate} />
      )}
      {editSpace && (
        <EditSpaceModal
          space={editSpace}
          onClose={() => setEditSpace(null)}
          onSubmit={handleEdit}
        />
      )}
      {deleteConfirm && (
        <div className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4">
          <div className="bg-card rounded-xl border border-border shadow-xl w-full max-w-sm p-6">
            <h2 className="text-base font-semibold text-foreground mb-2">Delete Space?</h2>
            <p className="text-sm text-muted-foreground mb-5">
              This will permanently delete the space and all its products. This action cannot be undone.
            </p>
            <div className="flex gap-3 justify-end">
              <button onClick={() => setDeleteConfirm(null)} className="btn-secondary">Cancel</button>
              <button
                onClick={() => handleDelete(deleteConfirm)}
                className="btn-primary bg-danger hover:bg-red-600"
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
