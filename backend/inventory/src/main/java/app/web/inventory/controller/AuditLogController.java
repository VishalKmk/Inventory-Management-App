package app.web.inventory.controller;

import java.time.LocalDateTime;
import java.util.UUID;

import app.web.inventory.dto.api.ApiResponse;
import app.web.inventory.dto.audit.*;
import app.web.inventory.dto.dashboard.ActivityTrendsDto;
import app.web.inventory.dto.pagination.PaginationDto;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import app.web.inventory.config.SecurityUtil;
import app.web.inventory.service.AuditLogService;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/audit-logs")
@Slf4j
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * Get audit logs with optional filtering
     * GET /api/audit-logs
     */
    @GetMapping
    public ResponseEntity<ApiResponse<AuditLogListDto>> getAuditLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "${app.pagination.default-page}") int page,
            @RequestParam(defaultValue = "${app.pagination.default-size}") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        try {
            UUID currentUserId = SecurityUtil.getCurrentUserId();

            AuditLogFilterRequest request = new AuditLogFilterRequest();
            request.setEntityType(entityType);
            request.setOperation(operation);
            request.setEntityId(entityId);
            request.setStartDate(startDate);
            request.setEndDate(endDate);
            request.setPage(page);
            request.setSize(size);
            request.setSortBy(sortBy);
            request.setSortDirection(sortDirection);

            Page<AuditLogDto> auditLogs = auditLogService.getAuditLogs(currentUserId, request);

            PaginationDto pagination = new PaginationDto(
                    auditLogs.getNumber(),
                    auditLogs.getSize(),
                    auditLogs.getTotalElements(),
                    auditLogs.getTotalPages(),
                    auditLogs.hasNext(),
                    auditLogs.hasPrevious());

            AuditLogListDto response = new AuditLogListDto(auditLogs.getContent(), pagination);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception ex) {
            log.error("Error retrieving audit logs", ex);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Internal server error"));
        }
    }

    /**
     * Get audit log summary statistics
     * GET /api/audit-logs/summary
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AuditLogSummaryDto>> getAuditLogSummary() {
        try {
            UUID currentUserId = SecurityUtil.getCurrentUserId();
            AuditLogSummaryDto summary = auditLogService.getAuditLogSummary(currentUserId);

            return ResponseEntity.ok(ApiResponse.success(summary));

        } catch (Exception ex) {
            log.error("Error retrieving audit log summary", ex);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Internal server error"));
        }
    }

    /**
     * Get recent activity
     * GET /api/audit-logs/recent
     */
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<java.util.List<AuditLogDto>>> getRecentActivity(
            @RequestParam(defaultValue = "24") int hours) {

        try {
            UUID currentUserId = SecurityUtil.getCurrentUserId();
            java.util.List<AuditLogDto> recentActivity = auditLogService.getRecentActivity(currentUserId, hours);

            return ResponseEntity.ok(ApiResponse.success(
                    "Recent activity from last " + hours + " hours",
                    recentActivity));

        } catch (Exception ex) {
            log.error("Error retrieving recent activity", ex);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Internal server error"));
        }
    }

    /**
     * Get activity trends and analytics
     * GET /api/audit-logs/trends
     */
    @GetMapping("/trends")
    public ResponseEntity<ApiResponse<ActivityTrendsDto>> getActivityTrends(
            @RequestParam(defaultValue = "30") int days) {

        try {
            UUID currentUserId = SecurityUtil.getCurrentUserId();
            ActivityTrendsDto trends = (ActivityTrendsDto) auditLogService.getActivityTrends(currentUserId, days);

            return ResponseEntity.ok(ApiResponse.success(trends));

        } catch (Exception ex) {
            log.error("Error retrieving activity trends", ex);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Internal server error"));
        }
    }

    /**
     * Get available filter options
     * GET /api/audit-logs/filters
     */
    @GetMapping("/filters")
    public ResponseEntity<ApiResponse<FilterOptionsDto>> getFilterOptions() {
        try {
            FilterOptionsDto filterOptions = new FilterOptionsDto(
                    new String[] { "SPACE", "PRODUCT", "USER" },
                    new String[] { "CREATE", "UPDATE", "DELETE", "STOCK_ADD", "STOCK_REMOVE", "STOCK_UPDATE" },
                    new String[] { "timestamp", "entityType", "operation" },
                    new String[] { "ASC", "DESC" });

            return ResponseEntity.ok(ApiResponse.success(filterOptions));

        } catch (Exception ex) {
            log.error("Error retrieving filter options", ex);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Internal server error"));
        }
    }
}