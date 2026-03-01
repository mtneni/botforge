package org.legendstack.basebot.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
