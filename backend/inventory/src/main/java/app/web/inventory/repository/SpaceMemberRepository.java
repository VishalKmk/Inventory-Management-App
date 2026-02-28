package app.web.inventory.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import app.web.inventory.model.SpaceMember;
import app.web.inventory.model.enums.SpaceRole;

public interface SpaceMemberRepository extends JpaRepository<SpaceMember, UUID> {

    // Check if user is a member of a space
    boolean existsBySpaceIdAndUserId(UUID spaceId, UUID userId);

    // Find specific membership
    Optional<SpaceMember> findBySpaceIdAndUserId(UUID spaceId, UUID userId);

    // Get all members of a space (paginated)
    Page<SpaceMember> findBySpaceId(UUID spaceId, Pageable pageable);

    // Get all spaces a user is a member of (including PENDING)
    List<SpaceMember> findByUserId(UUID userId);

    // Get all Active spaces for a user (excluding PENDING)
    @Query("SELECT sm FROM SpaceMember sm WHERE sm.user.id = :userId AND sm.role != 'PENDING'")
    List<SpaceMember> findActiveSpacesByUserId(@Param("userId") UUID userId);

    // Check if user has specific role
    // Check if user has specific role
    boolean existsBySpaceIdAndUserIdAndRole(UUID spaceId, UUID userId, SpaceRole role);

    // Find all memberships by user and role (e.g. PENDING)
    List<SpaceMember> findByUserIdAndRole(UUID userId, SpaceRole role);
}
