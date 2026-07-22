import React from 'react';

type BadgeVariant = 'critical' | 'high' | 'medium' | 'sku' | 'operation' | 'success' | 'info' | 'neutral';

interface BadgeProps {
  variant: BadgeVariant;
  children: React.ReactNode;
  className?: string;
}

export default function Badge({ variant, children, className = '' }: BadgeProps) {
  const variantClass = {
    critical: 'badge-critical',
    high: 'badge-high',
    medium: 'badge-medium',
    sku: 'badge-sku',
    operation: 'badge-operation',
    success: 'bg-success/10 text-success text-xs font-semibold px-2 py-0.5 rounded-full uppercase tracking-wide',
    info: 'bg-accent/10 text-accent text-xs font-semibold px-2 py-0.5 rounded-full uppercase tracking-wide',
    neutral: 'bg-secondary text-secondary-foreground text-xs font-semibold px-2 py-0.5 rounded-full uppercase tracking-wide',
  }[variant];

  return <span className={`${variantClass} ${className}`}>{children}</span>;
}