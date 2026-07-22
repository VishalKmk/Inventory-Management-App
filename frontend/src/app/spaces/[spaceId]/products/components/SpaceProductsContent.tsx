'use client';
import React, { useState, useCallback, useEffect } from 'react';

import AppLayout from '@/components/AppLayout';
import { Search, Plus, Download, Minus, AlertTriangle, Trash2, X, DollarSign, TrendingUp,  } from 'lucide-react';
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

interface Product {
  id: string;
  name: string;
  price: number;
  currentStock: number;
  minimumQuantity: number;
  maximumQuantity: number;
  isLowStock: boolean;
}

const INITIAL_PRODUCTS: Product[] = [
  { id: 'p1', name: 'High-Tensile Steel Bolts', price: 24.5, currentStock: 1450, minimumQuantity: 500, maximumQuantity: 2000, isLowStock: false },
  { id: 'p2', name: 'Industrial Grade Servo Motor', price: 580.0, currentStock: 12, minimumQuantity: 50, maximumQuantity: 150, isLowStock: true },
  { id: 'p3', name: 'Reinforced Cargo Pallets', price: 42.0, currentStock: 420, minimumQuantity: 100, maximumQuantity: 500, isLowStock: false },
  { id: 'p4', name: 'IoT Inventory Tracker Nodes', price: 125.75, currentStock: 3, minimumQuantity: 25, maximumQuantity: 100, isLowStock: true },
  { id: 'p5', name: 'Industrial Power Drill v2', price: 189.99, currentStock: 12, minimumQuantity: 50, maximumQuantity: 200, isLowStock: true },
  { id: 'p6', name: 'Pro-Shield Safety Helmet', price: 34.0, currentStock: 85, minimumQuantity: 100, maximumQuantity: 400, isLowStock: true },
  { id: 'p7', name: 'Precision Copper Spool 50m', price: 67.5, currentStock: 4, minimumQuantity: 20, maximumQuantity: 80, isLowStock: true },
  { id: 'p8', name: 'Hydraulic Seal Kit 3/4"', price: 52.25, currentStock: 18, minimumQuantity: 40, maximumQuantity: 120, isLowStock: true },
  { id: 'p9', name: 'Cut-Resistant Work Gloves L', price: 18.9, currentStock: 310, minimumQuantity: 80, maximumQuantity: 600, isLowStock: false },
  { id: 'p10', name: 'Conveyor Drive Belt 2m', price: 143.0, currentStock: 27, minimumQuantity: 15, maximumQuantity: 60, isLowStock: false },
];

function getStockStatus(p: Product) {
  if (p.currentStock === 0) return { label: 'OUT OF STOCK', color: 'text-danger', barClass: 'stock-bar-fill-critical' };
  if (p.currentStock <= p.minimumQuantity) return { label: 'LOW STOCK', color: 'text-warning', barClass: 'stock-bar-fill-low' };
  return { label: '', color: '', barClass: 'stock-bar-fill-healthy' };
}

interface AddProductModalProps {
  onClose: () => void;
  onSubmit: (p: Omit<Product, 'id' | 'isLowStock'>) => void;
}

function AddProductModal({ onClose, onSubmit }: AddProductModalProps) {
  const [form, setForm] = useState({ name: '', price: '', currentStock: '', minimumQuantity: '', maximumQuantity: '' });
  const set = (k: string, v: string) => setForm((f) => ({ ...f, [k]: v }));
  const valid = form.name && form.price && form.currentStock && form.minimumQuantity && form.maximumQuantity;

  return (
    <div className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4">
      <div className="bg-card rounded-xl border border-border shadow-xl w-full max-w-md p-6">
        <div className="flex items-center justify-between mb-5">
          <h2 className="text-lg font-semibold text-foreground">Add Product</h2>
          <button onClick={onClose} className="text-muted-foreground hover:text-foreground"><X size={18} /></button>
        </div>
        <div className="space-y-4">
          {[
            { label: 'Product Name', key: 'name', type: 'text', placeholder: 'e.g. Steel Bolts' },
            { label: 'Price ($)', key: 'price', type: 'number', placeholder: '0.00' },
            { label: 'Current Stock', key: 'currentStock', type: 'number', placeholder: '0' },
            { label: 'Minimum Quantity', key: 'minimumQuantity', type: 'number', placeholder: '0' },
            { label: 'Maximum Quantity', key: 'maximumQuantity', type: 'number', placeholder: '0' },
          ].map((f) => (
            <div key={f.key}>
              <label className="block text-sm font-medium text-foreground mb-1.5">{f.label}</label>
              <input
                type={f.type}
                value={(form as any)[f.key]}
                onChange={(e) => set(f.key, e.target.value)}
                placeholder={f.placeholder}
                className="input-field"
              />
            </div>
          ))}
        </div>
        <div className="flex gap-3 justify-end mt-5">
          <button onClick={onClose} className="btn-secondary">Cancel</button>
          <button
            disabled={!valid}
            onClick={() => onSubmit({
              name: form.name,
              price: parseFloat(form.price),
              currentStock: parseInt(form.currentStock),
              minimumQuantity: parseInt(form.minimumQuantity),
              maximumQuantity: parseInt(form.maximumQuantity),
            })}
            className="btn-primary"
          >
            Add Product
          </button>
        </div>
      </div>
    </div>
  );
}

interface SpaceProductsContentProps {
  spaceId: string;
}

export default function SpaceProductsContent({ spaceId }: SpaceProductsContentProps) {
  const spaceName = SPACE_NAMES[spaceId] || 'My Space';
  const spaceCode = SPACE_CODES[spaceId] || spaceId.slice(0, 8).toUpperCase();

  const [products, setProducts] = useState<Product[]>([]);
  const [search, setSearch] = useState('');
  const [lowStockOnly, setLowStockOnly] = useState(false);
  const [addOpen, setAddOpen] = useState(false);
  const [page, setPage] = useState(1);
  const PAGE_SIZE = 8;

  const loadProducts = useCallback(async () => {
    try {
      const response = await apiFetch<{ data: Product[] }>(`/api/spaces/${spaceId}/products?page=0&size=100`);
      setProducts(response.data.map((product) => ({ ...product, isLowStock: product.currentStock <= product.minimumQuantity })));
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Unable to load products');
    }
  }, [spaceId]);

  useEffect(() => { void loadProducts(); }, [loadProducts]);

  const filtered = products
    .filter((p) => {
      const q = search.toLowerCase();
      return (!q || p.name.toLowerCase().includes(q)) && (!lowStockOnly || p.isLowStock);
    });

  const totalPages = Math.ceil(filtered.length / PAGE_SIZE);
  const paginated = filtered.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  const adjustStock = useCallback(async (id: string, delta: number) => {
    try {
      await apiFetch(`/api/spaces/${spaceId}/products/${id}/stock/${delta > 0 ? 'add' : 'remove'}`, { method: 'POST', body: JSON.stringify({ quantity: 1 }) });
      await loadProducts();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Unable to update stock');
    }
  }, [loadProducts, spaceId]);

  const deleteProduct = useCallback(async (id: string) => {
    try {
      await apiFetch(`/api/spaces/${spaceId}/products/${id}`, { method: 'DELETE' });
      await loadProducts();
      toast.success('Product deleted');
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Unable to delete product');
    }
  }, [loadProducts, spaceId]);

  const addProduct = useCallback(async (data: Omit<Product, 'id' | 'isLowStock'>) => {
    try {
      await apiFetch(`/api/spaces/${spaceId}/products`, { method: 'POST', body: JSON.stringify(data) });
      setAddOpen(false);
      await loadProducts();
      toast.success('Product added');
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Unable to add product');
    }
  }, [loadProducts, spaceId]);

  const totalValue = products.reduce((s, p) => s + p.price * p.currentStock, 0);
  const lowStockCount = products.filter((p) => p.isLowStock).length;

  const exportCSV = () => {
    const rows = [['Name', 'Price', 'Stock', 'Min', 'Max'], ...filtered.map((p) => [p.name, p.price, p.currentStock, p.minimumQuantity, p.maximumQuantity])];
    const csv = rows.map((r) => r.join(',')).join('\n');
    const a = document.createElement('a');
    a.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
    a.download = `${spaceCode}-products.csv`;
    a.click();
  };

  return (
    <AppLayout
      searchPlaceholder="Search inventory or SKU..."
      userName="Alex Rivera"
      userRole="ADMIN"
      spaceId={spaceId}
      spaceName={spaceName}
      spaceCode={spaceCode}
    >
      <div className="px-6 py-6 max-w-screen-2xl mx-auto">
        {/* Header */}
        <div className="flex items-center justify-between mb-5">
          <div>
            <h1 className="text-2xl font-bold text-foreground">Product Management</h1>
            <p className="text-sm text-muted-foreground mt-0.5">{spaceCode} — {products.length} products total</p>
          </div>
        </div>

        {/* Toolbar */}
        <div className="flex items-center justify-between mb-4 gap-3 flex-wrap">
          <div className="flex items-center gap-3">
            <div className="relative">
              <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
              <input
                type="text"
                value={search}
                onChange={(e) => { setSearch(e.target.value); setPage(1); }}
                placeholder="Search products..."
                className="input-field pl-9 py-2 text-sm w-56"
              />
            </div>
            <label className="flex items-center gap-2 cursor-pointer">
              <span className="text-sm font-medium text-warning">Low Stock Only</span>
              <div
                onClick={() => { setLowStockOnly((p) => !p); setPage(1); }}
                className={`w-10 h-5 rounded-full transition-colors cursor-pointer ${lowStockOnly ? 'bg-warning' : 'bg-muted'}`}
              >
                <div className={`w-4 h-4 bg-white rounded-full shadow mt-0.5 transition-transform ${lowStockOnly ? 'translate-x-5' : 'translate-x-0.5'}`} />
              </div>
            </label>
          </div>
          <div className="flex gap-2">
            <button onClick={exportCSV} className="btn-secondary text-sm">
              <Download size={14} /> Export CSV
            </button>
            <button onClick={() => setAddOpen(true)} className="btn-primary text-sm">
              <Plus size={14} /> Add Product
            </button>
          </div>
        </div>

        {/* Table */}
        <div className="section-card mb-5">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-border">
                  <th className="text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider px-5 py-3">Product Name</th>
                  <th className="text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider px-4 py-3">Price</th>
                  <th className="text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider px-4 py-3">Stock Level</th>
                  <th className="text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider px-4 py-3">Min/Max</th>
                  <th className="text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider px-4 py-3">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {paginated.map((p) => {
                  const status = getStockStatus(p);
                  const pct = Math.min(100, (p.currentStock / p.maximumQuantity) * 100);
                  return (
                    <tr key={p.id} className="hover:bg-secondary/40 transition-colors">
                      <td className="px-5 py-3.5">
                        <p className="text-sm font-semibold text-foreground">{p.name}</p>
                      </td>
                      <td className="px-4 py-3.5 text-sm text-foreground font-medium">${p.price.toFixed(2)}</td>
                      <td className="px-4 py-3.5">
                        <div className="flex items-center gap-3">
                          <div className="w-28">
                            <div className="stock-bar-track">
                              <div className={status.barClass} style={{ width: `${pct}%` }} />
                            </div>
                          </div>
                          <span className="text-sm font-bold text-foreground tabular-nums w-10">{p.currentStock.toLocaleString()}</span>
                          {status.label && (
                            <span className={`text-xs font-semibold ${status.color} flex items-center gap-1`}>
                              <AlertTriangle size={11} /> {status.label}
                            </span>
                          )}
                        </div>
                      </td>
                      <td className="px-4 py-3.5 text-sm text-muted-foreground">{p.minimumQuantity} / {p.maximumQuantity}</td>
                      <td className="px-4 py-3.5">
                        <div className="flex items-center gap-1">
                          <button
                            onClick={() => adjustStock(p.id, -1)}
                            className={`w-7 h-7 flex items-center justify-center rounded border border-border hover:bg-secondary transition-colors ${p.isLowStock ? 'bg-primary/5' : ''}`}
                          >
                            <Minus size={12} />
                          </button>
                          <button
                            onClick={() => adjustStock(p.id, 1)}
                            className={`w-7 h-7 flex items-center justify-center rounded border border-border hover:bg-secondary transition-colors ${p.isLowStock ? 'bg-primary text-white' : ''}`}
                          >
                            <Plus size={12} />
                          </button>
                          <button
                            onClick={() => deleteProduct(p.id)}
                            className="w-7 h-7 flex items-center justify-center rounded border border-border hover:bg-red-50 hover:text-danger transition-colors ml-1"
                          >
                            <Trash2 size={12} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
          {/* Pagination */}
          <div className="flex items-center justify-between px-5 py-3 border-t border-border">
            <p className="text-xs text-muted-foreground">Showing {Math.min((page - 1) * PAGE_SIZE + 1, filtered.length)}–{Math.min(page * PAGE_SIZE, filtered.length)} of {filtered.length} products</p>
            <div className="flex gap-2">
              <button onClick={() => setPage((p) => Math.max(1, p - 1))} disabled={page === 1} className="btn-secondary text-xs py-1.5 px-3 disabled:opacity-40">Previous</button>
              <button onClick={() => setPage((p) => Math.min(totalPages, p + 1))} disabled={page === totalPages} className="btn-secondary text-xs py-1.5 px-3 disabled:opacity-40">Next</button>
            </div>
          </div>
        </div>

        {/* Summary Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {[
            { icon: <DollarSign size={20} className="text-muted-foreground" />, label: 'Total Value', value: `$${totalValue.toLocaleString(undefined, { maximumFractionDigits: 0 })}` },
            { icon: <AlertTriangle size={20} className="text-warning" />, label: 'Low Stock Items', value: `${lowStockCount} Alerts` },
            { icon: <TrendingUp size={20} className="text-success" />, label: 'Monthly Turnover', value: '+12.4%' },
          ].map((c) => (
            <div key={c.label} className="kpi-card flex items-center gap-4">
              <div className="w-10 h-10 bg-secondary rounded-lg flex items-center justify-center flex-shrink-0">{c.icon}</div>
              <div>
                <p className="text-xs text-muted-foreground">{c.label}</p>
                <p className="text-xl font-bold text-foreground">{c.value}</p>
              </div>
            </div>
          ))}
        </div>
      </div>

      {addOpen && <AddProductModal onClose={() => setAddOpen(false)} onSubmit={addProduct} />}
    </AppLayout>
  );
}
