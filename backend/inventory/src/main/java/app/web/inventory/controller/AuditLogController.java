package app.web.inventory.controller;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import app.web.inventory.config.SecurityUtil;
import app.web.inventory.dto.AuditLogDto;
import app.web.inventory.dto.AuditLogFilterRequest;
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
    public ResponseEntity<?> getAuditLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
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

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", auditLogs.getContent(),
                    "pagination", Map.of(
                            "page", auditLogs.getNumber(),
                            "size", auditLogs.getSize(),
                            "totalElements", auditLogs.getTotalElements(),
                            "totalPages", auditLogs.getTotalPages(),
                            "hasNext", auditLogs.hasNext(),
                            "hasPrevious", auditLogs.hasPrevious())));

        } catch (Exception ex) {
            log.error("Error retrieving audit logs", ex);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }

    /**
     * Get audit log summary statistics
     * GET /api/audit-logs/summary
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getAuditLogSummary() {
        try {
            UUID currentUserId = SecurityUtil.getCurrentUserId();
            var summary = auditLogService.getAuditLogSummary(currentUserId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", summary));

        } catch (Exception ex) {
            log.error("Error retrieving audit log summary", ex);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }

    /**
     * Get recent activity
     * GET /api/audit-logs/recent
     */
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentActivity(
            @RequestParam(defaultValue = "24") int hours) {

        try {
            UUID currentUserId = SecurityUtil.getCurrentUserId();
            var recentActivity = auditLogService.getRecentActivity(currentUserId, hours);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", recentActivity,
                    "message", "Recent activity from last " + hours + " hours"));

        } catch (Exception ex) {
            log.error("Error retrieving recent activity", ex);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }

    /**
     * Get activity trends and analytics
     * GET /api/audit-logs/trends
     */
    @GetMapping("/trends")
    public ResponseEntity<?> getActivityTrends(
            @RequestParam(defaultValue = "30") int days) {

        try {
            UUID currentUserId = SecurityUtil.getCurrentUserId();
            var trends = auditLogService.getActivityTrends(currentUserId, days);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", trends));

        } catch (Exception ex) {
            log.error("Error retrieving activity trends", ex);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }

    /**
     * Get available filter options
     * GET /api/audit-logs/filters
     */
    @GetMapping("/filters")
    public ResponseEntity<?> getFilterOptions() {
        try {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "entityTypes", new String[] { "SPACE", "PRODUCT", "USER" },
                            "operations",
                            new String[] { "CREATE", "UPDATE", "DELETE", "STOCK_ADD", "STOCK_REMOVE", "STOCK_UPDATE" },
                            "sortByOptions", new String[] { "timestamp", "entityType", "operation" },
                            "sortDirections", new String[] { "ASC", "DESC" })));

        } catch (Exception ex) {
            log.error("Error retrieving filter options", ex);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }
}