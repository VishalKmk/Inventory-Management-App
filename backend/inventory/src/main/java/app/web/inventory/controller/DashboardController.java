package app.web.inventory.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import app.web.inventory.config.SecurityUtil;
import app.web.inventory.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
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
    public ResponseEntity<?> getDashboardOverview() {
        try {
            UUID currentUserId = SecurityUtil.getCurrentUserId();
            var overview = dashboardService.getDashboardOverview(currentUserId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", overview));

        } catch (Exception ex) {
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }

    /**
     * Get inventory insights and statistics
     * GET /api/dashboard/insights
     */
    @GetMapping("/insights")
    public ResponseEntity<?> getInventoryInsights() {
        try {
            UUID currentUserId = SecurityUtil.getCurrentUserId();
            var insights = dashboardService.getInventoryInsights(currentUserId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", insights));

        } catch (Exception ex) {
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }

    /**
     * Get low stock alerts across all spaces
     * GET /api/dashboard/low-stock-alerts
     */
    @GetMapping("/low-stock-alerts")
    public ResponseEntity<?> getLowStockAlerts() {
        try {
            UUID currentUserId = SecurityUtil.getCurrentUserId();
            var alerts = dashboardService.getLowStockAlerts(currentUserId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", alerts));

        } catch (Exception ex) {
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }

    /**
     * Get recent activity summary
     * GET /api/dashboard/recent-activity
     */
    @GetMapping("/recent-activity")
    public ResponseEntity<?> getRecentActivity() {
        try {
            UUID currentUserId = SecurityUtil.getCurrentUserId();
            var activity = dashboardService.getRecentActivity(currentUserId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", activity));

        } catch (Exception ex) {
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }

    /**
     * Get space performance metrics
     * GET /api/dashboard/space-metrics
     */
    @GetMapping("/space-metrics")
    public ResponseEntity<?> getSpaceMetrics() {
        try {
            UUID currentUserId = SecurityUtil.getCurrentUserId();
            var metrics = dashboardService.getSpaceMetrics(currentUserId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", metrics));

        } catch (Exception ex) {
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }

    /**
     * Get top products by value across all spaces
     * GET /api/dashboard/top-products
     */
    @GetMapping("/top-products")
    public ResponseEntity<?> getTopProducts(
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(defaultValue = "value") String sortBy) {
        try {
            UUID currentUserId = SecurityUtil.getCurrentUserId();
            var topProducts = dashboardService.getTopProducts(currentUserId, limit, sortBy);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", topProducts));

        } catch (Exception ex) {
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }

    /**
     * Get inventory value trends (if audit logs are available)
     * GET /api/dashboard/trends
     */
    @GetMapping("/trends")
    public ResponseEntity<?> getInventoryTrends(
            @RequestParam(defaultValue = "30") int days) {
        try {
            UUID currentUserId = SecurityUtil.getCurrentUserId();
            var trends = dashboardService.getInventoryTrends(currentUserId, days);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", trends,
                    "message", "Trend data based on last " + days + " days"));

        } catch (Exception ex) {
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }
}