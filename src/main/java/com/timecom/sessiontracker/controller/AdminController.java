package com.timecom.sessiontracker.controller;

import com.timecom.sessiontracker.dto.AnalyticsSummaryResponse;
import com.timecom.sessiontracker.dto.SessionResponse;
import com.timecom.sessiontracker.service.AnalyticsService;
import com.timecom.sessiontracker.service.SessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin REST controller (ADMIN role required).
 * Provides session management and analytics endpoints.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final SessionService sessionService;
    private final AnalyticsService analyticsService;

    public AdminController(SessionService sessionService,
                           AnalyticsService analyticsService) {
        this.sessionService = sessionService;
        this.analyticsService = analyticsService;
    }

    /**
     * GET /api/admin/sessions
     * List all active sessions across all users.
     */
    @GetMapping("/sessions")
    public ResponseEntity<List<SessionResponse>> getAllActiveSessions() {
        List<SessionResponse> sessions = sessionService.getAllActiveSessions();
        return ResponseEntity.ok(sessions);
    }

    /**
     * DELETE /api/admin/sessions/{id}
     * Force-terminate any session.
     */
    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Map<String, String>> terminateSession(@PathVariable Long id) {
        sessionService.terminateSessionById(id);
        return ResponseEntity.ok(Map.of("message",
                "Session " + id + " terminated by admin"));
    }

    /**
     * GET /api/admin/analytics/summary
     * Get aggregated analytics dashboard data.
     */
    @GetMapping("/analytics/summary")
    public ResponseEntity<AnalyticsSummaryResponse> getAnalyticsSummary() {
        AnalyticsSummaryResponse summary = analyticsService.getSummary();
        return ResponseEntity.ok(summary);
    }
}
