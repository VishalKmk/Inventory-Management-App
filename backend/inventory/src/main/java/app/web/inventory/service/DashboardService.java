package app.web.inventory.service;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import app.web.inventory.dto.AuditLogDto;
import app.web.inventory.dto.SpaceDto;
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
        public Map<String, Object> getDashboardOverview(UUID userId) {
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

                Map<String, Object> overview = new HashMap<>();
                overview.put("totalSpaces", usedSpaces);
                overview.put("maxSpaces", maxSpaces);
                overview.put("spaceUtilization", Math.round(spaceUtilization * 100.0) / 100.0);
                overview.put("totalProducts", products.size());
                overview.put("totalValue", Math.round(totalValue * 100.0) / 100.0);
                overview.put("lowStockCount", lowStockProducts.size());
                overview.put("stockStatus", stockStatus);
                overview.put("averageProductsPerSpace",
                                usedSpaces > 0 ? Math.round((products.size() / (double) usedSpaces) * 100.0) / 100.0
                                                : 0);

                return overview;
        }

        /**
         * Get detailed inventory insights
         */
        public Map<String, Object> getInventoryInsights(UUID userId) {
                List<Products> products = productService.getProductsByOwner(userId);

                if (products.isEmpty()) {
                        return Map.of(
                                        "message", "No products found",
                                        "hasData", false);
                }

                // Price analysis
                DoubleSummaryStatistics priceStats = products.stream()
                                .mapToDouble(Products::getPrice)
                                .summaryStatistics();

                // Stock analysis
                IntSummaryStatistics stockStats = products.stream()
                                .mapToInt(Products::getCurrentStock)
                                .summaryStatistics();

                // Value distribution by space
                Map<String, Double> valueBySpace = products.stream()
                                .collect(Collectors.groupingBy(
                                                p -> p.getSpace().getName(),
                                                Collectors.summingDouble(p -> p.getPrice() * p.getCurrentStock())));

                // Product count by space
                Map<String, Long> countBySpace = products.stream()
                                .collect(Collectors.groupingBy(
                                                p -> p.getSpace().getName(),
                                                Collectors.counting()));

                Map<String, Object> insights = new HashMap<>();
                insights.put("hasData", true);
                insights.put("priceAnalysis", Map.of(
                                "minimum", Math.round(priceStats.getMin() * 100.0) / 100.0,
                                "maximum", Math.round(priceStats.getMax() * 100.0) / 100.0,
                                "average", Math.round(priceStats.getAverage() * 100.0) / 100.0));
                insights.put("stockAnalysis", Map.of(
                                "minimum", stockStats.getMin(),
                                "maximum", stockStats.getMax(),
                                "average", Math.round(stockStats.getAverage() * 100.0) / 100.0,
                                "total", stockStats.getSum()));
                insights.put("valueBySpace", valueBySpace.entrySet().stream()
                                .collect(Collectors.toMap(
                                                Map.Entry::getKey,
                                                e -> Math.round(e.getValue() * 100.0) / 100.0)));
                insights.put("productCountBySpace", countBySpace);

                return insights;
        }

        /**
         * Get low stock alerts with detailed information
         */
        public Map<String, Object> getLowStockAlerts(UUID userId) {
                List<Products> lowStockProducts = productService.getLowStockProducts(userId);

                // Group by space for better organization
                Map<String, List<Map<String, Object>>> alertsBySpace = lowStockProducts.stream()
                                .collect(Collectors.groupingBy(
                                                p -> p.getSpace().getName(),
                                                Collectors.mapping(this::createAlertInfo, Collectors.toList())));

                // Calculate severity levels
                Map<String, Long> severityLevels = lowStockProducts.stream()
                                .collect(Collectors.groupingBy(
                                                this::getStockSeverity,
                                                Collectors.counting()));

                Map<String, Object> alerts = new HashMap<>();
                alerts.put("totalAlerts", lowStockProducts.size());
                alerts.put("alertsBySpace", alertsBySpace);
                alerts.put("severityBreakdown", severityLevels);
                alerts.put("hasAlerts", !lowStockProducts.isEmpty());

                return alerts;
        }

        /**
         * Get recent activity (limited without audit logs)
         */
        /**
         * Get recent activity using audit logs instead of creation dates
         */
        @SuppressWarnings("unchecked")
        public Map<String, Object> getRecentActivity(UUID userId) {
                List<AuditLogDto> recentLogs = auditLogService.getRecentActivity(userId, 168); // Last 7 days

                List<Map<String, Object>> activities = recentLogs.stream()
                                .map(log -> {
                                        Map<String, Object> activity = new HashMap<>();
                                        activity.put("id", log.getId());
                                        activity.put("type", log.getOperation().toLowerCase());
                                        activity.put("entityType", log.getEntityType().toLowerCase());
                                        activity.put("entityId", log.getEntityId());
                                        activity.put("timestamp", log.getTimestamp());
                                        activity.put("ipAddress", log.getIpAddress());

                                        // Parse details for display
                                        if (log.getDetails() != null) {
                                                try {
                                                        ObjectMapper mapper = new ObjectMapper();
                                                        Map<String, Object> details = mapper.readValue(log.getDetails(),
                                                                        Map.class);
                                                        activity.put("details", details);

                                                        // Add readable description
                                                        String description = generateActivityDescription(
                                                                        log.getOperation(), log.getEntityType(),
                                                                        details);
                                                        activity.put("description", description);

                                                } catch (Exception e) {
                                                        activity.put("description",
                                                                        log.getOperation() + " " + log.getEntityType());
                                                }
                                        } else {
                                                activity.put("description",
                                                                log.getOperation() + " " + log.getEntityType());
                                        }

                                        return activity;
                                })
                                .collect(Collectors.toList());

                Map<String, Object> recentActivity = new HashMap<>();
                recentActivity.put("activities", activities);
                recentActivity.put("totalCount", activities.size());
                recentActivity.put("hasActivity", !activities.isEmpty());
                recentActivity.put("message", "Recent activities from audit logs");

                return recentActivity;
        }

        /**
         * Get space performance metrics
         */
        public Map<String, Object> getSpaceMetrics(UUID userId) {
                List<SpaceDto> spacesWithCounts = spaceService.getSpacesWithProductCount(userId);

                if (spacesWithCounts.isEmpty()) {
                        return Map.of(
                                        "message", "No spaces found",
                                        "hasData", false);
                }

                // Calculate space efficiency metrics
                List<Map<String, Object>> spaceMetrics = spacesWithCounts.stream()
                                .map(space -> {
                                        // Get products for this space to calculate value
                                        List<Products> spaceProducts = productService.getProductsBySpace(userId,
                                                        space.getId());
                                        double totalValue = spaceProducts.stream()
                                                        .mapToDouble(p -> p.getPrice() * p.getCurrentStock())
                                                        .sum();

                                        long lowStockCount = spaceProducts.stream()
                                                        .mapToLong(p -> productService.isLowStock(p) ? 1 : 0)
                                                        .sum();

                                        Map<String, Object> metric = new HashMap<>();
                                        metric.put("spaceId", space.getId());
                                        metric.put("spaceName", space.getName());
                                        metric.put("productCount", space.getProductCount());
                                        metric.put("totalValue", Math.round(totalValue * 100.0) / 100.0);
                                        metric.put("lowStockCount", lowStockCount);
                                        metric.put("healthScore", calculateSpaceHealthScore(spaceProducts));
                                        return metric;
                                })
                                .sorted((a, b) -> Double.compare((Double) b.get("totalValue"),
                                                (Double) a.get("totalValue")))
                                .collect(Collectors.toList());

                // Summary statistics
                double totalValue = spaceMetrics.stream()
                                .mapToDouble(m -> (Double) m.get("totalValue"))
                                .sum();

                long totalProducts = spaceMetrics.stream()
                                .mapToLong(m -> (Long) m.get("productCount"))
                                .sum();

                Map<String, Object> metrics = new HashMap<>();
                metrics.put("hasData", true);
                metrics.put("spaceMetrics", spaceMetrics);
                metrics.put("summary", Map.of(
                                "totalSpaces", spacesWithCounts.size(),
                                "totalValue", Math.round(totalValue * 100.0) / 100.0,
                                "totalProducts", totalProducts,
                                "averageValuePerSpace",
                                spacesWithCounts.size() > 0
                                                ? Math.round((totalValue / spacesWithCounts.size()) * 100.0) / 100.0
                                                : 0));

                return metrics;
        }

        /**
         * Get top products by various criteria
         */
        public Map<String, Object> getTopProducts(UUID userId, int limit, String sortBy) {
                List<Products> products = productService.getProductsByOwner(userId);

                if (products.isEmpty()) {
                        return Map.of(
                                        "message", "No products found",
                                        "hasData", false);
                }

                List<Map<String, Object>> topProducts;

                switch (sortBy.toLowerCase()) {
                        case "stock":
                                topProducts = products.stream()
                                                .sorted((a, b) -> Integer.compare(b.getCurrentStock(),
                                                                a.getCurrentStock()))
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

                Map<String, Object> result = new HashMap<>();
                result.put("hasData", true);
                result.put("topProducts", topProducts);
                result.put("sortedBy", sortBy);
                result.put("limit", limit);

                return result;
        }

        /**
         * Get basic inventory trends (without full audit system)
         */
        public Map<String, Object> getInventoryTrends(UUID userId, int days) {
                List<Products> products = productService.getProductsByOwner(userId);
                List<Spaces> spaces = spaceService.getSpacesByOwner(userId);

                // Since we don't have historical data, provide current snapshot analysis
                Map<String, Object> currentSnapshot = Map.of(
                                "date", new Date(),
                                "totalProducts", products.size(),
                                "totalSpaces", spaces.size(),
                                "totalValue", Math.round(products.stream()
                                                .mapToDouble(p -> p.getPrice() * p.getCurrentStock())
                                                .sum() * 100.0) / 100.0,
                                "lowStockCount", productService.getLowStockProducts(userId).size());

                Map<String, Object> trends = new HashMap<>();
                trends.put("hasHistoricalData", false);
                trends.put("currentSnapshot", currentSnapshot);
                trends.put("message", "Historical trend data requires audit logging system. Showing current state.");
                trends.put("requestedDays", days);

                return trends;
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

        private Map<String, Object> createAlertInfo(Products product) {
                Map<String, Object> alert = new HashMap<>();
                alert.put("productId", product.getId());
                alert.put("productName", product.getName());
                alert.put("spaceName", product.getSpace().getName());
                alert.put("currentStock", product.getCurrentStock());
                alert.put("minimumQuantity", product.getMinimumQuantity());
                alert.put("severity", getStockSeverity(product));
                alert.put("stockDifference",
                                product.getMinimumQuantity() != null
                                                ? product.getMinimumQuantity() - product.getCurrentStock()
                                                : 0);
                return alert;
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
                                .mapToLong(p -> productService.isLowStock(p) ? 1 : 0)
                                .sum();
                long outOfStockProducts = products.stream()
                                .mapToLong(p -> p.getCurrentStock() == 0 ? 1 : 0)
                                .sum();

                // Health score: 100 - (lowStock penalty + outOfStock penalty)
                double lowStockPenalty = (lowStockProducts / (double) totalProducts) * 30;
                double outOfStockPenalty = (outOfStockProducts / (double) totalProducts) * 50;

                return Math.max(0, Math.round((100 - lowStockPenalty - outOfStockPenalty) * 100.0) / 100.0);
        }

        private Map<String, Object> createProductSummary(Products product) {
                Map<String, Object> summary = new HashMap<>();
                summary.put("productId", product.getId());
                summary.put("name", product.getName());
                summary.put("spaceName", product.getSpace().getName());
                summary.put("price", product.getPrice());
                summary.put("currentStock", product.getCurrentStock());
                summary.put("totalValue", Math.round(product.getPrice() * product.getCurrentStock() * 100.0) / 100.0);
                summary.put("isLowStock", productService.isLowStock(product));
                return summary;
        }

        private String generateActivityDescription(String operation, String entityType, Map<String, Object> details) {
                switch (operation + "_" + entityType) {
                        case "CREATE_SPACE":
                                return "Created space: " + details.get("spaceName");
                        case "UPDATE_SPACE":
                                return "Renamed space from '" + details.get("oldName") + "' to '"
                                                + details.get("newName") + "'";
                        case "DELETE_SPACE":
                                return "Deleted space: " + details.get("spaceName");
                        case "CREATE_PRODUCT":
                                return "Created product '" + details.get("productName") + "' in space '"
                                                + details.get("spaceName") + "'";
                        case "UPDATE_PRODUCT":
                                return "Updated product: " + details.get("productName");
                        case "DELETE_PRODUCT":
                                return "Deleted product '" + details.get("productName") + "' from space '"
                                                + details.get("spaceName") + "'";
                        case "STOCK_ADD":
                                return "Added " + details.get("quantityAdded") + " units to '"
                                                + details.get("productName") + "'";
                        case "STOCK_REMOVE":
                                return "Removed " + details.get("quantityRemoved") + " units from '"
                                                + details.get("productName") + "'";
                        case "STOCK_UPDATE":
                                return "Updated stock for '" + details.get("productName") + "' from " +
                                                details.get("oldStock") + " to " + details.get("newStock");
                        default:
                                return operation + " " + entityType;
                }
        }

        /**
         * Get activity trends using audit logs
         */
        public Map<String, Object> getActivityTrends(UUID userId, int days) {
                return auditLogService.getActivityTrends(userId, days);
        }
}