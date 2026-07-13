# Inventory Management API Documentation

This document provides a detailed overview of the Inventory Management API endpoints.

**Base URL:** `http://localhost:8080`

**Authentication:**
Most endpoints require authentication via a JSON Web Token (JWT). To authenticate, include the following header in your requests:
`Authorization: Bearer <YOUR_JWT_TOKEN>`

Endpoints under the `/api/auth` path are public and do not require authentication.

---

## Table of Contents
1.  [Authentication](#authentication-endpoints)
2.  [Users](#user-endpoints)
3.  [Spaces](#space-endpoints)
4.  [Space Members](#space-member-endpoints)
5.  [Products](#product-endpoints)
6.  [Audit Logs](#audit-log-endpoints)
7.  [Dashboard](#dashboard-endpoints)

---

<a name="authentication-endpoints"></a>
## 1. Authentication Endpoints

Base Path: `/api/auth`

### 1.1 Register a New User
- **Endpoint:** `POST /api/auth/register`
- **Description:** Registers a new user account. An OTP will be sent to the provided email for verification.
- **Auth Required:** No

**Request Body:**
```json
{
  "name": "John Doe",
  "email": "john.doe@example.com",
  "password": "Password123!"
}
```

**Success Response (201 Created):**
```json
{
  "success": true,
  "message": "User registered. OTP sent to email",
  "data": {
    "id": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
    "email": "john.doe@example.com",
    "name": "John Doe",
    "verified": false,
    "createdAt": "2023-10-27T10:00:00Z"
  }
}
```

**Error Response (409 Conflict):**
```json
{
  "success": false,
  "message": "User already exists with email: john.doe@example.com",
  "data": null
}
```

### 1.2 Verify Email with OTP
- **Endpoint:** `POST /api/auth/verify-otp`
- **Description:** Verifies a user's email address using the OTP sent during registration.
- **Auth Required:** No

**Request Body:**
```json
{
  "email": "john.doe@example.com",
  "code": "123456"
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Email verified successfully",
  "data": "OTP verified"
}
```

**Error Response (400 Bad Request):**
```json
{
  "success": false,
  "message": "Invalid or expired OTP",
  "data": null
}
```

### 1.3 User Login
- **Endpoint:** `POST /api/auth/login`
- **Description:** Authenticates a user and returns a JWT token upon successful login.
- **Auth Required:** No

**Request Body:**
```json
{
  "email": "john.doe@example.com",
  "password": "Password123!"
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhMW..."
  }
}
```

**Error Response (400 Bad Request):**
```json
{
  "success": false,
  "message": "Invalid credentials",
  "data": null
}
```

### 1.4 Resend OTP
- **Endpoint:** `POST /api/auth/resend-otp`
- **Description:** Resends an OTP to the user's email address.
- **Auth Required:** No

**Request Body:**
```json
{
  "email": "john.doe@example.com"
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "OTP resent successfully",
  "data": "OTP sent"
}
```

---

<a name="user-endpoints"></a>
## 2. User Endpoints

Base Path: `/api/users`

### 2.1 Get Current User
- **Endpoint:** `GET /api/users/me`
- **Description:** Retrieves the profile information of the currently authenticated user.
- **Auth Required:** Yes

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": {
    "id": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
    "email": "john.doe@example.com",
    "name": "John Doe",
    "verified": true,
    "createdAt": "2023-10-27T10:00:00Z"
  }
}
```

**Error Response (401 Unauthorized):**
```json
{
  "success": false,
  "message": "Unauthorized",
  "data": null
}
```

### 2.2 Get Pending Invites
- **Endpoint:** `GET /api/users/me/invites`
- **Description:** Retrieves a list of pending space invitations for the current user.
- **Auth Required:** Yes

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": [
    {
      "spaceId": "s1p2a3c4-e5f6-7890-1234-567890abcdef",
      "spaceName": "John's Workshop",
      "spaceOwnerName": "Jane Smith",
      "role": "MEMBER",
      "invitedAt": "2023-10-27T11:00:00Z",
      "invitedBy": "b2c3d4e5-f6a7-8901-2345-67890abcdef1"
    }
  ]
}
```

---

<a name="space-endpoints"></a>
## 3. Space Endpoints

Base Path: `/api/spaces`

### 3.1 Create a New Space
- **Endpoint:** `POST /api/spaces`
- **Description:** Creates a new inventory space. Each user is limited to 10 spaces.
- **Auth Required:** Yes

**Request Body:**
```json
{
  "name": "My Home Inventory"
}
```

**Success Response (201 Created):**
```json
{
  "success": true,
  "message": "Space created successfully",
  "data": {
    "id": "s1p2a3c4-e5f6-7890-1234-567890abcdef",
    "name": "My Home Inventory",
    "ownerId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
    "ownerName": "John Doe",
    "productCount": 0,
    "createdAt": "2023-10-27T12:00:00Z",
    "updatedAt": "2023-10-27T12:00:00Z"
  }
}
```

**Error Response (409 Conflict):**
```json
{
  "success": false,
  "message": "Maximum limit of 10 spaces reached. Please delete some spaces to create new ones.",
  "data": null
}
```

### 3.2 Get Owned Spaces
- **Endpoint:** `GET /api/spaces/owned`
- **Description:** Retrieves a list of spaces owned by the current user.
- **Auth Required:** Yes

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": [
    {
      "id": "s1p2a3c4-e5f6-7890-1234-567890abcdef",
      "name": "My Home Inventory",
      "ownerId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
      "ownerName": "John Doe",
      "productCount": 15,
      "currentUserRole": "OWNER"
    }
  ]
}
```

### 3.3 Get Shared Spaces
- **Endpoint:** `GET /api/spaces/shared`
- **Description:** Retrieves a list of spaces the current user is a member of (but does not own).
- **Auth Required:** Yes

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": [
    {
      "id": "s2p3a4c5-e6f7-8901-2345-67890abcdef1",
      "name": "Office Supplies",
      "ownerId": "b2c3d4e5-f6a7-8901-2345-67890abcdef1",
      "ownerName": "Jane Smith",
      "productCount": 50,
      "currentUserRole": "ADMIN"
    }
  ]
}
```

### 3.4 Get Space by ID
- **Endpoint:** `GET /api/spaces/{spaceId}`
- **Description:** Retrieves details for a specific space.
- **Auth Required:** Yes
- **Path Variable:** `spaceId` (UUID)

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": {
    "id": "s1p2a3c4-e5f6-7890-1234-567890abcdef",
    "name": "My Home Inventory",
    "ownerId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
    "ownerName": "John Doe",
    "productCount": 15,
    "createdAt": "2023-10-27T12:00:00Z",
    "updatedAt": "2023-10-27T12:05:00Z"
  }
}
```

### 3.5 Get Space Creation Status
- **Endpoint:** `GET /api/spaces/creation-status`
- **Description:** Checks if the user can create more spaces.
- **Auth Required:** Yes

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": {
    "currentSpaces": 1,
    "maxSpaces": 10,
    "remainingSlots": 9,
    "canCreateMore": true
  }
}
```

### 3.6 Update a Space
- **Endpoint:** `PUT /api/spaces/{spaceId}`
- **Description:** Updates the name of a space. Requires Owner or Admin role.
- **Auth Required:** Yes
- **Path Variable:** `spaceId` (UUID)

**Request Body:**
```json
{
  "name": "My Updated Home Inventory"
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Space updated successfully",
  "data": {
    "id": "s1p2a3c4-e5f6-7890-1234-567890abcdef",
    "name": "My Updated Home Inventory",
    "ownerId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
    "ownerName": "John Doe",
    "productCount": 15,
    "createdAt": "2023-10-27T12:00:00Z",
    "updatedAt": "2023-10-27T13:00:00Z"
  }
}
```

### 3.7 Delete a Space
- **Endpoint:** `DELETE /api/spaces/{spaceId}`
- **Description:** Deletes a space and all its associated products and members. Only the owner can perform this action.
- **Auth Required:** Yes
- **Path Variable:** `spaceId` (UUID)

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Space deleted successfully",
  "data": null
}
```

---

<a name="space-member-endpoints"></a>
## 4. Space Member Endpoints

Base Path: `/api/spaces/{spaceId}/members`

### 4.1 Invite a Member
- **Endpoint:** `POST /api/spaces/{spaceId}/members/invite`
- **Description:** Invites a user to join a space. Requires Owner or Admin role.
- **Auth Required:** Yes
- **Path Variable:** `spaceId` (UUID)

**Request Body:**
```json
{
  "email": "jane.doe@example.com",
  "role": "ADMIN"
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "User invited successfully",
  "data": null
}
```

### 4.2 Get Space Members
- **Endpoint:** `GET /api/spaces/{spaceId}/members`
- **Description:** Retrieves a paginated list of all members in a space.
- **Auth Required:** Yes
- **Path Variable:** `spaceId` (UUID)
- **Query Parameters:** `page` (number, default: 0), `size` (number, default: 20)

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": {
    "content": [
      {
        "id": "m1e2m3b4-e5f6-7890-1234-567890abcdef",
        "userId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
        "userName": "John Doe",
        "email": "john.doe@example.com",
        "role": "OWNER",
        "joinedAt": "2023-10-27T12:00:00Z"
      },
      {
        "id": "m2e3m4b5-f6a7-8901-2345-67890abcdef1",
        "userId": "b2c3d4e5-f6a7-8901-2345-67890abcdef1",
        "userName": "Jane Smith",
        "email": "jane.smith@example.com",
        "role": "ADMIN",
        "joinedAt": "2023-10-27T14:00:00Z"
      }
    ],
    "pageable": { ... },
    "totalElements": 2,
    "totalPages": 1,
    "last": true,
    ...
  }
}
```

### 4.3 Remove a Member
- **Endpoint:** `DELETE /api/spaces/{spaceId}/members/{userId}`
- **Description:** Removes a member from a space. Requires Owner or Admin role. Admins cannot remove other Admins.
- **Auth Required:** Yes
- **Path Variables:** `spaceId` (UUID), `userId` (UUID of member to remove)

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Member removed successfully",
  "data": null
}
```

### 4.4 Accept an Invite
- **Endpoint:** `POST /api/spaces/{spaceId}/members/accept`
- **Description:** Accepts a pending invitation to join a space.
- **Auth Required:** Yes
- **Path Variable:** `spaceId` (UUID of the space to join)

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Invite accepted",
  "data": null
}
```

### 4.5 Decline an Invite
- **Endpoint:** `POST /api/spaces/{spaceId}/members/decline`
- **Description:** Declines a pending invitation to join a space.
- **Auth Required:** Yes
- **Path Variable:** `spaceId` (UUID of the space from the invite)

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Invite declined",
  "data": null
}
```

---

<a name="product-endpoints"></a>
## 5. Product Endpoints

Base Path: `/api/spaces/{spaceId}/products`

### 5.1 Create a Product
- **Endpoint:** `POST /api/spaces/{spaceId}/products`
- **Description:** Adds a new product to a specific space. Requires write access (Owner, Admin, or Member).
- **Auth Required:** Yes
- **Path Variable:** `spaceId` (UUID)

**Request Body:**
```json
{
  "name": "Laptop",
  "price": 1200.00,
  "currentStock": 10,
  "minimumQuantity": 5,
  "maximumQuantity": 20
}
```

**Success Response (201 Created):**
```json
{
  "success": true,
  "message": "Product created successfully",
  "data": {
    "id": "p1r2o3d4-u5c6-7890-1234-567890abcdef",
    "spaceId": "s1p2a3c4-e5f6-7890-1234-567890abcdef",
    "spaceName": "My Home Inventory",
    "name": "Laptop",
    "price": 1200.00,
    "currentStock": 10,
    "minimumQuantity": 5,
    "maximumQuantity": 20,
    "isLowStock": false,
    "createdAt": "2023-10-27T15:00:00Z",
    "updatedAt": "2023-10-27T15:00:00Z"
  }
}
```

### 5.2 Get Products in a Space
- **Endpoint:** `GET /api/spaces/{spaceId}/products`
- **Description:** Retrieves a paginated list of products in a space.
- **Auth Required:** Yes
- **Path Variable:** `spaceId` (UUID)
- **Query Parameters:**
  - `search` (string, optional): Filter products by name.
  - `page` (number, default: 0)
  - `size` (number, default: 10)
  - `sortBy` (string, default: "name")
  - `sortDirection` (string, default: "ASC")

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": {
    "data": [
      {
        "id": "p1r2o3d4-u5c6-7890-1234-567890abcdef",
        "spaceId": "s1p2a3c4-e5f6-7890-1234-567890abcdef",
        "name": "Laptop",
        "price": 1200.00,
        "currentStock": 10,
        "minimumQuantity": 5,
        "maximumQuantity": 20
      }
    ],
    "pagination": {
      "page": 0,
      "size": 10,
      "totalElements": 1,
      "totalPages": 1,
      "hasNext": false,
      "hasPrevious": false
    }
  }
}
```

### 5.3 Get a Specific Product
- **Endpoint:** `GET /api/spaces/{spaceId}/products/{productId}`
- **Description:** Retrieves details for a single product.
- **Auth Required:** Yes
- **Path Variables:** `spaceId` (UUID), `productId` (UUID)

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": {
    "id": "p1r2o3d4-u5c6-7890-1234-567890abcdef",
    "spaceId": "s1p2a3c4-e5f6-7890-1234-567890abcdef",
    "spaceName": "My Home Inventory",
    "name": "Laptop",
    "price": 1200.00,
    "currentStock": 10,
    "minimumQuantity": 5,
    "maximumQuantity": 20,
    "isLowStock": false,
    "createdAt": "2023-10-27T15:00:00Z",
    "updatedAt": "2023-10-27T15:00:00Z"
  }
}
```

### 5.4 Update a Product
- **Endpoint:** `PUT /api/spaces/{spaceId}/products/{productId}`
- **Description:** Updates a product's details. Requires write access.
- **Auth Required:** Yes
- **Path Variables:** `spaceId` (UUID), `productId` (UUID)

**Request Body:**
```json
{
  "name": "Gaming Laptop",
  "price": 1500.00
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Product updated successfully",
  "data": { ... } // Updated product object
}
```

### 5.5 Add Stock
- **Endpoint:** `POST /api/spaces/{spaceId}/products/{productId}/stock/add`
- **Description:** Adds a specified quantity to a product's stock. Requires write access.
- **Auth Required:** Yes
- **Path Variables:** `spaceId` (UUID), `productId` (UUID)

**Request Body:**
```json
{
  "quantity": 5
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Stock added successfully",
  "data": { ... } // Updated product object with new stock
}
```

### 5.6 Remove Stock
- **Endpoint:** `POST /api/spaces/{spaceId}/products/{productId}/stock/remove`
- **Description:** Removes a specified quantity from a product's stock. Requires write access.
- **Auth Required:** Yes
- **Path Variables:** `spaceId` (UUID), `productId` (UUID)

**Request Body:**
```json
{
  "quantity": 2
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Stock removed successfully",
  "data": { ... } // Updated product object with new stock
}
```

### 5.7 Delete a Product
- **Endpoint:** `DELETE /api/spaces/{spaceId}/products/{productId}`
- **Description:** Deletes a product from a space. Requires Owner or Admin role.
- **Auth Required:** Yes
- **Path Variables:** `spaceId` (UUID), `productId` (UUID)

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Product deleted successfully",
  "data": null
}
```

### 5.8 Get Low Stock Products
- **Endpoint:** `GET /api/spaces/{spaceId}/products/low-stock`
- **Description:** Retrieves a list of products in a space that are at or below their minimum quantity.
- **Auth Required:** Yes
- **Path Variable:** `spaceId` (UUID)

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Found 1 products with low stock",
  "data": [
    {
      "id": "p2r3o4d5-u6c7-8901-2345-67890abcdef1",
      "spaceId": "s1p2a3c4-e5f6-7890-1234-567890abcdef",
      "name": "Mouse",
      "price": 50.00,
      "currentStock": 2,
      "minimumQuantity": 3,
      "maximumQuantity": 10
    }
  ]
}
```

---

<a name="audit-log-endpoints"></a>
## 6. Audit Log Endpoints

Base Path: `/api/audit-logs`

### 6.1 Get Audit Logs
- **Endpoint:** `GET /api/audit-logs`
- **Description:** Retrieves a paginated and filterable list of audit logs for the user's activities.
- **Auth Required:** Yes
- **Query Parameters:**
  - `entityType` (string, e.g., "SPACE", "PRODUCT")
  - `operation` (string, e.g., "CREATE", "UPDATE")
  - `entityId` (UUID)
  - `startDate`, `endDate` (ISO DateTime string)
  - `page`, `size`, `sortBy`, `sortDirection`

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": {
    "data": [
      {
        "id": "log-uuid-1",
        "entityType": "PRODUCT",
        "entityId": "p1r2o3d4-...",
        "operation": "CREATE",
        "details": "{\"productName\":\"Laptop\",...}",
        "timestamp": "2023-10-27T15:00:00Z",
        ...
      }
    ],
    "pagination": { ... }
  }
}
```

### 6.2 Get Audit Log Summary
- **Endpoint:** `GET /api/audit-logs/summary`
- **Description:** Retrieves summary statistics of the user's activities.
- **Auth Required:** Yes

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": {
    "totalLogs": 150,
    "spaceLogs": 20,
    "productLogs": 130,
    "createOperations": 30,
    "updateOperations": 70,
    "deleteOperations": 10,
    "stockOperations": 40
  }
}
```

### 6.3 Get Recent Activity
- **Endpoint:** `GET /api/audit-logs/recent`
- **Description:** Retrieves recent activities, typically for a dashboard view.
- **Auth Required:** Yes
- **Query Parameter:** `hours` (number, default: 24)

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Recent activity from last 24 hours",
  "data": [ ... ] // List of AuditLogDto objects
}
```

### 6.4 Get Activity Trends
- **Endpoint:** `GET /api/audit-logs/trends`
- **Description:** Retrieves data for activity trend analysis.
- **Auth Required:** Yes
- **Query Parameter:** `days` (number, default: 30)

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": {
    "dailyActivity": {
      "2023-10-26": 10,
      "2023-10-27": 15
    },
    "operationBreakdown": {
      "CREATE": 5,
      "UPDATE": 12,
      "STOCK_ADD": 8
    },
    "totalActivities": 25,
    "period": "30 days"
  }
}
```

### 6.5 Get Filter Options
- **Endpoint:** `GET /api/audit-logs/filters`
- **Description:** Provides the available options for filtering audit logs.
- **Auth Required:** Yes

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": {
    "entityTypes": ["SPACE", "PRODUCT", "USER"],
    "operations": ["CREATE", "UPDATE", "DELETE", "STOCK_ADD", "STOCK_REMOVE", "STOCK_UPDATE"],
    "sortByOptions": ["timestamp", "entityType", "operation"],
    "sortDirections": ["ASC", "DESC"]
  }
}
```

---

<a name="dashboard-endpoints"></a>
## 7. Dashboard Endpoints

Base Path: `/api/dashboard`

### 7.1 Get Dashboard Overview
- **Endpoint:** `GET /api/dashboard/overview`
- **Description:** Retrieves a high-level overview of the user's entire inventory.
- **Auth Required:** Yes

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": {
    "totalSpaces": 2,
    "maxSpaces": 10,
    "spaceUtilization": 20.0,
    "totalProducts": 65,
    "totalValue": 25400.50,
    "lowStockCount": 8,
    "stockStatus": {
      "inStock": 50,
      "lowStock": 8,
      "outOfStock": 7
    },
    "averageProductsPerSpace": 32.5
  }
}
```

### 7.2 Get Inventory Insights
- **Endpoint:** `GET /api/dashboard/insights`
- **Description:** Provides deeper analytical insights into inventory price and stock distribution.
- **Auth Required:** Yes

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": {
    "hasData": true,
    "priceAnalysis": {
      "minimum": 5.0,
      "maximum": 1500.0,
      "average": 150.75
    },
    "stockAnalysis": {
      "minimum": 0,
      "maximum": 100,
      "average": 25.5,
      "total": 1657
    },
    "valueBySpace": {
      "My Home Inventory": 15400.0,
      "Office Supplies": 10000.50
    },
    "productCountBySpace": {
      "My Home Inventory": 15,
      "Office Supplies": 50
    }
  }
}
```

### 7.3 Get Low Stock Alerts
- **Endpoint:** `GET /api/dashboard/low-stock-alerts`
- **Description:** Retrieves a detailed breakdown of all products with low stock across all spaces.
- **Auth Required:** Yes

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": {
    "totalAlerts": 8,
    "alertsBySpace": {
      "Office Supplies": [
        {
          "productId": "p3r4o5d6-...",
          "productName": "Printer Paper",
          "spaceName": "Office Supplies",
          "currentStock": 1,
          "minimumQuantity": 5,
          "severity": "critical",
          "stockDifference": 4
        }
      ]
    },
    "severityBreakdown": {
      "critical": 2,
      "high": 3,
      "medium": 3
    },
    "hasAlerts": true
  }
}
```

### 7.4 Get Recent Activity Summary
- **Endpoint:** `GET /api/dashboard/recent-activity`
- **Description:** Retrieves a summary of recent activities, formatted for a dashboard feed.
- **Auth Required:** Yes

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": {
    "activities": [
      {
        "id": "log-uuid-1",
        "type": "stock_add",
        "entityType": "product",
        "entityId": "p1r2o3d4-...",
        "timestamp": "2023-10-27T16:00:00Z",
        "description": "Added 5 units to 'Laptop'"
      }
    ],
    "totalCount": 1,
    "hasActivity": true,
    "message": "Recent activities from audit logs"
  }
}
```

### 7.5 Get Space Metrics
- **Endpoint:** `GET /api/dashboard/space-metrics`
- **Description:** Provides performance and health metrics for each of the user's spaces.
- **Auth Required:** Yes

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": {
    "hasData": true,
    "spaceMetrics": [
      {
        "spaceId": "s1p2a3c4-...",
        "spaceName": "My Home Inventory",
        "productCount": 15,
        "totalValue": 15400.0,
        "lowStockCount": 2,
        "healthScore": 85.5
      }
    ],
    "summary": {
      "totalSpaces": 2,
      "totalValue": 25400.50,
      "totalProducts": 65,
      "averageValuePerSpace": 12700.25
    }
  }
}
```

### 7.6 Get Top Products
- **Endpoint:** `GET /api/dashboard/top-products`
- **Description:** Retrieves a list of top products based on value, price, or stock.
- **Auth Required:** Yes
- **Query Parameters:**
  - `limit` (number, default: 5)
  - `sortBy` (string, default: "value", options: "value", "price", "stock")

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": {
    "hasData": true,
    "topProducts": [
      {
        "productId": "p1r2o3d4-...",
        "name": "Gaming Laptop",
        "spaceName": "My Home Inventory",
        "price": 1500.0,
        "currentStock": 8,
        "totalValue": 12000.0,
        "isLowStock": false
      }
    ],
    "sortedBy": "value",
    "limit": 5
  }
}
```

### 7.7 Get Inventory Trends
- **Endpoint:** `GET /api/dashboard/trends`
- **Description:** Retrieves data for visualizing inventory trends over time.
- **Auth Required:** Yes
- **Query Parameter:** `days` (number, default: 30)

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Trend data based on last 30 days",
  "data": {
    "hasHistoricalData": true,
    "currentSnapshot": {
      "date": "2023-10-27T17:00:00Z",
      "totalProducts": 65,
      "totalSpaces": 2,
      "totalValue": 25400.50,
      "lowStockCount": 8
    },
    "dailyActivity": { ... },
    "operationBreakdown": { ... },
    "totalActivities": 150,
    "period": "30 days",
    "message": "Showing inventory trends for the last 30 days",
    "requestedDays": 30
  }
}
```
