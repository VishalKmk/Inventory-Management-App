'use client';
import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import Modal from '@/components/ui/Modal';
import { toast } from 'sonner';
import type { Product } from './ProductManagementContent';

interface AddProductFormValues {
  name: string;
  sku: string;
  category: string;
  price: string;
  currentStock: string;
  minimumQuantity: string;
  maximumQuantity: string;
}

interface AddProductModalProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (product: Omit<Product, 'id' | 'spaceId' | 'imageUrl' | 'imageAlt'>) => void;
}

const CATEGORIES = [
  'Fasteners',
  'Motors & Drives',
  'Logistics',
  'Electronics',
  'Tools & Hardware',
  'Safety Equipment',
  'Electrical',
  'Hydraulics',
  'Mechanical',
  'Other',
];

export default function AddProductModal({ open, onClose, onSubmit }: AddProductModalProps) {
  const [isSubmitting, setIsSubmitting] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<AddProductFormValues>({
    defaultValues: {
      name: '',
      sku: '',
      category: 'Fasteners',
      price: '',
      currentStock: '',
      minimumQuantity: '',
      maximumQuantity: '',
    },
  });

  const handleClose = () => {
    reset();
    onClose();
  };

  const onFormSubmit = async (data: AddProductFormValues) => {
    setIsSubmitting(true);
    const price = parseFloat(data.price);
    const currentStock = parseInt(data.currentStock, 10);
    const minimumQuantity = parseInt(data.minimumQuantity, 10);
    const maximumQuantity = parseInt(data.maximumQuantity, 10);

    if (currentStock > maximumQuantity) {
      toast.error(`Current stock (${currentStock}) cannot exceed maximum quantity (${maximumQuantity})`);
      setIsSubmitting(false);
      return;
    }

    onSubmit({ name: data.name, sku: data.sku, category: data.category, price, currentStock, minimumQuantity, maximumQuantity });
    reset();
    setIsSubmitting(false);
  };

  return (
    <Modal
      open={open}
      onClose={handleClose}
      title="Add New Product"
      subtitle="Register a new SKU in your first accessible space"
      maxWidth="max-w-xl"
    >
      <form onSubmit={handleSubmit(onFormSubmit)} noValidate className="space-y-4">
        {/* Name + SKU */}
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label htmlFor="add-name" className="block text-sm font-medium text-foreground mb-1.5">
              Product Name <span className="text-danger">*</span>
            </label>
            <input
              id="add-name"
              type="text"
              placeholder="e.g. Steel Anchor Bolt M12"
              className="input-field"
              {...register('name', { required: 'Product name is required' })}
            />
            {errors.name && <p className="text-danger text-xs mt-1">{errors.name.message}</p>}
          </div>
          <div>
            <label htmlFor="add-sku" className="block text-sm font-medium text-foreground mb-1.5">
              SKU / Product ID <span className="text-danger">*</span>
            </label>
            <input
              id="add-sku"
              type="text"
              placeholder="e.g. STL-AN-M12"
              className="input-field font-mono"
              {...register('sku', { required: 'SKU is required' })}
            />
            {errors.sku && <p className="text-danger text-xs mt-1">{errors.sku.message}</p>}
          </div>
        </div>

        {/* Category + Price */}
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label htmlFor="add-category" className="block text-sm font-medium text-foreground mb-1.5">
              Category
            </label>
            <select
              id="add-category"
              className="input-field cursor-pointer"
              {...register('category')}
            >
              {CATEGORIES.map((cat) => (
                <option key={`cat-${cat}`} value={cat}>
                  {cat}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label htmlFor="add-price" className="block text-sm font-medium text-foreground mb-1.5">
              Unit Price (USD) <span className="text-danger">*</span>
            </label>
            <div className="relative">
              <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground text-sm">$</span>
              <input
                id="add-price"
                type="number"
                step="0.01"
                min="0"
                placeholder="0.00"
                className="input-field pl-7"
                {...register('price', {
                  required: 'Price is required',
                  min: { value: 0, message: 'Price must be ≥ 0' },
                })}
              />
            </div>
            {errors.price && <p className="text-danger text-xs mt-1">{errors.price.message}</p>}
          </div>
        </div>

        {/* Stock fields */}
        <div className="grid grid-cols-3 gap-4">
          <div>
            <label htmlFor="add-current-stock" className="block text-sm font-medium text-foreground mb-1.5">
              Current Stock <span className="text-danger">*</span>
            </label>
            <p className="text-xs text-muted-foreground mb-1.5">Units on hand today</p>
            <input
              id="add-current-stock"
              type="number"
              min="0"
              placeholder="0"
              className="input-field"
              {...register('currentStock', {
                required: 'Required',
                min: { value: 0, message: '≥ 0' },
              })}
            />
            {errors.currentStock && (
              <p className="text-danger text-xs mt-1">{errors.currentStock.message}</p>
            )}
          </div>
          <div>
            <label htmlFor="add-min-qty" className="block text-sm font-medium text-foreground mb-1.5">
              Min Quantity <span className="text-danger">*</span>
            </label>
            <p className="text-xs text-muted-foreground mb-1.5">Triggers low stock alert</p>
            <input
              id="add-min-qty"
              type="number"
              min="0"
              placeholder="0"
              className="input-field"
              {...register('minimumQuantity', {
                required: 'Required',
                min: { value: 0, message: '≥ 0' },
              })}
            />
            {errors.minimumQuantity && (
              <p className="text-danger text-xs mt-1">{errors.minimumQuantity.message}</p>
            )}
          </div>
          <div>
            <label htmlFor="add-max-qty" className="block text-sm font-medium text-foreground mb-1.5">
              Max Quantity <span className="text-danger">*</span>
            </label>
            <p className="text-xs text-muted-foreground mb-1.5">Storage capacity ceiling</p>
            <input
              id="add-max-qty"
              type="number"
              min="1"
              placeholder="0"
              className="input-field"
              {...register('maximumQuantity', {
                required: 'Required',
                min: { value: 1, message: '≥ 1' },
              })}
            />
            {errors.maximumQuantity && (
              <p className="text-danger text-xs mt-1">{errors.maximumQuantity.message}</p>
            )}
          </div>
        </div>

        {/* Required fields note */}
        <p className="text-xs text-muted-foreground">
          <span className="text-danger">*</span> Required fields
        </p>

        {/* Actions */}
        <div className="flex items-center justify-end gap-3 pt-2 border-t border-border mt-2">
          <button type="button" onClick={handleClose} className="btn-secondary">
            Cancel
          </button>
          <button type="submit" disabled={isSubmitting} className="btn-primary">
            {isSubmitting ? (
              <>
                <svg className="animate-spin w-4 h-4" viewBox="0 0 24 24" fill="none">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                </svg>
                Registering...
              </>
            ) : (
              'Register Product'
            )}
          </button>
        </div>
      </form>
    </Modal>
  );
}
