package app.web.inventory.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogFilterRequest {
    private String entityType;
    private String operation;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private UUID entityId;
    private int page = 0;
    private int size = 20;
    private String sortBy = "timestamp";
    private String sortDirection = "DESC";
}