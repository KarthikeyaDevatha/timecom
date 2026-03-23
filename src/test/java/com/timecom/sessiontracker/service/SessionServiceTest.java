package com.timecom.sessiontracker.service;

import com.timecom.sessiontracker.entity.Session;
import com.timecom.sessiontracker.entity.User;
import com.timecom.sessiontracker.entity.enums.DeviceType;
import com.timecom.sessiontracker.exception.SessionExpiredException;
import com.timecom.sessiontracker.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SessionService.
 */
@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    private SessionService sessionService;

    private User testUser;

    @BeforeEach
    void setUp() {
        sessionService = new SessionService(sessionRepository, 5, 60);

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashed")
                .build();
    }

    @Test
    @DisplayName("Should create a new session")
    void createSession_ShouldCreateAndReturnSession() {
        when(sessionRepository.countByUserIdAndActiveTrue(anyLong())).thenReturn(0L);
        when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> {
            Session s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        Session session = sessionService.createSession(testUser, "token123",
                "127.0.0.1", "Mozilla/5.0");

        assertNotNull(session);
        assertEquals("token123", session.getToken());
        assertEquals("127.0.0.1", session.getIpAddress());
        assertTrue(session.isActive());
        assertEquals(DeviceType.DESKTOP, session.getDeviceType());
        verify(sessionRepository).save(any(Session.class));
    }

    @Test
    @DisplayName("Should expire oldest session when max reached")
    void createSession_ShouldExpireOldestWhenMaxReached() {
        Session oldSession = Session.builder()
                .id(1L).user(testUser).token("old").active(true)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        when(sessionRepository.countByUserIdAndActiveTrue(anyLong())).thenReturn(5L);
        when(sessionRepository.findByUserIdAndActiveTrue(anyLong()))
                .thenReturn(List.of(oldSession));
        when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> {
            Session s = inv.getArgument(0);
            if (s.getId() == null) s.setId(2L);
            return s;
        });

        Session newSession = sessionService.createSession(testUser, "new_token",
                "127.0.0.1", "Mozilla/5.0");

        assertNotNull(newSession);
        // Old session should be deactivated
        assertFalse(oldSession.isActive());
        verify(sessionRepository, times(2)).save(any(Session.class));
    }

    @Test
    @DisplayName("Should validate an active session")
    void validateSession_ShouldReturnSessionForValidToken() {
        Session session = Session.builder()
                .id(1L).user(testUser).token("valid_token").active(true)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        when(sessionRepository.findByToken("valid_token")).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(Session.class))).thenReturn(session);

        Session result = sessionService.validateSession("valid_token");
        assertNotNull(result);
        assertTrue(result.isActive());
    }

    @Test
    @DisplayName("Should throw for expired session")
    void validateSession_ShouldThrowForExpiredSession() {
        Session session = Session.builder()
                .id(1L).user(testUser).token("expired_token").active(true)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();

        when(sessionRepository.findByToken("expired_token")).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(Session.class))).thenReturn(session);

        assertThrows(SessionExpiredException.class,
                () -> sessionService.validateSession("expired_token"));
    }

    @Test
    @DisplayName("Should throw for non-existent session")
    void validateSession_ShouldThrowForMissingSession() {
        when(sessionRepository.findByToken("missing")).thenReturn(Optional.empty());

        assertThrows(SessionExpiredException.class,
                () -> sessionService.validateSession("missing"));
    }

    @Test
    @DisplayName("Should terminate a session")
    void terminateSession_ShouldDeactivateSession() {
        Session session = Session.builder()
                .id(1L).user(testUser).token("token").active(true)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        when(sessionRepository.findByToken("token")).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(Session.class))).thenReturn(session);

        sessionService.terminateSession("token");

        assertFalse(session.isActive());
        assertNotNull(session.getTerminatedAt());
        verify(sessionRepository).save(session);
    }

    @Test
    @DisplayName("Should detect mobile device type")
    void createSession_ShouldDetectMobileDevice() {
        when(sessionRepository.countByUserIdAndActiveTrue(anyLong())).thenReturn(0L);
        when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> {
            Session s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        Session session = sessionService.createSession(testUser, "token",
                "127.0.0.1", "Mozilla/5.0 (iPhone; CPU iPhone OS)");

        assertEquals(DeviceType.MOBILE, session.getDeviceType());
    }

    @Test
    @DisplayName("Should expire stale sessions")
    void expireStaleSessions_ShouldCallRepository() {
        when(sessionRepository.expireStaleSession(any(LocalDateTime.class))).thenReturn(3);

        int count = sessionService.expireStaleSessions();

        assertEquals(3, count);
        verify(sessionRepository).expireStaleSession(any(LocalDateTime.class));
    }
}
