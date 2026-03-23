-- =============================================
-- V1: Initial Schema for Session Tracker
-- =============================================

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL       PRIMARY KEY,
    username        VARCHAR(50)     NOT NULL UNIQUE,
    email           VARCHAR(100)    NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    role            VARCHAR(20)     NOT NULL DEFAULT 'USER',
    full_name       VARCHAR(100),
    is_enabled      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Sessions table
CREATE TABLE IF NOT EXISTS sessions (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token           VARCHAR(500)    NOT NULL UNIQUE,
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(500),
    device_type     VARCHAR(20)     DEFAULT 'UNKNOWN',
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at      TIMESTAMP       NOT NULL,
    last_activity_at TIMESTAMP,
    terminated_at   TIMESTAMP
);

-- Activity Logs table
CREATE TABLE IF NOT EXISTS activity_logs (
    id              BIGSERIAL       PRIMARY KEY,
    session_id      BIGINT          NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    user_id         BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action_type     VARCHAR(30)     NOT NULL,
    resource_path   VARCHAR(500),
    resource_id     VARCHAR(100),
    metadata        TEXT,
    ip_address      VARCHAR(45),
    timestamp       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- Indexes
-- =============================================

CREATE INDEX IF NOT EXISTS idx_session_user_active ON sessions(user_id, is_active);
CREATE INDEX IF NOT EXISTS idx_session_token ON sessions(token);
CREATE INDEX IF NOT EXISTS idx_session_expires ON sessions(expires_at);
CREATE INDEX IF NOT EXISTS idx_activity_session ON activity_logs(session_id);
CREATE INDEX IF NOT EXISTS idx_activity_user_time ON activity_logs(user_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_activity_type ON activity_logs(action_type);

-- =============================================
-- Seed Data: Admin user (password: admin123)
-- BCrypt hash for 'admin123'
-- =============================================

INSERT INTO users (username, email, password_hash, role, full_name)
VALUES ('admin', 'admin@timecom.com',
        '$2a$12$LQv3c1yqBo9SkvXS7QTJPerY0n57ZGFcAMhX4tFQzUPOF.1B0wJjS',
        'ADMIN', 'System Administrator')
ON CONFLICT (username) DO NOTHING;
