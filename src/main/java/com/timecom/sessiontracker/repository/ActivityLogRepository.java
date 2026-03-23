package com.timecom.sessiontracker.repository;

import com.timecom.sessiontracker.entity.ActivityLog;
import com.timecom.sessiontracker.entity.enums.ActionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for ActivityLog entity with analytics and
 * history queries.
 */
@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    Page<ActivityLog> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);

    List<ActivityLog> findBySessionIdOrderByTimestampDesc(Long sessionId);

    /**
     * Count activities grouped by action type within a time range.
     */
    @Query("SELECT a.actionType, COUNT(a) FROM ActivityLog a " +
           "WHERE a.timestamp BETWEEN :start AND :end " +
           "GROUP BY a.actionType")
    List<Object[]> countByActionTypeBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Count total activities within a time range.
     */
    long countByTimestampBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Count distinct active users within a time range.
     */
    @Query("SELECT COUNT(DISTINCT a.user.id) FROM ActivityLog a " +
           "WHERE a.timestamp BETWEEN :start AND :end")
    long countDistinctUsersBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Get recent activities for a specific user.
     */
    List<ActivityLog> findTop50ByUserIdOrderByTimestampDesc(Long userId);
}
