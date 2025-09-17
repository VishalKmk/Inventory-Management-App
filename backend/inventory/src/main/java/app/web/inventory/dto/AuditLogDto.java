package app.web.inventory.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDto {
    private UUID id;
    private String entityType;
    private UUID entityId;
    private String operation;
    private String details;
    private LocalDateTime timestamp;
    private String ipAddress;
    private UUID relatedEntityId;
    private String relatedEntityType;
}