package com.timecom.sessiontracker.dto;

import com.timecom.sessiontracker.entity.enums.ActionType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request payload for tracking a user activity event.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrackEventRequest {

    @NotNull(message = "Action type is required")
    private ActionType actionType;

    private String resourcePath;

    private String resourceId;

    /** Flexible metadata (e.g., product name, search query, cart total) */
    private Map<String, Object> metadata;
}
