package app.web.inventory.dto.dashboard;

import java.util.Date;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryTrendsDto {
    private Boolean hasHistoricalData;
    private CurrentSnapshot currentSnapshot;
    private Map<String, Long> dailyActivity;
    private Map<String, Long> operationBreakdown;
    private Long totalActivities;
    private String period;
    private String message;
    private Integer requestedDays;

    public InventoryTrendsDto(boolean hasHistoricalData, CurrentSnapshot snapshot, Map<String, Long> dailyActivity,
            Map<String, Long> operationBreakdown, Long totalActivities, String period, Object message, int days) {
        this.hasHistoricalData = hasHistoricalData;
        this.currentSnapshot = snapshot;
        this.dailyActivity = dailyActivity;
        this.operationBreakdown = operationBreakdown;
        this.totalActivities = totalActivities;
        this.period = period;
        this.message = message.toString();
        this.requestedDays = days;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentSnapshot {
        private Date date;
        private Integer totalProducts;
        private Integer totalSpaces;
        private Double totalValue;
        private Integer lowStockCount;
    }
}