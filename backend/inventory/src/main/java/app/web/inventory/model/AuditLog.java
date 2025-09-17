package app.web.inventory.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 100)
    private String entityType; // "SPACE", "PRODUCT", "USER"

    @Column
    private UUID entityId;

    @Column(nullable = false, length = 50)
    private String operation; // "CREATE", "UPDATE", "DELETE", "STOCK_ADD", "STOCK_REMOVE"

    @Column(length = 1000)
    private String details; // JSON string with change details

    @Column(length = 255)
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column
    private UUID relatedEntityId; // For hierarchical operations (e.g., spaceId for product operations)

    @Column(length = 50)
    private String relatedEntityType;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
}