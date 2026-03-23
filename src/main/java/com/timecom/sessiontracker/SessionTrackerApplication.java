package com.timecom.sessiontracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the Session Tracker application.
 * Enables async processing for activity tracking and
 * scheduling for session cleanup tasks.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class SessionTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SessionTrackerApplication.class, args);
    }
}
