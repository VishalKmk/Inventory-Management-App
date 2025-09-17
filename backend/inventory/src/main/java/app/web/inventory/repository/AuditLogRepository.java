package app.web.inventory.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import app.web.inventory.model.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    // Find audit logs by user
    Page<AuditLog> findByUserIdOrderByTimestampDesc(UUID userId, Pageable pageable);

    // Find audit logs by user and entity type
    Page<AuditLog> findByUserIdAndEntityTypeOrderByTimestampDesc(
            UUID userId, String entityType, Pageable pageable);

    // Find audit logs for a specific entity
    Page<AuditLog> findByUserIdAndEntityIdOrderByTimestampDesc(
            UUID userId, UUID entityId, Pageable pageable);

    // Find audit logs by operation type
    Page<AuditLog> findByUserIdAndOperationOrderByTimestampDesc(
            UUID userId, String operation, Pageable pageable);

    // Find audit logs within date range
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId AND a.timestamp BETWEEN :startDate AND :endDate ORDER BY a.timestamp DESC")
    Page<AuditLog> findByUserIdAndTimestampBetweenOrderByTimestampDesc(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    // Count operations by type for analytics
    @Query("SELECT a.operation, COUNT(a) FROM AuditLog a WHERE a.userId = :userId GROUP BY a.operation")
    List<Object[]> countOperationsByUser(@Param("userId") UUID userId);

    // Count operations by entity type
    @Query("SELECT a.entityType, COUNT(a) FROM AuditLog a WHERE a.userId = :userId GROUP BY a.entityType")
    List<Object[]> countEntitiesByUser(@Param("userId") UUID userId);

    // Get recent activity summary
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId AND a.timestamp >= :since ORDER BY a.timestamp DESC")
    List<AuditLog> findRecentActivity(@Param("userId") UUID userId, @Param("since") LocalDateTime since);
}