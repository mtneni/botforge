package org.legendstack.basebot.admin;

import org.legendstack.basebot.audit.AuditLogRepository;
import org.legendstack.basebot.audit.AuditService;
import org.legendstack.basebot.conversation.ConversationRepository;
import org.legendstack.basebot.security.TokenBudgetService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Analytics service for aggregating platform-wide metrics.
 * Computes stats from audit logs, conversations, and token usage.
 */
@Service
public class AnalyticsService {

    private final AuditLogRepository auditRepository;
    private final ConversationRepository conversationRepository;
    private final TokenBudgetService tokenBudgetService;

    public AnalyticsService(AuditLogRepository auditRepository,
            ConversationRepository conversationRepository,
            TokenBudgetService tokenBudgetService) {
        this.auditRepository = auditRepository;
        this.conversationRepository = conversationRepository;
        this.tokenBudgetService = tokenBudgetService;
    }

    /**
     * Dashboard overview stats.
     */
    public Map<String, Object> getDashboardStats() {
        var todayStart = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", conversationRepository.countDistinctUsers());
        stats.put("totalConversations", conversationRepository.count());
        stats.put("totalAuditEvents", auditRepository.count());
        stats.put("eventsToday", auditRepository.countByTimestampAfter(todayStart));
        stats.put("messagesToday", auditRepository.countByActionAndTimestampAfter(
                AuditService.ACTION_CHAT_MESSAGE, todayStart));
        stats.put("dailyTokenLimit", tokenBudgetService.getDailyLimit());
        return stats;
    }

    /**
     * Activity breakdown by action type for the last N days.
     */
    public List<Map<String, Object>> getActivityBreakdown(int days) {
        var since = Instant.now().minus(java.time.Duration.ofDays(days));
        var grouped = auditRepository.countByActionGrouped(since);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : grouped) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("action", row[0]);
            entry.put("count", row[1]);
            result.add(entry);
        }
        return result;
    }

    /**
     * Top active users for the given period.
     */
    public List<Map<String, Object>> getTopUsers(int days, int limit) {
        var since = Instant.now().minus(java.time.Duration.ofDays(days));
        var topUsers = auditRepository.findTopUsersByActivity(since);

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, topUsers.size()); i++) {
            Object[] row = topUsers.get(i);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("username", row[0]);
            entry.put("eventCount", row[1]);
            result.add(entry);
        }
        return result;
    }

    /**
     * Recent events timeline for the last N hours.
     */
    public List<Map<String, Object>> getRecentTimeline(int hours) {
        var since = Instant.now().minus(java.time.Duration.ofHours(hours));
        var events = auditRepository.findByTimestampBetween(since, Instant.now());

        // Group by hour bucket
        Map<String, Long> hourBuckets = new LinkedHashMap<>();
        for (var event : events) {
            var hourKey = event.getTimestamp().atZone(ZoneOffset.UTC)
                    .withMinute(0).withSecond(0).withNano(0).toInstant().toString();
            hourBuckets.merge(hourKey, 1L, (a, b) -> a + b);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        hourBuckets.forEach((time, count) -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("timestamp", time);
            entry.put("count", count);
            result.add(entry);
        });
        return result;
    }
}
