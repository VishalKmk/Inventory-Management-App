package app.web.inventory.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.web.inventory.dto.api.ApiResponse;
import app.web.inventory.dto.dashboard.DashboardOverviewDto;
import app.web.inventory.dto.dashboard.InventoryInsightsDto;
import app.web.inventory.dto.dashboard.InventoryTrendsDto;
import app.web.inventory.dto.dashboard.LowStockAlertsDto;
import app.web.inventory.dto.dashboard.RecentActivityDto;
import app.web.inventory.dto.dashboard.SpaceMetricsDto;
import app.web.inventory.dto.dashboard.SpaceDashboardDto;
import app.web.inventory.dto.dashboard.TopProductsDto;
import app.web.inventory.service.DashboardService;
import app.web.inventory.util.SecurityUtil;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/dashboard")
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Get dashboard overview statistics
     * GET /api/dashboard/overview
     */
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<DashboardOverviewDto>> getDashboardOverview() {
        try {
            UUID currentUserId = SecurityUtil.getCurrentUserId();
            DashboardOverviewDto overview = dashboardService.getDashboardOverview(currentUserId);

            return ResponseEntity.ok(ApiResponse.success(overview));

        } catch (Exception ex) {
            log.error("Error retrieving dashboard overview", ex);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Internal server error"));
        }
    }

    @GetMapping("/spaces/{spaceId}")
    public ResponseEntity<ApiResponse<SpaceDashboardDto>> getSpaceDashboard(
            @org.springframework.web.bind.annotation.PathVariable UUID spaceId,
            @RequestParam(defaultValue = "30") int days) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getSpaceDashboard(currentUserId, spaceId, days)));
    }

    /**
     * Get inventory insights and statistics
     * GET /api/dashboard/insights
     */
    @GetMapping("/insights")
    public ResponseEntity<ApiResponse<InventoryInsightsDto>> getInventoryInsights() {
        try {
            UUID currentUserId = SecurityUtil.getCurrentUserId();
            InventoryInsightsDto insights = dashboardService.getInventoryInsights(currentUserId);

            return ResponseEntity.ok(ApiResponse.success(insights));

        } catch (Exception ex) {
            log.error("Error retrieving inventory insights", ex);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Internal server error"));
        }
    }

    /**
     * Get low stock alerts across all spaces
     * GET /api/dashboard/low-stock-alerts
     */
    @GetMapping("/low-stock-alerts")
    public ResponseEntity<ApiResponse<LowStockAlertsDto>> getLowStockAlerts() {
        try {
            UUID currentUserId = SecurityUtil.getCurrentUserId();
            LowStockAlertsDto alerts = dashboardService.getLowStockAlerts(currentUserId);

            return ResponseEntity.ok(ApiResponse.success(alerts));

        } catch (Exception ex) {
            log.error("Error retrieving low stock alerts", ex);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Internal server error"));
        }
    }

    /**
     * Get recent activity summary
     * GET /api/dashboard/recent-activity
     */
    @GetMapping("/recent-activity")
    public ResponseEntity<ApiResponse<RecentActivityDto>> getRecentActivity() {
        try {
            UUID currentUserId = SecurityUtil.getCurrentUserId();
            RecentActivityDto activity = dashboardService.getRecentActivity(currentUserId);

            return ResponseEntity.ok(ApiResponse.success(activity));

        } catch (Exception ex) {
            log.error("Error retrieving recent activity", ex);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Internal server error"));
        }
    }

    /**
     * Get space performance metrics
     * GET /api/dashboard/space-metrics
     */
    @GetMapping("/space-metrics")
    public ResponseEntity<ApiResponse<SpaceMetricsDto>> getSpaceMetrics() {
        try {
            UUID currentUserId = SecurityUtil.getCurrentUserId();
            SpaceMetricsDto metrics = dashboardService.getSpaceMetrics(currentUserId);

            return ResponseEntity.ok(ApiResponse.success(metrics));

        } catch (Exception ex) {
            log.error("Error retrieving space metrics", ex);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Internal server error"));
        }
    }

    /**
     * Get top products by value across all spaces
     * GET /api/dashboard/top-products
     */
    @GetMapping("/top-products")
    public ResponseEntity<ApiResponse<TopProductsDto>> getTopProducts(
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(defaultValue = "value") String sortBy) {
        try {
            UUID currentUserId = SecurityUtil.getCurrentUserId();
            TopProductsDto topProducts = dashboardService.getTopProducts(currentUserId, limit, sortBy);

            return ResponseEntity.ok(ApiResponse.success(topProducts));

        } catch (Exception ex) {
            log.error("Error retrieving top products", ex);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Internal server error"));
        }
    }

    /**
     * Get inventory value trends (if audit logs are available)
     * GET /api/dashboard/trends
     */
    @GetMapping("/trends")
    public ResponseEntity<ApiResponse<InventoryTrendsDto>> getInventoryTrends(
            @RequestParam(defaultValue = "30") int days) {
        try {
            UUID currentUserId = SecurityUtil.getCurrentUserId();
            InventoryTrendsDto trends = dashboardService.getInventoryTrends(currentUserId, days);

            return ResponseEntity.ok(ApiResponse.success(
                    "Trend data based on last " + days + " days",
                    trends));

        } catch (Exception ex) {
            log.error("Error retrieving inventory trends", ex);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Internal server error"));
        }
    }
}
