package app.web.inventory.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.web.inventory.dto.api.ApiResponse;
import app.web.inventory.dto.space.InviteMemberRequest;
import app.web.inventory.dto.space.SpaceMemberDto;
import app.web.inventory.service.SpaceService;
import app.web.inventory.util.SecurityUtil;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/spaces/{spaceId}/members")
public class SpaceMemberController {

    private final SpaceService spaceService;

    public SpaceMemberController(SpaceService spaceService) {
        this.spaceService = spaceService;
    }

    /**
     * Invite a new member
     * POST /api/spaces/{spaceId}/members/invite
     */
    @PostMapping("/invite")
    public ResponseEntity<ApiResponse<Void>> inviteMember(
            @PathVariable UUID spaceId,
            @Valid @RequestBody InviteMemberRequest request) {

        UUID currentUserId = SecurityUtil.getCurrentUserId();
        spaceService.inviteUser(spaceId, currentUserId, request);

        return ResponseEntity.ok(ApiResponse.success("User invited successfully", null));
    }

    /**
     * Get all members
     * GET /api/spaces/{spaceId}/members
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<SpaceMemberDto>>> getMembers(
            @PathVariable UUID spaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID currentUserId = SecurityUtil.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("joinedAt").descending());

        Page<SpaceMemberDto> members = spaceService.getSpaceMembers(spaceId, currentUserId, pageable);

        return ResponseEntity.ok(ApiResponse.success(members));
    }

    /**
     * Remove a member
     * DELETE /api/spaces/{spaceId}/members/{userId}
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable UUID spaceId,
            @PathVariable UUID userId) {

        UUID currentUserId = SecurityUtil.getCurrentUserId();
        spaceService.removeMember(spaceId, currentUserId, userId);

        return ResponseEntity.ok(ApiResponse.success("Member removed successfully", null));
    }

    // ==========================================
    // Pending Invites Endpoints
    // ==========================================

    @PostMapping("/accept")
    public ResponseEntity<ApiResponse<Void>> acceptInvite(
            @PathVariable UUID spaceId) {

        UUID currentUserId = SecurityUtil.getCurrentUserId();
        spaceService.acceptInvite(spaceId, currentUserId);

        return ResponseEntity.ok(ApiResponse.success("Invite accepted", null));
    }

    @PostMapping("/decline")
    public ResponseEntity<ApiResponse<Void>> declineInvite(
            @PathVariable UUID spaceId) {

        UUID currentUserId = SecurityUtil.getCurrentUserId();
        spaceService.declineInvite(spaceId, currentUserId);

        return ResponseEntity.ok(ApiResponse.success("Invite declined", null));
    }
}
