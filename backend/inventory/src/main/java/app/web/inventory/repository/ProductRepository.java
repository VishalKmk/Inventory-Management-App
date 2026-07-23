package app.web.inventory.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import app.web.inventory.model.Products;

public interface ProductRepository extends JpaRepository<Products, UUID> {

    // Delete all products in a specific space
    void deleteBySpaceId(UUID spaceId);

    // Find all products in a specific space
    List<Products> findBySpaceId(UUID spaceId);

    // Find all products in a specific space (Paginated)
    Page<Products> findBySpaceId(UUID spaceId, Pageable pageable);

    @Query("SELECT p FROM Products p WHERE p.space.id = :spaceId AND LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Products> findBySpaceIdAndNameContainingIgnoreCase(@Param("spaceId") UUID spaceId, @Param("name") String name);

    // Paginated
    @Query("SELECT p FROM Products p WHERE p.space.id = :spaceId AND LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Products> findBySpaceIdAndNameContainingIgnoreCase(@Param("spaceId") UUID spaceId, @Param("name") String name,
            Pageable pageable);

    // Find all products owned by a user (across all their spaces)
    @Query("SELECT p FROM Products p WHERE p.space.owner.id = :ownerId")
    List<Products> findByOwnerId(@Param("ownerId") UUID ownerId);

    // Find products with low stock (current stock <= minimum quantity) - GLOBAL
    @Query("SELECT p FROM Products p WHERE p.space.owner.id = :ownerId AND p.currentStock <= p.minimumQuantity")
    List<Products> findLowStockProductsByOwnerId(@Param("ownerId") UUID ownerId);

    // Find product by ID and checks owner OR member:
    @Query("""
                SELECT p FROM Products p
                WHERE p.id = :productId
                AND (
                    p.space.owner.id = :userId
                    OR EXISTS (
                        SELECT sm FROM SpaceMember sm
                        WHERE sm.space.id = p.space.id
                        AND sm.user.id = :userId
                        AND sm.role != 'PENDING'
                    )
                )
            """)
    Optional<Products> findByIdAndUserHasAccess(
            @Param("productId") UUID productId,
            @Param("userId") UUID userId);

    // Find all products in a space where user has access (owner or member):
    @Query("""
                SELECT p FROM Products p
                WHERE p.space.id = :spaceId
                AND (
                    p.space.owner.id = :userId
                    OR EXISTS (
                        SELECT sm FROM SpaceMember sm
                        WHERE sm.space.id = :spaceId
                        AND sm.user.id = :userId
                        AND sm.role != 'PENDING'
                    )
                )
            """)
    List<Products> findBySpaceIdAndUserHasAccess(
            @Param("spaceId") UUID spaceId,
            @Param("userId") UUID userId);

    // Find products with low stock (current stock <= minimum quantity) in a space
    // where user has access (owner or member):
    @Query("""
                SELECT p FROM Products p
                WHERE p.space.id = :spaceId
                AND p.currentStock <= p.minimumQuantity
                AND (
                    p.space.owner.id = :userId
                    OR EXISTS (
                        SELECT sm FROM SpaceMember sm
                        WHERE sm.space.id = :spaceId
                        AND sm.user.id = :userId
                        AND sm.role != 'PENDING'
                    )
                )
            """)
    List<Products> findLowStockProductsBySpaceIdAndUserHasAccess(
            @Param("spaceId") UUID spaceId,
            @Param("userId") UUID userId);

    // Paginated version with access check
    @Query("""
                SELECT p FROM Products p
                WHERE p.space.id = :spaceId
                AND (
                    p.space.owner.id = :userId
                    OR EXISTS (
                        SELECT sm FROM SpaceMember sm
                        WHERE sm.space.id = :spaceId
                        AND sm.user.id = :userId
                        AND sm.role != 'PENDING'
                    )
                )
                AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Products> findBySpaceIdAndUserHasAccessAndSearch(
            @Param("spaceId") UUID spaceId,
            @Param("userId") UUID userId,
            @Param("search") String search,
            Pageable pageable);

    // Find products in a space with low stock - HIERARCHICAL
    @Query("SELECT p FROM Products p WHERE p.space.id = :spaceId AND p.currentStock <= p.minimumQuantity")
    List<Products> findLowStockProductsBySpaceId(@Param("spaceId") UUID spaceId);

    // Search products by name in user's spaces - GLOBAL
    @Query("SELECT p FROM Products p WHERE p.space.owner.id = :ownerId AND LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Products> findByOwnerIdAndNameContainingIgnoreCase(@Param("ownerId") UUID ownerId, @Param("name") String name);

}