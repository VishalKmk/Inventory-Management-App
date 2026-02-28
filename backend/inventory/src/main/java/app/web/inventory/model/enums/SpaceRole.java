package app.web.inventory.model.enums;

public enum SpaceRole {
    OWNER, // Full access, can delete space
    ADMIN, // Can manage products and members, cannot delete space
    MEMBER, // Can view and edit products (stock/price)
    VIEWER, // Read-only access
    PENDING // Waiting for approval (link join flow)
}
