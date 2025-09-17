import React, { useState, useEffect } from 'react';
import { Plus, Warehouse, Package, Settings, Trash2, Eye } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import Layout from '../components/layout/Layout';
import Card from '../components/ui/Card';
import Button from '../components/ui/Button';
import Modal from '../components/ui/Modal';
import Input from '../components/ui/Input';
import { useApi } from '../hooks/useApi';

interface Space {
  id: string;
  name: string;
  ownerId: string;
  ownerName: string;
  productCount: number;
  createdAt?: string;
}

const SpacesPage: React.FC = () => {
  const navigate = useNavigate();
  const { get, post, put, delete: deleteSpace } = useApi();
  const [spaces, setSpaces] = useState<Space[]>([]);
  const [loading, setLoading] = useState(true);
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [selectedSpace, setSelectedSpace] = useState<Space | null>(null);
  const [spaceName, setSpaceName] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    fetchSpaces();
  }, []);

  const fetchSpaces = async () => {
    try {
      const response = await get('/spaces');
      if (response.success) {
        setSpaces(response.data || []);
      }
    } catch (error) {
      console.error('Error fetching spaces:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateSpace = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);

    try {
      const response = await post('/spaces', { name: spaceName });
      if (response.success) {
        setCreateModalOpen(false);
        setSpaceName('');
        fetchSpaces();
      }
    } catch (error) {
      console.error('Error creating space:', error);
    } finally {
      setSubmitting(false);
    }
  };

  const handleEditSpace = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedSpace) return;

    setSubmitting(true);

    try {
      const response = await put(`/spaces/${selectedSpace.id}`, { name: spaceName });
      if (response.success) {
        setEditModalOpen(false);
        setSelectedSpace(null);
        setSpaceName('');
        fetchSpaces();
      }
    } catch (error) {
      console.error('Error updating space:', error);
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteSpace = async () => {
    if (!selectedSpace) return;

    setSubmitting(true);

    try {
      const response = await deleteSpace(`/spaces/${selectedSpace.id}`);
      if (response.success) {
        setDeleteModalOpen(false);
        setSelectedSpace(null);
        fetchSpaces();
      }
    } catch (error) {
      console.error('Error deleting space:', error);
    } finally {
      setSubmitting(false);
    }
  };

  const openEditModal = (space: Space) => {
    setSelectedSpace(space);
    setSpaceName(space.name);
    setEditModalOpen(true);
  };

  const openDeleteModal = (space: Space) => {
    setSelectedSpace(space);
    setDeleteModalOpen(true);
  };

  const SpaceCard: React.FC<{ space: Space }> = ({ space }) => (
    <Card className="group hover:shadow-lg transition-all duration-200">
      <div className="flex items-start justify-between mb-4">
        <div className="flex items-center gap-3">
          <div className="p-3 bg-indigo-100 dark:bg-indigo-900/30 rounded-xl">
            <Warehouse size={24} className="text-indigo-600 dark:text-indigo-400" />
          </div>
          <div>
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white">{space.name}</h3>
            <p className="text-sm text-gray-500 dark:text-slate-400">{space.ownerName}</p>
          </div>
        </div>
        
        <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
          <button
            onClick={() => openEditModal(space)}
            className="p-2 hover:bg-gray-100 dark:hover:bg-slate-700 rounded-lg transition-colors"
          >
            <Settings size={16} className="text-gray-500 dark:text-slate-400" />
          </button>
          <button
            onClick={() => openDeleteModal(space)}
            className="p-2 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg transition-colors"
          >
            <Trash2 size={16} className="text-red-500" />
          </button>
        </div>
      </div>

      <div className="flex items-center gap-4 mb-6">
        <div className="flex items-center gap-2 text-gray-600 dark:text-slate-400">
          <Package size={16} />
          <span className="text-sm">Products</span>
          <span className="px-2 py-1 bg-indigo-100 dark:bg-indigo-900/30 text-indigo-600 dark:text-indigo-400 rounded-full text-xs font-medium">
            {space.productCount || 0}
          </span>
        </div>
      </div>

      {space.createdAt && (
        <p className="text-xs text-gray-500 dark:text-slate-400 mb-4">
          Created {new Date(space.createdAt).toLocaleDateString()}
        </p>
      )}

      <Button
        variant="outline"
        className="w-full"
        leftIcon={<Eye size={16} />}
        onClick={() => navigate(`/spaces/${space.id}/products`)}
      >
        View Products
      </Button>
    </Card>
  );

  return (
    <Layout 
      title="Spaces" 
      subtitle="Manage your inventory across different spaces and locations"
      action={
        <Button leftIcon={<Plus size={18} />} onClick={() => setCreateModalOpen(true)}>
          Create Space
        </Button>
      }
    >
      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {[...Array(6)].map((_, i) => (
            <div key={i} className="animate-pulse">
              <div className="bg-gray-200 dark:bg-slate-700 rounded-2xl h-48"></div>
            </div>
          ))}
        </div>
      ) : spaces.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {spaces.map((space) => (
            <SpaceCard key={space.id} space={space} />
          ))}
        </div>
      ) : (
        <div className="text-center py-16">
          <Warehouse size={64} className="mx-auto text-gray-300 dark:text-slate-600 mb-4" />
          <h3 className="text-xl font-semibold text-gray-900 dark:text-white mb-2">No spaces yet</h3>
          <p className="text-gray-500 dark:text-slate-400 mb-6">
            Create your first space to start managing your inventory
          </p>
          <Button leftIcon={<Plus size={18} />} onClick={() => setCreateModalOpen(true)}>
            Create Your First Space
          </Button>
        </div>
      )}

      {/* Create Space Modal */}
      <Modal
        isOpen={createModalOpen}
        onClose={() => setCreateModalOpen(false)}
        title="Create New Space"
      >
        <form onSubmit={handleCreateSpace} className="space-y-6">
          <Input
            label="Space Name"
            placeholder="Enter space name (e.g., Warehouse A, Store Front)"
            value={spaceName}
            onChange={(e) => setSpaceName(e.target.value)}
            required
          />
          
          <div className="flex gap-3 pt-4">
            <Button
              type="button"
              variant="outline"
              className="flex-1"
              onClick={() => setCreateModalOpen(false)}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              className="flex-1"
              loading={submitting}
            >
              Create Space
            </Button>
          </div>
        </form>
      </Modal>

      {/* Edit Space Modal */}
      <Modal
        isOpen={editModalOpen}
        onClose={() => setEditModalOpen(false)}
        title="Edit Space"
      >
        <form onSubmit={handleEditSpace} className="space-y-6">
          <Input
            label="Space Name"
            placeholder="Enter space name"
            value={spaceName}
            onChange={(e) => setSpaceName(e.target.value)}
            required
          />
          
          <div className="flex gap-3 pt-4">
            <Button
              type="button"
              variant="outline"
              className="flex-1"
              onClick={() => setEditModalOpen(false)}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              className="flex-1"
              loading={submitting}
            >
              Update Space
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete Confirmation Modal */}
      <Modal
        isOpen={deleteModalOpen}
        onClose={() => setDeleteModalOpen(false)}
        title="Delete Space"
      >
        <div className="space-y-6">
          <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4">
            <p className="text-red-800 dark:text-red-400">
              Are you sure you want to delete "<strong>{selectedSpace?.name}</strong>"? 
              This action cannot be undone and will also delete all products in this space.
            </p>
          </div>
          
          <div className="flex gap-3">
            <Button
              type="button"
              variant="outline"
              className="flex-1"
              onClick={() => setDeleteModalOpen(false)}
            >
              Cancel
            </Button>
            <Button
              variant="danger"
              className="flex-1"
              loading={submitting}
              onClick={handleDeleteSpace}
            >
              Delete Space
            </Button>
          </div>
        </div>
      </Modal>
    </Layout>
  );
};

export default SpacesPage;