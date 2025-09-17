package app.web.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogSummaryDto {
    private long totalLogs;
    private long spaceLogs;
    private long productLogs;
    private long createOperations;
    private long updateOperations;
    private long deleteOperations;
    private long stockOperations;
}