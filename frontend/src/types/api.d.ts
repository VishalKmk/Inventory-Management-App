// Space type
export interface Space {
  id: string;
  name: string;
  createdAt: string;
  ownerId?: string;
  ownerName?: string;
  productCount?: number;
}

// Product type
export interface Product {
  id: string;
  name: string;
  spaceId: string;
  spaceName?: string;
  price: number;
  currentStock: number;
  minimumQuantity: number;
  maximumQuantity: number;
  createdAt: string;
  updatedAt?: string;
  isLowStock?: boolean;
}
