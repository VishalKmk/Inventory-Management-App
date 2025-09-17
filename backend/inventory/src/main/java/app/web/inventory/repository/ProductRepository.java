package app.web.inventory.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import app.web.inventory.model.Products;

public interface ProductRepository extends JpaRepository<Products, UUID> {

    // Find all products in a specific space
    List<Products> findBySpaceId(UUID spaceId);

    // NEW: Search products by name within a specific space (HIERARCHICAL)
    @Query("SELECT p FROM Products p WHERE p.space.id = :spaceId AND LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Products> findBySpaceIdAndNameContainingIgnoreCase(@Param("spaceId") UUID spaceId, @Param("name") String name);

    // Find all products owned by a user (across all their spaces)
    @Query("SELECT p FROM Products p WHERE p.space.owner.id = :ownerId")
    List<Products> findByOwnerId(@Param("ownerId") UUID ownerId);

    // Find products with low stock (current stock <= minimum quantity) - GLOBAL
    @Query("SELECT p FROM Products p WHERE p.space.owner.id = :ownerId AND p.currentStock <= p.minimumQuantity")
    List<Products> findLowStockProductsByOwnerId(@Param("ownerId") UUID ownerId);

    // Find products in a space with low stock - HIERARCHICAL
    @Query("SELECT p FROM Products p WHERE p.space.id = :spaceId AND p.currentStock <= p.minimumQuantity")
    List<Products> findLowStockProductsBySpaceId(@Param("spaceId") UUID spaceId);

    // Check if product exists in user's spaces (for security) - GLOBAL
    @Query("SELECT p FROM Products p WHERE p.id = :productId AND p.space.owner.id = :ownerId")
    Optional<Products> findByIdAndOwnerId(@Param("productId") UUID productId, @Param("ownerId") UUID ownerId);

    // Search products by name in user's spaces - GLOBAL
    @Query("SELECT p FROM Products p WHERE p.space.owner.id = :ownerId AND LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Products> findByOwnerIdAndNameContainingIgnoreCase(@Param("ownerId") UUID ownerId, @Param("name") String name);

}