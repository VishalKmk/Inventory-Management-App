package app.web.inventory.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import app.web.inventory.dto.SpaceDto;
import app.web.inventory.model.Spaces;
import app.web.inventory.model.Users;
import app.web.inventory.repository.SpaceRepository;
import jakarta.servlet.http.HttpServletRequest;

@Service
@Transactional
public class SpaceService {

    private final SpaceRepository spaceRepository;
    private final UserService userService;
    private final AuditLogService auditLogService;

    public SpaceService(SpaceRepository spaceRepository, UserService userService, AuditLogService auditLogService) {
        this.spaceRepository = spaceRepository;
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    /**
     * Create a new space for the given user (max 10 spaces per user)
     */
    public Spaces createSpace(UUID ownerId, String name) {
        // Check if user exists
        Users owner = userService.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Check space limit (max 10 spaces per user)
        long currentSpaceCount = spaceRepository.countByOwnerId(ownerId);
        if (currentSpaceCount >= 10) {
            throw new IllegalStateException(
                    "Maximum limit of 10 spaces reached. Please delete some spaces to create new ones.");
        }

        // Check for duplicate space name
        if (spaceRepository.existsByOwnerIdAndName(ownerId, name)) {
            throw new IllegalArgumentException("Space with name '" + name + "' already exists");
        }

        Spaces space = new Spaces();
        space.setName(name.trim());
        space.setOwner(owner);

        Spaces savedSpace = spaceRepository.save(space);

        // Log the creation
        Map<String, Object> details = Map.of(
                "spaceName", savedSpace.getName(),
                "action", "Space created");
        auditLogService.logAction(
                ownerId,
                "SPACE",
                savedSpace.getId(),
                "CREATE",
                details,
                getClientIpAddress(),
                getUserAgent(),
                null,
                null);

        return savedSpace;
    }

    /**
     * Get all spaces owned by a user
     */
    public List<Spaces> getSpacesByOwner(UUID ownerId) {
        return spaceRepository.findByOwnerId(ownerId);
    }

    /**
     * Get all spaces owned by a user as DTOs
     */
    public List<SpaceDto> getSpacesDtoByOwner(UUID ownerId) {
        return spaceRepository.findByOwnerId(ownerId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get a space by ID, ensuring it belongs to the owner
     */
    public Optional<Spaces> getSpaceByIdAndOwner(UUID spaceId, UUID ownerId) {
        return spaceRepository.findByIdAndOwnerId(spaceId, ownerId);
    }

    /**
     * Update space name
     */
    public Spaces updateSpace(UUID spaceId, UUID ownerId, String newName) {
        Spaces space = getSpaceByIdAndOwner(spaceId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Space not found or access denied"));

        // Check if new name would create duplicate (excluding current space)
        if (!space.getName().equals(newName.trim()) &&
                spaceRepository.existsByOwnerIdAndName(ownerId, newName.trim())) {
            throw new IllegalArgumentException("Space with name '" + newName + "' already exists");
        }
        String oldName = space.getName();

        space.setName(newName.trim());

        // Log the update
        Map<String, Object> details = Map.of(
                "oldName", oldName,
                "newName", newName.trim(),
                "action", "Space name updated");
        auditLogService.logAction(
                ownerId,
                "SPACE",
                spaceId,
                "UPDATE",
                details,
                getClientIpAddress(),
                getUserAgent(),
                null,
                null);
        return spaceRepository.save(space);
    }

    /**
     * Delete a space (only if it has no products)
     */
    public boolean deleteSpace(UUID spaceId, UUID ownerId) {
        Spaces space = getSpaceByIdAndOwner(spaceId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Space not found or access denied"));

        // Check if space has products
        long productCount = spaceRepository.countProductsInSpace(spaceId);
        if (productCount > 0) {
            throw new IllegalStateException(
                    "Cannot delete space with " + productCount + " products. Remove products first.");
        }
        String spaceName = space.getName();
        spaceRepository.delete(space);

        // Log the deletion
        Map<String, Object> details = Map.of(
                "spaceName", spaceName,
                "action", "Space deleted");
        auditLogService.logAction(
                ownerId,
                "SPACE",
                spaceId,
                "DELETE",
                details,
                getClientIpAddress(),
                getUserAgent(),
                null,
                null);
        return true;
    }

    /**
     * Get spaces with product counts
     */
    /**
     * Retrieves a list of spaces owned by the specified user, each with its
     * associated product count.
     *
     * @param ownerId the UUID of the space owner
     * @return a list of SpaceDto objects containing space details and product
     *         counts
     */
    public List<SpaceDto> getSpacesWithProductCount(UUID ownerId) {
        List<Object[]> results = spaceRepository.findSpacesWithProductCount(ownerId);

        return results.stream()
                .map(result -> {
                    Spaces space = (Spaces) result[0];
                    long productCount = 0;
                    if (result[1] instanceof Number) {
                        productCount = ((Number) result[1]).longValue();
                    }

                    return new SpaceDto(
                            space.getId(),
                            space.getName(),
                            space.getOwner().getId(),
                            space.getOwner().getName(),
                            productCount);
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    /**
     * Check if user has access to space
     */
    public boolean hasAccessToSpace(UUID spaceId, UUID ownerId) {
        return spaceRepository.findByIdAndOwnerId(spaceId, ownerId).isPresent();
    }

    /**
     * Convert Space entity to DTO
     */
    private SpaceDto convertToDto(Spaces space) {
        // productCount is not available in this context, set to 0
        return new SpaceDto(
                space.getId(),
                space.getName(),
                space.getOwner().getId(),
                space.getOwner().getName());
    }

    /**
     * Get space by ID (for internal use, no security check)
     */
    public Optional<Spaces> getSpaceById(UUID spaceId) {
        return spaceRepository.findById(spaceId);
    }

    /**
     * Get remaining space slots for user
     */
    public int getRemainingSpaceSlots(UUID ownerId) {
        long currentCount = spaceRepository.countByOwnerId(ownerId);
        return Math.max(0, 10 - (int) currentCount);
    }

    /**
     * Check if user can create more spaces
     */
    public boolean canCreateMoreSpaces(UUID ownerId) {
        return spaceRepository.countByOwnerId(ownerId) < 10;
    }

    // Helper methods for request context
    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            // Ignore and return null
        }
        return null;
    }

    // Helper method to get User-Agent from request
    private String getUserAgent() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return request.getHeader("User-Agent");
            }
        } catch (Exception e) {
            // Ignore and return null
        }
        return null;
    }
}
