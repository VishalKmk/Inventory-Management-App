package app.web.inventory.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryInsightsDto {
    private Boolean hasData;
    private PriceAnalysisDto priceAnalysis;
    private StockAnalysisDto stockAnalysis;
    private Map<String, Double> valueBySpace;
    private Map<String, Long> productCountBySpace;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceAnalysisDto {
        private Double minimum;
        private Double maximum;
        private Double average;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockAnalysisDto {
        private Integer minimum;
        private Integer maximum;
        private Double average;
        private Long total;
    }
}