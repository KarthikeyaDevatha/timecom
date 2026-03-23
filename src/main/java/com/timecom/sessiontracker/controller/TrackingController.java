package com.timecom.sessiontracker.controller;

import com.timecom.sessiontracker.dto.ActivityResponse;
import com.timecom.sessiontracker.dto.TrackEventRequest;
import com.timecom.sessiontracker.entity.Session;
import com.timecom.sessiontracker.entity.User;
import com.timecom.sessiontracker.exception.UserNotFoundException;
import com.timecom.sessiontracker.repository.UserRepository;
import com.timecom.sessiontracker.service.SessionService;
import com.timecom.sessiontracker.service.TrackingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Activity tracking REST controller.
 * Captures user events (page views, cart actions, etc.) and provides history.
 */
@RestController
@RequestMapping("/api/track")
public class TrackingController {

    private final TrackingService trackingService;
    private final SessionService sessionService;
    private final UserRepository userRepository;

    public TrackingController(TrackingService trackingService,
                              SessionService sessionService,
                              UserRepository userRepository) {
        this.trackingService = trackingService;
        this.sessionService = sessionService;
        this.userRepository = userRepository;
    }

    /**
     * POST /api/track
     * Log a user activity event.
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> trackEvent(
            @Valid @RequestBody TrackEventRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {

        String token = extractToken(httpRequest);
        String ipAddress = getClientIp(httpRequest);

        User user = getUser(userDetails.getUsername());
        Session session = sessionService.validateSession(token);

        // Log event asynchronously
        trackingService.trackEventAsync(request, user, session, ipAddress);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("message", "Event tracked successfully"));
    }

    /**
     * GET /api/track/history
     * Get activity history for the current user (paginated).
     */
    @GetMapping("/history")
    public ResponseEntity<Page<ActivityResponse>> getHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        User user = getUser(userDetails.getUsername());
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<ActivityResponse> history = trackingService
                .getUserActivityHistory(user.getId(), pageable);

        return ResponseEntity.ok(history);
    }

    /**
     * GET /api/track/recent
     * Get the last 50 activity events for the current user.
     */
    @GetMapping("/recent")
    public ResponseEntity<List<ActivityResponse>> getRecentActivity(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails.getUsername());
        List<ActivityResponse> recent = trackingService.getRecentActivity(user.getId());
        return ResponseEntity.ok(recent);
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(
                        "User not found: " + username));
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
