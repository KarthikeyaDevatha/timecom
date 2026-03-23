package com.timecom.sessiontracker.scheduler;

import com.timecom.sessiontracker.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task to clean up expired sessions.
 * Runs periodically to mark stale sessions as inactive.
 */
@Component
public class SessionCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(SessionCleanupScheduler.class);

    private final SessionService sessionService;

    public SessionCleanupScheduler(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * Expire stale sessions every 15 minutes.
     */
    @Scheduled(fixedDelayString = "${app.session.cleanup-interval-ms}")
    public void cleanupExpiredSessions() {
        log.debug("Running session cleanup task...");
        int expired = sessionService.expireStaleSessions();
        if (expired > 0) {
            log.info("Session cleanup completed: {} sessions expired", expired);
        }
    }
}
