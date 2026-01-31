package app.web.inventory.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopProductsDto {
    private Boolean hasData;
    private List<ProductSummary> topProducts;
    private String sortedBy;
    private Integer limit;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSummary {
        private UUID productId;
        private String name;
        private String spaceName;
        private Double price;
        private Integer currentStock;
        private Double totalValue;
        private Boolean isLowStock;
    }
}