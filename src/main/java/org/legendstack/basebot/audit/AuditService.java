package org.legendstack.basebot.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.legendstack.basebot.user.BotForgeUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Centralized audit logging for all security-sensitive actions.
 * Writes are async to avoid slowing down the main request path.
 */
@Service
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    private final AuditLogRepository repository;

    /** Audit action constants */
    public static final String ACTION_LOGIN = "LOGIN";
    public static final String ACTION_LOGOUT = "LOGOUT";
    public static final String ACTION_REGISTER = "REGISTER";
    public static final String ACTION_CHAT_MESSAGE = "CHAT_MESSAGE";
    public static final String ACTION_PERSONA_SWITCH = "PERSONA_SWITCH";
    public static final String ACTION_DOCUMENT_UPLOAD = "DOCUMENT_UPLOAD";
    public static final String ACTION_ADMIN_ACTION = "ADMIN_ACTION";

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    /**
     * Log an auditable action for a known user.
     */
    @Async
    public void log(BotForgeUser user, String action, String detail) {
        try {
            String ip = resolveIp();
            String sessionId = resolveSessionId();
            var entry = new AuditLogEntity(
                    user.getId(), user.getUsername(), action, detail, ip, sessionId);
            repository.save(entry);
            logger.debug("Audit: {} {} — {}", user.getUsername(), action, detail);
        } catch (Exception e) {
            // Audit failures must never break the main flow
            logger.error("Failed to write audit log: {}", e.getMessage());
        }
    }

    /**
     * Log an auditable action for anonymous/pre-auth actions (login, register).
     */
    @Async
    public void logAnonymous(String username, String action, String detail) {
        try {
            String ip = resolveIp();
            var entry = new AuditLogEntity("anonymous", username, action, detail, ip, null);
            repository.save(entry);
            logger.debug("Audit: {} {} — {}", username, action, detail);
        } catch (Exception e) {
            logger.error("Failed to write audit log: {}", e.getMessage());
        }
    }

    private String resolveIp() {
        try {
            var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String forwarded = request.getHeader("X-Forwarded-For");
                return forwarded != null ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
            }
        } catch (Exception ignored) {
        }
        return "unknown";
    }

    private String resolveSessionId() {
        try {
            var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                var session = attrs.getRequest().getSession(false);
                return session != null ? session.getId() : null;
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
