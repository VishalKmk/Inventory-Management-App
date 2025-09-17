# API Documentation - Inventory Management System

## Base URL
```
http://localhost:8080/api
```

## Authentication
All endpoints (except auth endpoints) require JWT token in the Authorization header:
```
Authorization: Bearer <your_jwt_token>
```

---

## 1. Authentication Endpoints

### 1.1 Register User
- **URL:** `/auth/register`
- **Method:** `POST`
- **Auth required:** No

**Request Body:**
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "Password123!"
}
```

**Success Response (201):**
```json
{
  "success": true,
  "message": "User registered. OTP sent to email",
  "data": {
    "user": {
      "id": "uuid",
      "email": "john@example.com",
      "name": "John Doe"
    }
  }
}
```

**Error Response (400):**
```json
{
  "success": false,
  "message": "Validation failed",
  "errors": {
    "email": "Email must be valid",
    "password": "Password must contain at least one lowercase letter, one uppercase letter, one digit, and one special character"
  },
  "timestamp": "2025-01-15T10:30:00"
}
```

### 1.2 Verify OTP
- **URL:** `/auth/verify-otp`
- **Method:** `POST`
- **Auth required:** No

**Request Body:**
```json
{
  "email": "john@example.com",
  "code": "123456"
}
```

**Success Response (200):**
```json
{
  "success": true,
  "message": "Email verified"
}
```

**Error Response (400):**
```json
{
  "success": false,
  "message": "Invalid or expired OTP"
}
```

### 1.3 Login
- **URL:** `/auth/login`
- **Method:** `POST`
- **Auth required:** No

**Request Body:**
```json
{
  "email": "john@example.com",
  "password": "Password123!"
}
```

**Success Response (200):**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

**Error Response (401):**
```json
{
  "success": false,
  "message": "Invalid credentials",
  "timestamp": "2025-01-15T10:30:00"
}
```

### 1.4 Resend OTP
- **URL:** `/auth/resend-otp`
- **Method:** `POST`
- **Auth required:** No

**Request Body:**
```json
{
  "email": "john@example.com"
}
```

**Success Response (200):**
```json
{
  "success": true,
  "message": "OTP resent"
}
```

---

## 2. User Endpoints

### 2.1 Get Current User
- **URL:** `/users/me`
- **Method:** `GET`
- **Auth required:** Yes

**Success Response (200):**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "email": "john@example.com",
    "name": "John Doe",
    "verified": true,
    "createdAt": "2025-01-15T10:30:00Z"
  }
}
```

**Error Response (401):**
```json
{
  "success": false,
  "message": "Unauthorized"
}
```

---

## 3. Space Endpoints

### 3.1 Create Space
- **URL:** `/spaces`
- **Method:** `POST`
- **Auth required:** Yes

**Request Body:**
```json
{
  "name": "Living Room"
}
```

**Success Response (201):**
```json
{
  "success": true,
  "message": "Space created successfully",
  "data": {
    "id": "uuid",
    "name": "Living Room",
    "ownerId": "uuid",
    "createdAt": "2025-01-15T10:30:00Z",
    "remainingSlots": 9
  }
}
```

**Error Response (400):**
```json
{
  "success": false,
  "message": "Maximum limit of 10 spaces reached. Please delete some spaces to create new ones."
}
```

### 3.2 Get All Spaces
- **URL:** `/spaces`
- **Method:** `GET`
- **Auth required:** Yes

**Success Response (200):**
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "name": "Living Room",
      "ownerId": "uuid",
      "ownerName": "John Doe",
      "productCount": 5
    }
  ]
}
```

### 3.3 Get Space by ID
- **URL:** `/spaces/{spaceId}`
- **Method:** `GET`
- **Auth required:** Yes

**Success Response (200):**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "name": "Living Room",
    "ownerId": "uuid",
    "ownerName": "John Doe",
    "createdAt": "2025-01-15T10:30:00Z",
    "updatedAt": "2025-01-15T10:30:00Z"
  }
}
```

**Error Response (404):**
```json
{
  "success": false,
  "message": "Space not found"
}
```

### 3.4 Update Space
- **URL:** `/spaces/{spaceId}`
- **Method:** `PUT`
- **Auth required:** Yes

**Request Body:**
```json
{
  "name": "Updated Living Room"
}
```

**Success Response (200):**
```json
{
  "success": true,
  "message": "Space updated successfully",
  "data": {
    "id": "uuid",
    "name": "Updated Living Room",
    "ownerId": "uuid",
    "updatedAt": "2025-01-15T10:30:00Z"
  }
}
```

### 3.5 Delete Space
- **URL:** `/spaces/{spaceId}`
- **Method:** `DELETE`
- **Auth required:** Yes

**Success Response (200):**
```json
{
  "success": true,
  "message": "Space deleted successfully"
}
```

**Error Response (400):**
```json
{
  "success": false,
  "message": "Cannot delete space with 3 products. Remove products first."
}
```

### 3.6 Get Space Creation Status
- **URL:** `/spaces/creation-status`
- **Method:** `GET`
- **Auth required:** Yes

**Success Response (200):**
```json
{
  "success": true,
  "data": {
    "currentSpaces": 5,
    "maxSpaces": 10,
    "remainingSlots": 5,
    "canCreateMore": true
  }
}
```

---

## 4. Product Endpoints

### 4.1 Create Product
- **URL:** `/spaces/{spaceId}/products`
- **Method:** `POST`
- **Auth required:** Yes

**Request Body:**
```json
{
  "name": "Laptop",
  "price": 999.99,
  "currentStock": 5,
  "minimumQuantity": 2,
  "maximumQuantity": 10
}
```

**Success Response (201):**
```json
{
  "success": true,
  "message": "Product created successfully",
  "data": {
    "id": "uuid",
    "spaceId": "uuid",
    "spaceName": "Living Room",
    "name": "Laptop",
    "price": 999.99,
    "currentStock": 5,
    "minimumQuantity": 2,
    "maximumQuantity": 10,
    "createdAt": "2025-01-15T10:30:00Z"
  }
}
```

**Error Response (400):**
```json
{
  "success": false,
  "message": "Validation failed",
  "errors": {
    "name": "Product name is required",
    "price": "Price must be non-negative"
  },
  "timestamp": "2025-01-15T10:30:00"
}
```

### 4.2 Get Products in Space
- **URL:** `/spaces/{spaceId}/products`
- **Method:** `GET`
- **Auth required:** Yes
- **Query Parameters:**
  - `search` (optional): Search products by name

**Success Response (200):**
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "spaceId": "uuid",
      "name": "Laptop",
      "price": 999.99,
      "currentStock": 5,
      "minimumQuantity": 2,
      "maximumQuantity": 10
    }
  ]
}
```

### 4.3 Get Product by ID
- **URL:** `/spaces/{spaceId}/products/{productId}`
- **Method:** `GET`
- **Auth required:** Yes

**Success Response (200):**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "spaceId": "uuid",
    "spaceName": "Living Room",
    "name": "Laptop",
    "price": 999.99,
    "currentStock": 5,
    "minimumQuantity": 2,
    "maximumQuantity": 10,
    "isLowStock": false,
    "createdAt": "2025-01-15T10:30:00Z",
    "updatedAt": "2025-01-15T10:30:00Z"
  }
}
```

**Error Response (404):**
```json
{
  "success": false,
  "message": "Product not found in this space"
}
```

### 4.4 Update Product
- **URL:** `/spaces/{spaceId}/products/{productId}`
- **Method:** `PUT`
- **Auth required:** Yes

**Request Body:**
```json
{
  "name": "Updated Laptop",
  "price": 1099.99,
  "minimumQuantity": 3,
  "maximumQuantity": 15
}
```

**Success Response (200):**
```json
{
  "success": true,
  "message": "Product updated successfully",
  "data": {
    "id": "uuid",
    "name": "Updated Laptop",
    "price": 1099.99,
    "minimumQuantity": 3,
    "maximumQuantity": 15,
    "updatedAt": "2025-01-15T10:30:00Z"
  }
}
```

### 4.5 Add Stock
- **URL:** `/spaces/{spaceId}/products/{productId}/stock/add`
- **Method:** `POST`
- **Auth required:** Yes

**Request Body:**
```json
{
  "quantity": 10
}
```

**Success Response (200):**
```json
{
  "success": true,
  "message": "Stock added successfully",
  "data": {
    "id": "uuid",
    "currentStock": 15,
    "quantityAdded": 10,
    "updatedAt": "2025-01-15T10:30:00Z"
  }
}
```

**Error Response (400):**
```json
{
  "success": false,
  "message": "Quantity to add must be positive"
}
```

### 4.6 Remove Stock
- **URL:** `/spaces/{spaceId}/products/{productId}/stock/remove`
- **Method:** `POST`
- **Auth required:** Yes

**Request Body:**
```json
{
  "quantity": 5
}
```

**Success Response (200):**
```json
{
  "success": true,
  "message": "Stock removed successfully",
  "data": {
    "id": "uuid",
    "currentStock": 10,
    "quantityRemoved": 5,
    "isLowStock": false,
    "updatedAt": "2025-01-15T10:30:00Z"
  }
}
```

**Error Response (400):**
```json
{
  "success": false,
  "message": "Insufficient stock. Current: 5, Requested: 10"
}
```

### 4.7 Delete Product
- **URL:** `/spaces/{spaceId}/products/{productId}`
- **Method:** `DELETE`
- **Auth required:** Yes

**Success Response (200):**
```json
{
  "success": true,
  "message": "Product deleted successfully"
}
```

### 4.8 Get Low Stock Products in Space
- **URL:** `/spaces/{spaceId}/products/low-stock`
- **Method:** `GET`
- **Auth required:** Yes

**Success Response (200):**
```json
{
  "success": true,
  "message": "Found 2 products with low stock",
  "data": [
    {
      "id": "uuid",
      "spaceId": "uuid",
      "name": "Laptop",
      "price": 999.99,
      "currentStock": 1,
      "minimumQuantity": 2,
      "maximumQuantity": 10
    }
  ]
}
```

---

## 5. Dashboard Endpoints

### 5.1 Dashboard Overview
- **URL:** `/dashboard/overview`
- **Method:** `GET`
- **Auth required:** Yes

**Success Response (200):**
```json
{
  "success": true,
  "data": {
    "totalSpaces": 5,
    "maxSpaces": 10,
    "spaceUtilization": 50.0,
    "totalProducts": 25,
    "totalValue": 12500.50,
    "lowStockCount": 3,
    "stockStatus": {
      "inStock": 20,
      "lowStock": 3,
      "outOfStock": 2
    },
    "averageProductsPerSpace": 5.0
  }
}
```

### 5.2 Inventory Insights
- **URL:** `/dashboard/insights`
- **Method:** `GET`
- **Auth required:** Yes

**Success Response (200):**
```json
{
  "success": true,
  "data": {
    "hasData": true,
    "priceAnalysis": {
      "minimum": 10.99,
      "maximum": 1999.99,
      "average": 299.50
    },
    "stockAnalysis": {
      "minimum": 0,
      "maximum": 100,
      "average": 15.5,
      "total": 388
    },
    "valueBySpace": {
      "Living Room": 5500.25,
      "Kitchen": 2300.75
    },
    "productCountBySpace": {
      "Living Room": 15,
      "Kitchen": 10
    }
  }
}
```

### 5.3 Low Stock Alerts
- **URL:** `/dashboard/low-stock-alerts`
- **Method:** `GET`
- **Auth required:** Yes

**Success Response (200):**
```json
{
  "success": true,
  "data": {
    "totalAlerts": 3,
    "hasAlerts": true,
    "alertsBySpace": {
      "Living Room": [
        {
          "productId": "uuid",
          "productName": "Laptop",
          "spaceName": "Living Room",
          "currentStock": 1,
          "minimumQuantity": 2,
          "severity": "medium",
          "stockDifference": 1
        }
      ]
    },
    "severityBreakdown": {
      "critical": 1,
      "high": 1,
      "medium": 1
    }
  }
}
```

### 5.4 Recent Activity
- **URL:** `/dashboard/recent-activity`
- **Method:** `GET`
- **Auth required:** Yes

**Success Response (200):**
```json
{
  "success": true,
  "data": {
    "hasActivity": true,
    "totalCount": 10,
    "message": "Recent activities from audit logs",
    "activities": [
      {
        "id": "uuid",
        "type": "create",
        "entityType": "product",
        "entityId": "uuid",
        "timestamp": "2025-01-15T10:30:00",
        "ipAddress": "192.168.1.100",
        "description": "Created product 'Laptop' in space 'Living Room'",
        "details": {
          "productName": "Laptop",
          "spaceName": "Living Room"
        }
      }
    ]
  }
}
```

### 5.5 Space Metrics
- **URL:** `/dashboard/space-metrics`
- **Method:** `GET`
- **Auth required:** Yes

**Success Response (200):**
```json
{
  "success": true,
  "data": {
    "hasData": true,
    "spaceMetrics": [
      {
        "spaceId": "uuid",
        "spaceName": "Living Room",
        "productCount": 15,
        "totalValue": 5500.25,
        "lowStockCount": 2,
        "healthScore": 85.5
      }
    ],
    "summary": {
      "totalSpaces": 5,
      "totalValue": 12500.50,
      "totalProducts": 25,
      "averageValuePerSpace": 2500.10
    }
  }
}
```

### 5.6 Top Products
- **URL:** `/dashboard/top-products`
- **Method:** `GET`
- **Auth required:** Yes
- **Query Parameters:**
  - `limit` (default: 5): Number of products to return
  - `sortBy` (default: "value"): Sort by "value", "stock", or "price"

**Success Response (200):**
```json
{
  "success": true,
  "data": {
    "hasData": true,
    "sortedBy": "value",
    "limit": 5,
    "topProducts": [
      {
        "productId": "uuid",
        "name": "Laptop",
        "spaceName": "Living Room",
        "price": 999.99,
        "currentStock": 5,
        "totalValue": 4999.95,
        "isLowStock": false
      }
    ]
  }
}
```

### 5.7 Inventory Trends
- **URL:** `/dashboard/trends`
- **Method:** `GET`
- **Auth required:** Yes
- **Query Parameters:**
  - `days` (default: 30): Number of days for trend analysis

**Success Response (200):**
```json
{
  "success": true,
  "message": "Trend data based on last 30 days",
  "data": {
    "hasHistoricalData": false,
    "requestedDays": 30,
    "currentSnapshot": {
      "date": "2025-01-15T10:30:00Z",
      "totalProducts": 25,
      "totalSpaces": 5,
      "totalValue": 12500.50,
      "lowStockCount": 3
    },
    "message": "Historical trend data requires audit logging system. Showing current state."
  }
}
```

---

## 6. Audit Log Endpoints

### 6.1 Get Audit Logs
- **URL:** `/audit-logs`
- **Method:** `GET`
- **Auth required:** Yes
- **Query Parameters:**
  - `entityType` (optional): Filter by entity type ("SPACE", "PRODUCT", "USER")
  - `operation` (optional): Filter by operation ("CREATE", "UPDATE", "DELETE", "STOCK_ADD", "STOCK_REMOVE", "STOCK_UPDATE")
  - `entityId` (optional): Filter by specific entity ID
  - `startDate` (optional): Start date for date range filter (ISO format)
  - `endDate` (optional): End date for date range filter (ISO format)
  - `page` (default: 0): Page number
  - `size` (default: 20): Page size
  - `sortBy` (default: "timestamp"): Sort field
  - `sortDirection` (default: "DESC"): Sort direction

**Success Response (200):**
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "entityType": "PRODUCT",
      "entityId": "uuid",
      "operation": "CREATE",
      "details": "{\"productName\":\"Laptop\",\"spaceName\":\"Living Room\"}",
      "timestamp": "2025-01-15T10:30:00",
      "ipAddress": "192.168.1.100",
      "relatedEntityId": "uuid",
      "relatedEntityType": "SPACE"
    }
  ],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

### 6.2 Audit Log Summary
- **URL:** `/audit-logs/summary`
- **Method:** `GET`
- **Auth required:** Yes

**Success Response (200):**
```json
{
  "success": true,
  "data": {
    "totalLogs": 150,
    "spaceLogs": 25,
    "productLogs": 125,
    "createOperations": 40,
    "updateOperations": 60,
    "deleteOperations": 10,
    "stockOperations": 40
  }
}
```

### 6.3 Recent Activity
- **URL:** `/audit-logs/recent`
- **Method:** `GET`
- **Auth required:** Yes
- **Query Parameters:**
  - `hours` (default: 24): Number of hours to look back

**Success Response (200):**
```json
{
  "success": true,
  "message": "Recent activity from last 24 hours",
  "data": [
    {
      "id": "uuid",
      "entityType": "PRODUCT",
      "entityId": "uuid",
      "operation": "STOCK_ADD",
      "details": "{\"productName\":\"Laptop\",\"quantityAdded\":5}",
      "timestamp": "2025-01-15T10:30:00",
      "ipAddress": "192.168.1.100"
    }
  ]
}
```

### 6.4 Activity Trends
- **URL:** `/audit-logs/trends`
- **Method:** `GET`
- **Auth required:** Yes
- **Query Parameters:**
  - `days` (default: 30): Number of days for trend analysis

**Success Response (200):**
```json
{
  "success": true,
  "data": {
    "dailyActivity": {
      "2025-01-14": 15,
      "2025-01-15": 10
    },
    "operationBreakdown": {
      "CREATE": 20,
      "UPDATE": 30,
      "DELETE": 5,
      "STOCK_ADD": 25,
      "STOCK_REMOVE": 15
    },
    "totalActivities": 95,
    "period": "30 days"
  }
}
```

### 6.5 Filter Options
- **URL:** `/audit-logs/filters`
- **Method:** `GET`
- **Auth required:** Yes

**Success Response (200):**
```json
{
  "success": true,
  "data": {
    "entityTypes": ["SPACE", "PRODUCT", "USER"],
    "operations": ["CREATE", "UPDATE", "DELETE", "STOCK_ADD", "STOCK_REMOVE", "STOCK_UPDATE"],
    "sortByOptions": ["timestamp", "entityType", "operation"],
    "sortDirections": ["ASC", "DESC"]
  }
}
```

---

## Error Responses

### Common Error Codes

**400 Bad Request:**
```json
{
  "success": false,
  "message": "Validation failed",
  "errors": {
    "field": "Error message"
  },
  "timestamp": "2025-01-15T10:30:00"
}
```

**401 Unauthorized:**
```json
{
  "success": false,
  "message": "Invalid credentials",
  "timestamp": "2025-01-15T10:30:00"
}
```

**403 Forbidden:**
```json
{
  "success": false,
  "message": "Access denied",
  "timestamp": "2025-01-15T10:30:00"
}
```

**404 Not Found:**
```json
{
  "success": false,
  "message": "Resource not found",
  "timestamp": "2025-01-15T10:30:00"
}
```

**500 Internal Server Error:**
```json
{
  "success": false,
  "message": "An unexpected error occurred",
  "timestamp": "2025-01-15T10:30:00",
  "reference": "uuid-for-tracking"
}
```

---

## Request/Response Notes

1. **Timestamps**: All timestamps are in ISO 8601 format (UTC)
2. **UUIDs**: All IDs are UUID v4 format
3. **Pagination**: Uses standard page/size parameters with 0-based indexing
4. **Validation**: Request validation follows Jakarta Bean Validation annotations
5. **Security**: All authenticated endpoints require valid JWT token
6. **CORS**: Configured for localhost:5173 and 127.0.0.1:5173
7. **Rate Limiting**: No explicit rate limiting mentioned in code
8. **Content-Type**: All requests should use `application/json`