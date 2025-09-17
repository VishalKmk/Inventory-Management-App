package app.web.inventory.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import app.web.inventory.model.Spaces;

public interface SpaceRepository extends JpaRepository<Spaces, UUID> {

    // Find all spaces owned by a user
    List<Spaces> findByOwnerId(UUID ownerId);

    // Count spaces owned by a user
    long countByOwnerId(UUID ownerId);

    // Find space by ID and owner (for security)
    Optional<Spaces> findByIdAndOwnerId(UUID id, UUID ownerId);

    // Check if space name exists for a user (prevent duplicates)
    boolean existsByOwnerIdAndName(UUID ownerId, String name);

    // Find space by name and owner
    Optional<Spaces> findByOwnerIdAndName(UUID ownerId, String name);

    // Count products in a space
    @Query("SELECT COUNT(p) FROM Products p WHERE p.space.id = :spaceId")
    long countProductsInSpace(@Param("spaceId") UUID spaceId);

    // Get spaces with product counts
    @Query("SELECT s, COUNT(p) FROM Spaces s LEFT JOIN Products p ON s.id = p.space.id WHERE s.owner.id = :ownerId GROUP BY s")
    List<Object[]> findSpacesWithProductCount(@Param("ownerId") UUID ownerId);
}