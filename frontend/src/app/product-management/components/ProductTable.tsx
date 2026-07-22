'use client';
import React, { useState } from 'react';
import { Minus, Plus, MoreHorizontal, Trash2, Edit2, AlertTriangle, ChevronLeft, ChevronRight } from 'lucide-react';
import AppImage from '@/components/ui/AppImage';
import type { Product } from './ProductManagementContent';
import { toast } from 'sonner';

interface StockBarProps {
  current: number;
  min: number;
  max: number;
}

function StockBar({ current, min, max }: StockBarProps) {
  const pct = max > 0 ? Math.min(100, (current / max) * 100) : 0;
  const isCritical = current <= min * 0.5;
  const isLow = current <= min && !isCritical;
  const fillClass = isCritical
    ? 'stock-bar-fill-critical'
    : isLow
    ? 'stock-bar-fill-low' :'stock-bar-fill-healthy';

  return (
    <div className="flex items-center gap-2 min-w-[140px]">
      <div className="stock-bar-track flex-1">
        <div className={fillClass} style={{ width: `${pct}%` }} />
      </div>
      <span className="text-sm font-semibold text-foreground tabular-nums w-12 text-right">
        {current.toLocaleString()}
      </span>
    </div>
  );
}

interface ProductTableProps {
  products: Product[];
  onStockAdjust: (id: string, delta: number) => Promise<void>;
  onDelete: (id: string) => Promise<void>;
  currentPage: number;
  totalPages: number;
  totalElements: number;
  pageSize: number;
  onPageChange: (page: number) => void;
}

export default function ProductTable({
  products,
  onStockAdjust,
  onDelete,
  currentPage,
  totalPages,
  totalElements,
  pageSize,
  onPageChange,
}: ProductTableProps) {
  const [openMenuId, setOpenMenuId] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const handleDelete = async (product: Product) => {
    setDeletingId(product.id);
    setOpenMenuId(null);
    try {
      await onDelete(product.id);
      toast.success(`Deleted "${product.name}"`);
    } catch {
      // The parent reports the API error.
    } finally {
      setDeletingId(null);
    }
  };

  const handleStockAdd = async (product: Product) => {
    if (product.currentStock >= product.maximumQuantity) {
      toast.error(
        `Cannot exceed maximum quantity. Current: ${product.currentStock}, Maximum: ${product.maximumQuantity}`
      );
      return;
    }
    try {
      await onStockAdjust(product.id, 1);
      toast.success(`+1 unit added to ${product.name}`);
    } catch { /* The parent reports the API error. */ }
  };

  const handleStockRemove = async (product: Product) => {
    if (product.currentStock <= 0) {
      toast.error(`${product.name} is already at zero stock`);
      return;
    }
    try {
      await onStockAdjust(product.id, -1);
      toast.success(`-1 unit removed from ${product.name}`);
    } catch { /* The parent reports the API error. */ }
  };

  const startItem = (currentPage - 1) * pageSize + 1;
  const endItem = Math.min(currentPage * pageSize, totalElements);

  if (products.length === 0) {
    return (
      <div className="section-card mb-4">
        <div className="flex flex-col items-center justify-center py-16 text-center">
          <div className="w-14 h-14 bg-secondary rounded-xl flex items-center justify-center mb-4">
            <AlertTriangle size={24} className="text-muted-foreground" />
          </div>
          <h3 className="text-base font-semibold text-foreground mb-1">No products found</h3>
          <p className="text-sm text-muted-foreground max-w-xs">
            No products match your current filters. Try adjusting your search or toggle off the Low Stock filter.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="section-card mb-4 overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full">
          <thead>
            <tr className="border-b border-border bg-secondary/50">
              {['PRODUCT NAME', 'PRICE', 'STOCK LEVEL', 'MIN / MAX', 'ACTIONS'].map((col) => (
                <th
                  key={`th-pm-${col}`}
                  className="text-left text-xs font-semibold text-muted-foreground tracking-wider px-5 py-3"
                >
                  {col}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {products.map((product) => {
              const isLow = product.currentStock <= product.minimumQuantity;
              const isCritical = product.currentStock <= product.minimumQuantity * 0.5;
              const isDeleting = deletingId === product.id;

              return (
                <tr
                  key={product.id}
                  className={`border-b border-border last:border-0 hover:bg-secondary/40 transition-all duration-300 ${
                    isDeleting ? 'opacity-0 max-h-0' : 'opacity-100'
                  }`}
                >
                  {/* Product Name */}
                  <td className="px-5 py-3.5">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-lg overflow-hidden border border-border flex-shrink-0 bg-secondary">
                        <AppImage
                          src={product.imageUrl}
                          alt={product.imageAlt}
                          width={40}
                          height={40}
                          className="w-full h-full object-cover"
                        />
                      </div>
                      <div>
                        <p className="text-sm font-semibold text-foreground leading-tight">
                          {product.name}
                        </p>
                        <p className="text-xs text-muted-foreground font-mono">
                          SKU: {product.sku}
                        </p>
                      </div>
                    </div>
                  </td>

                  {/* Price */}
                  <td className="px-5 py-3.5">
                    <span className="text-sm font-semibold text-foreground tabular-nums">
                      ${product.price.toFixed(2)}
                    </span>
                  </td>

                  {/* Stock Level */}
                  <td className="px-5 py-3.5">
                    <div className="space-y-1">
                      <StockBar
                        current={product.currentStock}
                        min={product.minimumQuantity}
                        max={product.maximumQuantity}
                      />
                      {isCritical && (
                        <div className="flex items-center gap-1 text-danger">
                          <AlertTriangle size={10} />
                          <span className="text-xs font-semibold">CRITICAL LOW</span>
                        </div>
                      )}
                      {isLow && !isCritical && (
                        <div className="flex items-center gap-1 text-warning">
                          <AlertTriangle size={10} />
                          <span className="text-xs font-semibold">LOW STOCK</span>
                        </div>
                      )}
                    </div>
                  </td>

                  {/* Min / Max */}
                  <td className="px-5 py-3.5">
                    <span className="text-sm text-muted-foreground tabular-nums">
                      {product.minimumQuantity.toLocaleString()} / {product.maximumQuantity.toLocaleString()}
                    </span>
                  </td>

                  {/* Actions */}
                  <td className="px-5 py-3.5">
                    <div className="flex items-center gap-1.5">
                      {/* Decrease */}
                      <button
                        onClick={() => handleStockRemove(product)}
                        disabled={product.currentStock <= 0}
                        className="w-7 h-7 flex items-center justify-center rounded-md border border-border hover:bg-secondary disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
                        title={`Remove 1 unit from ${product.name}`}
                        aria-label={`Remove 1 unit from ${product.name}`}
                      >
                        <Minus size={12} className="text-foreground" />
                      </button>

                      {/* Increase */}
                      <button
                        onClick={() => handleStockAdd(product)}
                        disabled={product.currentStock >= product.maximumQuantity}
                        className={`w-7 h-7 flex items-center justify-center rounded-md border disabled:opacity-40 disabled:cursor-not-allowed transition-colors ${
                          isLow
                            ? 'bg-primary border-primary text-primary-foreground hover:opacity-90'
                            : 'border-border hover:bg-secondary'
                        }`}
                        title={`Add 1 unit to ${product.name}`}
                        aria-label={`Add 1 unit to ${product.name}`}
                      >
                        <Plus size={12} className={isLow ? 'text-primary-foreground' : 'text-foreground'} />
                      </button>

                      {/* More */}
                      <div className="relative">
                        <button
                          onClick={() => setOpenMenuId(openMenuId === product.id ? null : product.id)}
                          className="w-7 h-7 flex items-center justify-center rounded-md border border-border hover:bg-secondary transition-colors"
                          title="More actions"
                          aria-label={`More actions for ${product.name}`}
                          aria-expanded={openMenuId === product.id}
                        >
                          <MoreHorizontal size={12} className="text-foreground" />
                        </button>

                        {openMenuId === product.id && (
                          <>
                            <div
                              className="fixed inset-0 z-10"
                              onClick={() => setOpenMenuId(null)}
                            />
                            <div className="absolute right-0 top-full mt-1 bg-card border border-border rounded-lg shadow-elevated z-20 py-1 min-w-[140px] fade-in">
                              <button
                                className="w-full flex items-center gap-2 px-3 py-2 text-sm text-foreground hover:bg-secondary transition-colors"
                                onClick={() => { setOpenMenuId(null); toast.info(`Edit ${product.name} — form would open here`); }}
                              >
                                <Edit2 size={13} className="text-muted-foreground" />
                                Edit Product
                              </button>
                              <button
                                className="w-full flex items-center gap-2 px-3 py-2 text-sm text-danger hover:bg-danger/5 transition-colors"
                                onClick={() => handleDelete(product)}
                              >
                                <Trash2 size={13} />
                                Delete Product
                              </button>
                            </div>
                          </>
                        )}
                      </div>
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      <div className="flex items-center justify-between px-5 py-3.5 border-t border-border">
        <p className="text-sm text-muted-foreground">
          Showing {startItem}–{endItem} of {totalElements} products
        </p>
        <div className="flex items-center gap-1.5">
          <button
            onClick={() => onPageChange(currentPage - 1)}
            disabled={currentPage === 1}
            className="w-8 h-8 flex items-center justify-center rounded-md border border-border hover:bg-secondary disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
            aria-label="Previous page"
          >
            <ChevronLeft size={14} />
          </button>
          {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
            <button
              key={`page-${page}`}
              onClick={() => onPageChange(page)}
              className={`w-8 h-8 flex items-center justify-center rounded-md text-sm font-medium transition-colors ${
                page === currentPage
                  ? 'bg-primary text-primary-foreground'
                  : 'border border-border hover:bg-secondary text-foreground'
              }`}
              aria-current={page === currentPage ? 'page' : undefined}
            >
              {page}
            </button>
          ))}
          <button
            onClick={() => onPageChange(currentPage + 1)}
            disabled={currentPage === totalPages}
            className="w-8 h-8 flex items-center justify-center rounded-md border border-border hover:bg-secondary disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
            aria-label="Next page"
          >
            <ChevronRight size={14} />
          </button>
        </div>
      </div>
    </div>
  );
}
