package app.web.inventory.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import app.web.inventory.dto.audit.AuditLogDto;
import app.web.inventory.dto.dashboard.DashboardOverviewDto;
import app.web.inventory.dto.dashboard.InventoryInsightsDto;
import app.web.inventory.dto.dashboard.InventoryInsightsDto.PriceAnalysisDto;
import app.web.inventory.dto.dashboard.InventoryInsightsDto.StockAnalysisDto;
import app.web.inventory.dto.dashboard.InventoryTrendsDto;
import app.web.inventory.dto.dashboard.InventoryTrendsDto.CurrentSnapshot;
import app.web.inventory.dto.dashboard.LowStockAlertsDto;
import app.web.inventory.dto.dashboard.LowStockAlertsDto.AlertInfo;
import app.web.inventory.dto.dashboard.RecentActivityDto;
import app.web.inventory.dto.dashboard.RecentActivityDto.ActivityItem;
import app.web.inventory.dto.dashboard.SpaceMetricsDto;
import app.web.inventory.dto.dashboard.SpaceMetricsDto.SpaceMetric;
import app.web.inventory.dto.dashboard.SpaceMetricsDto.SummaryDto;
import app.web.inventory.dto.dashboard.TopProductsDto;
import app.web.inventory.dto.dashboard.TopProductsDto.ProductSummary;
import app.web.inventory.dto.space.SpaceDto;
import app.web.inventory.model.Products;
import app.web.inventory.model.Spaces;

@Service
public class DashboardService {

    private final ProductService productService;
    private final SpaceService spaceService;
    private final AuditLogService auditLogService;

    public DashboardService(ProductService productService, SpaceService spaceService,
            AuditLogService auditLogService) {
        this.productService = productService;
        this.spaceService = spaceService;
        this.auditLogService = auditLogService;
    }

    /**
     * Get comprehensive dashboard overview
     */
    public DashboardOverviewDto getDashboardOverview(UUID userId) {
        // Get basic counts
        List<Spaces> spaces = spaceService.getSpacesByOwner(userId);
        List<Products> products = productService.getProductsByOwner(userId);
        List<Products> lowStockProducts = productService.getLowStockProducts(userId);

        // Calculate total inventory value
        double totalValue = products.stream()
                .mapToDouble(p -> p.getPrice() * p.getCurrentStock())
                .sum();

        // Calculate space utilization
        int maxSpaces = 10;
        int usedSpaces = spaces.size();
        double spaceUtilization = (usedSpaces / (double) maxSpaces) * 100;

        // Get stock status breakdown
        Map<String, Integer> stockStatus = getStockStatusBreakdown(products);

        return new DashboardOverviewDto(
                usedSpaces,
                maxSpaces,
                Math.round(spaceUtilization * 100.0) / 100.0,
                products.size(),
                Math.round(totalValue * 100.0) / 100.0,
                lowStockProducts.size(),
                stockStatus,
                usedSpaces > 0 ? Math.round((products.size() / (double) usedSpaces) * 100.0) / 100.0 : 0.0);
    }

    /**
     * Get detailed inventory insights
     */
    public InventoryInsightsDto getInventoryInsights(UUID userId) {
        List<Products> products = productService.getProductsByOwner(userId);

        if (products.isEmpty()) {
            return new InventoryInsightsDto(false, null, null, new HashMap<>(), new HashMap<>());
        }

        // Price analysis
        DoubleSummaryStatistics priceStats = products.stream()
                .mapToDouble(Products::getPrice)
                .summaryStatistics();

        PriceAnalysisDto priceAnalysis = new PriceAnalysisDto(
                Math.round(priceStats.getMin() * 100.0) / 100.0,
                Math.round(priceStats.getMax() * 100.0) / 100.0,
                Math.round(priceStats.getAverage() * 100.0) / 100.0);

        // Stock analysis
        IntSummaryStatistics stockStats = products.stream()
                .mapToInt(Products::getCurrentStock)
                .summaryStatistics();

        StockAnalysisDto stockAnalysis = new StockAnalysisDto(
                stockStats.getMin(),
                stockStats.getMax(),
                Math.round(stockStats.getAverage() * 100.0) / 100.0,
                stockStats.getSum());

        // Value distribution by space
        Map<String, Double> valueBySpace = products.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getSpace().getName(),
                        Collectors.summingDouble(p -> p.getPrice() * p.getCurrentStock())))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> Math.round(e.getValue() * 100.0) / 100.0));

        // Product count by space
        Map<String, Long> countBySpace = products.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getSpace().getName(),
                        Collectors.counting()));

        return new InventoryInsightsDto(true, priceAnalysis, stockAnalysis, valueBySpace, countBySpace);
    }

    /**
     * Get low stock alerts with detailed information
     */
    public LowStockAlertsDto getLowStockAlerts(UUID userId) {
        List<Products> lowStockProducts = productService.getLowStockProducts(userId);

        // Group by space for better organization
        Map<String, List<AlertInfo>> alertsBySpace = lowStockProducts.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getSpace().getName(),
                        Collectors.mapping(this::createAlertInfo, Collectors.toList())));

        // Calculate severity levels
        Map<String, Long> severityLevels = lowStockProducts.stream()
                .collect(Collectors.groupingBy(
                        this::getStockSeverity,
                        Collectors.counting()));

        return new LowStockAlertsDto(
                lowStockProducts.size(),
                alertsBySpace,
                severityLevels,
                !lowStockProducts.isEmpty());
    }

    /**
     * Get recent activity using audit logs
     */
    @SuppressWarnings("unchecked")
    public RecentActivityDto getRecentActivity(UUID userId) {
        List<AuditLogDto> recentLogs = auditLogService.getRecentActivity(userId, 168); // Last 7 days

        List<ActivityItem> activities = recentLogs.stream()
                .map((AuditLogDto log) -> {
                    Map<String, Object> details = new HashMap<>();
                    String description;

                    // Parse details for display
                    if (log.getDetails() != null) {
                        try {
                            ObjectMapper mapper = new ObjectMapper();
                            details = mapper.readValue(log.getDetails(), Map.class);
                            description = generateActivityDescription(log.getOperation(), log.getEntityType(), details);
                        } catch (JsonProcessingException e) {
                            description = log.getOperation() + " " + log.getEntityType();
                        }
                    } else {
                        description = log.getOperation() + " " + log.getEntityType();
                    }

                    return new ActivityItem(
                            log.getId(),
                            log.getOperation().toLowerCase(),
                            log.getEntityType().toLowerCase(),
                            log.getEntityId(),
                            log.getTimestamp(),
                            log.getIpAddress(),
                            details,
                            description);
                })
                .collect(Collectors.toList());

        return new RecentActivityDto(
                activities,
                activities.size(),
                !activities.isEmpty(),
                "Recent activities from audit logs");
    }

    /**
     * Get space performance metrics
     */
    public SpaceMetricsDto getSpaceMetrics(UUID userId) {
        List<SpaceDto> spacesWithCounts = spaceService.getSpacesWithProductCount(userId);

        if (spacesWithCounts.isEmpty()) {
            return new SpaceMetricsDto(false, new ArrayList<>(), null);
        }

        // Calculate space efficiency metrics
        List<SpaceMetric> spaceMetrics = spacesWithCounts.stream()
                .map(space -> {
                    // Get products for this space to calculate value
                    List<Products> spaceProducts = productService.getProductsBySpace(userId, space.getId());
                    double totalValue = spaceProducts.stream()
                            .mapToDouble(p -> p.getPrice() * p.getCurrentStock())
                            .sum();

                    long lowStockCount = spaceProducts.stream()
                            .filter(productService::isLowStock)
                            .count();

                    return new SpaceMetric(
                            space.getId(),
                            space.getName(),
                            space.getProductCount(),
                            Math.round(totalValue * 100.0) / 100.0,
                            lowStockCount,
                            calculateSpaceHealthScore(spaceProducts));
                })
                .sorted((a, b) -> Double.compare(b.getTotalValue(), a.getTotalValue()))
                .collect(Collectors.toList());

        // Summary statistics
        double totalValue = spaceMetrics.stream()
                .mapToDouble(SpaceMetric::getTotalValue)
                .sum();

        long totalProducts = spaceMetrics.stream()
                .mapToLong(SpaceMetric::getProductCount)
                .sum();

        SummaryDto summary = new SummaryDto(
                spacesWithCounts.size(),
                Math.round(totalValue * 100.0) / 100.0,
                totalProducts,
                !spacesWithCounts.isEmpty() ? Math.round((totalValue / spacesWithCounts.size()) * 100.0) / 100.0 : 0.0);

        return new SpaceMetricsDto(true, spaceMetrics, summary);
    }

    /**
     * Get top products by various criteria
     */
    public TopProductsDto getTopProducts(UUID userId, int limit, String sortBy) {
        List<Products> products = productService.getProductsByOwner(userId);

        if (products.isEmpty()) {
            return new TopProductsDto(false, new ArrayList<>(), sortBy, limit);
        }

        List<ProductSummary> topProducts;

        switch (sortBy.toLowerCase()) {
            case "stock":
                topProducts = products.stream()
                        .sorted((a, b) -> Integer.compare(b.getCurrentStock(), a.getCurrentStock()))
                        .limit(limit)
                        .map(this::createProductSummary)
                        .collect(Collectors.toList());
                break;
            case "price":
                topProducts = products.stream()
                        .sorted((a, b) -> Double.compare(b.getPrice(), a.getPrice()))
                        .limit(limit)
                        .map(this::createProductSummary)
                        .collect(Collectors.toList());
                break;
            case "value":
            default:
                topProducts = products.stream()
                        .sorted((a, b) -> Double.compare(
                                b.getPrice() * b.getCurrentStock(),
                                a.getPrice() * a.getCurrentStock()))
                        .limit(limit)
                        .map(this::createProductSummary)
                        .collect(Collectors.toList());
                break;
        }

        return new TopProductsDto(true, topProducts, sortBy, limit);
    }

    /**
     * Get inventory trends
     */
    public InventoryTrendsDto getInventoryTrends(UUID userId, int days) {
        List<Products> products = productService.getProductsByOwner(userId);
        List<Spaces> spaces = spaceService.getSpacesByOwner(userId);

        // Try to get historical data from audit logs
        Map<String, Object> trendsData = auditLogService.getActivityTrendsAsMap(userId, days);

        if (trendsData.containsKey("dailyActivity")) {
            // We have historical data from audit logs
            @SuppressWarnings("unchecked")
            Map<String, Long> dailyActivity = (Map<String, Long>) trendsData.get("dailyActivity");
            @SuppressWarnings("unchecked")
            Map<String, Long> operationBreakdown = (Map<String, Long>) trendsData.get("operationBreakdown");
            Long totalActivities = (Long) trendsData.get("totalActivities");
            String period = (String) trendsData.get("period");

            CurrentSnapshot snapshot = new CurrentSnapshot(
                    new Date(),
                    products.size(),
                    spaces.size(),
                    Math.round(products.stream().mapToDouble(p -> p.getPrice() * p.getCurrentStock()).sum() * 100.0)
                            / 100.0,
                    productService.getLowStockProducts(userId).size());

            return new InventoryTrendsDto(
                    true,
                    snapshot,
                    dailyActivity,
                    operationBreakdown,
                    totalActivities,
                    period,
                    null,
                    days);
        } else {
            // No historical data available
            CurrentSnapshot snapshot = new CurrentSnapshot(
                    new Date(),
                    products.size(),
                    spaces.size(),
                    Math.round(products.stream().mapToDouble(p -> p.getPrice() * p.getCurrentStock()).sum() * 100.0)
                            / 100.0,
                    productService.getLowStockProducts(userId).size());

            return new InventoryTrendsDto(
                    false,
                    snapshot,
                    null,
                    null,
                    null,
                    null,
                    "Historical trend data requires audit logging system. Showing current state.",
                    days);
        }
    }

    // Helper methods

    private Map<String, Integer> getStockStatusBreakdown(List<Products> products) {
        Map<String, Integer> status = new HashMap<>();
        status.put("inStock", 0);
        status.put("lowStock", 0);
        status.put("outOfStock", 0);

        for (Products product : products) {
            if (product.getCurrentStock() == 0) {
                status.put("outOfStock", status.get("outOfStock") + 1);
            } else if (productService.isLowStock(product)) {
                status.put("lowStock", status.get("lowStock") + 1);
            } else {
                status.put("inStock", status.get("inStock") + 1);
            }
        }

        return status;
    }

    private AlertInfo createAlertInfo(Products product) {
        return new AlertInfo(
                product.getId(),
                product.getName(),
                product.getSpace().getName(),
                product.getCurrentStock(),
                product.getMinimumQuantity(),
                getStockSeverity(product),
                product.getMinimumQuantity() != null ? product.getMinimumQuantity() - product.getCurrentStock() : 0);
    }

    private String getStockSeverity(Products product) {
        if (product.getCurrentStock() == 0) {
            return "critical";
        }
        if (product.getMinimumQuantity() != null) {
            double ratio = (double) product.getCurrentStock() / product.getMinimumQuantity();
            if (ratio <= 0.5) {
                return "high";
            } else if (ratio <= 0.8) {
                return "medium";
            }
        }
        return "low";
    }

    private double calculateSpaceHealthScore(List<Products> products) {
        if (products.isEmpty()) {
            return 100.0;
        }

        long totalProducts = products.size();
        long lowStockProducts = products.stream()
                .filter(productService::isLowStock)
                .count();
        long outOfStockProducts = products.stream()
                .filter(p -> p.getCurrentStock() == 0)
                .count();

        // Health score: 100 - (lowStock penalty + outOfStock penalty)
        double lowStockPenalty = (lowStockProducts / (double) totalProducts) * 30;
        double outOfStockPenalty = (outOfStockProducts / (double) totalProducts) * 50;

        return Math.max(0, Math.round((100 - lowStockPenalty - outOfStockPenalty) * 100.0) / 100.0);
    }

    private ProductSummary createProductSummary(Products product) {
        return new ProductSummary(
                product.getId(),
                product.getName(),
                product.getSpace().getName(),
                product.getPrice(),
                product.getCurrentStock(),
                Math.round(product.getPrice() * product.getCurrentStock() * 100.0) / 100.0,
                productService.isLowStock(product));
    }

    private String generateActivityDescription(String operation, String entityType, Map<String, Object> details) {
        return switch (operation + "_" + entityType) {
            case "CREATE_SPACE" -> "Created space: " + details.get("spaceName");
            case "UPDATE_SPACE" ->
                "Renamed space from '" + details.get("oldName") + "' to '" + details.get("newName") + "'";
            case "DELETE_SPACE" -> "Deleted space: " + details.get("spaceName");
            case "CREATE_PRODUCT" ->
                "Created product '" + details.get("productName") + "' in space '" + details.get("spaceName")
                        + "'";
            case "UPDATE_PRODUCT" -> "Updated product: " + details.get("productName");
            case "DELETE_PRODUCT" ->
                "Deleted product '" + details.get("productName") + "' from space '" + details.get("spaceName")
                        + "'";
            case "STOCK_ADD" ->
                "Added " + details.get("quantityAdded") + " units to '" + details.get("productName") + "'";
            case "STOCK_REMOVE" ->
                "Removed " + details.get("quantityRemoved") + " units from '" + details.get("productName") + "'";
            case "STOCK_UPDATE" -> "Updated stock for '" + details.get("productName") + "' from " +
                    details.get("oldStock") + " to " + details.get("newStock");
            default -> operation + " " + entityType;
        };
    }
}