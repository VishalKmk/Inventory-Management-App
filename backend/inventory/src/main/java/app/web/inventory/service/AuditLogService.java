package app.web.inventory.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import app.web.inventory.dto.audit.AuditLogDto;
import app.web.inventory.dto.audit.AuditLogFilterRequest;
import app.web.inventory.dto.audit.AuditLogSummaryDto;
import app.web.inventory.dto.dashboard.ActivityTrendsDto;
import app.web.inventory.model.AuditLog;
import app.web.inventory.repository.AuditLogRepository;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Create an audit log entry
     */
    public void logAction(UUID userId, String entityType, UUID entityId, String operation,
            Object changeDetails, String ipAddress, String userAgent,
            UUID relatedEntityId, String relatedEntityType) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(userId);
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLog.setOperation(operation);
            auditLog.setIpAddress(ipAddress);
            auditLog.setUserAgent(userAgent);
            auditLog.setRelatedEntityId(relatedEntityId);
            auditLog.setRelatedEntityType(relatedEntityType);

            if (changeDetails != null) {
                auditLog.setDetails(objectMapper.writeValueAsString(changeDetails));
            }

            auditLogRepository.save(auditLog);
            log.debug("Audit log created for user {} - {} {} {}", userId, operation, entityType, entityId);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize audit log details", e);
            // Save without details rather than failing completely
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(userId);
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLog.setOperation(operation);
            auditLog.setIpAddress(ipAddress);
            auditLog.setUserAgent(userAgent);
            auditLog.setDetails("Error serializing details");
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to create audit log", e);
            // Don't throw exception to avoid disrupting main business logic
        }
    }

    /**
     * Simplified method for basic logging
     */
    public void logAction(UUID userId, String entityType, UUID entityId, String operation, Object changeDetails) {
        logAction(userId, entityType, entityId, operation, changeDetails, null, null, null, null);
    }

    /**
     * Get audit logs with filters and pagination
     */
    @SuppressWarnings("null")
    public Page<AuditLogDto> getAuditLogs(UUID userId, AuditLogFilterRequest request) {
        Sort sort = Sort.by(Sort.Direction.fromString(request.getSortDirection()), request.getSortBy());
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        Page<AuditLog> auditLogs;

        // Apply filters
        if (request.getStartDate() != null && request.getEndDate() != null) {
            auditLogs = auditLogRepository.findByUserIdAndTimestampBetweenOrderByTimestampDesc(
                    userId, request.getStartDate(), request.getEndDate(), pageable);
        } else if (request.getEntityType() != null && !request.getEntityType().isEmpty()) {
            auditLogs = auditLogRepository.findByUserIdAndEntityTypeOrderByTimestampDesc(
                    userId, request.getEntityType().toUpperCase(), pageable);
        } else if (request.getEntityId() != null) {
            auditLogs = auditLogRepository.findByUserIdAndEntityIdOrderByTimestampDesc(
                    userId, request.getEntityId(), pageable);
        } else if (request.getOperation() != null && !request.getOperation().isEmpty()) {
            auditLogs = auditLogRepository.findByUserIdAndOperationOrderByTimestampDesc(
                    userId, request.getOperation().toUpperCase(), pageable);
        } else {
            auditLogs = auditLogRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
        }

        return auditLogs.map(this::convertToDto);
    }

    /**
     * Get audit log summary statistics
     */
    public AuditLogSummaryDto getAuditLogSummary(UUID userId) {
        List<Object[]> operationCounts = auditLogRepository.countOperationsByUser(userId);
        List<Object[]> entityCounts = auditLogRepository.countEntitiesByUser(userId);

        AuditLogSummaryDto summary = new AuditLogSummaryDto();

        // Count operations
        Map<String, Long> opMap = operationCounts.stream()
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> (Long) arr[1]));

        summary.setCreateOperations(opMap.getOrDefault("CREATE", 0L));
        summary.setUpdateOperations(opMap.getOrDefault("UPDATE", 0L));
        summary.setDeleteOperations(opMap.getOrDefault("DELETE", 0L));
        summary.setStockOperations(
                opMap.getOrDefault("STOCK_ADD", 0L) +
                        opMap.getOrDefault("STOCK_REMOVE", 0L) +
                        opMap.getOrDefault("STOCK_UPDATE", 0L));

        // Count entities
        Map<String, Long> entityMap = entityCounts.stream()
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> (Long) arr[1]));

        summary.setSpaceLogs(entityMap.getOrDefault("SPACE", 0L));
        summary.setProductLogs(entityMap.getOrDefault("PRODUCT", 0L));
        summary.setTotalLogs(summary.getSpaceLogs() + summary.getProductLogs());

        return summary;
    }

    /**
     * Get recent activity for dashboard
     */
    public List<AuditLogDto> getRecentActivity(UUID userId, int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        List<AuditLog> recentLogs = auditLogRepository.findRecentActivity(userId, since);

        return recentLogs.stream()
                .limit(50) // Limit to prevent excessive data
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get activity trends for analytics
     */
    public ActivityTrendsDto getActivityTrends(UUID userId, int days) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (days <= 0 || days > 365) {
            throw new IllegalArgumentException("Days must be between 1 and 365");
        }

        // Single timestamp reference - avoids microsecond drift
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);

        // All counting done in DB - no rows loaded into memory
        @SuppressWarnings("null")
        Map<String, Long> dailyActivity = auditLogRepository
                .countDailyActivityByUser(userId, startDate, endDate)
                .stream()
                .collect(Collectors.toMap(
                        row -> row[0].toString(),
                        row -> (Long) row[1]));

        @SuppressWarnings("null")
        Map<String, Long> operationBreakdown = auditLogRepository
                .countOperationBreakdownByUser(userId, startDate, endDate)
                .stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]));

        @SuppressWarnings("null")
        long totalActivities = auditLogRepository
                .countByUserIdAndTimestampBetween(userId, startDate, endDate);

        return new ActivityTrendsDto(
                dailyActivity,
                operationBreakdown,
                totalActivities,
                days + " days");
    }

    /**
     * Convert AuditLog entity to DTO
     */
    private AuditLogDto convertToDto(AuditLog auditLog) {
        return new AuditLogDto(
                auditLog.getId(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getOperation(),
                auditLog.getDetails(),
                auditLog.getTimestamp(),
                auditLog.getIpAddress(),
                auditLog.getRelatedEntityId(),
                auditLog.getRelatedEntityType());
    }
}