package app.web.inventory.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LowStockAlertsDto {
    private Integer totalAlerts;
    private Map<String, List<AlertInfo>> alertsBySpace;
    private Map<String, Long> severityBreakdown;
    private Boolean hasAlerts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlertInfo {
        private UUID productId;
        private String productName;
        private String spaceName;
        private Integer currentStock;
        private Integer minimumQuantity;
        private String severity;
        private Integer stockDifference;
    }
}