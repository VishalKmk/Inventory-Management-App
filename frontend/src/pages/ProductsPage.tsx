import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Plus, Package, Search, Edit, Trash2, AlertTriangle, PlusCircle, MinusCircle, ArrowLeft } from 'lucide-react';
import Layout from '../components/layout/Layout';
import Card from '../components/ui/Card';
import Button from '../components/ui/Button';
import Modal from '../components/ui/Modal';
import Input from '../components/ui/Input';
import { useApi } from '../hooks/useApi';

interface Product {
  id: string;
  spaceId: string;
  spaceName?: string;
  name: string;
  price: number;
  currentStock: number;
  minimumQuantity: number;
  maximumQuantity: number;
  isLowStock?: boolean;
}

interface Space {
  id: string;
  name: string;
}

const ProductsPage: React.FC = () => {
  const { spaceId } = useParams<{ spaceId: string }>();
  const navigate = useNavigate();
  const { get, post, put, delete: deleteProduct } = useApi();

  const [space, setSpace] = useState<Space | null>(null);
  const [products, setProducts] = useState<Product[]>([]);
  const [filteredProducts, setFilteredProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  
  // Modal states
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [stockModalOpen, setStockModalOpen] = useState(false);
  
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);
  const [stockOperation, setStockOperation] = useState<'add' | 'remove'>('add');
  const [submitting, setSubmitting] = useState(false);

  // Form data
  const [productForm, setProductForm] = useState({
    name: '',
    price: '',
    currentStock: '',
    minimumQuantity: '',
    maximumQuantity: '',
  });
  const [stockQuantity, setStockQuantity] = useState('');

  useEffect(() => {
    if (spaceId) {
      fetchSpaceAndProducts();
    }
  }, [spaceId]);

  useEffect(() => {
    const filtered = products.filter(product =>
      product.name.toLowerCase().includes(searchQuery.toLowerCase())
    );
    setFilteredProducts(filtered);
  }, [products, searchQuery]);

  const fetchSpaceAndProducts = async () => {
    try {
      const [spaceResponse, productsResponse] = await Promise.all([
        get(`/spaces/${spaceId}`),
        get(`/spaces/${spaceId}/products`)
      ]);

      if (spaceResponse.success) {
        setSpace(spaceResponse.data);
      }

      if (productsResponse.success) {
        setProducts(productsResponse.data || []);
      }
    } catch (error) {
      console.error('Error fetching data:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateProduct = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);

    try {
      const response = await post(`/spaces/${spaceId}/products`, {
        name: productForm.name,
        price: parseFloat(productForm.price),
        currentStock: parseInt(productForm.currentStock),
        minimumQuantity: parseInt(productForm.minimumQuantity),
        maximumQuantity: parseInt(productForm.maximumQuantity),
      });

      if (response.success) {
        setCreateModalOpen(false);
        resetForm();
        fetchSpaceAndProducts();
      }
    } catch (error) {
      console.error('Error creating product:', error);
    } finally {
      setSubmitting(false);
    }
  };

  const handleEditProduct = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedProduct) return;

    setSubmitting(true);

    try {
      const response = await put(`/spaces/${spaceId}/products/${selectedProduct.id}`, {
        name: productForm.name,
        price: parseFloat(productForm.price),
        minimumQuantity: parseInt(productForm.minimumQuantity),
        maximumQuantity: parseInt(productForm.maximumQuantity),
      });

      if (response.success) {
        setEditModalOpen(false);
        setSelectedProduct(null);
        resetForm();
        fetchSpaceAndProducts();
      }
    } catch (error) {
      console.error('Error updating product:', error);
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteProduct = async () => {
    if (!selectedProduct) return;

    setSubmitting(true);

    try {
      const response = await deleteProduct(`/spaces/${spaceId}/products/${selectedProduct.id}`);
      if (response.success) {
        setDeleteModalOpen(false);
        setSelectedProduct(null);
        fetchSpaceAndProducts();
      }
    } catch (error) {
      console.error('Error deleting product:', error);
    } finally {
      setSubmitting(false);
    }
  };

  const handleStockAdjustment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedProduct || !stockQuantity) return;

    setSubmitting(true);

    try {
      const endpoint = stockOperation === 'add' ? 'add' : 'remove';
      const response = await post(`/spaces/${spaceId}/products/${selectedProduct.id}/stock/${endpoint}`, {
        quantity: parseInt(stockQuantity)
      });

      if (response.success) {
        setStockModalOpen(false);
        setSelectedProduct(null);
        setStockQuantity('');
        fetchSpaceAndProducts();
      }
    } catch (error) {
      console.error('Error adjusting stock:', error);
    } finally {
      setSubmitting(false);
    }
  };

  const resetForm = () => {
    setProductForm({
      name: '',
      price: '',
      currentStock: '',
      minimumQuantity: '',
      maximumQuantity: '',
    });
  };

  const openEditModal = (product: Product) => {
    setSelectedProduct(product);
    setProductForm({
      name: product.name,
      price: product.price.toString(),
      currentStock: product.currentStock.toString(),
      minimumQuantity: product.minimumQuantity.toString(),
      maximumQuantity: product.maximumQuantity.toString(),
    });
    setEditModalOpen(true);
  };

  const openStockModal = (product: Product, operation: 'add' | 'remove') => {
    setSelectedProduct(product);
    setStockOperation(operation);
    setStockModalOpen(true);
  };

  const ProductCard: React.FC<{ product: Product }> = ({ product }) => (
    <Card className="group hover:shadow-lg transition-all duration-200">
      <div className="flex items-start justify-between mb-4">
        <div className="flex items-center gap-3">
          <div className={`p-3 rounded-xl ${
            product.isLowStock 
              ? 'bg-red-100 dark:bg-red-900/30' 
              : 'bg-green-100 dark:bg-green-900/30'
          }`}>
            <Package size={20} className={
              product.isLowStock 
                ? 'text-red-600 dark:text-red-400' 
                : 'text-green-600 dark:text-green-400'
            } />
          </div>
          <div>
            <h3 className="font-semibold text-gray-900 dark:text-white">{product.name}</h3>
            <p className="text-lg font-bold text-indigo-600 dark:text-indigo-400">
              ${product.price.toFixed(2)}
            </p>
          </div>
        </div>
        
        <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
          <button
            onClick={() => openEditModal(product)}
            className="p-1.5 hover:bg-gray-100 dark:hover:bg-slate-700 rounded-lg transition-colors"
          >
            <Edit size={14} className="text-gray-500 dark:text-slate-400" />
          </button>
          <button
            onClick={() => { setSelectedProduct(product); setDeleteModalOpen(true); }}
            className="p-1.5 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg transition-colors"
          >
            <Trash2 size={14} className="text-red-500" />
          </button>
        </div>
      </div>

      <div className="space-y-3 mb-4">
        <div className="flex justify-between items-center">
          <span className="text-sm text-gray-600 dark:text-slate-400">Stock</span>
          <div className="flex items-center gap-2">
            {product.isLowStock && (
              <AlertTriangle size={14} className="text-red-500" />
            )}
            <span className={`font-medium ${
              product.isLowStock 
                ? 'text-red-600 dark:text-red-400' 
                : 'text-gray-900 dark:text-white'
            }`}>
              {product.currentStock}
            </span>
          </div>
        </div>
        
        <div className="flex justify-between items-center text-sm">
          <span className="text-gray-600 dark:text-slate-400">Min: {product.minimumQuantity}</span>
          <span className="text-gray-600 dark:text-slate-400">Max: {product.maximumQuantity}</span>
        </div>
      </div>

      <div className="flex gap-2">
        <Button
          size="sm"
          variant="outline"
          className="flex-1"
          leftIcon={<PlusCircle size={14} />}
          onClick={() => openStockModal(product, 'add')}
        >
          Add
        </Button>
        <Button
          size="sm"
          variant="outline"
          className="flex-1"
          leftIcon={<MinusCircle size={14} />}
          onClick={() => openStockModal(product, 'remove')}
        >
          Remove
        </Button>
      </div>
    </Card>
  );

  return (
    <Layout 
      title={space?.name || 'Products'} 
      subtitle="Manage products in this space"
      action={
        <div className="flex items-center gap-3">
          <Button
            variant="outline"
            leftIcon={<ArrowLeft size={18} />}
            onClick={() => navigate('/spaces')}
          >
            Back to Spaces
          </Button>
          <Button 
            leftIcon={<Plus size={18} />}
            onClick={() => setCreateModalOpen(true)}
          >
            Add Product
          </Button>
        </div>
      }
    >
      <div className="space-y-6">
        {/* Search */}
        <div className="flex gap-4">
          <div className="flex-1">
            <Input
              placeholder="Search products..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              leftIcon={<Search size={18} />}
            />
          </div>
        </div>

        {/* Products Grid */}
        {loading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {[...Array(8)].map((_, i) => (
              <div key={i} className="animate-pulse">
                <div className="bg-gray-200 dark:bg-slate-700 rounded-2xl h-48"></div>
              </div>
            ))}
          </div>
        ) : filteredProducts.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {filteredProducts.map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>
        ) : (
          <div className="text-center py-16">
            <Package size={64} className="mx-auto text-gray-300 dark:text-slate-600 mb-4" />
            <h3 className="text-xl font-semibold text-gray-900 dark:text-white mb-2">
              {searchQuery ? 'No products found' : 'No products yet'}
            </h3>
            <p className="text-gray-500 dark:text-slate-400 mb-6">
              {searchQuery ? 'Try adjusting your search terms' : 'Add your first product to start tracking inventory'}
            </p>
            {!searchQuery && (
              <Button leftIcon={<Plus size={18} />} onClick={() => setCreateModalOpen(true)}>
                Add Your First Product
              </Button>
            )}
          </div>
        )}
      </div>

      {/* Create Product Modal */}
      <Modal
        isOpen={createModalOpen}
        onClose={() => setCreateModalOpen(false)}
        title="Add New Product"
        size="lg"
      >
        <form onSubmit={handleCreateProduct} className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input
              label="Product Name"
              placeholder="Enter product name"
              value={productForm.name}
              onChange={(e) => setProductForm({ ...productForm, name: e.target.value })}
              required
            />
            
            <Input
              type="number"
              step="0.01"
              label="Price"
              placeholder="0.00"
              value={productForm.price}
              onChange={(e) => setProductForm({ ...productForm, price: e.target.value })}
              required
            />
            
            <Input
              type="number"
              label="Current Stock"
              placeholder="0"
              value={productForm.currentStock}
              onChange={(e) => setProductForm({ ...productForm, currentStock: e.target.value })}
              required
            />
            
            <Input
              type="number"
              label="Minimum Quantity"
              placeholder="0"
              value={productForm.minimumQuantity}
              onChange={(e) => setProductForm({ ...productForm, minimumQuantity: e.target.value })}
              required
            />
          </div>
          
          <Input
            type="number"
            label="Maximum Quantity"
            placeholder="0"
            value={productForm.maximumQuantity}
            onChange={(e) => setProductForm({ ...productForm, maximumQuantity: e.target.value })}
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
              Add Product
            </Button>
          </div>
        </form>
      </Modal>

      {/* Edit Product Modal */}
      <Modal
        isOpen={editModalOpen}
        onClose={() => setEditModalOpen(false)}
        title="Edit Product"
        size="lg"
      >
        <form onSubmit={handleEditProduct} className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input
              label="Product Name"
              placeholder="Enter product name"
              value={productForm.name}
              onChange={(e) => setProductForm({ ...productForm, name: e.target.value })}
              required
            />
            
            <Input
              type="number"
              step="0.01"
              label="Price"
              placeholder="0.00"
              value={productForm.price}
              onChange={(e) => setProductForm({ ...productForm, price: e.target.value })}
              required
            />
            
            <Input
              type="number"
              label="Minimum Quantity"
              placeholder="0"
              value={productForm.minimumQuantity}
              onChange={(e) => setProductForm({ ...productForm, minimumQuantity: e.target.value })}
              required
            />
            
            <Input
              type="number"
              label="Maximum Quantity"
              placeholder="0"
              value={productForm.maximumQuantity}
              onChange={(e) => setProductForm({ ...productForm, maximumQuantity: e.target.value })}
              required
            />
          </div>
          
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
              Update Product
            </Button>
          </div>
        </form>
      </Modal>

      {/* Stock Adjustment Modal */}
      <Modal
        isOpen={stockModalOpen}
        onClose={() => setStockModalOpen(false)}
        title={`${stockOperation === 'add' ? 'Add' : 'Remove'} Stock`}
      >
        <form onSubmit={handleStockAdjustment} className="space-y-6">
          {selectedProduct && (
            <div className="bg-gray-50 dark:bg-slate-700 rounded-lg p-4">
              <h4 className="font-medium text-gray-900 dark:text-white mb-1">{selectedProduct.name}</h4>
              <p className="text-sm text-gray-600 dark:text-slate-400">
                Current stock: {selectedProduct.currentStock}
              </p>
            </div>
          )}
          
          <Input
            type="number"
            label="Quantity"
            placeholder="Enter quantity"
            value={stockQuantity}
            onChange={(e) => setStockQuantity(e.target.value)}
            required
            min="1"
          />
          
          <div className="flex gap-3 pt-4">
            <Button
              type="button"
              variant="outline"
              className="flex-1"
              onClick={() => setStockModalOpen(false)}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              className="flex-1"
              loading={submitting}
              variant={stockOperation === 'remove' ? 'danger' : 'primary'}
            >
              {stockOperation === 'add' ? 'Add Stock' : 'Remove Stock'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete Confirmation Modal */}
      <Modal
        isOpen={deleteModalOpen}
        onClose={() => setDeleteModalOpen(false)}
        title="Delete Product"
      >
        <div className="space-y-6">
          <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4">
            <p className="text-red-800 dark:text-red-400">
              Are you sure you want to delete "<strong>{selectedProduct?.name}</strong>"? 
              This action cannot be undone.
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
              onClick={handleDeleteProduct}
            >
              Delete Product
            </Button>
          </div>
        </div>
      </Modal>
    </Layout>
  );
};

export default ProductsPage;