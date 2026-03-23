package com.timecom.sessiontracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Analytics summary for admin dashboard.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsSummaryResponse {

    private long totalActiveSessions;
    private long totalActiveUsers;
    private long totalActivitiesToday;
    private long totalActivitiesThisWeek;
    private Map<String, Long> sessionsByDeviceType;
    private Map<String, Long> activityBreakdownToday;
}
