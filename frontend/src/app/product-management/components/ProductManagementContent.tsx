'use client';
import React, { useState, useCallback, useEffect } from 'react';
import ProductToolbar from './ProductToolbar';
import ProductTable from './ProductTable';
import ProductSummaryCards from './ProductSummaryCards';
import AddProductModal from './AddProductModal';
import { apiFetch } from '@/lib/api';
import { toast } from 'sonner';

export interface Product {
  id: string;
  spaceId: string;
  name: string;
  sku: string;
  category: string;
  price: number;
  currentStock: number;
  minimumQuantity: number;
  maximumQuantity: number;
  imageUrl: string;
  imageAlt: string;
}

const INITIAL_PRODUCTS: Product[] = [
{
  id: 'prod-stl-001',
  spaceId: 'space-wh001',
  name: 'High-Tensile Steel Bolts',
  sku: 'STL-B-990',
  category: 'Fasteners',
  price: 24.5,
  currentStock: 1450,
  minimumQuantity: 500,
  maximumQuantity: 2000,
  imageUrl: "https://images.unsplash.com/photo-1482263042246-1be5507a93ec",
  imageAlt: 'Box of high-tensile steel bolts on metal surface'
},
{
  id: 'prod-mot-002',
  spaceId: 'space-wh001',
  name: 'Industrial Grade Servo Motor',
  sku: 'MOT-SRV-22',
  category: 'Motors & Drives',
  price: 580.0,
  currentStock: 12,
  minimumQuantity: 50,
  maximumQuantity: 150,
  imageUrl: "https://img.rocket.new/generatedImages/rocket_gen_img_170dbd191-1773222617357.png",
  imageAlt: 'Industrial servo motor with silver casing on workbench'
},
{
  id: 'prod-pal-003',
  spaceId: 'space-wh001',
  name: 'Reinforced Cargo Pallets',
  sku: 'LOG-PAL-X5',
  category: 'Logistics',
  price: 42.0,
  currentStock: 420,
  minimumQuantity: 100,
  maximumQuantity: 500,
  imageUrl: "https://img.rocket.new/generatedImages/rocket_gen_img_1435e7315-1767712166219.png",
  imageAlt: 'Stack of wooden cargo pallets in warehouse aisle'
},
{
  id: 'prod-iot-004',
  spaceId: 'space-wh001',
  name: 'IoT Inventory Tracker Nodes',
  sku: 'IOT-NOD-A1',
  category: 'Electronics',
  price: 125.75,
  currentStock: 3,
  minimumQuantity: 25,
  maximumQuantity: 100,
  imageUrl: "https://img.rocket.new/generatedImages/rocket_gen_img_1c9338d72-1773209940839.png",
  imageAlt: 'Small IoT sensor node with antenna on circuit board'
},
{
  id: 'prod-drill-005',
  spaceId: 'space-wh001',
  name: 'Industrial Power Drill v2',
  sku: 'TL-DR-8821',
  category: 'Tools & Hardware',
  price: 189.99,
  currentStock: 12,
  minimumQuantity: 50,
  maximumQuantity: 200,
  imageUrl: "https://img.rocket.new/generatedImages/rocket_gen_img_1f219d492-1768358953473.png",
  imageAlt: 'Industrial power drill with black and orange body'
},
{
  id: 'prod-helm-006',
  spaceId: 'space-wh001',
  name: 'Pro-Shield Safety Helmet',
  sku: 'SF-HM-0019',
  category: 'Safety Equipment',
  price: 34.0,
  currentStock: 85,
  minimumQuantity: 100,
  maximumQuantity: 400,
  imageUrl: "https://images.unsplash.com/photo-1617118600471-ecc90bdb4db0",
  imageAlt: 'Yellow hard hat safety helmet on gray surface'
},
{
  id: 'prod-cop-007',
  spaceId: 'space-wh001',
  name: 'Precision Copper Spool 50m',
  sku: 'EL-CW-1200',
  category: 'Electrical',
  price: 67.5,
  currentStock: 4,
  minimumQuantity: 20,
  maximumQuantity: 80,
  imageUrl: "https://img.rocket.new/generatedImages/rocket_gen_img_1e08e6098-1772664447543.png",
  imageAlt: 'Orange copper wire spool on warehouse shelf'
},
{
  id: 'prod-hyd-008',
  spaceId: 'space-wh001',
  name: 'Hydraulic Seal Kit 3/4"',
  sku: 'HY-SK-0440',
  category: 'Hydraulics',
  price: 52.25,
  currentStock: 18,
  minimumQuantity: 40,
  maximumQuantity: 120,
  imageUrl: "https://img.rocket.new/generatedImages/rocket_gen_img_1087ba555-1772567366824.png",
  imageAlt: 'Hydraulic seal kit with rubber rings and gaskets'
},
{
  id: 'prod-glv-009',
  spaceId: 'space-wh001',
  name: 'Cut-Resistant Work Gloves L',
  sku: 'SF-GL-0077',
  category: 'Safety Equipment',
  price: 18.9,
  currentStock: 310,
  minimumQuantity: 80,
  maximumQuantity: 600,
  imageUrl: "https://img.rocket.new/generatedImages/rocket_gen_img_1dfb9b64f-1772166273831.png",
  imageAlt: 'Pair of cut-resistant gray work gloves on white background'
},
{
  id: 'prod-blt-010',
  spaceId: 'space-wh001',
  name: 'Conveyor Drive Belt 2m',
  sku: 'MEC-BLT-220',
  category: 'Mechanical',
  price: 143.0,
  currentStock: 27,
  minimumQuantity: 15,
  maximumQuantity: 60,
  imageUrl: "https://img.rocket.new/generatedImages/rocket_gen_img_164f44f91-1767453116350.png",
  imageAlt: 'Black rubber conveyor drive belt coiled on floor'
}];


export default function ProductManagementContent() {
  const [products, setProducts] = useState<Product[]>([]);
  const [spaceIds, setSpaceIds] = useState<string[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [lowStockOnly, setLowStockOnly] = useState(false);
  const [sortBy, setSortBy] = useState<'name' | 'price' | 'stock'>('name');
  const [addModalOpen, setAddModalOpen] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const PAGE_SIZE = 8;

  const loadProducts = useCallback(async () => {
    try {
      const [owned, shared] = await Promise.all([
        apiFetch<Array<{ id: string }>>('/api/spaces/owned'),
        apiFetch<Array<{ id: string }>>('/api/spaces/shared'),
      ]);
      const ids = [...owned, ...shared].map((space) => space.id);
      setSpaceIds(ids);
      const pages = await Promise.all(ids.map((spaceId) =>
        apiFetch<{ data: Array<Omit<Product, 'imageAlt'> & { imageUrl?: string }> }>(`/api/spaces/${spaceId}/products?page=0&size=100`)
      ));
      setProducts(pages.flatMap((page) => page.data).map((product) => ({
        ...product,
        imageUrl: product.imageUrl || '/assets/images/no_image.png',
        imageAlt: product.name,
      })));
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Unable to load products');
    }
  }, []);

  useEffect(() => { void loadProducts(); }, [loadProducts]);

  const filteredProducts = products.
  filter((p) => {
    const q = searchQuery.toLowerCase();
    const matchesSearch =
    !q ||
    p.name.toLowerCase().includes(q) ||
    p.sku.toLowerCase().includes(q) ||
    p.category.toLowerCase().includes(q);
    const matchesLowStock = !lowStockOnly || p.currentStock <= p.minimumQuantity;
    return matchesSearch && matchesLowStock;
  }).
  sort((a, b) => {
    if (sortBy === 'price') return b.price - a.price;
    if (sortBy === 'stock') return a.currentStock - b.currentStock;
    return a.name.localeCompare(b.name);
  });

  const totalPages = Math.ceil(filteredProducts.length / PAGE_SIZE);
  const paginatedProducts = filteredProducts.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE
  );

  const handleStockAdjust = useCallback(
    async (productId: string, delta: number) => {
      const product = products.find((item) => item.id === productId);
      if (!product) return;
      try {
        await apiFetch(`/api/spaces/${product.spaceId}/products/${productId}/stock/${delta > 0 ? 'add' : 'remove'}`, {
          method: 'POST', body: JSON.stringify({ quantity: 1 }),
        });
        await loadProducts();
      } catch (error) {
        toast.error(error instanceof Error ? error.message : 'Unable to update stock');
        throw error;
      }
    },
    [loadProducts, products]
  );

  const handleAddProduct = useCallback(async (newProduct: Omit<Product, 'id' | 'spaceId' | 'imageUrl' | 'imageAlt'>) => {
    const spaceId = spaceIds[0];
    if (!spaceId) { toast.error('Create a space before adding a product'); return; }
    try {
      await apiFetch(`/api/spaces/${spaceId}/products`, { method: 'POST', body: JSON.stringify(newProduct) });
      setAddModalOpen(false);
      await loadProducts();
      toast.success(`"${newProduct.name}" added`);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Unable to add product');
    }
  }, [loadProducts, spaceIds]);

  const handleDeleteProduct = useCallback(async (productId: string) => {
    const product = products.find((item) => item.id === productId);
    if (!product) return;
    try {
      await apiFetch(`/api/spaces/${product.spaceId}/products/${productId}`, { method: 'DELETE' });
      await loadProducts();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Unable to delete product');
      throw error;
    }
  }, [loadProducts, products]);

  const handleExportCSV = () => {
    const headers = ['Name', 'SKU', 'Category', 'Price', 'Current Stock', 'Min Qty', 'Max Qty'];
    const rows = filteredProducts.map((p) => [
    p.name,
    p.sku,
    p.category,
    p.price.toFixed(2),
    p.currentStock,
    p.minimumQuantity,
    p.maximumQuantity]
    );
    const csv = [headers, ...rows].map((r) => r.join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'wh001-products.csv';
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="px-6 py-6 max-w-screen-2xl mx-auto">
      {/* Page Header */}
      <div className="mb-5">
        <h1 className="text-2xl font-semibold text-foreground">Product Management</h1>
        <p className="text-sm text-muted-foreground mt-0.5">
          {products.length} products across your accessible spaces
        </p>
      </div>

      {/* Toolbar */}
      <ProductToolbar
        searchQuery={searchQuery}
        onSearchChange={(v) => {setSearchQuery(v);setCurrentPage(1);}}
        lowStockOnly={lowStockOnly}
        onLowStockToggle={(v) => {setLowStockOnly(v);setCurrentPage(1);}}
        sortBy={sortBy}
        onSortChange={(v) => setSortBy(v as 'name' | 'price' | 'stock')}
        onExportCSV={handleExportCSV}
        onAddProduct={() => setAddModalOpen(true)} />


      {/* Table */}
      <ProductTable
        products={paginatedProducts}
        onStockAdjust={handleStockAdjust}
        onDelete={handleDeleteProduct}
        currentPage={currentPage}
        totalPages={totalPages}
        totalElements={filteredProducts.length}
        pageSize={PAGE_SIZE}
        onPageChange={setCurrentPage} />


      {/* Summary Cards */}
      <ProductSummaryCards products={products} />

      {/* Add Product Modal */}
      <AddProductModal
        open={addModalOpen}
        onClose={() => setAddModalOpen(false)}
        onSubmit={handleAddProduct} />

    </div>);

}
