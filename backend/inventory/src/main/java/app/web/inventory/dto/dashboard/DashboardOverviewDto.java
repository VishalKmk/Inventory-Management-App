package app.web.inventory.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewDto {
    private Integer totalSpaces;
    private Integer maxSpaces;
    private Double spaceUtilization;
    private Integer totalProducts;
    private Double totalValue;
    private Integer lowStockCount;
    private Map<String, Integer> stockStatus;
    private Double averageProductsPerSpace;
}