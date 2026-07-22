'use client';
import React from 'react';
import { Search, SlidersHorizontal, Download, Plus } from 'lucide-react';
import Toggle from '@/components/ui/Toggle';

interface ProductToolbarProps {
  searchQuery: string;
  onSearchChange: (v: string) => void;
  lowStockOnly: boolean;
  onLowStockToggle: (v: boolean) => void;
  sortBy: string;
  onSortChange: (v: string) => void;
  onExportCSV: () => void;
  onAddProduct: () => void;
}

export default function ProductToolbar({
  searchQuery,
  onSearchChange,
  lowStockOnly,
  onLowStockToggle,
  sortBy,
  onSortChange,
  onExportCSV,
  onAddProduct,
}: ProductToolbarProps) {
  return (
    <div className="flex flex-wrap items-center gap-3 mb-4">
      {/* Search */}
      <div className="relative flex-1 min-w-[200px] max-w-xs">
        <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
        <input
          type="text"
          value={searchQuery}
          onChange={(e) => onSearchChange(e.target.value)}
          placeholder="Search by name, SKU, category..."
          className="input-field pl-9 py-2 text-sm"
        />
      </div>

      {/* Sort */}
      <div className="flex items-center gap-2">
        <SlidersHorizontal size={14} className="text-muted-foreground" />
        <select
          value={sortBy}
          onChange={(e) => onSortChange(e.target.value)}
          className="input-field py-2 text-sm w-auto pr-8 cursor-pointer"
          aria-label="Sort products by"
        >
          <option value="name">Sort: Name A–Z</option>
          <option value="price">Sort: Price High–Low</option>
          <option value="stock">Sort: Stock Low–High</option>
        </select>
      </div>

      {/* Low Stock Toggle */}
      <div className="flex items-center gap-2 border border-border rounded-lg px-3 py-2 bg-card">
        <span className="text-sm font-medium text-foreground">Low Stock Only</span>
        <Toggle checked={lowStockOnly} onChange={onLowStockToggle} />
      </div>

      {/* Spacer */}
      <div className="flex-1" />

      {/* Export CSV */}
      <button onClick={onExportCSV} className="btn-secondary text-sm">
        <Download size={14} />
        Export CSV
      </button>

      {/* Add Product */}
      <button onClick={onAddProduct} className="btn-primary text-sm">
        <Plus size={14} />
        Add Product
      </button>
    </div>
  );
}