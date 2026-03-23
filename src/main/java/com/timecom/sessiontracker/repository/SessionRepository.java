package com.timecom.sessiontracker.repository;

import com.timecom.sessiontracker.entity.Session;
import com.timecom.sessiontracker.entity.enums.DeviceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Session entity with custom queries for
 * session management, analytics, and cleanup.
 */
@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findByToken(String token);

    List<Session> findByUserIdAndActiveTrue(Long userId);

    List<Session> findByActiveTrue();

    long countByActiveTrue();

    long countByUserIdAndActiveTrue(Long userId);

    /**
     * Expire all sessions that have passed their expiry time.
     */
    @Modifying
    @Query("UPDATE Session s SET s.active = false, s.terminatedAt = :now " +
           "WHERE s.active = true AND s.expiresAt < :now")
    int expireStaleSession(@Param("now") LocalDateTime now);

    /**
     * Count active sessions grouped by device type.
     */
    @Query("SELECT s.deviceType, COUNT(s) FROM Session s " +
           "WHERE s.active = true GROUP BY s.deviceType")
    List<Object[]> countActiveSessionsByDeviceType();

    /**
     * Count distinct active users (users with at least one active session).
     */
    @Query("SELECT COUNT(DISTINCT s.user.id) FROM Session s WHERE s.active = true")
    long countDistinctActiveUsers();

    /**
     * Find sessions created within a date range.
     */
    @Query("SELECT s FROM Session s WHERE s.createdAt BETWEEN :start AND :end")
    List<Session> findSessionsCreatedBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
