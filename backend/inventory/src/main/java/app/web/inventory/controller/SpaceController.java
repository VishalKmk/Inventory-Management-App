package app.web.inventory.controller;

import java.util.List;
import java.util.UUID;

import app.web.inventory.dto.api.ApiResponse;
import app.web.inventory.dto.audit.AuditLogFilterRequest;
import app.web.inventory.dto.audit.AuditLogDto;
import app.web.inventory.dto.audit.AuditLogListDto;
import app.web.inventory.dto.dashboard.SpaceDashboardDto;
import app.web.inventory.dto.pagination.PaginationDto;
import app.web.inventory.service.AuditLogService;
import app.web.inventory.service.DashboardService;
import app.web.inventory.dto.space.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

import app.web.inventory.service.SpaceService;
import app.web.inventory.util.SecurityUtil;

@RestController
@RequestMapping("/api/spaces")
public class SpaceController {

    private final SpaceService spaceService;
    private final DashboardService dashboardService;
    private final AuditLogService auditLogService;

    public SpaceController(SpaceService spaceService, DashboardService dashboardService, AuditLogService auditLogService) {
        this.spaceService = spaceService;
        this.dashboardService = dashboardService;
        this.auditLogService = auditLogService;
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

    @GetMapping("/invites")
    public ResponseEntity<ApiResponse<List<SpaceInviteDto>>> getPendingInvites() {
        return ResponseEntity.ok(ApiResponse.success(spaceService.getPendingInvites(SecurityUtil.getCurrentUserId())));
    }

    /**
     * Get a dashboard scoped to one accessible space.
     * GET /api/spaces/{spaceId}/dashboard
     */
    @GetMapping("/{spaceId}/dashboard")
    public ResponseEntity<ApiResponse<SpaceDashboardDto>> getSpaceDashboard(
            @PathVariable UUID spaceId,
            @RequestParam(defaultValue = "30") int days) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getSpaceDashboard(currentUserId, spaceId, days)));
    }

    /**
     * Get audit logs scoped to one accessible space, with pagination and filters.
     * GET /api/spaces/{spaceId}/audit-logs
     */
    @GetMapping("/{spaceId}/audit-logs")
    public ResponseEntity<ApiResponse<AuditLogListDto>> getSpaceAuditLogs(
            @PathVariable UUID spaceId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();
        if (!spaceService.hasAccessToSpace(spaceId, currentUserId)) {
            return ResponseEntity.status(404).body(ApiResponse.error("Space not found or access denied"));
        }

        AuditLogFilterRequest request = new AuditLogFilterRequest(entityType, operation, startDate, endDate,
                null, page, size, sortBy, sortDirection);
        Page<AuditLogDto> logs = auditLogService.getSpaceAuditLogs(spaceId, request);
        PaginationDto pagination = new PaginationDto(logs.getNumber(), logs.getSize(), logs.getTotalElements(),
                logs.getTotalPages(), logs.hasNext(), logs.hasPrevious());
        return ResponseEntity.ok(ApiResponse.success(new AuditLogListDto(logs.getContent(), pagination)));
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
