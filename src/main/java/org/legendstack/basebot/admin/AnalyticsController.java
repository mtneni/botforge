package org.legendstack.basebot.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin-only analytics endpoints for the dashboard.
 */
@RestController
@RequestMapping("/api/admin/analytics")
@PreAuthorize("hasRole('ADMIN')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        return ResponseEntity.ok(analyticsService.getDashboardStats());
    }

    @GetMapping("/activity")
    public ResponseEntity<?> getActivityBreakdown(@RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(analyticsService.getActivityBreakdown(days));
    }

    @GetMapping("/top-users")
    public ResponseEntity<?> getTopUsers(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(analyticsService.getTopUsers(days, limit));
    }

    @GetMapping("/timeline")
    public ResponseEntity<?> getTimeline(@RequestParam(defaultValue = "24") int hours) {
        return ResponseEntity.ok(analyticsService.getRecentTimeline(hours));
    }
}
