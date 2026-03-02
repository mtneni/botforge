package org.legendstack.basebot.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

/**
 * Repository for audit log entries. Read-heavy — no update or delete operations
 * should be exposed.
 */
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    Page<AuditLogEntity> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);

    Page<AuditLogEntity> findByActionOrderByTimestampDesc(String action, Pageable pageable);

    List<AuditLogEntity> findByTimestampBetween(Instant start, Instant end);

    long countByTimestampAfter(Instant since);

    long countByAction(String action);

    long countByActionAndTimestampAfter(String action, Instant since);

    /**
     * Group audit events by action type with counts, for analytics.
     */
    @Query("SELECT a.action, COUNT(a) FROM AuditLogEntity a WHERE a.timestamp > ?1 GROUP BY a.action ORDER BY COUNT(a) DESC")
    List<Object[]> countByActionGrouped(Instant since);

    /**
     * Top active users by event count.
     */
    @Query("SELECT a.username, COUNT(a) FROM AuditLogEntity a WHERE a.timestamp > ?1 GROUP BY a.username ORDER BY COUNT(a) DESC")
    List<Object[]> findTopUsersByActivity(Instant since);
}
