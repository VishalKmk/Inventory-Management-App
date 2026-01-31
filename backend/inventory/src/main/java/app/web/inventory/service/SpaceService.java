package app.web.inventory.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import app.web.inventory.dto.space.SpaceCreationStatusDto;
import app.web.inventory.dto.space.SpaceDto;
import app.web.inventory.dto.space.SpaceResponseDto;
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
    public SpaceResponseDto createSpace(UUID ownerId, String name) {
        Users owner = userService.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        long currentSpaceCount = spaceRepository.countByOwnerId(ownerId);
        if (currentSpaceCount >= 10) {
            throw new IllegalStateException(
                    "Maximum limit of 10 spaces reached. Please delete some spaces to create new ones.");
        }

        if (spaceRepository.existsByOwnerIdAndName(ownerId, name)) {
            throw new IllegalArgumentException("Space with name '" + name + "' already exists");
        }

        Spaces space = new Spaces();
        space.setName(name.trim());
        space.setOwner(owner);

        Spaces savedSpace = spaceRepository.save(space);

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

        return convertToResponseDto(savedSpace);
    }

    /**
     * Update space name
     */
    public SpaceResponseDto updateSpace(UUID spaceId, UUID ownerId, String newName) {
        Spaces space = getSpaceByIdAndOwner(spaceId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Space not found or access denied"));

        if (!space.getName().equals(newName.trim()) &&
                spaceRepository.existsByOwnerIdAndName(ownerId, newName.trim())) {
            throw new IllegalArgumentException("Space with name '" + newName + "' already exists");
        }

        String oldName = space.getName();
        space.setName(newName.trim());

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

        Spaces savedSpace = spaceRepository.save(space);
        return convertToResponseDto(savedSpace);
    }

    /**
     * Delete a space (only if it has no products)
     */
    public void deleteSpace(UUID spaceId, UUID ownerId) {
        Spaces space = getSpaceByIdAndOwner(spaceId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Space not found or access denied"));

        long productCount = spaceRepository.countProductsInSpace(spaceId);
        if (productCount > 0) {
            throw new IllegalStateException(
                    "Cannot delete space with " + productCount + " products. Remove products first.");
        }

        String spaceName = space.getName();
        spaceRepository.delete(space);

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
    }

    /**
     * Get a specific space by ID as DTO
     */
    public SpaceResponseDto getSpaceByIdDto(UUID spaceId, UUID ownerId) {
        Spaces space = getSpaceByIdAndOwner(spaceId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Space not found or access denied"));

        return convertToResponseDto(space);
    }

    /**
     * Get creation status with remaining slots
     */
    public SpaceCreationStatusDto getCreationStatus(UUID ownerId) {
        long currentCount = spaceRepository.countByOwnerId(ownerId);
        int maxSpaces = 10;
        int remaining = Math.max(0, maxSpaces - (int) currentCount);
        boolean canCreate = currentCount < maxSpaces;

        return new SpaceCreationStatusDto(currentCount, maxSpaces, remaining, canCreate);
    }

    /**
     * Get spaces with product counts
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

    // =============================================================================
    // Internal/Utility Methods
    // =============================================================================

    public List<Spaces> getSpacesByOwner(UUID ownerId) {
        return spaceRepository.findByOwnerId(ownerId);
    }

    public java.util.Optional<Spaces> getSpaceByIdAndOwner(UUID spaceId, UUID ownerId) {
        return spaceRepository.findByIdAndOwnerId(spaceId, ownerId);
    }

    public boolean hasAccessToSpace(UUID spaceId, UUID ownerId) {
        return spaceRepository.findByIdAndOwnerId(spaceId, ownerId).isPresent();
    }

    public int getRemainingSpaceSlots(UUID ownerId) {
        long currentCount = spaceRepository.countByOwnerId(ownerId);
        return Math.max(0, 10 - (int) currentCount);
    }

    public boolean canCreateMoreSpaces(UUID ownerId) {
        return spaceRepository.countByOwnerId(ownerId) < 10;
    }

    /**
     * Convert Space entity to Response DTO
     */
    private SpaceResponseDto convertToResponseDto(Spaces space) {
        return new SpaceResponseDto(
                space.getId(),
                space.getName(),
                space.getOwner().getId(),
                space.getOwner().getName(),
                0L, // Product count not available in this context
                space.getCreatedAt(),
                space.getUpdatedAt());
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