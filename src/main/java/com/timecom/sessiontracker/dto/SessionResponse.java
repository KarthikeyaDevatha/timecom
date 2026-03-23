package com.timecom.sessiontracker.dto;

import com.timecom.sessiontracker.entity.enums.DeviceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Session information response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionResponse {

    private Long id;
    private String username;
    private String ipAddress;
    private String userAgent;
    private DeviceType deviceType;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime lastActivityAt;
}
