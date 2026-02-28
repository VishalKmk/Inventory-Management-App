package app.web.inventory.controller;

import java.util.List;
import java.util.UUID;

import app.web.inventory.dto.api.ApiResponse;
import app.web.inventory.dto.space.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import app.web.inventory.service.SpaceService;
import app.web.inventory.util.SecurityUtil;

@RestController
@RequestMapping("/api/spaces")
public class SpaceController {

    private final SpaceService spaceService;

    public SpaceController(SpaceService spaceService) {
        this.spaceService = spaceService;
    }

    /**
     * Create a new space
     * POST /api/spaces
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SpaceResponseDto>> createSpace(
            @Valid @RequestBody CreateSpaceRequest request) {

        UUID currentUserId = SecurityUtil.getCurrentUserId();
        SpaceResponseDto space = spaceService.createSpace(currentUserId, request.getName());

        return ResponseEntity.status(201)
                .body(ApiResponse.success("Space created successfully", space));
    }

    /**
     * Get spaces owned by current user
     * GET /api/spaces/owned
     */
    @GetMapping("/owned")
    public ResponseEntity<ApiResponse<List<SpaceDto>>> getOwnedSpaces() {
        UUID currentUserId = SecurityUtil.getCurrentUserId();
        List<SpaceDto> spaces = spaceService.getOwnedSpacesWithProductCount(currentUserId);

        return ResponseEntity.ok(ApiResponse.success(spaces));
    }

    /**
     * Get spaces shared with current user (member/admin of)
     * GET /api/spaces/shared
     */
    @GetMapping("/shared")
    public ResponseEntity<ApiResponse<List<SpaceDto>>> getSharedSpaces() {
        UUID currentUserId = SecurityUtil.getCurrentUserId();
        List<SpaceDto> spaces = spaceService.getSharedSpacesWithProductCount(currentUserId);

        return ResponseEntity.ok(ApiResponse.success(spaces));
    }

    /**
     * Get a specific space by ID
     * GET /api/spaces/{spaceId}
     */
    @GetMapping("/{spaceId}")
    public ResponseEntity<ApiResponse<SpaceResponseDto>> getSpaceById(@PathVariable UUID spaceId) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();
        SpaceResponseDto space = spaceService.getSpaceByIdDto(spaceId, currentUserId);

        return ResponseEntity.ok(ApiResponse.success(space));
    }

    /**
     * Get space creation status
     * GET /api/spaces/creation-status
     */
    @GetMapping("/creation-status")
    public ResponseEntity<ApiResponse<SpaceCreationStatusDto>> getCreationStatus() {
        UUID currentUserId = SecurityUtil.getCurrentUserId();
        SpaceCreationStatusDto status = spaceService.getCreationStatus(currentUserId);

        return ResponseEntity.ok(ApiResponse.success(status));
    }

    /**
     * Update space name
     * PUT /api/spaces/{spaceId}
     */
    @PutMapping("/{spaceId}")
    public ResponseEntity<ApiResponse<SpaceResponseDto>> updateSpaceById(
            @PathVariable UUID spaceId,
            @Valid @RequestBody UpdateSpaceRequest request) {

        UUID currentUserId = SecurityUtil.getCurrentUserId();
        SpaceResponseDto updatedSpace = spaceService.updateSpace(spaceId, currentUserId, request.getName());

        return ResponseEntity.ok(ApiResponse.success("Space updated successfully", updatedSpace));
    }

    /**
     * Delete a space
     * DELETE /api/spaces/{spaceId}
     */
    @DeleteMapping("/{spaceId}")
    public ResponseEntity<ApiResponse<Void>> deleteSpace(@PathVariable UUID spaceId) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();
        spaceService.deleteSpace(spaceId, currentUserId);

        return ResponseEntity.ok(ApiResponse.success("Space deleted successfully", null));
    }
}