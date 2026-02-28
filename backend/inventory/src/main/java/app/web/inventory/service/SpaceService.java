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
import app.web.inventory.dto.space.InviteMemberRequest;
import app.web.inventory.dto.space.SpaceMemberDto;
import app.web.inventory.dto.space.SpaceInviteDto;
import app.web.inventory.exception.DuplicateResourceException;
import app.web.inventory.exception.ResourceNotFoundException;
import app.web.inventory.model.Spaces;
import app.web.inventory.model.Users;
import app.web.inventory.model.SpaceMember;
import app.web.inventory.model.enums.SpaceRole;
import app.web.inventory.repository.SpaceRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@Transactional
public class SpaceService {

    private final SpaceRepository spaceRepository;
    private final UserService userService;
    private final AuditLogService auditLogService;
    private final app.web.inventory.repository.SpaceMemberRepository spaceMemberRepository; // New dependency

    public SpaceService(SpaceRepository spaceRepository, UserService userService, AuditLogService auditLogService,
            app.web.inventory.repository.SpaceMemberRepository spaceMemberRepository) {
        this.spaceRepository = spaceRepository;
        this.userService = userService;
        this.auditLogService = auditLogService;
        this.spaceMemberRepository = spaceMemberRepository;
    }

    /**
     * Create a new space for the given user (max 10 spaces per user)
     */
    public SpaceResponseDto createSpace(UUID ownerId, String name) {
        Users owner = userService.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        long currentSpaceCount = spaceRepository.countByOwnerId(ownerId);
        if (currentSpaceCount >= 10) {
            throw new IllegalStateException(
                    "Maximum limit of 10 spaces reached. Please delete some spaces to create new ones.");
        }

        if (spaceRepository.existsByOwnerIdAndName(ownerId, name)) {
            throw new DuplicateResourceException("Space with name '" + name + "' already exists");
        }

        Spaces space = new Spaces();
        space.setName(name.trim());
        space.setOwner(owner);

        Spaces savedSpace = spaceRepository.save(space);

        // Add creator as OWNER member
        addMemberToSpace(savedSpace, owner, app.web.inventory.model.enums.SpaceRole.OWNER);

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

    private void addMemberToSpace(Spaces space, Users user, SpaceRole assignedRole, SpaceRole initialStatus,
            UUID invitedBy) {
        app.web.inventory.model.SpaceMember member = new app.web.inventory.model.SpaceMember();
        member.setSpace(space);
        member.setUser(user);
        // If initialStatus is PENDING, we save PENDING as the role column for now,
        // OR we need a separate Status column. The User requested SpaceRole.PENDING.
        // So we set Role = PENDING.
        // But we need to store the "Intended Role" somewhere if we want them to become
        // ADMIN upon accept.
        // For simplicity, we will just set them as PENDING. When they accept, we might
        // default to MEMBER,
        // unless we store intended role.
        // Let's stick to PENDING as the role for now.
        member.setRole(initialStatus != null ? initialStatus : assignedRole);
        member.setInvitedBy(invitedBy);
        // Note: needed a field to store intended role if it's different from PENDING.
        // For now, let's assume all invites start as MEMBER on acceptance.
        spaceMemberRepository.save(member);
    }

    private void addMemberToSpace(Spaces space, Users user, SpaceRole role) {
        addMemberToSpace(space, user, role, role, null);
    }

    /**
     * Update space name
     */
    public SpaceResponseDto updateSpace(UUID spaceId, UUID userId, String newName) {
        Spaces space = getSpaceByIdAndUser(spaceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Space not found or access denied"));

        // Only OWNER (creator) or ADMIN (via member role logic, implemented later if
        // needed) can update name
        // For now, strict check: if not Space owner, check member role

        boolean isOwner = space.getOwner().getId().equals(userId);
        if (!isOwner) {
            boolean isAdmin = spaceMemberRepository.existsBySpaceIdAndUserIdAndRole(
                    spaceId, userId, app.web.inventory.model.enums.SpaceRole.ADMIN);
            if (!isAdmin) {
                // Check if they are the ORIGINAL owner (which is the case if isOwner is true,
                // but covering bases)
                // If merely a 'MEMBER', deny
                throw new SecurityException("Only Owners and Admins can rename spaces");
            }
        }

        if (!space.getName().equals(newName.trim()) &&
                spaceRepository.existsByOwnerIdAndName(userId, newName.trim())) {
            throw new DuplicateResourceException("Space with name '" + newName + "' already exists");
        }

        String oldName = space.getName();
        space.setName(newName.trim());

        Map<String, Object> details = Map.of(
                "oldName", oldName,
                "newName", newName.trim(),
                "action", "Space name updated");
        auditLogService.logAction(
                userId,
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
    public void deleteSpace(UUID spaceId, UUID userId) {
        Spaces space = getSpaceByIdAndUser(spaceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Space not found or access denied"));

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
                userId,
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
    public SpaceResponseDto getSpaceByIdDto(UUID spaceId, UUID userId) {
        Spaces space = getSpaceByIdAndUser(spaceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Space not found or access denied"));

        return convertToResponseDto(space);
    }

    /**
     * Invite a user to a space
     */
    public void inviteUser(UUID spaceId, UUID initiatorId, InviteMemberRequest request) {
        // 1. Check permissions (Owner or Admin)
        Spaces space = getSpaceByIdAndUser(spaceId, initiatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Space not found or access denied"));

        boolean isOwner = space.getOwner().getId().equals(initiatorId);
        boolean isAdmin = spaceMemberRepository.existsBySpaceIdAndUserIdAndRole(spaceId, initiatorId, SpaceRole.ADMIN);

        if (!isOwner && !isAdmin) {
            throw new SecurityException("Only owners and admins can invite members");
        }

        // 2. Find user to invite
        Users userToInvite = null;
        if (request.getUserId() != null) {
            userToInvite = userService.findById(request.getUserId())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("User with ID " + request.getUserId() + " not found"));
        } else if (request.getEmail() != null) {
            userToInvite = userService.findByEmail(request.getEmail())
                    .orElseThrow(
                            () -> new ResourceNotFoundException(
                                    "User with email " + request.getEmail() + " not found"));
        } else {
            throw new IllegalArgumentException("Must provide user ID or email");
        }

        // 3. Check if already member
        if (spaceMemberRepository.existsBySpaceIdAndUserId(spaceId, userToInvite.getId())) {
            throw new DuplicateResourceException("User is already a member of this space");
        }

        // 4. Add member
        // 4. Add member as PENDING
        addMemberToSpace(space, userToInvite, request.getRole() != null ? request.getRole() : SpaceRole.MEMBER,
                SpaceRole.PENDING, initiatorId);

        // 5. Audit log
        Map<String, Object> details = Map.of(
                "spaceName", space.getName(),
                "invitedUser", userToInvite.getEmail(),
                "role", request.getRole(),
                "action", "Member invited");

        auditLogService.logAction(
                initiatorId,
                "SPACE",
                spaceId,
                "INVITE",
                details,
                getClientIpAddress(),
                getUserAgent(),
                userToInvite.getId(),
                "USER");
    }

    /**
     * Remove a member from a space
     */
    public void removeMember(UUID spaceId, UUID initiatorId, UUID memberId) {
        // 1. Check permissions (Owner)
        Spaces space = getSpaceByIdAndUser(spaceId, initiatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Space not found or access denied"));

        boolean isOwner = space.getOwner().getId().equals(initiatorId);

        // Members typically can only be removed by Owner or Admin.
        // For simplicity, let's say currently ONLY OWNER can remove people OR Admins
        // can remove normal members.

        boolean isAdmin = spaceMemberRepository.existsBySpaceIdAndUserIdAndRole(spaceId, initiatorId, SpaceRole.ADMIN);

        if (!isOwner && !isAdmin) {
            throw new SecurityException("Insufficient permissions to remove members");
        }

        SpaceMember memberToRemove = spaceMemberRepository.findBySpaceIdAndUserId(spaceId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in this space"));

        // Protect Owner from being removed
        if (memberToRemove.getRole() == SpaceRole.OWNER) {
            throw new IllegalArgumentException("Cannot remove the Owner of the space");
        }

        // Admins cannot remove other Admins (optional rule)
        if (isAdmin && memberToRemove.getRole() == SpaceRole.ADMIN) {
            throw new SecurityException("Admins cannot remove other Admins");
        }

        spaceMemberRepository.delete(memberToRemove);

        // Audit log
        Map<String, Object> details = Map.of(
                "spaceName", space.getName(),
                "removedUser", memberToRemove.getUser().getEmail(),
                "action", "Member removed");

        auditLogService.logAction(
                initiatorId,
                "SPACE",
                spaceId,
                "REMOVE_MEMBER",
                details,
                getClientIpAddress(),
                getUserAgent(),
                memberId,
                "USER");
    }

    /**
     * Get members of a space
     */
    public Page<SpaceMemberDto> getSpaceMembers(UUID spaceId, UUID userId, Pageable pageable) {
        if (!hasAccessToSpace(spaceId, userId)) {
            throw new ResourceNotFoundException("Space not found or access denied");
        }

        return spaceMemberRepository.findBySpaceId(spaceId, pageable)
                .map(member -> new SpaceMemberDto(
                        member.getId(),
                        member.getUser().getId(),
                        member.getUser().getName(),
                        member.getUser().getEmail(),
                        member.getRole(),
                        member.getJoinedAt()));
    }

    /**
     * Get pending invites for user
     */
    public List<SpaceInviteDto> getPendingInvites(UUID userId) {
        return spaceMemberRepository.findByUserIdAndRole(userId, SpaceRole.PENDING).stream()
                .map(member -> new SpaceInviteDto(
                        member.getSpace().getId(),
                        member.getSpace().getName(),
                        member.getSpace().getOwner().getName(),
                        SpaceRole.MEMBER,
                        member.getJoinedAt(),
                        member.getInvitedBy()))
                .collect(Collectors.toList());
    }

    /**
     * Accept invite
     */
    public void acceptInvite(UUID spaceId, UUID userId) {
        SpaceMember member = spaceMemberRepository.findBySpaceIdAndUserId(spaceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Invite not found"));

        if (member.getRole() != SpaceRole.PENDING) {
            throw new IllegalStateException("User is already a member or not pending");
        }

        member.setRole(SpaceRole.MEMBER); // Default to MEMBER on accept
        spaceMemberRepository.save(member);
    }

    /**
     * Decline invite
     */
    public void declineInvite(UUID spaceId, UUID userId) {
        SpaceMember member = spaceMemberRepository.findBySpaceIdAndUserId(spaceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Invite not found"));

        if (member.getRole() != SpaceRole.PENDING) {
            throw new IllegalStateException("User is already a member or not pending");
        }

        spaceMemberRepository.delete(member);
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

    public java.util.Optional<Spaces> getSpaceByIdAndUser(UUID spaceId, UUID userId) {
        // Try owner first
        java.util.Optional<Spaces> space = spaceRepository.findByIdAndOwnerId(spaceId, userId);
        if (space.isPresent()) {
            return space;
        }

        // Try member
        return spaceMemberRepository.findBySpaceIdAndUserId(spaceId, userId)
                .map(app.web.inventory.model.SpaceMember::getSpace);
    }

    public boolean hasAccessToSpace(UUID spaceId, UUID userId) {
        // Check if owner
        if (spaceRepository.findByIdAndOwnerId(spaceId, userId).isPresent()) {
            return true;
        }
        // Check if member
        return spaceMemberRepository.existsBySpaceIdAndUserId(spaceId, userId);
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