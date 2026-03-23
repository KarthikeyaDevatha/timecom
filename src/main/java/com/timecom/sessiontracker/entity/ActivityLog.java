package com.timecom.sessiontracker.entity;

import com.timecom.sessiontracker.entity.enums.ActionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Activity log entity for tracking user actions within sessions.
 * Supports flexible metadata via a JSON column.
 */
@Entity
@Table(name = "activity_logs", indexes = {
        @Index(name = "idx_activity_session", columnList = "session_id"),
        @Index(name = "idx_activity_user_time", columnList = "user_id, timestamp"),
        @Index(name = "idx_activity_type", columnList = "action_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30)
    private ActionType actionType;

    @Column(name = "resource_path", length = 500)
    private String resourcePath;

    @Column(name = "resource_id", length = 100)
    private String resourceId;

    @Column(name = "metadata", columnDefinition = "TEXT")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "timestamp", updatable = false)
    private LocalDateTime timestamp;
}
