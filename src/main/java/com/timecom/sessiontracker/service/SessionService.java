package com.timecom.sessiontracker.service;

import com.timecom.sessiontracker.dto.SessionResponse;
import com.timecom.sessiontracker.entity.Session;
import com.timecom.sessiontracker.entity.User;
import com.timecom.sessiontracker.entity.enums.DeviceType;
import com.timecom.sessiontracker.exception.MaxSessionsExceededException;
import com.timecom.sessiontracker.exception.SessionExpiredException;
import com.timecom.sessiontracker.repository.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing the full session lifecycle:
 * creation, validation, refresh, expiration, and multi-session handling.
 */
@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final SessionRepository sessionRepository;
    private final int maxSessionsPerUser;
    private final int sessionTimeoutMinutes;

    public SessionService(
            SessionRepository sessionRepository,
            @Value("${app.session.max-sessions-per-user}") int maxSessionsPerUser,
            @Value("${app.session.timeout-minutes}") int sessionTimeoutMinutes) {
        this.sessionRepository = sessionRepository;
        this.maxSessionsPerUser = maxSessionsPerUser;
        this.sessionTimeoutMinutes = sessionTimeoutMinutes;
    }

    /**
     * Create a new session for a user after successful login.
     * Enforces the max sessions per user limit.
     */
    @Transactional
    public Session createSession(User user, String token, String ipAddress,
                                  String userAgent) {
        // Check active session count
        long activeCount = sessionRepository.countByUserIdAndActiveTrue(user.getId());

        if (activeCount >= maxSessionsPerUser) {
            // Expire the oldest active session to make room
            List<Session> activeSessions = sessionRepository
                    .findByUserIdAndActiveTrue(user.getId());
            if (!activeSessions.isEmpty()) {
                Session oldest = activeSessions.get(0);
                oldest.setActive(false);
                oldest.setTerminatedAt(LocalDateTime.now());
                sessionRepository.save(oldest);
                log.info("Expired oldest session {} for user {} (max sessions reached)",
                        oldest.getId(), user.getUsername());
            }
        }

        Session session = Session.builder()
                .user(user)
                .token(token)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .deviceType(detectDeviceType(userAgent))
                .active(true)
                .expiresAt(LocalDateTime.now().plusMinutes(sessionTimeoutMinutes))
                .lastActivityAt(LocalDateTime.now())
                .build();

        Session saved = sessionRepository.save(session);
        log.info("Created session {} for user {} from IP {}",
                saved.getId(), user.getUsername(), ipAddress);
        return saved;
    }

    /**
     * Validate and return a session by its token.
     * Marks expired sessions as inactive.
     */
    @Transactional
    public Session validateSession(String token) {
        Session session = sessionRepository.findByToken(token)
                .orElseThrow(() -> new SessionExpiredException("Session not found"));

        if (!session.isActive()) {
            throw new SessionExpiredException("Session has been terminated");
        }

        if (session.isExpired()) {
            session.setActive(false);
            session.setTerminatedAt(LocalDateTime.now());
            sessionRepository.save(session);
            throw new SessionExpiredException("Session has expired");
        }

        // Update last activity timestamp
        session.setLastActivityAt(LocalDateTime.now());
        sessionRepository.save(session);

        return session;
    }

    /**
     * Refresh a session's expiry time.
     */
    @Transactional
    public Session refreshSession(String token, String newToken) {
        Session session = validateSession(token);
        session.setToken(newToken);
        session.setExpiresAt(LocalDateTime.now().plusMinutes(sessionTimeoutMinutes));
        session.setLastActivityAt(LocalDateTime.now());
        return sessionRepository.save(session);
    }

    /**
     * Terminate a session (logout or admin action).
     */
    @Transactional
    public void terminateSession(String token) {
        sessionRepository.findByToken(token).ifPresent(session -> {
            session.setActive(false);
            session.setTerminatedAt(LocalDateTime.now());
            sessionRepository.save(session);
            log.info("Terminated session {} for user {}",
                    session.getId(), session.getUser().getUsername());
        });
    }

    /**
     * Terminate a session by ID.
     */
    @Transactional
    public void terminateSessionById(Long sessionId) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.setActive(false);
            session.setTerminatedAt(LocalDateTime.now());
            sessionRepository.save(session);
            log.info("Terminated session {} by admin", sessionId);
        });
    }

    /**
     * Get current session info.
     */
    @Transactional(readOnly = true)
    public SessionResponse getSessionByToken(String token) {
        Session session = sessionRepository.findByToken(token)
                .orElseThrow(() -> new SessionExpiredException("Session not found"));
        return toResponse(session);
    }

    /**
     * Get all active sessions for a user.
     */
    @Transactional(readOnly = true)
    public List<SessionResponse> getUserSessions(Long userId) {
        return sessionRepository.findByUserIdAndActiveTrue(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all active sessions (admin).
     */
    @Transactional(readOnly = true)
    public List<SessionResponse> getAllActiveSessions() {
        return sessionRepository.findByActiveTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Expire all stale sessions — called by scheduled cleanup task.
     */
    @Transactional
    public int expireStaleSessions() {
        int count = sessionRepository.expireStaleSession(LocalDateTime.now());
        if (count > 0) {
            log.info("Expired {} stale sessions", count);
        }
        return count;
    }

    /**
     * Detect device type from User-Agent string.
     */
    private DeviceType detectDeviceType(String userAgent) {
        if (userAgent == null) return DeviceType.UNKNOWN;
        String ua = userAgent.toLowerCase();
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) {
            return DeviceType.MOBILE;
        } else if (ua.contains("tablet") || ua.contains("ipad")) {
            return DeviceType.TABLET;
        } else if (ua.contains("mozilla") || ua.contains("chrome") || ua.contains("safari")) {
            return DeviceType.DESKTOP;
        }
        return DeviceType.UNKNOWN;
    }

    /**
     * Map Session entity to SessionResponse DTO.
     */
    private SessionResponse toResponse(Session session) {
        return SessionResponse.builder()
                .id(session.getId())
                .username(session.getUser().getUsername())
                .ipAddress(session.getIpAddress())
                .userAgent(session.getUserAgent())
                .deviceType(session.getDeviceType())
                .active(session.isActive())
                .createdAt(session.getCreatedAt())
                .expiresAt(session.getExpiresAt())
                .lastActivityAt(session.getLastActivityAt())
                .build();
    }
}
