package app.web.inventory.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.web.inventory.dto.space.InviteMemberRequest;
import app.web.inventory.dto.space.SpaceCreationStatusDto;
import app.web.inventory.dto.space.SpaceDto;
import app.web.inventory.dto.space.SpaceInviteDto;
import app.web.inventory.dto.space.SpaceMemberDto;
import app.web.inventory.dto.space.SpaceResponseDto;
import app.web.inventory.exception.DuplicateResourceException;
import app.web.inventory.exception.ResourceNotFoundException;
import app.web.inventory.model.SpaceMember;
import app.web.inventory.model.Spaces;
import app.web.inventory.model.Users;
import app.web.inventory.model.enums.SpaceRole;
import app.web.inventory.repository.SpaceRepository;
import app.web.inventory.util.RequestUtil;
import app.web.inventory.repository.ProductRepository;
import app.web.inventory.repository.SpaceMemberRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class SpaceService {

    private final SpaceRepository spaceRepository;
    private final UserService userService;
    private final AuditLogService auditLogService;
    private final SpaceMemberRepository spaceMemberRepository;
    private final ProductRepository productRepository;
    private final EmailService emailService;

    public SpaceService(SpaceRepository spaceRepository, UserService userService, AuditLogService auditLogService,
            app.web.inventory.repository.SpaceMemberRepository spaceMemberRepository,
            ProductRepository productRepository, EmailService emailService) {
        this.spaceRepository = spaceRepository;
        this.userService = userService;
        this.auditLogService = auditLogService;
        this.spaceMemberRepository = spaceMemberRepository;
        this.productRepository = productRepository;
        this.emailService = emailService;
    }

    /**
     * Create a new space for the given user.
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
        addOwnerToSpace(savedSpace, owner);

        Map<String, Object> details = Map.of(
                "spaceName", savedSpace.getName(),
                "action", "Space created");
        auditLogService.logAction(
                ownerId,
                "SPACE",
                savedSpace.getId(),
                "CREATE",
                details,
                RequestUtil.getClientIpAddress(),
                RequestUtil.getUserAgent(),
                null,
                null);

        return convertToResponseDto(savedSpace);
    }

    @SuppressWarnings("null")
    private void addMemberToSpace(Spaces space, Users user, SpaceRole intendedRole, UUID invitedBy) {
        SpaceMember member = SpaceMember.builder()
                .space(space)
                .user(user)
                .role(SpaceRole.PENDING)
                .intendedRole(intendedRole)
                .invitedBy(invitedBy)
                .build();
        spaceMemberRepository.save(member);
    }

    @SuppressWarnings("null")
    private void addOwnerToSpace(Spaces space, Users owner) {
        SpaceMember member = SpaceMember.builder()
                .space(space)
                .user(owner)
                .role(SpaceRole.OWNER)
                .intendedRole(SpaceRole.OWNER)
                .invitedBy(null)
                .build();
        spaceMemberRepository.save(member);
    }

    /**
     * Update the space name.
     */
    public SpaceResponseDto updateSpace(UUID spaceId, UUID userId, String newName) {
        Spaces space = getSpaceByIdAndUser(spaceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Space not found or access denied"));

        boolean isOwner = space.getOwner().getId().equals(userId);
        if (!isOwner) {
            boolean isAdmin = spaceMemberRepository.existsBySpaceIdAndUserIdAndRole(
                    spaceId, userId, SpaceRole.ADMIN);
            if (!isAdmin) {
                throw new SecurityException("Only Owners and Admins can rename spaces");
            }
        }

        // Use space.getOwner().getId() instead of userId for duplicate check
        if (!space.getName().equals(newName.trim()) &&
                spaceRepository.existsByOwnerIdAndName(space.getOwner().getId(), newName.trim())) {
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
                RequestUtil.getClientIpAddress(),
                RequestUtil.getUserAgent(),
                null,
                null);

        Spaces savedSpace = spaceRepository.save(space);
        return convertToResponseDto(savedSpace);
    }

    /**
     * Delete a space.
     */
    public void deleteSpace(UUID spaceId, UUID userId) {
        if (spaceId == null || userId == null) {
            throw new IllegalArgumentException("Space ID and User ID cannot be null");
        }

        Spaces space = getSpaceByIdAndUser(spaceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Space not found or access denied"));

        if (!space.getOwner().getId().equals(userId)) {
            throw new SecurityException("Only the owner can delete a space");
        }

        String spaceName = space.getName();
        String ownerName = space.getOwner().getName();

        // Fetch members BEFORE deleting to notify them
        List<SpaceMember> members = spaceMemberRepository
                .findBySpaceId(spaceId, Pageable.unpaged())
                .stream()
                .filter(m -> m.getRole() != SpaceRole.OWNER)
                .collect(Collectors.toList());

        long productCount = spaceRepository.countProductsInSpace(spaceId);

        // Delete products first
        productRepository.deleteBySpaceId(spaceId);

        // Delete members
        spaceMemberRepository.deleteBySpaceId(spaceId);

        // Delete space
        spaceRepository.delete(space);

        // Notify members after deletion
        for (SpaceMember member : members) {
            try {
                emailService.sendSpaceDeletionNoticeToMember(
                        member.getUser().getEmail(),
                        spaceName,
                        ownerName);
            } catch (Exception e) {
                log.warn("Failed to send deletion notice to member {}",
                        member.getUser().getEmail(), e);
            }
        }

        // Audit log
        Map<String, Object> details = Map.of(
                "spaceName", spaceName,
                "productsDeleted", productCount,
                "membersRemoved", members.size(),
                "action", "Space deleted");

        auditLogService.logAction(
                userId, "SPACE", spaceId, "DELETE", details,
                RequestUtil.getClientIpAddress(), RequestUtil.getUserAgent(), null, null);
    }

    /**
     * Get a specific space by ID as a DTO.
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

        // 4. Add member as PENDING
        SpaceRole intendedRole = request.getRole() != null ? request.getRole() : SpaceRole.MEMBER;

        // Only the Owner can grant OWNER or ADMIN-level roles; Admins are capped at
        // inviting MEMBER/VIEWER to prevent privilege escalation
        if (!isOwner && (intendedRole == SpaceRole.OWNER || intendedRole == SpaceRole.ADMIN)) {
            throw new SecurityException("Only the space owner can invite Admins or Owners");
        }
        if (intendedRole == SpaceRole.PENDING) {
            throw new IllegalArgumentException("Cannot invite a member with role PENDING");
        }

        addMemberToSpace(space, userToInvite, intendedRole, initiatorId);

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
                RequestUtil.getClientIpAddress(),
                RequestUtil.getUserAgent(),
                userToInvite.getId(),
                "USER");
    }

    /**
     * Remove a member from a space
     */
    public void removeMember(UUID spaceId, UUID initiatorId, UUID memberId) {

        // Null check
        if (spaceId == null || initiatorId == null || memberId == null) {
            throw new IllegalArgumentException("Space ID, initiator ID and member ID cannot be null");
        }
        // Self-removal check first
        if (initiatorId.equals(memberId)) {
            throw new IllegalArgumentException("Cannot remove yourself. Use the leave space feature instead.");
        }

        // Single permission check based on role (Owner or Admin can remove, but Admins
        // cannot remove other Admins)
        SpaceRole initiatorRole = getUserRoleInSpace(spaceId, initiatorId);
        if (initiatorRole != SpaceRole.OWNER && initiatorRole != SpaceRole.ADMIN) {
            throw new SecurityException("Insufficient permissions to remove members");
        }

        Spaces space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Space not found"));

        SpaceMember memberToRemove = spaceMemberRepository.findBySpaceIdAndUserId(spaceId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in this space"));

        if (memberToRemove.getRole() == SpaceRole.OWNER) {
            throw new IllegalArgumentException("Cannot remove the Owner of the space");
        }

        // Admins cannot remove other Admins - only Owner can
        if (initiatorRole == SpaceRole.ADMIN && memberToRemove.getRole() == SpaceRole.ADMIN) {
            throw new SecurityException("Admins cannot remove other Admins");
        }

        spaceMemberRepository.delete(memberToRemove);

        Map<String, Object> details = Map.of(
                "spaceName", space.getName(),
                "removedUser", memberToRemove.getUser().getEmail(),
                "removedUserRole", memberToRemove.getRole().name(),
                "action", "Member removed");

        auditLogService.logAction(
                initiatorId,
                "SPACE",
                spaceId,
                "REMOVE_MEMBER",
                details,
                RequestUtil.getClientIpAddress(),
                RequestUtil.getUserAgent(),
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
                        member.getIntendedRole() != null
                                ? member.getIntendedRole()
                                : SpaceRole.MEMBER, // fallback if somehow null
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

        SpaceRole roleToAssign = member.getIntendedRole() != null
                ? member.getIntendedRole()
                : SpaceRole.MEMBER;

        member.setRole(roleToAssign);
        member.setIntendedRole(null);
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
     * Get only spaces owned by user
     */
    public List<SpaceDto> getOwnedSpacesWithProductCount(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        List<Object[]> results = spaceRepository.findSpacesWithProductCount(userId);

        return results.stream()
                .map(result -> {
                    Spaces space = (Spaces) result[0];
                    long productCount = result[1] instanceof Number
                            ? ((Number) result[1]).longValue()
                            : 0L;

                    return new SpaceDto(
                            space.getId(),
                            space.getName(),
                            space.getOwner().getId(),
                            space.getOwner().getName(),
                            productCount,
                            "OWNER");
                })
                .collect(Collectors.toList());
    }

    /**
     * Get only spaces shared with user (member/admin of, not owned)
     */
    public List<SpaceDto> getSharedSpacesWithProductCount(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        return spaceMemberRepository.findActiveSpacesByUserId(userId)
                .stream()
                .filter(sm -> !sm.getSpace().getOwner().getId().equals(userId)) // exclude owned
                .map(sm -> {
                    long productCount = spaceRepository.countProductsInSpace(sm.getSpace().getId());

                    return new SpaceDto(
                            sm.getSpace().getId(),
                            sm.getSpace().getName(),
                            sm.getSpace().getOwner().getId(),
                            sm.getSpace().getOwner().getName(),
                            productCount,
                            sm.getRole().name());
                })
                .collect(Collectors.toList());
    }

    // =============================================================================
    // Internal/Utility Methods
    // =============================================================================

    // Get all spaces where user is owner or member (for listing)
    public List<Spaces> getAccessibleSpaces(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        // Get owned spaces
        List<Spaces> ownedSpaces = spaceRepository.findByOwnerId(userId);

        // Get member spaces (excluding PENDING)
        List<Spaces> memberSpaces = spaceMemberRepository
                .findActiveSpacesByUserId(userId)
                .stream()
                .map(member -> member.getSpace())
                .filter(s -> !s.getOwner().getId().equals(userId)) // avoid duplicates
                .collect(Collectors.toList());

        List<Spaces> allSpaces = new ArrayList<>(ownedSpaces);
        allSpaces.addAll(memberSpaces);
        return allSpaces;
    }

    // Get space by ID without user check (used internally when we already have
    // space object)
    public Spaces getSpaceById(UUID spaceId) {
        if (spaceId == null) {
            throw new IllegalArgumentException("Space ID cannot be null");
        }
        return spaceRepository.findById(spaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Space not found"));
    }

    public List<Spaces> getSpacesByOwner(UUID ownerId) {
        if (ownerId == null)
            throw new IllegalArgumentException("Owner ID cannot be null");
        return spaceRepository.findByOwnerId(ownerId);
    }

    public Optional<Spaces> getSpaceByIdAndUser(UUID spaceId, UUID userId) {

        if (spaceId == null) {
            throw new IllegalArgumentException("Space ID cannot be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        // Try owner first
        Optional<Spaces> space = spaceRepository.findByIdAndOwnerId(spaceId, userId);
        if (space.isPresent()) {
            return space;
        }

        // Try member
        return spaceMemberRepository.findBySpaceIdAndUserId(spaceId, userId)
                .map(SpaceMember::getSpace);
    }

    // Check if user has access to space (owner or member)
    public boolean hasAccessToSpace(UUID spaceId, UUID userId) {
        if (spaceRepository.findByIdAndOwnerId(spaceId, userId).isPresent()) {
            return true;
        }
        // Only active members (not PENDING) have access
        return spaceMemberRepository.findBySpaceIdAndUserId(spaceId, userId)
                .map(member -> member.getRole() != SpaceRole.PENDING)
                .orElse(false);
    }

    // Get the user's role in a space (useful for write permission checks)
    public SpaceRole getUserRoleInSpace(UUID spaceId, UUID userId) {
        if (spaceId == null || userId == null) {
            throw new IllegalArgumentException("Space ID and User ID cannot be null");
        }

        Spaces space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Space not found"));

        if (space.getOwner().getId().equals(userId)) {
            return SpaceRole.OWNER;
        }

        return spaceMemberRepository.findBySpaceIdAndUserId(spaceId, userId)
                .map(SpaceMember::getRole)
                .orElseThrow(() -> new SecurityException("Access denied"));
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
        long productCount = spaceRepository.countProductsInSpace(space.getId());

        return new SpaceResponseDto(
                space.getId(),
                space.getName(),
                space.getOwner().getId(),
                space.getOwner().getName(),
                productCount,
                space.getCreatedAt(),
                space.getUpdatedAt());
    }
}