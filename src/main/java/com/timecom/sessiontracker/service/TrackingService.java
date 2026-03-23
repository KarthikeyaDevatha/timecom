package com.timecom.sessiontracker.service;

import com.timecom.sessiontracker.dto.ActivityResponse;
import com.timecom.sessiontracker.dto.TrackEventRequest;
import com.timecom.sessiontracker.entity.ActivityLog;
import com.timecom.sessiontracker.entity.Session;
import com.timecom.sessiontracker.entity.User;
import com.timecom.sessiontracker.repository.ActivityLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for tracking user activity events.
 * Supports asynchronous event logging for non-blocking request processing.
 */
@Service
public class TrackingService {

    private static final Logger log = LoggerFactory.getLogger(TrackingService.class);

    private final ActivityLogRepository activityLogRepository;

    public TrackingService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    /**
     * Log a user activity event asynchronously.
     * This method runs on a separate thread to avoid blocking the main request.
     */
    @Async("taskExecutor")
    @Transactional
    public void trackEventAsync(TrackEventRequest request, User user,
                                 Session session, String ipAddress) {
        try {
            ActivityLog activityLog = ActivityLog.builder()
                    .session(session)
                    .user(user)
                    .actionType(request.getActionType())
                    .resourcePath(request.getResourcePath())
                    .resourceId(request.getResourceId())
                    .metadata(request.getMetadata())
                    .ipAddress(ipAddress)
                    .build();

            activityLogRepository.save(activityLog);
            log.debug("Tracked {} event for user {} on session {}",
                    request.getActionType(), user.getUsername(), session.getId());
        } catch (Exception e) {
            log.error("Failed to track event for user {}: {}",
                    user.getUsername(), e.getMessage(), e);
        }
    }

    /**
     * Log a user activity event synchronously.
     * Used for critical events like login/logout where order matters.
     */
    @Transactional
    public ActivityLog trackEventSync(TrackEventRequest request, User user,
                                       Session session, String ipAddress) {
        ActivityLog activityLog = ActivityLog.builder()
                .session(session)
                .user(user)
                .actionType(request.getActionType())
                .resourcePath(request.getResourcePath())
                .resourceId(request.getResourceId())
                .metadata(request.getMetadata())
                .ipAddress(ipAddress)
                .build();

        return activityLogRepository.save(activityLog);
    }

    /**
     * Get activity history for a user (paginated).
     */
    @Transactional(readOnly = true)
    public Page<ActivityResponse> getUserActivityHistory(Long userId, Pageable pageable) {
        return activityLogRepository
                .findByUserIdOrderByTimestampDesc(userId, pageable)
                .map(this::toResponse);
    }

    /**
     * Get recent activity for a user (last 50 events).
     */
    @Transactional(readOnly = true)
    public List<ActivityResponse> getRecentActivity(Long userId) {
        return activityLogRepository
                .findTop50ByUserIdOrderByTimestampDesc(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all activities for a specific session.
     */
    @Transactional(readOnly = true)
    public List<ActivityResponse> getSessionActivity(Long sessionId) {
        return activityLogRepository
                .findBySessionIdOrderByTimestampDesc(sessionId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ActivityResponse toResponse(ActivityLog log) {
        return ActivityResponse.builder()
                .id(log.getId())
                .actionType(log.getActionType())
                .resourcePath(log.getResourcePath())
                .resourceId(log.getResourceId())
                .metadata(log.getMetadata())
                .ipAddress(log.getIpAddress())
                .timestamp(log.getTimestamp())
                .build();
    }
}
