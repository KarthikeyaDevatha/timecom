package com.timecom.sessiontracker.service;

import com.timecom.sessiontracker.dto.AnalyticsSummaryResponse;
import com.timecom.sessiontracker.entity.enums.DeviceType;
import com.timecom.sessiontracker.repository.ActivityLogRepository;
import com.timecom.sessiontracker.repository.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service providing analytics and aggregated statistics
 * for the admin dashboard.
 */
@Service
public class AnalyticsService {

    private final SessionRepository sessionRepository;
    private final ActivityLogRepository activityLogRepository;

    public AnalyticsService(SessionRepository sessionRepository,
                            ActivityLogRepository activityLogRepository) {
        this.sessionRepository = sessionRepository;
        this.activityLogRepository = activityLogRepository;
    }

    /**
     * Generate a comprehensive analytics summary.
     */
    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse getSummary() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);
        LocalDateTime weekStart = LocalDate.now().minusDays(7).atStartOfDay();

        // Active sessions
        long activeSessions = sessionRepository.countByActiveTrue();
        long activeUsers = sessionRepository.countDistinctActiveUsers();

        // Activity counts
        long activitiesToday = activityLogRepository
                .countByTimestampBetween(todayStart, todayEnd);
        long activitiesWeek = activityLogRepository
                .countByTimestampBetween(weekStart, todayEnd);

        // Sessions by device type
        Map<String, Long> deviceBreakdown = new LinkedHashMap<>();
        for (Object[] row : sessionRepository.countActiveSessionsByDeviceType()) {
            DeviceType type = (DeviceType) row[0];
            Long count = (Long) row[1];
            deviceBreakdown.put(type.name(), count);
        }

        // Activity breakdown by type (today)
        Map<String, Long> activityBreakdown = new LinkedHashMap<>();
        for (Object[] row : activityLogRepository
                .countByActionTypeBetween(todayStart, todayEnd)) {
            String actionType = row[0].toString();
            Long count = (Long) row[1];
            activityBreakdown.put(actionType, count);
        }

        return AnalyticsSummaryResponse.builder()
                .totalActiveSessions(activeSessions)
                .totalActiveUsers(activeUsers)
                .totalActivitiesToday(activitiesToday)
                .totalActivitiesThisWeek(activitiesWeek)
                .sessionsByDeviceType(deviceBreakdown)
                .activityBreakdownToday(activityBreakdown)
                .build();
    }
}
