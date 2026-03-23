package com.timecom.sessiontracker.dto;

import com.timecom.sessiontracker.entity.enums.ActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Activity log entry response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityResponse {

    private Long id;
    private ActionType actionType;
    private String resourcePath;
    private String resourceId;
    private Map<String, Object> metadata;
    private String ipAddress;
    private LocalDateTime timestamp;
}
