package app.web.inventory.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpaceMetricsDto {
    private Boolean hasData;
    private List<SpaceMetric> spaceMetrics;
    private SummaryDto summary;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpaceMetric {
        private UUID spaceId;
        private String spaceName;
        private Long productCount;
        private Double totalValue;
        private Long lowStockCount;
        private Double healthScore;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryDto {
        private Integer totalSpaces;
        private Double totalValue;
        private Long totalProducts;
        private Double averageValuePerSpace;
    }
}