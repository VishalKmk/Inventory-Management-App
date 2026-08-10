package app.web.inventory.dashboard.dto;

import java.util.List;
import java.util.UUID;

import app.web.inventory.audit.dto.AuditLogDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpaceDashboardDto {
    private UUID spaceId;
    private String spaceName;
    private String currentUserRole;
    private long memberCount;
    private DashboardOverviewDto overview;
    private List<TopProductsDto.ProductSummary> lowStockProducts;
    private List<AuditLogDto> recentActivity;
    private ActivityTrendsDto trends;
}
