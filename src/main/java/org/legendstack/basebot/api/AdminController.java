package org.legendstack.basebot.api;

import org.legendstack.basebot.audit.AuditLogEntity;
import org.legendstack.basebot.audit.AuditLogRepository;
import org.legendstack.basebot.security.TokenBudgetService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin-only endpoints for auditing, monitoring, and user management.
 * All endpoints require ADMIN role.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AuditLogRepository auditLogRepository;
    private final TokenBudgetService tokenBudgetService;

    public AdminController(AuditLogRepository auditLogRepository,
            TokenBudgetService tokenBudgetService) {
        this.auditLogRepository = auditLogRepository;
        this.tokenBudgetService = tokenBudgetService;
    }

    /**
     * View audit logs with pagination. Admin only.
     */
    @GetMapping("/audit")
    public ResponseEntity<Page<AuditLogEntity>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return ResponseEntity.ok(auditLogRepository.findAll(pageable));
    }

    /**
     * View audit logs filtered by user.
     */
    @GetMapping("/audit/user/{userId}")
    public ResponseEntity<Page<AuditLogEntity>> getAuditLogsByUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(auditLogRepository.findByUserIdOrderByTimestampDesc(userId, pageable));
    }

    /**
     * Check token budget for a specific user. Admin only.
     */
    @GetMapping("/tokens/{userId}")
    public ResponseEntity<Map<String, Object>> getUserTokenBudget(@PathVariable String userId) {
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "used", tokenBudgetService.currentUsage(userId),
                "remaining", tokenBudgetService.remaining(userId),
                "dailyLimit", tokenBudgetService.getDailyLimit()));
    }
}
